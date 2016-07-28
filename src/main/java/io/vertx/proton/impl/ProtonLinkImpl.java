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

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.proton.ProtonHelper;
import io.vertx.proton.ProtonLink;
import io.vertx.proton.ProtonQoS;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.amqp.transport.Source;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Record;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
abstract class ProtonLinkImpl<T extends ProtonLink<T>> implements ProtonLink<T> {

  protected final Link link;
  private Handler<AsyncResult<T>> openHandler;
  private Handler<AsyncResult<T>> closeHandler;

  ProtonLinkImpl(Link link) {
    this.link = link;
    this.link.setContext(this);

    setQoS(getRemoteQoS());
  }

  protected abstract T self();

  @Override
  public ProtonSessionImpl getSession() {
    return (ProtonSessionImpl) this.link.getSession().getContext();
  }

  @Override
  public Record attachments() {
    return link.attachments();
  }

  @Override
  public ErrorCondition getCondition() {
    return link.getCondition();
  }

  @Override
  public int getCredit() {
    return link.getCredit();
  }

  @Override
  public boolean getDrain() {
    return link.getDrain();
  }

  public EndpointState getLocalState() {
    return link.getLocalState();
  }

  public String getName() {
    return link.getName();
  }

  @Override
  public ErrorCondition getRemoteCondition() {
    return link.getRemoteCondition();
  }

  public int getRemoteCredit() {
    return link.getRemoteCredit();
  }

  public EndpointState getRemoteState() {
    return link.getRemoteState();
  }

  @Override
  public Target getRemoteTarget() {
    return link.getRemoteTarget();
  }

  @Override
  public Target getTarget() {
    return link.getTarget();
  }

  @Override
  public T setTarget(Target target) {
    link.setTarget(target);
    return self();
  }

  @Override
  public Source getRemoteSource() {
    return link.getRemoteSource();
  }

  @Override
  public Source getSource() {
    return link.getSource();
  }

  @Override
  public T setSource(Source source) {
    link.setSource(source);
    return self();
  }

  public int getUnsettled() {
    return link.getUnsettled();
  }

  @Override
  public int getQueued() {
    return link.getQueued();
  }

  public boolean advance() {
    return link.advance();
  }

  public int drained() {
    int drained = link.drained();
    getSession().getConnectionImpl().flush();
    return drained;
  }

  public boolean detached() {
    return link.detached();
  }

  public Delivery delivery(byte[] tag, int offset, int length) {
    return link.delivery(tag, offset, length);
  }

  public Delivery current() {
    return link.current();
  }

  @Override
  public T setCondition(ErrorCondition condition) {
    link.setCondition(condition);
    return self();
  }

  public Delivery delivery(byte[] tag) {
    return link.delivery(tag);
  }

  @Override
  public T open() {
    link.open();
    getSession().getConnectionImpl().flush();
    return self();
  }

  @Override
  public T close() {
    link.close();
    getSession().getConnectionImpl().flush();
    return self();
  }

  public T detach() {
    link.detach();
    getSession().getConnectionImpl().flush();
    return self();
  }

  @Override
  public T openHandler(Handler<AsyncResult<T>> openHandler) {
    this.openHandler = openHandler;
    return self();
  }

  @Override
  public T closeHandler(Handler<AsyncResult<T>> closeHandler) {
    this.closeHandler = closeHandler;
    return self();
  }

  @Override
  public boolean isOpen() {
    return getLocalState() == EndpointState.ACTIVE;
  }

  @Override
  public ProtonQoS getQoS() {
    if (link.getSenderSettleMode() == SenderSettleMode.SETTLED) {
      return ProtonQoS.AT_MOST_ONCE;
    }

    return ProtonQoS.AT_LEAST_ONCE;
  }

  @Override
  public ProtonQoS getRemoteQoS() {
    if (link.getRemoteSenderSettleMode() == SenderSettleMode.SETTLED) {
      return ProtonQoS.AT_MOST_ONCE;
    }

    return ProtonQoS.AT_LEAST_ONCE;
  }

  @Override
  public T setQoS(ProtonQoS qos) {
    if (qos == null) {
      throw new IllegalArgumentException("Value must be specified");
    }
    switch (qos) {
    case AT_MOST_ONCE:
      link.setSenderSettleMode(SenderSettleMode.SETTLED);
      link.setReceiverSettleMode(ReceiverSettleMode.FIRST);
      break;
    case AT_LEAST_ONCE:
      link.setSenderSettleMode(SenderSettleMode.UNSETTLED);
      link.setReceiverSettleMode(ReceiverSettleMode.FIRST);
      break;
    }
    return self();
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // Implementation details hidden from public api.
  //
  /////////////////////////////////////////////////////////////////////////////
  void fireRemoteOpen() {
    if (openHandler != null) {
      openHandler.handle(ProtonHelper.future(self(), getRemoteCondition()));
    }
  }

  void fireRemoteClose() {
    if (closeHandler != null) {
      closeHandler.handle(ProtonHelper.future(self(), getRemoteCondition()));
    }
  }

  abstract void handleLinkFlow();
}
