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

package de.wps.radvis.backend.abfrage.fehlerprotokoll.domain.entity;

import java.time.LocalDateTime;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GeoserverFehlerprotokoll extends AbstractEntity {

	public GeoserverFehlerprotokoll(FehlerprotokollEintrag eintrag) {
		this(eintrag.getIconPosition(), eintrag.getOriginalGeometry(), eintrag.getDatum(),
			eintrag.getTitel(), eintrag.getBeschreibung(), eintrag.getEntityLink());
	}

	private Geometry iconPosition;

	private Geometry originalGeometry;

	private LocalDateTime datum;

	private String titel;

	private String beschreibung;

	private String entityLink;
}
