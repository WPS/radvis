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

package de.wps.radvis.backend.netz.domain.valueObject;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

class AequivalenzklasseTest {

	@Test
	void testEqualsAndHashcode() {
		Coordinate coordinate1 = new Coordinate(10, 10);
		Coordinate coordinate2 = new Coordinate(50, 50);

		Aequivalenzklasse aequivalenzklasse = Aequivalenzklasse.of(coordinate1, coordinate2);
		Aequivalenzklasse reversed = Aequivalenzklasse.of(coordinate2, coordinate1);
		Aequivalenzklasse different = Aequivalenzklasse.of(coordinate1, new Coordinate(100, 100));

		assertThat(aequivalenzklasse).isEqualTo(reversed);
		assertThat(aequivalenzklasse.hashCode()).isEqualTo(reversed.hashCode());
		assertThat(aequivalenzklasse).isNotEqualTo(different);
		assertThat(reversed).isNotEqualTo(different);
	}
}