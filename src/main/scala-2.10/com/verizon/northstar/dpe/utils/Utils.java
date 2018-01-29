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

package com.verizon.northstar.dpe.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.InvocationTargetException;

public class Utils {
  public static final String BOOLEAN_TYPE = "BooleanType";
  public static final String STRING_TYPE = "StringType";
  public static final String TIMESTAMP_TYPE = "TimestampType";
  public static final String DATE_TYPE = "DateType";
  public static final String INTEGER_TYPE = "IntegerType";
  public static final String LONG_TYPE = "LongType";
  public static final String DECIMAL_TYPE = "DecimalType";
  public static final String FLOAT_TYPE = "FloatType";
  public static final String DOUBLE_TYPE = "DoubleType";
  public static final String BINARY_TYPE = "BinaryType";
  public static final String ARRAY_TYPE = "ArrayType";
  public static final String MAP_TYPE = "MapType";

  public static final String BOOL = "bool";
  public static final String STRING = "string";
  public static final String TIME = "time";
  public static final String INTEGER = "int";
  public static final String DOUBLE = "double";
  public static final String BLOB = "blob";
  public static final String ARRAY = "array";
  public static final String MAP = "map";
  public static final String UNKNOWN = "unknown_type";

  public static Logger logger = LoggerFactory.getLogger(Utils.class);

  public static Object invokeMethod(Object o, String name) {
    return invokeMethod(o, name, new Class[]{}, new Object[]{});
  }

  public static Object invokeMethod(Object o, String name, Class[] argTypes, Object[] params) {
    try {
      return o.getClass().getMethod(name, argTypes).invoke(o, params);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      logger.error(e.getMessage(), e);
    }
    return null;
  }

  public static Object invokeStaticMethod(Class c, String name, Class[] argTypes, Object[] params) {
    try {
      return c.getMethod(name, argTypes).invoke(null, params);
    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      logger.error(e.getMessage(), e);
    }
    return null;
  }

  public static Class findClass(String name) {
    return findClass(name, false);
  }

  public static Class findClass(String name, boolean silence) {
    try {
      return Utils.class.forName(name);
    } catch (ClassNotFoundException e) {
      if (!silence) {
        logger.error(e.getMessage(), e);
      }
      return null;
    }
  }

  public static File createTempDir(String dir) {
    File file;

    file = (File) invokeStaticMethod(
        Utils.findClass("org.apache.spark.util.Utils"),
        "createTempDir",
        new Class[]{String.class, String.class},
        new Object[]{dir, "spark"});

    // fallback to old method
    if (file == null) {
      file = (File) invokeStaticMethod(
          Utils.findClass("org.apache.spark.util.Utils"),
          "createTempDir",
          new Class[]{String.class},
          new Object[]{dir});
    }

    return file;
  }

  public static String getInternalType(String t) {
    switch(t) {
      case BOOLEAN_TYPE:
        return BOOL;
      case STRING_TYPE:
        return STRING;
      case TIMESTAMP_TYPE: case DATE_TYPE:
        return TIME;
      case INTEGER_TYPE: case LONG_TYPE:
        return INTEGER;
      case FLOAT_TYPE: case DOUBLE_TYPE:
        return DOUBLE;
      case BINARY_TYPE:
        return BLOB;
      default:
        if(t.startsWith(ARRAY_TYPE)) {
          return ARRAY + "[" + getInternalType(t.substring(10, t.length()-1).split(",")[0]) + "]";
        } else if(t.startsWith(MAP_TYPE)) {
          String[] types = t.substring(8, t.length()-1).split(",");
          return MAP + "[" + getInternalType(types[0]) + "]" + getInternalType(types[1]);
        } else if(t.startsWith(DECIMAL_TYPE)) {
          return INTEGER;
        } else {
          return UNKNOWN;
        }
    }
  }
}