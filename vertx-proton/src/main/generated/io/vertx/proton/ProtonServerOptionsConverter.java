package io.vertx.proton;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter and mapper for {@link io.vertx.proton.ProtonServerOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.proton.ProtonServerOptions} original class using Vert.x codegen.
 */
public class ProtonServerOptionsConverter {

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
        case "maxTransfersPerDelivery":
          if (member.getValue() instanceof Number) {
            obj.setMaxTransfersPerDelivery(((Number)member.getValue()).intValue());
          }
          break;
        case "messageMaxDecodeDepth":
          if (member.getValue() instanceof Number) {
            obj.setMessageMaxDecodeDepth(((Number)member.getValue()).intValue());
          }
          break;
        case "messageZeroWidthArrayElementLimit":
          if (member.getValue() instanceof Number) {
            obj.setMessageZeroWidthArrayElementLimit(((Number)member.getValue()).intValue());
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
    json.put("maxTransfersPerDelivery", obj.getMaxTransfersPerDelivery());
    json.put("messageMaxDecodeDepth", obj.getMessageMaxDecodeDepth());
    json.put("messageZeroWidthArrayElementLimit", obj.getMessageZeroWidthArrayElementLimit());
  }
}
