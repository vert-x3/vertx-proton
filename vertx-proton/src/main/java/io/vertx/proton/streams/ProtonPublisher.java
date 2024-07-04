/*
* Copyright 2018 the original author or authors.
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
package io.vertx.proton.streams;

import org.apache.qpid.proton.amqp.transport.Source;
import org.apache.qpid.proton.amqp.transport.Target;
import org.reactivestreams.Publisher;

/*
 * An AMQP consumer, presented as a reactive streams Publisher
 */
public interface ProtonPublisher<T> extends Publisher<T> {

  /**
   * Retrieves the address from the remote source details. Should only be called in callbacks such
   * as on onSubscribe() to ensure detail is populated and safe threading.
   *
   * @return the remote address, or null if there was none.
   */
  String getRemoteAddress();

  /**
   * Sets the local Source details. Only useful to call before subscribing.
   *
   * @param source
   *          the source
   * @return the publisher
   */
  ProtonPublisher<T> setSource(Source source);

  /**
   * Retrieves the local Source details for access or customisation.
   *
   * @return the local Source, or null if there was none.
   */
  Source getSource();

  /**
   * Sets the local Target details. Only useful to call before subscribing.
   *
   * @param target
   *          the target
   * @return the publisher
   */
  ProtonPublisher<T> setTarget(Target target);

  /**
   * Retrieves the local Target details for access or customisation.
   *
   * @return the local Target, or null if there was none.
   */
  Target getTarget();

  /**
   * Retrieves the remote Source details. Should only be called in callbacks such
   * as on onSubscribe() to ensure detail is populated and safe threading.
   *
   * @return the remote Source, or null if there was none.
   */
  Source getRemoteSource();

  /**
   * Retrieves the remote Target details. Should only be called in callbacks such
   * as on onSubscribe() to ensure detail is populated and safe threading.
   *
   * @return the remote Target, or null if there was none.
   */
  Target getRemoteTarget();
}