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

package de.wps.radvis.backend.furtKreuzung.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;
import static org.valid4j.Assertive.require;

import java.util.Optional;

import org.hibernate.envers.Audited;

import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtKreuzungMusterloesung;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenKommentar;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenTyp;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.LichtsignalAnlageEigenschaften;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Audited
@Entity
@ToString
public class FurtKreuzung extends VersionierteEntity {

	@Getter
	@Embedded
	private FurtKreuzungNetzBezug netzbezug;

	@Getter
	@ManyToOne
	@Audited(targetAuditMode = NOT_AUDITED)
	private Verwaltungseinheit verantwortlicheOrganisation;

	@Getter
	@Enumerated(EnumType.STRING)
	private FurtenKreuzungenTyp typ;

	private FurtKreuzungMusterloesung musterloesung;

	@Getter
	private Boolean radnetzKonform;

	@Getter
	private FurtenKreuzungenKommentar kommentar;

	@Getter
	@Enumerated(EnumType.STRING)
	private KnotenForm knotenForm;

	@Embedded
	private LichtsignalAnlageEigenschaften lichtsignalAnlageEigenschaften;

	public FurtKreuzung(FurtKreuzungNetzBezug netzbezug, Verwaltungseinheit verantwortlicheOrganisation,
		FurtenKreuzungenTyp typ, Boolean radnetzKonform, FurtenKreuzungenKommentar kommentar, KnotenForm knotenForm,
		Optional<FurtKreuzungMusterloesung> musterloesung,
		Optional<LichtsignalAnlageEigenschaften> lichtsignalAnlageEigenschaften) {
		require(netzbezug, notNullValue());
		require(verantwortlicheOrganisation, notNullValue());
		require(typ, notNullValue());
		require(radnetzKonform, notNullValue());
		require(knotenForm, notNullValue());
		require(musterloesung, notNullValue());
		require(musterloesungErlaubt(musterloesung, radnetzKonform), "musterloesungErlaubt");
		require(lichtsignalAnlageEigenschaftenFuerKnotenFormErlaubt(lichtsignalAnlageEigenschaften, knotenForm),
			"lichtsignalAnlageEigenschaftenFuerKnotenFormErlaubt");

		this.netzbezug = netzbezug;
		this.verantwortlicheOrganisation = verantwortlicheOrganisation;
		this.typ = typ;
		this.radnetzKonform = radnetzKonform;
		this.kommentar = kommentar;
		this.knotenForm = knotenForm;
		this.musterloesung = musterloesung.orElse(null);
		this.lichtsignalAnlageEigenschaften = lichtsignalAnlageEigenschaften.orElse(null);
	}

	public void update(FurtKreuzungNetzBezug netzbezug, Verwaltungseinheit verantwortlicheOrganisation,
		FurtenKreuzungenTyp typ, Boolean konform, FurtenKreuzungenKommentar kommentar, KnotenForm knotenForm,
		Optional<FurtKreuzungMusterloesung> musterloesung,
		Optional<LichtsignalAnlageEigenschaften> lichtsignalAnlageEigenschaften) {
		require(netzbezug, notNullValue());
		require(verantwortlicheOrganisation, notNullValue());
		require(typ, notNullValue());
		require(konform, notNullValue());
		require(knotenForm, notNullValue());
		require(musterloesung, notNullValue());
		require(musterloesungErlaubt(musterloesung, konform), "musterloesungErlaubt");
		require(lichtsignalAnlageEigenschaftenFuerKnotenFormErlaubt(lichtsignalAnlageEigenschaften, knotenForm),
			"lichtsignalAnlageEigenschaftenFuerKnotenFormErlaubt");

		this.netzbezug = netzbezug;
		this.verantwortlicheOrganisation = verantwortlicheOrganisation;
		this.typ = typ;
		this.radnetzKonform = konform;
		this.kommentar = kommentar;
		this.knotenForm = knotenForm;
		this.musterloesung = musterloesung.orElse(null);
		this.lichtsignalAnlageEigenschaften = lichtsignalAnlageEigenschaften.orElse(null);
	}

	public Optional<FurtKreuzungMusterloesung> getMusterloesung() {
		return Optional.ofNullable(musterloesung);
	}

	public Optional<LichtsignalAnlageEigenschaften> getLichtsignalAnlageEigenschaften() {
		return Optional.ofNullable(lichtsignalAnlageEigenschaften);
	}

	public static boolean musterloesungErlaubt(Optional<FurtKreuzungMusterloesung> furtKreuzungMusterloesung,
		boolean radnetzKonform) {
		return furtKreuzungMusterloesung.isEmpty() || radnetzKonform;
	}

	public static boolean lichtsignalAnlageEigenschaftenFuerKnotenFormErlaubt(
		Optional<LichtsignalAnlageEigenschaften> lichtsignalAnlageEigenschaften, KnotenForm knotenForm) {
		return (lichtsignalAnlageEigenschaften.isEmpty() && !knotenForm.isLSAKnotenForm()) ||
			(lichtsignalAnlageEigenschaften.isPresent() && knotenForm.isLSAKnotenForm());
	}
}
