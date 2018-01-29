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

package com.verizon.northstar.dpe.context;

import org.apache.log4j.Logger;
import org.apache.spark.SparkConf;

import java.io.File;
import java.lang.reflect.Constructor;
import org.apache.spark.SecurityManager;
import java.lang.reflect.InvocationTargetException;

public class HTTPServer {
  private Logger logger = Logger.getLogger(this.getClass().getName());

  public Object createHttpServer(File outputDir) {
    SparkConf conf = new SparkConf();
    try {
      // try to create HttpServer
      Constructor<?> constructor = getClass().getClassLoader()
          .loadClass("org.apache.spark.HttpServer")
          .getConstructor(new Class[]{SparkConf.class, File.class, SecurityManager.class, int.class, String.class});
      return constructor.newInstance(new Object[] {conf, outputDir, new SecurityManager(conf), 0, "HTTP Server"});
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
        InstantiationException | InvocationTargetException e) {
      // fallback to old constructor
      Constructor<?> constructor = null;
      try {
        constructor = getClass().getClassLoader()
            .loadClass("org.apache.spark.HttpServer")
            .getConstructor(new Class[]{File.class, SecurityManager.class, int.class, String.class});
        return constructor.newInstance(new Object[] {outputDir, new SecurityManager(conf), 0, "HTTP Server"});
      } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |
          InstantiationException | InvocationTargetException e1) {
        logger.error(e1.getMessage(), e1);
        return null;
      }
    }
  }
}
