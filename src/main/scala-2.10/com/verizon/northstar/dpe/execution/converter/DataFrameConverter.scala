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

package com.verizon.northstar.dpe.execution.converter

import org.apache.spark.sql.DataFrame
import play.api.libs.json.{JsObject, Json}
import com.verizon.northstar.dpe.utils.Utils
import scala.util.Try

object DataFrameConverter {
  def convertToJson(df: DataFrame, limit: Int = 0): Try[String] = {
      if (df.count() == 0) {
        val empty = Json.toJson(Array[String]())
        Try(JsObject(Seq("columns" -> empty, "types" -> empty, "rows" -> empty)).toString())
      } else {
        val columns = Json.toJson(df.schema.fieldNames)
        val rawTypes = df.schema.fields.map(x => Utils.getInternalType(x.dataType.toString))
        var rawRows: Array[Array[String]] = null

        if (limit > 0) {
          rawRows = df.map(row => (row.toSeq, rawTypes).zipped.map((x, y) => getFormattedData(x, y)).toArray).take(limit)
        } else {
          rawRows = df.map(row => (row.toSeq, rawTypes).zipped.map((x, y) => getFormattedData(x, y)).toArray).toArray
        }

        val rows = Json.toJson(rawRows)
        val types = Json.toJson(rawTypes)
        Try(JsObject(Seq("columns" -> columns, "types" -> types, "rows" -> rows)).toString())
      }
  }

  def getFormattedData(data: Any, dataType: String): String = {
    if (data == null) {
      return ""
    }

    dataType match {
      case Utils.BLOB =>
        "0x" + data.asInstanceOf[Array[Byte]].map("%02X" format _).mkString
      case Utils.STRING =>
        "\"" + data + "\""
      case Utils.TIME =>
        "\"" + data.toString + "\""
      case _ =>
        if (dataType.startsWith(Utils.ARRAY)) {
          "[" + data.asInstanceOf[scala.collection.mutable.WrappedArray[Any]].array.map(
            d => getFormattedData(d, dataType.stripPrefix(Utils.ARRAY + "[").stripSuffix("]"))
          ).mkString(" ") + "]"
        } else if (dataType.startsWith(Utils.MAP)) {
          var nested = 0
          var end = 0
          dataType.zipWithIndex.map(
            e =>
              if (e._1 == '['){
                nested += 1
              } else if (e._1 == ']') {
                nested -= 1
                if(nested == 0) {
                  end = e._2
                }
              }
          )

          val keyType = dataType.substring(4, end)
          val valueType = dataType.substring(end+1, dataType.length)
          Utils.MAP + "[" + data.asInstanceOf[scala.collection.Map[Any, Any]].toList.map(
            e => getFormattedData(e._1, keyType) + ":" + getFormattedData(e._2, valueType)
          ).mkString(" ") + "]"
        } else {
          data.toString
        }
    }
  }
}