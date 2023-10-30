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

package de.wps.radvis.backend.matching.domain.valueObject;

import java.util.List;

import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.fahrradroute.domain.valueObject.Hoehenunterschied;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.LinearReferenzierteProfilEigenschaften;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfilRoutingResult {
	private List<Long> kantenIDs;
	private LineString routenGeometrie;
	private Hoehenunterschied anstieg;
	private Hoehenunterschied abstieg;
	private List<LinearReferenzierteProfilEigenschaften> linearReferenzierteProfilEigenschaften;

	public ProfilRoutingResult(RoutingResult routingResult,
		List<LinearReferenzierteProfilEigenschaften> linearReferenzierteProfilEigenschaften) {
		this.kantenIDs = routingResult.getKantenIDs();
		this.routenGeometrie = routingResult.getRoutenGeometrie();
		this.anstieg = routingResult.getAnstieg();
		this.abstieg = routingResult.getAbstieg();
		this.linearReferenzierteProfilEigenschaften = linearReferenzierteProfilEigenschaften;
	}
}
