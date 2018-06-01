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
import org.reactivestreams.Subscription;

import io.vertx.proton.ProtonSender;
import io.vertx.proton.streams.ProtonSubscriber;
import io.vertx.proton.streams.Tracker;

public class ProtonSubscriberWrapperImpl implements ProtonSubscriber<Message> {

  private final ProtonSubscriberImpl delegate;

  public ProtonSubscriberWrapperImpl(ProtonSubscriberImpl delegate) {
    this.delegate = delegate;
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    delegate.onSubscribe(subscription);
  }

  @Override
  public void onNext(Message m) {
    Objects.requireNonNull(m, "An element must be supplied when calling onNext");

    Tracker s = Tracker.create(m, null);

    delegate.onNext(s);
  }

  @Override
  public void onError(Throwable t) {
    delegate.onError(t);
  }

  @Override
  public void onComplete() {
    delegate.onComplete();
  }

  public boolean isEmitOnConnectionEnd() {
    return delegate.isEmitOnConnectionEnd();
  }

  public void setEmitOnConnectionEnd(boolean emitOnConnectionEnd) {
    delegate.setEmitOnConnectionEnd(emitOnConnectionEnd);
  }

  public ProtonSender getLink() {
    return delegate.getLink();
  }

  @Override
  public ProtonSubscriber<Message> setSource(Source source) {
    delegate.setSource(source);
    return this;
  }

  @Override
  public Source getSource() {
    return delegate.getSource();
  }

  @Override
  public ProtonSubscriber<Message> setTarget(Target target) {
    delegate.setTarget(target);
    return this;
  }

  @Override
  public Target getTarget() {
    return delegate.getTarget();
  }

  public Source getRemoteSource() {
    return delegate.getRemoteSource();
  }

  public Target getRemoteTarget() {
    return delegate.getRemoteTarget();
  }
}
