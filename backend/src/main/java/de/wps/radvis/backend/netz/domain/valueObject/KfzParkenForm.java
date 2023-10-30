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
public enum KfzParkenForm {

	PARKBUCHTEN("Parkbuchten"),
	FAHRBAHNPARKEN_MARKIERT("Fahrbahnparken (markiert)"),
	FAHRBAHNPARKEN_UNMARKIERT("Fahrbahnparken (unmarkiert)"),
	GEHWEGPARKEN_MARKIERT("Gehwegparken (markiert)"),
	GEHWEGPARKEN_UNMARKIERT("Gehwegparken (unmarkiert)"),
	HALBES_GEHWEGPARKEN_MARKIERT("Halbes Gehwegparken (markiert)"),
	HALBES_GEHWEGPARKEN_UNMARKIERT("Halbes Gehwegparken (unmarkiert)"),
	UNBEKANNT("Unbekannt");

	@NonNull
	private String displayText;

	@Override
	public String toString() {
		return displayText;
	}

	public KfzParkenForm nichtUnbekanntOrElse(KfzParkenForm otherValue) {
		return KfzParkenForm.UNBEKANNT.equals(this) ? otherValue : this;
	}

	public boolean widerspruchZu(KfzParkenForm other) {
		return !(this == KfzParkenForm.UNBEKANNT || other == KfzParkenForm.UNBEKANNT || this.equals(other));
	}
}
