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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Record;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.engine.Session;
import org.junit.Test;

public class ProtonSenderImplTest {

  @Test
  public void testAttachments() {
    Connection conn = Connection.Factory.create();
    Session sess = conn.session();
    Sender s = sess.sender("name");

    ProtonSenderImpl sender = new ProtonSenderImpl(s);

    Record attachments = sender.attachments();
    assertNotNull("Expected attachments but got null", attachments);
    assertSame("Got different attachments on subsequent call", attachments, sender.attachments());

    String key = "My-Connection-Key";

    assertNull("Expected attachment to be null", attachments.get(key, Connection.class));
    attachments.set(key, Connection.class, conn);
    assertNotNull("Expected attachment to be returned", attachments.get(key, Connection.class));
    assertSame("Expected attachment to be given object", conn, attachments.get(key, Connection.class));
  }
}
