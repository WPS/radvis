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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;

import de.wps.radvis.backend.barriere.domain.repository.BarriereNetzBezugAenderungRepository;
import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.furtKreuzung.domain.repository.FurtKreuzungNetzBezugAenderungRepository;
import de.wps.radvis.backend.integration.attributAbbildung.IntegrationAttributAbbildungConfiguration;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingRepository;
import de.wps.radvis.backend.integration.dlm.IntegrationDlmConfiguration;
import de.wps.radvis.backend.integration.radnetz.domain.RadNetzNetzbildungService;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungService;
import de.wps.radvis.backend.konsistenz.pruefung.domain.KonsistenzregelVerletzungsRepository;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DlmRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group6")
@ContextConfiguration(classes = {
	CommonConfiguration.class,
	NetzConfiguration.class,
	IntegrationDlmConfiguration.class,
	GeoConverterConfiguration.class,
	OrganisationConfiguration.class,
	MatchingConfiguration.class,
	IntegrationAttributAbbildungConfiguration.class,
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	PostgisConfigurationProperties.class,
	CommonConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	GraphhopperDlmConfigurationProperties.class,
	GraphhopperOsmConfigurationProperties.class,
	OsmPbfConfigurationProperties.class,
	DLMConfigurationProperties.class,
	NetzkorrekturConfigurationProperties.class,
	NetzConfigurationProperties.class
})
class DlmReimportJobMatchingTestIT extends DBIntegrationTestIT {

	// Überschreibt Mocks aus der entsprechenden Configuration
	@MockitoBean
	private DlmRepository dlmRepository;
	@MockitoBean
	KantenMappingRepository kantenMappingRepository;
	@MockitoBean
	KonsistenzregelVerletzungsRepository konsistenzregelVerletzungsRepository;
	@MockitoBean
	VernetzungKorrekturJob vernetzungKorrekturJob;
	@MockitoBean
	RadNetzNetzbildungService radNetzNetzbildungService;
	@MockitoBean
	RadwegeDBNetzbildungService radwegeDBNetzbildungService;
	@MockitoBean
	ImportedFeaturePersistentRepository importedFeaturePersistentRepository;
	@MockitoBean
	BenutzerResolver benutzerResolver;
	@MockitoBean
	BenutzerService benutzerService;
	@MockitoBean
	BarriereNetzBezugAenderungRepository barriereNetzBezugAenderungRepository;
	@MockitoBean
	FurtKreuzungNetzBezugAenderungRepository furtKreuzungNetzBezugAenderungRepository;
	@MockitoBean
	private NetzfehlerRepository netzfehlerRepository;
	@MockitoBean
	private BarriereRepository barriereRepository;

	@Autowired
	private NetzService netzService;
	@Autowired
	private KantenRepository kantenRepository;
	@Autowired
	private DlmReimportJob dlmReimportJob;

	@PersistenceContext
	private EntityManager entityManager;
	@Autowired
	protected PlatformTransactionManager transactionManager;

	@BeforeEach
	void setup() throws IOException {
		MockitoAnnotations.openMocks(this);
	}

	@DynamicPropertySource
	static void dlmProperties(DynamicPropertyRegistry registry) {
		registry.add("radVis.dlm.extent.minX", () -> 0);
		registry.add("radVis.dlm.extent.minY", () -> 0);
		registry.add("radVis.dlm.extent.maxX", () -> 3000);
		registry.add("radVis.dlm.extent.maxY", () -> 3000);
		registry.add("radVis.dlm.partitionenX", () -> 3);
		registry.add("radVis.dlm.pbfpartitionen", () -> 3);
	}

	@Test
	void kanteVerlaengert_twoPartitions_matchingErzeugtLineareReferenzen() {
		// arrange
		Kante kanteBeforeReimport = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(
						List.of(
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.belagArt(BelagArt.ASPHALT)
								.build()))
					.fuehrungsformAttributeRechts(
						List.of(
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.belagArt(BelagArt.ASPHALT)
								.build()))
					.build())
			.dlmId(DlmId.of("123")).build();
		netzService.saveKante(kanteBeforeReimport);

		when(dlmRepository.getPartitionen()).thenReturn(List.of(
			new Envelope(new Coordinate(0, 0), new Coordinate(150, 200)),
			new Envelope(new Coordinate(150, 0), new Coordinate(300, 200))));
		ImportedFeature updatedKanteFeature = ImportedFeatureTestDataProvider
			.withLineString(new Coordinate(0, 0), new Coordinate(200, 0))
			.fachId(kanteBeforeReimport.getDlmId().getValue())
			.build();
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					updatedKanteFeature));

		// act
		dlmReimportJob.doRun();
		entityManager.flush();

		// assert
		Kante expectedKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 200, 0, QuellSystem.DLM)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(
						List.of(
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 0.5))
								.belagArt(BelagArt.ASPHALT)
								.build(),
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1.0))
								.build()))
					.fuehrungsformAttributeRechts(
						List.of(
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 0.5))
								.belagArt(BelagArt.ASPHALT)
								.build(),
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1.0))
								.build()))
					.build())
			.ursprungsfeatureTechnischeID("123")
			.aufDlmAbgebildeteGeometry(GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(
				200, 0)))
			.isGrundnetz(true)
			.dlmId(DlmId.of("123")).build();
		expectedKante.setOsmWayIds(Set.of());

		RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
			.withComparedFields("dlmId", "vonKnoten.point", "geometry", "isGrundnetz", "ursprungsfeatureTechnischeID",
				"aufDlmAbgebildeteGeometry")
			.withIgnoreAllOverriddenEquals(false)
			.build();

		List<Kante> kantenAfterImport = StreamSupport.stream(kantenRepository.findAll().spliterator(), false).toList();
		assertThat(kantenAfterImport).hasSize(1);

		Kante actualKante = kantenAfterImport.get(0);
		assertThat(actualKante)
			.usingRecursiveComparison(configuration)
			.isEqualTo(expectedKante);
		assertThat(Hibernate.unproxy(actualKante.getFuehrungsformAttributGruppe()))
			.usingRecursiveComparison()
			.ignoringFields("id", "version")
			.ignoringAllOverriddenEquals()
			.isEqualTo(expectedKante.getFuehrungsformAttributGruppe());
	}

	@Test
	void kanteGeloescht_wirdNichtGematcht() {
		// arrange
		Kante kante1BeforeReimport = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(
						List.of(
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.belagArt(BelagArt.ASPHALT)
								.build()))
					.fuehrungsformAttributeRechts(
						List.of(
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.belagArt(BelagArt.ASPHALT)
								.build()))
					.build())
			.dlmId(DlmId.of("123")).build();
		Kante kante2BeforeReimport = KanteTestDataProvider.withCoordinatesAndQuelle(0, 1, 100, 1, QuellSystem.DLM)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(
						List.of(
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.belagArt(BelagArt.BETON)
								.build()))
					.fuehrungsformAttributeRechts(
						List.of(
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.belagArt(BelagArt.BETON)
								.build()))
					.build())
			.dlmId(DlmId.of("234")).build();

		kante1BeforeReimport = netzService.saveKante(kante1BeforeReimport);
		kante2BeforeReimport = netzService.saveKante(kante2BeforeReimport);

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		ImportedFeature updatedKante1Feature = ImportedFeatureTestDataProvider
			.withLineString(new Coordinate(0, 0), new Coordinate(100, 0))
			.fachId(kante1BeforeReimport.getDlmId().getValue())
			.build();
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					updatedKante1Feature
				// Keine Kante2, da gelöscht
				));

		// act
		dlmReimportJob.doRun();
		entityManager.flush();

		// assert
		RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
			.withComparedFields("dlmId", "vonKnoten.point", "geometry", "isGrundnetz", "ursprungsfeatureTechnischeID",
				"aufDlmAbgebildeteGeometry")
			.withIgnoreAllOverriddenEquals(false)
			.build();

		List<Kante> kantenAfterImport = StreamSupport.stream(kantenRepository.findAll().spliterator(), false).toList();
		assertThat(kantenAfterImport).hasSize(1);

		Kante actualKante = kantenAfterImport.get(0);
		assertThat(actualKante)
			.usingRecursiveComparison(configuration)
			.isEqualTo(kante1BeforeReimport);
		assertThat(Hibernate.unproxy(actualKante.getFuehrungsformAttributGruppe()))
			.usingRecursiveComparison()
			.ignoringFields("id", "version")
			.ignoringAllOverriddenEquals()
			.isEqualTo(kante1BeforeReimport.getFuehrungsformAttributGruppe());
	}

	@Test
	void nichtsGeaendert_keinMatchingUndKeineFehler() {
		// arrange
		Kante kanteBeforeReimport = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(
						List.of(
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.belagArt(BelagArt.ASPHALT)
								.build()))
					.fuehrungsformAttributeRechts(
						List.of(
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.belagArt(BelagArt.ASPHALT)
								.build()))
					.build())
			.dlmId(DlmId.of("123")).build();

		kanteBeforeReimport = netzService.saveKante(kanteBeforeReimport);

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		ImportedFeature updatedKante1Feature = ImportedFeatureTestDataProvider
			.withLineString(kanteBeforeReimport.getVonKnoten().getKoordinate(), kanteBeforeReimport.getNachKnoten()
				.getKoordinate())
			.fachId(kanteBeforeReimport.getDlmId().getValue())
			.build();
		when(dlmRepository.getKanten(any())).thenReturn(List.of(updatedKante1Feature));

		// act
		dlmReimportJob.doRun();
		entityManager.flush();

		// assert
		List<Kante> kantenAfterImport = StreamSupport.stream(kantenRepository.findAll().spliterator(), false).toList();
		assertThat(kantenAfterImport).hasSize(1);

		Kante actualKante = kantenAfterImport.get(0);
		assertThat(actualKante)
			.usingRecursiveComparison()
			.comparingOnlyFields("id", "version", "dlmId", "geometry", "isGrundnetz", "ursprungsfeatureTechnischeID",
				"aufDlmAbgebildeteGeometry")
			.usingOverriddenEquals()
			.isEqualTo(kanteBeforeReimport);
	}

	@SuppressWarnings("unchecked")
	@Test
	void geometryChanged_updateGeometryOfExistingKante() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("123")).build();
		netzService.saveKante(kante1);

		Kante kante2 = KanteTestDataProvider.withCoordinatesAndQuelle(100, 100, 200, 100, QuellSystem.DLM)
			.dlmId(DlmId.of("456")).build();
		netzService.saveKante(kante2);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		Coordinate[] newCoordinates = new Coordinate[] { new Coordinate(0, 0), new Coordinate(100, 0),
			new Coordinate(100, 100) };
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(newCoordinates).fachId(kante1.getDlmId().getValue())
						.build(),
					ImportedFeatureTestDataProvider.withLineString(kante2.getGeometry().getCoordinates())
						.fachId(kante2.getDlmId().getValue()).build()));

		// act
		dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
			.withComparedFields("id", "dlmId", "vonKnoten.id", "nachKnoten.id")
			.withIgnoreAllOverriddenEquals(false)
			.build();
		assertThat(StreamSupport.stream(kantenRepository.findAll().spliterator(), false).toList())
			.usingRecursiveFieldByFieldElementComparator(configuration)
			.containsExactlyInAnyOrder(kante1, kante2);

		LineString newKanteGeometry = GeometryTestdataProvider
			.createLineString(newCoordinates);
		Kante updatedKante = kantenRepository.findById(kante1.getId()).get();
		assertThat(updatedKante.getGeometry()).isEqualTo(newKanteGeometry);
		assertThat(updatedKante.getZugehoerigeDlmGeometrie()).isEqualTo(newKanteGeometry);
		assertThat(updatedKante.getLaengeBerechnet().getValue()).isEqualTo(200);

		assertThat(kantenRepository.findById(kante2.getId()).get()).usingRecursiveComparison()
			.comparingOnlyFields("geometry", "aufDlmAbgebildeteGeometry", "kantenLaengeInCm").usingOverriddenEquals()
			.isEqualTo(kante2);
	}

	@Test
	void vernetzungChanged_createNewKante_deleteOld() {
		// arrange
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM).build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM)
			.build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM)
			.build();

		Kante kante1 = KanteTestDataProvider.fromKnoten(knoten1, knoten2).quelle(QuellSystem.DLM).dlmId(DlmId.of("123"))
			.build();
		netzService.saveKante(kante1);

		Kante kante2 = KanteTestDataProvider.fromKnoten(knoten2, knoten3).quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("456")).build();
		netzService.saveKante(kante2);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen())
			.thenReturn(List.of(new Envelope(new Coordinate(0, 0), new Coordinate(200, 200))));
		Coordinate[] newCoordinates = new Coordinate[] { knoten1.getKoordinate(), knoten3.getKoordinate() };
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(newCoordinates).fachId(kante1.getDlmId().getValue())
						.build(),
					ImportedFeatureTestDataProvider.withLineString(kante2.getGeometry().getCoordinates())
						.fachId(kante2.getDlmId().getValue()).build()));

		// act
		dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		Kante newKante = KanteTestDataProvider.fromKnoten(knoten1, knoten3).quelle(QuellSystem.DLM)
			.dlmId(kante1.getDlmId()).build();
		RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
			.withComparedFields("dlmId", "vonKnoten.id", "nachKnoten.id", "geometry")
			.withIgnoreAllOverriddenEquals(false)
			.build();
		assertThat(StreamSupport.stream(kantenRepository.findAll().spliterator(), false).toList())
			.usingRecursiveFieldByFieldElementComparator(configuration)
			.containsExactlyInAnyOrder(newKante, kante2);
		assertThat(kantenRepository.findById(kante1.getId())).isEmpty();
	}

	@Test
	@SuppressWarnings("unchecked")
	void vernetzungChanged_correctHandlingOfSubnetworks() {
		// arrange
		// Verbindungen zwischen Knoten: (1--[k1]--2) (3--[k2]--4--[k3]--5)
		Knoten knoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(10, 100), QuellSystem.DLM)
			.build();
		Knoten knoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(20, 100), QuellSystem.DLM)
			.build();
		Knoten knoten3 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1030, 100), QuellSystem.DLM)
			.build();
		Knoten knoten4 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1040, 100), QuellSystem.DLM)
			.build();
		Knoten knoten5 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1050, 100), QuellSystem.DLM)
			.build();

		Kante kante1 = KanteTestDataProvider.fromKnoten(knoten1, knoten2)
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("1"))
			.kantenAttributGruppe(KantenAttributGruppe.builder().netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG)).build())
			.build();
		netzService.saveKante(kante1);

		Kante kante2 = KanteTestDataProvider.fromKnoten(knoten3, knoten4)
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("2"))
			.kantenAttributGruppe(KantenAttributGruppe.builder().netzklassen(Set.of(Netzklasse.KREISNETZ_ALLTAG))
				.build())
			.build();
		netzService.saveKante(kante2);
		Kante kante3 = KanteTestDataProvider.fromKnoten(knoten4, knoten5)
			.quelle(QuellSystem.DLM)
			.dlmId(DlmId.of("3"))
			.kantenAttributGruppe(KantenAttributGruppe.builder().netzklassen(Set.of(Netzklasse.RADSCHNELLVERBINDUNG))
				.build())
			.build();
		netzService.saveKante(kante3);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen()).thenReturn(List.of(
			new Envelope(new Coordinate(0, 0), new Coordinate(2000, 100))));
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(kante1.getGeometry().reverse().getCoordinates())
						.fachId(kante1.getDlmId().getValue())
						.build(),
					ImportedFeatureTestDataProvider.withLineString(kante2.getGeometry().reverse().getCoordinates())
						.fachId(kante2.getDlmId().getValue())
						.build(),
					ImportedFeatureTestDataProvider.withLineString(kante3.getGeometry().reverse().getCoordinates())
						.fachId(kante3.getDlmId().getValue())
						.build()));

		// act
		dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		List<Kante> actualKanten = kantenRepository.findAllByDlmIdIn(List.of(kante1.getDlmId(), kante2.getDlmId(),
			kante3.getDlmId()));
		assertThat(actualKanten).hasSize(3);

		Optional<Kante> actualKante1 = actualKanten.stream().filter(k -> k.getDlmId().equals(kante1.getDlmId()))
			.findFirst();
		assertThat(actualKante1.get().getVonKnoten().getId()).isEqualTo(knoten2.getId());
		assertThat(actualKante1.get().getNachKnoten().getId()).isEqualTo(knoten1.getId());
		assertThat(actualKante1.get().getGeometry()).isEqualTo(kante1.getGeometry().reverse());
		assertThat(actualKante1.get().getKantenAttributGruppe().getNetzklassen()).isEqualTo(kante1
			.getKantenAttributGruppe().getNetzklassen());

		Optional<Kante> actualKante2 = actualKanten.stream().filter(k -> k.getDlmId().equals(kante2.getDlmId()))
			.findFirst();
		assertThat(actualKante2.get().getVonKnoten().getId()).isEqualTo(knoten4.getId());
		assertThat(actualKante2.get().getNachKnoten().getId()).isEqualTo(knoten3.getId());
		assertThat(actualKante2.get().getGeometry()).isEqualTo(kante2.getGeometry().reverse());
		assertThat(actualKante2.get().getKantenAttributGruppe().getNetzklassen()).isEqualTo(kante2
			.getKantenAttributGruppe().getNetzklassen());

		Optional<Kante> actualKante3 = actualKanten.stream().filter(k -> k.getDlmId().equals(kante3.getDlmId()))
			.findFirst();
		assertThat(actualKante3.get().getVonKnoten().getId()).isEqualTo(knoten5.getId());
		assertThat(actualKante3.get().getNachKnoten().getId()).isEqualTo(knoten4.getId());
		assertThat(actualKante3.get().getGeometry()).isEqualTo(kante3.getGeometry().reverse());
		assertThat(actualKante3.get().getKantenAttributGruppe().getNetzklassen()).isEqualTo(kante3
			.getKantenAttributGruppe().getNetzklassen());
	}

	@SuppressWarnings("unchecked")
	@Test
	void topologyChangedOverMultiplePartitions_createNewAndDeleteOldKantenCorrectly() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 100, 10, QuellSystem.DLM)
			.dlmId(DlmId.of("11")).build();
		netzService.saveKante(kante1);

		Kante kante2 = KanteTestDataProvider.withCoordinatesAndQuelle(20, 20, 2222, 20, QuellSystem.DLM)
			.dlmId(DlmId.of("22")).build();
		netzService.saveKante(kante2);

		Kante kante3 = KanteTestDataProvider.withCoordinatesAndQuelle(30, 30, 300, 30, QuellSystem.DLM)
			.dlmId(DlmId.of("33")).build();
		netzService.saveKante(kante3);
		netzService.saveKante(kante2);

		Kante kante4 = KanteTestDataProvider.withCoordinatesAndQuelle(40, 40, 2400, 40, QuellSystem.DLM)
			.dlmId(DlmId.of("44_unchanged")).build();
		netzService.saveKante(kante4);

		entityManager.flush();
		entityManager.clear();

		when(dlmRepository.getPartitionen()).thenReturn(List.of(
			new Envelope(new Coordinate(0, 0), new Coordinate(1000, 3000)),
			new Envelope(new Coordinate(1000, 0), new Coordinate(2000, 3000)),
			new Envelope(new Coordinate(2000, 0), new Coordinate(3000, 3000))));

		// Über mehrere Partitionen verlängert
		Coordinate[] newCoordinatesKante1 = new Coordinate[] { new Coordinate(10, 10), new Coordinate(3111, 10) };
		// Über mehrere Partitionen verkürzt
		Coordinate[] newCoordinatesKante2 = new Coordinate[] { new Coordinate(20, 20), new Coordinate(2002, 20) };
		// Über mehrere Partitionen verschoben
		Coordinate[] newCoordinatesKante3 = new Coordinate[] { new Coordinate(3030, 30), new Coordinate(3300, 30) };
		Coordinate[] kante4Coordinates = kante4.getGeometry().getCoordinates();
		when(dlmRepository.getKanten(any()))
			.thenReturn(
				List.of(
					ImportedFeatureTestDataProvider.withLineString(newCoordinatesKante1).fachId(kante1.getDlmId()
						.getValue()).build(),
					ImportedFeatureTestDataProvider.withLineString(newCoordinatesKante2).fachId(kante2.getDlmId()
						.getValue()).build(),
					ImportedFeatureTestDataProvider.withLineString(newCoordinatesKante3).fachId(kante3.getDlmId()
						.getValue()).build(),
					ImportedFeatureTestDataProvider.withLineString(kante4Coordinates).fachId(kante4.getDlmId()
						.getValue()).build()));

		// act
		dlmReimportJob.doRun();
		entityManager.flush();
		entityManager.clear();

		// assert
		Map<DlmId, Kante> kantenAfterJob = StreamSupport.stream(kantenRepository.findAll().spliterator(), false)
			.collect(
				Collectors.toMap(Kante::getDlmId, Function.identity()));

		assertThat(kantenAfterJob).hasSize(4);

		LineString newKante1Geometry = GeometryTestdataProvider.createLineString(newCoordinatesKante1);
		Kante updatedKante1 = kantenAfterJob.get(kante1.getDlmId());
		assertThat(updatedKante1).isNotNull();
		assertThat(updatedKante1.getGeometry()).isEqualTo(newKante1Geometry);
		assertThat(updatedKante1.getZugehoerigeDlmGeometrie()).isEqualTo(newKante1Geometry);

		LineString newKante2Geometry = GeometryTestdataProvider.createLineString(newCoordinatesKante2);
		Kante updatedKante2 = kantenAfterJob.get(kante2.getDlmId());
		assertThat(updatedKante2).isNotNull();
		assertThat(updatedKante2.getGeometry()).isEqualTo(newKante2Geometry);
		assertThat(updatedKante2.getZugehoerigeDlmGeometrie()).isEqualTo(newKante2Geometry);

		LineString newKante3Geometry = GeometryTestdataProvider.createLineString(newCoordinatesKante3);
		Kante updatedKante3 = kantenAfterJob.get(kante3.getDlmId());
		assertThat(updatedKante3).isNotNull();
		assertThat(updatedKante3.getGeometry()).isEqualTo(newKante3Geometry);
		assertThat(updatedKante3.getZugehoerigeDlmGeometrie()).isEqualTo(newKante3Geometry);

		Kante kante4AfterJob = kantenAfterJob.get(kante4.getDlmId());
		RecursiveComparisonConfiguration configuration = RecursiveComparisonConfiguration.builder()
			.withComparedFields("dlmId", "vonKnoten.point", "geometry", "isGrundnetz", "ursprungsfeatureTechnischeID",
				"aufDlmAbgebildeteGeometry")
			.withIgnoreAllOverriddenEquals(false)
			.build();

		assertThat(kante4AfterJob)
			.usingRecursiveComparison(configuration)
			.isEqualTo(kante4);
		assertThat(Hibernate.unproxy(kante4AfterJob.getFuehrungsformAttributGruppe()))
			.usingRecursiveComparison()
			.ignoringFields("id", "version")
			.ignoringAllOverriddenEquals()
			.isEqualTo(kante4.getFuehrungsformAttributGruppe());
	}
}
