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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.matching.domain.entity.MappedGrundnetzkante;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;

class MappedGrundnetzkanteTest {

	@Test
	public void testeMappedGrundnetzkante_grundnetzkanteIstInUeberschneidungEnthalten() {
		// arrange
		Kante grundnetzKante = KanteTestDataProvider.withDefaultValues()
			.id(10L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(100, 100),
				new Coordinate(110, 110)))
			.build();
		LineString ueberschneidungInDLM = GeometryTestdataProvider.createLineString(new Coordinate(90, 90),
			new Coordinate(120, 120), new Coordinate(140, 120));

		// act
		MappedGrundnetzkante result = new MappedGrundnetzkante(grundnetzKante.getGeometry(), grundnetzKante.getId(),
			ueberschneidungInDLM);

		// assert
		assertThat(result.getKanteId()).isEqualTo(10L);
		assertThat(result.getLinearReferenzierterAbschnitt()).isEqualTo(LinearReferenzierterAbschnitt.of(0, 1));
	}

	@Test
	public void testeMappedGrundnetzkante_teilweiseUeberschneidungAbPunkt() {
		// arrange
		Kante grundnetzKante = KanteTestDataProvider.withDefaultValues()
			.id(10L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(90, 90),
				new Coordinate(110, 110)))
			.build();
		LineString ueberschneidungInDLM = GeometryTestdataProvider.createLineString(new Coordinate(95, 95),
			new Coordinate(120, 120), new Coordinate(140, 120));

		// act
		MappedGrundnetzkante result = new MappedGrundnetzkante(grundnetzKante.getGeometry(), grundnetzKante.getId(),
			ueberschneidungInDLM);

		// assert
		assertThat(result.getKanteId()).isEqualTo(10L);
		assertThat(result.getLinearReferenzierterAbschnitt()).isEqualTo(LinearReferenzierterAbschnitt.of(0.25, 1));
	}

	@Test
	public void testeMappedGrundnetzkante_teilweiseUeberschneidungBisPunkt() {
		// arrange
		Kante grundnetzKante = KanteTestDataProvider.withDefaultValues()
			.id(10L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(90, 90),
				new Coordinate(115, 115)))
			.build();
		LineString ueberschneidungInDLM = GeometryTestdataProvider.createLineString(new Coordinate(85, 85),
			new Coordinate(95, 95));

		// act
		MappedGrundnetzkante result = new MappedGrundnetzkante(grundnetzKante.getGeometry(), grundnetzKante.getId(),
			ueberschneidungInDLM);

		// assert
		assertThat(result.getKanteId()).isEqualTo(10L);
		assertThat(result.getLinearReferenzierterAbschnitt()).isEqualTo(LinearReferenzierterAbschnitt.of(0, 0.2));
	}

	@Test
	public void testeMappedGrundnetzkante_ueberschneidungIstInKanteEnthalten() {
		// arrange
		Kante grundnetzKante = KanteTestDataProvider.withDefaultValues()
			.id(10L)
			.geometry(GeometryTestdataProvider.createLineString(new Coordinate(90, 90),
				new Coordinate(115, 115)))
			.build();
		LineString ueberschneidungInDLM = GeometryTestdataProvider.createLineString(new Coordinate(95, 95),
			new Coordinate(105, 105));

		// act
		MappedGrundnetzkante result = new MappedGrundnetzkante(grundnetzKante.getGeometry(), grundnetzKante.getId(),
			ueberschneidungInDLM);

		// assert
		assertThat(result.getKanteId()).isEqualTo(10L);
		assertThat(result.getLinearReferenzierterAbschnitt()).isEqualTo(LinearReferenzierterAbschnitt.of(0.2, 0.6));
	}
}
