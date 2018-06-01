/*
* Copyright 2018 the original author or authors.
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
package io.vertx.proton.streams;

import java.util.Objects;

/**
 * Options for configuring Publisher attributes.
 */
public class ProtonPublisherOptions {
  private String linkName;
  private boolean durable;
  private boolean shared;
  private boolean global;
  private boolean dynamic;
  private int maxOutstandingCredit;

  public ProtonPublisherOptions() {
  }

  /**
   * Sets the link name to be used for the subscription.
   *
   * @param linkName the name to use
   * @return the options
   */
  public ProtonPublisherOptions setLinkName(String linkName) {
    this.linkName = linkName;
    return this;
  }

  public String getLinkName() {
    return linkName;
  }

  /**
   * Sets whether the link to be used for the subscription should
   * have 'source' terminus details indicating it is durable, that
   * is a terminus-expiry-policy of "never" and terminus-durability
   * of 2/unsettled-state, and that the link should detach rather than
   * close when cancel is called on the subscription.
   *
   * @param durable true if the subscription should be considered durable
   * @return the options
   */
  public ProtonPublisherOptions setDurable(boolean durable) {
    this.durable = durable;
    return this;
  }

  public boolean isDurable() {
    return durable;
  }

  /**
   * Sets whether the link to be used for the subscription should
   * have 'source' terminus capability indicating it is 'shared'.
   *
   * @param shared true if the subscription should be considered shared
   * @return the options
   */
  public ProtonPublisherOptions setShared(boolean shared) {
    this.shared = shared;
    return this;
  }

  public boolean isShared() {
    return shared;
  }

  /**
   * Sets whether the link to be used for a shared subscription should
   * also have 'source' terminus capability indicating it is 'global',
   * that is its subscription can be shared across connections
   * regardless of their container-id values.
   *
   * @param global true if the subscription should be considered global
   * @return the options
   */
  public ProtonPublisherOptions setGlobal(boolean global) {
    this.global = global;
    return this;
  }

  public boolean isGlobal() {
    return global;
  }

  /**
   * Sets whether the link to be used for the subscription should indicate
   * a 'dynamic' source terminus, requesting the server peer names it with
   * a dynamic address. The remote address can then be inspected using
   * {@link ProtonPublisher#getRemoteAddress()} (or inspecting the remote
   * source details directly) when the onSubscribe() handler is fired.
   *
   * @param dynamic true if the link should request a dynamic source address
   * @return the options
   */
  public ProtonPublisherOptions setDynamic(boolean dynamic) {
    this.dynamic = dynamic;
    return this;
  }

  public boolean isDynamic() {
    return dynamic;
  }

  /**
   * Sets the maximum credit the consumer link will leave outstanding at a time.
   * If the total unfilled subscription requests remains below this level, the
   * consumer credit issued will match the unfilled requests. If the requests
   * exceeds this value, the consumer link will cap it and refresh it once the
   * level drops below a threshold or more requests are made. If not set a
   * reasonable default is used.
   *
   * @param maxOutstandingCredit the limit on outstanding consumer credit
   * @return the options
   */
  public ProtonPublisherOptions setMaxOutstandingCredit(int maxOutstandingCredit) {
    this.maxOutstandingCredit = maxOutstandingCredit;

    return this;
  }

  public int getMaxOutstandingCredit() {
    return maxOutstandingCredit;
  }

  @Override
  public int hashCode() {
    final int prime = 31;

    int result = 1;
    result = prime * result + Objects.hashCode(linkName);
    result = prime * result + (durable ? 1231 : 1237);
    result = prime * result + (shared ? 1231 : 1237);
    result = prime * result + (global ? 1231 : 1237);
    result = prime * result + (dynamic ? 1231 : 1237);
    result = prime * result + maxOutstandingCredit;

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

    ProtonPublisherOptions other = (ProtonPublisherOptions) obj;
    if (!Objects.equals(linkName, other.linkName)){
      return false;
    }

    if (durable != other.durable){
      return false;
    }

    if (shared != other.shared){
      return false;
    }

    if (global != other.global){
      return false;
    }

    if (dynamic != other.dynamic){
      return false;
    }

    if (maxOutstandingCredit != other.maxOutstandingCredit){
      return false;
    }

    return true;
  }
}
