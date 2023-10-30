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

import java.util.HashSet;
import java.util.Set;

import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import lombok.Getter;
import lombok.NonNull;

public class NetzklassenStreckeVonKanten extends StreckeVonKanten {

	@Getter
	private final Set<Netzklasse> netzklassen;

	public NetzklassenStreckeVonKanten(@NonNull Kante kante, boolean vonKnotenEndpunkt, boolean nachKnotenEndpunkt) {
		super(kante, vonKnotenEndpunkt, nachKnotenEndpunkt);
		netzklassen = kante.getHoechsteNetzklassen().orElse(new HashSet<>());
	}

	public void addKante(Kante kante, boolean istNeuerKnotenEndpunkt) {
		require(passtAnStreckeRan(kante));
		super.addKante(kante, istNeuerKnotenEndpunkt);
	}

	public void merge(NetzklassenStreckeVonKanten other) {
		require(other.netzklassen.equals(netzklassen));
		super.merge(other);
	}

	@Override
	public boolean passtAnStreckeRan(Kante kante) {
		return kante.getHoechsteNetzklassen().orElse(new HashSet<>()).equals(netzklassen);
	}

	public boolean passtAnStreckeRan(NetzklassenStreckeVonKanten other) {
		return other.netzklassen.equals(netzklassen);
	}
}
