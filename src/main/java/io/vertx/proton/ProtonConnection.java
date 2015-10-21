package io.vertx.proton;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public interface ProtonConnection {

  ProtonSession session();

  void flush();

}
