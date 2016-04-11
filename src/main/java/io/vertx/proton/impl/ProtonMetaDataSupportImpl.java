package io.vertx.proton.impl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.apache.qpid.proton.amqp.Symbol;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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
