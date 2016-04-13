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

public class ProtonSaslExternalImpl extends ProtonSaslMechanismImpl {

  public static final String MECH_NAME = "EXTERNAL";

  @Override
  public byte[] getInitialResponse() {
    return EMPTY;
  }

  @Override
  public byte[] getChallengeResponse(byte[] challenge) {
    return EMPTY;
  }

  @Override
  public int getPriority() {
    return PRIORITY.HIGHER.getValue();
  }

  @Override
  public String getName() {
    return MECH_NAME;
  }

  @Override
  public boolean isApplicable(String username, String password) {
    return true;
  }
}
