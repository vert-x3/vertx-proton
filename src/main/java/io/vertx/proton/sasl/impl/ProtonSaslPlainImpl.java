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
package io.vertx.proton.sasl.impl;

import java.nio.charset.StandardCharsets;

public class ProtonSaslPlainImpl extends ProtonSaslMechanismImpl {

  public static final String MECH_NAME = "PLAIN";

  @Override
  public int getPriority() {
    return PRIORITY.LOWER.getValue();
  }

  @Override
  public String getName() {
    return MECH_NAME;
  }

  @Override
  public byte[] getInitialResponse() {

    String username = getUsername();
    String password = getPassword();

    if (username == null) {
      username = "";
    }

    if (password == null) {
      password = "";
    }

    byte[] usernameBytes = username.getBytes(StandardCharsets.UTF_8);
    byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
    byte[] data = new byte[usernameBytes.length + passwordBytes.length + 2];
    System.arraycopy(usernameBytes, 0, data, 1, usernameBytes.length);
    System.arraycopy(passwordBytes, 0, data, 2 + usernameBytes.length, passwordBytes.length);
    return data;
  }

  @Override
  public byte[] getChallengeResponse(byte[] challenge) {
    return EMPTY;
  }

  @Override
  public boolean isApplicable(String username, String password) {
    return username != null && username.length() > 0 && password != null && password.length() > 0;
  }
}
