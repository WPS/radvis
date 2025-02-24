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

import java.util.Set;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Schadenart {
	ABPLATZUNGEN_SCHLAGLOECHER("Abplatzungen / Schlaglöcher"),
	ABSACKUNG_SETZUNG("Absackung / Setzung"),
	AUSSPUELUNGEN_RINNEN("Ausspülungen / Rinnen"),
	GRASEINWUCHS("Graseinwuchs"),
	KANTEN("Kanten"),
	NETZRISSE("Netzrisse"),
	PFLASTER_KLAPPERT("Pflaster klappert"),
	PFLASTERBRUECHE("Pflasterbrüche"),
	PFLASTERSTEINE_FEHLEN("Pflastersteine fehlen"),
	RISSE("Risse"),
	SONSTIGER_SCHADEN("sonstiger Schaden"),
	STARK_ABSCHUESSIGE_SEITENRAENDER("stark abschüssige Seitenränder"),
	STARK_WELLENARTIGE_OBERFLAECHE("stark wellenartige Oberfläche (holprig)"),
	WURZELHEBUNG_WURZELBRUECHE_WELLENBILDUNG("Wurzelhebung / -brüche / Wellenbildung"),
	;

	@NonNull
	@Getter
	private final String displayText;

	public static String toDisplayText(Set<Schadenart> schaeden) {
		return schaeden.stream().map(s -> s.getDisplayText())
			.collect(Collectors.joining(", "));
	}
}
