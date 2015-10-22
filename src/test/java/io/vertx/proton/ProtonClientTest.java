/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.vertx.proton.ProtonHelper.message;
import static io.vertx.proton.ProtonHelper.tag;

@RunWith(VertxUnitRunner.class)
public class ProtonClientTest extends MockServerTestBase {


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
            connection.send(tag(""), message("command", "disconnect"));
        });
    }

    @Test
    public void testLocalDisconnectHandling(TestContext context) {
        Async async = context.async();
        connect(context, connection -> {
            context.assertFalse(connection.isDisconnected());
            connection.disconnectHandler(x -> {
                context.assertTrue(connection.isDisconnected());
                async.complete();
            });
            // We will force the disconnection to the server
            connection.disconnect();
        });
    }

    @Test
    public void testRequestResponse(TestContext context) {
        sendReceiveEcho(context, "Hello World");
    }

    @Test
    public void testTransferLargeMessage(TestContext context) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 1024*1024*5; i++) {
            builder.append('a'+(i%26));
        }
        sendReceiveEcho(context, builder.toString());
    }

    private void sendReceiveEcho(TestContext context, String data) {
        Async async = context.async();
        connect(context, connection -> {

            ProtonSession session = connection.session().open();
            session.receiver("echo")
                .handler((r, d, m) -> {
                    String actual = (String) ((AmqpValue) m.getBody()).getValue();
                    context.assertEquals(data, actual);
                    connection.disconnect();
                    async.complete();
                })
                .flow(10)
                .open();

            session.sender()
                .open()
                .send(tag(""), message("echo", data));


        });
    }

}
