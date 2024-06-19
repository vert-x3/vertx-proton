/*
* Copyright 2016 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.vertx.proton.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.qpid.proton.amqp.Symbol;

import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;

public class ProtonMetaDataSupportImpl {
  private static final Logger LOG = LoggerFactory.getLogger(ProtonMetaDataSupportImpl.class);

  public static final String PRODUCT = "vertx-proton";
  public static final Symbol PRODUCT_KEY = Symbol.valueOf("product");
  public static final String VERSION;
  public static final Symbol VERSION_KEY = Symbol.valueOf("version");

  static {
    String version = "unknown";
    try {
      InputStream in = null;
      String path = ProtonMetaDataSupportImpl.class.getPackage().getName().replace(".", "/");
      if ((in = ProtonMetaDataSupportImpl.class.getResourceAsStream("/" + path + "/version.txt")) != null) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));) {
          String line = reader.readLine();
          if (line != null && !line.isEmpty()) {
            version = line;
          }
        }
      }
    } catch (Throwable err) {
      LOG.error("Problem determining version details", err);
    }

    VERSION = version;
  }
}
