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

import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonServer;
import io.vertx.proton.sasl.ProtonSaslAuthenticator;
import org.apache.qpid.proton.engine.Transport;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class ProtonServerImplTest {

    @Test(timeout = 20000)
    public void testConnectionOpenResultReturnsConnection(TestContext context) {
        Async async = context.async();
        Vertx vertx = Vertx.vertx();
        ProtonServer.create(vertx).saslAuthenticator(new ProtonSaslAuthenticator() {
            @Override
            public void init(Transport transport) {
                async.complete();
            }

            @Override
            public boolean process() {
                return false;
            }

            @Override
            public boolean succeeded() {
                return false;
            }
        }).connectHandler(protonConnection -> {}).listen(server ->
                ProtonClient.create(vertx).connect("localhost", server.result().actualPort(),
                        protonConnectionAsyncResult -> {})
        );
    }

}
