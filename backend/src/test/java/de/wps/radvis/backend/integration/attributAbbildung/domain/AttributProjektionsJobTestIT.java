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

package de.wps.radvis.backend.integration.attributAbbildung.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.envers.repository.config.EnableEnversRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group6")
@EnableEnversRepositories(basePackageClasses = NetzConfiguration.class)
@EntityScan(basePackageClasses = { NetzConfiguration.class, OrganisationConfiguration.class,
	BenutzerConfiguration.class })
@EnableConfigurationProperties({ FeatureToggleProperties.class })
@ContextConfiguration(classes = { CommonConfiguration.class })
class AttributProjektionsJobTestIT extends DBIntegrationTestIT {
	@MockitoBean
	private CoordinateReferenceSystemConverter coordinateReferenceSystemConverter;
	@Mock
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Mock
	private AttributProjektionsService attributProjektionsService;
	@Mock
	private AttributeAnreicherungsService attributAnreicherungsService;
	@Mock
	private AttributProjektionsStatistikService attributProjektionsStatistikService;
	@Mock
	private KantenDublettenPruefungService kantenDublettenPruefungService;
	@Mock
	private NetzService netzService;
	@Mock
	private ImportedFeaturePersistentRepository importedFeaturePersistentRepository;

	@Mock
	private DLMConfigurationProperties dlmConfigurationProperties;

	@Mock
	private NetzfehlerRepository netzfehlerRepository;

	@PersistenceContext
	EntityManager entityManager;

	@Autowired
	private KantenRepository kantenRepository;

	private AttributProjektionsJob attributProjektionsJob;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		attributProjektionsJob = new AttributProjektionsJob(jobExecutionDescriptionRepository,
			attributProjektionsService, attributAnreicherungsService, attributProjektionsStatistikService,
			netzfehlerRepository, kantenDublettenPruefungService, netzService, importedFeaturePersistentRepository,
			dlmConfigurationProperties, entityManager, QuellSystem.RadNETZ);
	}

	@Test
	void testSetzeRadNETZKantenAlsGrundnetzKanten() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withDefaultValues().build();
		Kante kante2 = KanteTestDataProvider.withDefaultValues().build();
		Kante kante3 = KanteTestDataProvider.withDefaultValues().build();
		Kante kante4 = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ).isGrundnetz(false).build();
		Kante kante5 = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ).isGrundnetz(false).build();
		Kante kante6 = KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ).isGrundnetz(false).build();

		kantenRepository.saveAll(List.of(kante1, kante2, kante3, kante4, kante5, kante6));

		entityManager.flush();
		entityManager.clear();

		// Act
		attributProjektionsJob.setzeRadNETZKantenAlsGrundnetzKanten();

		// Assert

		List<Kante> kanten = kantenRepository.findKanteByQuelle(QuellSystem.RadNETZ)
			.collect(Collectors.toList());

		assertThat(kanten).hasSize(3);
		assertThat(kanten).allMatch(Kante::isGrundnetz);

	}

}
