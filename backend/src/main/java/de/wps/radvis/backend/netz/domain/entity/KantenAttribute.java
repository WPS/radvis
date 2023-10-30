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

import static org.hamcrest.Matchers.notNullValue;
import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;
import static org.valid4j.Assertive.require;

import java.util.Optional;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;

import org.hibernate.envers.Audited;

import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netz.domain.valueObject.WegeNiveau;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(doNotUseGetters = true)
@ToString
public class KantenAttribute {

	@Setter
	@Enumerated(EnumType.STRING)
	private WegeNiveau wegeNiveau;

	@Getter
	@Enumerated(EnumType.STRING)
	private Beleuchtung beleuchtung;

	@Getter
	@Enumerated(EnumType.STRING)
	private StrassenquerschnittRASt06 strassenquerschnittRASt06;

	@Getter
	@Enumerated(EnumType.STRING)
	private Umfeld umfeld;

	@Setter
	private Laenge laengeManuellErfasst;

	@Setter
	private VerkehrStaerke dtvFussverkehr;

	@Setter
	private VerkehrStaerke dtvRadverkehr;

	@Setter
	private VerkehrStaerke dtvPkw;

	@Setter
	private VerkehrStaerke sv;

	@Setter
	private Kommentar kommentar;

	@Setter
	private StrassenName strassenName;

	@Setter
	private StrassenNummer strassenNummer;

	@Setter
	@ManyToOne
	@Audited(targetAuditMode = NOT_AUDITED)
	private Verwaltungseinheit gemeinde;

	@Getter
	@Enumerated(EnumType.STRING)
	private Status status;

	@Builder(builderMethodName = "privateBuilder", toBuilder = true)
	private KantenAttribute(
		WegeNiveau wegeNiveau, Beleuchtung beleuchtung, Umfeld umfeld,
		StrassenquerschnittRASt06 strassenquerschnittRASt06, Laenge laengeManuellErfasst,
		VerkehrStaerke dtvFussverkehr, VerkehrStaerke dtvRadverkehr,
		VerkehrStaerke dtvPkw, VerkehrStaerke sv,
		Kommentar kommentar, StrassenName strassenName,
		StrassenNummer strassenNummer, Verwaltungseinheit gemeinde, Status status) {
		require(beleuchtung, notNullValue());
		require(umfeld, notNullValue());
		require(strassenquerschnittRASt06, notNullValue());
		require(status, notNullValue());

		this.wegeNiveau = wegeNiveau;
		this.beleuchtung = beleuchtung;
		this.umfeld = umfeld;
		this.strassenquerschnittRASt06 = strassenquerschnittRASt06;
		this.laengeManuellErfasst = laengeManuellErfasst;
		this.dtvFussverkehr = dtvFussverkehr;
		this.dtvRadverkehr = dtvRadverkehr;
		this.dtvPkw = dtvPkw;
		this.sv = sv;
		this.kommentar = kommentar;
		this.strassenName = strassenName;
		this.strassenNummer = strassenNummer;
		this.gemeinde = gemeinde;
		this.status = status;
	}

	private static KantenAttributeBuilder privateBuilder() {
		return new KantenAttributeBuilder();
	}

	public static KantenAttributeBuilder builder() {
		return privateBuilder()
			.beleuchtung(Beleuchtung.UNBEKANNT)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT)
			.umfeld(Umfeld.UNBEKANNT)
			.status(Status.defaultWert());
	}

	public Optional<Laenge> getLaengeManuellErfasst() {
		return Optional.ofNullable(laengeManuellErfasst);
	}

	public Optional<VerkehrStaerke> getDtvFussverkehr() {
		return Optional.ofNullable(dtvFussverkehr);
	}

	public Optional<VerkehrStaerke> getDtvRadverkehr() {
		return Optional.ofNullable(dtvRadverkehr);
	}

	public Optional<WegeNiveau> getWegeNiveau() {
		return Optional.ofNullable(wegeNiveau);
	}

	public Optional<VerkehrStaerke> getDtvPkw() {
		return Optional.ofNullable(dtvPkw);
	}

	public Optional<VerkehrStaerke> getSv() {
		return Optional.ofNullable(sv);
	}

	public Optional<Kommentar> getKommentar() {
		return Optional.ofNullable(kommentar);
	}

	public Optional<StrassenName> getStrassenName() {
		return Optional.ofNullable(strassenName);
	}

	public Optional<StrassenNummer> getStrassenNummer() {
		return Optional.ofNullable(strassenNummer);
	}

	public Optional<Verwaltungseinheit> getGemeinde() {
		return Optional.ofNullable(gemeinde);
	}

	public void setBeleuchtung(Beleuchtung beleuchtung) {
		require(beleuchtung, notNullValue());
		this.beleuchtung = beleuchtung;
	}

	public void setUmfeld(Umfeld umfeld) {
		require(umfeld, notNullValue());
		this.umfeld = umfeld;
	}

	public void setStrassenquerschnittRASt06(StrassenquerschnittRASt06 strassenquerschnittRASt06) {
		require(strassenquerschnittRASt06, notNullValue());
		this.strassenquerschnittRASt06 = strassenquerschnittRASt06;
	}

	public void setStatus(Status status) {
		require(status, notNullValue());
		this.status = status;
	}

	public KantenAttributeBuilder getBuilderMitGleichenAttributen() {
		return KantenAttribute.builder()
			.wegeNiveau(wegeNiveau)
			.beleuchtung(beleuchtung)
			.umfeld(umfeld)
			.strassenquerschnittRASt06(strassenquerschnittRASt06)
			.laengeManuellErfasst(laengeManuellErfasst)
			.dtvFussverkehr(dtvFussverkehr)
			.dtvRadverkehr(dtvRadverkehr)
			.dtvPkw(dtvPkw)
			.sv(sv)
			.kommentar(kommentar)
			.strassenName(strassenName)
			.strassenNummer(strassenNummer)
			.gemeinde(gemeinde)
			.status(status);
	}
}
