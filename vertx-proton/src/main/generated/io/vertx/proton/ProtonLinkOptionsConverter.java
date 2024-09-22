package io.vertx.proton;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * Converter and mapper for {@link io.vertx.proton.ProtonLinkOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.proton.ProtonLinkOptions} original class using Vert.x codegen.
 */
public class ProtonLinkOptionsConverter {

  private static final Base64.Decoder BASE64_DECODER = Base64.getUrlDecoder();
  private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();

   static void fromJson(Iterable<java.util.Map.Entry<String, Object>> json, ProtonLinkOptions obj) {
    for (java.util.Map.Entry<String, Object> member : json) {
      switch (member.getKey()) {
        case "linkName":
          if (member.getValue() instanceof String) {
            obj.setLinkName((String)member.getValue());
          }
          break;
        case "dynamic":
          if (member.getValue() instanceof Boolean) {
            obj.setDynamic((Boolean)member.getValue());
          }
          break;
      }
    }
  }

   static void toJson(ProtonLinkOptions obj, JsonObject json) {
    toJson(obj, json.getMap());
  }

   static void toJson(ProtonLinkOptions obj, java.util.Map<String, Object> json) {
    if (obj.getLinkName() != null) {
      json.put("linkName", obj.getLinkName());
    }
    json.put("dynamic", obj.isDynamic());
  }
}
