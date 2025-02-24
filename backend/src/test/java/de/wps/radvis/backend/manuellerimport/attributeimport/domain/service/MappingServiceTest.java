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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.KantenMapping;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.provider.MappedFeatureTestDataProvider;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.Haendigkeit;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.KantenKonfliktProtokoll;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedFeature;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Konflikt;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

/**
 * Die Klasse wird hier anhand des LUBWMappers getestet
 */
class MappingServiceTest {

	private MappingService mappingService;
	private LUBWMapper lubwMapper;

	@Mock
	private NetzService netzService;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		this.mappingService = new MappingService();
		this.lubwMapper = new LUBWMapper(netzService);
	}

	@Test
	void testMap_simple_BeleuchtungMitLUBW() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(0.1, 0),
			new Coordinate(115, 115));

		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1,
			Map.of("beleuchtun", "2")
		)
			.haendigkeit(Haendigkeit.of(lineString1, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.875))
			.build();

		LineString lineString2 = GeometryTestdataProvider.createLineString(new Coordinate(0.2, 0),
			new Coordinate(120.2, 120));
		MappedFeature mappedFeature2 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2,
			Map.of("beleuchtun", "1"))
			.haendigkeit(Haendigkeit.of(lineString2, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1);
		kantenMapping.add(mappedFeature2);

		// act

		assertThat(kante1.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung()).isEqualTo(
			Beleuchtung.UNBEKANNT);

		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "beleuchtun", kantenMapping,
			kantenKonfliktProtokoll);

		// assert

		assertThat(kante1.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung()).isEqualTo(
			Beleuchtung.VORHANDEN);

		assertThat(kantenKonfliktProtokoll.getKonflikte()).hasSize(1);
		assertThat(kantenKonfliktProtokoll.getKonflikte())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactly(new Konflikt("beleuchtun", "1", Set.of("2")));
	}

	@Test
	void testMap_simple_NonLowerCaseBeleuchtungMitLUBW() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(0.1, 0),
			new Coordinate(115, 115));

		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1,
			Map.of("BELEUCHTUN", "2")
		)
			.haendigkeit(Haendigkeit.of(lineString1, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.875))
			.build();

		LineString lineString2 = GeometryTestdataProvider.createLineString(new Coordinate(0.2, 0),
			new Coordinate(120.2, 120));
		MappedFeature mappedFeature2 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2,
			Map.of("BELEUCHTUN", "1"))
			.haendigkeit(Haendigkeit.of(lineString2, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1);
		kantenMapping.add(mappedFeature2);

		// act

		assertThat(kante1.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung()).isEqualTo(
			Beleuchtung.UNBEKANNT);

		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "BELEUCHTUN", kantenMapping,
			kantenKonfliktProtokoll);

		// assert

		assertThat(kante1.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung()).isEqualTo(
			Beleuchtung.VORHANDEN);

		assertThat(kantenKonfliktProtokoll.getKonflikte()).hasSize(1);
		assertThat(kantenKonfliktProtokoll.getKonflikte())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactly(new Konflikt("BELEUCHTUN", "1", Set.of("2")));
	}

	@Test
	void testMap_simple_NoNullValuesWritten() {
		// arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();
		kante.getKantenAttributGruppe().getKantenAttribute()
			.setBeleuchtung(Beleuchtung.RETROREFLEKTIERENDE_RANDMARKIERUNG);

		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(0.1, 0),
			new Coordinate(115, 115));

		Map<String, Object> propertiesMap = new HashMap<>();
		propertiesMap.put("BELEUCHTUN", null);
		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1,
			propertiesMap
		)
			.haendigkeit(Haendigkeit.of(lineString1, kante.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.875))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante);
		kantenMapping.add(mappedFeature1);

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "BELEUCHTUN", kantenMapping,
			kantenKonfliktProtokoll);

		// assert
		assertThat(kante.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung()).isEqualTo(
			Beleuchtung.RETROREFLEKTIERENDE_RANDMARKIERUNG);
	}

	@Test
	void testMap_seitenbezogen_RichtungOhneSeitenbezugOhneUmkehrDerStationierungsrichtungMitLUBW() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(0.1, 0),
			new Coordinate(120.1, 120));

		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1,
			Map.of("richtung", "2")
		)
			.haendigkeit(Haendigkeit.of(lineString1, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.85))
			.build();

		LineString lineString2 = GeometryTestdataProvider.createLineString(new Coordinate(0.2, 0),
			new Coordinate(120.2, 120));
		MappedFeature mappedFeature2 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2,
			Map.of("richtung", "1"))
			.haendigkeit(Haendigkeit.of(lineString2, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1);
		kantenMapping.add(mappedFeature2);

		// act

		assertThat(kante1.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.UNBEKANNT);
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "richtung", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kante1.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
			Richtung.BEIDE_RICHTUNGEN);

		assertThat(kantenKonfliktProtokoll.getKonflikte()).hasSize(1);
		assertThat(kantenKonfliktProtokoll.getKonflikte())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactly(new Konflikt("richtung", "1", Set.of("2")));
	}

	@Test
	@SuppressWarnings("deprecation")
	void testMap_seitenbezogen_NoNullValuesWritten() {
		// arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();
		kante.getFahrtrichtungAttributGruppe().setRichtung(Richtung.IN_RICHTUNG);

		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(0.1, 0),
			new Coordinate(120.1, 120));

		Map<String, Object> propertiesMap = new HashMap<>();
		propertiesMap.put("richtung", null);
		MappedFeature mappedFeature = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString,
			propertiesMap
		)
			.haendigkeit(Haendigkeit.of(lineString, kante.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.85))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante);
		kantenMapping.add(mappedFeature);

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "richtung", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kante.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
			Richtung.IN_RICHTUNG);
	}

	@Test
	void testMap_seitenbezogen_RichtungOhneSeitenbezugMitUmkehrDerStationierungsrichtungMitLUBW() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(0.1, 0),
			new Coordinate(95.1, 95));

		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1,
			Map.of("richtung", "2")
		)
			.haendigkeit(Haendigkeit.of(lineString1, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.375))
			.build();

		LineString lineString2 = GeometryTestdataProvider.createLineString(new Coordinate(95.2, 95),
			new Coordinate(112.2, 112));
		MappedFeature mappedFeature2 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2,
			Map.of("richtung", "1"))
			.haendigkeit(Haendigkeit.of(lineString2, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.375, 0.8))
			.build();

		// feature 3 hat die selbe Richtungsaussage wie linestring1, und sollten zusammen linestring 2 überstimmen
		LineString lineString3 = GeometryTestdataProvider.createLineString(new Coordinate(120.2, 120),
			new Coordinate(112.2, 112));
		MappedFeature mappedFeature3 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString3,
			Map.of("richtung", "3"))
			.haendigkeit(Haendigkeit.of(lineString2, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.8, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1);
		kantenMapping.add(mappedFeature2);
		kantenMapping.add(mappedFeature3);

		// act
		assertThat(kante1.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.UNBEKANNT);
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "richtung", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kante1.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
			Richtung.IN_RICHTUNG);

		assertThat(kantenKonfliktProtokoll.getKonflikte()).hasSize(1);
		assertThat(kantenKonfliktProtokoll.getKonflikte())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactly(new Konflikt("richtung", "2", Set.of("1")));
	}

	@Test
	void testMap_seitenbezogen_RichtungMitSeitenbezugOhneUmkehrDerStationierungsRichtungMitLUBW() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		// links/oberhalb der Kante
		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(84, 80),
			new Coordinate(124.1, 120));
		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1,
			Map.of("richtung", "2")
		)
			.haendigkeit(Haendigkeit.of(lineString1, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();

		// rechts/unterhalb der Kante
		LineString lineString2 = GeometryTestdataProvider.createLineString(new Coordinate(76, 80),
			new Coordinate(116.2, 120));
		MappedFeature mappedFeature2 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2,
			Map.of("richtung", "1"))
			.haendigkeit(Haendigkeit.of(lineString2, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1);
		kantenMapping.add(mappedFeature2);

		// act
		assertThat(kante1.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.UNBEKANNT);
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "richtung", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kante1.getFahrtrichtungAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
			Richtung.BEIDE_RICHTUNGEN);
		assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(Richtung.IN_RICHTUNG);

		assertThat(kantenKonfliktProtokoll.getKonflikte()).isEmpty();
	}

	@Test
	void testMap_seitenbezogen_RichtungMitSeitenbezugMitUmkehrDerStationierungsRichtungMitLUBW() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		// anders orientiert => wird umgedreht
		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(124.1, 120),
			new Coordinate(84, 80));

		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1,
			Map.of("richtung", "2")
		)
			.haendigkeit(Haendigkeit.of(lineString1, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();

		LineString lineString2 = GeometryTestdataProvider.createLineString(new Coordinate(76, 80),
			new Coordinate(116.2, 120));
		MappedFeature mappedFeature2 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2,
			Map.of("richtung", "1"))
			.haendigkeit(Haendigkeit.of(lineString2, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1);
		kantenMapping.add(mappedFeature2);

		// act
		assertThat(kante1.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.UNBEKANNT);
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "richtung", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kante1.getFahrtrichtungAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
			Richtung.BEIDE_RICHTUNGEN);
		assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(Richtung.GEGEN_RICHTUNG);

		assertThat(kantenKonfliktProtokoll.getKonflikte()).isEmpty();
	}

	@Test
	void testMap_seitenbezogen_mitGeometrischemSeitenbezug_RichtungMitUmkehrDerStationierungsRichtungMitLUBW() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(80, 80),
			new Coordinate(92, 92));
		LineString lineString2 = GeometryTestdataProvider.createLineString(new Coordinate(92, 92),
			new Coordinate(108, 108));
		LineString lineString3 = GeometryTestdataProvider.createLineString(new Coordinate(108, 108),
			new Coordinate(120, 120));

		// reversed
		LineString LS1_links = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(lineString1, 0, 3)
			.reverse();
		MappedFeature mappedFeature1_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
			LS1_links,
			Map.of("richtung", "2")
		)
			.haendigkeit(Haendigkeit.of(LS1_links, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, .3))
			.build();

		// reversed, hat aber keinen effekt, da Wert beide richtungen
		LineString LS2_links = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(lineString2, 0, 3)
			.reverse();
		MappedFeature mappedFeature2_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
			GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(LS2_links, 0, 3).reverse(),
			Map.of("richtung", "1")
		)
			.haendigkeit(Haendigkeit.of(LS2_links, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.3, .7))
			.build();

		LineString LS3_links = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(lineString3, 0, 3);
		MappedFeature mappedFeature3_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
			LS3_links,
			Map.of("richtung", "3")
		)
			.haendigkeit(Haendigkeit.of(LS3_links, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.7, 1))
			.build();

		LineString LS1_rechts = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(lineString1, 0, -3);
		MappedFeature mappedFeature1_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
			LS1_rechts,
			Map.of("richtung", "2")
		)
			.haendigkeit(Haendigkeit.of(LS1_rechts, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, .4))
			.build();

		LineString LS2_rechts = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(lineString2, 0, -3);
		MappedFeature mappedFeature2_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
			LS2_rechts,
			Map.of("richtung", "1")
		)
			.haendigkeit(Haendigkeit.of(LS2_rechts, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, .8))
			.build();

		LineString LS3_rechts = GeometryTestdataProvider.getLinestringVerschobenUmCoordinate(lineString3, 0, -3);
		MappedFeature mappedFeature3_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
			LS3_rechts,
			Map.of("richtung", "2")
		)
			.haendigkeit(Haendigkeit.of(LS3_rechts, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(.6, 1.))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1_links);
		kantenMapping.add(mappedFeature2_links);
		kantenMapping.add(mappedFeature3_links);
		kantenMapping.add(mappedFeature1_rechts);
		kantenMapping.add(mappedFeature2_rechts);
		kantenMapping.add(mappedFeature3_rechts);

		// act
		assertThat(kante1.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(Richtung.UNBEKANNT);
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "richtung", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kante1.getFahrtrichtungAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()).isEqualTo(
			Richtung.GEGEN_RICHTUNG);
		assertThat(kante1.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()).isEqualTo(Richtung.IN_RICHTUNG);

		assertThat(kantenKonfliktProtokoll.getKonflikte())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactlyInAnyOrder(
				new Konflikt("richtung", "3", Set.of("1")),
				new Konflikt("richtung", "2", Set.of("1"))
			);
	}

	@Test
	void testMap_linearreferenziert_NoNullValuesWritten() {
		// arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();
		ZustaendigkeitAttribute zustaendigkeitAttribute = ZustaendigkeitAttribute.builder()
			.vereinbarungsKennung(VereinbarungsKennung.of("666"))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();
		kante.getZustaendigkeitAttributGruppe().replaceZustaendigkeitAttribute(List.of(zustaendigkeitAttribute));
		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(0.1, 0),
			new Coordinate(100.1, 100));

		Map<String, Object> propertiesMap = new HashMap<>();
		propertiesMap.put("vereinbaru", null);
		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString,
			propertiesMap)
			.haendigkeit(Haendigkeit.of(lineString, kante.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante);
		kantenMapping.add(mappedFeature1);

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "vereinbaru", kantenMapping,
			kantenKonfliktProtokoll);

		// assert
		assertThat(kante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("666"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
					.build()
			);
	}

	@Test
	void testMap_linearreferenziert_VereinbarungskennungMitLUBW() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(0.1, 0),
			new Coordinate(100.1, 100));

		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1,
			Map.of("vereinbaru", "1"))
			.haendigkeit(Haendigkeit.of(lineString1, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.build();

		LineString lineString2 = GeometryTestdataProvider.createLineString(new Coordinate(0.2, 0),
			new Coordinate(120.2, 120));
		MappedFeature mappedFeature2 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2,
			Map.of("vereinbaru", "2"))
			.haendigkeit(Haendigkeit.of(lineString2, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1);
		kantenMapping.add(mappedFeature2);
		// act

		assertThat(kante1.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute()).containsExactly(
			ZustaendigkeitAttribute.builder()
				.build());
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "vereinbaru", kantenMapping,
			kantenKonfliktProtokoll);

		// assert

		assertThat(kante1.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("1"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
					.build(),
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("2"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
					.build()
			);

		assertThat(kantenKonfliktProtokoll.getKonflikte()).isEmpty();
	}

	@Test
	void testMap_linearreferenziert_VereinbarungskennungMitLUBW_mitUeberlappung() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(0.1, 0),
			new Coordinate(100.1, 100));

		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1,
			Map.of("vereinbaru", "1"))
			.haendigkeit(Haendigkeit.of(lineString1, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
			.build();

		LineString lineString2 = GeometryTestdataProvider.createLineString(new Coordinate(0.2, 0),
			new Coordinate(120.2, 120));
		MappedFeature mappedFeature2 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2,
			Map.of("vereinbaru", "2"))
			.haendigkeit(Haendigkeit.of(lineString2, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1);
		kantenMapping.add(mappedFeature2);
		// act

		assertThat(kante1.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute()).containsExactly(
			ZustaendigkeitAttribute.builder()
				.build());
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "vereinbaru", kantenMapping,
			kantenKonfliktProtokoll);

		// assert
		assertThat(kante1.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("1"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
					.build(),
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("2"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 0.55))
					.build(),
				ZustaendigkeitAttribute.builder()
					.vereinbarungsKennung(VereinbarungsKennung.of("2"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.55, 1))
					.build()
			);

		assertThat(kantenKonfliktProtokoll.getKonflikte())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactly(
				new Konflikt(LinearReferenzierterAbschnitt.of(0.5, 0.55), Seitenbezug.BEIDSEITIG, "vereinbaru", "2",
					Set.of("1"),
					"Das Attribut konnte nicht geschrieben werden, da es Überschneidungen mit 1 anderen Abschnitten auf der gleichen Seite gibt."));
	}

	@Test
	void testMap_linearreferenziert_VereinbarungskennungMitLUBW_mitLuecke() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider.withLeereGrundnetzAttribute().zustaendigkeitAttribute(
					List.of(ZustaendigkeitAttributGruppeTestDataProvider.withLineareReferenz(0, 1.)
						.vereinbarungsKennung(VereinbarungsKennung.of("春猿火"))
						.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
						.build())
				).build())
			.id(1L)
			.build();

		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(0.1, 0),
			new Coordinate(100.1, 100));

		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1,
			Map.of("vereinbaru", "1"))
			.haendigkeit(Haendigkeit.of(lineString1, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.build();

		LineString lineString2 = GeometryTestdataProvider.createLineString(new Coordinate(0.2, 0),
			new Coordinate(120.2, 120));
		MappedFeature mappedFeature2 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2,
			Map.of("vereinbaru", "2"))
			.haendigkeit(Haendigkeit.of(lineString2, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.55, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1);
		kantenMapping.add(mappedFeature2);
		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "vereinbaru", kantenMapping,
			kantenKonfliktProtokoll);

		// assert
		assertThat(kante1.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute())
			.containsExactlyInAnyOrder(
				ZustaendigkeitAttribute.builder()
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
					.vereinbarungsKennung(VereinbarungsKennung.of("1"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
					.build(),
				ZustaendigkeitAttribute.builder()
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
					.vereinbarungsKennung(VereinbarungsKennung.of("春猿火")) // Lücke
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 0.55))
					.build(),
				ZustaendigkeitAttribute.builder()
					.baulastTraeger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L).build())
					.vereinbarungsKennung(VereinbarungsKennung.of("2"))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.55, 1))
					.build()
			);

		assertThat(kantenKonfliktProtokoll.getKonflikte()).isEmpty();
	}

	@Test
	void testMap_seitenbezogenUndLinearreferenziert_ohnegeometrischenSeitenbezug_BelagMitLUBW() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(0.1, 0),
			new Coordinate(100.1, 100));

		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1,
			Map.of("belag", "10", "breite", "1", "wegart", 110))
			.haendigkeit(Haendigkeit.of(lineString1, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.6))
			.build();

		LineString lineString2 = GeometryTestdataProvider.createLineString(new Coordinate(0.2, 0),
			new Coordinate(120.2, 120));
		MappedFeature mappedFeature2 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2,
			Map.of("belag", "20", "breite", "2", "wegart", 121))
			.haendigkeit(Haendigkeit.of(lineString2, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1);
		kantenMapping.add(mappedFeature2);

		// act
		assertThat(kante1.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.containsExactly(FuehrungsformAttribute.builder().build());

		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "belag", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kante1.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();

		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.ASPHALT)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.BETON)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 0.6))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.BETON)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.6, 1))
					.build()
			);

		assertThat(kantenKonfliktProtokoll.getKonflikte()).hasSize(1);
		assertThat(kantenKonfliktProtokoll.getKonflikte())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactly(
				new Konflikt(LinearReferenzierterAbschnitt.of(0.5, 0.6), Seitenbezug.BEIDSEITIG, "belag", "20",
					Set.of("10"),
					"Das Attribut konnte nicht geschrieben werden, da es Überschneidungen mit 1 anderen Abschnitten auf der gleichen Seite gibt."));
	}

	@Test
	void testMap_seitenbezogenUndLinearreferenziert_NoNullValuesWritten() {
		// arrange
		Kante kante = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();
		FuehrungsformAttribute fuehrungsformAttribute = FuehrungsformAttribute.builder()
			.belagArt(BelagArt.ASPHALT)
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();
		kante.getFuehrungsformAttributGruppe().replaceFuehrungsformAttribute(List.of(fuehrungsformAttribute));

		LineString lineString = GeometryTestdataProvider.createLineString(new Coordinate(0.1, 0),
			new Coordinate(100.1, 100));

		Map<String, Object> propertiesMap = new HashMap<>();
		propertiesMap.put("belag", null);
		propertiesMap.put("breite", null);
		propertiesMap.put("wegart", null);
		MappedFeature mappedFeature = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString,
			propertiesMap)
			.haendigkeit(Haendigkeit.of(lineString, kante.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante);
		kantenMapping.add(mappedFeature);

		// act
		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "belag", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kante.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();

		assertThat(kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.ASPHALT)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
					.build()
			);
	}

	// Exemplarischer Test für ein weiteres Attribut, Breite,
	// um sicher zu gehen das es ein mapping gibt
	@Test
	void testMap_seitenbezogenUndLinearreferenziert_mitGeometrischenSeitenbezug_BreiteMitLUBW() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(0.1, -10),
			new Coordinate(120.2, 110));

		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1,
			Map.of("belag", "10", "breite", "1", "wegart", 110))
			.haendigkeit(Haendigkeit.of(lineString1, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();

		LineString lineString2 = GeometryTestdataProvider.createLineString(new Coordinate(0.2, 10),
			new Coordinate(120.2, 130));
		MappedFeature mappedFeature2 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2,
			Map.of("belag", "20", "breite", "2", "wegart", 121))
			.haendigkeit(Haendigkeit.of(lineString2, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1);
		kantenMapping.add(mappedFeature2);

		// act
		assertThat(kante1.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.containsExactly(FuehrungsformAttribute.builder().build());

		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "breite", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kante1.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();

		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				FuehrungsformAttribute.builder()
					.breite(Laenge.of(1.5))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1.))
					.build()
			);

		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				FuehrungsformAttribute.builder()
					.breite(Laenge.of(1.49))
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 1))
					.build()
			);

		assertThat(kantenKonfliktProtokoll.getKonflikte()).hasSize(0);
	}

	// Exemplarischer Test für ein weiteres Attribut, Radverkehrsführung (wegart),
	// um sicher zu gehen das es ein mapping gibt
	@Test
	void testMap_seitenbezogenUndLinearreferenziert_mitGeometrischenSeitenbezug_WegartMitLUBW() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(0.1, -10),
			new Coordinate(120.2, 110));

		MappedFeature mappedFeature1 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1,
			Map.of("belag", "10", "breite", "1", "wegart", 110))
			.haendigkeit(Haendigkeit.of(lineString1, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();

		LineString lineString2 = GeometryTestdataProvider.createLineString(new Coordinate(0.2, 10),
			new Coordinate(120.2, 130));
		MappedFeature mappedFeature2 = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2,
			Map.of("belag", "20", "breite", "2", "wegart", 121))
			.haendigkeit(Haendigkeit.of(lineString2, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1);
		kantenMapping.add(mappedFeature2);

		// act
		assertThat(kante1.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.containsExactly(FuehrungsformAttribute.builder().build());

		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "wegart", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kante1.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();

		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				FuehrungsformAttribute.builder()
					.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_SELBSTSTAENDIG)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1.))
					.build()
			);

		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				FuehrungsformAttribute.builder()
					.radverkehrsfuehrung(Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 1))
					.build()
			);

		assertThat(kantenKonfliktProtokoll.getKonflikte()).hasSize(0);
	}

	@Test
	void testMap_seitenbezogenUndLinearreferenziert_mitGeometrischenSeitenbezug_BelagMitLUBW() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		LineString lineString1_rechts = GeometryTestdataProvider.createLineString(new Coordinate(4.1, 0),
			new Coordinate(104.1, 100));

		MappedFeature mappedFeature1_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1_rechts,
			Map.of("belag", "10"))
			.haendigkeit(Haendigkeit.of(lineString1_rechts, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.build();

		LineString lineString1_links = GeometryTestdataProvider.createLineString(new Coordinate(0.2, 0),
			new Coordinate(120.2, 120));
		MappedFeature mappedFeature1_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1_links,
			Map.of("belag", "20"))
			.haendigkeit(Haendigkeit.of(lineString1_links, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
			.build();

		LineString lineString2_rechts = GeometryTestdataProvider.createLineString(new Coordinate(4.2, 0),
			new Coordinate(124.2, 120));

		MappedFeature mappedFeature2_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2_rechts,
			Map.of("belag", "30"))
			.haendigkeit(Haendigkeit.of(lineString2_rechts, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
			.build();

		LineString lineString2_links = GeometryTestdataProvider.createLineString(new Coordinate(6, 10),
			new Coordinate(116.2, 120));
		MappedFeature mappedFeature2_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2_links,
			Map.of("belag", "40"))
			.haendigkeit(Haendigkeit.of(lineString2_links, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1_rechts);
		kantenMapping.add(mappedFeature1_links);
		kantenMapping.add(mappedFeature2_rechts);
		kantenMapping.add(mappedFeature2_links);

		// act

		assertThat(kante1.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.containsExactly(FuehrungsformAttribute.builder().build());

		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "belag", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kante1.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();

		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.ASPHALT)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
					.build()
			);

		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.BETON)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
					.build()
			);

		assertThat(kantenKonfliktProtokoll.getKonflikte()).isEmpty();
	}

	@Test
	void testMap_seitenbezogenUndLinearreferenziert_mitGeometrischenSeitenbezug_BelagMitLUBW_mitKonflikt() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.id(1L)
			.build();

		LineString lineString1_links = GeometryTestdataProvider.createLineString(new Coordinate(70, 74),
			new Coordinate(101.1, 104));
		MappedFeature mappedFeature1_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1_links,
			Map.of("belag", "20"))
			.haendigkeit(Haendigkeit.of(lineString1_links, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
			.build();

		LineString lineString2_links = GeometryTestdataProvider.createLineString(new Coordinate(101.1, 104),
			new Coordinate(120.2, 124));
		MappedFeature mappedFeature2_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2_links,
			Map.of("belag", "40"))
			.haendigkeit(Haendigkeit.of(lineString2_links, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.45, 1))
			.build();

		LineString lineString1_rechts = GeometryTestdataProvider.createLineString(new Coordinate(70, 66),
			new Coordinate(100.1, 96));

		MappedFeature mappedFeature1_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1_rechts,
			Map.of("belag", "10"))
			.haendigkeit(Haendigkeit.of(lineString1_rechts, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
			.build();

		LineString lineString2_rechts = GeometryTestdataProvider.createLineString(new Coordinate(100.1, 96),
			new Coordinate(120.2, 116));

		MappedFeature mappedFeature2_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2_rechts,
			Map.of("belag", "30"))
			.haendigkeit(Haendigkeit.of(lineString2_rechts, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1_links);
		kantenMapping.add(mappedFeature1_rechts);
		kantenMapping.add(mappedFeature2_links);
		kantenMapping.add(mappedFeature2_rechts);

		// act

		assertThat(kante1.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.containsExactly(FuehrungsformAttribute.builder().build());

		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "belag", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kante1.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();

		// Lineare Referenzen richten sich nach
		// 1. normalisierung (die ALLE features, sowohl links wie auch rechts) betrachtet
		// 2. linearen Referenzen auf der Kante
		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.BETON)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.45))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.45, 0.5))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 0.55))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.55, 1))
					.build()
			);

		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.ASPHALT)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.45))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.ASPHALT)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.45, 0.5))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 0.55))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.55, 1))
					.build()
			);

		assertThat(kantenKonfliktProtokoll.getKonflikte())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactlyInAnyOrder(
				new Konflikt(LinearReferenzierterAbschnitt.of(0.50, 0.55), Seitenbezug.RECHTS, "belag", "30",
					Set.of("10"),
					"Das Attribut konnte nicht geschrieben werden, da es Überschneidungen mit 1 anderen Abschnitten auf der gleichen Seite gibt."),
				new Konflikt(LinearReferenzierterAbschnitt.of(0.50, 0.55), Seitenbezug.LINKS, "belag", "40",
					Set.of("20"),
					"Das Attribut konnte nicht geschrieben werden, da es Überschneidungen mit 1 anderen Abschnitten auf der gleichen Seite gibt."),
				new Konflikt(LinearReferenzierterAbschnitt.of(0.45, 0.50), Seitenbezug.LINKS, "belag", "40",
					Set.of("20"),
					"Das Attribut konnte nicht geschrieben werden, da es Überschneidungen mit 1 anderen Abschnitten auf der gleichen Seite gibt.")
			);
	}

	@Test
	void testMap_seitenbezogenUndLinearreferenziert_mitGeometrischenSeitenbezug_BelagMitLUBW_mitLuecke() {
		// arrange
		Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .5)
							.belagArt(BelagArt.UNGEBUNDENE_DECKE).build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(.5, 1)
							.belagArt(BelagArt.UNBEKANNT).build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0, .65).belagArt(BelagArt.UNBEKANNT)
							.build(),
						FuehrungsformAttributeTestDataProvider.withLineareReferenz(0.65, 1.)
							.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
							.build()))
					.isZweiseitig(true)
					.build()
			)
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.isZweiseitig(true)
			.id(1L)
			.build();

		LineString lineString1_links = GeometryTestdataProvider.createLineString(new Coordinate(70, 74),
			new Coordinate(101.1, 104));
		MappedFeature mappedFeature1_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1_links,
			Map.of("belag", "20"))
			.haendigkeit(Haendigkeit.of(lineString1_links, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.45))
			.build();

		LineString lineString2_links = GeometryTestdataProvider.createLineString(new Coordinate(101.1, 104),
			new Coordinate(120.2, 124));
		MappedFeature mappedFeature2_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2_links,
			Map.of("belag", "40"))
			.haendigkeit(Haendigkeit.of(lineString2_links, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.6, 1))
			.build();

		LineString lineString1_rechts = GeometryTestdataProvider.createLineString(new Coordinate(70, 66),
			new Coordinate(100.1, 96));

		MappedFeature mappedFeature1_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString1_rechts,
			Map.of("belag", "10"))
			.haendigkeit(Haendigkeit.of(lineString1_rechts, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.35))
			.build();

		LineString lineString2_rechts = GeometryTestdataProvider.createLineString(new Coordinate(100.1, 96),
			new Coordinate(120.2, 116));

		MappedFeature mappedFeature2_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
			lineString2_rechts,
			Map.of("belag", "30"))
			.haendigkeit(Haendigkeit.of(lineString2_rechts, kante1.getGeometry()))
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.7, 1))
			.build();

		KantenMapping kantenMapping = new KantenMapping(kante1);
		kantenMapping.add(mappedFeature1_links);
		kantenMapping.add(mappedFeature1_rechts);
		kantenMapping.add(mappedFeature2_links);
		kantenMapping.add(mappedFeature2_rechts);

		// act

		assertThat(kante1.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();

		KantenKonfliktProtokoll kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kantenMapping.getKante().getId(),
			kantenMapping.getKante().getGeometry());
		mappingService.map(lubwMapper, "belag", kantenMapping, kantenKonfliktProtokoll);

		// assert
		assertThat(kante1.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();

		// Lineare Referenzen richten sich nach
		// 1. normalisierung (die ALLE features, sowohl links wie auch rechts) betrachtet
		// 2. linearen Referenzen auf der Kante
		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.BETON)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.35))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.BETON)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.35, 0.45))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.UNGEBUNDENE_DECKE) // Lücke
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.45, 0.5))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.UNBEKANNT) // Lücke
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 0.6))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.6, .7))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.7, 1))
					.build()
			);

		assertThat(kante1.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.usingRecursiveFieldByFieldElementComparator(
				RecursiveComparisonConfiguration.builder().withComparatorForType(
					LineareReferenzTestProvider.comparatorWithTolerance(0.005), LinearReferenzierterAbschnitt.class)
					.build())
			.containsExactlyInAnyOrder(
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.ASPHALT)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.35))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.BETON)
					// kommt von einem Segment, wo wir nur die Linke Kante haben, die dann für beide Seiten gilt
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.35, 0.45))
					.build(),
				FuehrungsformAttribute.builder() // tatsächliche Lücke
					.belagArt(BelagArt.UNBEKANNT)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.45, 0.6))
					.build(),
				FuehrungsformAttribute.builder()
					// kommt von einem Segment, wo wir nur die Linke Kante haben, die dann für beide Seiten gilt
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.6, 0.65))
					.build(),
				FuehrungsformAttribute.builder()
					// kommt von einem Segment, wo wir nur die Linke Kante haben, die dann für beide Seiten gilt
					.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.65, 0.7))
					.build(),
				FuehrungsformAttribute.builder()
					.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
					.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.7, 1))
					.build()
			);

		assertThat(kantenKonfliktProtokoll.getKonflikte()).isEmpty();
	}
}
