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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.qpid.proton.amqp.transport.Source;
import org.apache.qpid.proton.amqp.transport.Target;
import org.reactivestreams.Subscription;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonSender;
import io.vertx.proton.impl.ProtonConnectionImpl;
import io.vertx.proton.impl.ProtonDeliveryImpl;
import io.vertx.proton.streams.ProtonSubscriber;
import io.vertx.proton.streams.ProtonSubscriberOptions;
import io.vertx.proton.streams.Tracker;

public class ProtonSubscriberImpl implements ProtonSubscriber<Tracker> {

  private static final Logger LOG = LoggerFactory.getLogger(ProtonSubscriberImpl.class);

  private Subscription sub;
  private Context connCtx;
  private ProtonConnectionImpl conn;
  private ProtonSender sender;
  private final AtomicBoolean subscribed = new AtomicBoolean();
  private final AtomicBoolean completed = new AtomicBoolean();
  private final AtomicBoolean cancelledSub = new AtomicBoolean();
  private boolean emitOnConnectionEnd = true;
  private long outstandingRequests = 0;

  public ProtonSubscriberImpl(String address, ProtonConnectionImpl conn) {
    this(address, conn, new ProtonSubscriberOptions());
  }

  public ProtonSubscriberImpl(String address, ProtonConnectionImpl conn, ProtonSubscriberOptions options) {
    this.connCtx = conn.getContext();
    this.conn = conn;

    ProtonLinkOptions linkOptions = new ProtonLinkOptions();
    if(options.getLinkName() != null) {
      linkOptions.setLinkName(options.getLinkName());
    }

    sender = conn.createSender(address, linkOptions);
    sender.setAutoDrained(false);
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    Objects.requireNonNull(subscription, "A subscription must be supplied");

    if(subscribed.getAndSet(true)) {
      LOG.trace("Only a single Subscription is supported and already subscribed, cancelling new subscriber.");
      subscription.cancel();
      return;
    }

    this.sub = subscription;

    connCtx.runOnContext(x-> {
      conn.addEndHandler(v -> {
        if(emitOnConnectionEnd) {
          cancelSub();
        }
      });

      sender.sendQueueDrainHandler(sender -> {
        if(!completed.get() && !cancelledSub.get()) {
          long credit = sender.getCredit();
          long newRequests = credit - outstandingRequests;

          if(newRequests > 0) {
            outstandingRequests += newRequests;
            sub.request(newRequests);
          }
        }
      });

      sender.detachHandler(res-> {
        cancelSub();
        sender.detach();
      });

      sender.closeHandler(res-> {
        cancelSub();
        sender.close();
      });

      sender.openHandler(res -> {
        LOG.trace("Attach received");
      });

      sender.open();
    });
  }

  private void cancelSub() {
    if(!cancelledSub.getAndSet(true)) {
      sub.cancel();
    }
  }

  @Override
  public void onNext(Tracker tracker) {
    Objects.requireNonNull(tracker, "An element must be supplied when calling onNext");

    if(!completed.get()) {
      connCtx.runOnContext(x-> {
        outstandingRequests--;

        TrackerImpl env = (TrackerImpl) tracker;

        ProtonDelivery delivery = sender.send(tracker.message(), d -> {
          Handler<Tracker> h = env.handler();
          if(h != null) {
            h.handle(env);
          }
        });
        env.setDelivery((ProtonDeliveryImpl) delivery);
      });
    }
  }

  @Override
  public void onError(Throwable t) {
    Objects.requireNonNull(t, "An error must be supplied when calling onError");

    if(!completed.getAndSet(true)) {
      connCtx.runOnContext(x-> {
        // clear the handlers, we are done
        sender.sendQueueDrainHandler(null);
        sender.detachHandler(null);
        sender.closeHandler(null);

        sender.close();
      });
    }
  }

  @Override
  public void onComplete() {
    if(!completed.getAndSet(true)) {
      connCtx.runOnContext(x-> {
        // clear the handlers, we are done
        sender.sendQueueDrainHandler(null);
        sender.detachHandler(null);
        sender.closeHandler(null);

        sender.close();
      });
    }
  }

  @Override
  public ProtonSubscriber<Tracker> setSource(Source source) {
    sender.setSource(source);
    return this;
  }

  @Override
  public Source getSource() {
    return sender.getSource();
  }

  @Override
  public ProtonSubscriber<Tracker> setTarget(Target target) {
    sender.setTarget(target);
    return this;
  }

  @Override
  public Target getTarget() {
    return sender.getTarget();
  }

  public Source getRemoteSource() {
    return sender.getRemoteSource();
  }

  public Target getRemoteTarget() {
    return sender.getRemoteTarget();
  }

  public boolean isEmitOnConnectionEnd() {
    return emitOnConnectionEnd;
  }

  public void setEmitOnConnectionEnd(boolean emitOnConnectionEnd) {
    this.emitOnConnectionEnd = emitOnConnectionEnd;
  }

  public ProtonSender getLink() {
    return sender;
  }
}
