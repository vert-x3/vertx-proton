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

import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

public class IntegerUtilTest {

  private static byte[] jdkIntToAscii(int number) {
    return String.valueOf(number).getBytes(StandardCharsets.UTF_8);
  }

  @Test
  public void shouldEncodePositiveIntegerInAscii() {
    final ThreadLocalRandom random = ThreadLocalRandom.current();
    for (int i = 0; i < 10_000; i++) {
      final int number = (int) random.nextLong(0, Integer.MAX_VALUE + 1L);
      assert number > 0;
      final byte[] ascii = IntegerUtil.positiveToAscii(number);
      final byte[] expectedAscii = jdkIntToAscii(number);
      Assert.assertArrayEquals(expectedAscii, ascii);
    }
  }

}
