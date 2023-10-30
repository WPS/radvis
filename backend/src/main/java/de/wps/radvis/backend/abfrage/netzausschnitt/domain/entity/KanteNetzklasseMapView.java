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

package de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity;

import java.util.Set;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import lombok.Getter;

public class KanteNetzklasseMapView extends KanteMapView {

	@Getter
	private final Set<Netzklasse> netzklassen;

	public KanteNetzklasseMapView(Long id, Geometry geometrie, Geometry verlaufLinks, Geometry verlaufRechts,
		boolean isZweiseitig, KantenAttributGruppe kantenAttributGruppe) {
		super(id, geometrie, verlaufLinks, verlaufRechts, isZweiseitig);
		this.netzklassen = kantenAttributGruppe.getNetzklassen();
	}
}
