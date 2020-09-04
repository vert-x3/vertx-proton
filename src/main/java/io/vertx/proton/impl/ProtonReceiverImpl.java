/*
* Copyright 2016, 2020 the original author or authors.
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
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.proton.ProtonMessageHandler;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.Modified;
import org.apache.qpid.proton.amqp.messaging.Rejected;
import org.apache.qpid.proton.amqp.transport.DeliveryState;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.LinkError;
import org.apache.qpid.proton.amqp.transport.Source;
import org.apache.qpid.proton.codec.CompositeReadableBuffer;
import org.apache.qpid.proton.codec.ReadableBuffer;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.message.impl.MessageImpl;

import static io.vertx.proton.ProtonHelper.accepted;

import java.util.Optional;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonReceiverImpl extends ProtonLinkImpl<ProtonReceiver> implements ProtonReceiver {

  private static final Logger LOG = LoggerFactory.getLogger(ProtonReceiverImpl.class);

  private ProtonMessageHandler handler;
  private Handler<ProtonReceiver> maxMessageSizeExceededHandler;
  private int prefetch = 1000;
  private Handler<AsyncResult<Void>> drainCompleteHandler;
  private Long drainTimeoutTaskId = null;
  private Session session;
  private int maxFrameSize;
  private long sessionIncomingCapacity;
  private long windowFullThreshhold;

  ProtonReceiverImpl(Receiver receiver) {
    super(receiver);
    session = receiver.getSession();
    sessionIncomingCapacity = session.getIncomingCapacity();
    maxFrameSize = session.getConnection().getTransport().getMaxFrameSize();
    windowFullThreshhold = sessionIncomingCapacity - maxFrameSize;
  }

  @Override
  protected ProtonReceiverImpl self() {
    return this;
  }

  private Receiver getReceiver() {
    return (Receiver) link;
  }

  public int recv(byte[] bytes, int offset, int size) {
    return getReceiver().recv(bytes, offset, size);
  }

  @Override
  public String getRemoteAddress() {
    Source remoteSource = getRemoteSource();

    return remoteSource == null ? null : remoteSource.getAddress();
  }

  @Override
  public ProtonReceiver drain(long timeout, Handler<AsyncResult<Void>> completionHandler) {
    if (prefetch > 0) {
      throw new IllegalStateException("Manual credit management not available while prefetch is non-zero");
    }

    if (completionHandler == null) {
      throw new IllegalArgumentException("A completion handler must be provided");
    }

    if (drainCompleteHandler != null) {
      throw new IllegalStateException("A previous drain operation has not yet completed");
    }

    if ((getCredit() - getQueued()) <= 0) {
      // We have no remote credit
      if (getQueued() == 0) {
        // All the deliveries have been processed, drain is a no-op, nothing to do but complete.
        completionHandler.handle(Future.succeededFuture());
      } else {
          // There are still deliveries to process, wait for them to be.
          setDrainHandlerAndTimeoutTask(timeout, completionHandler);
      }
    } else {
      setDrainHandlerAndTimeoutTask(timeout, completionHandler);

      getReceiver().drain(0);
      flushConnection();
    }

    return this;
  }

  private void setDrainHandlerAndTimeoutTask(long delay, Handler<AsyncResult<Void>> completionHandler) {
    drainCompleteHandler = completionHandler;

    if(delay > 0) {
      Vertx vertx = Vertx.currentContext().owner();
      drainTimeoutTaskId = vertx.setTimer(delay, x -> {
        drainTimeoutTaskId = null;
        drainCompleteHandler = null;
        completionHandler.handle(Future.failedFuture("Drain attempt timed out"));
      });
    }
  }

  @Override
  public ProtonReceiver flow(int credits) throws IllegalStateException {
    flow(credits, true);
    return this;
  }

  private void flow(int credits, boolean checkPrefetch) throws IllegalStateException {
    if (checkPrefetch && prefetch > 0) {
      throw new IllegalStateException("Manual credit management not available while prefetch is non-zero");
    }

    if (drainCompleteHandler != null) {
      throw new IllegalStateException("A previous drain operation has not yet completed");
    }

    getReceiver().flow(credits);
    flushConnection();
  }

  public boolean draining() {
    return getReceiver().draining();
  }

  public ProtonReceiver setDrain(boolean drain) {
    getReceiver().setDrain(drain);
    return this;
  }

  @Override
  public ProtonReceiver handler(ProtonMessageHandler handler) {
    this.handler = handler;
    onDelivery();
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ProtonReceiver maxMessageSizeExceededHandler(Handler<ProtonReceiver> handler) {
    this.maxMessageSizeExceededHandler = handler;
    return this;
  }

  private void flushConnection() {
    getSession().getConnectionImpl().flush();
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // Implementation details hidden from public api.
  //
  /////////////////////////////////////////////////////////////////////////////

  private boolean autoAccept = true;
  private CompositeReadableBuffer splitContent;

  private boolean isMaxMessageSizeExceeded(int size) {
    return Optional.ofNullable(getMaxMessageSize())
        .map(ul -> ul.longValue() > 0 && ul.longValue() < size)
        .orElse(false);
  }

  void onDelivery() {
    if (this.handler == null) {
      return;
    }

    Receiver receiver = getReceiver();
    Delivery delivery = receiver.current();

    if (delivery != null) {

      if(delivery.isAborted()) {
        handleAborted(receiver, delivery);
        return;
      }

      if (delivery.isPartial()) {
        handlePartial(receiver, delivery);

        // Delivery is not yet completely received,
        // return and allow further frames to arrive.
        return;
      }

      // Complete prior partial content if needed, or grab it all.
      ReadableBuffer data = receiver.recv();
      if(splitContent != null) {
        data = completePartial(data);
      }
      if (isMaxMessageSizeExceeded(data.capacity())) {
        handleMaxMessageSizeExceeded();
        return;
      }
      receiver.advance();

      MessageImpl msg = (MessageImpl) Proton.message();
      ProtonDeliveryImpl delImpl = new ProtonDeliveryImpl(delivery);
      try {
        msg.decode(data);
      } catch (Throwable t) {
        LOG.debug("Unable to decode message, undeliverable", t);

        handleDecodeFailure(receiver, delImpl);
        return;
      }

      handler.handle(delImpl, msg);

      if (autoAccept && delivery.getLocalState() == null) {
        accepted(delImpl, true);
      }

      if (prefetch > 0) {
        // Replenish credit if prefetch is configured.
        // TODO: batch credit replenish, optionally flush if exceeding a given threshold?
        flow(1, false);
      } else {
        processForDrainCompletion();
      }
    }
  }

  private void handleMaxMessageSizeExceeded() {

    LOG.debug("received message exceeding configured max-message-size [" + getMaxMessageSize() + " bytes]");
    splitContent = null;

    if (maxMessageSizeExceededHandler != null) {
      maxMessageSizeExceededHandler.handle(this);
    }
    // close link if handler has not sent a detach frame already
    if (!getReceiver().detached() && isOpen()) {
      LOG.debug("closing link with error condition " + LinkError.MESSAGE_SIZE_EXCEEDED);
      setCondition(new ErrorCondition(LinkError.MESSAGE_SIZE_EXCEEDED, "max-message-size of " + getMaxMessageSize() + " bytes exceeded"));
      close();
    }
  }

  private void handleDecodeFailure(Receiver receiver, ProtonDeliveryImpl delImpl) {
    Modified modified = new Modified();
    modified.setDeliveryFailed(true);
    modified.setUndeliverableHere(true);

    delImpl.disposition(modified, true);

    if(!receiver.getDrain()) {
      flow(1, false);
    } else {
      processForDrainCompletion();
    }
  }

  private void handleAborted(Receiver receiver, Delivery delivery) {
    splitContent = null;

    receiver.advance();
    delivery.settle();

    if(!receiver.getDrain()) {
      flow(1, false);
    } else {
      processForDrainCompletion();
    }
  }

  private void handlePartial(final Receiver receiver, final Delivery delivery) {
    if (sessionIncomingCapacity <= 0 || maxFrameSize <= 0 || session.getIncomingBytes() < windowFullThreshhold) {
      // No window, or there is still capacity, so do nothing.
    } else {
      // The session window could be effectively full, we need to
      // read part of the delivery content to ensure there is
      // room made for receiving more of the delivery.
      if(delivery.available() > 0) {
        ReadableBuffer buff = receiver.recv();

        if(splitContent == null && buff instanceof CompositeReadableBuffer) {
          // Its a composite and there is no prior partial content, use it.
          splitContent = (CompositeReadableBuffer) buff;
        } else {
          int remaining = buff.remaining();
          if(remaining > 0) {
            if (splitContent == null) {
              splitContent = new CompositeReadableBuffer();
            }

            if (isMaxMessageSizeExceeded(splitContent.capacity() + remaining)) {
              handleMaxMessageSizeExceeded();
            } else {
              byte[] chunk = new byte[remaining];
              buff.get(chunk);

              splitContent.append(chunk);
            }
          }
        }
      }
    }
  }

  private ReadableBuffer completePartial(final ReadableBuffer finalContent) {
    int pending = finalContent.remaining();
    if(pending > 0) {
      byte[] chunk = new byte[pending];
      finalContent.get(chunk);

      splitContent.append(chunk);
    }

    ReadableBuffer data = splitContent;
    splitContent = null;

    return data;
  }

  @Override
  public boolean isAutoAccept() {
    return autoAccept;
  }

  @Override
  public ProtonReceiver setAutoAccept(boolean autoAccept) {
    this.autoAccept = autoAccept;
    return this;
  }

  @Override
  public ProtonReceiver setPrefetch(int messages) {
    if (messages < 0) {
      throw new IllegalArgumentException("Value must not be negative");
    }

    prefetch = messages;
    return this;
  }

  @Override
  public int getPrefetch() {
    return prefetch;
  }

  @Override
  public ProtonReceiver open() {
    super.open();
    if (prefetch > 0) {
      // Grant initial credit if prefetching.
      flow(prefetch, false);
    }

    return this;
  }

  @Override
  void handleLinkFlow(){
    processForDrainCompletion();
  }

  private void processForDrainCompletion() {
    Handler<AsyncResult<Void>> h = drainCompleteHandler;
    if(h != null && getCredit() <= 0 && getQueued() <= 0) {
      boolean timeoutTaskCleared = false;

      Long timerId = drainTimeoutTaskId;
      if(timerId != null) {
        Vertx vertx = Vertx.currentContext().owner();
        timeoutTaskCleared = vertx.cancelTimer(timerId);
      } else {
        timeoutTaskCleared = true;
      }

      drainTimeoutTaskId = null;
      drainCompleteHandler = null;

      if(timeoutTaskCleared) {
        h.handle(Future.succeededFuture());
      }
    }
  }
}
