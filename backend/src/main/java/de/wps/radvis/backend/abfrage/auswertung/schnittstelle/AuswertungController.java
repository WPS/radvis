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

package de.wps.radvis.backend.abfrage.auswertung.schnittstelle;

import java.math.BigInteger;
import java.util.Objects;
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.abfrage.auswertung.domain.entity.AuswertungsFilter;
import de.wps.radvis.backend.abfrage.auswertung.domain.repository.AuswertungRepository;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityNotFoundException;
import lombok.NonNull;

@RestController
@RequestMapping("/api/auswertung")
public class AuswertungController {

	private final AuswertungRepository auswertungRepository;

	private final VerwaltungseinheitRepository verwaltungseinheitRepository;

	public AuswertungController(@NonNull AuswertungRepository auswertungRepository,
		VerwaltungseinheitRepository verwaltungseinheitRepository) {
		this.auswertungRepository = auswertungRepository;
		this.verwaltungseinheitRepository = verwaltungseinheitRepository;
	}

	@GetMapping
	public BigInteger getCmAnzahl(
		@RequestParam(required = false) Long gemeindeKreisBezirkId,
		@RequestParam(required = false) Long wahlkreisId,
		@RequestParam(required = false) Set<Netzklasse> netzklassen,
		@RequestParam(required = false) boolean beachteNichtKlassifizierteKanten,
		@RequestParam(required = false) Set<IstStandard> istStandards,
		@RequestParam(required = false) boolean beachteKantenOhneStandards,
		@RequestParam(required = false) Long baulastId,
		@RequestParam(required = false) Long unterhaltId,
		@RequestParam(required = false) Long erhaltId,
		@RequestParam(required = false) BelagArt belagart,
		@RequestParam(required = false) Radverkehrsfuehrung fuehrung

	) {
		Verwaltungseinheit baulast = null;
		if (Objects.nonNull(baulastId)) {
			baulast = verwaltungseinheitRepository.findById(baulastId).orElseThrow(
				EntityNotFoundException::new);
		}

		Verwaltungseinheit unterhalt = null;
		if (Objects.nonNull(unterhaltId)) {
			unterhalt = verwaltungseinheitRepository.findById(unterhaltId).orElseThrow(
				EntityNotFoundException::new);
		}

		Verwaltungseinheit erhalt = null;
		if (Objects.nonNull(erhaltId)) {
			erhalt = verwaltungseinheitRepository.findById(erhaltId).orElseThrow(
				EntityNotFoundException::new);
		}

		AuswertungsFilter auswertungsFilter = new AuswertungsFilter(gemeindeKreisBezirkId, wahlkreisId, netzklassen,
			beachteNichtKlassifizierteKanten, istStandards, beachteKantenOhneStandards, baulast, unterhalt, erhalt,
			belagart, fuehrung);
		return auswertungRepository.getCmAnzahl(auswertungsFilter);
	}
}
