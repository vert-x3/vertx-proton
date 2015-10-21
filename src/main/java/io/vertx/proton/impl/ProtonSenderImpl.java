/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton.impl;

import io.vertx.core.Handler;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.amqp.transport.Target;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.message.Message;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonSenderImpl extends ProtonLinkImpl<ProtonSender> implements ProtonSender {

    private Handler<ProtonSender> drainHandler;

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
        Delivery delivery = sender().delivery(tag); // start a new delivery..
        sender().send(encodedMessage, 0, len);
        if( link.getSenderSettleMode()== SenderSettleMode.SETTLED ) {
            delivery.settle();
        }
        sender().advance(); // ends the delivery.
        getSessionImpl().getConnectionImpl().flush();

        return new ProtonDeliveryImpl(delivery);
    }

    @Override
    protected ProtonSenderImpl self() {
        return this;
    }

    @Override
    public boolean sendQueueFull() {
        return link.getRemoteCredit() <= 0;
    }


    @Override
    public void sendQueueDrainHandler(Handler<ProtonSender> drainHandler) {
        this.drainHandler = drainHandler;
        fireLinkFlow();
    }

    @Override
    void fireLinkFlow() {
        if( link.getRemoteCredit()>0 && drainHandler!=null ) {
            drainHandler.handle(this);
        }
    }
}
