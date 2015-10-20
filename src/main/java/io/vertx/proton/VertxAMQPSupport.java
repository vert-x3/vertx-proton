/**
 * Copyright 2015 Red Hat, Inc.
 */

package io.vertx.proton;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import org.apache.qpid.proton.Proton;
import org.apache.qpid.proton.amqp.messaging.AmqpValue;
import org.apache.qpid.proton.amqp.messaging.Source;
import org.apache.qpid.proton.amqp.messaging.Target;
import org.apache.qpid.proton.amqp.transport.ErrorCondition;
import org.apache.qpid.proton.message.Message;

import java.nio.charset.StandardCharsets;

/**
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class VertxAMQPSupport {

    static public Message message() {
        return Proton.message();
    }

    static public Message message(String body) {
        Message message = message();
        message.setBody(new AmqpValue(body));
        return message;
    }

    static public Target target(String address) {
        Target target = new Target();
        target.setAddress(address);
        return target;
    }

    static public Source source(String address) {
        Source source = new Source();
        source.setAddress(address);
        return source;
    }

    static public byte[] tag(String tag) {
        return tag.getBytes(StandardCharsets.UTF_8);
    }


    static <T> AsyncResult<T> future(T value, ErrorCondition err) {
        if (err.getCondition() != null) {
            return Future.failedFuture(err.toString());
        } else {
            return Future.succeededFuture(null);
        }
    }

}
