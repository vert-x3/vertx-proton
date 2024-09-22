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
package io.vertx.tests.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;

import io.vertx.proton.impl.ProtonConnectionImpl;
import io.vertx.proton.impl.ProtonReceiverImpl;
import io.vertx.proton.impl.ProtonSessionImpl;
import org.apache.qpid.proton.amqp.Binary;
import org.apache.qpid.proton.amqp.UnsignedLong;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.amqp.transport.LinkError;
import org.apache.qpid.proton.codec.ReadableBuffer;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Record;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.engine.Transport;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.message.impl.MessageImpl;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.internal.ContextInternal;
import io.vertx.core.internal.net.NetSocketInternal;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonHelper;
import io.vertx.proton.ProtonMessageHandler;
import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonTransportOptions;

public class ProtonReceiverImplTest {

  @Test
  public void testAttachments() {
    Connection conn = Connection.Factory.create();
    Transport.Factory.create().bind(conn);
    Session sess = conn.session();
    Receiver r = sess.receiver("name");

    ProtonReceiverImpl receiver = new ProtonReceiverImpl(r);

    Record attachments = receiver.attachments();
    assertNotNull("Expected attachments but got null", attachments);
    assertSame("Got different attachments on subsequent call", attachments, receiver.attachments());

    String key = "My-Connection-Key";

    assertNull("Expected attachment to be null", attachments.get(key, Connection.class));
    attachments.set(key, Connection.class, conn);
    assertNotNull("Expected attachment to be returned", attachments.get(key, Connection.class));
    assertSame("Expected attachment to be given object", conn, attachments.get(key, Connection.class));
  }

  @Test
  public void testDrainWithoutDisablingPrefetchThrowsISE() {
    Connection conn = Connection.Factory.create();
    Transport.Factory.create().bind(conn);
    Session sess = conn.session();
    Receiver r = sess.receiver("name");

    ProtonReceiverImpl receiver = new ProtonReceiverImpl(r);

    try {
      receiver.drain(0, h-> {});
      fail("should have thrown due to prefetch still being enabled");
    } catch (IllegalStateException ise) {
      // Expected
    }
  }

  @Test
  public void testDrainWithoutHandlerThrowsIAE() {
    Connection conn = Connection.Factory.create();
    Transport.Factory.create().bind(conn);
    Session sess = conn.session();
    Receiver r = sess.receiver("name");

    ProtonReceiverImpl receiver = new ProtonReceiverImpl(r);

    receiver.setPrefetch(0);
    try {
      receiver.drain(0, null);
      fail("should have thrown due to lack of handler");
    } catch (IllegalArgumentException iae) {
      // Expected
    }
  }

  @Test
  public void testDrainWithExistingDrainOutstandingThrowsISE() {
    ProtonConnectionImpl conn = new ProtonConnectionImpl(null, null, null);
    conn.bindClient(null, Mockito.mock(NetSocketInternal.class), null, new ProtonTransportOptions());
    conn.fireDisconnect();
    ProtonReceiver receiver = conn.createReceiver("address");

    AtomicBoolean drain1complete = new AtomicBoolean();

    receiver.setPrefetch(0);
    receiver.flow(1);
    receiver.drain(0, h -> {
      drain1complete.set(true);
    });

    try {
      receiver.drain(0, h2-> {});
      fail("should have thrown due to outstanding drain operation");
    } catch (IllegalStateException ise) {
      // Expected
      assertFalse("first drain should not have been completed", drain1complete.get());
    }
  }

  @Test
  public void testFlowWithExistingDrainOutstandingThrowsISE() {
    ProtonConnectionImpl conn = new ProtonConnectionImpl(null, null, null);
    conn.bindClient(null, Mockito.mock(NetSocketInternal.class), null, new ProtonTransportOptions());
    conn.fireDisconnect();
    ProtonReceiver receiver = conn.createReceiver("address");

    AtomicBoolean drain1complete = new AtomicBoolean();

    receiver.setPrefetch(0);
    receiver.flow(1);
    receiver.drain(0, h -> {
      drain1complete.set(true);
    });

    try {
      receiver.flow(1);
      fail("should have thrown due to outstanding drain operation");
    } catch (IllegalStateException ise) {
      // Expected
      assertFalse("drain should not have been completed", drain1complete.get());
    }
  }

  /**
   * Verifies that the receiver detaches the link with an
   * amqp:link:message-size-exceeded error code when receiving a single-transfer message that
   * exceeds the link's max-message-size.
   */
  @Test
  public void testSingleTransferExceedingMaxMessageSizeResultsInLinkBeingDetached() {
    int maxFrameSize = 800;
    long maxMessageSize = 500;
    Transport transport = mock(Transport.class);
    when(transport.getMaxFrameSize()).thenReturn(maxFrameSize);

    Connection con = mock(Connection.class);
    ProtonConnectionImpl conImpl = new ProtonConnectionImpl(mock(Vertx.class), "hostname", mock(ContextInternal.class));
    when(con.getTransport()).thenReturn(transport);
    when(con.getContext()).thenReturn(conImpl);

    Session session = mock(Session.class);
    ProtonSessionImpl sessionImpl = new ProtonSessionImpl(session);
    when(session.getConnection()).thenReturn(con);
    when(session.getContext()).thenReturn(sessionImpl);
    when(session.getIncomingCapacity()).thenReturn(10_000);
    when(session.getIncomingBytes()).thenReturn(10_000);

    Receiver r = mock(Receiver.class);
    when(r.getLocalState()).thenReturn(EndpointState.ACTIVE);
    when(r.getSession()).thenReturn(session);
    when(r.getMaxMessageSize()).thenReturn(new UnsignedLong(maxMessageSize));
    ProtonReceiverImpl recImpl = new ProtonReceiverImpl(r);
    when(r.getContext()).thenReturn(recImpl);

    ProtonMessageHandler messageHandler = mock(ProtonMessageHandler.class);
    ProtonReceiverImpl receiver = new ProtonReceiverImpl(r);
    receiver.handler(messageHandler);

    byte[] encodedMessage = createEncodedMessage(700);
    assertTrue(encodedMessage.length <= maxFrameSize);
    assertTrue(encodedMessage.length > maxMessageSize);

    ReadableBuffer frame0 = ReadableBuffer.ByteBufferReader.wrap(encodedMessage);

    Delivery delivery0 = mock(Delivery.class);
    when(delivery0.getLink()).thenReturn(r);
    when(delivery0.isPartial()).thenReturn(false);
    when(delivery0.available()).thenReturn(encodedMessage.length);
    when(delivery0.isSettled()).thenReturn(false);

    when(r.current()).thenReturn(delivery0);
    when(r.recv()).thenReturn(frame0);

    // WHEN delivering the message in a single transfer frame
    receiver.onDelivery();

    // THEN the receiver is being detached with the expected error condition
    verify(r).detach();
    ArgumentCaptor<ErrorCondition> conditionCapture = ArgumentCaptor.forClass(ErrorCondition.class);
    verify(r).setCondition(conditionCapture.capture());
    ErrorCondition errorCondition = conditionCapture.getValue();
    assertNotNull(errorCondition);
    assertEquals("Unxpected error condition", LinkError.MESSAGE_SIZE_EXCEEDED, errorCondition.getCondition());

    // and the message is not being delivered to the application layer
    verify(messageHandler, never()).handle(any(ProtonDelivery.class), any(Message.class));
  }

  /**
   * Verifies that the receivers maxMessageSizeExceeded closes the link with an
   * when receiving a multi-transfer message that exceeds the link's max-message-size.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testMultiTransferExceedingMaxMessageSizeWithMaxMessageSizeExceededHandler() {
    int maxFrameSize = 512;
    long maxMessageSize = 650;
    Transport transport = mock(Transport.class);
    when(transport.getMaxFrameSize()).thenReturn(maxFrameSize);

    Connection con = mock(Connection.class);
    ProtonConnectionImpl conImpl = new ProtonConnectionImpl(mock(Vertx.class), "hostname", mock(ContextInternal.class));
    when(con.getTransport()).thenReturn(transport);
    when(con.getContext()).thenReturn(conImpl);

    Session session = mock(Session.class);
    ProtonSessionImpl sessionImpl = new ProtonSessionImpl(session);
    when(session.getConnection()).thenReturn(con);
    when(session.getContext()).thenReturn(sessionImpl);
    when(session.getIncomingCapacity()).thenReturn(10_000);
    when(session.getIncomingBytes()).thenReturn(10_000);

    Receiver r = mock(Receiver.class);
    when(r.getLocalState()).thenReturn(EndpointState.ACTIVE);
    when(r.getSession()).thenReturn(session);
    when(r.getMaxMessageSize()).thenReturn(new UnsignedLong(maxMessageSize));
    ProtonReceiverImpl recImpl = new ProtonReceiverImpl(r);
    when(r.getContext()).thenReturn(recImpl);

    ProtonMessageHandler messageHandler = mock(ProtonMessageHandler.class);
    Handler<ProtonReceiver> maxMessageSizeExceededHandler = mock(Handler.class);
    doAnswer(invocation -> {
      // Contrary to the default behaviour of detaching, the handler will close the link manually.
      ProtonReceiver receiver = invocation.getArgument(0);
      receiver.close();
      when(r.getLocalState()).thenReturn(EndpointState.CLOSED);
      return null;
    }).when(maxMessageSizeExceededHandler).handle(any(ProtonReceiver.class));

    ProtonReceiverImpl receiver = new ProtonReceiverImpl(r);
    receiver.handler(messageHandler);
    receiver.maxMessageSizeExceededHandler(maxMessageSizeExceededHandler);

    byte[] encodedMessage = createEncodedMessage(700);
    assertTrue(encodedMessage.length > maxFrameSize);
    assertTrue(encodedMessage.length <= 2 * maxFrameSize);
    assertTrue(encodedMessage.length > maxMessageSize);

    byte[] chunk0 = new byte[maxFrameSize];
    System.arraycopy(encodedMessage, 0, chunk0, 0, chunk0.length);
    ReadableBuffer frame0 = ReadableBuffer.ByteBufferReader.wrap(chunk0);

    Delivery delivery0 = mock(Delivery.class);
    when(delivery0.getLink()).thenReturn(r);
    when(delivery0.isPartial()).thenReturn(true);
    when(delivery0.available()).thenReturn(chunk0.length);
    when(delivery0.isSettled()).thenReturn(false);

    byte[] chunk1 = new byte[encodedMessage.length - maxFrameSize];
    System.arraycopy(encodedMessage, maxFrameSize, chunk1, 0, chunk1.length);
    ReadableBuffer frame1 = ReadableBuffer.ByteBufferReader.wrap(chunk1);

    Delivery delivery1 = mock(Delivery.class);
    when(delivery1.getLink()).thenReturn(r);
    when(delivery1.isPartial()).thenReturn(false);
    when(delivery1.available()).thenReturn(chunk1.length);
    when(delivery1.isSettled()).thenReturn(false);

    when(r.current()).thenReturn(delivery0, delivery1);
    when(r.recv()).thenReturn(frame0, frame1);

    // WHEN delivering the message in two consecutive transfer frames
    receiver.onDelivery();
    receiver.onDelivery();

    // THEN the maxMessageSizeExceeded handler is being invoked
    verify(maxMessageSizeExceededHandler).handle(receiver);
    // and the receiver has been closed by the maxMessageSizeExceeded handler
    verify(r).close();
    verify(r, never()).detach();
    // and the message is not being delivered to the application layer
    verify(messageHandler, never()).handle(any(ProtonDelivery.class), any(Message.class));
  }

  /**
   * Verifies that the receiver accepts a multi-transfer message that does
   * not exceed the link's max-message-size and delivers it to the application layer.
   */
  @SuppressWarnings("unchecked")
  @Test
  public void testMultiTransferMessage() {
    int maxFrameSize = 512;
    int maxMessageSize = 800;
    Transport transport = mock(Transport.class);
    when(transport.getMaxFrameSize()).thenReturn(maxFrameSize);

    Connection con = mock(Connection.class);
    ProtonConnectionImpl conImpl = new ProtonConnectionImpl(mock(Vertx.class), "hostname", mock(ContextInternal.class));
    when(con.getTransport()).thenReturn(transport);
    when(con.getContext()).thenReturn(conImpl);

    Session session = mock(Session.class);
    ProtonSessionImpl sessionImpl = new ProtonSessionImpl(session);
    when(session.getConnection()).thenReturn(con);
    when(session.getContext()).thenReturn(sessionImpl);
    when(session.getIncomingCapacity()).thenReturn(10_000);
    when(session.getIncomingBytes()).thenReturn(10_000);

    Receiver r = mock(Receiver.class);
    when(r.getLocalState()).thenReturn(EndpointState.ACTIVE);
    when(r.getSession()).thenReturn(session);
    when(r.getMaxMessageSize()).thenReturn(new UnsignedLong(maxMessageSize));
    ProtonReceiverImpl recImpl = new ProtonReceiverImpl(r);
    when(r.getContext()).thenReturn(recImpl);

    ProtonMessageHandler messageHandler = mock(ProtonMessageHandler.class);
    Handler<ProtonReceiver> maxMessageSizeExceededHandler = mock(Handler.class);
    ProtonReceiverImpl receiver = new ProtonReceiverImpl(r);
    receiver.handler(messageHandler);
    receiver.maxMessageSizeExceededHandler(maxMessageSizeExceededHandler);

    byte[] encodedMessage = createEncodedMessage(maxMessageSize);
    assertTrue(encodedMessage.length > maxFrameSize);
    assertTrue(encodedMessage.length <= 2 * maxFrameSize);
    assertTrue(encodedMessage.length <= maxMessageSize);

    byte[] chunk0 = new byte[maxFrameSize];
    System.arraycopy(encodedMessage, 0, chunk0, 0, chunk0.length);
    ReadableBuffer frame0 = ReadableBuffer.ByteBufferReader.wrap(chunk0);

    Delivery delivery0 = mock(Delivery.class);
    when(delivery0.getLink()).thenReturn(r);
    when(delivery0.isPartial()).thenReturn(true);
    when(delivery0.available()).thenReturn(chunk0.length);
    when(delivery0.isSettled()).thenReturn(false);

    byte[] chunk1 = new byte[encodedMessage.length - maxFrameSize];
    System.arraycopy(encodedMessage, maxFrameSize, chunk1, 0, chunk1.length);
    ReadableBuffer frame1 = ReadableBuffer.ByteBufferReader.wrap(chunk1);

    Delivery delivery1 = mock(Delivery.class);
    when(delivery1.getLink()).thenReturn(r);
    when(delivery1.isPartial()).thenReturn(false);
    when(delivery1.available()).thenReturn(chunk1.length);
    when(delivery1.isSettled()).thenReturn(false);

    when(r.current()).thenReturn(delivery0, delivery1);
    when(r.recv()).thenReturn(frame0, frame1);

    // WHEN delivering the message in two consecutive transfer frames
    receiver.onDelivery();
    receiver.onDelivery();

    // THEN the disposition in reply to the second transfer has state accepted
    verify(delivery1).disposition(any(Accepted.class));
    // and the receiver has been advanced to the next delivery
    verify(r).advance();
    // and the sender has been issued a new credit
    verify(r).flow(1);
    // and the message has been delivered to the application layer
    verify(messageHandler).handle(any(ProtonDelivery.class), any(Message.class));
    // and the link is not being closed
    verify(r, never()).close();
    verify(maxMessageSizeExceededHandler, never()).handle(any(ProtonReceiver.class));
  }

  private byte[] createEncodedMessage(int targetMessageSize) {
    MessageImpl msg = (MessageImpl) ProtonHelper.message();
    msg.setContentType("application/octet-stream");
    msg.setAddress("telemetry");

    // the address, content-type, sections etc incur an overhead of 62 bytes
    byte[] payload = new byte[targetMessageSize - 62];
    for (int i = 0; i < payload.length; i++) {
      payload[i] = (byte) (i % 256);
    }

    msg.setBody(new Data(new Binary(payload)));

    byte[] encodedMessage = new byte[targetMessageSize];
    assertEquals("Unxpected encoding size", targetMessageSize, msg.encode(encodedMessage, 0, encodedMessage.length));

    return encodedMessage;
  }
}
