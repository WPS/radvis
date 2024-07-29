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

package de.wps.radvis.backend.weitereKartenebenen.schnittstelle;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.weitereKartenebenen.domain.WeitereKartenebenenConfigurationProperties;
import de.wps.radvis.backend.weitereKartenebenen.domain.entity.WeitereKartenebene;
import de.wps.radvis.backend.weitereKartenebenen.domain.repository.WeitereKartenebenenRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/weitere-kartenebenen")
@Validated
@Slf4j
public class WeitereKartenebenenController {
	private final WeitereKartenebenenRepository weitereKartenebenenRepository;
	private final BenutzerResolver benutzerResolver;
	private WeitereKartenebenenConfigurationProperties configurationProperties;

	public WeitereKartenebenenController(WeitereKartenebenenRepository weitereKartenebenenRepository,
		BenutzerResolver benutzerResolver, WeitereKartenebenenConfigurationProperties configurationProperties) {
		this.weitereKartenebenenRepository = weitereKartenebenenRepository;
		this.benutzerResolver = benutzerResolver;
		this.configurationProperties = configurationProperties;
	}

	@GetMapping("/list")
	public Stream<WeitereKartenebenenView> list(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (benutzer == null) {
			return Stream.empty();
		}

		return weitereKartenebenenRepository.findAllByBenutzerOrderById(
			benutzer).stream()
			.map(WeitereKartenebenenView::new);
	}

	@GetMapping("vordefiniert")
	public List<VordefinierteLayerView> getVordefinierteLayer() {
		return configurationProperties.getVordefinierteLayer().stream()
			.map(config -> new VordefinierteLayerView(config)).collect(Collectors.toList());
	}

	@PostMapping(path = "/save")
	@Transactional
	public Stream<WeitereKartenebenenView> save(Authentication authentication,
		@RequestBody List<@Valid SaveWeitereKartenebeneCommand> commands) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);

		// Entferne weitere Kartenebenen, die nicht in den Commands vorhanden sind
		weitereKartenebenenRepository.findAllByBenutzerOrderById(benutzer)
			.stream().filter(
				bestehenderLayer -> commands.stream()
					.filter(command -> command.getId() != null)
					.noneMatch(command -> command.getId().equals(bestehenderLayer.getId())))
			.forEach(weitereKartenebenenRepository::delete);

		// Erstelle neue Layer oder aktualisiere bestehende Layer
		commands.forEach(command -> {
			if (command.getId() == null) {
				weitereKartenebenenRepository.save(
					new WeitereKartenebene(
						command.getName(),
						command.getUrl(),
						command.getWeitereKartenebeneTyp(),
						command.getDeckkraft(),
						command.getZoomstufe(),
						command.getZindex(),
						command.getFarbe(),
						benutzer,
						command.getQuellangabe(),
						command.getDateiLayerId()));
			} else {
				weitereKartenebenenRepository.findById(command.getId())
					.orElseThrow(EntityNotFoundException::new)
					.update(
						command.getName(),
						command.getUrl(),
						command.getWeitereKartenebeneTyp(),
						command.getDeckkraft(),
						command.getZoomstufe(),
						command.getZindex(),
						command.getFarbe(),
						command.getQuellangabe());
			}
		});

		return weitereKartenebenenRepository.findAllByBenutzerOrderById(benutzer).stream()
			.map(WeitereKartenebenenView::new);
	}
}
