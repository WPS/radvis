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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Netzklasse {
	RADNETZ_ALLTAG("Alltag (RadNETZ)", 6),
	RADNETZ_FREIZEIT("Freizeit (RadNETZ)", 5),
	RADNETZ_ZIELNETZ("Zielnetz (RadNETZ)", 4),

	RADSCHNELLVERBINDUNG("Radschnellverbindung", 3),
	RADVORRANGROUTEN("Radvorrangrouten", 2),

	KREISNETZ_FREIZEIT("Freizeit (Kreisnetz)", 1),
	KREISNETZ_ALLTAG("Alltag (Kreisnetz)", 1),

	KOMMUNALNETZ_FREIZEIT("Freizeit (Kommunalnetz)", 0),
	KOMMUNALNETZ_ALLTAG("Alltag (Kommunalnetz)", 0);

	private final String displayText;

	@Getter
	private final int prioritaet;

	@Override
	public String toString() {
		return displayText;
	}

	private final static String HOECHSTE_NETZKLASSE_BEZEICHNUNG_ALLTAG_UND_FREIZEIT_RADNETZ = "Alltag und Freizeit (RadNETZ)";

	public static String getHoechsteNetzklasseBezeichnung(Set<Netzklasse> netzklassen) {
		if (netzklassen.size() == 1) {
			Netzklasse netzklasse = netzklassen.iterator().next();
			switch (netzklasse) {
			case RADNETZ_ALLTAG:
			case RADNETZ_FREIZEIT:
			case RADNETZ_ZIELNETZ:
			case RADVORRANGROUTEN:
			case RADSCHNELLVERBINDUNG:
			case KOMMUNALNETZ_ALLTAG:
			case KOMMUNALNETZ_FREIZEIT:
			case KREISNETZ_ALLTAG:
			case KREISNETZ_FREIZEIT:
				return netzklasse.displayText;
			default:
				return "";
			}
		} else {
			if (netzklassen.size() == 2 && netzklassen.contains(RADNETZ_ALLTAG) && netzklassen.contains(
				RADNETZ_FREIZEIT)) {
				return HOECHSTE_NETZKLASSE_BEZEICHNUNG_ALLTAG_UND_FREIZEIT_RADNETZ;
			}
			return "";
		}
	}

	public static final Set<Netzklasse> RADNETZ_NETZKLASSEN = Set.of(RADNETZ_FREIZEIT, RADNETZ_ALLTAG, RADNETZ_ZIELNETZ);

	public static boolean isRadNETZ(Set<Netzklasse> netzklassen) {
		return RADNETZ_NETZKLASSEN.stream().anyMatch(netzklassen::contains);
	}
}
