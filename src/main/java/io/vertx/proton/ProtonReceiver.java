package io.vertx.proton;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonReceiver extends ProtonLink<ProtonReceiver> {

  ProtonReceiver handler(ProtonMessageHandler handler);

  /**
   * Sets the number of message credits the receiver grants and
   * replenishes automatically as messages are delivered.
   *
   * To manage credit manually, you can instead set prefetch to 0
   * before opening the consumer and then explicitly call
   * {@link #flow(int)} as needed to manually grant credit.
   *
   * @param messages the message prefetch
   * @return the receiver
   */
  ProtonReceiver setPrefetch(int messages);

  int getPrefetch();

  /**
   * Sets whether received deliveries should be automatically accepted
   * (and settled) after the message handler runs for them, if no other
   * disposition has been applied during handling.
   *
   * True by default.
   *
   * @param autoSettle whether deliveries should be auto accepted after handling if no disposition was applied
   * @return the receiver
   */
  ProtonReceiver setAutoAccept(boolean autoAccept);

  boolean isAutoAccept();

  /**
   * Grants the number of message credits to the sender.
   *
   * For use when {@link #setPrefetch(int)} has been used to disable
   * automatic prefetch credit handling.
   *
   * @param messages the credits to flow
   * @return the receiver
   */
  ProtonReceiver flow(int credits); // TODO: change method to alert existing uses they
                                    // generally don't need to use this now?
                                    // Could throw if prefetch is still enabled?
}
