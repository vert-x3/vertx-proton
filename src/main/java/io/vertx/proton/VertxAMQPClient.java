package io.vertx.proton;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.engine.Connection;

/**
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class VertxAMQPClient {

  private final Vertx vertx;
  private final String host;
  private final int port;
  private final NetClient netClient;

  public VertxAMQPClient(Vertx vertx, String host, int port) {
    this.vertx = vertx;
    this.host = host;
    this.port = port;
    Connection connection = Proton.connection();
    connection.setContainer("client-id:1");
    connection.open();
    this.netClient = vertx.createNetClient();
  }

  public void connect(Handler<AsyncResult<VertxAMQPConnection>> connection) {
    netClient.connect(port, host, res -> {
      if (res.succeeded()) {
        VertxAMQPConnection amqpConnection = new VertxAMQPConnection(res.result());
        connection.handle(Future.succeededFuture(amqpConnection));
      } else {
        connection.handle(Future.failedFuture(res.cause()));
      }
    });
  }
}
