package io.vertx.proton;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import io.vertx.core.spi.json.JsonCodec;

/**
 * Converter and Codec for {@link io.vertx.proton.ProtonTransportOptions}.
 * NOTE: This class has been automatically generated from the {@link io.vertx.proton.ProtonTransportOptions} original class using Vert.x codegen.
 */
public class ProtonTransportOptionsConverter implements JsonCodec<ProtonTransportOptions, JsonObject> {

  public static final ProtonTransportOptionsConverter INSTANCE = new ProtonTransportOptionsConverter();

  @Override public JsonObject encode(ProtonTransportOptions value) { return (value != null) ? value.toJson() : null; }

  @Override public ProtonTransportOptions decode(JsonObject value) { return (value != null) ? new ProtonTransportOptions(value) : null; }

  @Override public Class<ProtonTransportOptions> getTargetClass() { return ProtonTransportOptions.class; }

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
