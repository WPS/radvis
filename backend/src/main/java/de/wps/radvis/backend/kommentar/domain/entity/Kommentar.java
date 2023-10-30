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

package de.wps.radvis.backend.kommentar.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Entity
@Getter
public class Kommentar extends AbstractEntity {
	public static final int MAX_LENGTH = 4000;

	private String kommentarText;

	private LocalDateTime datum;

	@ManyToOne
	private Benutzer benutzer;

	@Builder(builderMethodName = "privateBuilder")
	private Kommentar(String kommentarText, Benutzer benutzer, LocalDateTime datum) {
		require(isValid(kommentarText));
		require(benutzer, notNullValue());
		require(datum, notNullValue());
		this.datum = datum;
		this.kommentarText = kommentarText;
		this.benutzer = benutzer;
	}

	public Kommentar(String kommentarText, Benutzer benutzer) {
		this(kommentarText, benutzer, LocalDateTime.now());
	}

	public static boolean isValid(String value) {
		return value != null && value.length() <= MAX_LENGTH;
	}
}
