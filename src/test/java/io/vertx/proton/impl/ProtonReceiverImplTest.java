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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.engine.Record;
import org.apache.qpid.proton.engine.Session;
import org.junit.Test;

import io.vertx.proton.ProtonReceiver;

public class ProtonReceiverImplTest {

  @Test
  public void testAttachments() {
    Connection conn = Connection.Factory.create();
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
    ProtonConnectionImpl conn = new ProtonConnectionImpl(null, null);
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
    ProtonConnectionImpl conn = new ProtonConnectionImpl(null, null);
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
}
