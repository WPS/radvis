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

package de.wps.radvis.backend.integration.attributAbbildung;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributProjektionsService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributProjektionsStatistikService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributeAnreicherungsService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.AttributeProjektionsProtokollService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeMergeService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeUebertragungService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenDublettenPruefungService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingRepository;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingService;
import de.wps.radvis.backend.integration.radnetz.domain.RadNetzNetzbildungService;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungService;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import lombok.NonNull;

@Configuration
@EnableJpaRepositories
@EntityScan
public class IntegrationAttributAbbildungConfiguration {

	@Autowired
	private NetzfehlerRepository netzfehlerRepository;

	@Autowired
	private ImportedFeaturePersistentRepository importedFeaturePersistentRepository;

	@Autowired
	private KantenMappingRepository kantenMappingRepository;

	@Autowired
	private KantenRepository kantenRepository;

	private final NetzService netzService;

	private final RadNetzNetzbildungService radNetzNetzbildungService;

	private final RadwegeDBNetzbildungService radwegeDBNetzbildungService;

	private NetzConfigurationProperties netzConfigurationProperties;

	public IntegrationAttributAbbildungConfiguration(@NonNull NetzService netzService,
		@NonNull RadNetzNetzbildungService radNetzNetzbildungService,
		@NonNull RadwegeDBNetzbildungService radwegeDBNetzbildungService,
		NetzConfigurationProperties netzConfigurationProperties) {
		this.netzConfigurationProperties = netzConfigurationProperties;
		this.netzService = netzService;
		this.radNetzNetzbildungService = radNetzNetzbildungService;
		this.radwegeDBNetzbildungService = radwegeDBNetzbildungService;
	}

	@Bean
	public KantenDublettenPruefungService kantenDublettenPruefungService() {
		return new KantenDublettenPruefungService();
	}

	@Bean
	public KantenAttributeMergeService kantenAttributeMergeService() {
		return new KantenAttributeMergeService();
	}

	@Bean
	public AttributeAnreicherungsService attributeAnreicherungsService() {
		return new AttributeAnreicherungsService(kantenAttributeMergeService(), attributeProjektionsProtokollService());
	}

	@Bean
	public AttributProjektionsService attributProjektionsService() {
		return new AttributProjektionsService(attributeProjektionsProtokollService(), kantenMappingRepository);
	}

	@Bean
	public AttributeProjektionsProtokollService attributeProjektionsProtokollService() {
		return new AttributeProjektionsProtokollService(netzfehlerRepository);
	}

	@Bean
	public AttributProjektionsStatistikService attributProjektionsStatistikService() {
		return new AttributProjektionsStatistikService(attributeProjektionsProtokollService(), netzService,
			importedFeaturePersistentRepository, radNetzNetzbildungService, radwegeDBNetzbildungService);
	}

	@Bean
	public KantenMappingService kantenMappingService() {
		return new KantenMappingService(kantenMappingRepository, kantenRepository);
	}

	@Bean
	public KantenAttributeUebertragungService kantenAttributeUebertragungService() {
		return new KantenAttributeUebertragungService(netzConfigurationProperties.getMinimaleSegmentLaenge());
	}
}
