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
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class KurzeDlmKantenKonsistenzregel implements Konsistenzregel {

	public static String VERLETZUNGS_TYP = "KURZE_DLM_KANTE";

	private final KantenRepository kantenRepository;

	@Override
	public List<KonsistenzregelVerletzungsDetails> pruefen() {
		return kantenRepository.findAllByQuelleEqualsAndKantenLaengeInCmLessThan(QuellSystem.DLM, 100)
			.stream()
			.map(this::getKonsistenzregelVerletzungsDetails)
			.collect(Collectors.toList());
	}

	private KonsistenzregelVerletzungsDetails getKonsistenzregelVerletzungsDetails(Kante kante) {
		return new KonsistenzregelVerletzungsDetails(kante.getGeometry().getCentroid(),
			kante.getGeometry(), "Das DLM enthält an dieser Stelle eine auffällig kurze Kante (< 1 m)",
			kante.getId().toString());
	}

	@Override
	public String getVerletzungsTyp() {
		return VERLETZUNGS_TYP;
	}

	@Override
	public String getTitel() {
		return "Kantenlänge < 1m";
	}

	@Override
	public RegelGruppe getGruppe() {
		return RegelGruppe.DLM;
	}
}
