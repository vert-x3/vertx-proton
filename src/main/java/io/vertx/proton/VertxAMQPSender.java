/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.message.Message;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class VertxAMQPSender extends VertxAMQPLink<VertxAMQPSender> {

    VertxAMQPSender(Sender sender) {
        super(sender);
    }
    private Sender sender() {
        return (Sender)link;
    }

    public VertxAMQPDelivery send( Message message) {
        return this.send(null, message);
    }

    public VertxAMQPDelivery send(byte[] tag, Message message) {
        int BUFFER_SIZE = 1024;
        byte[] encodedMessage = new byte[BUFFER_SIZE];
        int len = message.encode(encodedMessage, 0, BUFFER_SIZE);
        Delivery delivery = sender().delivery(tag);
        sender().send(encodedMessage, 0, len);
        sender().advance();
        return new VertxAMQPDelivery(delivery);
    }

    @Override
    protected VertxAMQPSender self() {
        return this;
    }
}
