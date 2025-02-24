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

package de.wps.radvis.backend.netz.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenOrtslage;
import de.wps.radvis.backend.netz.schnittstelle.view.KnotenEditView;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class KnotenEditViewTest {

	@Test
	public void test_landkreisIstAbgeleitetAusGemeinde() {
		// arrange
		Verwaltungseinheit landkreis = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L).build();

		// act
		KnotenEditView knotenEditView = new KnotenEditView(
			Knoten.builder()
				.knotenAttribute(
					KnotenAttribute
						.builder()
						.gemeinde(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L)
							.uebergeordneteOrganisation(landkreis)
							.build())
						.build())
				.build(),
			KnotenOrtslage.AUSSERORTS,
			false);

		// assert
		assertThat(knotenEditView.getLandkreis()).isPresent();
		assertThat(knotenEditView.getLandkreis().get().getId()).isEqualTo(1L);
	}
}
