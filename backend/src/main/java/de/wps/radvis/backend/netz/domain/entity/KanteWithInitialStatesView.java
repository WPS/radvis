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

package de.wps.radvis.backend.netz.domain.entity;

import static org.valid4j.Assertive.require;

import org.hibernate.annotations.Immutable;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.netz.domain.valueObject.Status;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@ToString
@Getter
@Immutable
public class KanteWithInitialStatesView {
	@Id
	private long kanteId;

	@Enumerated(EnumType.STRING)
	private Status status;

	private long vonKnotenId;

	private short vonKnotenGrad;

	private long nachKnotenId;

	private short nachKnotenGrad;

	private int kantenLaengeInCm;

	private LineString kanteGeometrie;

	public void decreaseKnotenGrad(long knotenId) {
		require(containsKnoten(knotenId));

		if (getVonKnotenId() == knotenId) {
			vonKnotenGrad--;
		} else {
			nachKnotenGrad--;
		}
	}

	/**
	 * Ermittelt den jeweils anderen Knoten dieser Kante. Ist der angegebene Knoten der von-Knoten, wird der nach-
	 * Knoten zurückgegeben und umgekehrt.
	 */
	public Long getOtherKnoten(Long knotenId) {
		require(containsKnoten(knotenId));
		return vonKnotenId == knotenId ? nachKnotenId : vonKnotenId;
	}

	public boolean containsKnoten(Long knotenId) {
		return vonKnotenId == knotenId || nachKnotenId == knotenId;
	}
}
