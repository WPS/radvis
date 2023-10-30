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

package de.wps.radvis.backend.konsistenz.pruefung.schnittstelle;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.transaction.Transactional;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.locationtech.jts.geom.Envelope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.abfrage.fehlerprotokoll.schnittstelle.view.FehlerprotokollView;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.RadvisViewController;
import de.wps.radvis.backend.konsistenz.pruefung.domain.KonsistenzregelVerletzungsRepository;
import de.wps.radvis.backend.konsistenz.pruefung.domain.entity.KonsistenzregelVerletzung;
import de.wps.radvis.backend.konsistenz.pruefung.schnittstelle.view.KonsistenzregelView;
import de.wps.radvis.backend.konsistenz.regeln.domain.Konsistenzregel;

@RestController
@RadvisViewController
@RequestMapping("/api/konsistenzregel")
public class KonsistenzregelController {

	private final List<Konsistenzregel> regeln;
	private final KonsistenzregelVerletzungsRepository konsistenzregelVerletzungsRepository;

	public KonsistenzregelController(KonsistenzregelVerletzungsRepository konsistenzregelVerletzungsRepository,
		List<Konsistenzregel> regeln) {
		this.regeln = regeln;
		this.konsistenzregelVerletzungsRepository = konsistenzregelVerletzungsRepository;
	}

	@GetMapping("/verletzung/list")
	@Transactional
	public List<FehlerprotokollView> getVerletzungen(
		@RequestParam Set<String> typen,
		@ModelAttribute("sichtbereich") Envelope sichtbereich
	) {
		Stream<KonsistenzregelVerletzung> allByTypIn = konsistenzregelVerletzungsRepository.findAllByTypInAndInBereich(
			typen,
			EnvelopeAdapter.toPolygon(sichtbereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()));
		return allByTypIn.map(FehlerprotokollView::new)
			.collect(Collectors.toList());
	}

	@GetMapping("/list")
	public Stream<KonsistenzregelView> getRegeln() {
		return regeln.stream().map(KonsistenzregelView::new);
	}

}
