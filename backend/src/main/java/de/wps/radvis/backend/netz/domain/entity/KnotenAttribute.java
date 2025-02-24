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

package de.wps.radvis.backend.netz.domain.entity;

import java.util.Optional;
import java.util.Set;

import de.wps.radvis.backend.netz.domain.valueObject.Bauwerksmangel;
import de.wps.radvis.backend.netz.domain.valueObject.BauwerksmangelArt;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.QuerungshilfeDetails;
import de.wps.radvis.backend.netz.domain.valueObject.Zustandsbeschreibung;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Builder
public class KnotenAttribute {

	private final Kommentar kommentar;

	private final Zustandsbeschreibung zustandsbeschreibung;

	private final KnotenForm knotenForm;

	private final Verwaltungseinheit gemeinde;

	private final QuerungshilfeDetails querungshilfeDetails;

	private final Bauwerksmangel bauwerksmangel;

	private final Set<BauwerksmangelArt> bauwerksmangelArt;

	public KnotenAttribute(Kommentar kommentar,
		Zustandsbeschreibung zustandsbeschreibung, KnotenForm knotenForm,
		Verwaltungseinheit gemeinde, QuerungshilfeDetails querungshilfeDetails,
		Bauwerksmangel bauwerksmangel, Set<BauwerksmangelArt> bauwerksmangelArt) {
		this.kommentar = kommentar;
		this.zustandsbeschreibung = zustandsbeschreibung;
		this.knotenForm = knotenForm;
		this.gemeinde = gemeinde;
		this.querungshilfeDetails = querungshilfeDetails;
		this.bauwerksmangel = bauwerksmangel;
		this.bauwerksmangelArt = bauwerksmangelArt;
	}

	public Optional<Kommentar> getKommentar() {
		return Optional.ofNullable(kommentar);
	}

	public Optional<Zustandsbeschreibung> getZustandsbeschreibung() {
		return Optional.ofNullable(zustandsbeschreibung);
	}

	public Optional<KnotenForm> getKnotenForm() {
		return Optional.ofNullable(knotenForm);
	}

	public Optional<Verwaltungseinheit> getGemeinde() {
		return Optional.ofNullable(gemeinde);
	}

	public Optional<QuerungshilfeDetails> getQuerungshilfeDetails() {
		return Optional.ofNullable(querungshilfeDetails);
	}

	public Optional<Bauwerksmangel> getBauwerksmangel() {
		return Optional.ofNullable(bauwerksmangel);
	}

	public Optional<Set<BauwerksmangelArt>> getBauwerksmangelArt() {
		if (bauwerksmangelArt == null || bauwerksmangelArt.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(bauwerksmangelArt);
	}

	public boolean istLeer() {
		return this.kommentar == null && this.knotenForm == null;
	}
}
