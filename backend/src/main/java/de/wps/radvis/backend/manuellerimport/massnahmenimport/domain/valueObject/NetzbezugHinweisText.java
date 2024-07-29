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

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import lombok.Getter;

@Getter
public enum NetzbezugHinweisText {
	FALSCHER_GEOMETRIE_TYP("Netzbezug nicht automatisiert ermittelbar.",
		"Unzulässiger Geometrie-Typ. "
			+ "Erlaubt sind (Multi)Point und (Multi)LineString sowie eine GeometryCollection, "
			+ "die nur (Multi)Points und/oder (Multi)LineStrings enthält. "
			+ "Wenn Sie die Maßnahme importieren möchten, müssen Sie den Netzbezug bearbeiten."),

	GEOMETRIEN_ABWEICHEND("Abweichende Geometrie gefunden.",
		"Die Geometrie der importierten Maßnahme weicht von der Geometrie der zu bearbeitenden Maßnahme ab."),

	NETZBEZUG_NICHT_GEFUNDEN("Kein Netzbezug gefunden.",
		"Wenn Sie die Maßnahme importieren möchten, müssen Sie den Netzbezug bearbeiten."),

	NETZBEZUG_UNVOLLSTAENDIG("Kein vollständiger Netzbezug gefunden.",
		"Es wurde kein vollständiger Netzbezug gefunden. "
			+ "Wenn Sie die Maßnahme ohne Bearbeitung des Netzbezuges importieren, "
			+ "wird nur der gefundene Netzbezug berücksichtigt.");

	private final String text;

	private final String tooltip;

	NetzbezugHinweisText(String text, String tooltip) {
		require(text, notNullValue());
		require(tooltip, notNullValue());
		this.tooltip = tooltip;
		this.text = text;
	}
}
