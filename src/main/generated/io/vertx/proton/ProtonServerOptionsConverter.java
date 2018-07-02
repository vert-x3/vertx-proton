package io.vertx.proton;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.proton.ProtonServerOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.proton.ProtonServerOptions} original class using Vert.x codegen.
 */
 class ProtonServerOptionsConverter {

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, ProtonServerOptions obj) {
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

   static void toJson(ProtonServerOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(ProtonServerOptions obj, java.util.Map<String, Object> json) {
    json.put("heartbeat", obj.getHeartbeat());
    json.put("maxFrameSize", obj.getMaxFrameSize());
  }
}
