package io.vertx.proton;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.proton.impl.ProtonServerImpl;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public interface ProtonServer {

  /**
   * Create a ProtonServer instance with the given Vertx instance.
   *
   * @param vertx
   *          the vertx instance to use
   * @return the server instance
   */
  static ProtonServer create(Vertx vertx) {
    return new ProtonServerImpl(vertx);
  }

  /**
   * Create a ProtonServer instance with the given Vertx instance and options.
   *
   * @param vertx
   *          the vertx instance to use
   * @param options
   *          the options to use
   * @return the server instance
   */
  static ProtonServer create(Vertx vertx, ProtonServerOptions options) {
    return new ProtonServerImpl(vertx, options);
  }

  /**
   * The handler called when a new client connection is accepted.
   *
   * @param handler
   *          the handler
   * @return the server
   */
  ProtonServer connectHandler(Handler<ProtonConnection> handler);

  /**
   * Returns the current connectHandler.
   *
   * @return the handler
   */
  Handler<ProtonConnection> connectHandler();

  /**
   * Gets the actual port being listened on.
   *
   * @return the port
   */
  int actualPort();

  /**
   * Start listening on the given port and host interface, with the result handler called when the operation completes.
   *
   * @param port
   *          the port to listen on (may be 0 to auto-select port)
   * @param host
   *          the host interface to listen on (may be "0.0.0.0" to indicate all interfaces).
   * @param handler
   *          the result handler
   * @return the server
   */
  ProtonServer listen(int port, String host, Handler<AsyncResult<ProtonServer>> handler);

  /**
   * Start listening on the given port and host interface "0.0.0.0", with the result handler called when the operation
   * completes.
   *
   * @param port
   *          the port to listen on (may be 0 to auto-select port)
   * @param handler
   *          the result handler
   * @return the server
   */
  ProtonServer listen(int port, Handler<AsyncResult<ProtonServer>> handler);

  /**
   * Start listening on the host and port configured in the options used when creating the server, with the result
   * handler called when the operation completes.
   *
   * @param port
   *          the port to listen on (may be 0 to auto-select port)
   * @param host
   *          the host interface to listen on (may be "0.0.0.0" to indicate all interfaces).
   * @param handler
   *          the result handler
   * @return the server
   */
  ProtonServer listen(Handler<AsyncResult<ProtonServer>> handler);

  /**
   * Start listening on the given port, and host interface "0.0.0.0".
   *
   * @param port
   *          the port to listen on (may be 0 to auto-select port)
   * @return the server
   */
  ProtonServer listen(int port);

  /**
   * Start listening on the given port, and host interface.
   *
   * @param port
   *          the port to listen on (may be 0 to auto-select port)
   * @param host
   *          the host interface to listen on (may be "0.0.0.0" to indicate all interfaces).
   * @return the server
   */
  ProtonServer listen(int port, String host);

  /**
   * Start listening on the host and port configured in the options used when creating the server.
   *
   * @return the server
   */
  ProtonServer listen();

  /**
   * Closes the server and any currently open connections. May not complete until after method has returned.
   */
  void close();

  /**
   * Closes the server and any currently open connections, notifying the given handler when complete.
   *
   * @param handler
   *          the completion handler
   */
  void close(Handler<AsyncResult<Void>> handler);
}
