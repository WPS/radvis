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

import java.time.LocalDateTime;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.NetzBezugAenderungsArt;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@MappedSuperclass
public class NetzBezugAenderung extends AbstractEntity {

	@Enumerated(EnumType.STRING)
	private NetzBezugAenderungsArt netzBezugAenderungsArt;
	private Long netzEntityId;
	@OneToOne(optional = false)
	private Benutzer benutzer;
	private LocalDateTime datum;
	@Enumerated(EnumType.STRING)
	private NetzAenderungAusloeser ausloeser;
	private Geometry geometry;

	public Geometry getIconPosition() {
		return geometry.getCentroid();
	}

	public Geometry getOriginalGeometry() {
		return geometry;
	}

	public String getBeschreibung() {
		return String.format("Wegen des nächtlichen DLM-Reimports hat sich der Netzbezug verändert. Grund: %s",
			netzBezugAenderungsArt);
	}

}