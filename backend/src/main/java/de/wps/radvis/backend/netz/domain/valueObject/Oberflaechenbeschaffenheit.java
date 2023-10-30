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
import lombok.NonNull;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum Oberflaechenbeschaffenheit {

	// @formatter:off
	NEUWERTIG("Neuwertig"),
	SEHR_GUTER_BIS_GUTER_ZUSTAND("Sehr guter bis guter Zustand"),
	GUTER_BIS_MITTLERER_ZUSTAND("Guter bis mittlerer Zustand"),
	ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE("Anlass zur intensiven Beobachtung und Analyse"),
	EINLEITUNG_BAULICHER_ODER_VERKEHRSBESCHRAENKENDER_MASSNAHMEN("Einleitung baulicher oder verkehrsbeschränkender Maßnahmen"),
	UNBEKANNT("Unbekannt")
	;
	// @formatter:on

	@NonNull
	private String displayText;

	@Override
	public String toString() {
		return displayText;
	}

	public Oberflaechenbeschaffenheit nichtUnbekanntOrElse(Oberflaechenbeschaffenheit otherValue) {
		return Oberflaechenbeschaffenheit.UNBEKANNT.equals(this) ? otherValue : this;
	}

	public boolean widerspruchZu(Oberflaechenbeschaffenheit other) {
		return !(this == Oberflaechenbeschaffenheit.UNBEKANNT || other == Oberflaechenbeschaffenheit.UNBEKANNT || this
			.equals(other));
	}
}
