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
package io.vertx.proton.impl;

import io.vertx.core.Handler;
import io.vertx.proton.ProtonDelivery;

import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Record;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonDeliveryImpl implements ProtonDelivery {

  private final Delivery delivery;
  private Handler<ProtonDelivery> handler;
  private boolean autoSettle;

  public ProtonDeliveryImpl(Delivery delivery) {
    this.delivery = delivery;
    delivery.setContext(this);
  }

  public ProtonLinkImpl getLink() {
    return (ProtonLinkImpl) this.delivery.getLink().getContext();
  }

  public void clear() {
    delivery.clear();
  }

  public DeliveryState getLocalState() {
    return delivery.getLocalState();
  }

  @Override
  public boolean isSettled() {
    return delivery.isSettled();
  }

  @Override
  public boolean remotelySettled() {
    return delivery.remotelySettled();
  }

  @Override
  public Record attachments() {
    return delivery.attachments();
  }

  @Override
  public byte[] getTag() {
    return delivery.getTag();
  }

  public void setDefaultDeliveryState(DeliveryState state) {
    delivery.setDefaultDeliveryState(state);
  }

  public DeliveryState getDefaultDeliveryState() {
    return delivery.getDefaultDeliveryState();
  }

  public boolean isReadable() {
    return delivery.isReadable();
  }

  public boolean isUpdated() {
    return delivery.isUpdated();
  }

  public boolean isWritable() {
    return delivery.isWritable();
  }

  public int pending() {
    return delivery.pending();
  }

  public boolean isPartial() {
    return delivery.isPartial();
  }

  public DeliveryState getRemoteState() {
    return delivery.getRemoteState();
  }

  @Override
  public int getMessageFormat() {
    return delivery.getMessageFormat();
  }

  public boolean isBuffered() {
    return delivery.isBuffered();
  }

  @Override
  public ProtonDelivery disposition(DeliveryState state, boolean settle) {
    if(delivery.isSettled()) {
      return this;
    }

    delivery.disposition(state);
    if (settle) {
      settle();
    } else {
      flushConnection();
    }

    return this;
  }

  @Override
  public ProtonDelivery settle() {
    delivery.settle();
    flushConnection();

    return this;
  }

  private void flushConnection() {
    getLinkImpl().getSession().getConnectionImpl().flush();
  }

  public ProtonDelivery handler(Handler<ProtonDelivery> handler) {
    this.handler = handler;
    if (delivery.isSettled()) {
      fireUpdate();
    }
    return this;
  }

  boolean isAutoSettle() {
    return autoSettle;
  }

  void setAutoSettle(boolean autoSettle) {
    this.autoSettle = autoSettle;
  }

  void fireUpdate() {
    if (this.handler != null) {
      this.handler.handle(this);
    }

    if (autoSettle && delivery.remotelySettled() && !delivery.isSettled()) {
      settle();
    }
  }

  public ProtonLinkImpl getLinkImpl() {
    return (ProtonLinkImpl) delivery.getLink().getContext();
  }

}
