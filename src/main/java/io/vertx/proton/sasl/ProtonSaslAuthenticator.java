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
package io.vertx.proton.sasl;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.net.NetSocket;
import io.vertx.proton.ProtonConnection;
import org.apache.qpid.proton.engine.Transport;

public interface ProtonSaslAuthenticator {

  void init(NetSocket socket, ProtonConnection protonConnection, Transport transport);

  /**
   * Process the SASL authentication cycle until such time as an outcome is determined. This should be called by the
   * managing entity until a completion handler result value is true indicating that the handshake has completed
   * (successfully or otherwise). The result can then be verified by calling {@link #succeeded()}.
   *
   * Any processing of the connection and/or transport objects MUST occur on the calling {@link Context}, and the
   * completion handler MUST be invoked on this same context also. If the completion handler is called on another
   * context an {@link IllegalStateException} will be thrown.
   *
   * @param completionHandler
   *          handler to call when processing of the current state is complete. Value given is true if the SASL
   *          handshake completed.
   */
  void process(Handler<Boolean> completionHandler);

  /**
   * Once called after process finished it returns true if the authentication succeeded.
   *
   * @return true if auth succeeded
   */
  boolean succeeded();
}