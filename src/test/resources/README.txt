#
# Copyright 2016 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# The various SSL stores and certificates were created with the following commands:

# Clean up existing files
# -----------------------
rm -f *.crt *.csr *.keystore *.truststore

# Create a key and self-signed certificate for the CA, to sign certificate requests and use for trust:
# ----------------------------------------------------------------------------------------------------
keytool -storetype pkcs12 -keystore ca-pkcs12.keystore -storepass password -keypass password -alias ca -genkey -dname "O=My Trusted Inc.,CN=my-vertx-ca.org" -validity 9999 -ext bc:c=ca:true
keytool -storetype pkcs12 -keystore ca-pkcs12.keystore -storepass password -alias ca -exportcert -rfc > ca.crt

# Create a key pair for the broker, and sign it with the CA:
# ----------------------------------------------------------
keytool -storetype pkcs12 -keystore broker-pkcs12.keystore -storepass password -keypass password -alias broker -genkey -dname "O=Server,CN=localhost" -validity 9999 -ext bc=ca:false -ext eku=sA

keytool -storetype pkcs12 -keystore broker-pkcs12.keystore -storepass password -alias broker -certreq -file broker.csr
keytool -storetype pkcs12 -keystore ca-pkcs12.keystore -storepass password -alias ca -gencert -rfc -infile broker.csr -outfile broker.crt -validity 9999 -ext bc=ca:false -ext eku=sA

keytool -storetype pkcs12 -keystore broker-pkcs12.keystore -storepass password -keypass password -importcert -alias ca -file ca.crt -noprompt
keytool -storetype pkcs12 -keystore broker-pkcs12.keystore -storepass password -keypass password -importcert -alias broker -file broker.crt

# Create trust store for the broker, import the CA cert:
# -------------------------------------------------------
keytool -storetype pkcs12 -keystore broker-pkcs12.truststore -storepass password -keypass password -importcert -alias ca -file ca.crt -noprompt

# Create a key pair for the client, and sign it with the CA:
# ----------------------------------------------------------
keytool -storetype pkcs12 -keystore client-pkcs12.keystore -storepass password -keypass password -alias client -genkey -dname "O=Client,CN=client" -validity 9999 -ext bc=ca:false -ext eku=cA

keytool -storetype pkcs12 -keystore client-pkcs12.keystore -storepass password -alias client -certreq -file client.csr
keytool -storetype pkcs12 -keystore ca-pkcs12.keystore -storepass password -alias ca -gencert -rfc -infile client.csr -outfile client.crt -validity 9999 -ext bc=ca:false -ext eku=cA

keytool -storetype pkcs12 -keystore client-pkcs12.keystore -storepass password -keypass password -importcert -alias ca -file ca.crt -noprompt
keytool -storetype pkcs12 -keystore client-pkcs12.keystore -storepass password -keypass password -importcert -alias client -file client.crt

# Create trust store for the client, import the CA cert:
# -------------------------------------------------------
keytool -storetype pkcs12 -keystore client-pkcs12.truststore -storepass password -keypass password -importcert -alias ca -file ca.crt -noprompt

# Create a truststore with self-signed certificate for an alternative CA, to
# allow 'failure to trust' of certs signed by the original CA above:
# ------------------------------------------------------------------
keytool -storetype pkcs12 -keystore other-ca-pkcs12.truststore -storepass password -keypass password -alias other-ca -genkey -dname "O=Other Trusted Inc.,CN=other-vertx-ca.org" -validity 9999 -ext bc:c=ca:true
keytool -storetype pkcs12 -keystore other-ca-pkcs12.truststore -storepass password -alias other-ca -exportcert -rfc > other-ca.crt
keytool -storetype pkcs12 -keystore other-ca-pkcs12.truststore -storepass password -alias other-ca -delete
keytool -storetype pkcs12 -keystore other-ca-pkcs12.truststore -storepass password -keypass password -importcert -alias other-ca -file other-ca.crt -noprompt

# Create a key pair for the broker with an unexpected hostname, and sign it with the CA:
# --------------------------------------------------------------------------------------
keytool -storetype pkcs12 -keystore broker-wrong-host-pkcs12.keystore -storepass password -keypass password -alias broker-wrong-host -genkey -dname "O=Server,CN=wronghost" -validity 9999 -ext bc=ca:false -ext eku=sA

keytool -storetype pkcs12 -keystore broker-wrong-host-pkcs12.keystore -storepass password -alias broker-wrong-host -certreq -file broker-wrong-host.csr
keytool -storetype pkcs12 -keystore ca-pkcs12.keystore -storepass password -alias ca -gencert -rfc -infile broker-wrong-host.csr -outfile broker-wrong-host.crt -validity 9999 -ext bc=ca:false -ext eku=sA

keytool -storetype pkcs12 -keystore broker-wrong-host-pkcs12.keystore -storepass password -keypass password -importcert -alias ca -file ca.crt -noprompt
keytool -storetype pkcs12 -keystore broker-wrong-host-pkcs12.keystore -storepass password -keypass password -importcert -alias broker-wrong-host -file broker-wrong-host.crt
