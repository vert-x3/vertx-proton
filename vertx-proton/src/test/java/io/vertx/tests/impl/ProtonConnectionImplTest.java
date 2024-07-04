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
package io.vertx.tests.impl;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import io.vertx.proton.impl.ProtonConnectionImpl;
import org.apache.qpid.proton.engine.Record;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.vertx.core.Vertx;

public class ProtonConnectionImplTest {

  private Vertx vertx;

  @Before
  public void setup() {
    vertx = Vertx.vertx();
  }

  @After
  public void tearDown() {
    if (vertx != null) {
      vertx.close();
    }
  }

  @Test
  public void testAttachments() {
    ProtonConnectionImpl conn = new ProtonConnectionImpl(vertx, "hostname", null);

    Record attachments = conn.attachments();
    assertNotNull("Expected attachments but got null", attachments);
    assertSame("Got different attachments on subsequent call", attachments, conn.attachments());

    String key = "My-Vertx-Key";

    assertNull("Expected attachment to be null", attachments.get(key, Vertx.class));
    attachments.set(key, Vertx.class, vertx);
    assertNotNull("Expected attachment to be returned", attachments.get(key, Vertx.class));
    assertSame("Expected attachment to be given object", vertx, attachments.get(key, Vertx.class));
  }
}
