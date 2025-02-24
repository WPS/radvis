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

import java.util.ArrayList;
import java.util.List;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;

public class FuehrungsformAttributGruppeTestDataProvider {
	public static FuehrungsformAttributGruppe.FuehrungsformAttributGruppeBuilder withGrundnetzDefaultwerte() {
		return FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(new ArrayList<>(
				(List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte().build()))))
			.fuehrungsformAttributeRechts(new ArrayList<>(
				(List.of(FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte().build()))));
	}

	public static FuehrungsformAttributGruppe.FuehrungsformAttributGruppeBuilder withAttribute(
		FuehrungsformAttribute.FuehrungsformAttributeBuilder attribute) {
		return FuehrungsformAttributGruppe.builder()
			.fuehrungsformAttributeLinks(new ArrayList<>(
				(List.of(attribute.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1)).build()))))
			.fuehrungsformAttributeRechts(new ArrayList<>(
				(List.of(attribute.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1)).build()))));
	}

	public static FuehrungsformAttribute.FuehrungsformAttributeBuilder withLineareReferenz(double von, double bis) {
		return FuehrungsformAttributeTestDataProvider.withLineareReferenz(von, bis);
	}
}
