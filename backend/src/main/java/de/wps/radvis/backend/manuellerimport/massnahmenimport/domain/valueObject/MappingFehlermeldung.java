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

public enum MappingFehlermeldung {
	MASSNAHME_KEINE_ID("ID fehlt."),
	MASSNAHME_MEHRFACH("ID in der Quelle mehrfach vorhanden."),
	MASSNAHME_NICHT_EINDEUTIG("%s Maßnahmen gefunden."),
	MASSNAHME_NICHT_GEFUNDEN("Maßnahme nicht gefunden."),
	PFLICHTATTRIBUT_NICHT_AUSGEWAEHLT("Pflichtattribut nicht ausgewählt."),
	PFLICHTATTRIBUT_NICHT_GESETZT("Pflichtattribut fehlt."),
	ATTRIBUT_WERT_UNGUELTIG("'%s' ist kein gültiger Wert."),
	VERWALTUNGSEINHEIT_NICHT_GEFUNDEN("Verwaltungseinheit '%s' existiert nicht."),
	QUERVALIDIERUNG_PFLICHTATTRIBUTE("Pflichtattribut für Umsetzungsstatus '%s' fehlt."),
	QUERVALIDIERUNG_MASSNAHMENKATEGORIE("Nur eine Kategorie pro Oberkategorie erlaubt."),
	LOESCHUNG_QUELLE_RADNETZ_UNGUELTIG("Maßnahme hat Quelle RadNETZ und kann daher nicht gelöscht werden."),
	MASSNAHME_ID_INVALID(
		"Die ID %s ist ungültig: Erlaubt sind alphanumerische Zeichen sowie Punkt, Leerzeichen und Unterstrich");

	private final String fehlermeldung;

	MappingFehlermeldung(String fehlermeldung) {
		this.fehlermeldung = fehlermeldung;
	}

	public String getText(Object... args) {

		return String.format(fehlermeldung, args);
	}
}
