package io.vertx.proton;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Converter and Codec for {@link io.vertx.proton.ProtonLinkOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.proton.ProtonLinkOptions} original class using Vert.x codegen.
 */
public class ProtonLinkOptionsConverter implements JsonCodec<ProtonLinkOptions, JsonObject> {

  public static final ProtonLinkOptionsConverter INSTANCE = new ProtonLinkOptionsConverter();

  @Override public JsonObject encode(ProtonLinkOptions value) { return (value != null) ? value.toJson() : null; }

  @Override public ProtonLinkOptions decode(JsonObject value) { return (value != null) ? new ProtonLinkOptions(value) : null; }

  @Override public Class<ProtonLinkOptions> getTargetClass() { return ProtonLinkOptions.class; }

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
