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
public interface ProtonHelper {

    public static Message message() {
        return Proton.message();
    }

    public  static Message message(String body) {
        Message value = message();
        value.setBody(new AmqpValue(body));
        return value;
    }

    public  static Message message(String address, String body) {
        Message value = message(body);
        value.setAddress(address);
        return value;
    }

    public static byte[] tag(String tag) {
        return tag.getBytes(StandardCharsets.UTF_8);
    }


    static <T> AsyncResult<T> future(T value, ErrorCondition err) {
        if (err.getCondition() != null) {
            return Future.failedFuture(err.toString());
        } else {
            return Future.succeededFuture(null);
        }
    }

    public static Target target(String address) {
        Target value = new Target();
        value.setAddress(address);
        return value;
    }

    public static Source source(String address) {
        Source value = new Source();
        value.setAddress(address);
        return value;
    }

}
