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

import java.util.Set;

import javax.security.sasl.SaslException;

import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import org.apache.qpid.proton.engine.Sasl;
import org.apache.qpid.proton.engine.Transport;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.ProtonConnection;
import io.vertx.proton.sasl.ProtonSaslMechanism;
import io.vertx.proton.sasl.impl.ProtonSaslMechanismFinderImpl;

/**
 * Manage the SASL authentication process
 */
public class ProtonSaslClientAuthenticatorImpl implements ProtonSaslAuthenticator {

  private Sasl sasl;
  private final String username;
  private final String password;
  private ProtonSaslMechanism mechanism;
  private Set<String> mechanismsRestriction;
  private Handler<AsyncResult<ProtonConnection>> handler;
  private NetSocket socket;
  private ProtonConnection connection;
  private boolean succeeded;

  /**
   * Create the authenticator and initialize it.
   *
   * @param username
   *          The username provide credentials to the remote peer, or null if there is none.
   * @param password
   *          The password provide credentials to the remote peer, or null if there is none.
   * @param allowedSaslMechanisms
   *          The possible mechanism(s) to which the client should restrict its mechanism selection to if offered by the
   *          server, or null/empty if no restriction.
   * @param handler
   *          The handler to convey the result of the SASL process to
   */
  public ProtonSaslClientAuthenticatorImpl(String username, String password, Set<String> allowedSaslMechanisms, Handler<AsyncResult<ProtonConnection>> handler) {
    this.handler = handler;
    this.username = username;
    this.password = password;
    this.mechanismsRestriction = allowedSaslMechanisms;
  }

  @Override
  public void init(NetSocket socket, ProtonConnection protonConnection, Transport transport) {
    this.socket = socket;
    this.connection = protonConnection;
    this.sasl = transport.sasl();
    sasl.client();
  }

  @Override
  public void process(Handler<Boolean> completionHandler) {
    if (sasl == null) {
      throw new IllegalStateException("Init was not called with the associated transport");
    }

    boolean done = false;
    succeeded = false;

    try {
      switch (sasl.getState()) {
      case PN_SASL_IDLE:
        handleSaslInit();
        break;
      case PN_SASL_STEP:
        handleSaslStep();
        break;
      case PN_SASL_FAIL:
        done = true;
        handler.handle(Future.failedFuture(new SecurityException("Failed to authenticate")));
        break;
      case PN_SASL_PASS:
        done = true;
        succeeded = true;
        handler.handle(Future.succeededFuture(connection));
        break;
      default:
      }
    } catch (Exception e) {
      done = true;
      try {
        if (socket != null) {
          socket.close();
        }
      } finally {
        handler.handle(Future.failedFuture(e));
      }
    }

    completionHandler.handle(done);
  }

  @Override
  public boolean succeeded() {
    return succeeded;
  }

  private void handleSaslInit() throws SecurityException {
    try {
      String[] remoteMechanisms = sasl.getRemoteMechanisms();
      if (remoteMechanisms != null && remoteMechanisms.length != 0) {
        mechanism = ProtonSaslMechanismFinderImpl.findMatchingMechanism(username, password, mechanismsRestriction,
            remoteMechanisms);
        if (mechanism != null) {
          mechanism.setUsername(username);
          mechanism.setPassword(password);

          sasl.setMechanisms(mechanism.getName());
          byte[] response = mechanism.getInitialResponse();
          if (response != null) {
            sasl.send(response, 0, response.length);
          }
        } else {
          throw new SecurityException(
              "Could not find a suitable SASL mechanism for the remote peer using the available credentials.");
        }
      }
    } catch (SaslException se) {
      throw new SecurityException("Exception while processing SASL init.", se);
    }
  }

  private void handleSaslStep() throws SecurityException {
    try {
      if (sasl.pending() != 0) {
        byte[] challenge = new byte[sasl.pending()];
        sasl.recv(challenge, 0, challenge.length);
        byte[] response = mechanism.getChallengeResponse(challenge);
        sasl.send(response, 0, response.length);
      }
    } catch (SaslException se) {
      throw new SecurityException("Exception while processing SASL step.", se);
    }
  }
}
