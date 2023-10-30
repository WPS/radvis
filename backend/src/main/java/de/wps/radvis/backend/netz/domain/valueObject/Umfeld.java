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

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Umfeld {

	GESCHAEFTSSTRASSE("Geschäftsstraße"),
	STRASSE_MIT_HOHER_WOHNDICHTE_MISCHNUTZUNG("Straße mit hoher Wohndichte / Mischnutzung"),
	STRASSE_MIT_GERINGER_BIS_MITTLERER_WOHNDICHTE("Straße mit geringer bis mittlerer Wohndichte"),
	GEWERBEGEBIET("Gewerbegebiet"),
	UNBEKANNT("Unbekannt");

	private String displayText;

	@Override
	public String toString() {
		return displayText;
	}

	public Umfeld nichtUnbekanntOrElse(Umfeld otherValue) {
		return Umfeld.UNBEKANNT.equals(this) ? otherValue : this;
	}
}
