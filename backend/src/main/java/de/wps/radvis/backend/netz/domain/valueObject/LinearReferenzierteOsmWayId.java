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

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonSerialize(using = OsmWayIdWithLRJsonSerializer.class)
@JsonDeserialize(using = OsmWayIdWithLRJsonDeserializer.class)
@Embeddable
@NoArgsConstructor
@EqualsAndHashCode
public class LinearReferenzierteOsmWayId {

	@Getter
	@Setter(AccessLevel.PRIVATE)
	private Long value;

	@Getter
	@Setter(AccessLevel.PRIVATE)
	@Embedded
	private LinearReferenzierterAbschnitt linearReferenzierterAbschnitt;

	private LinearReferenzierteOsmWayId(long value, LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		this.value = value;
		this.linearReferenzierterAbschnitt = linearReferenzierterAbschnitt;
	}

	public static LinearReferenzierteOsmWayId of(long value,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		require(value, notNullValue());
		require(linearReferenzierterAbschnitt, notNullValue());
		return new LinearReferenzierteOsmWayId(value, linearReferenzierterAbschnitt);
	}

	@Override
	public String toString() {
		return String.format("%s[id=%s, lr=%s]", this.getClass().getSimpleName(), value,
			linearReferenzierterAbschnitt.toString());
	}
}