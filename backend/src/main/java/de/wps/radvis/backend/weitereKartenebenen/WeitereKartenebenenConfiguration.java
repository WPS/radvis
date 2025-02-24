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

package de.wps.radvis.backend.weitereKartenebenen;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.weitereKartenebenen.domain.DateiLayerService;
import de.wps.radvis.backend.weitereKartenebenen.domain.WeitereKartenebenenConfigurationProperties;
import de.wps.radvis.backend.weitereKartenebenen.domain.WeitereKartenebenenService;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.DateiLayerRepository;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.GeoserverRepository;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.WeitereKartenebenenRepository;
import de.wps.radvis.backend.weitereKartenebenen.schnittstelle.DateiLayerGuard;
import de.wps.radvis.backend.weitereKartenebenen.schnittstelle.WeitereKartenebenenGuard;
import de.wps.radvis.backend.weitereKartenebenen.schnittstelle.repositoryImpl.GeoserverRepositoryImpl;

@Configuration
@EnableJpaRepositories
@EntityScan
public class WeitereKartenebenenConfiguration {
	@Autowired
	private DateiLayerRepository dateiLayerRepository;

	@Autowired
	private WeitereKartenebenenRepository weitereKartenebenenRepository;

	@Autowired
	private BenutzerResolver benutzerResolver;

	@Autowired
	private GeoJsonImportRepository geoJsonImportRepository;

	@Autowired
	private WeitereKartenebenenConfigurationProperties weitereKartenebenenConfigurationProperties;

	@Bean
	public GeoserverRepository geoserverRepository() {
		return new GeoserverRepositoryImpl(weitereKartenebenenConfigurationProperties, geoJsonImportRepository);
	}

	@Bean
	public DateiLayerGuard dateiLayerGuard() {
		return new DateiLayerGuard(benutzerResolver);
	}

	@Bean
	public DateiLayerService dateiLayerService() {
		return new DateiLayerService(dateiLayerRepository, weitereKartenebenenRepository, geoserverRepository());
	}

	@Bean
	public WeitereKartenebenenService weitereKartenebenenService() {
		return new WeitereKartenebenenService(weitereKartenebenenRepository);
	}

	@Bean
	public WeitereKartenebenenGuard weitereKartenebenenGuard() {
		return new WeitereKartenebenenGuard(benutzerResolver);
	}
}
