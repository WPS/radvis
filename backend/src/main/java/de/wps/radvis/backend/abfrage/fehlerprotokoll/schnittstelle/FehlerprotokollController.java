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

package de.wps.radvis.backend.abfrage.fehlerprotokoll.schnittstelle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.locationtech.jts.geom.Envelope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.abfrage.fehlerprotokoll.domain.service.FehlerprotokollAbfrageService;
import de.wps.radvis.backend.abfrage.fehlerprotokoll.schnittstelle.view.FehlerprotokollTypView;
import de.wps.radvis.backend.abfrage.fehlerprotokoll.schnittstelle.view.FehlerprotokollView;
import de.wps.radvis.backend.common.domain.FeatureTogglz;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.common.domain.valueObject.FehlerprotokollTyp;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.RadvisViewController;
import de.wps.radvis.backend.manuellerimport.common.domain.entity.ManuellerImportFehler;
import de.wps.radvis.backend.manuellerimport.common.domain.repository.ManuellerImportFehlerRepository;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportTyp;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

@RestController
@RadvisViewController
@RequestMapping("/api/fehlerprotokoll")
public class FehlerprotokollController {

	private final FehlerprotokollAbfrageService fehlerprotokollAbfrageService;
	private final ManuellerImportFehlerRepository manuellerImportFehlerRepository;
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;

	public FehlerprotokollController(
		FehlerprotokollAbfrageService fehlerprotokollAbfrageService,
		ManuellerImportFehlerRepository manuellerImportFehlerRepository,
		VerwaltungseinheitResolver verwaltungseinheitResolver) {
		this.fehlerprotokollAbfrageService = fehlerprotokollAbfrageService;
		this.manuellerImportFehlerRepository = manuellerImportFehlerRepository;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
	}

	@GetMapping("/types")
	public List<FehlerprotokollTypView> getFehlerprotokollTypen() {
		return Arrays.asList(FehlerprotokollTyp.values()).stream().map(f -> new FehlerprotokollTypView(f))
			.collect(Collectors.toList());
	}

	@GetMapping("/list")
	public Stream<FehlerprotokollView> getFehlerprotokolle(
		@ModelAttribute("sichtbereich") Envelope sichtbereich,
		@RequestParam List<FehlerprotokollTyp> selectedTypen) {
		if (!FeatureTogglz.FEHLERPROTOKOLL.isActive()) {
			return Stream.empty();
		}

		List<FehlerprotokollEintrag> alleFehlerprotokolleFuerTypen = fehlerprotokollAbfrageService
			.getAlleFehlerprotokolleFuerTypenInBereich(
				selectedTypen, sichtbereich);
		return alleFehlerprotokolleFuerTypen.stream()
			.map(FehlerprotokollView::new);
	}

	@GetMapping("/manuellerImport")
	public Stream<FehlerprotokollView> getFehlerprotokolleFromManuellerImport(
		@ModelAttribute("sichtbereich") Envelope sichtbereich,
		@RequestParam Long organisation,
		@RequestParam boolean includeNetzklassenImport,
		@RequestParam boolean includeAttributeImport) {
		List<ManuellerImportFehler> result = new ArrayList<>();
		Verwaltungseinheit resolvedOrganisation = verwaltungseinheitResolver.resolve(organisation);
		if (includeNetzklassenImport) {
			List<ManuellerImportFehler> allLatestByOrganisationAndType = manuellerImportFehlerRepository
				.getAllLatestByOrganisationAndTypeInBereich(
					resolvedOrganisation,
					ImportTyp.NETZKLASSE_ZUWEISEN,
					EnvelopeAdapter.toPolygon(sichtbereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()));
			result.addAll(
				allLatestByOrganisationAndType);
		}
		if (includeAttributeImport) {
			List<ManuellerImportFehler> allLatestByOrganisationAndType = manuellerImportFehlerRepository
				.getAllLatestByOrganisationAndTypeInBereich(
					resolvedOrganisation,
					ImportTyp.ATTRIBUTE_UEBERNEHMEN,
					EnvelopeAdapter.toPolygon(sichtbereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()));
			result.addAll(allLatestByOrganisationAndType);
		}
		return result.stream().map(FehlerprotokollView::new);
	}
}
