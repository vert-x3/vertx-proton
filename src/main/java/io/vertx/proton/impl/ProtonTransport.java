/*
* Copyright 2016, 2017 the original author or authors.
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

import io.netty.buffer.ByteBuf;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferInternal;
import io.vertx.core.net.impl.NetSocketInternal;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;

import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonTransportOptions;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.engine.BaseHandler;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Event.Type;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.engine.impl.TransportInternal;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ProtonTransport extends BaseHandler {
  private static final Logger LOG = LoggerFactory.getLogger(ProtonTransport.class);
  private static final int DEFAULT_MAX_FRAME_SIZE = 32 * 1024; // 32kb

  private final Connection connection;
  private final Vertx vertx;
  private final NetClient netClient;
  private final NetSocket socket;
  private final Transport transport = Proton.transport();
  private final Collector collector = Proton.collector();
  private ProtonSaslAuthenticator authenticator;

  private volatile Long idleTimeoutCheckTimerId;

  private boolean failed;

  ProtonTransport(Connection connection, Vertx vertx, NetClient netClient, NetSocket socket,
                  ProtonSaslAuthenticator authenticator, ProtonTransportOptions options) {
    this.connection = connection;
    this.vertx = vertx;
    this.netClient = netClient;
    this.socket = socket;
    int maxFrameSize = options.getMaxFrameSize() == 0 ? DEFAULT_MAX_FRAME_SIZE : options.getMaxFrameSize();
    transport.setMaxFrameSize(maxFrameSize);
    transport.setOutboundFrameSizeLimit(maxFrameSize);
    transport.setEmitFlowEventOnSend(false); // TODO: make configurable
    transport.setIdleTimeout(2 * options.getHeartbeat());
    ((TransportInternal) transport).setUseReadOnlyOutputBuffer(false);
    if (authenticator != null) {
      authenticator.init(this.socket, (ProtonConnection) this.connection.getContext(), transport);
    }
    this.authenticator = authenticator;
    transport.bind(connection);
    connection.collect(collector);
    socket.endHandler(this::handleSocketEnd);
    socket.handler(this::handleSocketBuffer);
  }

  private void handleSocketEnd(Void arg) {
    cancelIdleTimeoutCheck();
    transport.unbind();
    transport.close();
    if (this.netClient != null) {
      this.netClient.close();
    } else {
      this.socket.close();
    }
    ((ProtonConnectionImpl) this.connection.getContext()).fireDisconnect();
  }

  private void handleSocketBuffer(Buffer buff) {
    pumpInbound(buff);

    if (!failed) {
      processSaslAuthentication();
    }

    Event protonEvent = null;
    while ((protonEvent = collector.peek()) != null) {
      ProtonConnectionImpl conn = (ProtonConnectionImpl) protonEvent.getConnection().getContext();

      Type eventType = protonEvent.getType();
      if (LOG.isTraceEnabled() && !eventType.equals(Type.TRANSPORT)) {
        LOG.trace("New Proton Event: " + eventType);
      }

      switch (eventType) {
      case CONNECTION_REMOTE_OPEN: {
        conn.fireRemoteOpen();
        initiateIdleTimeoutChecks();
        break;
      }
      case CONNECTION_REMOTE_CLOSE: {
        conn.fireRemoteClose();
        break;
      }
      case SESSION_REMOTE_OPEN: {
        ProtonSessionImpl session = (ProtonSessionImpl) protonEvent.getSession().getContext();
        if (session == null) {
          conn.fireRemoteSessionOpen(protonEvent.getSession());
        } else {
          session.fireRemoteOpen();
        }
        break;
      }
      case SESSION_REMOTE_CLOSE: {
        ProtonSessionImpl session = (ProtonSessionImpl) protonEvent.getSession().getContext();
        session.fireRemoteClose();
        break;
      }
      case LINK_REMOTE_OPEN: {
        ProtonLinkImpl<?> link = (ProtonLinkImpl<?>) protonEvent.getLink().getContext();
        if (link == null) {
          conn.fireRemoteLinkOpen(protonEvent.getLink());
        } else {
          link.fireRemoteOpen();
        }
        break;
      }
      case LINK_REMOTE_DETACH: {
        ProtonLinkImpl<?> link = (ProtonLinkImpl<?>) protonEvent.getLink().getContext();
        link.fireRemoteDetach();
        break;
      }
      case LINK_REMOTE_CLOSE: {
        ProtonLinkImpl<?> link = (ProtonLinkImpl<?>) protonEvent.getLink().getContext();
        link.fireRemoteClose();
        break;
      }
      case LINK_FLOW: {
        ProtonLinkImpl<?> link = (ProtonLinkImpl<?>) protonEvent.getLink().getContext();
        link.handleLinkFlow();
        break;
      }
      case DELIVERY: {
        ProtonDeliveryImpl delivery = (ProtonDeliveryImpl) protonEvent.getDelivery().getContext();
        if (delivery != null) {
          delivery.fireUpdate();
        } else {
          ProtonReceiverImpl receiver = (ProtonReceiverImpl) protonEvent.getLink().getContext();
          receiver.onDelivery();
        }
        break;
      }
      case TRANSPORT_ERROR: {
        failed = true;
        break;
      }

      case CONNECTION_INIT:
      case CONNECTION_BOUND:
      case CONNECTION_UNBOUND:
      case CONNECTION_LOCAL_OPEN:
      case CONNECTION_LOCAL_CLOSE:
      case CONNECTION_FINAL:

      case SESSION_INIT:
      case SESSION_LOCAL_OPEN:
      case SESSION_LOCAL_CLOSE:
      case SESSION_FINAL:

      case LINK_INIT:
      case LINK_LOCAL_OPEN:
      case LINK_LOCAL_DETACH:
      case LINK_LOCAL_CLOSE:
      case LINK_FINAL:
      }
      collector.pop();
    }

    flush();

    if (failed) {
      disconnect();
    }
  }

  private void processSaslAuthentication() {
    if (authenticator == null) {
      return;
    }

    socket.pause();

    authenticator.process(complete -> {
      if(complete) {
        authenticator = null;
      }

      socket.resume();
    });
  }

  private void initiateIdleTimeoutChecks() {
    // Using nano time since it is not related to the wall clock, which may change
    long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    long deadline = transport.tick(now);
    if (deadline != 0) {
      // timer treats 0 as error, ensure value is at least 1 as there was a deadline
      long delay = Math.max(deadline - now, 1);
      if (LOG.isTraceEnabled()) {
        LOG.trace("IdleTimeoutCheck being initiated, initial delay: " + delay);
      }
      idleTimeoutCheckTimerId = vertx.setTimer(delay, new IdleTimeoutCheck());
    }
  }

  private void cancelIdleTimeoutCheck() {
    Long timerId = idleTimeoutCheckTimerId;
    if (timerId != null) {
      vertx.cancelTimer(timerId);
      idleTimeoutCheckTimerId = null;
      LOG.trace("IdleTimeoutCheck cancelled");
    }
  }

  private void pumpInbound(Buffer buffer) {
    if (failed) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("Skipping processing of data following transport error: " + buffer);
      }
      return;
    }

    // Lets push bytes from vert.x to proton engine.
    try {
      ByteBuf data = ((BufferInternal)buffer).getByteBuf();
      do {
        ByteBuffer transportBuffer = transport.tail();

        int amount = Math.min(transportBuffer.remaining(), data.readableBytes());
        transportBuffer.limit(transportBuffer.position() + amount);
        data.readBytes(transportBuffer);

        transport.process();
      } while (data.isReadable());
    } catch (Exception te) {
      failed = true;
      LOG.trace("Exception while processing transport input", te);
    }
  }

  void flush() {
    boolean done = false;
    while (!done) {
      ByteBuffer outputBuffer = transport.getOutputBuffer();
      if (outputBuffer != null && outputBuffer.hasRemaining()) {
        final NetSocketInternal internal = (NetSocketInternal) socket;
        final ByteBuf bb = internal.channelHandlerContext().alloc().directBuffer(outputBuffer.remaining());
        bb.writeBytes(outputBuffer);
        internal.writeMessage(bb);
        transport.outputConsumed();
      } else {
        done = true;
      }
    }
  }

  public void disconnect() {
    if (netClient != null) {
      netClient.close();
    } else {
      socket.close();
    }
  }

  private final class IdleTimeoutCheck implements Handler<Long> {
    @Override
    public void handle(Long event) {
      boolean checkScheduled = false;

      if (connection.getLocalState() == EndpointState.ACTIVE) {
        // Using nano time since it is not related to the wall clock, which may change
        long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        long deadline = transport.tick(now);

        try {
          flush();
        } catch (IllegalStateException ex) {
          // socket closed already
          deadline = 0;
        }

        if (transport.isClosed()) {
          LOG.info("IdleTimeoutCheck closed the transport due to the peer exceeding our requested idle-timeout.");
          disconnect();
        } else {
          if (deadline != 0) {
            // timer treats 0 as error, ensure value is at least 1 as there was a deadline
            long delay = Math.max(deadline - now, 1);
            checkScheduled = true;
            if (LOG.isTraceEnabled()) {
              LOG.trace("IdleTimeoutCheck rescheduling with delay: " + delay);
            }
            idleTimeoutCheckTimerId = vertx.setTimer(delay, this);
          }
        }
      } else {
        LOG.trace("IdleTimeoutCheck skipping check, connection is not active.");
      }

      if (!checkScheduled) {
        idleTimeoutCheckTimerId = null;
        LOG.trace("IdleTimeoutCheck exiting");
      }
    }
  }
}
