package io.vertx.proton;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonReceiver {
  
  ProtonReceiver flow(int credits);

  ProtonReceiver handler(ProtonMessageHandler handler);
}
