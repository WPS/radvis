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

package de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.service;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.EntityManager;

import jakarta.transaction.Transactional;

import de.wps.radvis.backend.manuellerimport.netzzugehoerigkeit.domain.entity.NetzklasseImportSession;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ManuellerNetzklassenImportUebernahmeService {

	private final NetzService netzService;

	private final EntityManager entityManager;

	@Transactional
	public void uebernehmeNetzzugehoerigkeit(NetzklasseImportSession netzklasseImportSession) {

		Set<Kante> kanten = netzService.getKantenInOrganisationsbereichEagerFetchNetzklassen(
			netzklasseImportSession.getOrganisation());

		kanten.forEach(kante -> {
			Set<Netzklasse> neueNetzklassen = new HashSet<>(kante.getKantenAttributGruppe().getNetzklassen());
			if (netzklasseImportSession.getKanteIds().contains(kante.getId())) {
				neueNetzklassen.add(netzklasseImportSession.getNetzklasse());
				kante.ueberschreibeNetzklassen(neueNetzklassen);
			} else {
				neueNetzklassen.remove(netzklasseImportSession.getNetzklasse());
				kante.ueberschreibeNetzklassen(neueNetzklassen);
			}
		});
		log.info("Schreibe in DB...");
		entityManager.flush();
		entityManager.clear();
	}
}
