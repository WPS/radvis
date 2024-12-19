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

package de.wps.radvis.backend.integration.radnetz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.integration.radnetz.IntegrationRadNetzConfiguration;
import de.wps.radvis.backend.integration.radnetz.domain.entity.RadNetzNetzbildungStatistik;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;

@Tag("group6")
@ContextConfiguration(classes = {
	IntegrationRadNetzConfiguration.class,
	NetzConfiguration.class,
	GeoConverterConfiguration.class,
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	NetzfehlerConfiguration.class,
	KommentarConfiguration.class,
	CommonConfiguration.class,
	KonsistenzregelPruefungsConfiguration.class,
	KonsistenzregelnConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
class RadNetzNetzbildungServiceIntegrationTestIT extends DBIntegrationTestIT {

	@Autowired
	private KantenRepository kantenRepository;
	@Autowired
	private KnotenRepository knotenRepository;
	@Autowired
	private RadNetzNetzbildungService radNetzNetzbildungService;

	@Test
	public void readRadNetz_keineFeatures_kantenSindLeer() {
		// arrange
		assertThat(kantenRepository).isNotNull();
		assertThat(knotenRepository).isNotNull();
		List<ImportedFeature> features = new ArrayList<>();

		// act
		this.radNetzNetzbildungService.bildeRadNetz(features.stream(), Stream.empty(),
			new RadNetzNetzbildungStatistik());

		// assert
		assertThat(kantenRepository.findAll()).isEmpty();
		assertThat(knotenRepository.findAll()).isEmpty();
	}

	@Test
	public void readRadNetz_vieleFeatures_ModelEnthaeltKanten() {
		// arrange
		assertThat(kantenRepository).isNotNull();
		assertThat(knotenRepository).isNotNull();
		List<ImportedFeature> features = new ArrayList<>();

		Coordinate coordinate1 = new Coordinate(10, 10);
		Coordinate coordinate2 = new Coordinate(15, 15);
		ImportedFeature feature1 = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2)
			.quelle(QuellSystem.RadNETZ).build();
		features.add(feature1);

		//
		Coordinate coordinate3 = new Coordinate(20, 20);
		Coordinate coordinate4 = new Coordinate(25, 25);
		ImportedFeature feature2 = ImportedFeatureTestDataProvider.withLineString(coordinate3, coordinate4)
			.quelle(QuellSystem.RadNETZ).build();
		features.add(feature2);

		//
		Coordinate coordinate5 = new Coordinate(30, 30);
		Coordinate coordinate6 = new Coordinate(35, 35);
		Coordinate coordinate7 = new Coordinate(40, 40);
		ImportedFeature feature3 = ImportedFeatureTestDataProvider.withLineString(coordinate5, coordinate6, coordinate7)
			.quelle(QuellSystem.RadNETZ).build();
		features.add(feature3);

		//
		Coordinate coordinate8 = new Coordinate(45, 45);
		Coordinate coordinate9 = new Coordinate(50, 50);
		Coordinate coordinate10 = new Coordinate(55, 55);
		ImportedFeature feature4 = ImportedFeatureTestDataProvider
			.withLineString(coordinate8, coordinate9, coordinate10)
			.quelle(QuellSystem.RadNETZ).build();
		features.add(feature4);

		//
		Coordinate coordinate11 = new Coordinate(60, 60);
		Coordinate coordinate12 = new Coordinate(65, 65);
		Coordinate coordinate13 = new Coordinate(70, 70);
		ImportedFeature feature5 = ImportedFeatureTestDataProvider
			.withLineString(coordinate11, coordinate12, coordinate13).quelle(QuellSystem.RadNETZ).build();
		features.add(feature5);

		// act
		this.radNetzNetzbildungService.bildeRadNetz(features.stream(), Stream.empty(),
			new RadNetzNetzbildungStatistik());

		// assert
		Iterable<Kante> alleKanten = kantenRepository.findAll();
		assertThat(alleKanten)
			.extracting("vonKnoten.koordinate", "nachKnoten.koordinate")
			.containsExactlyInAnyOrder(
				new Tuple(coordinate1, coordinate2),
				new Tuple(coordinate3, coordinate4),
				new Tuple(coordinate5, coordinate7),
				new Tuple(coordinate8, coordinate10),
				new Tuple(coordinate11, coordinate13));

		assertThat(alleKanten)
			.extracting(Kante::getQuelle)
			.containsOnly(QuellSystem.RadNETZ);
		assertThat(kantenRepository.findAll()).hasSize(5);
		assertThat(knotenRepository.findAll()).hasSize(10);

	}

	@Test
	public void readRadNetz_KantenMitGemeinsamenKnoten_ErsterNachIstZweiterVon() {
		// arrange
		assertThat(kantenRepository).isNotNull();
		assertThat(knotenRepository).isNotNull();
		List<ImportedFeature> features = new ArrayList<>();

		Coordinate coordinate1 = new Coordinate(10, 10);
		Coordinate coordinate2 = new Coordinate(15, 15);
		ImportedFeature feature1 = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2).build();
		features.add(feature1);

		Coordinate coordinate3 = new Coordinate(15, 15);
		Coordinate coordinate4 = new Coordinate(25, 25);
		ImportedFeature feature2 = ImportedFeatureTestDataProvider.withLineString(coordinate3, coordinate4).build();
		features.add(feature2);

		// act
		this.radNetzNetzbildungService.bildeRadNetz(features.stream(), Stream.empty(),
			new RadNetzNetzbildungStatistik());

		// assert
		assertThat(kantenRepository.findAll()).hasSize(2);
		assertThat(knotenRepository.findAll()).hasSize(3);
	}

	@Test
	public void readRadNetz_KantenMitGemeinsamenKnoten_ErsterVonIstZweiterNach() {
		// arrange
		assertThat(kantenRepository).isNotNull();
		assertThat(knotenRepository).isNotNull();
		List<ImportedFeature> features = new ArrayList<>();

		Coordinate coordinate1 = new Coordinate(15, 15);
		Coordinate coordinate2 = new Coordinate(25, 25);
		ImportedFeature feature1 = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2).build();
		features.add(feature1);

		Coordinate coordinate3 = new Coordinate(10, 10);
		Coordinate coordinate4 = new Coordinate(15, 15);
		ImportedFeature feature2 = ImportedFeatureTestDataProvider.withLineString(coordinate3, coordinate4).build();
		features.add(feature2);

		// act
		this.radNetzNetzbildungService.bildeRadNetz(features.stream(), Stream.empty(),
			new RadNetzNetzbildungStatistik());

		// assert
		assertThat(kantenRepository.findAll()).hasSize(2);
		assertThat(knotenRepository.findAll()).hasSize(3);
	}
}
