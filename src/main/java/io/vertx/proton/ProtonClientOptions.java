package io.vertx.proton;

import io.vertx.core.net.NetClientOptions;

public class ProtonClientOptions extends NetClientOptions {
    //TODO: Use a delegate? Override methods to change return type?
    //TODO: Config for AMQP levle heartbeating /idle-timeout? Have
    //      that on the Connection instead?
}
