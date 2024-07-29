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

package de.wps.radvis.backend.manuellerimport.common.domain.valueobject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Set;

import de.wps.radvis.backend.common.domain.SetToStringAttributeConverter;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import jakarta.persistence.Convert;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Embeddable
public class Konflikt {

	@Embedded
	private LinearReferenzierterAbschnitt linearReferenzierterAbschnitt;
	@Enumerated(EnumType.STRING)
	private Seitenbezug seitenbezug;
	private String attributName;
	private String uebernommenerWert;
	@Convert(converter = SetToStringAttributeConverter.class)
	private Set<String> nichtUebernommeneWerte;
	private String bemerkung;

	public Konflikt(String attributName, String uebernommenerWert, Set<String> nichtUebernommeneWerte) {
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt1 = LinearReferenzierterAbschnitt.of(0, 1);
		require(linearReferenzierterAbschnitt1, notNullValue());
		require(attributName, notNullValue());
		require(uebernommenerWert, notNullValue());
		require(nichtUebernommeneWerte, notNullValue());
		require(!nichtUebernommeneWerte.isEmpty(),
			"Bei einem Konflikt muss es immer zumindest einen Wert geben, der nicht übernommen wurde");

		this.linearReferenzierterAbschnitt = linearReferenzierterAbschnitt1;
		this.attributName = attributName;
		this.uebernommenerWert = uebernommenerWert;
		this.nichtUebernommeneWerte = nichtUebernommeneWerte;
	}

	public Konflikt(LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, Seitenbezug seitenbezug,
		String attributName,
		String uebernommenerWert, Set<String> nichtUebernommeneWerte, String bemerkung) {
		require(linearReferenzierterAbschnitt, notNullValue());
		require(seitenbezug, notNullValue());
		require(attributName, notNullValue());
		require(uebernommenerWert, notNullValue());
		require(nichtUebernommeneWerte, notNullValue());
		require(!nichtUebernommeneWerte.isEmpty(),
			"Bei einem Konflikt muss es immer zumindest einen Wert geben, der nicht übernommen wurde");
		require(bemerkung, notNullValue());

		this.linearReferenzierterAbschnitt = linearReferenzierterAbschnitt;
		this.seitenbezug = seitenbezug;
		this.attributName = attributName;
		this.uebernommenerWert = uebernommenerWert;
		this.nichtUebernommeneWerte = nichtUebernommeneWerte;
		this.bemerkung = bemerkung;
	}
}
