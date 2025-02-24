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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.wps.radvis.backend.barriere.BarriereConfiguration;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.manuellerimport.attributeimport.AttributeImportConfiguration;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.FeatureMappingTestDataProvider;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.KantenKonfliktProtokoll;
import de.wps.radvis.backend.manuellerimport.common.ManuellerImportCommonConfiguration;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.ManuellerImportFehler;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ManuellerImportFehlerRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportTyp;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Konflikt;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.entity.MappedGrundnetzkante;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.quellimport.common.ImportsCommonConfiguration;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;

@Tag("group1")
@ContextConfiguration(classes = {
	CommonConfiguration.class,
	GeoConverterConfiguration.class,
	ImportsCommonConfiguration.class,
	NetzConfiguration.class,
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	MatchingConfiguration.class,

	KommentarConfiguration.class,
	ManuellerImportCommonConfiguration.class,
	AttributeImportConfiguration.class,
	KonsistenzregelPruefungsConfiguration.class,
	KonsistenzregelnConfiguration.class,
	BarriereConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	GraphhopperOsmConfigurationProperties.class,
	GraphhopperDlmConfigurationProperties.class,
	DLMConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OsmPbfConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class,
})
@ActiveProfiles("test")
class ManuellerAttributeImportServiceTestIT extends DBIntegrationTestIT {
	@MockitoBean
	private NetzfehlerRepository netzfehlerRepository;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	BenutzerRepository benutzerRepository;
	@Autowired
	KantenRepository kantenRepository;
	@Autowired
	ManuellerImportFehlerRepository manuellerImportFehlerRepository;

	@Test
	public void createKeinMatchingFehler() {
		// arrange
		FeatureMapping mitMapping = FeatureMappingTestDataProvider.withCoordinates(new Coordinate(40, 40),
			new Coordinate(80, 80)).build();
		mitMapping.add(new MappedGrundnetzkante(GeometryTestdataProvider.createLineString(), 55L,
			GeometryTestdataProvider.createLineString()));
		List<FeatureMapping> featureMappings = List.of(
			FeatureMappingTestDataProvider.withCoordinates(new Coordinate(10, 10), new Coordinate(20, 10)).build(),
			FeatureMappingTestDataProvider.withCoordinates(new Coordinate(50, 10), new Coordinate(20, 10)).build(),
			FeatureMappingTestDataProvider.withCoordinates(new Coordinate(30, 10), new Coordinate(20, 10)).build(),
			mitMapping);
		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		Benutzer benutzer = benutzerRepository.save(
			BenutzerTestDataProvider.defaultBenutzer().organisation(gebietskoerperschaft).build());
		LocalDateTime now = LocalDateTime.now();

		// act
		List<ManuellerImportFehler> manuelleImportKeinMatchingFehlerList = (List<ManuellerImportFehler>) manuellerImportFehlerRepository
			.saveAll(
				featureMappings.stream()
					.filter(fm -> fm.getKantenAufDieGemappedWurde().isEmpty())
					.map(fm -> new ManuellerImportFehler(fm.getImportedLineString(),
						ImportTyp.ATTRIBUTE_UEBERNEHMEN, now, benutzer, gebietskoerperschaft))
					.collect(Collectors.toList()));

		// assert
		assertThat(manuellerImportFehlerRepository.findAll()).containsExactly(
			manuelleImportKeinMatchingFehlerList.get(0),
			manuelleImportKeinMatchingFehlerList.get(1),
			manuelleImportKeinMatchingFehlerList.get(2));
	}

	@Test
	public void createAttributeNichtEindeutigFehler() {
		// arrange
		LineString lineString1 = GeometryTestdataProvider.createLineString();
		LineString lineString2 = GeometryTestdataProvider.createLineString();
		LineString lineString3 = GeometryTestdataProvider.createLineString();

		Kante kante1 = kantenRepository.save(
			KanteTestDataProvider.withDefaultValues().geometry(lineString1).build());
		Kante kante2 = kantenRepository.save(
			KanteTestDataProvider.withDefaultValues().geometry(lineString2).build());
		Kante kante3 = kantenRepository.save(
			KanteTestDataProvider.withDefaultValues().geometry(lineString3).build());

		KantenKonfliktProtokoll kKP1 = new KantenKonfliktProtokoll(kante1.getId(), lineString1);
		KantenKonfliktProtokoll kKP2 = new KantenKonfliktProtokoll(kante2.getId(), lineString2);
		KantenKonfliktProtokoll kKP3 = new KantenKonfliktProtokoll(kante3.getId(), lineString3);
		Konflikt konflikt = new Konflikt("name", "ja", Set.of("nein1", "nein2"));
		kKP1.add(konflikt);
		kKP2.add(konflikt);
		kKP3.add(konflikt);
		List<KantenKonfliktProtokoll> konfliktProtokolle = new ArrayList<>(List.of(kKP1, kKP2, kKP3));

		// Dieses Protokoll bekommt eine Kante, die nicht existiert
		konfliktProtokolle.add(new KantenKonfliktProtokoll(0L, GeometryTestdataProvider.createLineString()));

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		Benutzer benutzer = benutzerRepository.save(
			BenutzerTestDataProvider.defaultBenutzer().organisation(gebietskoerperschaft).build());
		LocalDateTime now = LocalDateTime.now();

		// act
		List<ManuellerImportFehler> manuelleImportKeinMatchingFehlerList = (List<ManuellerImportFehler>) manuellerImportFehlerRepository
			.saveAll(konfliktProtokolle.stream()
				.filter(kKP -> kantenRepository.findById(kKP.getKanteId()).isPresent())
				.map(kantenKonfliktProtokoll -> kantenRepository.findById(
					kantenKonfliktProtokoll.getKanteId()).map(
						k -> new ManuellerImportFehler(k, now, benutzer, gebietskoerperschaft,
							kantenKonfliktProtokoll.getKonflikte())))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList()));

		// assert

		assertThat(manuellerImportFehlerRepository.findAll()).containsExactly(
			manuelleImportKeinMatchingFehlerList.get(0),
			manuelleImportKeinMatchingFehlerList.get(1),
			manuelleImportKeinMatchingFehlerList.get(2));

	}
}
