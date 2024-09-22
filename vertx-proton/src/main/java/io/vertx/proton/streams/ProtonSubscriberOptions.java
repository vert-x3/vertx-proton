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

import java.util.Objects;

/**
 * Options for configuring Subscriber attributes.
 */
public class ProtonSubscriberOptions {
  private String linkName;

  public ProtonSubscriberOptions() {
  }

  /**
   * Sets the link name to be used for the producer.
   *
   * @param linkName the name to use
   * @return the options
   */
  public ProtonSubscriberOptions setLinkName(String linkName) {
    this.linkName = linkName;
    return this;
  }

  public String getLinkName() {
    return linkName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;

    int result = 1;
    result = prime * result + Objects.hashCode(linkName);

    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (obj == null || getClass() != obj.getClass()){
      return false;
    }

    ProtonSubscriberOptions other = (ProtonSubscriberOptions) obj;
    if (!Objects.equals(linkName, other.linkName)){
      return false;
    }

    return true;
  }
}
