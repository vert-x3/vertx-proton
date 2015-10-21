/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton.impl;

import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.message.Message;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonSenderImpl extends ProtonLinkImpl<ProtonSender> implements ProtonSender {

    ProtonSenderImpl(Sender sender) {
        super(sender);
    }
    private Sender sender() {
        return (Sender)link;
    }


    public ProtonDeliveryImpl send(byte[] tag, Message message) {
        int BUFFER_SIZE = 1024;
        byte[] encodedMessage = new byte[BUFFER_SIZE];
        int len = message.encode(encodedMessage, 0, BUFFER_SIZE);
        Delivery delivery = sender().delivery(tag);
        sender().send(encodedMessage, 0, len);
        sender().advance();
        getSessionImpl().getConnectionImpl().flush();
        return new ProtonDeliveryImpl(delivery);
    }

    @Override
    protected ProtonSenderImpl self() {
        return this;
    }
}
