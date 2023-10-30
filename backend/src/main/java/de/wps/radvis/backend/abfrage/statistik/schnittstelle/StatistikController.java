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

package de.wps.radvis.backend.abfrage.statistik.schnittstelle;

import java.util.Set;

import jakarta.transaction.Transactional;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.abfrage.statistik.domain.StatistikRepository;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import lombok.NonNull;

@RestController
@RequestMapping("/api/statistik")
public class StatistikController {

	private final StatistikRepository statistikRepository;
	private final KantenRepository kantenRepository;

	public StatistikController(@NonNull StatistikRepository statistikRepository,
		@NonNull KantenRepository kantenRepository) {
		this.statistikRepository = statistikRepository;
		this.kantenRepository = kantenRepository;
	}

	@GetMapping("matchingquote/{quelle}")
	public double getMatchingQuote(@PathVariable String quelle) {
		QuellSystem quellSystem = QuellSystem.valueOf(quelle);
		if (quellSystem == QuellSystem.DLM) {
			throw new RuntimeException("Diese Abfrage ist nicht sinnvoll, das DLM wird nicht explizit abgebildet");
		}
		return this.statistikRepository.getDlmMatchingQuote(quellSystem);
	}

	@Transactional
	@GetMapping("laengeInMeterQuelle/{quelle}")
	public double getlaengeInMeter(@PathVariable String quelle) {
		return this.kantenRepository.findKanteByQuelle(QuellSystem.valueOf(quelle)).map(
			Kante::getGeometry).mapToDouble(LineString::getLength).sum();
	}

	@Transactional
	@GetMapping("laengeInMeterNetzklasse/{netzklasse}")
	public double getlaengeInMeterNachNetzklasse(@PathVariable String netzklasse) {
		return this.kantenRepository
			.getKantenInBereichNachNetzklasse(new Envelope(0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE),
				Set.of(NetzklasseFilter.valueOf(netzklasse)), false)
			.stream().map(
				Kante::getGeometry)
			.mapToDouble(LineString::getLength).sum();
	}

}
