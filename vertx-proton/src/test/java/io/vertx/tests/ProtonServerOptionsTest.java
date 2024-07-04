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
package io.vertx.tests;

import io.vertx.proton.ProtonServerOptions;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProtonServerOptionsTest {

  @Test
  public void testHeartbeat() {
    ProtonServerOptions options = new ProtonServerOptions();

    options.setHeartbeat(1000);
    assertEquals(options.getHeartbeat(), 1000);
    options.setHeartbeat(2000);
    assertNotEquals(options.getHeartbeat(), 1000);
  }
}
