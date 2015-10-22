/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton.impl;

import io.vertx.core.Handler;
import io.vertx.proton.ProtonDelivery;
import io.vertx.proton.ProtonSender;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.codec.WritableBuffer;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Sender;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.message.ProtonJMessage;
import org.apache.qpid.proton.message.impl.MessageImpl;

import java.nio.ByteBuffer;

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

    @Override
    public void send(byte[] tag, Message message, Handler<ProtonDelivery> onReceived) {

        Delivery delivery = sender().delivery(tag); // start a new delivery..
        int BUFFER_SIZE = 1024;
        byte[] encodedMessage = new byte[BUFFER_SIZE];

        // protonj has a nice encode2 method which tells us what
        // encoded message length would be even if our buffer is too small.

        MessageImpl msg = (MessageImpl) message;
        int len = msg.encode2(encodedMessage, 0, BUFFER_SIZE);

        // looks like the message is bigger than our initial buffer, lets resize and try again.
        if( len > encodedMessage.length ) {
            encodedMessage = new byte[len];
            msg.encode(encodedMessage, 0, len);
        }
        sender().send(encodedMessage, 0, len);

        if( onReceived==null || link.getSenderSettleMode() == SenderSettleMode.SETTLED  ) {
            delivery.settle();
        }
        sender().advance(); // ends the delivery.
        getSession().getConnectionImpl().flush();

        new ProtonDeliveryImpl(delivery).handler(onReceived);
    }

    public void send(byte[] tag, Message message) {
        send(tag, message, null);
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
