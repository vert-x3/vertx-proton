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
package io.vertx.proton;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.codegen.json.annotations.JsonGen;
import io.vertx.core.json.JsonObject;

/**
 * Options for configuring link attributes.
 */
@DataObject
@JsonGen(publicConverter = false)
public class ProtonLinkOptions {
    private String linkName;
    private boolean dynamic;

    public ProtonLinkOptions() {
    }

    /**
     * Create options from JSON
     *
     * @param json  the JSON
     */
    public ProtonLinkOptions(JsonObject json) {
      ProtonLinkOptionsConverter.fromJson(json, this);
    }

    /**
     * Convert to JSON
     *
     * @return the JSON
     */
    public JsonObject toJson() {
      JsonObject json = new JsonObject();
      ProtonLinkOptionsConverter.toJson(this, json);
      return json;
    }

    public ProtonLinkOptions setLinkName(String linkName) {
        this.linkName = linkName;
        return this;
    }

    public String getLinkName() {
        return linkName;
    }

    /**
     * Sets whether the link remote terminus to be used should indicate it is
     * 'dynamic', requesting the peer names it with a dynamic address.
     * The address provided by the peer can then be inspected using
     * {@link ProtonLink#getRemoteAddress()} (or inspecting the remote
     * terminus details directly) after the link has remotely opened.
     *
     * @param dynamic true if the link should request a dynamic terminus address
     * @return the options
     */
    public ProtonLinkOptions setDynamic(boolean dynamic) {
      this.dynamic = dynamic;
      return this;
    }

    public boolean isDynamic() {
      return dynamic;
    }
}
