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

package de.wps.radvis.backend.barriere.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.linearref.LengthIndexedLine;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;

class BarriereNetzBezugTest {

	private final Set<AbschnittsweiserKantenSeitenBezug> testAbschnittsweiserKantenSeitenBezug = new HashSet<>();
	private final Set<PunktuellerKantenSeitenBezug> testPunktuellerKantenSeitenBezug = new HashSet<>();
	private final Set<Knoten> testKnotenBezug = new HashSet<>();

	private Kante testKante;
	private Knoten testKnoten;

	@BeforeEach
	void setUp() {
		testKante = KanteTestDataProvider.withDefaultValues().id(1L).build();
		testKnoten = KnotenTestDataProvider.withDefaultValues().id(10L).build();
		testAbschnittsweiserKantenSeitenBezug.add(new AbschnittsweiserKantenSeitenBezug(
			testKante,
			LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.LINKS));
		testPunktuellerKantenSeitenBezug.add(new PunktuellerKantenSeitenBezug(
			testKante, LineareReferenz.of(0.5), Seitenbezug.BEIDSEITIG
		));
		testKnotenBezug.add(testKnoten);
	}

	@Test
	void getDisplayGeometry_allesVorhanden_ersterKnotenVerwendet() {
		// arrange
		BarriereNetzBezug barriereNetzBezug = new BarriereNetzBezug(testAbschnittsweiserKantenSeitenBezug,
			testPunktuellerKantenSeitenBezug, testKnotenBezug);

		// act
		Optional<Point> displayGeometry = barriereNetzBezug.getDisplayGeometry();

		// assert
		assertThat(displayGeometry).isPresent();
		assertThat(displayGeometry.get().getCoordinate()).isEqualTo(testKnoten.getKoordinate());
	}

	@Test
	void getDisplayGeometry_allesBisAufKnotenVorhanden_ersterPunktuellerNetzbezugVerwendet() {
		// arrange
		BarriereNetzBezug barriereNetzBezug = new BarriereNetzBezug(testAbschnittsweiserKantenSeitenBezug,
			testPunktuellerKantenSeitenBezug, Set.of());

		// act
		Optional<Point> displayGeometry = barriereNetzBezug.getDisplayGeometry();

		// assert
		assertThat(displayGeometry).isPresent();
		assertThat(displayGeometry.get().getCoordinate()).isEqualTo(
			new LengthIndexedLine(testKante.getGeometry())
				.extractPoint(0.5 * testKante.getGeometry().getLength())
		);
	}

	@Test
	void getDisplayGeometry_nurKantenBezug_startpunktErsteKante() {
		// arrange
		BarriereNetzBezug barriereNetzBezug = new BarriereNetzBezug(testAbschnittsweiserKantenSeitenBezug,
			Set.of(), Set.of());

		// act
		Optional<Point> displayGeometry = barriereNetzBezug.getDisplayGeometry();

		// assert
		assertThat(displayGeometry).isPresent();
		assertThat(displayGeometry.get().getCoordinate()).isEqualTo(testKante.getVonKnoten().getKoordinate());
	}
}