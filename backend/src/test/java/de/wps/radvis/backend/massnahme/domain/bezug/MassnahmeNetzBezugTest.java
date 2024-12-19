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

package de.wps.radvis.backend.massnahme.domain.bezug;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;

class MassnahmeNetzBezugTest {

	@Test
	void istWertsemantisch() {
		MassnahmeNetzBezug mitKantenbezug_1 = new MassnahmeNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG)),
			Set.of(),
			Set.of());
		MassnahmeNetzBezug mitKantenbezug_1_copy = new MassnahmeNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG)),
			Set.of(),
			Set.of());
		MassnahmeNetzBezug mitKantenbezug_2 = new MassnahmeNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				KanteTestDataProvider.withDefaultValues().id(2L).build(),
				LinearReferenzierterAbschnitt.of(0, 0.5), Seitenbezug.BEIDSEITIG)),
			Set.of(),
			Set.of());
		MassnahmeNetzBezug mitKnotenezug_1 = new MassnahmeNetzBezug(Set.of(),
			Set.of(),
			Set.of(KnotenTestDataProvider.withDefaultValues().id(1L).build()));
		MassnahmeNetzBezug mitKnotenezug_1_copy = new MassnahmeNetzBezug(Set.of(),
			Set.of(),
			Set.of(KnotenTestDataProvider.withDefaultValues().id(1L).build()));
		MassnahmeNetzBezug mitKnotenezug_2 = new MassnahmeNetzBezug(Set.of(),
			Set.of(),
			Set.of(KnotenTestDataProvider.withDefaultValues().id(2L).build()));

		assertThat(mitKantenbezug_1).isEqualTo(mitKantenbezug_1_copy);
		assertThat(mitKnotenezug_1).isEqualTo(mitKnotenezug_1_copy);
		assertThat(mitKnotenezug_1).isNotEqualTo(mitKnotenezug_2);
		assertThat(mitKantenbezug_1).isNotEqualTo(mitKantenbezug_2);
	}

	@Test
	void mindestensEineKanteOderKnoten_throws() {
		// Act + Assert
		assertThrows(RequireViolation.class, () -> new MassnahmeNetzBezug(Set.of(), Set.of(), Set.of()));
	}

	@Test
	void mindestensEineKanteOderKnoten_notThrows() {
		// Act + Assert
		assertDoesNotThrow(
			() -> new MassnahmeNetzBezug(Set.of(mock(AbschnittsweiserKantenSeitenBezug.class)), Set.of(), Set.of()));
	}

	@Test
	void getGeometrie() {
		Kante kante = KanteTestDataProvider.withDefaultValues().geometry(
			GeometryTestdataProvider.createLineString(new Coordinate(0, 0), new Coordinate(100, 100))).build();
		MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(kante, LinearReferenzierterAbschnitt.of(0.2, 0.6),
				Seitenbezug.BEIDSEITIG)),
			Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.7), Seitenbezug.BEIDSEITIG)),
			Set.of(
				KnotenTestDataProvider.withDefaultValues()
					.point(
						KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
							.createPoint(new Coordinate(10, 100)))
					.build()));

		Geometry geometry = netzbezug.getGeometrie();

		assertThat(geometry.getNumGeometries()).isEqualTo(3);
		assertThat(List.of(geometry.getGeometryN(0), geometry.getGeometryN(1), geometry.getGeometryN(2)))
			.containsExactlyInAnyOrder(
				GeometryTestdataProvider.createLineString(new Coordinate(20, 20), new Coordinate(60, 60)),
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(70, 70)),
				KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(new Coordinate(10, 100)));
	}

	@Test
	void defragment_joinAdjacent() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0, 0.5), Seitenbezug.BEIDSEITIG),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.5, 1), Seitenbezug.BEIDSEITIG),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(2L).build(),
				LinearReferenzierterAbschnitt.of(0.3, 0.5), Seitenbezug.BEIDSEITIG),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(2L).build(),
				LinearReferenzierterAbschnitt.of(0.5, 0.7), Seitenbezug.BEIDSEITIG));
		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>());

		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug()).hasSize(2);
		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug())
			.contains(new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG));
		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug())
			.contains(new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(2L).build(),
				LinearReferenzierterAbschnitt.of(0.3, 0.7), Seitenbezug.BEIDSEITIG));
	}

	@Test
	void defragment_defragmentiertNurElementeMitGleichemSeitenbezug() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0, 0.2), Seitenbezug.LINKS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.4), Seitenbezug.LINKS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.1, 0.3), Seitenbezug.RECHTS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.3, 0.5), Seitenbezug.RECHTS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.5, 0.8), Seitenbezug.BEIDSEITIG),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.8, 0.9), Seitenbezug.BEIDSEITIG));
		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>());

		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug()).hasSize(3);
		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug())
			.containsExactlyInAnyOrder(
				new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
					LinearReferenzierterAbschnitt.of(0, 0.4), Seitenbezug.LINKS),
				new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
					LinearReferenzierterAbschnitt.of(0.1, 0.5), Seitenbezug.RECHTS),
				new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
					LinearReferenzierterAbschnitt.of(0.5, 0.9), Seitenbezug.BEIDSEITIG));
	}

	@Test
	void defragment_faesstIdentischeAbschnitteLinksUndRechtsZusammen() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.0, 1.0), Seitenbezug.LINKS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0., 1.0), Seitenbezug.RECHTS));
		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>());

		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug()).hasSize(1);
		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug())
			.containsExactlyInAnyOrder(
				new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
					LinearReferenzierterAbschnitt.of(0, 1.), Seitenbezug.BEIDSEITIG));
	}

	@Test
	void defragment_LRMergeAgainNachSeiteMerge() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.0, 0.5), Seitenbezug.LINKS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.5, 1.0), Seitenbezug.BEIDSEITIG),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.0, 0.5), Seitenbezug.RECHTS));
		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>());

		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug()).hasSize(1);
		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug())
			.containsExactlyInAnyOrder(
				new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
					LinearReferenzierterAbschnitt.of(0, 1.), Seitenbezug.BEIDSEITIG));
	}

	@Test
	void defragment_erstLRMergeDannSeiteMerge() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.0, 0.2), Seitenbezug.LINKS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.LINKS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.RECHTS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.6, 0.8), Seitenbezug.LINKS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.6, 0.8), Seitenbezug.RECHTS));
		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>());

		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug()).hasSize(3);
		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug())
			.containsExactlyInAnyOrder(
				new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
					LinearReferenzierterAbschnitt.of(0.6, 0.8), Seitenbezug.BEIDSEITIG),
				new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
					LinearReferenzierterAbschnitt.of(0.0, 0.5), Seitenbezug.LINKS),
				new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
					LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.RECHTS));
	}

	@Test
	void defragment_defragmentiertNurSegmenteAufGleicherKante() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.0, 0.2), Seitenbezug.LINKS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(2L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.LINKS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(3L).build(),
				LinearReferenzierterAbschnitt.of(0., 0.3), Seitenbezug.RECHTS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(4L).build(),
				LinearReferenzierterAbschnitt.of(0.3, 0.5), Seitenbezug.RECHTS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(5L).build(),
				LinearReferenzierterAbschnitt.of(0.5, 0.8), Seitenbezug.BEIDSEITIG),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(6L).build(),
				LinearReferenzierterAbschnitt.of(0.8, 1.), Seitenbezug.BEIDSEITIG));
		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>());

		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug()).hasSize(kantenBezuege.size());
		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug()).containsExactlyInAnyOrderElementsOf(
			kantenBezuege);
	}

	@Test
	void defragment_keepNonAdjacent() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0, 0.3), Seitenbezug.BEIDSEITIG),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(2L).build(),
				LinearReferenzierterAbschnitt.of(0.3, 0.5), Seitenbezug.BEIDSEITIG),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.5, 1), Seitenbezug.BEIDSEITIG));
		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>());

		assertThat(massnahmeNetzBezug.getImmutableKantenAbschnittBezug()).containsExactlyInAnyOrderElementsOf(
			kantenBezuege);
	}

	@Test
	void keineUeberlappungErlaubt_punktUeberlappungOkay() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.0, 0.2), Seitenbezug.LINKS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.LINKS));

		assertDoesNotThrow(() -> new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>()));
	}

	@Test
	void keineUeberlappungErlaubt_streckenUeberlappungEinerSeiteVerboten_Links() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.0, 0.3), Seitenbezug.LINKS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.LINKS));

		assertThatThrownBy(() -> new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>())).isInstanceOf(
			RequireViolation.class);
	}

	@Test
	void keineUeberlappungErlaubt_streckenUeberlappungEinerSeiteVerboten_Rechts() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.0, 0.3), Seitenbezug.RECHTS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.RECHTS));

		assertThatThrownBy(() -> new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>())).isInstanceOf(
			RequireViolation.class);
	}

	@Test
	void keineUeberlappungErlaubt_streckenUeberlappungLinksUndBeidseitig() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.0, 0.3), Seitenbezug.LINKS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.BEIDSEITIG));

		assertThatThrownBy(() -> new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>())).isInstanceOf(
			RequireViolation.class);
	}

	@Test
	void keineUeberlappungErlaubt_streckenUeberlappungRechtsUndBeidseitig() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.0, 0.3), Seitenbezug.RECHTS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.BEIDSEITIG));

		assertThatThrownBy(() -> new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>())).isInstanceOf(
			RequireViolation.class);
	}

	@Test
	void keineUeberlappungErlaubt_streckenUeberlappungBeideBeidseitig() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.0, 0.3), Seitenbezug.BEIDSEITIG),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.BEIDSEITIG));

		assertThatThrownBy(() -> new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>())).isInstanceOf(
			RequireViolation.class);
	}

	@Test
	void keineUeberlappungErlaubt_streckenUeberlappungLinksUndRechts_istErlaubt() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.0, 0.3), Seitenbezug.LINKS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.RECHTS));

		assertDoesNotThrow(() -> new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>()));
	}

	@Test
	void keineUeberlappungErlaubt_triggertNurAufKantenEbene() {
		Set<AbschnittsweiserKantenSeitenBezug> kantenBezuege = Set.of(
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(1L).build(),
				LinearReferenzierterAbschnitt.of(0.0, 0.3), Seitenbezug.RECHTS),
			new AbschnittsweiserKantenSeitenBezug(KanteTestDataProvider.withDefaultValues().id(2L).build(),
				LinearReferenzierterAbschnitt.of(0.2, 0.5), Seitenbezug.BEIDSEITIG));

		assertDoesNotThrow(() -> new MassnahmeNetzBezug(kantenBezuege, new HashSet<>(), new HashSet<>()));
	}

	@Nested
	class WithKanteErsetzt {
		@Test
		void withKanteErsetzt_kanteNotInNetzbezug_doesNothing() {
			// arrange
			Kante kante = KanteTestDataProvider.withDefaultValues().id(2l).build();
			AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug = new AbschnittsweiserKantenSeitenBezug(
				kante,
				LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.BEIDSEITIG);
			MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
				Set.of(abschnittsweiserKantenSeitenBezug),
				Collections.emptySet(),
				Collections.emptySet());

			// act
			MassnahmeNetzBezug result = massnahmeNetzBezug.withKanteErsetzt(
				KanteTestDataProvider.withDefaultValues().id(3l).build(),
				Set.of(KanteTestDataProvider.withDefaultValues().id(4l).build()), 1.0);

			// assert
			assertThat(result.getImmutableKantenAbschnittBezug()).containsExactly(abschnittsweiserKantenSeitenBezug);
		}

		@Test
		void withKanteErsetzt_punktuellerNetzbezug_updates() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
				.id(1l)
				.build();
			Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 50, 0, QuellSystem.DLM).id(2l)
				.build();
			Kante ersatzKante2 = KanteTestDataProvider.withCoordinatesAndQuelle(50, 0, 100, 0, QuellSystem.DLM).id(3l)
				.build();

			MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
				Collections.emptySet(),
				Set.of(new PunktuellerKantenSeitenBezug(zuErsetzendeKante, LineareReferenz.of(0.25),
					Seitenbezug.BEIDSEITIG)),
				Collections.emptySet());

			// act
			MassnahmeNetzBezug result = massnahmeNetzBezug.withKanteErsetzt(zuErsetzendeKante,
				Set.of(ersatzKante1, ersatzKante2), 1.0);

			// assert
			assertThat(result.getImmutableKantenPunktBezug())
				.containsExactly(new PunktuellerKantenSeitenBezug(ersatzKante1, LineareReferenz.of(0.5),
					Seitenbezug.BEIDSEITIG));
		}

		@Test
		void withKanteErsetzt_punktuellerNetzbezug_onDifferentKante_doesNothing() {
			// arrange
			Kante netzbezugKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
				.id(1l)
				.build();
			Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
				.id(2l)
				.build();
			Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 50, 0, QuellSystem.DLM).id(3l)
				.build();

			PunktuellerKantenSeitenBezug punktuellerKantenSeitenBezug = new PunktuellerKantenSeitenBezug(netzbezugKante,
				LineareReferenz.of(0.25),
				Seitenbezug.BEIDSEITIG);
			MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante, LinearReferenzierterAbschnitt.of(0, 1),
					Seitenbezug.BEIDSEITIG)),
				Set.of(punktuellerKantenSeitenBezug),
				Collections.emptySet());

			// act
			MassnahmeNetzBezug result = massnahmeNetzBezug.withKanteErsetzt(zuErsetzendeKante, Set.of(ersatzKante1),
				1.0);

			// assert
			assertThat(result.getImmutableKantenPunktBezug())
				.containsExactly(punktuellerKantenSeitenBezug);
		}

		@Test
		void withKanteErsetzt_punktuellerNetzbezug_withinToleranceAtEndOfNewKante_usesBestMatch() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
				.id(1l)
				.build();
			Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 48, 0, QuellSystem.DLM).id(2l)
				.build();
			Kante ersatzKante2 = KanteTestDataProvider.withCoordinatesAndQuelle(48, 0, 98, 0, QuellSystem.DLM).id(3l)
				.build();

			MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
				Collections.emptySet(),
				Set.of(new PunktuellerKantenSeitenBezug(zuErsetzendeKante, LineareReferenz.of(0.5),
					Seitenbezug.BEIDSEITIG)),
				Collections.emptySet());

			// act
			MassnahmeNetzBezug result = massnahmeNetzBezug.withKanteErsetzt(zuErsetzendeKante,
				Set.of(ersatzKante1, ersatzKante2), 5.0);

			// assert
			assertThat(result.getImmutableKantenPunktBezug()).hasSize(1);
			assertThat(result.getImmutableKantenPunktBezug())
				.contains(new PunktuellerKantenSeitenBezug(ersatzKante2, LineareReferenz.of(0.04),
					Seitenbezug.BEIDSEITIG));
		}

		@Test
		void withKanteErsetzt_punktuellerNetzbezug_neueKantenNotWithinTolerance_doesNothing() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
				.id(1l)
				.build();
			Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 2, 50, 2, QuellSystem.DLM).id(2l)
				.build();
			Kante ersatzKante2 = KanteTestDataProvider.withCoordinatesAndQuelle(50, 2, 100, 2, QuellSystem.DLM).id(3l)
				.build();

			MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
				Collections.emptySet(),
				Set.of(new PunktuellerKantenSeitenBezug(zuErsetzendeKante, LineareReferenz.of(0.25),
					Seitenbezug.BEIDSEITIG)),
				Collections.emptySet());

			// act
			MassnahmeNetzBezug result = massnahmeNetzBezug.withKanteErsetzt(zuErsetzendeKante,
				Set.of(ersatzKante1, ersatzKante2), 1.0);

			// assert
			assertThat(result.getImmutableKantenPunktBezug())
				.containsExactly(new PunktuellerKantenSeitenBezug(zuErsetzendeKante, LineareReferenz.of(0.25),
					Seitenbezug.BEIDSEITIG));
		}

		@Test
		void withKanteErsetzt_punktuellerNetzbezug_stationierungsumkehrKorrektUebertragen() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
				.id(1l)
				.build();
			Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 50, 0, QuellSystem.DLM).id(2l)
				.build();
			Kante ersatzKante2 = KanteTestDataProvider.withCoordinatesAndQuelle(100, 0, 50, 0, QuellSystem.DLM).id(3l)
				.build();

			MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
				Collections.emptySet(),
				Set.of(new PunktuellerKantenSeitenBezug(zuErsetzendeKante, LineareReferenz.of(0.25), Seitenbezug.LINKS),
					new PunktuellerKantenSeitenBezug(zuErsetzendeKante, LineareReferenz.of(0.8), Seitenbezug.RECHTS)),
				Collections.emptySet());

			// act
			MassnahmeNetzBezug result = massnahmeNetzBezug.withKanteErsetzt(zuErsetzendeKante,
				Set.of(ersatzKante1, ersatzKante2), 1.0);

			// assert
			assertThat(result.getImmutableKantenPunktBezug()).hasSize(2);
			assertThat(result.getImmutableKantenPunktBezug())
				.contains(new PunktuellerKantenSeitenBezug(ersatzKante1, LineareReferenz.of(0.5), Seitenbezug.LINKS),
					new PunktuellerKantenSeitenBezug(ersatzKante2, LineareReferenz.of(0.4), Seitenbezug.LINKS));
		}

		@Test
		void withKanteErsetzt_abschnitt_splits() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
				.id(1l)
				.build();
			Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 50, 0, QuellSystem.DLM).id(2l)
				.build();
			Kante ersatzKante2 = KanteTestDataProvider.withCoordinatesAndQuelle(50, 0, 100, 0, QuellSystem.DLM).id(3l)
				.build();

			MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante,
					LinearReferenzierterAbschnitt.of(0.3, 0.7), Seitenbezug.BEIDSEITIG)),
				Collections.emptySet(),
				Collections.emptySet());

			// act
			MassnahmeNetzBezug result = massnahmeNetzBezug.withKanteErsetzt(zuErsetzendeKante,
				Set.of(ersatzKante1, ersatzKante2), 1.0);

			// assert
			assertThat(result.getImmutableKantenAbschnittBezug()).hasSize(2);
			assertThat(result.getImmutableKantenAbschnittBezug())
				.contains(
					new AbschnittsweiserKantenSeitenBezug(ersatzKante1, LinearReferenzierterAbschnitt.of(0.6, 1),
						Seitenbezug.BEIDSEITIG),
					new AbschnittsweiserKantenSeitenBezug(ersatzKante2, LinearReferenzierterAbschnitt.of(0, 0.4),
						Seitenbezug.BEIDSEITIG));
		}

		@Test
		void withKanteErsetzt_abschnitt_stationierungsrichtungKorrektUebertragen() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
				.id(1l)
				.build();
			Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 50, 0, QuellSystem.DLM).id(2l)
				.build();
			Kante ersatzKante2 = KanteTestDataProvider.withCoordinatesAndQuelle(100, 0, 50, 0, QuellSystem.DLM).id(3l)
				.build();

			MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
				Set.of(new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante,
					LinearReferenzierterAbschnitt.of(0.4, 0.7), Seitenbezug.LINKS)),
				Collections.emptySet(),
				Collections.emptySet());

			// act
			MassnahmeNetzBezug result = massnahmeNetzBezug.withKanteErsetzt(zuErsetzendeKante,
				Set.of(ersatzKante1, ersatzKante2), 1.0);

			// assert
			assertThat(result.getImmutableKantenAbschnittBezug()).hasSize(2);
			assertThat(result.getImmutableKantenAbschnittBezug())
				.contains(
					new AbschnittsweiserKantenSeitenBezug(ersatzKante1, LinearReferenzierterAbschnitt.of(0.8, 1),
						Seitenbezug.LINKS),
					new AbschnittsweiserKantenSeitenBezug(ersatzKante2, LinearReferenzierterAbschnitt.of(0.6, 1),
						Seitenbezug.RECHTS));
		}

		@Test
		void withKanteErsetzt_abschnitt_merge() {
			// arrange
			Kante ersatzKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
				.id(1l)
				.build();
			Kante zuErsetzendeKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 50, 0, QuellSystem.DLM)
				.id(2l)
				.build();
			Kante zuErsetzendeKante2 = KanteTestDataProvider.withCoordinatesAndQuelle(50, 0, 100, 0, QuellSystem.DLM)
				.id(3l)
				.build();

			MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
				Set.of(
					new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante1, LinearReferenzierterAbschnitt.of(0.6, 1),
						Seitenbezug.BEIDSEITIG),
					new AbschnittsweiserKantenSeitenBezug(zuErsetzendeKante2, LinearReferenzierterAbschnitt.of(0, 0.4),
						Seitenbezug.BEIDSEITIG)),
				Collections.emptySet(),
				Collections.emptySet());

			// act
			MassnahmeNetzBezug result = massnahmeNetzBezug.withKanteErsetzt(zuErsetzendeKante1, Set.of(ersatzKante),
				1.0);
			result = result.withKanteErsetzt(zuErsetzendeKante2, Set.of(ersatzKante), 1.0);

			// assert
			assertThat(result.getImmutableKantenAbschnittBezug()).hasSize(1);
			assertThat(result.getImmutableKantenAbschnittBezug())
				.contains(
					new AbschnittsweiserKantenSeitenBezug(ersatzKante, LinearReferenzierterAbschnitt.of(0.3, 0.7),
						Seitenbezug.BEIDSEITIG));
		}

		@Test
		void withKanteErsetzt_abschnitt_nichtVollstaendigUebertragen_doesNothing() {
			// arrange
			Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
				.id(1l)
				.build();
			Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 50, 0, QuellSystem.DLM).id(2l)
				.build();

			AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug = new AbschnittsweiserKantenSeitenBezug(
				zuErsetzendeKante,
				LinearReferenzierterAbschnitt.of(0.3, 0.7), Seitenbezug.BEIDSEITIG);
			MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
				Set.of(abschnittsweiserKantenSeitenBezug),
				Collections.emptySet(),
				Collections.emptySet());

			// act
			MassnahmeNetzBezug result = massnahmeNetzBezug.withKanteErsetzt(zuErsetzendeKante, Set.of(ersatzKante1),
				1.0);

			// assert
			assertThat(result.getImmutableKantenAbschnittBezug())
				.containsExactly(abschnittsweiserKantenSeitenBezug);
		}

		@Test
		void withKanteErsetzt_abschnitt_onDifferentKante_doesNothing() {
			// arrange
			Kante netzbezugKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
				.id(1l)
				.build();
			Kante zuErsetzendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
				.id(2l)
				.build();
			Kante ersatzKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 50, 0, QuellSystem.DLM).id(3l)
				.build();

			AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug = new AbschnittsweiserKantenSeitenBezug(
				netzbezugKante, LinearReferenzierterAbschnitt.of(0, 1),
				Seitenbezug.BEIDSEITIG);
			MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
				Set.of(abschnittsweiserKantenSeitenBezug),
				Set.of(new PunktuellerKantenSeitenBezug(zuErsetzendeKante, LineareReferenz.of(0.25),
					Seitenbezug.BEIDSEITIG)),
				Collections.emptySet());

			// act
			MassnahmeNetzBezug result = massnahmeNetzBezug.withKanteErsetzt(zuErsetzendeKante, Set.of(ersatzKante1),
				1.0);

			// assert
			assertThat(result.getImmutableKantenAbschnittBezug())
				.containsExactly(abschnittsweiserKantenSeitenBezug);
		}
	}

	@Test
	void withoutKanten() {
		// arrange
		Kante zuEntfernendeKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
			.id(1l)
			.build();
		Kante zuEntfernendeKante2 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 50, 100, 50, QuellSystem.DLM)
			.id(2l)
			.build();
		Kante bleibendeKante = KanteTestDataProvider.withCoordinatesAndQuelle(0, 100, 100, 100, QuellSystem.DLM)
			.id(3l)
			.build();

		AbschnittsweiserKantenSeitenBezug abschnittAufBleibenderKante = new AbschnittsweiserKantenSeitenBezug(
			bleibendeKante, LinearReferenzierterAbschnitt.of(0.3, 0.7),
			Seitenbezug.BEIDSEITIG);
		PunktuellerKantenSeitenBezug punktAufBleibenderKante = new PunktuellerKantenSeitenBezug(bleibendeKante,
			LineareReferenz.of(0.25),
			Seitenbezug.BEIDSEITIG);
		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
			Set.of(
				new AbschnittsweiserKantenSeitenBezug(zuEntfernendeKante1, LinearReferenzierterAbschnitt.of(0.3, 0.7),
					Seitenbezug.BEIDSEITIG),
				new AbschnittsweiserKantenSeitenBezug(zuEntfernendeKante2, LinearReferenzierterAbschnitt.of(0.3, 0.7),
					Seitenbezug.BEIDSEITIG),
				abschnittAufBleibenderKante),
			Set.of(new PunktuellerKantenSeitenBezug(zuEntfernendeKante1, LineareReferenz.of(0.25),
				Seitenbezug.BEIDSEITIG),
				new PunktuellerKantenSeitenBezug(zuEntfernendeKante2, LineareReferenz.of(0.25),
					Seitenbezug.BEIDSEITIG),
				punktAufBleibenderKante),
			Collections.emptySet());

		// act
		MassnahmeNetzBezug result = massnahmeNetzBezug
			.withoutKanten(Set.of(zuEntfernendeKante1.getId(), zuEntfernendeKante2.getId()));

		// assert
		assertThat(result.getImmutableKantenPunktBezug()).containsExactly(punktAufBleibenderKante);
		assertThat(result.getImmutableKantenAbschnittBezug()).containsExactly(abschnittAufBleibenderKante);
	}

	@Test
	void withoutKnoten() {
		// arrange
		Knoten zuEntfernenderKnoten1 = KnotenTestDataProvider.withDefaultValues().id(1l).build();
		Knoten zuEntfernenderKnoten2 = KnotenTestDataProvider.withDefaultValues().id(2l).build();
		Knoten bleibenderKnoten = KnotenTestDataProvider.withDefaultValues().id(3l).build();

		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(zuEntfernenderKnoten1, zuEntfernenderKnoten2, bleibenderKnoten));

		// act
		MassnahmeNetzBezug result = massnahmeNetzBezug
			.withoutKnoten(Set.of(zuEntfernenderKnoten1.getId(), zuEntfernenderKnoten2.getId()));

		// assert
		assertThat(result.getImmutableKnotenBezug()).containsExactly(bleibenderKnoten);
	}

	@Test
	void withoutKanten_entferntLetzteKante() {
		// arrange
		Kante zuEntfernendeKante1 = KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM)
			.id(1l)
			.build();

		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
			Set.of(
				new AbschnittsweiserKantenSeitenBezug(zuEntfernendeKante1, LinearReferenzierterAbschnitt.of(0.3, 0.7),
					Seitenbezug.BEIDSEITIG)),
			Collections.emptySet(),
			Collections.emptySet());

		// act
		MassnahmeNetzBezug result = massnahmeNetzBezug.withoutKanten(Set.of(zuEntfernendeKante1.getId()));

		// assert
		assertThat(result.getImmutableKantenAbschnittBezug()).isEmpty();
	}

	@Test
	void withoutKnoten_letzterKnotenEntfernt_doesNotThrow() {
		// arrange
		Knoten zuEntfernenderKnoten1 = KnotenTestDataProvider.withDefaultValues().id(1l).build();

		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(zuEntfernenderKnoten1));

		// act + assert
		assertDoesNotThrow(() -> massnahmeNetzBezug.withoutKnoten(Set.of(zuEntfernenderKnoten1.getId())));
	}

	@Test
	void withKnotenErsetzt() {
		// arrange
		Knoten zuErsetzenderKnoten = KnotenTestDataProvider.withDefaultValues().id(1l).build();
		Knoten knoten2 = KnotenTestDataProvider.withDefaultValues().id(2l).build();
		Knoten ersatzKnoten = KnotenTestDataProvider.withDefaultValues().id(3l).build();

		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(zuErsetzenderKnoten, knoten2));

		// act
		Map<Long, Knoten> ersatzKnotenZuordnung = new HashMap<>();
		ersatzKnotenZuordnung.put(zuErsetzenderKnoten.getId(), ersatzKnoten);
		MassnahmeNetzBezug withKnotenErsetzt = massnahmeNetzBezug.withKnotenErsetzt(ersatzKnotenZuordnung);

		// assert
		assertThat(withKnotenErsetzt.getImmutableKnotenBezug()).contains(knoten2, ersatzKnoten);
		assertThat(withKnotenErsetzt.getImmutableKnotenBezug()).doesNotContain(zuErsetzenderKnoten);
	}

	@Test
	void withKnotenErsetzt_knotenNotInNetzbezug_doesNothing() {
		// arrange
		Knoten zuErsetzenderKnoten = KnotenTestDataProvider.withDefaultValues().id(1l).build();
		Knoten knoten2 = KnotenTestDataProvider.withDefaultValues().id(2l).build();
		Knoten ersatzKnoten = KnotenTestDataProvider.withDefaultValues().id(3l).build();

		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(knoten2));

		// act
		Map<Long, Knoten> ersatzKnotenZuordnung = new HashMap<>();
		ersatzKnotenZuordnung.put(zuErsetzenderKnoten.getId(), ersatzKnoten);
		MassnahmeNetzBezug withKnotenErsetzt = massnahmeNetzBezug.withKnotenErsetzt(ersatzKnotenZuordnung);

		// assert
		assertThat(withKnotenErsetzt.getImmutableKnotenBezug()).containsExactly(knoten2);
	}

	@Test
	void withKnotenErsetzt_ersatzKnotenAlreadyInNetzbezug_doesNothing() {
		// arrange
		Knoten zuErsetzenderKnoten = KnotenTestDataProvider.withDefaultValues().id(1l).build();
		Knoten ersatzKnoten = KnotenTestDataProvider.withDefaultValues().id(3l).build();

		MassnahmeNetzBezug massnahmeNetzBezug = new MassnahmeNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(ersatzKnoten, zuErsetzenderKnoten));

		// act
		Map<Long, Knoten> ersatzKnotenZuordnung = new HashMap<>();
		ersatzKnotenZuordnung.put(zuErsetzenderKnoten.getId(), ersatzKnoten);
		MassnahmeNetzBezug withKnotenErsetzt = massnahmeNetzBezug.withKnotenErsetzt(ersatzKnotenZuordnung);

		// assert
		assertThat(withKnotenErsetzt.getImmutableKnotenBezug()).contains(zuErsetzenderKnoten);
	}
}
