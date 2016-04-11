package io.vertx.proton;

import java.lang.IllegalStateException;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonReceiver extends ProtonLink<ProtonReceiver> {

  /**
   * Sets the handler to process messages as they arrive. Should be set before opening unless prefetch is disabled and
   * credit is being manually controlled.
   *
   * @param handler
   *          the handler to process messages
   * @return the receiver
   */
  ProtonReceiver handler(ProtonMessageHandler handler);

  /**
   * Sets the number of message credits the receiver grants and replenishes automatically as messages are delivered.
   *
   * To manage credit manually, you can instead set prefetch to 0 before opening the consumer and then explicitly call
   * {@link #flow(int)} as needed to manually grant credit.
   *
   * @param messages
   *          the message prefetch
   * @return the receiver
   */
  ProtonReceiver setPrefetch(int messages);

  /**
   * Get the current prefetch value.
   *
   * @return the prefetch
   * @see #setPrefetch(int)
   */
  int getPrefetch();

  /**
   * Sets whether received deliveries should be automatically accepted (and settled) after the message handler runs for
   * them, if no other disposition has been applied during handling.
   *
   * True by default.
   *
   * @param autoSettle
   *          whether deliveries should be auto accepted after handling if no disposition was applied
   * @return the receiver
   */
  ProtonReceiver setAutoAccept(boolean autoAccept);

  /**
   * Get whether the receiver is auto accepting.
   *
   * @return whether deliveries are being auto accepted after handling if no disposition was applied
   * @see #setAutoAccept(boolean)
   */
  boolean isAutoAccept();

  /**
   * Grants the given number of message credits to the sender.
   *
   * For use when {@link #setPrefetch(int)} has been used to disable automatic prefetch credit handling.
   *
   * @param messages
   *          the credits to flow
   * @return the receiver
   * @throws IllegalStateException
   *           if prefetch is non-zero.
   */
  ProtonReceiver flow(int credits) throws IllegalStateException;
}
