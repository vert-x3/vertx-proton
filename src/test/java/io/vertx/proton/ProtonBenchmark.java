/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.qpid.proton.message.Message;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.ExecutionException;

import static io.vertx.proton.ProtonHelper.message;
import static io.vertx.proton.ProtonHelper.tag;

@RunWith(VertxUnitRunner.class)
public class ProtonBenchmark {

    private Vertx vertx;
    private ProtonServer server;

    @Before
    public void setup() throws ExecutionException, InterruptedException {
        // Create the Vert.x instance
        vertx = Vertx.vertx();
        server = MockServer.start(vertx);
    }

    @After
    public void tearDown() {
        server.close();
        vertx.close();
    }

    @Test
    public void testClientIdentification(TestContext context) {
        Async async = context.async();
        connect(context, connection -> {
            connection
                .setContainer("foo")
                .openHandler(x -> {
                    context.assertEquals("foo", connection.getContainer());
                    // Our mock server responds with a pong container id
                    context.assertEquals("pong: foo", connection.getRemoteContainer());
                    connection.disconnect();
                    async.complete();
                })
                .open();
        });
    }

    @Test
    public void testRemoteDisconnectHandling(TestContext context) {
        Async async = context.async();
        connect(context, connection->{
            context.assertFalse(connection.isDisconnected());
            connection.disconnectHandler(x ->{
                context.assertTrue(connection.isDisconnected());
                async.complete();
            });
            // Send a reqeust to the sever for him to disconnect us
            connection.send(tag(""), command("disconnect"));
        });
    }

    @Test
    public void testLocalDisconnectHandling(TestContext context) {
        Async async = context.async();
        connect(context, connection->{
            context.assertFalse(connection.isDisconnected());
            connection.disconnectHandler(x ->{
                context.assertTrue(connection.isDisconnected());
                async.complete();
            });
            // We will force the disconnection to the server
            connection.disconnect();
        });
    }

    private void connect(TestContext context, Handler<ProtonConnection> handler) {
        ProtonClient client = ProtonClient.create(vertx);
        client.connect("localhost", server.actualPort(), res->{
            context.assertTrue(res.succeeded());
            handler.handle(res.result());
        });
    }

    private Message command(String kind) {
        Message res = message(kind);
        res.setAddress("command");
        return res;
    }

}
