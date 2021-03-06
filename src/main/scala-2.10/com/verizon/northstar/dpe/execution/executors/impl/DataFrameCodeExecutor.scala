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

package com.verizon.northstar.dpe.execution.executors.impl

import java.util.logging.Logger

import com.verizon.northstar.dpe.execution.CodeExecutor
import com.verizon.northstar.dpe.execution.converter.DataFrameConverter
import com.verizon.northstar.dpe.execution.executors.{ExecutorResponse, HandleException}
import com.verizon.northstar.dpe.execution.mime.MIMEType
import com.verizon.northstar.dpe.execution.output.DPEOutput
import com.verizon.northstar.dpe.interpreter.Interpreter
import com.verizon.northstar.dpe.interpreter.output.{ExecuteError, Results}

import scala.util.Try

class DataFrameCodeExecutor(interpreter: Interpreter, limit: Int) extends CodeExecutor {
  private val logger = Logger.getLogger(this.getClass.getName)

  private def convertToTable(code: String): DPEOutput = {
    logger.info("CODE: " + code)
    val (result, message) = interpreter.interpret(code)
    result match {
      case Results.Success =>
        val rddVarName =  interpreter.lastExecutionVariableName.get
        interpreter.read(rddVarName).map(variableVal => {
          DataFrameConverter.convertToJson(
            variableVal.asInstanceOf[org.apache.spark.sql.DataFrame],
            limit
          ).map(output =>
            DPEOutput(MIMEType.TABLE, output, status=result.toString, errorDescr="")
          ).get
        }).getOrElse(DPEOutput(status=result.toString,
          errorDescr=ExecutorResponse.NoVariableFound(rddVarName)))
      case Results.Aborted =>
        logger.info(ExecutorResponse.ErrorMessage(ExecutorResponse.Aborted))
        DPEOutput(status=result.toString,
          errorDescr=ExecutorResponse.ErrorMessage(ExecutorResponse.Aborted))
      case Results.Error =>
        val error = message.right.get.asInstanceOf[ExecuteError]
        val errorMessage = ExecutorResponse.ErrorMessage(error.value)
        DPEOutput(status=result.toString, errorDescr=errorMessage)
      case Results.Incomplete =>
        logger.info(ExecutorResponse.Incomplete)
        DPEOutput(status=result.toString, errorDescr=ExecutorResponse.Incomplete)
    }
  }

  override def execute(code: String): DPEOutput = {
    val lines = code.trim.split("\n")
    Try({
      val res: DPEOutput = if (lines.length == 1 && lines.head.length == 0){
        HandleException.handleException(Some(new Exception("Statement field is empty")))
      } else if (lines.length == 1) {
        convertToTable(lines.head)
      } else {
        convertToTable(lines.drop(1).reduce(_ + _))
      }
      res
    }).recover({
      case e: Exception => {
        e.printStackTrace()
        HandleException.handleException(Some(e))
      }
    }).get
  }
}