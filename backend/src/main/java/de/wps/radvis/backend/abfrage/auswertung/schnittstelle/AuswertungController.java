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
import java.util.Set;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.abfrage.auswertung.domain.entity.AuswertungsFilter;
import de.wps.radvis.backend.abfrage.auswertung.domain.repository.AuswertungRepository;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import lombok.NonNull;

@RestController
@RequestMapping("/api/auswertung")
public class AuswertungController {

	private final AuswertungRepository auswertungRepository;

	public AuswertungController(@NonNull AuswertungRepository auswertungRepository) {
		this.auswertungRepository = auswertungRepository;
	}

	@GetMapping
	public BigInteger getCmAnzahl(
		@RequestParam(required = false) Long gemeindeKreisBezirkId,
		@RequestParam(required = false) Set<Netzklasse> netzklassen,
		@RequestParam(required = false) boolean beachteNichtKlassifizierteKanten,
		@RequestParam(required = false) Set<IstStandard> istStandards,
		@RequestParam(required = false) boolean beachteKantenOhneStandards,
		@RequestParam(required = false) Long baulastId,
		@RequestParam(required = false) Long unterhaltId,
		@RequestParam(required = false) Long erhaltId
	) {
		AuswertungsFilter auswertungsFilter = new AuswertungsFilter(gemeindeKreisBezirkId, netzklassen,
			beachteNichtKlassifizierteKanten, istStandards, beachteKantenOhneStandards, baulastId, unterhaltId,
			erhaltId);
		return auswertungRepository.getCmAnzahl(auswertungsFilter);
	}
}
