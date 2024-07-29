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

package de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.service;

import static de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.service.MassnahmenImportZuordnungAssert.assertThatMassnahmenImportZuordnung;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.geotools.api.feature.simple.SimpleFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.mockito.Mock;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.SimpleFeatureTestDataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportZuordnung;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.entity.MassnahmenImportZuordnungTestDataProvider;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweis;
import de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject.NetzbezugHinweisText;
import de.wps.radvis.backend.matching.domain.entity.MatchingStatistik;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;

class MassnahmeNetzbezugServiceTest {

	MassnahmeNetzbezugService service;

	@Mock
	SimpleMatchingService simpleMatchingService;

	@Mock
	NetzService netzService;

	@BeforeEach
	void setUp() {
		openMocks(this);
		service = new MassnahmeNetzbezugService(simpleMatchingService, netzService);
	}

	@Test
	void testBestimmeNetzbezugDerZuordnung_KorrekteGeometryCollection_NetzbezugErstelltUndKeineHinweise() {
		// Arrange
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM)
			.id(1L)
			.build();
		Knoten bisKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 0), QuellSystem.DLM)
			.id(2L)
			.build();
		Kante kante = KanteTestDataProvider.fromKnoten(vonKnoten, bisKnoten)
			.id(100L)
			.build();

		MultiPoint multiPoint = GeometryTestdataProvider.createMultiPoint(
			new Coordinate(0, 0),
			new Coordinate(50, 0)
		);
		LineString lineString1 = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 0),
			new Coordinate(20, 0)
		);
		LineString lineString2 = GeometryTestdataProvider.createLineString(
			new Coordinate(80, 0),
			new Coordinate(100, 0)
		);
		MultiLineString multiLineString = GeometryTestdataProvider.createMultiLineString(lineString1, lineString2);
		GeometryCollection geometryCollection = GeometryTestdataProvider.creatGeometryCollection(
			multiPoint,
			multiLineString
		);
		SimpleFeature feature = SimpleFeatureTestDataProvider.withGeometryAndAttributes(
			Collections.emptyMap(),
			geometryCollection
		);

		when(simpleMatchingService.matche(eq(lineString1), any())).thenReturn(
			Optional.of(new OsmMatchResult(lineString1, List.of(OsmWayId.of(kante.getId()))))
		);
		when(simpleMatchingService.matche(eq(lineString2), any())).thenReturn(
			Optional.of(new OsmMatchResult(lineString2, List.of(OsmWayId.of(kante.getId()))))
		);
		when(netzService.getKante(kante.getId())).thenReturn(kante);
		when(netzService.getRadVisNetzKantenInBereich(any())).thenReturn(new ArrayList<>(List.of(kante)));

		Envelope envelopePunkt1 = vonKnoten.getPoint().getEnvelopeInternal();
		envelopePunkt1.expandBy(MassnahmeNetzbezugService.GEOMETRIE_SUCHRADIUS_IN_METER);
		Envelope envelopePunkt2 = multiPoint.getGeometryN(1).getEnvelopeInternal();
		envelopePunkt2.expandBy(MassnahmeNetzbezugService.GEOMETRIE_SUCHRADIUS_IN_METER);
		when(netzService.getRadVisNetzKnotenInBereich(envelopePunkt1)).thenReturn(new ArrayList<>(List.of(vonKnoten)));
		when(netzService.getRadVisNetzKnotenInBereich(envelopePunkt2)).thenReturn(new ArrayList<>());

		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.neuWithFeature(feature);
		MatchingStatistik matchingStatistik = new MatchingStatistik();

		// Act
		service.bestimmeNetzbezugDerZuordnung(zuordnung, matchingStatistik);

		// Assert
		MassnahmeNetzBezug expectedNetzBezug = new MassnahmeNetzBezug(
			Set.of(
				new AbschnittsweiserKantenSeitenBezug(kante, LinearReferenzierterAbschnitt.of(0, 0.2),
					Seitenbezug.BEIDSEITIG),
				new AbschnittsweiserKantenSeitenBezug(kante, LinearReferenzierterAbschnitt.of(0.8, 1),
					Seitenbezug.BEIDSEITIG)
			),
			Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.5), Seitenbezug.BEIDSEITIG)),
			Set.of(vonKnoten));

		Optional<MassnahmeNetzBezug> netzbezug = zuordnung.getNetzbezug();
		assertThat(netzbezug).isPresent();
		netzbezug.ifPresent(massnahmeNetzBezug -> assertThat(massnahmeNetzBezug).isEqualTo(expectedNetzBezug));
		assertThatMassnahmenImportZuordnung(zuordnung).doesNotHaveAnyHinweis();
	}

	@Test
	void testBestimmeNetzbezugDerZuordnung_snappeLineareReferenzenAufEndpunkte() {
		// Arrange
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM)
			.id(1L)
			.build();
		Knoten bisKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 0), QuellSystem.DLM)
			.id(2L)
			.build();
		Kante kante = KanteTestDataProvider.fromKnoten(vonKnoten, bisKnoten)
			.id(100L)
			.build();

		LineString lineString1 = GeometryTestdataProvider.createLineString(
			new Coordinate(0.9, 0),
			new Coordinate(20, 0));
		LineString lineString2 = GeometryTestdataProvider.createLineString(
			new Coordinate(80, 0),
			new Coordinate(99.5, 0));
		MultiLineString multiLineString = GeometryTestdataProvider.createMultiLineString(lineString1, lineString2);
		GeometryCollection geometryCollection = GeometryTestdataProvider.creatGeometryCollection(
			multiLineString);
		SimpleFeature feature = SimpleFeatureTestDataProvider.withGeometryAndAttributes(
			Collections.emptyMap(),
			geometryCollection);

		when(simpleMatchingService.matche(eq(lineString1), any())).thenReturn(
			Optional.of(new OsmMatchResult(lineString1, List.of(OsmWayId.of(kante.getId())))));
		when(simpleMatchingService.matche(eq(lineString2), any())).thenReturn(
			Optional.of(new OsmMatchResult(lineString2, List.of(OsmWayId.of(kante.getId())))));
		when(netzService.getKante(kante.getId())).thenReturn(kante);

		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.neuWithFeature(feature);
		MatchingStatistik matchingStatistik = new MatchingStatistik();

		// Act
		service.bestimmeNetzbezugDerZuordnung(zuordnung, matchingStatistik);

		// Assert
		MassnahmeNetzBezug expectedNetzBezug = new MassnahmeNetzBezug(
			Set.of(
				new AbschnittsweiserKantenSeitenBezug(kante, LinearReferenzierterAbschnitt.of(0, 0.2),
					Seitenbezug.BEIDSEITIG),
				new AbschnittsweiserKantenSeitenBezug(kante, LinearReferenzierterAbschnitt.of(0.8, 1),
					Seitenbezug.BEIDSEITIG)),
			Set.of(),
			Set.of());

		Optional<MassnahmeNetzBezug> netzbezug = zuordnung.getNetzbezug();
		assertThat(netzbezug).isPresent();
		netzbezug.ifPresent(massnahmeNetzBezug -> assertThat(massnahmeNetzBezug).isEqualTo(expectedNetzBezug));
		assertThatMassnahmenImportZuordnung(zuordnung).doesNotHaveAnyHinweis();
	}

	@Test
	void testBestimmeNetzbezugDerZuordnung_FehlerhafteGeometryCollection_jederHinweisVorhanden() {
		// Arrange
		Point point = GeometryTestdataProvider.createPoint(new Coordinate(0, 0));
		LineString lineString = GeometryTestdataProvider.createLineString(
			new Coordinate(0, 0),
			new Coordinate(20, 0)
		);
		MultiPolygon multiPolygon = GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100);
		GeometryCollection geometryCollection = GeometryTestdataProvider.creatGeometryCollection(
			point,
			lineString,
			multiPolygon
		);
		SimpleFeature feature = SimpleFeatureTestDataProvider.withGeometryAndAttributes(
			Collections.emptyMap(),
			geometryCollection
		);

		when(simpleMatchingService.matche(any(), any())).thenReturn(Optional.empty());
		when(netzService.getRadVisNetzKantenInBereich(any())).thenReturn(new ArrayList<>(Collections.emptyList()));
		when(netzService.getRadVisNetzKnotenInBereich(any())).thenReturn(new ArrayList<>(Collections.emptyList()));

		// Act
		MassnahmenImportZuordnung zuordnung = MassnahmenImportZuordnungTestDataProvider.neuWithFeature(feature);
		MatchingStatistik matchingStatistik = new MatchingStatistik();

		// Act
		service.bestimmeNetzbezugDerZuordnung(zuordnung, matchingStatistik);

		// Assert
		List<NetzbezugHinweis> expectedHinweise = List.of(
			NetzbezugHinweis.ofWarnung(NetzbezugHinweisText.NETZBEZUG_UNVOLLSTAENDIG),
			NetzbezugHinweis.ofWarnung(NetzbezugHinweisText.NETZBEZUG_UNVOLLSTAENDIG),
			NetzbezugHinweis.ofWarnung(NetzbezugHinweisText.FALSCHER_GEOMETRIE_TYP),
			NetzbezugHinweis.ofError(NetzbezugHinweisText.NETZBEZUG_NICHT_GEFUNDEN)
		);

		Optional<MassnahmeNetzBezug> netzbezug = zuordnung.getNetzbezug();
		assertThat(netzbezug).isEmpty();
		assertThatMassnahmenImportZuordnung(zuordnung).hasExactlyNetzbezugsHinweise(
			expectedHinweise.toArray(new NetzbezugHinweis[0])
		);
	}

	@Test
	void testeCreatePunktNetzbezug_prefersKnoten() {
		// Arrange
		Point point = GeometryTestdataProvider.createPoint(new Coordinate(11.4, 10));
		Kante naechsteKante = KanteTestDataProvider.withDefaultValues().id(1L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(8, 8), new Coordinate(16, 8)))
			.build();
		Kante weitereKante = KanteTestDataProvider.withDefaultValues().id(2L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(3, 3), new Coordinate(8, 8)))
			.build();
		Knoten closestKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(8, 8), QuellSystem.DLM)
			.build();
		Knoten otherKnoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(16, 8), QuellSystem.DLM)
			.build();
		Knoten otherKnoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3, 3), QuellSystem.DLM)
			.build();

		Envelope expectedEnvelope = point.getEnvelopeInternal();
		expectedEnvelope.expandBy(MassnahmeNetzbezugService.GEOMETRIE_SUCHRADIUS_IN_METER);
		when(netzService.getRadVisNetzKantenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of(weitereKante, naechsteKante)));
		when(netzService.getRadVisNetzKnotenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of(otherKnoten1, closestKnoten, otherKnoten2)));

		// Act
		Optional<MassnahmeNetzBezug> result = service.createNetzbezugForPoint(point);

		// Assert
		assertThat(result).isPresent();
		assertThat(result.get().getImmutableKantenAbschnittBezug()).isEmpty();
		assertThat(result.get().getImmutableKantenPunktBezug()).isEmpty();
		assertThat(result.get().getImmutableKnotenBezug()).containsExactly(closestKnoten);
	}

	@Test
	void testeCreatePunktNetzbezug_prefersKnotenButNotOverlySo() {
		// Arrange
		Point point = GeometryTestdataProvider.createPoint(new Coordinate(11.5, 10));
		Kante naechsteKante = KanteTestDataProvider.withDefaultValues().id(1L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(8, 8), new Coordinate(16, 8)))
			.build();
		Kante weitereKante = KanteTestDataProvider.withDefaultValues().id(2L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(3, 3), new Coordinate(8, 8)))
			.build();
		Knoten closestKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(8, 8), QuellSystem.DLM)
			.build();
		Knoten otherKnoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(16, 8), QuellSystem.DLM)
			.build();
		Knoten otherKnoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3, 3), QuellSystem.DLM)
			.build();

		Envelope expectedEnvelope = point.getEnvelopeInternal();
		expectedEnvelope.expandBy(MassnahmeNetzbezugService.GEOMETRIE_SUCHRADIUS_IN_METER);
		when(netzService.getRadVisNetzKantenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of(weitereKante, naechsteKante)));
		when(netzService.getRadVisNetzKnotenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of(otherKnoten1, closestKnoten, otherKnoten2)));

		// Act
		Optional<MassnahmeNetzBezug> result = service.createNetzbezugForPoint(point);

		// Assert
		assertThat(result).isPresent();
		assertThat(result.get().getImmutableKantenAbschnittBezug()).isEmpty();
		assertThat(result.get().getImmutableKnotenBezug()).isEmpty();
		assertThat(result.get().getImmutableKantenPunktBezug()).hasSize(1);
		PunktuellerKantenSeitenBezug punktbezug = result.get().getImmutableKantenPunktBezug().stream().findFirst()
			.get();
		assertThat(punktbezug.getKante()).isEqualTo(naechsteKante);
		assertThat(LineareReferenz.fractionEqual(punktbezug.getLineareReferenz(), LineareReferenz.of(0.438))).isTrue();
		assertThat(punktbezug.getSeitenbezug()).isEqualTo(Seitenbezug.BEIDSEITIG);
	}

	@Test
	void testeCreatePunktNetzbezug_nurKanten_NimmtNaechsteKante() {
		// Arrange
		Point point = GeometryTestdataProvider.createPoint(new Coordinate(10, 10));
		Kante naechsteKante = KanteTestDataProvider.withDefaultValues().id(1L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(8, 8), new Coordinate(16, 8)))
			.build();
		Kante weitereKante = KanteTestDataProvider.withDefaultValues().id(2L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(3, 3), new Coordinate(8, 8)))
			.build();

		Envelope expectedEnvelope = point.getEnvelopeInternal();
		expectedEnvelope.expandBy(MassnahmeNetzbezugService.GEOMETRIE_SUCHRADIUS_IN_METER);
		when(netzService.getRadVisNetzKantenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of(weitereKante, naechsteKante)));
		when(netzService.getRadVisNetzKnotenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of()));

		// Act
		Optional<MassnahmeNetzBezug> result = service.createNetzbezugForPoint(point);

		// Assert
		assertThat(result).isPresent();
		assertThat(result.get().getImmutableKantenAbschnittBezug()).isEmpty();
		assertThat(result.get().getImmutableKnotenBezug()).isEmpty();
		assertThat(result.get().getImmutableKantenPunktBezug()).hasSize(1);
		PunktuellerKantenSeitenBezug punktbezug = result.get().getImmutableKantenPunktBezug().stream().findFirst()
			.get();
		assertThat(punktbezug.getKante()).isEqualTo(naechsteKante);
		assertThat(LineareReferenz.fractionEqual(punktbezug.getLineareReferenz(), LineareReferenz.of(0.25))).isTrue();
		assertThat(punktbezug.getSeitenbezug()).isEqualTo(Seitenbezug.BEIDSEITIG);
	}

	@Test
	void testeCreatePunktNetzbezug_nurKnoten_NimmtNaechstenKnoten() {
		// Arrange
		Point point = GeometryTestdataProvider.createPoint(new Coordinate(11.5, 10));
		Knoten closestKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(8, 8), QuellSystem.DLM)
			.build();
		Knoten otherKnoten1 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(16, 8), QuellSystem.DLM)
			.build();
		Knoten otherKnoten2 = KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(3, 3), QuellSystem.DLM)
			.build();

		Envelope expectedEnvelope = point.getEnvelopeInternal();
		expectedEnvelope.expandBy(MassnahmeNetzbezugService.GEOMETRIE_SUCHRADIUS_IN_METER);
		when(netzService.getRadVisNetzKantenInBereich(expectedEnvelope)).thenReturn(new ArrayList<>(List.of()));
		when(netzService.getRadVisNetzKnotenInBereich(expectedEnvelope)).thenReturn(
			new ArrayList<>(List.of(otherKnoten1, closestKnoten, otherKnoten2)));

		// Act
		Optional<MassnahmeNetzBezug> result = service.createNetzbezugForPoint(point);

		// Assert
		assertThat(result).isPresent();
		assertThat(result.get().getImmutableKantenAbschnittBezug()).isEmpty();
		assertThat(result.get().getImmutableKantenPunktBezug()).isEmpty();
		assertThat(result.get().getImmutableKnotenBezug()).containsExactly(closestKnoten);
	}
}