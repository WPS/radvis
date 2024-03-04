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

package de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepositoryFactory;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ManuellerImportFehlerRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.service.ManuellerNetzklassenImportAbbildungsService;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.service.ManuellerNetzklassenImportService;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.service.ManuellerNetzklassenImportUebernahmeService;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.schnittstelle.KnotenToGeoJsonConverter;
import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.schnittstelle.controller.ManuellerNetzklassenImportGuard;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
public class NetzzugehoerigkeitConfiguration {

	@Autowired
	private NetzService netzService;

	@Autowired
	VerwaltungseinheitService verwaltungseinheitService;

	@Autowired
	BenutzerResolver benutzerResolver;

	@Autowired
	private SimpleMatchingService simpleMatchingService;

	@Autowired
	private InMemoryKantenRepositoryFactory inMemoryKantenRepositoryFactory;

	@Autowired
	ManuellerImportFehlerRepository manuellerImportFehlerRepository;

	@Autowired
	private ManuellerImportService manuellerImportService;

	@Autowired
	private ShapeZipService shapeZipService;

	@Autowired
	private ShapeFileRepository shapeFileRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Bean
	public ManuellerNetzklassenImportAbbildungsService netzklassenAbbildungsService() {
		return new ManuellerNetzklassenImportAbbildungsService(simpleMatchingService,
			inMemoryKantenRepositoryFactory);
	}

	@Bean
	public ManuellerNetzklassenImportUebernahmeService manuellerNetzklassenImportUebernahmeService() {
		return new ManuellerNetzklassenImportUebernahmeService(netzService, entityManager);
	}

	@Bean
	public ManuellerNetzklassenImportService manuellerNetzklassenImportService() {
		return new ManuellerNetzklassenImportService(manuellerImportService, netzklassenAbbildungsService(),
			manuellerNetzklassenImportUebernahmeService(), shapeZipService, shapeFileRepository,
			manuellerImportFehlerRepository);
	}

	@Bean
	public KnotenToGeoJsonConverter knotenToGeoJsonConverter() {
		return new KnotenToGeoJsonConverter();
	}

	@Bean
	public ManuellerNetzklassenImportGuard manuellerNetzklassenImportGuard() {
		return new ManuellerNetzklassenImportGuard(benutzerResolver, verwaltungseinheitService);
	}

}
