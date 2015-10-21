package io.vertx.proton;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonReceiver extends ProtonLink<ProtonReceiver> {

  ProtonReceiver flow(int credits);

  ProtonReceiver handler(ProtonMessageHandler handler);
}
