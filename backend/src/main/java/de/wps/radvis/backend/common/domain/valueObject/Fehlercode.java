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

package de.wps.radvis.backend.common.domain.valueObject;

import static org.valid4j.Assertive.require;

import lombok.Getter;

/**
 * Fehlercodes wie in https://bis2wps.atlassian.net/wiki/spaces/RAD/pages/1457913858/Fehlercodes dokumentiert
 * 
 */
public enum Fehlercode {
	HINTERGRUNDKARTEN("101"),
	ORTSSUCHE("102"),
	DATEI_LAYER("103"),
	DLM_REIMPORT("201"),
	OSM_DOWNLOAD("202"),
	FAHRRADROUTE_TOUBIZ_IMPORT("203"),
	FAHRRADROUTE_TFIS_IMPORT("204"),
	LEIHSTATIONEN_IMPORT("205"),
	SERVICESTATIONEN_IMPORT("206"),
	ABSTELLANLAGEN_IMPORT("207"),
	WEGWEISENDE_BESCHILDERUNG_IMPORT("208"),
	FAHRRADZAEHLSTELLEN_IMPORT("209"),
	MAILVERSAND("300"),
	SONSTIGES("000"); // als default-Wert in logback-spring.xml

	@Getter
	private String codeNumber;

	private Fehlercode(String codeNumber) {
		require(codeNumber.length() == 3, "Fehlercodes müssen dreistellig sein.");
		this.codeNumber = codeNumber;
	}
}
