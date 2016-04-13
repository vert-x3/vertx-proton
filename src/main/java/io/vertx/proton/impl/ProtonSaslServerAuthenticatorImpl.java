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

import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Sasl.SaslOutcome;
import org.apache.qpid.proton.engine.Transport;

import io.vertx.core.Handler;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.sasl.impl.ProtonSaslAnonymousImpl;

/**
 * Manage the SASL authentication process
 */
public class ProtonSaslServerAuthenticatorImpl implements ProtonSaslAuthenticator {

  private Sasl sasl;
  private Handler<ProtonConnection> handler;
  private ProtonConnectionImpl connection;

  public ProtonSaslServerAuthenticatorImpl(Handler<ProtonConnection> handler, ProtonConnectionImpl connection) {
    this.handler = handler;
    this.connection = connection;
  }

  @Override
  public void init(Transport transport) {
    this.sasl = transport.sasl();
    sasl.server();
    sasl.allowSkip(false);
    sasl.setMechanisms(ProtonSaslAnonymousImpl.MECH_NAME);
  }

  @Override
  public boolean process() {
    if (sasl == null) {
      throw new IllegalStateException("Init was not called with the associated transport");
    }

    String[] remoteMechanisms = sasl.getRemoteMechanisms();
    if (remoteMechanisms.length > 0) {
      String chosen = remoteMechanisms[0];
      if (ProtonSaslAnonymousImpl.MECH_NAME.equals(chosen)) {
        sasl.done(SaslOutcome.PN_SASL_OK);
        handler.handle(connection);
        return true;
      } else {
        sasl.done(SaslOutcome.PN_SASL_AUTH);
      }
    }

    return false;
  }
}
