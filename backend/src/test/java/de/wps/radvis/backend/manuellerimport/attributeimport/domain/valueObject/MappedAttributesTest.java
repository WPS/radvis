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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Konflikt;
import de.wps.radvis.backend.netz.domain.entity.Kante;

class MappedAttributesTest {

	private KantenKonfliktProtokoll kantenKonfliktProtokoll;

	@BeforeEach
	void setUp() {
		Kante kante = mock(Kante.class);
		when(kante.getGeometry()).thenReturn(
			GeometryTestdataProvider.createLineString(new Coordinate(100, 100), new Coordinate(200, 200)));
		when(kante.getId()).thenReturn(1L);

		kantenKonfliktProtokoll = new KantenKonfliktProtokoll(kante.getId(), kante.getGeometry());
	}

	@Test
	void testeGetAttributwertMitGroesstemAnteil() {

		List<MappedAttributes> mappedAttributes = List.of(
			MappedAttributes.of(Map.of("怪物", "Monster without a Name", "Fritzgerald", "The Great Gatsby"),
				LinearReferenzierterAbschnitt.of(0.2, 0.8),
				Seitenbezug.BEIDSEITIG, false),
			MappedAttributes.of(Map.of("怪物", "Monster with a Name", "Fritzgerald", "Fuck Tom Buchanan"),
				LinearReferenzierterAbschnitt.of(0., 0.4),
				Seitenbezug.BEIDSEITIG, false),
			MappedAttributes.of(Map.of("怪物", "Monster with a Name", "Fritzgerald", "The Great Gatsby"),
				LinearReferenzierterAbschnitt.of(0.4, 1.),
				Seitenbezug.BEIDSEITIG, false)
		);

		String groesterAnteil = MappedAttributes.getAttributwertMitGroesstemAnteil(mappedAttributes, "怪物",
			kantenKonfliktProtokoll);

		assertThat(groesterAnteil).isEqualTo("Monster with a Name");
		assertThat(kantenKonfliktProtokoll.getKonflikte())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactlyInAnyOrder(
				new Konflikt("怪物", "Monster with a Name", Set.of("Monster without a Name")));
	}

	@Test
	void testeGetAttributwertMitGroesstemAnteil_addiertKeineUeberlappungenAuf() {

		List<MappedAttributes> mappedAttributes = List.of(
			MappedAttributes.of(Map.of("怪物", "Monster without a Name", "Fritzgerald", "The Great Gatsby"),
				LinearReferenzierterAbschnitt.of(0.2, 1.),
				Seitenbezug.BEIDSEITIG, false),
			MappedAttributes.of(Map.of("怪物", "Monster with a Name", "Fritzgerald", "Fuck Tom Buchanan"),
				LinearReferenzierterAbschnitt.of(0., .9),
				Seitenbezug.BEIDSEITIG, false),
			MappedAttributes.of(Map.of("怪物", "Monster with a Name", "Fritzgerald", "The Great Gatsby"),
				LinearReferenzierterAbschnitt.of(0.2, 0.5),
				Seitenbezug.BEIDSEITIG, false)
		);

		String groesterAnteil = MappedAttributes.getAttributwertMitGroesstemAnteil(mappedAttributes, "Fritzgerald",
			kantenKonfliktProtokoll);

		assertThat(groesterAnteil).isEqualTo("Fuck Tom Buchanan");
		assertThat(kantenKonfliktProtokoll.getKonflikte())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactlyInAnyOrder(
				new Konflikt("Fritzgerald", "Fuck Tom Buchanan", Set.of("The Great Gatsby")));
	}

	@Test
	void testeGetAttributwertMitGroesstemAnteil_requireviolationBeiLeererListe() {
		assertThatThrownBy(() -> MappedAttributes.getAttributwertMitGroesstemAnteil(List.of(), "怪物",
			kantenKonfliktProtokoll)).isInstanceOf(RequireViolation.class);
	}

	@Test
	void testeLoeseUeberschneidungenAuf() {

		List<MappedAttributes> mappedAttributes = List.of(
			MappedAttributes.of(Map.of("怪物", "Monster with a Name", "Fritzgerald", "Fuck Tom Buchanan"),
				LinearReferenzierterAbschnitt.of(0., 0.2),
				Seitenbezug.BEIDSEITIG, false),
			MappedAttributes.of(Map.of("怪物", "Monster without a Name", "Fritzgerald", "The Great Gatsby"),
				LinearReferenzierterAbschnitt.of(0.2, 0.8),
				Seitenbezug.BEIDSEITIG, false),
			MappedAttributes.of(Map.of("怪物", "Monster with a Name", "Fritzgerald", "The Great Gatsby"),
				LinearReferenzierterAbschnitt.of(0.2, 0.8),
				Seitenbezug.BEIDSEITIG, false),
			MappedAttributes.of(Map.of("怪物", "Crabigator", "Fritzgerald", "The Great Gatsby"),
				LinearReferenzierterAbschnitt.of(0.8, 1.),
				Seitenbezug.BEIDSEITIG, false),
			MappedAttributes.of(Map.of("怪物", "Allicrab", "Fritzgerald", "The Great Gatsby"),
				LinearReferenzierterAbschnitt.of(0.8, 1.),
				Seitenbezug.BEIDSEITIG, false),
			MappedAttributes.of(Map.of("怪物", "Koichi", "Fritzgerald", "The Great Gatsby"),
				LinearReferenzierterAbschnitt.of(0.8, 1.),
				Seitenbezug.BEIDSEITIG, false)
		);

		List<MappedAttributes> ueberschneidungsfreieListe = MappedAttributes.loeseUeberschneidungenAuf(mappedAttributes,
			"怪物",
			kantenKonfliktProtokoll);

		assertThat(LinearReferenzierterAbschnitt.segmentsCoverFullLine(ueberschneidungsfreieListe.stream()
			.map(MappedAttributes::getLinearReferenzierterAbschnitt).collect(Collectors.toList()))).isTrue();
		assertThat(ueberschneidungsfreieListe).usingRecursiveFieldByFieldElementComparator()
			.containsExactlyInAnyOrder(
				MappedAttributes.of(Map.of("怪物", "Monster with a Name", "Fritzgerald", "Fuck Tom Buchanan"),
					LinearReferenzierterAbschnitt.of(0., 0.2),
					Seitenbezug.BEIDSEITIG, false),
				MappedAttributes.of(Map.of("怪物", "Monster with a Name", "Fritzgerald", "The Great Gatsby"),
					LinearReferenzierterAbschnitt.of(0.2, 0.8),
					Seitenbezug.BEIDSEITIG, false),
				MappedAttributes.of(Map.of("怪物", "Koichi", "Fritzgerald", "The Great Gatsby"),
					LinearReferenzierterAbschnitt.of(0.8, 1.),
					Seitenbezug.BEIDSEITIG, false)
			);
		assertThat(kantenKonfliktProtokoll.getKonflikte())
			.usingRecursiveFieldByFieldElementComparator()
			.containsExactlyInAnyOrder(
				new Konflikt(LinearReferenzierterAbschnitt.of(0.2, 0.8), "怪物", "Monster with a Name",
					Set.of("Monster without a Name")),
				new Konflikt(LinearReferenzierterAbschnitt.of(0.8, 1.), "怪物", "Koichi",
					Set.of("Allicrab", "Crabigator"))
			);
	}

	@Test
	void testeLoeseUeberschneidungenAuf_keinKonfliktBeiGleichemAttributwert() {

		List<MappedAttributes> mappedAttributes = List.of(
			MappedAttributes.of(Map.of("怪物", "Crabigator", "Fritzgerald", "The Great Gatsby"),
				LinearReferenzierterAbschnitt.of(0.8, 1.),
				Seitenbezug.BEIDSEITIG, false),
			MappedAttributes.of(Map.of("怪物", "Crabigator", "Fritzgerald", "The Great Gatsby"),
				LinearReferenzierterAbschnitt.of(0.8, 1.),
				Seitenbezug.BEIDSEITIG, false),
			MappedAttributes.of(Map.of("怪物", "Crabigator", "Fritzgerald", "The Great Gatsby"),
				LinearReferenzierterAbschnitt.of(0.8, 1.),
				Seitenbezug.BEIDSEITIG, false)
		);

		List<MappedAttributes> ueberschneidungsfreieListe = MappedAttributes.loeseUeberschneidungenAuf(mappedAttributes,
			"怪物",
			kantenKonfliktProtokoll);

		assertThat(ueberschneidungsfreieListe).usingRecursiveFieldByFieldElementComparator()
			.containsExactlyInAnyOrder(
				MappedAttributes.of(Map.of("怪物", "Crabigator", "Fritzgerald", "The Great Gatsby"),
					LinearReferenzierterAbschnitt.of(0.8, 1.),
					Seitenbezug.BEIDSEITIG, false)
			);
		assertThat(kantenKonfliktProtokoll.getKonflikte()).isEmpty();
	}
}
