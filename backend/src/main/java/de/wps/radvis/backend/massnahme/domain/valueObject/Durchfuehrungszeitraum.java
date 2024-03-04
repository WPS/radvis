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

package de.wps.radvis.backend.massnahme.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Durchfuehrungszeitraum {

	private Integer geplanterUmsetzungsstartJahr;

	@JsonIgnore
	private LocalDateTime vonZeitpunkt;

	@JsonIgnore
	private LocalDateTime bisZeitpunkt;

	private Durchfuehrungszeitraum(Integer geplanterUmsetzungsstartJahr) {
		require(geplanterUmsetzungsstartJahr, notNullValue());
		require(isJahrValid(geplanterUmsetzungsstartJahr), "Jahr ist nicht valide.");
		this.geplanterUmsetzungsstartJahr = geplanterUmsetzungsstartJahr;
		setzeStartUndEndzeitAusJahr(geplanterUmsetzungsstartJahr);
	}

	@JsonCreator
	public static Durchfuehrungszeitraum of(Integer geplanterUmsetzungsstartJahr) {
		return new Durchfuehrungszeitraum(geplanterUmsetzungsstartJahr);
	}

	public static Durchfuehrungszeitraum of(String geplanterUmsetzungsstartJahr) {
		return new Durchfuehrungszeitraum(Integer.parseInt(geplanterUmsetzungsstartJahr));
	}

	public static boolean isValid(String value) {
		try {
			return Durchfuehrungszeitraum.isJahrValid(Integer.parseInt(value));
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static boolean isJahrValid(Integer jahr) {
		return jahr >= 2000 && jahr <= 3000;
	}

	private void setzeStartUndEndzeitAusJahr(Integer jahr) {
		vonZeitpunkt = LocalDateTime.of(jahr, 1, 1, 0, 0);
		bisZeitpunkt = LocalDateTime.of(jahr, 12, 31, 23, 59);
	}

}
