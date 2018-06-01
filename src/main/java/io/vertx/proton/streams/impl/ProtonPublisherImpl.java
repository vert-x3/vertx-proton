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

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.Released;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.amqp.messaging.TerminusExpiryPolicy;
import org.apache.qpid.proton.amqp.transport.Source;
import org.apache.qpid.proton.amqp.transport.Target;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.vertx.core.impl.ContextInternal;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.proton.ProtonLinkOptions;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.impl.ProtonConnectionImpl;
import io.vertx.proton.streams.Delivery;
import io.vertx.proton.streams.ProtonPublisher;
import io.vertx.proton.streams.ProtonPublisherOptions;

public class ProtonPublisherImpl implements ProtonPublisher<Delivery> {

  private static final Logger LOG = LoggerFactory.getLogger(ProtonPublisherImpl.class);

  private static final Symbol SHARED = Symbol.valueOf("shared");
  private static final Symbol GLOBAL = Symbol.valueOf("global");

  private ContextInternal connCtx;
  private final ProtonConnectionImpl conn;
  private final AtomicBoolean subscribed = new AtomicBoolean();
  private AmqpSubscription subscription;
  private ProtonReceiver receiver;
  private boolean emitOnConnectionEnd = true;
  private int maxOutstandingCredit = 1000;

  private boolean durable;

  public ProtonPublisherImpl(String address, ProtonConnectionImpl conn, ProtonPublisherOptions options) {
    this.connCtx = conn.getContext();
    this.conn = conn;

    ProtonLinkOptions linkOptions = new ProtonLinkOptions();
    if(options.getLinkName() != null) {
      linkOptions.setLinkName(options.getLinkName());
    }

    receiver = conn.createReceiver(address, linkOptions);
    receiver.setAutoAccept(false);
    receiver.setPrefetch(0);

    if(options.getMaxOutstandingCredit() > 0) {
      maxOutstandingCredit = options.getMaxOutstandingCredit();
    }

    org.apache.qpid.proton.amqp.messaging.Source source = (org.apache.qpid.proton.amqp.messaging.Source) receiver.getSource();
    durable = options.isDurable();
    if(durable) {
      source.setExpiryPolicy(TerminusExpiryPolicy.NEVER);
      source.setDurable(TerminusDurability.UNSETTLED_STATE);
    }

    if(options.isDynamic()) {
      source.setAddress(null);
      source.setDynamic(true);
    }

    ArrayList<Symbol> capabilities = new ArrayList<>();
    if(options.isShared()) {
      capabilities.add(SHARED);
    }
    if(options.isGlobal()) {
      capabilities.add(GLOBAL);
    }

    if(!capabilities.isEmpty()) {
      Symbol[] caps = capabilities.toArray(new Symbol[capabilities.size()]);
      source.setCapabilities(caps);
    }
  }

  @Override
  public void subscribe(Subscriber<? super Delivery> subscriber) {
    LOG.trace("Subscribe called");
    Objects.requireNonNull(subscriber, "A subscriber must be supplied");

    if(subscribed.getAndSet(true)) {
      throw new IllegalStateException("Only a single susbcriber supported, and subscribe already called.");
    }

    subscription = new AmqpSubscription(subscriber);

    connCtx.runOnContext(x-> {
      conn.addEndHandler(v -> {
        if(emitOnConnectionEnd) {
          subscription.indicateError(new Exception("Connection closed: " + conn.getContainer()));
        }
      });

      receiver.closeHandler(res-> {
        subscription.indicateError(new Exception("Link closed unexpectedly"));
        receiver.close();
      });

      receiver.detachHandler(res-> {
        subscription.indicateError(new Exception("Link detached unexpectedly"));
        receiver.detach();
      });

      receiver.openHandler(res -> {
        subscription.indicateSubscribed();
      });

      receiver.handler((delivery, message) -> {
        Delivery envelope = new DeliveryImpl(message, delivery, connCtx);
        if(!subscription.onNextWrapper(envelope)){
          delivery.disposition(Released.getInstance(), true);
        }
      });

      receiver.open();
    });
  }

  // ==================================================

  public class AmqpSubscription implements Subscription {

    private Subscriber<? super Delivery> subcriber;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean completed = new AtomicBoolean();
    private long outstandingRequests = 0;

    public AmqpSubscription(Subscriber<? super Delivery> sub) {
      this.subcriber = sub;
    }

    private boolean onNextWrapper(Delivery next) {
      if(!completed.get() && !cancelled.get()){
        LOG.trace("calling onNext");
        subcriber.onNext(next);

        // Now top up credits if still needed
        outstandingRequests = outstandingRequests - 1;

        if(!cancelled.get()) {
          int currentCredit = receiver.getCredit();
          if(currentCredit < (maxOutstandingCredit * 0.5) && outstandingRequests > currentCredit) {
            int creditLimit = (int) Math.min(outstandingRequests, maxOutstandingCredit);

            int credits = creditLimit - currentCredit;
            if(credits > 0) {
              LOG.trace("Updating credit for outstanding requests: {0}", credits);
              flowCreditIfNeeded(credits);
            }
          }
        }

        return true;
      } else {
        LOG.trace("skipped calling onNext, already completed or cancelled");
        return false;
      }
    }

    @Override
    public void request(long n) {
      LOG.trace("Request called: {0}", n);
      if(n <= 0 && !cancelled.get()) {
        LOG.warn("non-positive subscription request, requests must be > 0");
        connCtx.runOnContext(x -> {
          indicateError(new IllegalArgumentException("non-positive subscription request, requests must be > 0"));
        });
      } else if(!cancelled.get()) {
        connCtx.runOnContext(x -> {
          LOG.trace("Processing request: {0}", n);

          if(n == Long.MAX_VALUE) {
            outstandingRequests = Long.MAX_VALUE;
          } else {
            try {
              outstandingRequests = Math.addExact(n, outstandingRequests);
            } catch (ArithmeticException ae) {
              outstandingRequests = Long.MAX_VALUE;
            }
          }

          if(cancelled.get()) {
            LOG.trace("Not sending more credit, subscription cancelled since request was originally scheduled");
            return;
          }

          flowCreditIfNeeded(n);
        });
      }
    }

    private void flowCreditIfNeeded(long n) {
      int currentCredit = receiver.getCredit();
      if(currentCredit < maxOutstandingCredit) {
        int limit = maxOutstandingCredit - currentCredit;
        int addedCredit  = (int) Math.min(n, limit);

        if(addedCredit > 0) {
          if(!completed.get()) {
            LOG.trace("Flowing additional credits : {0}", addedCredit);
            receiver.flow(addedCredit);
          } else {
            LOG.trace("Skipping flowing additional credits as already completed: {0}", addedCredit);
          }
        }
      }
    }

    @Override
    public void cancel() {
      LOG.trace("Cancel called");
      if(!cancelled.getAndSet(true)) {
        LOG.trace("Cancellation scheduled");
        connCtx.runOnContext(x -> {
          LOG.trace("Cancelling");
          receiver.closeHandler(y -> {
            indicateCompletion();
            receiver.close();
          });
          receiver.detachHandler(y -> {
            indicateCompletion();
            receiver.detach();
          });

          if(durable) {
            receiver.detach();
          } else {
            receiver.close();
          }
        });
      } else {
        LOG.trace("Cancel no-op, already called.");
      }
    }

    private void indicateError(Throwable t) {
      if(!completed.getAndSet(true)){
        Subscriber<?> sub = subcriber;
        subcriber = null;
        if(sub != null && !cancelled.get()) {
          LOG.trace("Indicating error");
          sub.onError(t);
        } else {
          LOG.trace("Skipping error indication, no sub or already cancelled");
        }
      }
      else {
        LOG.trace("indicateError no-op, already completed");
      }
    }

    private void indicateSubscribed() {
      if(!completed.get()){
        LOG.trace("Indicating subscribed");
        if(subcriber != null) {
          subcriber.onSubscribe(this);
        }
      } else {
        LOG.trace("indicateSubscribed no-op, already completed");
      }
    }

    private void indicateCompletion() {
      if(!completed.getAndSet(true)){
        Subscriber<?> sub = subcriber;
        subcriber = null;

        boolean canned = cancelled.get();
        if(sub != null && ((outstandingRequests > 0  && canned) || !canned)) {
          LOG.trace("Indicating completion");
          sub.onComplete();
        } else {
          LOG.trace("Skipping completion indication");
        }
      } else {
        LOG.trace("indicateCompletion no-op, already completed");
      }
    }
  }

  public boolean isEmitOnConnectionEnd() {
    return emitOnConnectionEnd;
  }

  public void setEmitOnConnectionEnd(boolean emitOnConnectionEnd) {
    this.emitOnConnectionEnd = emitOnConnectionEnd;
  }

  public ProtonReceiver getLink() {
    return receiver;
  }

  // ==================================================

  @Override
  public ProtonPublisher<Delivery> setSource(Source source) {
    receiver.setSource(source);
    return this;
  }

  @Override
  public Source getSource() {
    return receiver.getSource();
  }

  @Override
  public ProtonPublisher<Delivery> setTarget(Target target) {
    receiver.setTarget(target);
    return this;
  }

  @Override
  public Target getTarget() {
    return receiver.getTarget();
  }

  @Override
  public Source getRemoteSource() {
    return receiver.getRemoteSource();
  }

  @Override
  public Target getRemoteTarget() {
    return receiver.getRemoteTarget();
  }

  @Override
  public String getRemoteAddress() {
    Source remoteSource = getRemoteSource();

    return remoteSource == null ? null : remoteSource.getAddress();
  }
}