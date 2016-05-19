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
import io.vertx.proton.ProtonClient;
import io.vertx.proton.ProtonServer;
import org.apache.qpid.proton.engine.Transport;
import org.junit.Test;

import java.util.concurrent.Callable;

import static com.jayway.awaitility.Awaitility.await;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ProtonServerImplTest {

    @Test
    public void shouldInvokePassedAuthenticator() throws InterruptedException {
        // Given
        Vertx vertx = Vertx.vertx();
        ProtonSaslAuthenticator authenticator = mock(ProtonSaslAuthenticator.class);
        ProtonServer.create(vertx, authenticator).connectHandler(protonConnection -> {}).listen(9999);

        // When
        ProtonClient.create(vertx).connect("localhost", 9999, protonConnectionAsyncResult -> {});

        // Then
        await().until(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                verify(authenticator).init(any(Transport.class));
                return true;
            }
        });
    }

}
