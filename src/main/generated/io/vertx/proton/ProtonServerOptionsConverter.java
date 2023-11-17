package io.vertx.proton;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Converter and mapper for {@link io.vertx.proton.ProtonServerOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.proton.ProtonServerOptions} original class using Vert.x codegen.
 */
public class ProtonServerOptionsConverter {


  private static final Base64.Decoder BASE64_DECODER = JsonUtil.BASE64_DECODER;
  private static final Base64.Encoder BASE64_ENCODER = JsonUtil.BASE64_ENCODER;

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, ProtonServerOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "openSslEngineOptions":
          if (member.getValue() instanceof JsonObject) {
            obj.setOpenSslEngineOptions(new io.vertx.core.net.OpenSSLEngineOptions((io.vertx.core.json.JsonObject)member.getValue()));
          }
          break;
        case "heartbeat":
          if (member.getValue() instanceof Number) {
            obj.setHeartbeat(((Number)member.getValue()).intValue());
          }
          break;
        case "maxFrameSize":
          if (member.getValue() instanceof Number) {
            obj.setMaxFrameSize(((Number)member.getValue()).intValue());
          }
          break;
      }
    }
  }

   static void toJson(ProtonServerOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(ProtonServerOptions obj, java.util.Map<String, Object> json) {
    if (obj.getOpenSslEngineOptions() != null) {
      json.put("openSslEngineOptions", obj.getOpenSslEngineOptions().toJson());
    }
    json.put("heartbeat", obj.getHeartbeat());
    json.put("maxFrameSize", obj.getMaxFrameSize());
  }
}
