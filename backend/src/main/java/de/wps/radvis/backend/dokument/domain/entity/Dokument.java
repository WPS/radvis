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

package de.wps.radvis.backend.dokument.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Dokument extends AbstractEntity {

	public static final int DATEINAME_MAX_LENGTH = 255;

	private String dateiname;

	private int dateigroesseInBytes;

	@ManyToOne
	private Benutzer benutzer;

	LocalDateTime datum;

	@Basic(fetch = FetchType.LAZY)
	private byte[] datei;

	@Builder()
	public Dokument(Long id, String dateiname, Benutzer benutzer, byte[] datei, LocalDateTime datum) {
		super(id);
		require(dateiname, notNullValue());
		require(isDateinameValid(dateiname));
		require(benutzer, notNullValue());
		require(datei, notNullValue());
		require(isValid(datei));
		require(datum, notNullValue());
		this.dateiname = dateiname;
		this.benutzer = benutzer;
		this.datei = datei;
		this.dateigroesseInBytes = datei.length;
		this.datum = datum;
	}

	public Dokument(String dateiname, Benutzer benutzer, byte[] datei, LocalDateTime datum) {
		this(null,
			dateiname,
			benutzer,
			datei,
			datum);
	}

	public static boolean isDateinameValid(String value) {
		return value.length() <= DATEINAME_MAX_LENGTH;
	}

	public static boolean isValid(byte[] fileContent) {
		return fileContent.length < 100_000_000;
	}
}
