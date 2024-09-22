module io.vertx.proton {

  requires static io.vertx.codegen.api;
  requires static io.vertx.codegen.json;
  requires static io.vertx.docgen;

  requires io.netty.buffer;
  requires io.netty.handler;
  requires io.netty.transport;
  requires io.vertx.core.logging;
  requires io.vertx.core;
  requires org.apache.qpid.proton.j;
  requires static org.reactivestreams;
  requires java.security.sasl;

  exports io.vertx.proton;
  exports io.vertx.proton.streams;
  exports io.vertx.proton.sasl;

  exports io.vertx.proton.impl to io.vertx.proton.tests;
  exports io.vertx.proton.streams.impl to io.vertx.proton.tests;
  exports io.vertx.proton.sasl.impl to io.vertx.proton.tests;

}
