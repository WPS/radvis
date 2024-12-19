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

package de.wps.radvis.backend.fahrradroute.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.fahrradroute.domain.entity.provider.FahrradrouteVarianteTestDataProvider;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;

class FahrradrouteVarianteTest {
	@Test
	void entferneKante() {
		// arrange
		AbschnittsweiserKantenBezug bezug1 = new AbschnittsweiserKantenBezug(
			KanteTestDataProvider.withDefaultValues().id(1L).build(), LinearReferenzierterAbschnitt.of(0, 1));
		AbschnittsweiserKantenBezug bezug2 = new AbschnittsweiserKantenBezug(
			KanteTestDataProvider.withDefaultValues().id(2L).build(), LinearReferenzierterAbschnitt.of(0, 1));
		AbschnittsweiserKantenBezug bezug3 = new AbschnittsweiserKantenBezug(
			KanteTestDataProvider.withDefaultValues().id(3L).build(), LinearReferenzierterAbschnitt.of(0, 1));
		List<AbschnittsweiserKantenBezug> alleBezuege = new ArrayList<>();
		alleBezuege.addAll(List.of(bezug1, bezug2, bezug3));

		FahrradrouteVariante variante = FahrradrouteVarianteTestDataProvider.defaultTfis()
			.abschnittsweiserKantenBezug(alleBezuege).build();

		// act
		variante.removeKantenFromNetzbezug(List.of(2L));

		// assert
		assertThat(variante.getAbschnittsweiserKantenBezug()).containsExactly(bezug1, bezug3);
	}
}
