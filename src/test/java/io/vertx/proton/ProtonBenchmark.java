/**
 * Copyright 2015 Red Hat, Inc.
 */
package io.vertx.proton;

import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.TestOptions;
import io.vertx.ext.unit.TestSuite;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.apache.qpid.proton.message.Message;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.vertx.proton.ProtonHelper.message;
import static io.vertx.proton.ProtonHelper.tag;

@RunWith(VertxUnitRunner.class)
public class ProtonBenchmark extends MockServerTestBase {

    static final long BENCHMARK_DURATION = 5000;

    public static void main(String[] args) {
        ProtonBenchmark benchmark = new ProtonBenchmark();
        TestSuite.create("benchmark")
            .before(x->benchmark.setup())
            .test("benchmark", x -> benchmark.benchmarkAtMostOnceSendThroughput(x))
            .after(x -> benchmark.tearDown())
            .run(new TestOptions().setTimeout(BENCHMARK_DURATION+10000));
    }

    @Test
    public void benchmarkAtLeastOnceSendThroughput(TestContext context) {
        Async async = context.async();
        connect(context, connection ->
        {
            ProtonSender sender =
                connection.session().open().sender()
                    .setQoS(ProtonQoS.AT_LEAST_ONCE)
                    .open();

            String name = "At Least Once Send Throughput";
            byte[] tag = tag("m1");
            Message message = message("drop", "Hello World");

            benchmark(BENCHMARK_DURATION, name, counter -> {
                sender.sendQueueDrainHandler(s -> {
                    while (!sender.sendQueueFull()) {
                        sender.send(tag, message, d -> {
                            counter.incrementAndGet();
                        });
                    }
                });
            }, () -> {
                connection.disconnect();
                async.complete();
            });
        });
    }

    @Test
    public void benchmarkAtMostOnceSendThroughput(TestContext context) {
        Async async = context.async();
        connect(context, connection ->
        {
            ProtonSender sender =
                connection.session().open().sender()
                    .setQoS(ProtonQoS.AT_MOST_ONCE)
                    .open();

            String name = "At Most Once Send Throughput";
            byte[] tag = tag("m1");
            Message message = message("drop", "Hello World");

            benchmark(BENCHMARK_DURATION, name, counter -> {
                sender.sendQueueDrainHandler(s -> {
                    while (!sender.sendQueueFull()) {
                        sender.send(tag, message);
                        counter.incrementAndGet();
                    }
                });
            }, () -> {
                connection.disconnect();
                async.complete();
            });
        });
    }

    @Test
    public void benchmarkRequestResponse(TestContext context) {
        Async async = context.async();
        connect(context, connection ->
        {
            ProtonSession session = connection.session().open();
            ProtonSender sender = session.sender().open();

            byte[] tag = tag("m1");
            Message message = message("echo", "Hello World");

            benchmark(BENCHMARK_DURATION, "Request Response Throughput", counter -> {

                session.createReceiver(MockServer.Addresses.echo.toString())
                    .handler((d, m)->{
                        counter.incrementAndGet();
                    })
                    .flow(10)
                    .open();

                sender.sendQueueDrainHandler(s -> {
                    while (!sender.sendQueueFull()) {
                        sender.send(tag, message);
                    }
                });
            }, () -> {
                connection.disconnect();
                async.complete();
            });
        });
    }


}
