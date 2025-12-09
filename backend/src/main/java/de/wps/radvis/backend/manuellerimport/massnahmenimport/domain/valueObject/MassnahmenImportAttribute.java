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

package de.wps.radvis.backend.manuellerimport.massnahmenimport.domain.valueObject;

import java.util.List;

public enum MassnahmenImportAttribute {
	// Pflichtattribute
	UMSETZUNGSSTATUS("Umsetzungsstatus"),
	BEZEICHNUNG("Bezeichnung"),
	KATEGORIEN("Kategorien"),
	ZUSTAENDIGER("Zuständige/r"),
	SOLL_STANDARD("Soll-Standard"),
	DURCHFUEHRUNGSZEITRAUM("Durchführungszeitraum"),
	BAULASTTRAEGER("Baulastträger"),
	HANDLUNGSVERANTWORTLICHER("Wer soll tätig werden?"),

	// optionale Attribute
	PRIORITAET("Priorität"),
	KOSTENANNAHME("Kostenannahme"),
	UNTERHALTSZUSTAENDIGER("Unterhaltszuständige/r"),
	MAVIS_ID("MaViS-ID"),
	VERBA_ID("Verba-ID"),
	LGVFG_ID("LGVFG-ID"),
	REALISIERUNGSHILFE("Realisierungshilfe"),
	NETZKLASSEN("Netzklassen"),
	PLANUNG_ERFORDERLICH("Planung erforderlich"),
	VEROEFFENTLICHT("Veröffentlicht"),
	ZURUECKSTELLUNGS_GRUND("Zurückstellungsgrund"),
	BEGRUENDUNG_ZURUECKSTELLUNG("Begründung Zurückstellung"),
	BEGRUENDUNG_STORNIERUNGSANFRAGE("Begründung Stornierungsanfrage"),
	;

	private final String displayText;

	MassnahmenImportAttribute(String displayText) {
		this.displayText = displayText;
	}

	@Override
	public String toString() {
		return displayText;
	}

	public static List<MassnahmenImportAttribute> getPflichtAttribute() {
		return List.of(UMSETZUNGSSTATUS, BEZEICHNUNG, KATEGORIEN, ZUSTAENDIGER, SOLL_STANDARD);
	}
}
