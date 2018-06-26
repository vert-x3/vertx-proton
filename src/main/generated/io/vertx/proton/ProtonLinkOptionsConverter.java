package io.vertx.proton;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.proton.ProtonLinkOptions}.
 * NOTE: This class has been automatically generated from the {@link "io.vertx.proton.ProtonLinkOptions} original class using Vert.x codegen.
 */
 class ProtonLinkOptionsConverter {

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, ProtonLinkOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "dynamic":
          if (member.getValue() instanceof Boolean) {
            obj.setDynamic((Boolean)member.getValue());
          }
          break;
        case "linkName":
          if (member.getValue() instanceof String) {
            obj.setLinkName((String)member.getValue());
          }
          break;
      }
    }
  }

   static void toJson(ProtonLinkOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(ProtonLinkOptions obj, java.util.Map<String, Object> json) {
    json.put("dynamic", obj.isDynamic());
    if (obj.getLinkName() != null) {
      json.put("linkName", obj.getLinkName());
    }
  }
}
