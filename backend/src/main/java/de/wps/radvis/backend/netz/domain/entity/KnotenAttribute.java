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

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;

import java.util.Optional;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.Audited;

import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Zustandsbeschreibung;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.Builder;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Builder
public class KnotenAttribute {

	private Kommentar kommentar;

	private Zustandsbeschreibung zustandsbeschreibung;

	@Enumerated(EnumType.STRING)
	private KnotenForm knotenForm;

	@ManyToOne
	@Audited(targetAuditMode = NOT_AUDITED)
	private Verwaltungseinheit gemeinde;

	public KnotenAttribute(Kommentar kommentar,
		Zustandsbeschreibung zustandsbeschreibung, KnotenForm knotenForm,
		Verwaltungseinheit gemeinde) {
		this.kommentar = kommentar;
		this.zustandsbeschreibung = zustandsbeschreibung;
		this.knotenForm = knotenForm;
		this.gemeinde = gemeinde;
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

	public boolean istLeer() {
		return this.kommentar == null && this.knotenForm == null;
	}
}
