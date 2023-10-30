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

package de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import com.fasterxml.jackson.annotation.JsonValue;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;

@EqualsAndHashCode
public class DurchschnittlicherZaehlstand {

	@Getter
	@NonNull
	private final Long absoluterWert;

	@Getter
	@NonNull
	private final Integer anzahlZaehlstaende;

	private DurchschnittlicherZaehlstand(Long absoluterWert, Integer anzahlZaehlstaende) {
		require(absoluterWert, notNullValue());
		require(anzahlZaehlstaende, notNullValue());
		this.absoluterWert = absoluterWert;
		this.anzahlZaehlstaende = anzahlZaehlstaende;
	}

	public static DurchschnittlicherZaehlstand of(Long absoluterWert, Integer anzahlZaehlstaende) {
		return new DurchschnittlicherZaehlstand(absoluterWert, anzahlZaehlstaende);
	}

	public static DurchschnittlicherZaehlstand mitAnfangsWert(Zaehlstand zaehlstand) {
		return DurchschnittlicherZaehlstand.of(zaehlstand.getValue(), 1);
	}

	public DurchschnittlicherZaehlstand union(DurchschnittlicherZaehlstand v2) {
		return DurchschnittlicherZaehlstand.of(v2.absoluterWert + this.absoluterWert,
			v2.anzahlZaehlstaende + this.anzahlZaehlstaende);
	}

	@JsonValue
	public Double getDurchschnittswert() {
		return absoluterWert.doubleValue() / anzahlZaehlstaende;
	}
}
