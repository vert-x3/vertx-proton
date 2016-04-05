/**
 * = Vert.x Proton
 *
 * This component faciliates AMQP integrations for Vert.x by providing a thin wrapper around the
 * link:http://qpid.apache.org/[Apache Qpid] Proton AMQP 1.0 protocol engine.
 * 
 * == Using Vert.x Proton
 *
 * To use Vert.x Proton, add the following dependency to the _dependencies_ section of your build descriptor:
 *
 * * Maven (in your `pom.xml`):
 *
 * [source,xml,subs="+attributes"]
 * ----
 * <dependency>
 *   <groupId>${maven.groupId}</groupId>
 *   <artifactId>${maven.artifactId}</artifactId>
 *   <version>${maven.version}</version>
 * </dependency>
 * ----
 *
 * * Gradle (in your `build.gradle` file):
 *
 * [source,groovy,subs="+attributes"]
 * ----
 * compile ${maven.groupId}:${maven.artifactId}:${maven.version}
 * ----
 *
 * === Creating a connection
 *
 * Here is an example of connecting and then opening a connection, which can then be used
 * to create senders and receivers.
 *
 * [source,java]
 * ----
 * {@link examples.VertxProtonExamples#example1}
 * ----
 *
 * === Creating a sender
 * 
 * Here is an example of creating a sender and sending a message with it. The onUpdated handler
 * provided in the send call is invoked when disposition updates are received for the delivery,
 * with the example using this to print the delivery state and whether the delivery was settled.
 *
 * [source,java]
 * ----
 * {@link examples.VertxProtonExamples#example2}
 * ----
 * 
 * === Creating a receiver
 * 
 * Here is an example of creating a receiver, and setting a message handler to process the incoming
 * messages and their related delivery.
 *
 * [source,java]
 * ----
 * {@link examples.VertxProtonExamples#example3}
 * ----
 */
@Document(fileName = "index.adoc")
package io.vertx.proton;

import io.vertx.docgen.Document;
