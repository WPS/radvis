/*
 * Copyright (c) 2023 WPS - Workplace Solutions GmbH
 *
 * Licensed under the EUPL, Version 1.2 or as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * https://joinup.ec.europa.eu/collection/eupl/eupl-text-eupl-12
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */

package de.wps.radvis.backend.ortssuche;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.ortssuche.domain.OrtsSucheConfigurationProperties;
import de.wps.radvis.backend.ortssuche.domain.OrtssucheRepository;
import de.wps.radvis.backend.ortssuche.schnittstelle.OrtssucheRepositoryImpl;
import lombok.NonNull;

@Configuration
public class OrtsSucheConfiguration {

	private final OrtsSucheConfigurationProperties ortsSucheConfigurationProperties;
	private final RestTemplate restTemplate;
	private final GeoConverterConfiguration geoConverterConfiguration;

	public OrtsSucheConfiguration(
		@NonNull GeoConverterConfiguration geoConverterConfiguration,
		@NonNull OrtsSucheConfigurationProperties ortsSucheConfigurationProperties,
		@NonNull CommonConfigurationProperties configurationProperties) {

		this.geoConverterConfiguration = geoConverterConfiguration;
		this.ortsSucheConfigurationProperties = ortsSucheConfigurationProperties;

		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

		if (configurationProperties.getProxyAdress() != null) {
			Proxy proxy = new Proxy(Type.HTTP,
				new InetSocketAddress(configurationProperties.getProxyAdress(),
					configurationProperties.getProxyPort()));
			requestFactory.setProxy(proxy);
			restTemplate = new RestTemplate(requestFactory);
		} else {
			restTemplate = new RestTemplate();
		}
	}

	@Bean
	public OrtssucheRepository ortssucheRepository() {
		return new OrtssucheRepositoryImpl(geoConverterConfiguration.coordinateReferenceSystemConverter(),
			restTemplate,
			ortsSucheConfigurationProperties);
	}
}
