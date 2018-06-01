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

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.impl.ContextInternal;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.streams.Delivery;

public class DeliveryImpl implements Delivery {
  private static final Accepted ACCEPTED = Accepted.getInstance();

  private final Message message;
  private final ProtonDelivery delivery;
  private ContextInternal ctx;

  DeliveryImpl(Message message, ProtonDelivery delivery, ContextInternal ctx) {
    this.message = message;
    this.delivery = delivery;
    this.ctx = ctx;
  }

  @Override
  public Message message() {
      return message;
  }

  public ProtonDelivery delivery() {
    return delivery;
  }

  @Override
  public Delivery accept() {
    ackOnContext(v -> {
      delivery.disposition(ACCEPTED, true);
    });

    return this;
  }

  @Override
  public Delivery disposition(final DeliveryState state, final boolean settle) {
    ackOnContext(v -> {
      delivery.disposition(state, settle);
    });

    return this;
  }

  private void ackOnContext(Handler<Void> action) {
    if (onContextEventLoop()) {
      action.handle(null);
    } else {
      ctx.runOnContext(action);
    }
  }

  public boolean onContextEventLoop() {
    return ctx.nettyEventLoop().inEventLoop();
  }

  public Context getCtx() {
    return ctx;
  }
}