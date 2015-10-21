package io.vertx.proton;

import io.vertx.proton.impl.ProtonConnectionImpl;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public interface ProtonConnection {

  ProtonConnection setContainer(String container);

  ProtonSession session();

}
