/*
 * Copyright (C) 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.syndesis.connector.kafka;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.syndesis.connector.support.util.ConnectorOptions;
import io.syndesis.connector.support.util.KeyStoreHelper;
import io.syndesis.integration.component.proxy.ComponentProxyComponent;
import io.syndesis.integration.component.proxy.ComponentProxyCustomizer;
import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.component.kafka.KafkaConfiguration;
import org.apache.camel.support.PropertyBindingSupport;
import org.apache.camel.support.jsse.KeyManagersParameters;
import org.apache.camel.support.jsse.KeyStoreParameters;
import org.apache.camel.support.jsse.SSLContextParameters;
import org.apache.camel.support.jsse.TrustManagersParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KafkaConnectionCustomizer implements ComponentProxyCustomizer, CamelContextAware {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaConnectionCustomizer.class);
    private static final String CERTIFICATE_OPTION = "brokerCertificate";
    public static final ObjectMapper MAPPER = new ObjectMapper();
    private CamelContext camelContext;

    @Override
    public CamelContext getCamelContext() {
        return this.camelContext;
    }

    @Override
    public final void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }

    public KafkaConnectionCustomizer(CamelContext context) {
        super();
        setCamelContext(context);
    }

    @Override
    public void customize(ComponentProxyComponent component, Map<String, Object> options) {
        KafkaConfiguration configuration = new KafkaConfiguration();
        if (ConnectorOptions.extractOption(options, CERTIFICATE_OPTION) != null) {
            LOG.info("Setting SSLContextParameters configuration as a self-signed certificate was provided");
            SSLContextParameters sslContextParameters = createSSLContextParameters(
                ConnectorOptions.extractOption(options, CERTIFICATE_OPTION));
            configuration.setSslContextParameters(sslContextParameters);
            configuration.setSecurityProtocol("SSL");
            // If present, Kafka client 2.0 is using this parameter to verify host
            // we must set to blank to skip host verification
            configuration.setSslEndpointAlgorithm("");
        }

        String extraOptions = options.getOrDefault("extraOptions", "[]").toString();
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, String>> attributes = MAPPER.readValue(extraOptions, List.class);
            for (Map<String, String> attribute : attributes) {
                final String key = attribute.get("key").trim();
                if (!key.isEmpty()) {
                    final String value = attribute.get("value");
                    final String property = getCanonicalPropertyName(key);

                    //In case this is an attribute on KafkaConfiguration that can be set
                    //So the PropertyBindingSupport does its work below
                    options.put(property, value);

                    //In case this is an unknown attribute on KafkaConfiguration
                    configuration.getAdditionalProperties().put("additionalProperties." + property,
                        value);
                }
            }

            PropertyBindingSupport.bindProperties(this.getCamelContext(), configuration, options);
        } catch (JsonProcessingException e) {
            LOG.error(e.getMessage(), e);
        }
        options.put("configuration", configuration);
    }

    private static String getCanonicalPropertyName(final String name) {
        //If there are "." it means we have to convert to "Camel-alike" properties
        String property = name;
        while (property.contains(".")) {
            int i = property.indexOf('.');
            property = property.substring(0, i)
                           + property.substring(i + 1, i + 2).toUpperCase(Locale.ENGLISH)
                           + property.substring(i + 2);
        }
        return property;
    }


    private static SSLContextParameters createSSLContextParameters(String certificate) {
        KeyStoreHelper brokerKeyStoreHelper = new KeyStoreHelper(certificate, "brokerCertificate").store();

        KeyStoreParameters keyStore = createKeyStore(brokerKeyStoreHelper);
        KeyStoreParameters brokerStore = createKeyStore(brokerKeyStoreHelper);
        KeyManagersParameters kmp = createKeyManagerParameters(keyStore);
        TrustManagersParameters tmp = createTrustManagerParameters(brokerStore);

        SSLContextParameters scp = new SSLContextParameters();
        scp.setKeyManagers(kmp);
        scp.setTrustManagers(tmp);

        return scp;
    }

    private static KeyStoreParameters createKeyStore(KeyStoreHelper helper) {
        KeyStoreParameters keyStoreParams = new KeyStoreParameters();
        keyStoreParams.setResource(helper.getKeyStorePath());
        keyStoreParams.setPassword(helper.getPassword());
        return keyStoreParams;
    }

    private static KeyManagersParameters createKeyManagerParameters(KeyStoreParameters keyStore) {
        KeyManagersParameters keyManagersParams = new KeyManagersParameters();
        keyManagersParams.setKeyStore(keyStore);
        keyManagersParams.setKeyPassword(keyStore.getPassword());
        return keyManagersParams;
    }

    private static TrustManagersParameters createTrustManagerParameters(KeyStoreParameters keystore) {
        TrustManagersParameters trustManagersParams = new TrustManagersParameters();
        trustManagersParams.setKeyStore(keystore);
        return trustManagersParams;
    }
}
