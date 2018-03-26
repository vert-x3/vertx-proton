/*
 * Copyright (c) 2014 Red Hat, Inc. and others
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.proton;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link io.vertx.proton.ProtonClientOptions}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.proton.ProtonClientOptions} original class using Vert.x codegen.
 */
 class ProtonClientOptionsConverter {

   static void fromJson(JsonObject json, ProtonClientOptions obj) {
    if (json.getValue("enabledSaslMechanisms") instanceof JsonArray) {
      json.getJsonArray("enabledSaslMechanisms").forEach(item -> {
        if (item instanceof String)
          obj.addEnabledSaslMechanism((String)item);
      });
    }
    if (json.getValue("heartbeat") instanceof Number) {
      obj.setHeartbeat(((Number)json.getValue("heartbeat")).intValue());
    }
    if (json.getValue("maxFrameSize") instanceof Number) {
      obj.setMaxFrameSize(((Number)json.getValue("maxFrameSize")).intValue());
    }
    if (json.getValue("sniServerName") instanceof String) {
      obj.setSniServerName((String)json.getValue("sniServerName"));
    }
    if (json.getValue("virtualHost") instanceof String) {
      obj.setVirtualHost((String)json.getValue("virtualHost"));
    }
  }

   static void toJson(ProtonClientOptions obj, JsonObject json) {
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