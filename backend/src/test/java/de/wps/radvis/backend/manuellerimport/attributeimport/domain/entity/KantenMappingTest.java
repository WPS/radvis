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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.provider.MappedFeatureTestDataProvider;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.Haendigkeit;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedAttributes;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedFeature;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.provider.LineareReferenzTestProvider;

class KantenMappingTest {

	@Test
	void getMappedAttributes_orientierungZurKanteRichtigBerechnet() {
		// arrange
		KantenMapping kantenMapping = createKantenMapping(1L,
			GeometryTestdataProvider.createLineString(new Coordinate(100, 100), new Coordinate(120, 120),
				new Coordinate(120, 140)));

		LineString lineString1 = GeometryTestdataProvider.createLineString(new Coordinate(101, 99),
			new Coordinate(125, 125));
		MappedFeature MA1 = MappedFeatureTestDataProvider.withLineString(
			lineString1).build();
		kantenMapping.add(MA1);
		MappedFeature MA2 = MappedFeatureTestDataProvider.withLineString(
			lineString1.reverse()).build();
		kantenMapping.add(MA2);

		// act
		List<MappedAttributes> result = kantenMapping.getMappedAttributes();

		assertThat(result).hasSize(2);
		assertThat(result).extracting(MappedAttributes::getSeitenbezug)
			.containsExactly(Seitenbezug.BEIDSEITIG, Seitenbezug.BEIDSEITIG);
		assertThat(result).extracting(MappedAttributes::getLinearReferenzierterAbschnitt)
			.usingElementComparator(LineareReferenzTestProvider.comparatorWithTolerance(0.05))
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 1), LinearReferenzierterAbschnitt.of(0, 1));
		assertThat(result).extracting(MappedAttributes::isOrientierungUmgedrehtZurKante)
			.containsExactly(false, true);
	}

	@Test
	void testeGetNormalizedMappedAttributes_sindNormalized() {
		// arrange
		KantenMapping kantenMapping = createKantenMapping(1L,
			GeometryTestdataProvider.createLineString(new Coordinate(100, 100), new Coordinate(120, 120),
				new Coordinate(140, 140)));

		MappedFeature MA1 = createMF(
			LinearReferenzierterAbschnitt.of(0, 0.25), new Coordinate(101, 99), new Coordinate(110, 110));
		kantenMapping.add(MA1);

		MappedFeature MA2 = createMF(LinearReferenzierterAbschnitt.of(0.125, 0.375), new Coordinate(105, 105),
			new Coordinate(115, 115));
		kantenMapping.add(MA2);

		MappedFeature MA3 = createMF(
			LinearReferenzierterAbschnitt.of(0.25, 0.5), new Coordinate(110, 110), new Coordinate(125, 130));
		kantenMapping.add(MA3);

		// act
		List<MappedAttributes> result = kantenMapping.getNormalizedMappedAttributes();

		// assert
		assertThat(result).hasSize(6);
		assertThat(result).extracting(MappedAttributes::getSeitenbezug).containsOnly(Seitenbezug.BEIDSEITIG);
		assertThat(result).extracting(MappedAttributes::isOrientierungUmgedrehtZurKante).containsOnly(false);
		assertThat(result).extracting(MappedAttributes::getLinearReferenzierterAbschnitt)
			.usingElementComparator(LineareReferenzTestProvider.lenientComparator)
			.containsExactly(LinearReferenzierterAbschnitt.of(0, 0.125), LinearReferenzierterAbschnitt.of(0.125, 0.25),
				LinearReferenzierterAbschnitt.of(0.125, 0.25), LinearReferenzierterAbschnitt.of(0.25, 0.375),
				LinearReferenzierterAbschnitt.of(0.25, 0.375),
				LinearReferenzierterAbschnitt.of(0.375, 0.5));
	}

	@Test
	void testeGetNormalizedMappedAttributesSeitenbezogen() {
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
		List<MappedAttributes> links = kantenMapping.getNormalizedMappedAttributesLinks();
		List<MappedAttributes> rechts = kantenMapping.getNormalizedMappedAttributesRechts();

		// assert
		// Lineare Referenzen richten sich nach normalisierung (die ALLE features, sowohl links wie auch rechts) betrachtet
		assertThat(links)
			.extracting(MappedAttributes::getLinearReferenzierterAbschnitt)
			.usingElementComparator(LineareReferenzTestProvider.comparatorWithTolerance(0.005))
			.containsExactlyInAnyOrder(
				LinearReferenzierterAbschnitt.of(0, 0.45),
				LinearReferenzierterAbschnitt.of(0.45, 0.5),
				LinearReferenzierterAbschnitt.of(0.45, 0.5),
				LinearReferenzierterAbschnitt.of(0.5, 0.55),
				LinearReferenzierterAbschnitt.of(0.5, 0.55),
				LinearReferenzierterAbschnitt.of(0.55, 1)
			);
		assertThat(links).extracting(MappedAttributes::isOrientierungUmgedrehtZurKante).containsOnly(false);
		assertThat(links).extracting(MappedAttributes::getProperties).extracting(Map::values)
			.map(Collection::stream)
			.map(Stream::findFirst)
			.map(Optional::get)
			.map(Object::toString)
			.containsExactly("20", "20", "40", "20", "40", "40");
		assertThat(links).extracting(MappedAttributes::getSeitenbezug).containsOnly(Seitenbezug.LINKS);

		assertThat(rechts)
			.extracting(MappedAttributes::getLinearReferenzierterAbschnitt)
			.usingElementComparator(LineareReferenzTestProvider.comparatorWithTolerance(0.005))
			.containsExactlyInAnyOrder(
				LinearReferenzierterAbschnitt.of(0, 0.45),
				LinearReferenzierterAbschnitt.of(0.45, 0.5),
				LinearReferenzierterAbschnitt.of(0.5, 0.55),
				LinearReferenzierterAbschnitt.of(0.5, 0.55),
				LinearReferenzierterAbschnitt.of(0.55, 1)
			);
		assertThat(rechts).extracting(MappedAttributes::isOrientierungUmgedrehtZurKante).containsOnly(false);
		assertThat(rechts).extracting(MappedAttributes::getProperties).extracting(Map::values)
			.map(Collection::stream)
			.map(Stream::findFirst)
			.map(Optional::get)
			.map(Object::toString)
			.containsExactly("10", "10", "10", "30", "30");
		assertThat(rechts).extracting(MappedAttributes::getSeitenbezug).containsOnly(Seitenbezug.RECHTS);
	}

	@Nested
	public class HabenFeaturesGeometrischenSeitenbezugTest {

		@Test
		void testeHabenFeaturesGeometrischenSeitenbezug_vollstaendigeAbdeckungUndMitGrossemAbstand_true() {
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

			// act + assert
			assertThat(kantenMapping.habenFeaturesGeometrischenSeitenbezug()).isTrue();
		}

		@Test
		void testeHabenFeaturesGeometrischenSeitenbezug_unvollstaendigeAbdeckungUndMitGrossemAbstand_true() {
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

			LineString lineString1_rechts = GeometryTestdataProvider.createLineString(new Coordinate(70, 66),
				new Coordinate(100.1, 96));

			MappedFeature mappedFeature1_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString1_rechts,
					Map.of("belag", "10"))
				.haendigkeit(Haendigkeit.of(lineString1_rechts, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
				.build();

			KantenMapping kantenMapping = new KantenMapping(kante1);
			kantenMapping.add(mappedFeature1_links);
			kantenMapping.add(mappedFeature1_rechts);

			// act + assert
			assertThat(kantenMapping.habenFeaturesGeometrischenSeitenbezug()).isTrue();
		}

		@Test
		void testeHabenFeaturesGeometrischenSeitenbezug_unvollstaendigeAbdeckungUndMitSchmalemAbstand_false() {
			// arrange
			Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
				.id(1L)
				.build();

			LineString lineString1_links = GeometryTestdataProvider.createLineString(new Coordinate(70, 73),
				new Coordinate(101.1, 103));
			MappedFeature mappedFeature1_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString1_links,
					Map.of("belag", "20"))
				.haendigkeit(Haendigkeit.of(lineString1_links, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
				.build();

			LineString lineString1_rechts = GeometryTestdataProvider.createLineString(new Coordinate(70, 67),
				new Coordinate(100.1, 97));

			MappedFeature mappedFeature1_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString1_rechts,
					Map.of("belag", "10"))
				.haendigkeit(Haendigkeit.of(lineString1_rechts, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
				.build();

			KantenMapping kantenMapping = new KantenMapping(kante1);
			kantenMapping.add(mappedFeature1_links);
			kantenMapping.add(mappedFeature1_rechts);

			// act + assert
			assertThat(kantenMapping.habenFeaturesGeometrischenSeitenbezug()).isFalse();
		}

		@Test
		void testeHabenFeaturesGeometrischenSeitenbezug_vollstaendigeAbdeckungUndMitMediumAbstand_true() {
			// arrange
			Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
				.id(1L)
				.build();

			LineString lineString1_links = GeometryTestdataProvider.createLineString(new Coordinate(70, 73),
				new Coordinate(101.1, 103));
			MappedFeature mappedFeature1_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString1_links,
					Map.of("belag", "20"))
				.haendigkeit(Haendigkeit.of(lineString1_links, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
				.build();

			LineString lineString2_links = GeometryTestdataProvider.createLineString(new Coordinate(101.1, 103),
				new Coordinate(120.2, 123));
			MappedFeature mappedFeature2_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString2_links,
					Map.of("belag", "40"))
				.haendigkeit(Haendigkeit.of(lineString2_links, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.45, 1))
				.build();

			LineString lineString1_rechts = GeometryTestdataProvider.createLineString(new Coordinate(70, 67),
				new Coordinate(100.1, 97));

			MappedFeature mappedFeature1_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString1_rechts,
					Map.of("belag", "10"))
				.haendigkeit(Haendigkeit.of(lineString1_rechts, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
				.build();

			LineString lineString2_rechts = GeometryTestdataProvider.createLineString(new Coordinate(100.1, 97),
				new Coordinate(120.2, 117));

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

			// act + assert
			assertThat(kantenMapping.habenFeaturesGeometrischenSeitenbezug()).isTrue();
		}

		@Test
		void testeHabenFeaturesGeometrischenSeitenbezug_vollstaendigeAbdeckungUndMitSchmalemAbstand_false() {
			// arrange
			Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
				.id(1L)
				.build();

			LineString lineString1_links = GeometryTestdataProvider.createLineString(new Coordinate(70, 71.5),
				new Coordinate(101.1, 101.5));
			MappedFeature mappedFeature1_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString1_links,
					Map.of("belag", "20"))
				.haendigkeit(Haendigkeit.of(lineString1_links, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
				.build();

			LineString lineString2_links = GeometryTestdataProvider.createLineString(new Coordinate(101.1, 101.5),
				new Coordinate(120.2, 121.5));
			MappedFeature mappedFeature2_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString2_links,
					Map.of("belag", "40"))
				.haendigkeit(Haendigkeit.of(lineString2_links, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.45, 1))
				.build();

			LineString lineString1_rechts = GeometryTestdataProvider.createLineString(new Coordinate(70, 68.5),
				new Coordinate(100.1, 98.5));

			MappedFeature mappedFeature1_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString1_rechts,
					Map.of("belag", "10"))
				.haendigkeit(Haendigkeit.of(lineString1_rechts, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
				.build();

			LineString lineString2_rechts = GeometryTestdataProvider.createLineString(new Coordinate(100.1, 98.5),
				new Coordinate(120.2, 118.5));

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

			// act + assert
			assertThat(kantenMapping.habenFeaturesGeometrischenSeitenbezug()).isFalse();
		}

		@Test
		void testeHabenFeaturesGeometrischenSeitenbezug_vollstaendigeAbdeckungUndMitGrossemAbstand_beideLinks_true() {
			Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
				.id(1L)
				.build();

			LineString lineString1_links = GeometryTestdataProvider.createLineString(new Coordinate(70, 78),
				new Coordinate(101.1, 108));
			MappedFeature mappedFeature1_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString1_links,
					Map.of("belag", "20"))
				.haendigkeit(Haendigkeit.of(lineString1_links, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
				.build();

			LineString lineString2_links = GeometryTestdataProvider.createLineString(new Coordinate(101.1, 108),
				new Coordinate(120.2, 128));
			MappedFeature mappedFeature2_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString2_links,
					Map.of("belag", "40"))
				.haendigkeit(Haendigkeit.of(lineString2_links, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.45, 1))
				.build();

			LineString lineString1_rechts = GeometryTestdataProvider.createLineString(new Coordinate(70, 71),
				new Coordinate(100.1, 101));

			MappedFeature mappedFeature1_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString1_rechts,
					Map.of("belag", "10"))
				.haendigkeit(Haendigkeit.of(lineString1_rechts, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
				.build();

			LineString lineString2_rechts = GeometryTestdataProvider.createLineString(new Coordinate(100.1, 101),
				new Coordinate(120.2, 121));

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

			// act + assert
			assertThat(kantenMapping.habenFeaturesGeometrischenSeitenbezug()).isTrue();
		}

		@Test
		void testeHabenFeaturesGeometrischenSeitenbezug_vollstaendigeAbdeckungUndMitModeratenAbstand_beideWeitLinks_false() {
			Kante kante1 = KanteTestDataProvider.withCoordinatesAndQuelle(80, 80.2, 120, 120.2, QuellSystem.DLM)
				.id(1L)
				.build();

			LineString lineString1_links = GeometryTestdataProvider.createLineString(new Coordinate(70, 82),
				new Coordinate(101.1, 112));
			MappedFeature mappedFeature1_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString1_links,
					Map.of("belag", "20"))
				.haendigkeit(Haendigkeit.of(lineString1_links, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
				.build();

			LineString lineString2_links = GeometryTestdataProvider.createLineString(new Coordinate(101.1, 112),
				new Coordinate(120.2, 132));
			MappedFeature mappedFeature2_links = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString2_links,
					Map.of("belag", "40"))
				.haendigkeit(Haendigkeit.of(lineString2_links, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.45, 1))
				.build();

			LineString lineString1_rechts = GeometryTestdataProvider.createLineString(new Coordinate(70, 76),
				new Coordinate(100.1, 106));

			MappedFeature mappedFeature1_rechts = MappedFeatureTestDataProvider.withLineStringAndProperties(
					lineString1_rechts,
					Map.of("belag", "10"))
				.haendigkeit(Haendigkeit.of(lineString1_rechts, kante1.getGeometry()))
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.55))
				.build();

			LineString lineString2_rechts = GeometryTestdataProvider.createLineString(new Coordinate(100.1, 106),
				new Coordinate(120.2, 126));

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

			// act + assert
			assertThat(kantenMapping.habenFeaturesGeometrischenSeitenbezug()).isFalse();
		}
	}

	private KantenMapping createKantenMapping(long id, LineString lineString) {
		Kante kante = KanteTestDataProvider.withDefaultValues().id(id).geometry(lineString).build();

		return new KantenMapping(kante);
	}

	private MappedFeature createMF(LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		Coordinate... coordinates) {
		return MappedFeatureTestDataProvider.withLSAndLR(GeometryTestdataProvider.createLineString(coordinates),
			linearReferenzierterAbschnitt).build();
	}
}