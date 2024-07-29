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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject;

import java.util.Set;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AttributUebernahmeFehler {

	private final String message;

	private final Set<String> nichtUerbenommeneWerte;

	private final LinearReferenzierterAbschnitt linearReferenzierterAbschnitt;

	private final Seitenbezug seitenbezug;

	public AttributUebernahmeFehler(String message, Set<String> nichtUerbenommeneWerte,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		this(message, nichtUerbenommeneWerte, linearReferenzierterAbschnitt, Seitenbezug.BEIDSEITIG);
	}
}
