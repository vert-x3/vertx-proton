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

import org.apache.qpid.proton.amqp.transport.Source;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.message.Message;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.streams.Delivery;
import io.vertx.proton.streams.ProtonPublisher;

public class ProtonPublisherWrapperImpl implements ProtonPublisher<Message> {

  private ProtonPublisherImpl delegate;

  public ProtonPublisherWrapperImpl(ProtonPublisherImpl delegate) {
    this.delegate = delegate;
  }

  @Override
  public void subscribe(Subscriber<? super Message> subscriber) {
    Objects.requireNonNull(subscriber, "A subscriber must be supplied");
    delegate.subscribe(new AmqpSubscriberWrapperImpl(subscriber));
  }

  public boolean isEmitOnConnectionEnd() {
    return delegate.isEmitOnConnectionEnd();
  }

  public void setEmitOnConnectionEnd(boolean emitOnConnectionEnd) {
    delegate.setEmitOnConnectionEnd(emitOnConnectionEnd);
  }

  // ==================================================

  private static class AmqpSubscriberWrapperImpl implements Subscriber<Delivery> {

    private Subscriber<? super Message> delegateSub;

    public AmqpSubscriberWrapperImpl(Subscriber<? super Message> subscriber) {
      this.delegateSub = subscriber;
    }

    @Override
    public void onSubscribe(Subscription s) {
      delegateSub.onSubscribe(s);
    }

    @Override
    public void onNext(Delivery d) {
      Message m = d.message();
      delegateSub.onNext(m);
      d.accept();
    }

    @Override
    public void onError(Throwable t) {
      delegateSub.onError(t);
    }

    @Override
    public void onComplete() {
      delegateSub.onComplete();
    }
  }

  public ProtonReceiver getLink() {
    return delegate.getLink();
  }

  // ==================================================

  @Override
  public String getRemoteAddress() {
    return delegate.getRemoteAddress();
  }

  @Override
  public ProtonPublisher<Message> setSource(Source source) {
    delegate.setSource(source);
    return this;
  }

  @Override
  public Source getSource() {
    return delegate.getSource();
  }

  @Override
  public ProtonPublisher<Message> setTarget(Target target) {
    delegate.setTarget(target);
    return this;
  }

  @Override
  public Target getTarget() {
    return delegate.getTarget();
  }

  @Override
  public Source getRemoteSource() {
    return delegate.getRemoteSource();
  }

  @Override
  public Target getRemoteTarget() {
    return delegate.getRemoteTarget();
  }
}