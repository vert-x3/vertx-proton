/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import org.apache.qpid.proton.message.Message;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public interface ProtonMessageHandler {

  /**
   * Handler to process messages and their related deliveries.
   *
   * @param delivery
   *          the delivery used to carry the message
   * @param message
   *          the message
   */
  void handle(ProtonDelivery delivery, Message message);
}