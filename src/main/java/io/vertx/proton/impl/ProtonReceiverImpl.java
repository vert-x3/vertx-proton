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
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.proton.ProtonMessageHandler;
import io.vertx.proton.ProtonReceiver;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.message.Message;

import static io.vertx.proton.ProtonHelper.accepted;

import java.io.ByteArrayOutputStream;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonReceiverImpl extends ProtonLinkImpl<ProtonReceiver> implements ProtonReceiver {
  private ProtonMessageHandler handler;
  private int prefetch = 1000;
  private Handler<AsyncResult<Void>> drainCompleteHandler;
  private Long drainTimeoutTaskId = null;

  ProtonReceiverImpl(Receiver receiver) {
    super(receiver);
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

  private void flushConnection() {
    getSession().getConnectionImpl().flush();
  }

  /////////////////////////////////////////////////////////////////////////////
  //
  // Implementation details hidden from public api.
  //
  /////////////////////////////////////////////////////////////////////////////
  protected ByteArrayOutputStream current = new ByteArrayOutputStream();
  byte[] buffer = new byte[1024];
  private boolean autoAccept = true;

  void onDelivery() {
    if (this.handler == null) {
      return;
    }

    Receiver receiver = getReceiver();
    Delivery delivery = receiver.current();

    if (delivery != null) {
      int count;
      while ((count = receiver.recv(buffer, 0, buffer.length)) > 0) {
        current.write(buffer, 0, count);
      }

      if (delivery.isPartial()) {
        // Delivery is not yet completely received,
        // return and allow further frames to arrive.
        return;
      }

      byte[] data = current.toByteArray();
      current.reset();

      Message msg = Proton.message();
      msg.decode(data, 0, data.length);

      receiver.advance();

      ProtonDeliveryImpl delImpl = new ProtonDeliveryImpl(delivery);

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
