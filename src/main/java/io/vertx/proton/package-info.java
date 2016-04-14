/*
* Copyright 2016 the original author or authors.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

/**
 * = Vert.x Proton
 *
 * This component facilitates AMQP integrations for Vert.x by providing a thin wrapper around the
 * link:http://qpid.apache.org/[Apache Qpid] Proton AMQP 1.0 protocol engine.
 *
 * WARNING: this module has the tech preview status, this means the API can change between versions. It also
 *          exposes Proton classes directly because it is not an abstraction layer of an AMQP client, it is rather
 *          an integration layer to make Proton integrated with Vert.x and its threading model as well as
 *          networking layer.
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
 * Here is an example of connecting and then opening a connection, which can then be used to create senders and
 * receivers.
 *
 * [source,java]
 * ----
 * {@link examples.VertxProtonExamples#example1}
 * ----
 *
 * === Creating a sender
 * 
 * Here is an example of creating a sender and sending a message with it. The onUpdated handler provided in the send
 * call is invoked when disposition updates are received for the delivery, with the example using this to print the
 * delivery state and whether the delivery was settled.
 *
 * [source,java]
 * ----
 * {@link examples.VertxProtonExamples#example2}
 * ----
 * 
 * === Creating a receiver
 * 
 * Here is an example of creating a receiver, and setting a message handler to process the incoming messages and their
 * related delivery.
 *
 * [source,java]
 * ----
 * {@link examples.VertxProtonExamples#example3}
 * ----
 */
@Document(fileName = "index.adoc")
package io.vertx.proton;

import io.vertx.docgen.Document;
