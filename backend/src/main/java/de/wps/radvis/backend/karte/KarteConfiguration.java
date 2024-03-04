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

package de.wps.radvis.backend.karte;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.karte.domain.HintergrundKartenRepository;
import de.wps.radvis.backend.karte.domain.HintergrundKartenService;
import de.wps.radvis.backend.karte.domain.KarteConfigurationProperties;
import de.wps.radvis.backend.karte.schnittstelle.HintergrundKartenRepositoryImpl;
import lombok.NonNull;

@Configuration
public class KarteConfiguration {

	private final KarteConfigurationProperties karteConfigurationProperties;
	private final CommonConfigurationProperties commonConfigurationProperties;

	@Autowired
	private WebClient.Builder webClientBuilder;

	public KarteConfiguration(@NonNull KarteConfigurationProperties karteConfigurationProperties,
		@NonNull CommonConfigurationProperties commonConfigurationProperties) {
		this.karteConfigurationProperties = karteConfigurationProperties;
		this.commonConfigurationProperties = commonConfigurationProperties;
	}

	@Bean
	public HintergrundKartenRepository hintergrundKartenRepository() {
		return new HintergrundKartenRepositoryImpl(karteConfigurationProperties.getHintergrundKarten());
	}

	@Bean
	public HintergrundKartenService hintergrundKartenService() {
		return new HintergrundKartenService(hintergrundKartenRepository(), commonConfigurationProperties,
			webClientBuilder);
	}
}
