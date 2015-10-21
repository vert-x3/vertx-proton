package io.vertx.proton;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonReceiver {

  int recv(byte[] bytes, int offset, int size);

  ProtonReceiver drain(int credit);

  ProtonReceiver flow(int credits);

  boolean draining();

  ProtonReceiver setDrain(boolean drain);

  ProtonReceiver handler(ProtonMessageHandler handler);
}
