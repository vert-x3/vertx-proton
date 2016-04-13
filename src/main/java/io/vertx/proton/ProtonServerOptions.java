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

import java.util.List;
import java.util.Set;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.NetServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.PfxOptions;
import io.vertx.core.net.TrustOptions;

/**
 * Options for configuring {@link io.vertx.proton.ProtonServer} creation.
 */
public class ProtonServerOptions extends NetServerOptions {

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
  public ProtonServerOptions setUsePooledBuffers(boolean usePooledBuffers) {
    super.setUsePooledBuffers(usePooledBuffers);
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
  public int getAcceptBacklog() {
    return super.getAcceptBacklog();
  }

  @Override
  public ProtonServerOptions setAcceptBacklog(int acceptBacklog) {
    super.setAcceptBacklog(acceptBacklog);
    return this;
  }

  @Override
  public int getPort() {
    return super.getPort();
  }

  @Override
  public ProtonServerOptions setPort(int port) {
    super.setPort(port);
    return this;
  }

  @Override
  public String getHost() {
    return super.getHost();
  }

  @Override
  public ProtonServerOptions setHost(String host) {
    super.setHost(host);
    return this;
  }

  @Override
  @Deprecated
  public boolean isClientAuthRequired() {
    return super.isClientAuthRequired();
  }

  @Override
  @Deprecated
  public ProtonServerOptions setClientAuthRequired(boolean clientAuthRequired) {
    super.setClientAuthRequired(clientAuthRequired);
    return this;
  }

  @Override
  public ClientAuth getClientAuth() {
    return super.getClientAuth();
  }

  @Override
  public ProtonServerOptions setClientAuth(ClientAuth clientAuth) {
    super.setClientAuth(clientAuth);
    return this;
  }

  @Override
  public boolean isTcpNoDelay() {
    return super.isTcpNoDelay();
  }

  @Override
  public boolean isTcpKeepAlive() {
    return super.isTcpKeepAlive();
  }

  @Override
  public int getSoLinger() {
    return super.getSoLinger();
  }

  @Override
  public boolean isUsePooledBuffers() {
    return super.isUsePooledBuffers();
  }

  @Override
  public int getIdleTimeout() {
    return super.getIdleTimeout();
  }

  @Override
  public boolean isSsl() {
    return super.isSsl();
  }

  @Override
  public KeyCertOptions getKeyCertOptions() {
    return super.getKeyCertOptions();
  }

  @Override
  public TrustOptions getTrustOptions() {
    return super.getTrustOptions();
  }

  @Override
  public Set<String> getEnabledCipherSuites() {
    return super.getEnabledCipherSuites();
  }

  @Override
  public List<String> getCrlPaths() {
    return super.getCrlPaths();
  }

  @Override
  public List<Buffer> getCrlValues() {
    return super.getCrlValues();
  }

  @Override
  public int getSendBufferSize() {
    return super.getSendBufferSize();
  }

  @Override
  public int getReceiveBufferSize() {
    return super.getReceiveBufferSize();
  }

  @Override
  public boolean isReuseAddress() {
    return super.isReuseAddress();
  }

  @Override
  public int getTrafficClass() {
    return super.getTrafficClass();
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return super.equals(obj);
  }
}
