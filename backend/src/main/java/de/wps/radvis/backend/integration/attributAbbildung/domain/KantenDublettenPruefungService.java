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

package de.wps.radvis.backend.integration.attributAbbildung.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.wps.radvis.backend.common.domain.exception.KeineUeberschneidungException;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KanteDublette;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KantenDublettenPruefungService {

	public List<KanteDublette> findDubletten(Set<Kante> geometrischFuehrendeKanten,
		Set<Kante> geometrischUntergeordneteKanten) {
		List<Kante> geometrischFuehrendeKantenDistinktUndSortiert = Kante
			.distinktiereUndSortiereNachMinYDerGeometrie(geometrischFuehrendeKanten);
		log.info("Anzahl der zu verarbeitenden geometrische führenden Kanten: "
			+ geometrischFuehrendeKantenDistinktUndSortiert.size());
		List<Kante> geometrischUntergeordneteKantenDistinktUndSortiert = Kante
			.distinktiereUndSortiereNachMinYDerGeometrie(geometrischUntergeordneteKanten);
		log.info("Anzahl der zu verarbeitenden geometrische untergeordneten Kanten: "
			+ geometrischUntergeordneteKantenDistinktUndSortiert.size());
		List<KanteDublette> kantenDubletten = new ArrayList<>();
		long i = 0;

		for (Kante geometrischFuehrendeKante : geometrischFuehrendeKantenDistinktUndSortiert) {
			for (Kante geometrischUntergeordneteKante : geometrischUntergeordneteKantenDistinktUndSortiert) {
				if (geometrischUntergeordneteKante.getZugehoerigeDlmGeometrie().getEnvelopeInternal().getMaxY()
					< geometrischFuehrendeKante.getZugehoerigeDlmGeometrie().getEnvelopeInternal().getMinY()) {
					continue;
				}
				try {
					KanteDublette kanteDublette = new KanteDublette(geometrischFuehrendeKante,
						geometrischUntergeordneteKante);
					kantenDubletten.add(kanteDublette);
				} catch (KeineUeberschneidungException e) {
					if (geometrischUntergeordneteKante.getZugehoerigeDlmGeometrie().getEnvelopeInternal().getMinY()
						> geometrischFuehrendeKante.getZugehoerigeDlmGeometrie().getEnvelopeInternal().getMaxY()) {
						break;
					}
				}
			}
			// Zaehlerstand loggen
			if (geometrischFuehrendeKanten.size() > 5 && i % (geometrischFuehrendeKanten.size() / 5) == 0) {
				log.info("{}% der geometrisch führenden Kanten verarbeitet.",
					i / (double) geometrischFuehrendeKanten.size() * 100);
			}
			i++;
		}

		return kantenDubletten;
	}

}
