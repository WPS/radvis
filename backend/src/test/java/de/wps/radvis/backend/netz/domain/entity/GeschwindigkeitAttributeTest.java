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

package de.wps.radvis.backend.netz.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;

class GeschwindigkeitAttributeTest {

	@Test
	public void testeReversed() {
		GeschwindigkeitAttribute g1 =
			GeschwindigkeitAttribute.builder()
				.ortslage(KantenOrtslage.INNERORTS)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH)
				.build();
		GeschwindigkeitAttribute g2 =
			GeschwindigkeitAttribute.builder()
				.ortslage(KantenOrtslage.INNERORTS)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_50_KMH)
				.build();
		GeschwindigkeitAttribute g3 =
			GeschwindigkeitAttribute.builder()
				.ortslage(KantenOrtslage.INNERORTS)
				.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_20_KMH)
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(Hoechstgeschwindigkeit.MAX_20_KMH)
				.build();

		assertThat(g1.vertauscht()).isEqualTo(g1);
		assertThat(g2.vertauscht().vertauscht()).isEqualTo(g2);
		assertThat(g2.vertauscht().getHoechstgeschwindigkeit()).isEqualTo(Hoechstgeschwindigkeit.MAX_50_KMH);
		assertThat(g2.vertauscht().getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung())
			.contains(Hoechstgeschwindigkeit.MAX_20_KMH);
		assertThat(g3.vertauscht()).isEqualTo(g3);
	}

}