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

package de.wps.radvis.backend.quellimport.grundnetz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.barriere.BarriereConfiguration;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.integration.attributAbbildung.IntegrationAttributAbbildungConfiguration;
import de.wps.radvis.backend.integration.grundnetz.IntegrationGrundnetzConfiguration;
import de.wps.radvis.backend.integration.grundnetzReimport.GrundnetzReimportConfiguration;
import de.wps.radvis.backend.integration.radnetz.domain.RadNetzNetzbildungService;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungService;
import de.wps.radvis.backend.massnahme.domain.MassnahmeNetzbezugAenderungProtokollierungsService;
import de.wps.radvis.backend.massnahme.domain.MassnahmeService;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeNetzBezugAenderungRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeUmsetzungsstandViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.UmsetzungsstandRepository;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.service.DlmPbfErstellungService;
import de.wps.radvis.backend.matching.domain.service.GraphhopperUpdateService;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.ImportsGrundnetzConfiguration;

@Tag("group3")
@ContextConfiguration(classes = {
	GrundnetzReimportConfiguration.class,
	NetzConfiguration.class,
	IntegrationGrundnetzConfiguration.class,
	IntegrationAttributAbbildungConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerConfiguration.class,
	ImportsGrundnetzConfiguration.class,
	CommonConfiguration.class,
	MatchingConfiguration.class,
	BarriereConfiguration.class,
	DLMWFSImportRepositoryTestIT.TestConfiguration.class
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
})
@ActiveProfiles("dev")
class DLMWFSImportRepositoryTestIT extends DBIntegrationTestIT {
	@EnableJpaRepositories(basePackages = "de.wps.radvis.backend.massnahme")
	@EntityScan(basePackages = { "de.wps.radvis.backend.massnahme", "de.wps.radvis.backend.kommentar",
		"de.wps.radvis.backend.dokument" })
	public static class TestConfiguration {
		@Bean
		public NetzfehlerRepository netzfehlerRepository() {
			return Mockito.mock(NetzfehlerRepository.class);
		}

		@Bean
		public ImportedFeaturePersistentRepository importedFeaturePersistentRepository() {
			return Mockito.mock(ImportedFeaturePersistentRepository.class);
		}

		@MockBean
		public RadNetzNetzbildungService radNetzNetzbildungService;
		@MockBean
		public RadwegeDBNetzbildungService radwegeDBNetzbildungService;

		@MockBean
		public DlmPbfErstellungService dlmPbfErstellungService;

		@MockBean
		public GraphhopperUpdateService graphhopperUpdateService;

		@Autowired
		MassnahmeRepository massnahmeRepository;

		@Autowired
		private KantenRepository kantenRepository;

		@Autowired
		private BenutzerService benutzerService;

		@MockBean
		private MassnahmeViewRepository massnahmeViewRepository;

		@MockBean
		private MassnahmeUmsetzungsstandViewRepository massnahmeUmsetzungsstandViewRepository;

		@MockBean
		private UmsetzungsstandRepository umsetzungsstandRepository;

		@Bean
		public MassnahmeService massnahmeService() {
			return new MassnahmeService(massnahmeRepository, massnahmeViewRepository,
				massnahmeUmsetzungsstandViewRepository, umsetzungsstandRepository, kantenRepository,
				massnahmeNetzbezugAenderungProtokollierungsService());
		}

		@Autowired
		private MassnahmeNetzBezugAenderungRepository massnahmeNetzBezugAenderungRepository;

		@Bean
		public MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService() {
			return new MassnahmeNetzbezugAenderungProtokollierungsService(benutzerService,
				massnahmeNetzBezugAenderungRepository);
		}
	}

	@Autowired
	DLMWFSImportRepository dlmwfsImportRepository;

	@Test
	void testeDLMWFSImportRepository_featuresLiegenImAngefordertenBereich() {
		Envelope envelope = new Envelope(410431.4298323798, 410431.4298323798 + 100, 5298316.5840815855,
			5298316.5840815855 + 100);
		List<ImportedFeature> importedFeatures = Stream.concat(dlmwfsImportRepository.readStrassenFeatures(envelope),
			dlmwfsImportRepository.readWegeFeatures(envelope)).collect(
			Collectors.toList());
		Polygon envelopePolygon = EnvelopeAdapter.toPolygon(envelope,
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		Double maxDistance = importedFeatures.stream().map(ImportedFeature::getGeometrie)
			.mapToDouble(envelopePolygon::distance).max().orElse(0);
		assertThat(maxDistance).isLessThan(1);

		assertThat(importedFeatures.stream().map(ImportedFeature::getGeometrie)
			.allMatch(geometry -> envelope.intersects(geometry.getEnvelopeInternal()))).isTrue();
	}
}
