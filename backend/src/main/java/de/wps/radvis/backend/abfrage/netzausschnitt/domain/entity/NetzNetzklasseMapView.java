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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.wps.radvis.backend.netz.domain.entity.Knoten;
import lombok.Getter;

public class NetzNetzklasseMapView {

	@Getter
	private Set<KanteNetzklasseMapView> kanten;

	@Getter
	private Set<Knoten> knoten;

	public NetzNetzklasseMapView(Set<KanteNetzklasseMapView> kanten, List<Knoten> knoten) {
		this.kanten = kanten;
		this.knoten = new HashSet<>(knoten);
	}
}
