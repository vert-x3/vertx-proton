/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton.impl;

import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonMessageHandler;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.message.Message;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonReceiverImpl extends ProtonLink<ProtonReceiverImpl> implements ProtonReceiver {
    private ProtonMessageHandler handler;

    ProtonReceiverImpl(Receiver receiver) {
        super(receiver);
    }

    @Override
    protected ProtonReceiverImpl self() {
        return this;
    }

    private Receiver getReceiver() {
        return (Receiver) link;
    }

    public int recv(byte[] bytes, int offset, int size) {
        return getReceiver().recv(bytes, offset, size);
    }

    public ProtonReceiver drain(int credit) {
        getReceiver().drain(credit);
        return this;
    }

    public ProtonReceiver flow(int credits) {
        getReceiver().flow(credits);
        return this;
    }

    public boolean draining() {
        return getReceiver().draining();
    }

    public ProtonReceiver setDrain(boolean drain) {
        getReceiver().setDrain(drain);
        return this;
    }

    public ProtonReceiver handler(ProtonMessageHandler handler) {
        this.handler = handler;
        onDelivery();
        return this;
    }


    /////////////////////////////////////////////////////////////////////////////
    //
    // Implementation details hidden from public api.
    //
    /////////////////////////////////////////////////////////////////////////////
    void onDelivery() {
        if (this.handler == null) {
            return;
        }

        Receiver receiver = getReceiver();
        Delivery delivery = receiver.current();
        if (delivery != null && delivery.isReadable() && !delivery.isPartial()) {
            // TODO: properly account for unknown message size, ensure we recv all bytes
            int BUFFER_SIZE = 1024;
            byte[] encodedMessage = new byte[BUFFER_SIZE];
            int count = receiver.recv(encodedMessage, 0, BUFFER_SIZE);

            Message msg = Proton.message();
            msg.decode(encodedMessage, 0, count);
            delivery.disposition(new Accepted());
            handler.handle(this, new ProtonDeliveryImpl(delivery), msg);
        }
    }
}
