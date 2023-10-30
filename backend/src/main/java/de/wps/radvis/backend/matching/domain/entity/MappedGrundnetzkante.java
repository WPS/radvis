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

package de.wps.radvis.backend.matching.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.ensure;
import static org.valid4j.Assertive.require;

import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MappedGrundnetzkante {
	private final Long kanteId;
	private final LinearReferenzierterAbschnitt linearReferenzierterAbschnitt;

	public MappedGrundnetzkante(LineString kantenGeometrie, Long kanteId, LineString dlmMatchingResult) {
		require(kantenGeometrie, notNullValue());
		require(kanteId, notNullValue());
		require(dlmMatchingResult, notNullValue());
		LineString ueberschneidung = LineStrings.calculateUeberschneidungslinestring(kantenGeometrie, dlmMatchingResult)
			.orElseThrow();

		this.kanteId = kanteId;
		this.linearReferenzierterAbschnitt = LinearReferenzierterAbschnitt.of(kantenGeometrie, ueberschneidung);

		ensure(this.kanteId, notNullValue());
		ensure(this.linearReferenzierterAbschnitt, notNullValue());
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof MappedGrundnetzkante) {
			MappedGrundnetzkante mappedGrundnetzkante = (MappedGrundnetzkante) object;
			return mappedGrundnetzkante.kanteId.equals(this.kanteId);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.kanteId.intValue();
	}
}
