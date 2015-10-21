/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton.impl;

import io.vertx.proton.ProtonReceiver;
import io.vertx.proton.ProtonMessageHandler;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.Accepted;
import org.apache.qpid.proton.amqp.transport.Source;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.Receiver;
import org.apache.qpid.proton.message.Message;

import java.io.ByteArrayOutputStream;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ProtonReceiverImpl extends ProtonLinkImpl<ProtonReceiver> implements ProtonReceiver {
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
    protected ByteArrayOutputStream current = new ByteArrayOutputStream();

    void onDelivery() {
        if (this.handler == null) {
            return;
        }

        Receiver receiver = getReceiver();
        Delivery delivery = receiver.current();
        if( delivery!=null ) {

            int count;
            byte[] buffer = new byte[1024];
            while ((count = receiver.recv(buffer, 0, buffer.length)) > 0) {
                current.write(buffer, 0, count);

    //                if (current.size() > session.getMaxFrameSize()) {
    //                    throw new AmqpProtocolException("Frame size of " + current.size() + " larger than max allowed " + session.getMaxFrameSize());
    //                }
            }

            // Expecting more deliveries..
            if (count == 0) {
                return;
            }

            byte[] data = current.toByteArray();
            current.reset();

            Message msg = Proton.message();
            msg.decode(data, 0, data.length);
            delivery.disposition(new Accepted());
            // receiver.advance();

            handler.handle(this, new ProtonDeliveryImpl(delivery), msg);
        }
    }
}
