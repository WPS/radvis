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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;

class PunktuellerKantenSeitenBezugTest {

	@Test
	void testGetPointGeometry() {
		Kante kante = KanteTestDataProvider.withDefaultValues()
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(10, 10),
				new Coordinate(10, 15),
				new Coordinate(15, 15)))
			.build();
		PunktuellerKantenSeitenBezug punktuellerKantenSeitenBezug = new PunktuellerKantenSeitenBezug(
			kante, LineareReferenz.of(0.6), Seitenbezug.BEIDSEITIG);

		assertThat(punktuellerKantenSeitenBezug.getPointGeometry().getCoordinate()).isEqualTo(new Coordinate(11, 15));
	}
}
