/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import org.apache.qpid.proton.message.Message;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public interface VertxAMQPMessageHandler {
    void handle(VertxAMQPReceiver receiver, VertxAMQPDelivery deliver, Message message);
}
