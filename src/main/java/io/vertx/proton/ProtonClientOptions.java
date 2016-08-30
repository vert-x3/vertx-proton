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

  public ProtonClientOptions() {
    super();
    setHostnameVerificationAlgorithm("HTTPS");
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
}
