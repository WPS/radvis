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

package de.wps.radvis.backend.abfrage.auswertung.domain.entity;

import static org.valid4j.Assertive.require;

import java.util.Objects;
import java.util.Set;

import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuswertungsFilter {

	private Long gemeindeKreisBezirkId;

	private Long wahlkreisId;

	Set<Netzklasse> netzklassen;

	boolean beachteNichtKlassifizierteKanten;

	Set<IstStandard> istStandards;

	boolean beachteKantenOhneStandards;

	private Verwaltungseinheit baulast;

	private Verwaltungseinheit unterhalt;

	private Verwaltungseinheit erhalt;

	private BelagArt belagArt;

	private Radverkehrsfuehrung radverkehrsfuehrung;

	public AuswertungsFilter(Long gemeindeKreisBezirkId, Long wahlkreisId, Set<Netzklasse> netzklassen,
		boolean beachteNichtKlassifizierteKanten, Set<IstStandard> istStandards, boolean beachteKantenOhneStandards,
		Verwaltungseinheit baulast, Verwaltungseinheit unterhalt, Verwaltungseinheit erhalt, BelagArt belagArt,
		Radverkehrsfuehrung radverkehrsfuehrung) {
		require(Objects.isNull(gemeindeKreisBezirkId) || Objects.isNull(wahlkreisId),
			"Es dürfen nicht beide IDs gesetzt sein.");

		this.gemeindeKreisBezirkId = gemeindeKreisBezirkId;
		this.wahlkreisId = wahlkreisId;
		this.netzklassen = netzklassen;
		this.beachteNichtKlassifizierteKanten = beachteNichtKlassifizierteKanten;
		this.istStandards = istStandards;
		this.beachteKantenOhneStandards = beachteKantenOhneStandards;
		this.baulast = baulast;
		this.unterhalt = unterhalt;
		this.erhalt = erhalt;
		this.belagArt = belagArt;
		this.radverkehrsfuehrung = radverkehrsfuehrung;
	}

	public boolean isWahlkreis() {
		return Objects.nonNull(wahlkreisId);
	}

	public boolean isGemeindeKreisBezirk() {
		return Objects.nonNull(gemeindeKreisBezirkId);
	}
}
