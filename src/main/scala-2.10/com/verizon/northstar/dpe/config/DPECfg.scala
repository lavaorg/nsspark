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

package com.verizon.northstar.dpe.config

import java.io.File
import java.util.logging.Logger

import com.verizon.northstar.dpe.utils.Utils

import scala.util.Properties

object DPECfg {
  private val logger = Logger.getLogger(this.getClass.getName)
  private final val replOutputDir: String =
    Properties.envOrElse("DPE_SPARK_REPL_OUTPUT_DIR", "/tmp/dpe-spark")

  // Spark resource allocation file
  final val SPARK_ALLOCATION_FILE: String = Properties.envOrElse("DPE_SPARK_ALLOCATION_FILE",
      System.getenv("SPARK_HOME") + "/conf/allocation.xml")
  logger.info("SPARK_ALLOCATION_FILE: " + SPARK_ALLOCATION_FILE)

  // REPL output dir
  final val REPL_OUTPUT_DIR: File = Utils.createTempDir(replOutputDir);
  logger.info("REPL_OUTPUT_DIR: " + DPECfg.REPL_OUTPUT_DIR.getAbsolutePath)
}
