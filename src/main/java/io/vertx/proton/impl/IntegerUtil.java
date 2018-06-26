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

public final class IntegerUtil {

  private IntegerUtil() {

  }

  private static int digitsOfPositive(int value) {
    //divide & conquer approach
    if (value < 100000) {
      if (value < 100) {
        if (value < 10) {
          return 1;
        } else {
          return 2;
        }
      } else {
        if (value < 1000) {
          return 3;
        } else {
          if (value < 10000) {
            return 4;
          } else {
            return 5;
          }
        }
      }
    } else {
      if (value < 10000000) {
        if (value < 1000000) {
          return 6;
        } else {
          return 7;
        }
      } else {
        if (value < 100000000) {
          return 8;
        } else {
          if (value < 1000000000) {
            return 9;
          } else {
            return 10;
          }
        }
      }
    }
  }

  private static void positiveToAscii(int num, byte[] numberBuffer, int expectedSize) {
    //the loop cannot be inverted to remain counted
    for (int i = 0; i < expectedSize; i++) {
      byte b = (byte) (num % 10L + '0');
      num /= 10;
      numberBuffer[expectedSize - i - 1] = b;
    }
  }

  /**
   * Return the ASCII representation of a positive {@code integer}.
   *
   * @param number a positive {@code integer}
   * @return a new {@code byte[]} with the given {@code number} ASCII encoded
   */
  public static byte[] positiveToAscii(int number) {
    final int digits = digitsOfPositive(number);
    final byte[] ascii = new byte[digits];
    positiveToAscii(number, ascii, digits);
    return ascii;
  }
}
