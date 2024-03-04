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

package de.wps.radvis.backend.fahrradzaehlstelle.schnittstelle;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.fahrradzaehlstelle.domain.FahrradzaehlstelleRepository;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.FahrradzaehlstelleService;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.ArtDerAuswertung;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.FahrradzaehlstelleAuswertung;
import de.wps.radvis.backend.fahrradzaehlstelle.schnittstelle.view.FahrradzaehlstelleDetailView;
import de.wps.radvis.backend.fahrradzaehlstelle.schnittstelle.view.FahrradzaehlstelleListView;
import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/fahrradzaehlstelle")
@Validated
public class FahradzaehlstelleController {

	private final FahrradzaehlstelleRepository fahrradzaehlstelleRepository;
	private final FahrradzaehlstelleService fahrradzaehlstelleService;

	public FahradzaehlstelleController(FahrradzaehlstelleRepository fahrradzaehlstelleRepository,
		FahrradzaehlstelleService fahrradzaehlstelleService) {
		this.fahrradzaehlstelleRepository = fahrradzaehlstelleRepository;
		this.fahrradzaehlstelleService = fahrradzaehlstelleService;
	}

	@GetMapping("/list")
	public List<FahrradzaehlstelleListView> getAlleFahrradzaehlstellen() {
		return StreamSupport.stream(fahrradzaehlstelleRepository.findAll().spliterator(), false)
			.map(FahrradzaehlstelleListView::new).collect(Collectors.toList());
	}

	@GetMapping("/{id}")
	public FahrradzaehlstelleDetailView getFahrradzaehlstelle(@PathVariable("id") Long id) {
		return new FahrradzaehlstelleDetailView(fahrradzaehlstelleRepository.findById(id).orElseThrow(
			EntityNotFoundException::new));
	}

	@GetMapping("/channelData")
	public FahrradzaehlstelleAuswertung getChannelData(@RequestParam List<Long> channelIds,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Instant startDate,
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Instant endDate,
		@RequestParam ArtDerAuswertung artDerAuswertung) {

		return fahrradzaehlstelleService.getAuswertung(channelIds, startDate, endDate, artDerAuswertung);

	}

}
