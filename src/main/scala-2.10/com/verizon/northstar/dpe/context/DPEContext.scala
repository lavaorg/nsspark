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

package com.verizon.northstar.dpe.context

import java.util.logging.Logger

import com.verizon.northstar.dpe.config.DPECfg
import com.verizon.northstar.dpe.utils.Utils
import org.apache.spark.api.java.JavaSparkContext
import org.apache.spark.{SparkConf, SparkContext}

object DPEContext {
  private val logger = Logger.getLogger(this.getClass.getName)
  private val sc: SparkContext = initSparkContext()

  def getSparkContext(): SparkContext = {
    return sc
  }

  private def initSparkContext(): SparkContext = {
    val conf = new SparkConf()

    val classServerUri = startHTTPServer()
    if (classServerUri != null) {
      conf.set("spark.repl.class.uri", classServerUri)
    }

    if (DPECfg.REPL_OUTPUT_DIR != null) {
      conf.set("spark.repl.class.outputDir", DPECfg.REPL_OUTPUT_DIR.getAbsolutePath);
    }

    conf.set("spark.scheduler.mode", "FAIR")
    conf.set("spark.scheduler.allocation.file", DPECfg.SPARK_ALLOCATION_FILE)
    return new JavaSparkContext(conf)
  }

  private def startHTTPServer(): String = {
    val httpServer = new HTTPServer()
    val classServer = httpServer.createHttpServer(DPECfg.REPL_OUTPUT_DIR)
    Utils.invokeMethod(classServer, "start")
    val classServerUri = Utils.invokeMethod(classServer, "uri").toString
    logger.info("Class server URI: " + classServerUri)
    return classServerUri
  }
}