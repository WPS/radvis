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

package de.wps.radvis.backend.abfrage.signatur.schnittstelle;

import static org.valid4j.Assertive.require;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.validation.constraints.Pattern;

import org.geojson.FeatureCollection;
import org.locationtech.jts.geom.Envelope;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.NetzausschnittService;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.NetzklassenStreckenSignaturView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteFuehrungsformAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteGeschwindigkeitAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteZustaendigkeitAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.schnittstelle.NetzToGeoJsonConverter;
import de.wps.radvis.backend.abfrage.signatur.domain.AttributGruppeNichtBestimmbarException;
import de.wps.radvis.backend.abfrage.signatur.domain.AttributGruppenService;
import de.wps.radvis.backend.abfrage.signatur.domain.SignaturService;
import de.wps.radvis.backend.abfrage.signatur.domain.valueObject.SignaturTyp;
import de.wps.radvis.backend.common.schnittstelle.RadvisViewController;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.AttributGruppe;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import lombok.NonNull;

@RestController
@RequestMapping("/api/signaturen")
@RadvisViewController
@Validated
public class SignaturController {

	private final NetzToGeoJsonConverter netzToGeoJsonConverter;
	private final NetzService netzService;
	private final NetzausschnittService netzausschnittService;
	private final SignaturService signaturService;

	private static final String DATEINAME_PATTERN = "[A-Za-zäöüÄÖÜß\\d_\\-\\s]+";

	public SignaturController(@NonNull NetzToGeoJsonConverter netzToGeoJsonConverter, @NonNull NetzService netzService,
		@NonNull NetzausschnittService netzausschnittService, @NonNull SignaturService signaturService) {
		this.netzToGeoJsonConverter = netzToGeoJsonConverter;
		this.netzService = netzService;
		this.netzausschnittService = netzausschnittService;
		this.signaturService = signaturService;
	}

	@GetMapping("geojson")
	public FeatureCollection getSignaturForAttributeGeoJson(@ModelAttribute("sichtbereich") Envelope sichtbereich,
		@RequestParam List<String> attribute,
		@RequestParam("netzklasseFilter") Set<NetzklasseFilter> netzklasseFilterQueryParams) {
		AttributGruppe attributGruppe;
		try {
			attributGruppe = AttributGruppenService.getAttributGruppe(attribute);
		} catch (AttributGruppeNichtBestimmbarException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}

		switch (attributGruppe.toString()) {
		case "KANTENATTRIBUTE":
			Set<Kante> kanten = this.netzService
				.getKantenInBereichMitNetzklassen(sichtbereich, netzklasseFilterQueryParams, false);
			return netzToGeoJsonConverter.convertKantenAttribute(kanten, attribute);
		case "ZUSTAENDIGKEITATTRIBUTE":
			Set<KanteZustaendigkeitAttributeView> kanteZustaendigkeitAttributeViews = this.netzausschnittService
				.findKanteZustaendigkeitAttributeViewInAuschnitt(sichtbereich, netzklasseFilterQueryParams, false);
			return netzToGeoJsonConverter.convertZustaendigkeitAttributeView((kanteZustaendigkeitAttributeViews),
				attribute);
		case "GESCHWINDIGKEITATTRIBUTE":
			Set<KanteGeschwindigkeitAttributeView> kanteGeschwindigkeitAttributeViews = this.netzausschnittService
				.findKanteGeschwindigkeitAttributeViewInAuschnitt(sichtbereich, netzklasseFilterQueryParams, false);
			return netzToGeoJsonConverter.convertGeschwindigkeitattribute(kanteGeschwindigkeitAttributeViews,
				attribute);
		case "FUEHRUNGSFORMATTRIBUTE":
			Set<KanteFuehrungsformAttributeView> kanteFuehrungsformAttributeViews = this.netzausschnittService
				.findKanteFuehrungsformAttributeViewInAuschnitt(sichtbereich, netzklasseFilterQueryParams, false);
			return netzToGeoJsonConverter.convertFuehrungsformAttribute(kanteFuehrungsformAttributeViews, attribute);
		default:
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("geojsonMitDLM")
	public FeatureCollection getSignaturForAttributeMitDLMIstRadNETZGeoJson(
		@ModelAttribute("sichtbereich") Envelope sichtbereich,
		@RequestParam List<String> attribute) {
		AttributGruppe attributGruppe;
		try {
			attributGruppe = AttributGruppenService.getAttributGruppe(attribute);
		} catch (AttributGruppeNichtBestimmbarException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}

		switch (attributGruppe.toString()) {
		case "KANTENATTRIBUTE":
			Set<Kante> kanten = this.netzService
				.getKantenInBereichMitNetzklassen(sichtbereich, Collections.emptySet(), true);
			return netzToGeoJsonConverter.convertKantenAttribute(kanten, attribute);
		case "ZUSTAENDIGKEITATTRIBUTE":
			Set<KanteZustaendigkeitAttributeView> kanteZustaendigkeitAttributeViews = this.netzausschnittService
				.findKanteZustaendigkeitAttributeViewInAuschnitt(sichtbereich, Collections.emptySet(), true);
			return netzToGeoJsonConverter.convertZustaendigkeitAttributeView((kanteZustaendigkeitAttributeViews),
				attribute);
		case "GESCHWINDIGKEITATTRIBUTE":
			Set<KanteGeschwindigkeitAttributeView> kanteGeschwindigkeitAttributeViews = this.netzausschnittService
				.findKanteGeschwindigkeitAttributeViewInAuschnitt(sichtbereich, Collections.emptySet(), true);
			return netzToGeoJsonConverter.convertGeschwindigkeitattribute(kanteGeschwindigkeitAttributeViews,
				attribute);
		case "FUEHRUNGSFORMATTRIBUTE":
			Set<KanteFuehrungsformAttributeView> kanteFuehrungsformAttributeViews = this.netzausschnittService
				.findKanteFuehrungsformAttributeViewInAuschnitt(sichtbereich, Collections.emptySet(), true);
			return netzToGeoJsonConverter.convertFuehrungsformAttribute(kanteFuehrungsformAttributeViews, attribute);
		default:
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("geojson/netzklassen/strecken")
	public FeatureCollection getSignaturForNetzklassenGeoJson(@RequestParam List<String> attribute,
		@RequestParam("netzklasseFilter") Set<NetzklasseFilter> netzklasseFilterQueryParams) {
		AttributGruppe attributGruppe;
		try {
			attributGruppe = AttributGruppenService.getAttributGruppe(attribute);
		} catch (AttributGruppeNichtBestimmbarException e) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
		}

		require(attributGruppe.equals(AttributGruppe.KANTENATTRIBUTE));
		require(
			netzklasseFilterQueryParams.size() == 1 && netzklasseFilterQueryParams.contains(NetzklasseFilter.RADNETZ));
		require(!attribute.isEmpty());
		require(attribute.get(0).equals("hoechsteNetzklasse"));

		List<NetzklassenStreckenSignaturView> netzklassenSignaturViews = netzausschnittService
			.getNetzklassenSignaturView();
		return netzToGeoJsonConverter.convertNetzklassenSignaturViews(netzklassenSignaturViews);
	}

	@GetMapping
	public List<SignaturView> getVerfuegbareSignaturen() {
		Stream<SignaturView> signaturen = signaturService.getAvailableSignaturen().stream()
			.map(signatur -> new SignaturView(signatur, SignaturTyp.NETZ));
		Stream<SignaturView> massnahmenSignaturen = signaturService.getAvailableMassnahmenSignaturen().stream()
			.map(signatur -> new SignaturView(signatur, SignaturTyp.MASSNAHME));
		return Stream.concat(signaturen, massnahmenSignaturen).collect(Collectors.toList());
	}

	@GetMapping("/style/{signaturTyp}/{signatur}")
	public String getStylingForSignatur(@PathVariable SignaturTyp signaturTyp,
		@PathVariable @Pattern(regexp = DATEINAME_PATTERN) String signatur) {
		try {
			return this.signaturService.readSldFileFromSignaturName(signaturTyp, signatur);
		} catch (IOException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
		}
	}
}
