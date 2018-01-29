/*
 * Copyright (C) 2017 Verizon. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verizon.northstar.dpe.interpreter

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.concurrent.ExecutionException
import java.util.logging.Logger

import com.verizon.northstar.dpe.config.DPECfg
import com.verizon.northstar.dpe.context.DPEContext
import com.verizon.northstar.dpe.interpreter.output.ExecuteOutput.ExecuteOutput
import com.verizon.northstar.dpe.interpreter.output._
import com.verizon.northstar.dpe.utils.Utils
import org.apache.spark.repl.SparkIMain
import org.apache.spark.sql.SQLContext

import scala.annotation.tailrec
import scala.concurrent.{Await, Future}
import scala.language.reflectiveCalls
import scala.tools.nsc.GenericRunnerSettings
import scala.tools.nsc.interpreter.{IR, JPrintWriter, OutputStream}
import scala.tools.nsc.util.ClassPath

/**
  * The Spark interpreter implementation.
  *
  * Created by Eugen Feller <eugen.feller@verizon.com> on 8/22/16.
  */
class ScalaInterpreter extends Interpreter {
  private val logger = Logger.getLogger(this.getClass.getName)
  private val ExecutionExceptionName = "lastException"
  private val taskManager: TaskManager = new TaskManager(maximumWorkers = 100)
  private val lastResultOut = new ByteArrayOutputStream()
  private val multiOutputStream = MultiOutputStream(List(Console.out, lastResultOut))
  private val thisClassloader = this.getClass.getClassLoader
  private var sqlContext: SQLContext = null
  private var sparkIMain: SparkIMain = createSparkIMain(new JPrintWriter(multiOutputStream, true),
    DPECfg.REPL_OUTPUT_DIR.getAbsolutePath)

  override def start() {
    require(sparkIMain != null)
    logger.info("Starting Scala interpreter")

    // Get SC and init SQL context
    val sc = DPEContext.getSparkContext()
    sc.setLocalProperty("spark.scheduler.pool", "production")

    sqlContext = new SQLContext(sc)

    // Bind quietly in order not to dirty code output
    sparkIMain.beQuietDuring {
      sparkIMain.bind("sc", "org.apache.spark.SparkContext", sc)
      sparkIMain.bind("sqlContext", "org.apache.spark.sql.SQLContext", sqlContext)
    }

    System.setProperty("scala.repl.name.line", ("$line" + this.hashCode()).replace('-', '0'))
  }

  override def interpret(code: String, silent: Boolean = false, output: Option[OutputStream]):
  (Results.Result, Either[ExecuteOutput, ExecuteFailure]) = {
    require(sparkIMain != null)
    val starting = (Results.Success, Left(""))
    interpretRec(code.trim.split("\n").toList, false, starting)
  }

  override def lastExecutionVariableName: Option[String] = {
    require(sparkIMain != null)
    val lastRequestMethod = classOf[SparkIMain].getDeclaredMethod("lastRequest")
    lastRequestMethod.setAccessible(true)

    val request = lastRequestMethod.invoke(sparkIMain).asInstanceOf[SparkIMain#Request]
    val mostRecentVariableName = sparkIMain.mostRecentVar
    request.definedNames.map(_.toString).find(_ == mostRecentVariableName)
  }

  override def read(variableName: String): Option[AnyRef] = {
    require(sparkIMain != null)
    val variable = sparkIMain.valueOfTerm(variableName)
    if (variable == null || variable.isEmpty) None
    else variable
  }

  override def close(): Unit = {
    if (sparkIMain != null) {
      sparkIMain.close()
      sparkIMain = null
    }

    if (sqlContext != null) {
      sqlContext.clearCache()
    }
  }

  protected def createSparkIMain(out: JPrintWriter, outputDir: String): SparkIMain = {
    taskManager.start()
    val settings = new GenericRunnerSettings( println _ )
    settings.classpath.value = buildClasspath(thisClassloader)
    logger.info("Interpreter CP: " + settings.classpath.value)

    val sparkIMain = new SparkIMain(settings, out)
    sparkIMain.initializeSynchronous()

    // Change virtual directory
    logger.info("Interpreter class dir: " + outputDir)
    settings.outputDirs.setSingleOutput(outputDir)

    val cl = Utils.invokeMethod(sparkIMain, "classLoader").asInstanceOf[ClassLoader]
    val rootField = cl.getClass().getSuperclass().getDeclaredField("root")
    rootField.setAccessible(true)
    rootField.set(cl, settings.outputDirs.getSingleOutput.get)
    return sparkIMain
  }

  protected def buildClasspath(classLoader: ClassLoader): String = {
    def toClassLoaderList(classLoader: ClassLoader): Seq[ClassLoader] = {
      @tailrec
      def toClassLoaderListHelper(aClassLoader: ClassLoader,
                                  theList: Seq[ClassLoader]):Seq[ClassLoader] = {
        if( aClassLoader == null )
          return theList

        toClassLoaderListHelper(aClassLoader.getParent, aClassLoader +: theList)
      }
      toClassLoaderListHelper(classLoader, Seq())
    }

    val urls = toClassLoaderList(classLoader).flatMap{
      case cl: java.net.URLClassLoader => cl.getURLs.toList
      case a => List()
    }

    urls.foldLeft("")((l, r) => ClassPath.join(l, r.toString))
  }

  protected def truncateResult(result:String, showType:Boolean =false, noTruncate: Boolean = false): String = {
    val resultRX="""(?s)(res\d+):\s+(.+)\s+=\s+(.*)""".r

    result match {
      case resultRX(varName,varType,resString) => {
        var returnStr=resString
        if (noTruncate)
        {
          val r = read(varName)
          returnStr = r.getOrElse("").toString
        }

        if (showType)
          returnStr=varType+" = "+returnStr

        returnStr

      }
      case _ =>  ""
    }
  }

  protected def interpretRec(lines: List[String],
                             silent: Boolean = false,
                             results: (Results.Result, Either[ExecuteOutput, ExecuteFailure])):
  (Results.Result, Either[ExecuteOutput, ExecuteFailure]) = {
    lines match {
      case Nil => results
      case x :: xs =>
        val output = interpretLine(x)

        output._1 match {
          // if success, keep interpreting and aggregate ExecuteOutputs
          case Results.Success =>
            val result = for {
              originalResult <- output._2.left
            } yield(truncateResult(originalResult, InterpreterOptions.showTypes,InterpreterOptions.noTruncation))
            interpretRec(xs, silent, (output._1, result))

          // if incomplete, keep combining incomplete statements
          case Results.Incomplete =>
            xs match {
              case Nil => interpretRec(Nil, silent, (Results.Incomplete, results._2))
              case _ => interpretRec(x + "\n" + xs.head :: xs.tail, silent, results)
            }

          //
          case Results.Aborted => output
          //interpretRec(Nil, silent, output)

          // if failure, stop interpreting and return the error
          case Results.Error =>
            val result = for {
              curr <- output._2.right
            } yield curr
            interpretRec(Nil, silent, (output._1, result))
        }
    }
  }

  protected def interpretLine(line: String, silent: Boolean = false):
  (Results.Result, Either[ExecuteOutput, ExecuteFailure]) =
  {
    val futureResult = interpretAddTask(line, silent)

    // Map the old result types to our new types
    val mappedFutureResult = interpretMapToCustomResult(futureResult)

    // Determine whether to provide an error or output
    val futureResultAndOutput = interpretMapToResultAndOutput(mappedFutureResult)

    // Generate execute info
    val futureResultAndExecuteInfo = interpretMapToResultAndExecuteInfo(futureResultAndOutput)

    // Block indefinitely until our result has arrived
    import scala.concurrent.duration._
    logger.info("Waiting for execution to finish")
    Await.result(futureResultAndExecuteInfo, Duration.Inf)
  }

  protected def interpretAddTask(code: String, silent: Boolean) = {
    taskManager.add {
      if (silent) {
          sparkIMain.beSilentDuring {
            sparkIMain.interpret(code)
          }
        } else {
          sparkIMain.interpret(code)
        }
      }
  }

  protected def interpretMapToCustomResult(future: Future[IR.Result]) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    future map {
      case IR.Success             => Results.Success
      case IR.Error               => Results.Error
      case IR.Incomplete          => Results.Incomplete
    } recover {
      case ex: ExecutionException => Results.Aborted
    }
  }

  protected def interpretMapToResultAndOutput(future: Future[Results.Result]) = {
    import scala.concurrent.ExecutionContext.Implicits.global
    future map {
      result => {
        val output = lastResultOut.toString(Charset.forName("UTF-8").name()).trim
        lastResultOut.reset()
        (result, output)
      }
    }
  }

  protected def interpretMapToResultAndExecuteInfo(future: Future[(Results.Result, String)]):
  Future[(Results.Result, Either[ExecuteOutput, ExecuteFailure])] =
  {
    import scala.concurrent.ExecutionContext.Implicits.global
    future map {
      case (Results.Success, output)    => (Results.Success, Left(output))
      case (Results.Incomplete, output) => (Results.Incomplete, Left(output))
      case (Results.Aborted, output)    => (Results.Aborted, Right(null))
      case (Results.Error, output)      => {
        val x = sparkIMain.valueOfTerm(ExecutionExceptionName)
        (Results.Error, Right(interpretConstructExecuteError(sparkIMain.valueOfTerm(ExecutionExceptionName), output)))
      }
    }
  }

  protected def interpretConstructExecuteError(value: Option[AnyRef], output: String) =
    value match {
      // Runtime error
      case Some(e) if e != null =>
        val ex = e.asInstanceOf[Throwable]
        // Clear runtime error message
        sparkIMain.directBind(ExecutionExceptionName, classOf[Throwable].getName, null)
        ExecuteError(ex.getClass.getName,
          ex.getLocalizedMessage,
          ex.getStackTrace.map(_.toString).toList
        )
      // Compile time error, need to check internal reporter
      case _ =>
        if (sparkIMain.isReportingErrors) {
          // TODO: This wrapper is not needed when just getting compile
          // error that we are not parsing... maybe have it be purely
          // output and have the error check this?
          ExecuteError("Compile Error", output, List())
        }
        else {
          ExecuteError("Unknown", "Unable to retrieve error!", List())
        }
    }
}
