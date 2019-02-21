/*
* Copyright 2016, 2017 the original author or authors.
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

import java.util.Set;

import io.vertx.codegen.annotations.DataObject;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.SSLEngineOptions;
import io.vertx.core.net.TrustOptions;

/**
 * Options for configuring {@link io.vertx.proton.ProtonServer} creation.
 */
@DataObject(generateConverter = true, publicConverter = false)
public class ProtonServerOptions extends NetServerOptions {

  private int heartbeat;
  private int maxFrameSize;

  public ProtonServerOptions() {
  }

  /**
   * Copy constructor
   *
   * @param other  the options to copy
   */
  public ProtonServerOptions(ProtonServerOptions other) {
    super(other);
    this.heartbeat = other.heartbeat;
    this.maxFrameSize = other.maxFrameSize;
  }

  /**
   * Create options from JSON
   *
   * @param json  the JSON
   */
  public ProtonServerOptions(JsonObject json) {
    super(json);
    ProtonServerOptionsConverter.fromJson(json, this);
  }

  /**
   * Convert to JSON
   *
   * @return the JSON
   */
  @Override
  public JsonObject toJson() {
    JsonObject json = super.toJson();
    ProtonServerOptionsConverter.toJson(this, json);
    return json;
  }

  @Override
  public ProtonServerOptions setSendBufferSize(int sendBufferSize) {
    super.setSendBufferSize(sendBufferSize);
    return this;
  }

  @Override
  public ProtonServerOptions setReceiveBufferSize(int receiveBufferSize) {
    super.setReceiveBufferSize(receiveBufferSize);
    return this;
  }

  @Override
  public ProtonServerOptions setReuseAddress(boolean reuseAddress) {
    super.setReuseAddress(reuseAddress);
    return this;
  }

  @Override
  public ProtonServerOptions setTrafficClass(int trafficClass) {
    super.setTrafficClass(trafficClass);
    return this;
  }

  @Override
  public ProtonServerOptions setTcpNoDelay(boolean tcpNoDelay) {
    super.setTcpNoDelay(tcpNoDelay);
    return this;
  }

  @Override
  public ProtonServerOptions setTcpKeepAlive(boolean tcpKeepAlive) {
    super.setTcpKeepAlive(tcpKeepAlive);
    return this;
  }

  @Override
  public ProtonServerOptions setSoLinger(int soLinger) {
    super.setSoLinger(soLinger);
    return this;
  }

  @Override
  public ProtonServerOptions setIdleTimeout(int idleTimeout) {
    super.setIdleTimeout(idleTimeout);
    return this;
  }

  @Override
  public ProtonServerOptions setSsl(boolean ssl) {
    super.setSsl(ssl);
    return this;
  }

  @Override
  public ProtonServerOptions setKeyStoreOptions(JksOptions options) {
    super.setKeyStoreOptions(options);
    return this;
  }

  @Override
  public ProtonServerOptions setPfxKeyCertOptions(PfxOptions options) {
    super.setPfxKeyCertOptions(options);
    return this;
  }

  @Override
  public ProtonServerOptions setPemKeyCertOptions(PemKeyCertOptions options) {
    super.setPemKeyCertOptions(options);
    return this;
  }

  @Override
  public ProtonServerOptions setTrustStoreOptions(JksOptions options) {
    super.setTrustStoreOptions(options);
    return this;
  }

  @Override
  public ProtonServerOptions setPfxTrustOptions(PfxOptions options) {
    super.setPfxTrustOptions(options);
    return this;
  }

  @Override
  public ProtonServerOptions setPemTrustOptions(PemTrustOptions options) {
    super.setPemTrustOptions(options);
    return this;
  }

  @Override
  public ProtonServerOptions addEnabledCipherSuite(String suite) {
    super.addEnabledCipherSuite(suite);
    return this;
  }

  @Override
  public ProtonServerOptions addCrlPath(String crlPath) throws NullPointerException {
    super.addCrlPath(crlPath);
    return this;
  }

  @Override
  public ProtonServerOptions addCrlValue(Buffer crlValue) throws NullPointerException {
    super.addCrlValue(crlValue);
    return this;
  }

  @Override
  public ProtonServerOptions setAcceptBacklog(int acceptBacklog) {
    super.setAcceptBacklog(acceptBacklog);
    return this;
  }

  @Override
  public ProtonServerOptions setPort(int port) {
    super.setPort(port);
    return this;
  }

  @Override
  public ProtonServerOptions setHost(String host) {
    super.setHost(host);
    return this;
  }

  @Override
  public ProtonServerOptions setClientAuth(ClientAuth clientAuth) {
    super.setClientAuth(clientAuth);
    return this;
  }

  @Override
  public int hashCode() {
    final int prime = 31;

    int result = super.hashCode();
    result = prime * result + this.heartbeat;
    result = prime * result + this.maxFrameSize;

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

    if (!super.equals(obj)) {
      return false;
    }

    ProtonServerOptions other = (ProtonServerOptions) obj;
    if (this.heartbeat != other.heartbeat) {
      return false;
    }
    if (this.maxFrameSize != other.maxFrameSize) {
      return false;
    }

    return true;
  }

  @Override
  public ProtonServerOptions setUseAlpn(boolean useAlpn) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ProtonServerOptions addEnabledSecureTransportProtocol(String protocol) {
    super.addEnabledSecureTransportProtocol(protocol);
    return this;
  }

  @Override
  public ProtonServerOptions removeEnabledSecureTransportProtocol(String protocol) {
    super.removeEnabledSecureTransportProtocol(protocol);
    return this;
  }

  @Override
  public ProtonServerOptions setEnabledSecureTransportProtocols(Set<String> enabledSecureTransportProtocols) {
    super.setEnabledSecureTransportProtocols(enabledSecureTransportProtocols);
    return this;
  }

  @Override
  public ProtonServerOptions setJdkSslEngineOptions(JdkSSLEngineOptions sslEngineOptions) {
    super.setJdkSslEngineOptions(sslEngineOptions);
    return this;
  }

  @Override
  public ProtonServerOptions setKeyCertOptions(KeyCertOptions options) {
    super.setKeyCertOptions(options);
    return this;
  }

  @Override
  public ProtonServerOptions setOpenSslEngineOptions(OpenSSLEngineOptions sslEngineOptions) {
    super.setOpenSslEngineOptions(sslEngineOptions);
    return this;
  }

  @Override
  public ProtonServerOptions setSslEngineOptions(SSLEngineOptions sslEngineOptions) {
    super.setSslEngineOptions(sslEngineOptions);
    return this;
  }

  @Override
  public ProtonServerOptions setTrustOptions(TrustOptions options) {
    super.setTrustOptions(options);
    return this;
  }

  @Override
  public ProtonServerOptions setLogActivity(boolean logEnabled) {
    super.setLogActivity(logEnabled);
    return this;
  }

  @Override
  public ProtonServerOptions setSni(boolean sni) {
    super.setSni(sni);
    return this;
  }

  @Override
  public ProtonServerOptions setReusePort(boolean reusePort) {
    super.setReusePort(reusePort);
    return this;
  }

  @Override
  public ProtonServerOptions setTcpFastOpen(boolean tcpFastOpen) {
    super.setTcpFastOpen(tcpFastOpen);
    return this;
  }

  @Override
  public ProtonServerOptions setTcpCork(boolean tcpCork) {
    super.setTcpCork(tcpCork);
    return this;
  }

  @Override
  public ProtonServerOptions setTcpQuickAck(boolean tcpQuickAck) {
    super.setTcpQuickAck(tcpQuickAck);
    return this;
  }

  /**
   * Sets the heart beat (in milliseconds) as maximum delay between sending frames for the remote peers.
   * If no frames are received within 2 * heart beat, the connection is closed.
   *
   * @param heartbeat heart beat maximum delay
   * @return current ProtonServerOptions instance
   */
  public ProtonServerOptions setHeartbeat(int heartbeat) {
    this.heartbeat = heartbeat;
    return this;
  }

  /**
   * Gets the heart beat (in milliseconds) as maximum delay between sending frames for the remote peers.
   *
   * @return heart beat maximum delay
   */
  public int getHeartbeat() {
    return this.heartbeat;
  }

  /**
   * Sets the maximum frame size for connections.
   * <p>
   * If this property is not set explicitly, a reasonable default value is used.
   * <p>
   * Setting this property to a negative value will result in no maximum frame size being announced at all.
   *
   * @param maxFrameSize The frame size in bytes.
   * @return This instance for setter chaining.
   */
  public ProtonServerOptions setMaxFrameSize(int maxFrameSize) {
    if (maxFrameSize < 0) {
      this.maxFrameSize = -1;
    } else {
      this.maxFrameSize = maxFrameSize;
    }
    return this;
  }

  /**
   * Gets the maximum frame size for connections.
   * <p>
   * If this property is not set explicitly, a reasonable default value is used.
   *
   * @return The frame size in bytes or -1 if no limit is set.
   */
  public int getMaxFrameSize() {
    return maxFrameSize;
  }
}
