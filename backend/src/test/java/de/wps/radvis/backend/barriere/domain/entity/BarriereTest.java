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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.barriere.domain.entity.provider.BarriereTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;

class BarriereTest {

	@Test
	void ersetzeKnotenInNetzbezug() {
		// arrange
		Knoten zuErsetzenderKnoten = KnotenTestDataProvider.withDefaultValues().id(1l).build();
		Knoten knoten2 = KnotenTestDataProvider.withDefaultValues().id(2l).build();
		Knoten ersatzKnoten = KnotenTestDataProvider.withDefaultValues().id(3l).build();

		Barriere barriere = BarriereTestDataProvider.withDefaultValues().netzbezug(new BarriereNetzBezug(
			Collections.emptySet(),
			Collections.emptySet(),
			Set.of(zuErsetzenderKnoten, knoten2))).build();

		// act
		Map<Long, Knoten> ersatzKnotenZuordnung = new HashMap<>();
		ersatzKnotenZuordnung.put(zuErsetzenderKnoten.getId(), ersatzKnoten);
		barriere.ersetzeKnotenInNetzbezug(ersatzKnotenZuordnung);

		// assert
		assertThat(barriere.getNetzbezug().getImmutableKnotenBezug()).contains(knoten2, ersatzKnoten);
		assertThat(barriere.getNetzbezug().getImmutableKnotenBezug()).doesNotContain(zuErsetzenderKnoten);
	}
}
