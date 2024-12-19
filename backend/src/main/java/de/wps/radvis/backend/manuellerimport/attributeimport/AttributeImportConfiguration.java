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

package de.wps.radvis.backend.manuellerimport.attributeimport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.service.ShapeZipService;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.repository.ShapeFileAttributeRepository;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.service.AttributMapperFactory;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.service.ManuellerAttributeImportAbbildungsService;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.service.ManuellerAttributeImportService;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.service.ManuellerAttributeImportUebernahmeService;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.service.MappingService;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.FeatureMappingToGeoJsonConverter;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.KonfliktToGeoJsonConverter;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.controller.ManuellerAttributeImportGuard;
import de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.repositoryImpl.ShapeFileAttributeRepositoryImpl;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.InMemoryKantenRepositoryFactory;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ManuellerImportFehlerRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.service.ManuellerImportService;
import de.wps.radvis.backend.matching.domain.service.GrundnetzMappingService;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Configuration
public class AttributeImportConfiguration {
	@Autowired
	VerwaltungseinheitService verwaltungseinheitService;

	@Autowired
	BenutzerResolver benutzerResolver;

	@Autowired
	KantenRepository kantenRepository;

	@Autowired
	ManuellerImportFehlerRepository manuellerImportFehlerRepository;

	@Autowired
	private GrundnetzMappingService grundnetzMappingService;

	@Autowired
	private InMemoryKantenRepositoryFactory inMemoryKantenRepositoryFactory;

	@Autowired
	private ManuellerImportService manuellerImportService;

	@Autowired
	private ShapeZipService shapeZipService;

	@Autowired
	private ShapeFileRepository shapeFileRepository;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private NetzConfigurationProperties netzConfigurationProperties;

	@Bean
	public ManuellerAttributeImportAbbildungsService attributeAbbildungsService() {
		return new ManuellerAttributeImportAbbildungsService(inMemoryKantenRepositoryFactory,
			grundnetzMappingService);
	}

	@Bean
	public ManuellerAttributeImportUebernahmeService attributeUebernahmeService() {
		return new ManuellerAttributeImportUebernahmeService(inMemoryKantenRepositoryFactory,
			mappingService(), entityManager, netzConfigurationProperties.getMinimaleSegmentLaenge());
	}

	@Bean
	public MappingService mappingService() {
		return new MappingService();
	}

	@Bean
	public FeatureMappingToGeoJsonConverter featureMappingToGeoJsonConverter() {
		return new FeatureMappingToGeoJsonConverter();
	}

	@Bean
	public KonfliktToGeoJsonConverter konfliktToGeoJsonConverter() {
		return new KonfliktToGeoJsonConverter();
	}

	@Bean
	public ManuellerAttributeImportService attributeManuellerAttributeImportService() {
		return new ManuellerAttributeImportService(manuellerImportService, attributeAbbildungsService(),
			attributeUebernahmeService(), shapeZipService, shapeFileRepository,
			shapeFileAttributeRepository(), attributMapperFactory(), kantenRepository, manuellerImportFehlerRepository);
	}

	@Bean
	public ShapeFileAttributeRepository shapeFileAttributeRepository() {
		return new ShapeFileAttributeRepositoryImpl();
	}

	@Bean
	public AttributMapperFactory attributMapperFactory() {
		return new AttributMapperFactory(verwaltungseinheitService);
	}

	@Bean
	public ManuellerAttributeImportGuard manuellerAttributeImportGuard() {
		return new ManuellerAttributeImportGuard(benutzerResolver, verwaltungseinheitService);
	}
}
