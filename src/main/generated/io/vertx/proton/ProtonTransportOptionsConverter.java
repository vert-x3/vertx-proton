package io.vertx.proton;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.impl.JsonUtil;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Converter and mapper for {@link io.vertx.proton.ProtonTransportOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.proton.ProtonTransportOptions} original class using Vert.x codegen.
 */
public class ProtonTransportOptionsConverter {


   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, ProtonTransportOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
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

   static void toJson(ProtonTransportOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(ProtonTransportOptions obj, java.util.Map<String, Object> json) {
    json.put("heartbeat", obj.getHeartbeat());
    json.put("maxFrameSize", obj.getMaxFrameSize());
  }
}
