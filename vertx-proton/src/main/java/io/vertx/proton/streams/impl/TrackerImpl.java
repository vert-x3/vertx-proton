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
package io.vertx.proton.streams.impl;

import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.message.Message;

import io.vertx.core.Handler;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.impl.ProtonDeliveryImpl;
import io.vertx.proton.streams.Tracker;

public class TrackerImpl implements Tracker {
  private final Message message;
  private volatile ProtonDeliveryImpl delivery;
  private volatile Handler<Tracker> onUpdated;

  public TrackerImpl(Message message, Handler<Tracker> onUpdated) {
    this.message = message;
    this.onUpdated = onUpdated;
  }

  @Override
  public Message message() {
      return message;
  }

  public ProtonDelivery delivery() {
    return delivery;
  }

  public void setDelivery(ProtonDeliveryImpl delivery) {
    this.delivery = delivery;
  }

  @Override
  public boolean isAccepted() {
    return delivery.getRemoteState() instanceof Accepted;
  }

  @Override
  public DeliveryState getRemoteState() {
    return delivery.getRemoteState();
  }

  @Override
  public boolean isRemotelySettled() {
    return delivery.remotelySettled();
  }

  public Handler<Tracker> handler() {
    return onUpdated;
  }

  public void setHandler(Handler<Tracker> onUpdated) {
    this.onUpdated = onUpdated;
  }

}