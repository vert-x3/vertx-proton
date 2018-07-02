package io.vertx.proton;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.proton.ProtonClientOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.proton.ProtonClientOptions} original class using Vert.x codegen.
 */
 class ProtonClientOptionsConverter {

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, ProtonClientOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "enabledSaslMechanisms":
          if (member.getValue() instanceof JsonArray) {
            ((Iterable<Object>)member.getValue()).forEach( item -> {
              if (item instanceof String)
                obj.addEnabledSaslMechanism((String)item);
            });
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
        case "sniServerName":
          if (member.getValue() instanceof String) {
            obj.setSniServerName((String)member.getValue());
          }
          break;
        case "virtualHost":
          if (member.getValue() instanceof String) {
            obj.setVirtualHost((String)member.getValue());
          }
          break;
      }
    }
  }

   static void toJson(ProtonClientOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(ProtonClientOptions obj, java.util.Map<String, Object> json) {
    if (obj.getEnabledSaslMechanisms() != null) {
      JsonArray array = new JsonArray();
      obj.getEnabledSaslMechanisms().forEach(item -> array.add(item));
      json.put("enabledSaslMechanisms", array);
    }
    json.put("heartbeat", obj.getHeartbeat());
    json.put("maxFrameSize", obj.getMaxFrameSize());
    if (obj.getSniServerName() != null) {
      json.put("sniServerName", obj.getSniServerName());
    }
    if (obj.getVirtualHost() != null) {
      json.put("virtualHost", obj.getVirtualHost());
    }
  }
}
