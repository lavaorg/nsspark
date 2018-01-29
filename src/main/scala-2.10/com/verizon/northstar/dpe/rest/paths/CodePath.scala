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

package com.verizon.northstar.dpe.rest.paths

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.server.Directives._
import com.verizon.northstar.dpe.execution.executors.impl.{DataFrameCodeExecutor, NumberCodeExecutor}
import com.verizon.northstar.dpe.execution.output.DPEOutput
import com.verizon.northstar.dpe.interpreter.output.Results
import com.verizon.northstar.dpe.interpreter.{Interpreter, ScalaInterpreter}
import com.verizon.northstar.dpe.rest.model.Converter
import com.verizon.northstar.dpe.rest.model.Models.{ExecuteCode, ServiceJsonProtoocol}

trait CodePath extends ConcisePaths {
  import ServiceJsonProtoocol._

  def execute(interpreter: Interpreter, code: ExecuteCode): DPEOutput = {
    code.converter match {
      case Converter.DataFrame => {
        val df = new DataFrameCodeExecutor(interpreter, code.limit)
        return df.execute(code.statements)
      }
      case Converter.Number => {
        val df = new NumberCodeExecutor(interpreter)
        return df.execute(code.statements)
      }
      case _ => {
        return DPEOutput(status = Results.Error.toString,
          errorDescr = s"Wrong converter selected: ${code.converter}")
      }
    }
  }

  val codePath =
    concisePath("execute") {
      post {
        entity(as[ExecuteCode]) {
          code => complete {
            val interpreter = new ScalaInterpreter()
            interpreter.start()
            val result = execute(interpreter, code)
            interpreter.close()
            result
          }
        }
      }
    }
}