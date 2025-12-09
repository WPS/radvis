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

package de.wps.radvis.backend.massnahme.domain.valueObject;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public enum Umsetzungsstatus {

	// @formatter:off
	IDEE("Idee"),
	PLANUNG("Planung"),
	UMSETZUNG("Umsetzung"),
	UMGESETZT("Umgesetzt"),
	STORNIERT("Storniert"),
	ZURUECKGESTELLT("Zurückgestellt"),
	STORNIERUNG_ANGEFRAGT("Stornierung angefragt"),
	STORNIERT_ENGSTELLE("Storniert (Engstelle begründet)"),
	STORNIERT_NICHT_ERFORDERLICH("Storniert (Nicht mehr erforderlich)")
	;
	// @formatter:on

	@NonNull
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}

	public static final List<Umsetzungsstatus> STATUS_AB_PLANUNG = List.of(Umsetzungsstatus.PLANUNG,
		Umsetzungsstatus.UMGESETZT, Umsetzungsstatus.UMSETZUNG);

	public static boolean isAbPlanung(Umsetzungsstatus umsetzungsstatus) {
		return STATUS_AB_PLANUNG.contains(umsetzungsstatus);
	}

	public static final List<Umsetzungsstatus> STORNIERT_STATUS = List.of(Umsetzungsstatus.STORNIERT,
		Umsetzungsstatus.STORNIERT_ENGSTELLE, Umsetzungsstatus.STORNIERT_NICHT_ERFORDERLICH);

	public static boolean isStorniert(Umsetzungsstatus umsetzungsstatus) {
		return STORNIERT_STATUS.contains(umsetzungsstatus);
	}

	/**
	 * Status, bei denen die Maßnahme als "historisch" gilt und bei entsprechender Filterung nicht angezeigt werden soll
	 */
	public static final List<Umsetzungsstatus> HISTORISCH_STATUS = List.of(Umsetzungsstatus.STORNIERT,
		Umsetzungsstatus.STORNIERT_ENGSTELLE, Umsetzungsstatus.STORNIERT_NICHT_ERFORDERLICH,
		Umsetzungsstatus.UMGESETZT);
}
