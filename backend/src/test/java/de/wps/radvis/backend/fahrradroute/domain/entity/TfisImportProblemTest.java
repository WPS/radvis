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

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.GeometryTestdataProvider;

class TfisImportProblemTest {

	@Test
	void addSackgassen() {
		// arrange
		TfisImportProblem tfisImportProblem = new TfisImportProblem(1l, "Test-Route",
			GeometryTestdataProvider.createLineString(), LocalDateTime.now(),
			"Fehler beim Import", true);

		// act
		tfisImportProblem.addSackgassen(List.of(GeometryTestdataProvider.createPoint(new Coordinate(0, 0))));

		// assert
		assertThat(tfisImportProblem.getBeschreibung()).contains(TfisImportProblem.LUECKEN_HINWEISTEXT);
	}

}
