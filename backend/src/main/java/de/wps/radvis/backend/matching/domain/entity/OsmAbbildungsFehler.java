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

import java.time.LocalDateTime;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OsmAbbildungsFehler extends AbstractEntity {

	private Long kanteId;
	private Geometry originalGeometry;
	private LocalDateTime datum;

	private boolean radnetz;
	private boolean kreisnetz;
	private boolean kommunalnetz;

	public OsmAbbildungsFehler(Long kanteId, Geometry originalGeometry, LocalDateTime datum, boolean radnetz,
		boolean kreisnetz, boolean kommunalnetz) {
		this.kanteId = kanteId;
		this.originalGeometry = originalGeometry;
		this.datum = datum;
		this.radnetz = radnetz;
		this.kreisnetz = kreisnetz;
		this.kommunalnetz = kommunalnetz;
	}

	public boolean isSonstigeNetzklasse() {
		return !radnetz && !kreisnetz && !kommunalnetz;
	}
}
