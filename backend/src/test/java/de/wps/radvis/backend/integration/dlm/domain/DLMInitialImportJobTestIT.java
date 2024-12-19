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

package de.wps.radvis.backend.integration.dlm.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.barriere.BarriereConfiguration;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.integration.attributAbbildung.IntegrationAttributAbbildungConfiguration;
import de.wps.radvis.backend.integration.dlm.IntegrationDlmConfiguration;
import de.wps.radvis.backend.integration.radnetz.IntegrationRadNetzConfiguration;
import de.wps.radvis.backend.integration.radwegedb.IntegrationRadwegeDBConfiguration;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.ImportsGrundnetzConfiguration;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DlmRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group2")
@ContextConfiguration(classes = {
	IntegrationDlmConfiguration.class,
	NetzConfiguration.class,
	IntegrationAttributAbbildungConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	DLMInitialImportJobTestIT.TestConfiguration.class,
	BenutzerConfiguration.class,
	ImportsGrundnetzConfiguration.class,
	CommonConfiguration.class,
	IntegrationRadNetzConfiguration.class,
	IntegrationRadwegeDBConfiguration.class,
	MatchingConfiguration.class,
	BarriereConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	CommonConfigurationProperties.class,
	DLMConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	GraphhopperOsmConfigurationProperties.class,
	OsmPbfConfigurationProperties.class,
	GraphhopperDlmConfigurationProperties.class,
	NetzkorrekturConfigurationProperties.class,
	DLMConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
@ActiveProfiles("dev")
class DLMInitialImportJobTestIT extends DBIntegrationTestIT {

	public static class TestConfiguration {
		@Bean
		public NetzfehlerRepository netzfehlerRepository() {
			return Mockito.mock(NetzfehlerRepository.class);
		}

		@Bean
		public ImportedFeaturePersistentRepository importedFeaturePersistentRepository() {
			return Mockito.mock(ImportedFeaturePersistentRepository.class);
		}
	}

	private DLMInitialImportJob dlmInitialImportJob;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private NetzService netzService;
	@Autowired
	private CreateKantenService createKantenService;
	@Autowired
	private KnotenRepository knotenRepository;
	@Autowired
	private KantenRepository kantenRepository;
	@Autowired
	private BenutzerRepository benutzerRepository;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Mock
	private DlmRepository dlmWFSImportRepository;

	MockedStatic<AdditionalRevInfoHolder> auditingContextServiceMockedStatic;

	@BeforeEach
	void setup() {
		openMocks(this);

		// technischen Benutzer anlegen
		Benutzer technischerBenutzer = BenutzerTestDataProvider.technischerBenutzer().build();
		if (benutzerRepository.findByServiceBwId(technischerBenutzer.getServiceBwId()).isEmpty()) {
			gebietskoerperschaftRepository.save((Gebietskoerperschaft) technischerBenutzer.getOrganisation());
			benutzerRepository.save(technischerBenutzer);
		}

		dlmInitialImportJob = new DLMInitialImportJob(
			jobExecutionDescriptionRepository,
			dlmWFSImportRepository, netzService,
			entityManager,
			new de.wps.radvis.backend.integration.dlm.domain.InitialPartitionenImportService(
				createKantenService, dlmWFSImportRepository, netzService));

		auditingContextServiceMockedStatic = mockStatic(AdditionalRevInfoHolder.class);
	}

	@AfterEach
	void cleanUp() {
		auditingContextServiceMockedStatic.close();
	}

	@Test
	public void bereitsKantenVorhanden_sollFehlschlagen() {
		// arrange
		netzService.saveKante(KanteTestDataProvider.withDefaultValues().build());
		entityManager.flush();
		entityManager.clear();

		// act & assert
		assertThrows(RuntimeException.class, () -> dlmInitialImportJob.run());
	}

	@Test
	public void sollKantenUndKnotenImportieren() {
		// arrange
		Envelope bereich1 = new Envelope(0, 13, 0, 50);
		Envelope bereich2 = new Envelope(13, 50, 0, 50);
		List<Envelope> partitions = List.of(bereich1, bereich2);

		when(dlmWFSImportRepository.getPartitionen()).thenReturn(partitions);

		// Zu importierende features festlegen
		Map<String, Object> featureAttribute1 = new HashMap<>();
		featureAttribute1.put("eigenname", "bestehendeKante1"); // Strassenname
		Map<String, Object> featureAttribute2 = new HashMap<>();
		featureAttribute2.put("eigenname", "bestehendeKante2");
		Map<String, Object> featureAttribute3 = new HashMap<>();
		featureAttribute3.put("eigenname", "andererStrassenname3");

		ImportedFeature importedFeature1 = ImportedFeatureTestDataProvider.defaultWFSObject()
			.lineString(new Coordinate(0, 0), new Coordinate(10, 0))
			.fachId("1")
			.attribute(featureAttribute1)
			.build();
		ImportedFeature importedFeature2 = ImportedFeatureTestDataProvider.defaultWFSObject()
			.lineString(new Coordinate(10, 0), new Coordinate(20, 0))
			.fachId("2")
			.attribute(featureAttribute2)
			.build();
		ImportedFeature importedFeature3 = ImportedFeatureTestDataProvider.defaultWFSObject()
			.lineString(new Coordinate(20, 0), new Coordinate(30, 0))
			.fachId("3")
			.attribute(featureAttribute3)
			.build();

		when(dlmWFSImportRepository.getKanten(bereich1)).thenReturn(
			List.of(importedFeature1, importedFeature2));
		when(dlmWFSImportRepository.getKanten(bereich2)).thenReturn(
			List.of(importedFeature2, importedFeature3));

		// act
		dlmInitialImportJob.run();

		// assert
		assertThat(knotenRepository.findAll()).hasSize(4);
		List<Kante> dlmKanten = kantenRepository.findKanteByQuelle(QuellSystem.DLM).collect(Collectors.toList());
		assertThat(dlmKanten).hasSize(3);
		List<KantenAttribute> kantenAttribute = dlmKanten.stream().map(Kante::getKantenAttributGruppe)
			.map(KantenAttributGruppe::getKantenAttribute).collect(
				Collectors.toList());
		assertThat(kantenAttribute).map((ka) -> ka.getStrassenName().orElseThrow().getValue())
			.containsExactlyInAnyOrder("bestehendeKante1", "bestehendeKante2", "andererStrassenname3");
	}

	@Test
	public void fehlschlag_KantenUndKnotenAufgeraeumt() {
		// arrange
		Envelope bereich1 = new Envelope(0, 13, 0, 50);
		Envelope bereich2 = new Envelope(13, 50, 0, 50);
		List<Envelope> partitions = List.of(bereich1, bereich2);

		when(dlmWFSImportRepository.getPartitionen()).thenReturn(partitions);
		when(dlmWFSImportRepository.getKanten(bereich1)).thenReturn(
			List.of(ImportedFeatureTestDataProvider.defaultWFSObject().build()));
		when(dlmWFSImportRepository.getKanten(bereich2)).thenThrow(new RuntimeException());

		// act
		dlmInitialImportJob.run();

		// assert
		assertThat(knotenRepository.findAll()).hasSize(0);
		List<Kante> dlmKanten = kantenRepository.findKanteByQuelle(QuellSystem.DLM).collect(Collectors.toList());
		assertThat(dlmKanten).hasSize(0);
	}

}
