/*
* Copyright 2016 the original author or authors.
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

/**
 * Options for configuring transport layer
 */
public class ProtonTransportOptions {

  private int heartbeat;

  /**
   * Set the heartbeat as maximum delay between sending frames for the remote peers.
   * If no frames are received within 2*heartbeat, the connection is closed
   *
   * @param heartbeat hearthbeat maximum delay
   * @return  current ProtonTransportOptions instance
   */
  public ProtonTransportOptions setHeartbeat(int heartbeat) {
    this.heartbeat = heartbeat;
    return this;
  }

  /**
   * Return the heartbeat as maximum delay between sending frames for the remote peers.
   *
   * @return  hearthbeat maximum delay
   */
  public int getHeartbeat() {
    return this.heartbeat;
  }

  @Override
  public int hashCode() {
    int result = this.heartbeat;

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

    return true;
  }
}
