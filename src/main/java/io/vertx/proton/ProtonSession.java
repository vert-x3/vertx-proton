package io.vertx.proton;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonSession {

  ProtonSender sender(String name);

  ProtonSender sender(String name, String address);

  ProtonReceiver receiver(String name);

  ProtonReceiver receiver(String name, String address);
}
