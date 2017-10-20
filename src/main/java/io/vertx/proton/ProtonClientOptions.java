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

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.core.net.SSLEngineOptions;
import io.vertx.core.net.TrustOptions;

/**
 * Options for configuring {@link io.vertx.proton.ProtonClient} connect operations.
 */
public class ProtonClientOptions extends NetClientOptions {

  private Set<String> enabledSaslMechanisms = new LinkedHashSet<>();

  private int heartbeat;
  private int maxFrameSize;
  private String virtualHost;
  private String sniServerName;

  public ProtonClientOptions() {
    super();
    setHostnameVerificationAlgorithm("HTTPS");
  }

  /**
   * Copy constructor
   *
   * @param other  the options to copy
   */
  public ProtonClientOptions(ProtonClientOptions other) {
    super(other);
    this.heartbeat = other.heartbeat;
    this.maxFrameSize = other.maxFrameSize;
    this.virtualHost = other.virtualHost;
    this.sniServerName = other.sniServerName;
  }

  /**
   * Get the mechanisms the client should be restricted to use.
   *
   * @return the mechanisms, or null/empty set if there is no restriction in place
   */
  public Set<String> getEnabledSaslMechanisms() {
    return enabledSaslMechanisms;
  }

  /**
   * Adds a mechanism name that the client may use during SASL negotiation.
   *
   * @param saslMechanism
   *          the sasl mechanism name .
   * @return a reference to this, so the API can be used fluently
   */
  public ProtonClientOptions addEnabledSaslMechanism(String saslMechanism) {
    Objects.requireNonNull(saslMechanism, "Mechanism must not be null");
    enabledSaslMechanisms.add(saslMechanism);
    return this;
  }

  @Override
  public ProtonClientOptions setSendBufferSize(int sendBufferSize) {
    super.setSendBufferSize(sendBufferSize);
    return this;
  }

  @Override
  public ProtonClientOptions setReceiveBufferSize(int receiveBufferSize) {
    super.setReceiveBufferSize(receiveBufferSize);
    return this;
  }

  @Override
  public ProtonClientOptions setReuseAddress(boolean reuseAddress) {
    super.setReuseAddress(reuseAddress);
    return this;
  }

  @Override
  public ProtonClientOptions setTrafficClass(int trafficClass) {
    super.setTrafficClass(trafficClass);
    return this;
  }

  @Override
  public ProtonClientOptions setTcpNoDelay(boolean tcpNoDelay) {
    super.setTcpNoDelay(tcpNoDelay);
    return this;
  }

  @Override
  public ProtonClientOptions setTcpKeepAlive(boolean tcpKeepAlive) {
    super.setTcpKeepAlive(tcpKeepAlive);
    return this;
  }

  @Override
  public ProtonClientOptions setSoLinger(int soLinger) {
    super.setSoLinger(soLinger);
    return this;
  }

  @Override
  public ProtonClientOptions setUsePooledBuffers(boolean usePooledBuffers) {
    super.setUsePooledBuffers(usePooledBuffers);
    return this;
  }

  @Override
  public ProtonClientOptions setIdleTimeout(int idleTimeout) {
    super.setIdleTimeout(idleTimeout);
    return this;
  }

  @Override
  public ProtonClientOptions setSsl(boolean ssl) {
    super.setSsl(ssl);
    return this;
  }

  @Override
  public ProtonClientOptions setKeyStoreOptions(JksOptions options) {
    super.setKeyStoreOptions(options);
    return this;
  }

  @Override
  public ProtonClientOptions setPfxKeyCertOptions(PfxOptions options) {
    super.setPfxKeyCertOptions(options);
    return this;
  }

  @Override
  public ProtonClientOptions setPemKeyCertOptions(PemKeyCertOptions options) {
    super.setPemKeyCertOptions(options);
    return this;
  }

  @Override
  public ProtonClientOptions setTrustStoreOptions(JksOptions options) {
    super.setTrustStoreOptions(options);
    return this;
  }

  @Override
  public ProtonClientOptions setPemTrustOptions(PemTrustOptions options) {
    super.setPemTrustOptions(options);
    return this;
  }

  @Override
  public ProtonClientOptions setPfxTrustOptions(PfxOptions options) {
    super.setPfxTrustOptions(options);
    return this;
  }

  @Override
  public ProtonClientOptions addEnabledCipherSuite(String suite) {
    super.addEnabledCipherSuite(suite);
    return this;
  }

  @Override
  public ProtonClientOptions addCrlPath(String crlPath) throws NullPointerException {
    super.addCrlPath(crlPath);
    return this;
  }

  @Override
  public ProtonClientOptions addCrlValue(Buffer crlValue) throws NullPointerException {
    super.addCrlValue(crlValue);
    return this;
  }

  @Override
  public ProtonClientOptions setTrustAll(boolean trustAll) {
    super.setTrustAll(trustAll);
    return this;
  }

  @Override
  public ProtonClientOptions setConnectTimeout(int connectTimeout) {
    super.setConnectTimeout(connectTimeout);
    return this;
  }

  @Override
  public ProtonClientOptions setReconnectAttempts(int attempts) {
    super.setReconnectAttempts(attempts);
    return this;
  }

  @Override
  public ProtonClientOptions setReconnectInterval(long interval) {
    super.setReconnectInterval(interval);
    return this;
  }

  @Override
  public int hashCode() {
    final int prime = 31;

    int result = super.hashCode();
    result = prime * result + Objects.hashCode(enabledSaslMechanisms);
    result = prime * result + this.heartbeat;
    result = prime * result + this.maxFrameSize;
    result = prime * result + (this.virtualHost != null ? this.virtualHost.hashCode() : 0);
    result = prime * result + (this.sniServerName != null ? this.sniServerName.hashCode() : 0);

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

    ProtonClientOptions other = (ProtonClientOptions) obj;
    if (!Objects.equals(enabledSaslMechanisms, other.enabledSaslMechanisms)){
      return false;
    }
    if (this.heartbeat != other.heartbeat) {
      return false;
    }
    if (this.maxFrameSize != other.maxFrameSize) {
      return false;
    }
    if (!Objects.equals(this.virtualHost, other.virtualHost)) {
      return false;
    }
    if (!Objects.equals(this.sniServerName, other.sniServerName)) {
      return false;
    }

    return true;
  }

  @Override
  public ProtonClientOptions setUseAlpn(boolean useAlpn) {
    throw new UnsupportedOperationException();
  }

  @Override
  public ProtonClientOptions addEnabledSecureTransportProtocol(String protocol) {
    super.addEnabledSecureTransportProtocol(protocol);
    return this;
  }

  @Override
  public ProtonClientOptions setHostnameVerificationAlgorithm(String hostnameVerificationAlgorithm) {
    super.setHostnameVerificationAlgorithm(hostnameVerificationAlgorithm);
    return this;
  }

  @Override
  public ProtonClientOptions setKeyCertOptions(KeyCertOptions options) {
    super.setKeyCertOptions(options);
    return this;
  }

  @Override
  public ProtonClientOptions setLogActivity(boolean logEnabled) {
    super.setLogActivity(logEnabled);
    return this;
  }

  @Override
  public ProtonClientOptions setMetricsName(String metricsName) {
    super.setMetricsName(metricsName);
    return this;
  }

  @Override
  public ProtonClientOptions setProxyOptions(ProxyOptions proxyOptions) {
    super.setProxyOptions(proxyOptions);
    return this;
  }

  @Override
  public ProtonClientOptions setTrustOptions(TrustOptions options) {
    super.setTrustOptions(options);
    return this;
  }

  @Override
  public ProtonClientOptions setJdkSslEngineOptions(JdkSSLEngineOptions sslEngineOptions) {
    super.setJdkSslEngineOptions(sslEngineOptions);
    return this;
  }

  @Override
  public ProtonClientOptions setOpenSslEngineOptions(OpenSSLEngineOptions sslEngineOptions) {
    super.setOpenSslEngineOptions(sslEngineOptions);
    return this;
  }

  @Override
  public ProtonClientOptions setSslEngineOptions(SSLEngineOptions sslEngineOptions) {
    super.setSslEngineOptions(sslEngineOptions);
    return this;
  }

  @Override
  public ProtonClientOptions setLocalAddress(String localAddress) {
    super.setLocalAddress(localAddress);
    return this;
  }

  @Override
  public ProtonClientOptions setReusePort(boolean reusePort) {
    super.setReusePort(reusePort);
    return this;
  }

  @Override
  public ProtonClientOptions setTcpCork(boolean tcpCork) {
    super.setTcpCork(tcpCork);
    return this;
  }

  @Override
  public ProtonClientOptions setTcpFastOpen(boolean tcpFastOpen) {
    super.setTcpFastOpen(tcpFastOpen);
    return this;
  }

  @Override
  public ProtonClientOptions setTcpQuickAck(boolean tcpQuickAck) {
    super.setTcpQuickAck(tcpQuickAck);
    return this;
  }

  /**
   * Override the hostname value used in the connection AMQP Open frame and TLS SNI server name (if TLS is in use).
   * By default, the hostname specified in {@link ProtonClient#connect} will be used for both, with SNI performed
   * implicit where a FQDN was specified.
   *
   * The SNI server name can also be overridden explicitly using {@link #setSniServerName(String)}.
   *
   * @param virtualHost hostname to set
   * @return  current ProtonClientOptions instance
   */
  public ProtonClientOptions setVirtualHost(String virtualHost) {
    this.virtualHost = virtualHost;
    return this;
  }

  /**
   * Get the virtual host name override for the connection AMQP Open frame and TLS SNI server name (if TLS is in use)
   * set by {@link #setVirtualHost(String)}.
   *
   * @return  the hostname
   */
  public String getVirtualHost() {
    return this.virtualHost;
  }

  /**
   * Explicitly override the hostname to use for the TLS SNI server name.
   *
   * If neither the {@link ProtonClientOptions#setVirtualHost(String) virtualhost} or SNI server name is explicitly
   * overridden, the hostname specified in {@link ProtonClient#connect} will be used, with SNI performed implicitly
   * where a FQDN was specified.
   *
   * This method should typically only be needed to set different values for the [virtual] hostname and SNI server name.
   *
   * @param sniServerName hostname to set as SNI server name
   * @return  current ProtonClientOptions instance
   */
  public ProtonClientOptions setSniServerName(String sniServerName) {
    this.sniServerName = sniServerName;
    return this;
  }

  /**
   * Get the hostname override for TLS SNI Server Name set by {@link #setSniServerName(String)}.
   *
   * @return  the hostname
   */
  public String getSniServerName() {
    return this.sniServerName;
  }

  /**
   * Set the heartbeat (in milliseconds) as maximum delay between sending frames for the remote peers.
   * If no frames are received within 2*heartbeat, the connection is closed
   *
   * @param heartbeat hearthbeat maximum delay
   * @return  current ProtonClientOptions instance
   */
  public ProtonClientOptions setHeartbeat(int heartbeat) {
    this.heartbeat = heartbeat;
    return this;
  }

  /**
   * Return the heartbeat (in milliseconds) as maximum delay between sending frames for the remote peers.
   *
   * @return  hearthbeat maximum delay
   */
  public int getHeartbeat() {
    return this.heartbeat;
  }

  /**
   * Sets the maximum frame size to announce in the AMQP <em>OPEN</em> frame.
   * <p>
   * If this property is not set explicitly, a reasonable default value is used.
   * <p>
   * Setting this property to a negative value will result in no maximum frame size being announced at all.
   * 
   * @param maxFrameSize The frame size in bytes.
   * @return This instance for setter chaining.
   */
  public ProtonClientOptions setMaxFrameSize(int maxFrameSize) {
    if (maxFrameSize < 0) {
      this.maxFrameSize = -1;
    } else {
      this.maxFrameSize = maxFrameSize;
    }
    return this;
  }

  /**
   * Gets the maximum frame size to announce in the AMQP <em>OPEN</em> frame.
   * <p>
   * If this property is not set explicitly, a reasonable default value is used.
   * 
   * @return The frame size in bytes or -1 if no limit is set.
   */
  public int getMaxFrameSize() {
    return maxFrameSize;
  }
}
