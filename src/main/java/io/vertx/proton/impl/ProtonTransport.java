/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton.impl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.engine.BaseHandler;
import org.apache.qpid.proton.engine.Collector;
import org.apache.qpid.proton.engine.Connection;
import org.apache.qpid.proton.engine.Event;
import org.apache.qpid.proton.engine.Transport;

import java.nio.ByteBuffer;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
class ProtonTransport extends BaseHandler {

    private final Connection connection;
    private final NetClient netClient;
    private final NetSocket socket;
    private final Transport transport = Proton.transport();
    private final Collector collector = Proton.collector();

    ProtonTransport(Connection connection, NetClient netClient, NetSocket socket) {
        this.connection = connection;
        this.netClient = netClient;
        this.socket = socket;
        transport.bind(connection);
        connection.collect(collector);
        socket.endHandler(this::handleSocketEnd);
        socket.handler(this::handleSocketBuffer);
    }

    private void handleSocketEnd(Void arg) {
        transport.unbind();
        transport.close();
        if( this.netClient!=null ) {
            this.netClient.close();
        } else {
            this.socket.close();
        }
        ((ProtonConnectionImpl) this.connection.getContext()).fireDisconnect();
    }

    private void handleSocketBuffer(Buffer buff) {

        pumpInbound(ByteBuffer.wrap(buff.getBytes()));
        Event protonEvent = null;
        while ((protonEvent = collector.peek()) != null) {
            ProtonConnectionImpl connnection = (ProtonConnectionImpl) protonEvent.getConnection().getContext();
            switch (protonEvent.getType()) {
                case CONNECTION_REMOTE_OPEN: {
                    connnection.fireRemoteOpen();
                    break;
                }
                case CONNECTION_REMOTE_CLOSE: {
                    connnection.fireRemoteClose();
                    break;
                }
                case SESSION_REMOTE_OPEN: {
                    ProtonSessionImpl session = (ProtonSessionImpl) protonEvent.getSession().getContext();
                    if( session == null ) {
                        connnection.fireRemoteSessionOpen(protonEvent.getSession());
                    } else {
                        session.fireRemoteOpen();
                    }
                    break;
                }
                case SESSION_REMOTE_CLOSE: {
                    ProtonSessionImpl session = (ProtonSessionImpl) protonEvent.getSession().getContext();
                    session.fireRemoteClose();
                    break;
                }
                case LINK_REMOTE_OPEN: {
                    ProtonLinkImpl link = (ProtonLinkImpl) protonEvent.getLink().getContext();
                    if( link == null ) {
                        connnection.fireRemoteLinkOpen(protonEvent.getLink());
                    } else {
                        link.fireRemoteOpen();
                    }
                    break;
                }
                case LINK_REMOTE_CLOSE: {
                    ProtonLinkImpl link = (ProtonLinkImpl) protonEvent.getLink().getContext();
                    link.fireRemoteClose();
                    break;
                }
                case LINK_FLOW:{
                    ProtonLinkImpl link = (ProtonLinkImpl) protonEvent.getLink().getContext();
                    link.fireLinkFlow();
                    break;
                }
                case DELIVERY: {
                    ProtonDeliveryImpl delivery = (ProtonDeliveryImpl) protonEvent.getDelivery().getContext();
                    if (delivery != null) {
                        delivery.fireUpdate();
                    } else {
                        ProtonReceiverImpl receiver = (ProtonReceiverImpl) protonEvent.getLink().getContext();
                        receiver.onDelivery();
                    }
                    break;
                }

                case CONNECTION_INIT:
                case CONNECTION_BOUND:
                case CONNECTION_UNBOUND:
                case CONNECTION_LOCAL_OPEN:
                case CONNECTION_LOCAL_CLOSE:
                case CONNECTION_FINAL:

                case SESSION_INIT:
                case SESSION_LOCAL_OPEN:
                case SESSION_LOCAL_CLOSE:
                case SESSION_FINAL:

                case LINK_INIT:
                case LINK_LOCAL_OPEN:
                case LINK_LOCAL_DETACH:
                case LINK_REMOTE_DETACH:
                case LINK_LOCAL_CLOSE:
                case LINK_FINAL:
            }
            collector.pop();
        }
        flush();
    }

    private void pumpInbound(ByteBuffer bytes) {
        // Lets push bytes from vert.x to proton engine.
        ByteBuffer inputBuffer = transport.getInputBuffer();
        while (bytes.hasRemaining() && inputBuffer.hasRemaining()) {
            inputBuffer.put(bytes.get());
            transport.processInput().checkIsOk();
        }
    }

    void flush() {
        boolean done = false;
        while (!done) {
            ByteBuffer outputBuffer = transport.getOutputBuffer();
            if (outputBuffer != null && outputBuffer.hasRemaining()) {
                byte buffer[] = new byte[outputBuffer.remaining()];
                outputBuffer.get(buffer);
                socket.write(Buffer.buffer(buffer));
                transport.outputConsumed();
            } else {
                done = true;
            }
        }
    }


    public void disconnect() {
        if (netClient != null) {
            netClient.close();
        } else {
            socket.close();
        }
    }
}
