/*
* Copyright 2026 the original author or authors.
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
package io.vertx.tests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.codec.WritableBuffer;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.message.impl.MessageImpl;
import org.mockito.Mockito;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.internal.logging.Logger;
import io.vertx.core.internal.logging.LoggerFactory;
import io.vertx.ext.unit.TestContext;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.ProtonServerOptions;

public class TestSupport {

  private static Logger LOG = LoggerFactory.getLogger(TestSupport.class);

  public static ProtonServer createServer(Vertx vertx, Handler<ProtonConnection> serverConnHandler) throws InterruptedException,
                                                                                                    ExecutionException {
    return createServer(vertx, new ProtonServerOptions(), serverConnHandler);
  }

  public static ProtonServer createServer(Vertx vertx, ProtonServerOptions options, Handler<ProtonConnection> serverConnHandler) throws InterruptedException,
                                                                                                                                 ExecutionException {
    ProtonServer server = ProtonServer.create(vertx, options);

    server.connectHandler(serverConnHandler);

    FutureHandler<ProtonServer, AsyncResult<ProtonServer>> handler = FutureHandler.asyncResult();
    server.listen(0, handler);
    handler.get();

    return server;
  }

  public static void validateMessage(TestContext context, int count, Object expected, Message msg) {
    Object actual = getMessageBody(context, msg);
    if (LOG.isTraceEnabled()) {
      LOG.trace("Got msg " + count + ", body: " + actual);
    }

    context.assertEquals(expected, actual, "Unexpected message body");
  }

  public static void validateMessageArray(TestContext context, int count, boolean[] expected, Message msg) {
    Object actual = getMessageBody(context, msg);
    if (LOG.isTraceEnabled()) {
      LOG.trace("Got msg " + count + ", body: " + actual);
    }

    context.assertTrue(actual != null && expected.getClass().isArray(), "Unexpected message body: " + actual);
    context.assertEquals(boolean.class, expected.getClass().getComponentType(), "Unexpected message body: " + actual);
    context.assertTrue(Arrays.equals(expected, ((boolean[]) actual)), "Unexpected message body: " + Arrays.toString((boolean[]) actual));
  }

  public static Object getMessageBody(TestContext context, Message msg) {
    Section body = msg.getBody();

    context.assertNotNull(body);
    context.assertTrue(body instanceof AmqpValue);

    return ((AmqpValue) body).getValue();
  }

  public static MessageImpl prepareMessageWithZeroWidthArrayElements(TestContext context, int elementCount) {
    context.assertTrue(elementCount < 254); // array8 encoding (1 byte size includes 1 byte count, 1 byte constructor)

    MessageImpl providedEncodingMessage = Mockito.mock(MessageImpl.class);
    Mockito.when(providedEncodingMessage.encode(Mockito.any(WritableBuffer.class)))
    .then(i -> {
      WritableBuffer buffer = i.getArgument(0);

      int encodingWidth = 1;
      int arrayPayloadSize = encodingWidth + 1; // variable width for element count + byte type descriptor
      int expectedEncodedArraySize = 1 + encodingWidth + arrayPayloadSize; // array type code + variable width for
                                                                           // array size + other encoded payload

      // Described Type amqp-value
      buffer.put((byte) 0x00); // DescribedType
      buffer.put((byte) 0x53); // small-ulong constructor
      buffer.put((byte) 0x77); // amqp-value ulong descriptor
      // Write the array encoding code, array size, element count, constructor
      buffer.put((byte) 0xE0); // 'array8' type descriptor code
      buffer.put((byte) arrayPayloadSize);
      buffer.put((byte) elementCount);
      buffer.put((byte) 0x41); // boolean-true type constructor

      int expectedLength = 1 + 1 + 1 + expectedEncodedArraySize;
      context.assertEquals(expectedLength, buffer.position());

      return expectedLength;
    });
    return providedEncodingMessage;
  }

  public static List<Object> prepareNestedLists(final int depth) {
    final List<Object> body = new ArrayList<>();

    List<Object> current = body;
    for (int i = 1; i < depth; ++i) {
        final List<Object> next = new ArrayList<>();

        current.add(next);
        current = next;
    }

    return body;
  }

  public static Message prepareMessageWithDecodeDepth(final TestContext context, final int depth) {
    List<Object> body = prepareNestedLists(depth);

    Message message = Proton.message();
    message.setBody(new AmqpValue(body));
    return message;
  }
}
