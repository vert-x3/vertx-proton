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
 * Converter for {@link io.vertx.proton.ProtonTransportOptions}.
 *
 * NOTE: This class has been automatically generated from the {@link io.vertx.proton.ProtonTransportOptions} original class using Vert.x codegen.
 */
 class ProtonTransportOptionsConverter {

   static void fromJson(JsonObject json, ProtonTransportOptions obj) {
    if (json.getValue("heartbeat") instanceof Number) {
      obj.setHeartbeat(((Number)json.getValue("heartbeat")).intValue());
    }
    if (json.getValue("maxFrameSize") instanceof Number) {
      obj.setMaxFrameSize(((Number)json.getValue("maxFrameSize")).intValue());
    }
  }

   static void toJson(ProtonTransportOptions obj, JsonObject json) {
    json.put("heartbeat", obj.getHeartbeat());
    json.put("maxFrameSize", obj.getMaxFrameSize());
  }
}