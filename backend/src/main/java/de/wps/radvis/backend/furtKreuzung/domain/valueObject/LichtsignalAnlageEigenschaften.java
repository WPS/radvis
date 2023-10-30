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

package de.wps.radvis.backend.furtKreuzung.domain.valueObject;

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Optional;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode
@Embeddable
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LichtsignalAnlageEigenschaften {

	@Getter
	private boolean fahrradSignal;

	@Getter
	private boolean gruenVorlauf;

	@Getter
	private boolean getrenntePhasen;

	@Getter
	@Enumerated(EnumType.STRING)
	private Rechtsabbieger rechtsabbieger;

	@Getter
	@Enumerated(EnumType.STRING)
	private Linksabbieger linksabbieger;

	@Getter
	private boolean vorgezogeneHalteLinie;

	@Getter
	private boolean radAufstellflaeche;

	@Getter
	@Enumerated(EnumType.STRING)
	private GruenAnforderung gruenAnforderung;

	private Umlaufzeit umlaufzeit;

	@Builder
	public LichtsignalAnlageEigenschaften(
		boolean fahrradSignal,
		boolean gruenVorlauf,
		boolean getrenntePhasen,
		Rechtsabbieger rechtsabbieger,
		Linksabbieger linksabbieger,
		boolean vorgezogeneHalteLinie,
		boolean radAufstellflaeche,
		GruenAnforderung gruenAnforderung,
		Umlaufzeit umlaufzeit
	) {
		require(rechtsabbieger, notNullValue());
		require(linksabbieger, notNullValue());
		require(gruenAnforderung, notNullValue());

		this.fahrradSignal = fahrradSignal;
		this.gruenVorlauf = gruenVorlauf;
		this.getrenntePhasen = getrenntePhasen;
		this.rechtsabbieger = rechtsabbieger;
		this.linksabbieger = linksabbieger;
		this.vorgezogeneHalteLinie = vorgezogeneHalteLinie;
		this.radAufstellflaeche = radAufstellflaeche;
		this.gruenAnforderung = gruenAnforderung;
		this.umlaufzeit = umlaufzeit;
	}
}
