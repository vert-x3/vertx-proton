/*
* Copyright 2016, 2017 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package io.vertx.proton;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.json.JsonObject;

/**
 * Options for configuring transport layer
 */
@DataObject(generateConverter = true, publicConverter = false)
public class ProtonTransportOptions {

  private int heartbeat;
  private int maxFrameSize;

  public ProtonTransportOptions() {
  }

  /**
   * Create options from JSON
   *
   * @param json  the JSON
   */
  public ProtonTransportOptions(JsonObject json) {
    ProtonTransportOptionsConverter.fromJson(json, this);
  }

  /**
   * Convert to JSON
   *
   * @return the JSON
   */
  public JsonObject toJson() {
    JsonObject json = new JsonObject();
    ProtonTransportOptionsConverter.toJson(this, json);
    return json;
  }

  /**
   * Set the heart beat as maximum delay between sending frames for the remote peers.
   * If no frames are received within 2 * heart beat, the connection is closed
   *
   * @param heartbeat The maximum delay in milliseconds.
   * @return current ProtonTransportOptions instance.
   */
  public ProtonTransportOptions setHeartbeat(int heartbeat) {
    this.heartbeat = heartbeat;
    return this;
  }

  /**
   * Returns the heart beat as maximum delay between sending frames for the remote peers.
   *
   * @return The maximum delay in milliseconds.
   */
  public int getHeartbeat() {
    return this.heartbeat;
  }

  /**
   * Sets the maximum frame size to announce in the AMQP <em>OPEN</em> frame.
   * <p>
   * If this property is not set explicitly, a reasonable default value is used.
   * <p>
   * Setting this property to a negative value will result in no maximum frame size being announced at all.
   *
   * @param maxFrameSize The frame size in bytes.
   * @return This instance for setter chaining.
   */
  public ProtonTransportOptions setMaxFrameSize(int maxFrameSize) {
    if (maxFrameSize < 0) {
      this.maxFrameSize = -1;
    } else {
      this.maxFrameSize = maxFrameSize;
    }
    return this;
  }

  /**
   * Gets the maximum frame size to announce in the AMQP <em>OPEN</em> frame.
   * <p>
   * If this property is not set explicitly, a reasonable default value is used.
   *
   * @return The frame size in bytes or -1 if no limit is set.
   */
  public int getMaxFrameSize() {
    return maxFrameSize;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + heartbeat;
    result = prime * result + maxFrameSize;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()){
      return false;
    }

    ProtonTransportOptions other = (ProtonTransportOptions) obj;
    if (this.heartbeat != other.heartbeat) {
      return false;
    }
    if (this.maxFrameSize != other.maxFrameSize) {
      return false;
    }

    return true;
  }
}
