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

package de.wps.radvis.backend.netz.domain.entity.provider;

import java.util.List;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;

public class GeschwindigkeitsAttributeTestDataProvider {
	public static GeschwindigkeitAttribute.GeschwindigkeitAttributeBuilder withGrundnetzDefaultwerte() {
		return GeschwindigkeitAttribute.builder()
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.UNBEKANNT);
	}

	public static GeschwindigkeitAttribute.GeschwindigkeitAttributeBuilder withLineareReferenz(double von, double bis) {
		return GeschwindigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(von, bis))
			.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.UNBEKANNT);
	}

	public static GeschwindigkeitAttributGruppe.GeschwindigkeitAttributGruppeBuilder gruppeWithGrundnetzDefaultwerte() {
		return GeschwindigkeitAttributGruppe.builder().geschwindigkeitAttribute(
			List.of(GeschwindigkeitsAttributeTestDataProvider.withGrundnetzDefaultwerte().build())
		);
	}
}
