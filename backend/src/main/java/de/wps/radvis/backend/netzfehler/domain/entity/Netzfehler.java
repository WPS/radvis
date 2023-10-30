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

package de.wps.radvis.backend.netzfehler.domain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerBeschreibung;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerTyp;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Netzfehler extends AbstractEntity {
	@NonNull
	@Enumerated(EnumType.STRING)
	private NetzfehlerTyp netzfehlerTyp;

	@NonNull
	private NetzfehlerBeschreibung netzfehlerBeschreibung;

	@NonNull
	private String jobZuordnung;

	@NonNull
	private Geometry geometry;

	private boolean erledigt;

	public Netzfehler(@NonNull NetzfehlerTyp netzfehlerTyp, @NonNull NetzfehlerBeschreibung netzfehlerBeschreibung,
		String jobZuordnung, @NonNull Geometry geometry) {
		super();
		this.netzfehlerTyp = netzfehlerTyp;
		this.netzfehlerBeschreibung = netzfehlerBeschreibung;
		this.jobZuordnung = jobZuordnung;
		this.geometry = geometry;
		this.erledigt = false;
	}

	public void alsErledigtMarkieren() {
		erledigt = true;
	}
}
