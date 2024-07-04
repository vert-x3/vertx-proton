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
package io.vertx.proton.impl;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.apache.qpid.proton.codec.ReadableBuffer;
import org.apache.qpid.proton.codec.WritableBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Proton WritableBuffer implementation that wraps a Netty ByteBuf
 */
public class ProtonWritableBufferImpl implements WritableBuffer {

  public static final int INITIAL_CAPACITY = 1024;

  public ByteBuf nettyBuffer;

  public ProtonWritableBufferImpl() {
    this(INITIAL_CAPACITY);
  }

  public ProtonWritableBufferImpl(int initialCapacity) {
    nettyBuffer = Unpooled.buffer(initialCapacity);
  }

  public ProtonWritableBufferImpl(ByteBuf buffer) {
    nettyBuffer = buffer;
  }

  public ByteBuf getBuffer() {
    return nettyBuffer;
  }

  @Override
  public void put(byte b) {
    nettyBuffer.writeByte(b);
  }

  @Override
  public void putFloat(float f) {
    nettyBuffer.writeFloat(f);
  }

  @Override
  public void putDouble(double d) {
    nettyBuffer.writeDouble(d);
  }

  @Override
  public void put(byte[] src, int offset, int length) {
    nettyBuffer.writeBytes(src, offset, length);
  }

  @Override
  public void put(ByteBuffer payload) {
    nettyBuffer.writeBytes(payload);
  }

  public void put(ByteBuf payload) {
    nettyBuffer.writeBytes(payload);
  }

  @Override
  public void putShort(short s) {
    nettyBuffer.writeShort(s);
  }

  @Override
  public void putInt(int i) {
    nettyBuffer.writeInt(i);
  }

  @Override
  public void putLong(long l) {
    nettyBuffer.writeLong(l);
  }

  @Override
  public boolean hasRemaining() {
    return nettyBuffer.writerIndex() < nettyBuffer.maxCapacity();
  }

  @Override
  public int remaining() {
    return nettyBuffer.maxCapacity() - nettyBuffer.writerIndex();
  }

  @Override
  public void ensureRemaining(int remaining) {
    nettyBuffer.ensureWritable(remaining);
  }

  @Override
  public int position() {
    return nettyBuffer.writerIndex();
  }

  @Override
  public void position(int position) {
    nettyBuffer.writerIndex(position);
  }

  @Override
  public int limit() {
    return nettyBuffer.capacity();
  }

  @Override
  public void put(ReadableBuffer buffer) {
    buffer.get(this);
  }

  @Override
  public void put(String value) {
    nettyBuffer.writeCharSequence(value, StandardCharsets.UTF_8);
  }
}
