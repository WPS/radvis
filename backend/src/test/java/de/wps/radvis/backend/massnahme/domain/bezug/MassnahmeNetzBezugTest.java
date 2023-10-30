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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
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
}
