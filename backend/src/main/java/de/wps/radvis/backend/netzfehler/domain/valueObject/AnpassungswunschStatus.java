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

package de.wps.radvis.backend.netzfehler.domain.valueObject;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum AnpassungswunschStatus {

	OFFEN(false, "Offen"),
	KLAERUNGSBEDARF(false, "Klärungsbedarf"),
	KORRIGIERT(false, "Im Drittsystem korrigiert"),
	NACHBEARBEITUNG(false, "In Nachbearbeitung"),
	ABGELEHNT(true, "Abgelehnt"),
	ERLEDIGT(true, "Erledigt"),
	ZURUECKGEZOGEN(true, "Zurückgezogen"),

	// Dieser Zustand kann nur automatisch gesetzt werden.
	// Z.B., wenn die zugrunde liegende KonsistenzregelVerletzung nicht mehr existiert.
	UMGESETZT(true, "Umgesetzt");

	private final boolean abgeschlossen;
	private final String displayText;

	@Override
	public String toString() {
		return displayText;
	}

	public boolean istAbgeschlossen() {
		return this.abgeschlossen;
	}

	public static final List<AnpassungswunschStatus> ALLE_ABGESCHLOSSENEN = Arrays.stream(values())
		.filter(AnpassungswunschStatus::istAbgeschlossen)
		.collect(Collectors.toList());
}
