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

package de.wps.radvis.backend.konsistenz.regeln.domain;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.service.SackgassenService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import lombok.AllArgsConstructor;

@AllArgsConstructor
abstract public class AbstractNetzklassenLueckeKonsistenzregel implements Konsistenzregel {

	private SackgassenService sackgassenService;

	@Override
	public List<KonsistenzregelVerletzungsDetails> pruefen() {
		return sackgassenService.bestimmeSackgassenknotenVonKantenFuerNetzklasse(getNetzklassen())
			.stream()
			.map(this::createVerletzungDetails)
			.collect(Collectors.toList());
	}

	@Override
	public RegelGruppe getGruppe() {
		return RegelGruppe.DATENPRUEFUNG;
	}

	abstract protected Set<Netzklasse> getNetzklassen();

	abstract protected String getBeschreibung();

	private KonsistenzregelVerletzungsDetails createVerletzungDetails(Knoten knoten) {
		return new KonsistenzregelVerletzungsDetails(
			knoten.getPoint(),
			getBeschreibung(),
			knoten.getId().toString());
	}
}
