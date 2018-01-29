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

package com.verizon.northstar.dpe.execution.output

import play.api.libs.json._

object OutputFormatter {
  implicit val numberTypeWrites = new Writes[Number] {
    def writes(number: Number) = Json.obj(
        "type" -> number.dataType,
        "value" -> number.value
      )
  }

  private def getDataType[T](v: T) = v match {
    case _: Int    => "int"
    case _: String => "string"
    case _: Double => "double"
    case _: Long   => "long"
    case _: Short  => "short"
    case _: Float  => "float"
    case _         => "unknown"
  }

  def generateNumber(value: AnyRef): String = {
    val number = Number(getDataType(value), value.toString)
    Json.toJson(number).toString
  }
}
