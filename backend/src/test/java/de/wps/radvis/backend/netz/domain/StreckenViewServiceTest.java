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

package de.wps.radvis.backend.netz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.entity.StreckenEinerPartition;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.StreckenViewService;

class StreckenViewServiceTest {

	private StreckenViewService streckenViewService;

	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@BeforeEach
	void setUp() {
		streckenViewService = new StreckenViewService();
	}

	@Test
	void testeCreateStreckenEinerPartition_alleKantenImAusschnitt_ErkenntAlleStreckenRichtig() {
		// Arrange
		Envelope tatsaechlicherAusschnitt = new Envelope(1000, 2000, 1000, 2000);

		// started am Zentralen Punkt, geht hoch
		List<Coordinate[]> strecke1Koordinaten = new ArrayList<>();
		strecke1Koordinaten.add(new Coordinate[] {
			new Coordinate(1500, 1500),
			new Coordinate(1500, 1600),
			new Coordinate(1550, 1650),
			new Coordinate(1450, 1700),
		});
		strecke1Koordinaten.add(new Coordinate[] {
			new Coordinate(1450, 1700),
			new Coordinate(1500, 1750),
			new Coordinate(1520, 1800),
			new Coordinate(1500, 1900),
		});

		// started am Zentralen Punkt, geht nach rechts unten
		List<Coordinate[]> strecke2Koordinaten = new ArrayList<>();
		strecke2Koordinaten.add(new Coordinate[] {
			new Coordinate(1500, 1500),
			new Coordinate(1600, 1400),
			new Coordinate(1650, 1350),
		});
		strecke2Koordinaten.add(new Coordinate[] {
			new Coordinate(1650, 1350),
			new Coordinate(1700, 1200),
			new Coordinate(1800, 1150),
		});
		strecke2Koordinaten.add(new Coordinate[] {
			new Coordinate(1800, 1150),
			new Coordinate(1820, 1130),
		});

		// started unten Links, endet im Mittelpunkt
		List<Coordinate[]> strecke3Koordinaten = new ArrayList<>();
		strecke3Koordinaten.add(new Coordinate[] {
			new Coordinate(1100, 1100),
			new Coordinate(1300, 1300),
			new Coordinate(1350, 1300),
		});
		strecke3Koordinaten.add(new Coordinate[] {
			new Coordinate(1350, 1300),
			new Coordinate(1400, 1430),
			new Coordinate(1500, 1500),
		});

		// verbindet Strecke 2 und Strecke 3 direkt
		List<Coordinate[]> strecke4Koordinaten = new ArrayList<>();
		strecke4Koordinaten.add(new Coordinate[] {
			new Coordinate(1100, 1100),
			new Coordinate(1200, 1100),
		});
		strecke4Koordinaten.add(new Coordinate[] {
			new Coordinate(1200, 1100),
			new Coordinate(1500, 1100),
			new Coordinate(1820, 1130),
		});

		// Endet in Kreuzungspunkt von Strecke2 und Strecke 4
		List<Coordinate[]> strecke5Koordinaten = new ArrayList<>();
		strecke5Koordinaten.add(new Coordinate[] {
			new Coordinate(1820, 1050),
			new Coordinate(1820, 1130),
		});

		// Faengt an am Kreuzungspunkt von Strecke3 und Strecke 4
		List<Coordinate[]> strecke6Koordinaten = new ArrayList<>();
		strecke6Koordinaten.add(new Coordinate[] {
			new Coordinate(1100, 1100),
			new Coordinate(1050, 1050)
		});

		AtomicLong currentKnotenId = new AtomicLong();
		AtomicLong currentKanteId = new AtomicLong();

		Knoten zentralerKreuzungsknoten = KnotenTestDataProvider
			.withCoordinateAndQuelle(new Coordinate(1500, 1500), QuellSystem.DLM)
			.id(currentKnotenId.incrementAndGet()).build();

		List<Kante> strecke1 = KanteTestDataProvider
			.createStreckeUeberCoordinates(strecke1Koordinaten, zentralerKreuzungsknoten, null,
				currentKnotenId, currentKanteId);

		List<Kante> strecke2 = KanteTestDataProvider
			.createStreckeUeberCoordinates(strecke2Koordinaten, zentralerKreuzungsknoten, null,
				currentKnotenId, currentKanteId);

		List<Kante> strecke3 = KanteTestDataProvider
			.createStreckeUeberCoordinates(strecke3Koordinaten, null, zentralerKreuzungsknoten,
				currentKnotenId, currentKanteId);

		List<Kante> strecke4 = KanteTestDataProvider.createStreckeUeberCoordinates(strecke4Koordinaten,
			strecke3.get(0).getVonKnoten(), strecke2.get(strecke2.size() - 1).getNachKnoten(), currentKnotenId,
			currentKanteId);

		List<Kante> strecke5 = KanteTestDataProvider.createStreckeUeberCoordinates(strecke5Koordinaten,
			null, strecke4.get(strecke4.size() - 1).getNachKnoten(), currentKnotenId, currentKanteId);

		List<Kante> strecke6 = KanteTestDataProvider
			.createStreckeUeberCoordinates(strecke6Koordinaten, strecke4.get(0).getVonKnoten(), null,
				currentKnotenId, currentKanteId);

		List<Kante> alleKanten = new ArrayList<>();
		alleKanten.addAll(strecke1);
		alleKanten.addAll(strecke2);
		alleKanten.addAll(strecke3);
		alleKanten.addAll(strecke4);
		alleKanten.addAll(strecke5);
		alleKanten.addAll(strecke6);

		// Act
		StreckenEinerPartition<StreckeVonKanten> streckenEinerPartition = streckenViewService
			.createStreckenEinerPartition(alleKanten, tatsaechlicherAusschnitt, new HashSet<>());

		// Assert
		assertThat(streckenEinerPartition.vollstaendig).hasSize(6);
		assertThat(streckenEinerPartition.unvollstaendig).isEmpty();

		List<List<Kante>> kantenDerStrecken = streckenEinerPartition.vollstaendig.stream()
			.map(StreckeVonKanten::getKanten)
			.collect(Collectors.toList());

		assertStreckeIstInErgebnis(strecke1, kantenDerStrecken);
		assertEqualGeometryInOrderOrReverseOrder(strecke1, streckenEinerPartition.vollstaendig);
		assertStreckeIstInErgebnis(strecke2, kantenDerStrecken);
		assertEqualGeometryInOrderOrReverseOrder(strecke2, streckenEinerPartition.vollstaendig);
		assertStreckeIstInErgebnis(strecke3, kantenDerStrecken);
		assertEqualGeometryInOrderOrReverseOrder(strecke3, streckenEinerPartition.vollstaendig);
		assertStreckeIstInErgebnis(strecke4, kantenDerStrecken);
		assertEqualGeometryInOrderOrReverseOrder(strecke4, streckenEinerPartition.vollstaendig);
		assertStreckeIstInErgebnis(strecke5, kantenDerStrecken);
		assertEqualGeometryInOrderOrReverseOrder(strecke5, streckenEinerPartition.vollstaendig);
		assertStreckeIstInErgebnis(strecke6, kantenDerStrecken);
		assertEqualGeometryInOrderOrReverseOrder(strecke6, streckenEinerPartition.vollstaendig);

		assertThat(streckenEinerPartition.vollstaendig)
			.allMatch(s -> s.isVonKnotenEndpunkt() && s.isNachKnotenEndpunkt());
		assertThat(streckenEinerPartition.unvollstaendig)
			.noneMatch(s -> s.isVonKnotenEndpunkt() && s.isNachKnotenEndpunkt());
	}

	@Test
	void testeStationierungsrichtungAendertSichInnerhalbEinerStrecke_BleibtEineStrecke() {
		Kante kante1 = KanteTestDataProvider.withDefaultValues().id(1L)
			.geometry(GEO_FACTORY.createLineString(
				new Coordinate[] {
					new Coordinate(1500, 1500),
					new Coordinate(1600, 1400),
					new Coordinate(1650, 1350),
				}))
			.vonKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1500, 1500), QuellSystem.DLM)
					.id(1L).build())
			.nachKnoten(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1650, 1350), QuellSystem.RadNETZ)
				.id(2L).build())
			.build();
		Kante reversedKante = KanteTestDataProvider.withDefaultValues()
			.id(2L)
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(1800, 1150),
				new Coordinate(1700, 1200),
				new Coordinate(1650, 1350),
			}))
			.nachKnoten(kante1.getNachKnoten())
			.vonKnoten(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1800, 1150), QuellSystem.RadNETZ)
				.id(3L).build())
			.build();
		Kante kante2 = KanteTestDataProvider.withDefaultValues()
			.id(3L)
			.geometry(GEO_FACTORY.createLineString(new Coordinate[] {
				new Coordinate(1800, 1150),
				new Coordinate(1820, 1130),
			}))
			.vonKnoten(reversedKante.getVonKnoten())
			.nachKnoten(
				KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(1820, 1130), QuellSystem.RadNETZ)
					.id(4L).build())
			.build();

		List<Kante> strecke = List.of(kante1, reversedKante, kante2);

		List<Kante> alleKanten = new ArrayList<>(strecke);

		// act
		final var streckenEinerPartition = streckenViewService
			.createStreckenEinerPartition(alleKanten, new Envelope(1000, 2000, 1000, 2000), new HashSet<>());

		assertThat(streckenEinerPartition.vollstaendig).hasSize(1);
		assertThat(streckenEinerPartition.unvollstaendig).hasSize(0);
		StreckeVonKanten resultStrecke = streckenEinerPartition.vollstaendig.get(0);
		assertThat(kante1.getVonKnoten().equals(resultStrecke.getVonKnoten())
			|| kante2.getNachKnoten().equals(resultStrecke.getVonKnoten())).isTrue();
		assertThat(kante1.getVonKnoten().equals(resultStrecke.getNachKnoten())
			|| kante2.getNachKnoten().equals(resultStrecke.getNachKnoten())).isTrue();

		LineString expectedGeometry = GEO_FACTORY.createLineString(new Coordinate[] {
			new Coordinate(1500, 1500),
			new Coordinate(1600, 1400),
			new Coordinate(1650, 1350),
			new Coordinate(1700, 1200),
			new Coordinate(1800, 1150),
			new Coordinate(1820, 1130),
		});

		isEqualInOrderOrReverseOrder(resultStrecke, expectedGeometry);
	}

	@Test
	void teste_IgnoriereKantenVollstaendigAusserhalbPartitionenUndErkenntUnvollstaendigeStrecken() {
		// Arrange

		List<Coordinate[]> coordinatesEinerKanteHalbAusserhalb = new ArrayList<>();
		coordinatesEinerKanteHalbAusserhalb.add(new Coordinate[] {
			new Coordinate(800, 800),
			new Coordinate(900, 850),
			new Coordinate(1001, 1001),
		});
		coordinatesEinerKanteHalbAusserhalb.add(new Coordinate[] {
			new Coordinate(1001, 1001),
			new Coordinate(1100, 1001),
			new Coordinate(1100, 1100),
		});

		// verbunden mit coordinatesEinerKanteHalbAusserhalb
		List<Coordinate[]> vollstaendigAusserhalbGehtWeiter1 = new ArrayList<>();
		vollstaendigAusserhalbGehtWeiter1.add(new Coordinate[] {
			new Coordinate(800, 800),
			new Coordinate(700, 700),
		});
		vollstaendigAusserhalbGehtWeiter1.add(new Coordinate[] {
			new Coordinate(700, 700),
			new Coordinate(650, 650),
		});
		// verbunden mit coordinatesEinerKanteHalbAusserhalb
		List<Coordinate[]> vollstaendigAusserhalbGehtWeiter2 = new ArrayList<>();
		vollstaendigAusserhalbGehtWeiter2.add(new Coordinate[] {
			new Coordinate(800, 800),
			new Coordinate(700, 850),
		});
		vollstaendigAusserhalbGehtWeiter2.add(new Coordinate[] {
			new Coordinate(700, 850),
			new Coordinate(650, 900),
		});

		List<Coordinate[]> eineKanteAusserhalb = new ArrayList<>();
		eineKanteAusserhalb.add(new Coordinate[] {
			new Coordinate(1800, 800),
			new Coordinate(1800, 850),
			new Coordinate(1900, 950),
		});
		eineKanteAusserhalb.add(new Coordinate[] {
			new Coordinate(1900, 950),
			new Coordinate(1900, 1001),
			new Coordinate(1900, 1100),
		});
		eineKanteAusserhalb.add(new Coordinate[] {
			new Coordinate(1900, 1100),
			new Coordinate(1800, 1300),
		});

		// verbunden mit eineKanteAusserhalb
		List<Coordinate[]> innerhalb1 = new ArrayList<>();
		innerhalb1.add(new Coordinate[] {
			new Coordinate(1800, 1300),
			new Coordinate(1700, 1400),
		});
		innerhalb1.add(new Coordinate[] {
			new Coordinate(1700, 1400),
			new Coordinate(1600, 1300),
		});
		// verbunden mit eineKanteAusserhalb
		List<Coordinate[]> innerhalb2 = new ArrayList<>();
		innerhalb2.add(new Coordinate[] {
			new Coordinate(1800, 1300),
			new Coordinate(1850, 1400),
		});
		innerhalb2.add(new Coordinate[] {
			new Coordinate(1850, 1400),
			new Coordinate(1950, 1600),
		});

		List<Coordinate[]> schneidetAusschnittAberKnotenAusserhalb = new ArrayList<>();
		schneidetAusschnittAberKnotenAusserhalb.add(new Coordinate[] {
			new Coordinate(850, 1200),
			new Coordinate(1200, 850),
		});

		AtomicLong knotenId = new AtomicLong();
		AtomicLong kanteId = new AtomicLong();

		List<Kante> streckeMitKanteHalbAusserhalb = KanteTestDataProvider
			.createStreckeUeberCoordinates(coordinatesEinerKanteHalbAusserhalb,
				null,
				null, knotenId, kanteId);
		List<Kante> streckeAusserhalb1 = KanteTestDataProvider
			.createStreckeUeberCoordinates(vollstaendigAusserhalbGehtWeiter1,
				streckeMitKanteHalbAusserhalb.get(0).getVonKnoten(),
				null, knotenId, kanteId);
		List<Kante> streckeAusserhalb2 = KanteTestDataProvider
			.createStreckeUeberCoordinates(vollstaendigAusserhalbGehtWeiter2,
				streckeMitKanteHalbAusserhalb.get(0).getVonKnoten(),
				null, knotenId, kanteId);

		List<Kante> streckeMitEinerKanteAusserhalb = KanteTestDataProvider
			.createStreckeUeberCoordinates(eineKanteAusserhalb,
				null,
				null, knotenId, kanteId);
		List<Kante> streckeInnerhalb1 = KanteTestDataProvider.createStreckeUeberCoordinates(innerhalb1,
			streckeMitEinerKanteAusserhalb.get(streckeMitEinerKanteAusserhalb.size() - 1).getNachKnoten(),
			null, knotenId, kanteId);
		List<Kante> streckeInnerhalb2 = KanteTestDataProvider.createStreckeUeberCoordinates(innerhalb2,
			streckeMitEinerKanteAusserhalb.get(streckeMitEinerKanteAusserhalb.size() - 1).getNachKnoten(),
			null, knotenId, kanteId);

		List<Kante> streckeSchneidetAusschnittAberKnotenAusserhalb = KanteTestDataProvider
			.createStreckeUeberCoordinates(
				schneidetAusschnittAberKnotenAusserhalb, null, null, knotenId, kanteId);

		List<Kante> alleKanten = new ArrayList<>();
		alleKanten.addAll(streckeMitKanteHalbAusserhalb);
		alleKanten.addAll(streckeAusserhalb1);
		alleKanten.addAll(streckeAusserhalb2);
		alleKanten.addAll(streckeMitEinerKanteAusserhalb);
		alleKanten.addAll(streckeInnerhalb1);
		alleKanten.addAll(streckeInnerhalb2);
		alleKanten.addAll(streckeSchneidetAusschnittAberKnotenAusserhalb);

		Envelope actualEnvelope = new Envelope(1000, 2000, 1000, 2000);

		// Act
		final var streckenEinerPartition = streckenViewService
			.createStreckenEinerPartition(alleKanten, actualEnvelope, new HashSet<>());

		assertThat(streckenEinerPartition.vollstaendig).hasSize(4);
		List<List<Kante>> vollstaendigeKanten = streckenEinerPartition.vollstaendig.stream()
			.map(StreckeVonKanten::getKanten).collect(Collectors.toList());
		assertStreckeIstInErgebnis(streckeMitKanteHalbAusserhalb, vollstaendigeKanten);
		assertEqualGeometryInOrderOrReverseOrder(streckeMitKanteHalbAusserhalb, streckenEinerPartition.vollstaendig);
		assertStreckeIstInErgebnis(streckeInnerhalb1, vollstaendigeKanten);
		assertEqualGeometryInOrderOrReverseOrder(streckeInnerhalb1, streckenEinerPartition.vollstaendig);
		assertStreckeIstInErgebnis(streckeInnerhalb2, vollstaendigeKanten);
		assertEqualGeometryInOrderOrReverseOrder(streckeInnerhalb2, streckenEinerPartition.vollstaendig);
		assertStreckeIstInErgebnis(streckeSchneidetAusschnittAberKnotenAusserhalb, vollstaendigeKanten);
		assertEqualGeometryInOrderOrReverseOrder(streckeSchneidetAusschnittAberKnotenAusserhalb,
			streckenEinerPartition.vollstaendig);

		assertThat(streckenEinerPartition.unvollstaendig).hasSize(1);
		List<List<Kante>> unvollstaendigeKanten = streckenEinerPartition.unvollstaendig.stream()
			.map(StreckeVonKanten::getKanten).collect(Collectors.toList());
		assertStreckeIstInErgebnis(streckeMitEinerKanteAusserhalb.subList(1, 3), unvollstaendigeKanten);
		assertEqualGeometryInOrderOrReverseOrder(streckeMitEinerKanteAusserhalb.subList(1, 3),
			streckenEinerPartition.unvollstaendig);
	}

	@Test
	void testeAlgorithmusTerminiertBeiKreisbahnUndErgebnisEnthaeltKreisStrecke() {
		// baue DreieckStrecke

		List<Coordinate[]> oberesEck = new ArrayList<>();
		oberesEck.add(new Coordinate[] {
			new Coordinate(1200, 1200),
			new Coordinate(1300, 1400),
			new Coordinate(1500, 1600),
		});
		oberesEck.add(new Coordinate[] {
			new Coordinate(1500, 1600),
			new Coordinate(1700, 1400),
			new Coordinate(1800, 1200),
		});

		List<Coordinate[]> verbindunsLinie = new ArrayList<>();
		verbindunsLinie.add(new Coordinate[] {
			new Coordinate(1800, 1200),
			new Coordinate(1200, 1200),
		});

		AtomicLong knotenId = new AtomicLong();
		AtomicLong kanteId = new AtomicLong();

		List<Kante> streckeOberesEck = KanteTestDataProvider.createStreckeUeberCoordinates(oberesEck,
			null,
			null, knotenId, kanteId);

		List<Kante> streckeVerbindungsLinie = KanteTestDataProvider.createStreckeUeberCoordinates(verbindunsLinie,
			streckeOberesEck.get(0).getVonKnoten(),
			streckeOberesEck.get(streckeOberesEck.size() - 1).getNachKnoten(), knotenId, kanteId);

		List<Kante> alleKanten = new ArrayList<>();
		alleKanten.addAll(streckeOberesEck);
		alleKanten.addAll(streckeVerbindungsLinie);

		// act
		final var streckenEinerPartition = streckenViewService
			.createStreckenEinerPartition(alleKanten, new Envelope(1000, 2000, 1000, 2000), new HashSet<>());

		assertThat(streckenEinerPartition.vollstaendig).hasSize(1);
		assertThat(streckenEinerPartition.vollstaendig.get(0).getKanten())
			.containsOnlyOnceElementsOf(streckeOberesEck)
			.containsOnlyOnceElementsOf(streckeVerbindungsLinie);
		assertThat(streckenEinerPartition.vollstaendig.get(0).getVonKnoten())
			.isEqualTo(streckenEinerPartition.vollstaendig.get(0).getNachKnoten());
	}

	@Test
	void mergeUnvollstaendigeStrecken_zusammenhaengendeUnvollstaendigeStreckenWerdenGemerged() {
		ArrayList<Coordinate[]> mittelstueck = new ArrayList<>();

		mittelstueck.add(new Coordinate[] {
			new Coordinate(1500, 1500),
			new Coordinate(1550, 1550),
			new Coordinate(1600, 1600)
		});
		mittelstueck.add(new Coordinate[] {
			new Coordinate(1600, 1600),
			new Coordinate(1650, 1650)
		});

		ArrayList<Coordinate[]> anfangsstueck = new ArrayList<>();

		anfangsstueck.add(new Coordinate[] {
			new Coordinate(1300, 1300),
			new Coordinate(1350, 1350),
			new Coordinate(1400, 1400)
		});
		anfangsstueck.add(new Coordinate[] {
			new Coordinate(1400, 1400),
			new Coordinate(1500, 1500),
		});

		ArrayList<Coordinate[]> endstueck = new ArrayList<>();

		endstueck.add(new Coordinate[] {
			new Coordinate(1650, 1650),
			new Coordinate(1700, 1700),
			new Coordinate(1750, 1750)
		});
		endstueck.add(new Coordinate[] {
			new Coordinate(1750, 1750),
			new Coordinate(1800, 1800),
		});

		AtomicLong knotenId = new AtomicLong();
		AtomicLong kanteId = new AtomicLong();

		List<Kante> anfang = KanteTestDataProvider.createStreckeUeberCoordinates(anfangsstueck, null, null, knotenId,
			kanteId);
		List<Kante> mitte = KanteTestDataProvider
			.createStreckeUeberCoordinates(mittelstueck, anfang.get(anfang.size() - 1).getNachKnoten(),
				null, knotenId, kanteId);
		List<Kante> ende = KanteTestDataProvider
			.createStreckeUeberCoordinates(endstueck, mitte.get(mitte.size() - 1).getNachKnoten(),
				null, knotenId, kanteId);

		StreckeVonKanten anfangsStrecke = new StreckeVonKanten(anfang.get(0), true, false);
		anfangsStrecke.addKante(anfang.get(1), false);
		StreckeVonKanten mitteStrecke = new StreckeVonKanten(mitte.get(1), false, false);
		mitteStrecke.addKante(mitte.get(0), false);
		StreckeVonKanten endStrecke = new StreckeVonKanten(ende.get(1), false, true);
		endStrecke.addKante(ende.get(0), false);

		final var streckenEinerPartition = streckenViewService
			.mergeUnvollstaendigeStrecken(List.of(mitteStrecke, endStrecke, anfangsStrecke));

		assertThat(streckenEinerPartition.unvollstaendig).hasSize(0);
		assertThat(streckenEinerPartition.vollstaendig).hasSize(1);

		List<Kante> gesamteStreckeAlsKanten =
			Stream.concat(Stream.concat(anfang.stream(), mitte.stream()), ende.stream())
				.collect(Collectors.toList());
		assertEqualInOrderOrReverseOrder(gesamteStreckeAlsKanten,
			streckenEinerPartition.vollstaendig.get(0).getKanten());
		assertEqualGeometryInOrderOrReverseOrder(gesamteStreckeAlsKanten, streckenEinerPartition.vollstaendig);
	}

	@Test
	void mergeUnvollstaendigeStrecken_NichtMergenWennEndpunkte() {
		ArrayList<Coordinate[]> mittelstueck = new ArrayList<>();

		mittelstueck.add(new Coordinate[] {
			new Coordinate(1500, 1500),
			new Coordinate(1550, 1550),
			new Coordinate(1600, 1600)
		});
		mittelstueck.add(new Coordinate[] {
			new Coordinate(1600, 1600),
			new Coordinate(1650, 1650)
		});

		ArrayList<Coordinate[]> anfangsstueck = new ArrayList<>();

		anfangsstueck.add(new Coordinate[] {
			new Coordinate(1300, 1300),
			new Coordinate(1350, 1350),
			new Coordinate(1400, 1400)
		});
		anfangsstueck.add(new Coordinate[] {
			new Coordinate(1400, 1400),
			new Coordinate(1500, 1500),
		});

		ArrayList<Coordinate[]> endstueck = new ArrayList<>();

		endstueck.add(new Coordinate[] {
			new Coordinate(1650, 1650),
			new Coordinate(1700, 1700),
			new Coordinate(1750, 1750)
		});
		endstueck.add(new Coordinate[] {
			new Coordinate(1750, 1750),
			new Coordinate(1800, 1800),
		});

		AtomicLong knotenId = new AtomicLong();
		AtomicLong kanteId = new AtomicLong();

		List<Kante> anfang = KanteTestDataProvider.createStreckeUeberCoordinates(anfangsstueck, null, null, knotenId,
			kanteId);
		List<Kante> mitte = KanteTestDataProvider
			.createStreckeUeberCoordinates(mittelstueck, anfang.get(anfang.size() - 1).getNachKnoten(),
				null, knotenId, kanteId);
		List<Kante> ende = KanteTestDataProvider
			.createStreckeUeberCoordinates(endstueck, mitte.get(mitte.size() - 1).getNachKnoten(),
				null, knotenId, kanteId);

		StreckeVonKanten anfangsStrecke = new StreckeVonKanten(anfang.get(0), false, false);
		anfangsStrecke.addKante(anfang.get(1), true);
		StreckeVonKanten mitteStrecke = new StreckeVonKanten(mitte.get(1), false, false);
		mitteStrecke.addKante(mitte.get(0), false);
		StreckeVonKanten endStrecke = new StreckeVonKanten(ende.get(1), false, false);
		endStrecke.addKante(ende.get(0), true);

		final var streckenEinerPartition = streckenViewService
			.mergeUnvollstaendigeStrecken(List.of(mitteStrecke, endStrecke, anfangsStrecke));

		assertThat(streckenEinerPartition.unvollstaendig).hasSize(3);
		assertThat(streckenEinerPartition.vollstaendig).hasSize(0);

		List<List<Kante>> alleKantenInErgebnis = streckenEinerPartition.unvollstaendig.stream()
			.map(StreckeVonKanten::getKanten)
			.collect(Collectors.toList());
		assertStreckeIstInErgebnis(anfang, alleKantenInErgebnis);
		assertStreckeIstInErgebnis(mitte, alleKantenInErgebnis);
		assertStreckeIstInErgebnis(ende, alleKantenInErgebnis);
		assertEqualGeometryInOrderOrReverseOrder(anfang, streckenEinerPartition.unvollstaendig);
		assertEqualGeometryInOrderOrReverseOrder(mitte, streckenEinerPartition.unvollstaendig);
		assertEqualGeometryInOrderOrReverseOrder(ende, streckenEinerPartition.unvollstaendig);
	}

	private void assertStreckeIstInErgebnis(List<Kante> expectedStrecke, List<List<Kante>> resultStrecken) {
		Optional<List<Kante>> streckeInResult = resultStrecken.stream()
			.filter(strecke -> strecke.stream().anyMatch(expectedStrecke::contains))
			.findFirst();
		assertThat(streckeInResult).isPresent();
		assertEqualInOrderOrReverseOrder(expectedStrecke, streckeInResult.get());
	}

	private void assertEqualGeometryInOrderOrReverseOrder(List<Kante> expectedStrecke,
		List<StreckeVonKanten> alleStreckenInResult) {
		Optional<StreckeVonKanten> streckeInResult = alleStreckenInResult.stream()
			.filter(strecke -> strecke.getKanten().stream()
				.anyMatch(expectedStrecke::contains)).findFirst();
		assertThat(streckeInResult).isPresent();

		LineString expectedGeometry = mergeKantenInEinzelnenLineString(expectedStrecke);
		isEqualInOrderOrReverseOrder(streckeInResult.get(), expectedGeometry);
	}

	private LineString mergeKantenInEinzelnenLineString(List<Kante> kanten) {
		List<Coordinate> expectedCoordinates = new ArrayList<>();
		for (Kante kante : kanten) {
			expectedCoordinates.addAll(Arrays.asList(kante.getGeometry().getCoordinates()));
		}

		// Aufeinanderfolgende doppelte Koordinaten rausfiltern
		AtomicReference<Coordinate> previous = new AtomicReference<>(null);
		expectedCoordinates = expectedCoordinates.stream()
			.filter(coordinate -> !coordinate.equals(previous.getAndSet(coordinate)))
			.collect(Collectors.toList());

		LineString expectedGeometry = GEO_FACTORY.createLineString(expectedCoordinates.toArray(Coordinate[]::new));
		return expectedGeometry;
	}

	private void isEqualInOrderOrReverseOrder(StreckeVonKanten streckeInResult, LineString expectedGeometry) {
		assertThat(streckeInResult.getStrecke().getCoordinates())
			.satisfiesAnyOf(
				s -> assertThat(s).containsExactlyElementsOf(
					Arrays.stream(expectedGeometry.getCoordinates()).collect(Collectors.toList())),
				s -> assertThat(s).containsExactlyElementsOf(
					Arrays.stream(expectedGeometry.reverse().getCoordinates()).collect(Collectors.toList())));
	}

	private void assertEqualInOrderOrReverseOrder(List<Kante> expected, List<Kante> actual) {
		assertThat(actual)
			.satisfiesAnyOf(s ->
				assertThat(s).isEqualTo(expected), s -> {
				List<Kante> reversed = new ArrayList<>(expected);
				Collections.reverse(reversed);
				assertThat(s).isEqualTo(reversed);
			});
	}
}
