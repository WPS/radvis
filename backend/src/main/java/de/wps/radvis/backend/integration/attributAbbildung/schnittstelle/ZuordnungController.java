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

package de.wps.radvis.backend.integration.attributAbbildung.schnittstelle;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeMergeService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KantenMapping;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.MappedKante;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.MehrdeutigeLinearReferenzierteAttributeException;
import de.wps.radvis.backend.integration.attributAbbildung.schnittstelle.annotation.AuthorizeLeseZuordnung;
import de.wps.radvis.backend.integration.attributAbbildung.schnittstelle.annotation.AuthorizeZuordnung;
import de.wps.radvis.backend.integration.attributAbbildung.schnittstelle.command.ChangeZuordnungCommand;
import de.wps.radvis.backend.integration.attributAbbildung.schnittstelle.command.LoescheZuordnungenCommand;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.schnittstelle.view.KanteEditView;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/zuordnung")
@Validated
@Slf4j
public class ZuordnungController {

	private final NetzService netzService;
	private final KantenAttributeMergeService kantenAttributeMergeService;
	private final KantenMappingService kantenMappingService;

	public ZuordnungController(
		@NonNull NetzService netzService,
		@NonNull KantenAttributeMergeService kantenAttributeMergeService,
		@NonNull KantenMappingService kantenMappingService) {
		this.netzService = netzService;
		this.kantenAttributeMergeService = kantenAttributeMergeService;
		this.kantenMappingService = kantenMappingService;
	}

	@PostMapping(path = "changeZuordnung")
	@AuthorizeZuordnung
	@Transactional
	@WithAuditing(context = AuditingContext.CHANGE_ZUORDNUNG_COMMAND)
	public List<KanteEditView> changeZuordnung(@RequestBody @Valid ChangeZuordnungCommand commands) {
		List<Kante> dlmKanten = netzService.getKanten(commands.getDlmnetzKanteIds());
		List<Kante> radNETZKanten = netzService.getKanten(Set.of(commands.getRadnetzKanteId()));

		Kante radNETZKante;
		if (radNETZKanten.size() == 1) {
			radNETZKante = radNETZKanten.get(0);
		} else {
			throw new EntityNotFoundException(
				"Die RadNETZ-Kante " + commands.getRadnetzKanteId() + " wurde nicht gefunden.");
		}

		dlmKanten.forEach(dlmKante -> {
			dlmKante.ueberschreibeKantenAttribute(kantenAttributeMergeService.mergeKantenAttribute(
				dlmKante.getKantenAttributGruppe().getKantenAttribute(),
				radNETZKante.getKantenAttributGruppe().getKantenAttribute(),
				QuellSystem.RadNETZ));
			dlmKante.ueberschreibeNetzklassen(kantenAttributeMergeService.mergeNetzklassen(
				dlmKante.getKantenAttributGruppe().getNetzklassen(),
				radNETZKante.getKantenAttributGruppe().getNetzklassen(),
				QuellSystem.RadNETZ));
			dlmKante.ueberschreibeIstStandards(kantenAttributeMergeService.mergeIstStandards(
				dlmKante.getKantenAttributGruppe().getIstStandards(),
				radNETZKante.getKantenAttributGruppe().getIstStandards(),
				QuellSystem.RadNETZ));

			dlmKante.ueberschreibeFahrtrichtungAttribute(
				radNETZKante.getFahrtrichtungAttributGruppe().isZweiseitig(),
				kantenAttributeMergeService.mergeFahrtrichtung(
					dlmKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks(),
					radNETZKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks(), QuellSystem.RadNETZ),
				kantenAttributeMergeService.mergeFahrtrichtung(
					dlmKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts(),
					radNETZKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts(), QuellSystem.RadNETZ));

			LinearReferenzierterAbschnitt lineareREferenz = LinearReferenzierterAbschnitt.of(0, 1);

			try {
				dlmKante.ueberschreibeGeschwindgkeitsAttribute(
					kantenAttributeMergeService.mergeGeschwindigkeitAttribute(
						dlmKante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute().get(0),
						lineareREferenz,
						radNETZKante.getGeschwindigkeitAttributGruppe().getImmutableGeschwindigkeitAttribute(),
						QuellSystem.RadNETZ));
			} catch (MehrdeutigeLinearReferenzierteAttributeException e) {
				log.error(e.getMessage(), e);
			}

			try {
				dlmKante.ueberschreibeZustaendigketisAttribute(kantenAttributeMergeService.mergeZustaendigkeitAttribute(
					dlmKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute().get(0),
					lineareREferenz,
					radNETZKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute(),
					QuellSystem.RadNETZ));
			} catch (MehrdeutigeLinearReferenzierteAttributeException e) {
				log.error(e.getMessage(), e);
			}

			try {
				dlmKante.ueberschreibeFuehrungsformAttribute(
					radNETZKante.getFuehrungsformAttributGruppe().isZweiseitig(),
					List.of(kantenAttributeMergeService.mergeFuehrungsformAttribute(
						dlmKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0),
						lineareREferenz,
						radNETZKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks(),
						QuellSystem.RadNETZ)),
					List.of(kantenAttributeMergeService.mergeFuehrungsformAttribute(
						dlmKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0),
						lineareREferenz,
						radNETZKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks(),
						QuellSystem.RadNETZ)));
			} catch (MehrdeutigeLinearReferenzierteAttributeException e) {
				log.error(e.getMessage(), e);
			}

			if (radNETZKante.getFahrtrichtungAttributGruppe().isZweiseitig()
				== radNETZKante.getFuehrungsformAttributGruppe().isZweiseitig()) {
				dlmKante.changeSeitenbezug(radNETZKante.getFuehrungsformAttributGruppe().isZweiseitig());
			} else {
				throw new IllegalStateException(
					"Zweiseitigkeit von Fahrtrichtung und Fuehrungsform darf sich nicht unterscheiden.");
			}
		});

		dlmKanten.forEach(dlmKante -> {
			KantenMapping kantenMapping = kantenMappingService.getOrCreate(dlmKante.getId(), QuellSystem.RadNETZ);
			kantenMapping.getAbgebildeteKanten().clear();
			MappedKante mappedKante = new MappedKante(LinearReferenzierterAbschnitt.of(0., 1.),
				LinearReferenzierterAbschnitt.of(0., 1.), false, radNETZKante.getId());
			kantenMapping.getAbgebildeteKanten().add(mappedKante);
			kantenMappingService.save(kantenMapping);
		});

		return netzService.saveKanten(dlmKanten)
			.stream().map(kante -> new KanteEditView(kante, false)).collect(Collectors.toList());
	}

	@GetMapping(path = "radnetz/{radNETZKanteId}")
	@AuthorizeLeseZuordnung
	public List<Long> getZuordnungRadNETZZuDLM(@PathVariable Long radNETZKanteId) {
		return kantenMappingService.getDlmKanteIds(radNETZKanteId);
	}

	@GetMapping(path = "dlm/{dLMKanteId}")
	@AuthorizeLeseZuordnung
	public List<Long> getZuordnungDLMZuRadNETZ(@PathVariable Long dLMKanteId) {
		return kantenMappingService.findRadNETZKanteIds(dLMKanteId);
	}

	@PostMapping(path = "loescheZuordnung")
	@WithAuditing(context = AuditingContext.LOESCHE_ZUORDNUNGEN_COMMAND)
	@AuthorizeZuordnung
	@Transactional
	@SuppressWarnings("deprecation")
	public void loescheZuordnung(@RequestBody @Valid LoescheZuordnungenCommand loescheZuordnungCommand) {
		kantenMappingService.loescheZuordnungDlm(loescheZuordnungCommand.getDlmnetzKanteId(), QuellSystem.RadNETZ);
	}
}
