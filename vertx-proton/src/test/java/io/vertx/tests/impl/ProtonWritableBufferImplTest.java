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
package io.vertx.tests.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import io.vertx.proton.impl.ProtonWritableBufferImpl;
import org.apache.qpid.proton.codec.ReadableBuffer;
import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Tests for behavior of ProtonWritableBufferImpl
 */
public class ProtonWritableBufferImplTest {

  @Test
  public void testGetBuffer() {
    ByteBuf buffer = Unpooled.buffer(1024);
    ProtonWritableBufferImpl writable = new ProtonWritableBufferImpl(buffer);

    assertSame(buffer, writable.getBuffer());
  }

  @Test
  public void testLimit() {
    ByteBuf buffer = Unpooled.buffer(1024);
    ProtonWritableBufferImpl writable = new ProtonWritableBufferImpl(buffer);

    assertEquals(buffer.capacity(), writable.limit());
  }

  @Test
  public void testRemaining() {
    ByteBuf buffer = Unpooled.buffer(1024);
    ProtonWritableBufferImpl writable = new ProtonWritableBufferImpl(buffer);

    assertEquals(buffer.maxCapacity(), writable.remaining());
    writable.put((byte) 0);
    assertEquals(buffer.maxCapacity() - 1, writable.remaining());
  }

  @Test
  public void testHasRemaining() {
    ByteBuf buffer = Unpooled.buffer(100, 100);
    ProtonWritableBufferImpl writable = new ProtonWritableBufferImpl(buffer);

    assertTrue(writable.hasRemaining());
    writable.put((byte) 0);
    assertTrue(writable.hasRemaining());
    buffer.writerIndex(buffer.maxCapacity());
    assertFalse(writable.hasRemaining());
  }

  @Test
  public void testGetPosition() {
    ByteBuf buffer = Unpooled.buffer(1024);
    ProtonWritableBufferImpl writable = new ProtonWritableBufferImpl(buffer);

    assertEquals(0, writable.position());
    writable.put((byte) 0);
    assertEquals(1, writable.position());
  }

  @Test
  public void testSetPosition() {
    ByteBuf buffer = Unpooled.buffer(1024);
    ProtonWritableBufferImpl writable = new ProtonWritableBufferImpl(buffer);

    assertEquals(0, writable.position());
    writable.position(1);
    assertEquals(1, writable.position());
  }

  @Test
  public void testPutByteBuffer() {
    ByteBuffer input = ByteBuffer.allocate(1024);
    input.put((byte) 1);
    input.flip();

    ByteBuf buffer = Unpooled.buffer(1024);
    ProtonWritableBufferImpl writable = new ProtonWritableBufferImpl(buffer);

    assertEquals(0, writable.position());
    writable.put(input);
    assertEquals(1, writable.position());
  }

  @Test
  public void testPutByteBuf() {
    ByteBuf input = Unpooled.buffer();
    input.writeByte((byte) 1);

    ByteBuf buffer = Unpooled.buffer(1024);
    ProtonWritableBufferImpl writable = new ProtonWritableBufferImpl(buffer);

    assertEquals(0, writable.position());
    writable.put(input);
    assertEquals(1, writable.position());
  }

  @Test
  public void testPutString() {
    String ascii = new String("ASCII");

    ByteBuf buffer = Unpooled.buffer(1024);
    ProtonWritableBufferImpl writable = new ProtonWritableBufferImpl(buffer);

    assertEquals(0, writable.position());
    writable.put(ascii);
    assertEquals(ascii.length(), writable.position());

    ByteBuf written = writable.getBuffer();
    assertEquals(ascii, written.toString(StandardCharsets.UTF_8));
  }

  @Test
  public void testPutReadableBuffer() {
    doPutReadableBufferTestImpl(true);
    doPutReadableBufferTestImpl(false);
  }

  private void doPutReadableBufferTestImpl(boolean readOnly) {
    ByteBuffer buf = ByteBuffer.allocate(1024);
    buf.put((byte) 1);
    buf.flip();
    if (readOnly) {
      buf = buf.asReadOnlyBuffer();
    }

    ReadableBuffer input = new ReadableBuffer.ByteBufferReader(buf);

    if (readOnly) {
      assertFalse("Expected buffer not to hasArray()", input.hasArray());
    } else {
      assertTrue("Expected buffer to hasArray()", input.hasArray());
    }

    ByteBuf buffer = Unpooled.buffer(1024);
    ProtonWritableBufferImpl writable = new ProtonWritableBufferImpl(buffer);

    assertEquals(0, writable.position());
    writable.put(input);
    assertEquals(1, writable.position());
  }
}
