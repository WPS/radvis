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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtKreuzungMusterloesung;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenKommentar;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenTyp;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.LichtsignalAnlageEigenschaften;
import de.wps.radvis.backend.netz.domain.entity.AbstractEntityWithNetzbezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.valueObject.Bauwerksmangel;
import de.wps.radvis.backend.netz.domain.valueObject.BauwerksmangelArt;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.QuerungshilfeDetails;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Audited
@Entity
@ToString
public class FurtKreuzung extends AbstractEntityWithNetzbezug {

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

	@Enumerated(EnumType.STRING)
	private QuerungshilfeDetails querungshilfeDetails;

	@Enumerated(EnumType.STRING)
	private Bauwerksmangel bauwerksmangel;

	@Type(value = ListArrayType.class, parameters = {
		@Parameter(name = ListArrayType.SQL_ARRAY_TYPE, value = "text")
	})
	@Column(name = "bauwerksmangel_art", columnDefinition = "text[]")
	private Set<BauwerksmangelArt> bauwerksmangelArt;

	@Embedded
	private LichtsignalAnlageEigenschaften lichtsignalAnlageEigenschaften;

	public FurtKreuzung(FurtKreuzungNetzBezug netzbezug, Verwaltungseinheit verantwortlicheOrganisation,
		FurtenKreuzungenTyp typ, Boolean radnetzKonform, FurtenKreuzungenKommentar kommentar, KnotenForm knotenForm,
		Optional<FurtKreuzungMusterloesung> musterloesung,
		Optional<LichtsignalAnlageEigenschaften> lichtsignalAnlageEigenschaften,
		QuerungshilfeDetails querungshilfeDetails,
		Bauwerksmangel bauwerksmangel, Set<BauwerksmangelArt> bauwerksmangelArt) {
		this(null, null, netzbezug, verantwortlicheOrganisation, typ, radnetzKonform, kommentar, knotenForm,
			musterloesung, lichtsignalAnlageEigenschaften, querungshilfeDetails, bauwerksmangel, bauwerksmangelArt);
	}

	@Builder
	private FurtKreuzung(Long id, Long version, FurtKreuzungNetzBezug netzbezug,
		Verwaltungseinheit verantwortlicheOrganisation, FurtenKreuzungenTyp typ, Boolean radnetzKonform,
		FurtenKreuzungenKommentar kommentar, KnotenForm knotenForm, Optional<FurtKreuzungMusterloesung> musterloesung,
		Optional<LichtsignalAnlageEigenschaften> lichtsignalAnlageEigenschaften,
		QuerungshilfeDetails querungshilfeDetails,
		Bauwerksmangel bauwerksmangel, Set<BauwerksmangelArt> bauwerksmangelArt) {
		super(id, version);
		require(netzbezug, notNullValue());
		require(verantwortlicheOrganisation, notNullValue());
		require(typ, notNullValue());
		require(radnetzKonform, notNullValue());
		require(knotenForm, notNullValue());
		require(musterloesung, notNullValue());
		require(musterloesungErlaubt(musterloesung, radnetzKonform), "musterloesungErlaubt");
		require(lichtsignalAnlageEigenschaftenFuerKnotenFormErlaubt(lichtsignalAnlageEigenschaften, knotenForm),
			"lichtsignalAnlageEigenschaftenFuerKnotenFormErlaubt");
		require(isBauwerksmangelValid(bauwerksmangel, bauwerksmangelArt, knotenForm));
		require(isQuerungshilfeDetailsValid(querungshilfeDetails, knotenForm));

		this.netzbezug = netzbezug;
		this.verantwortlicheOrganisation = verantwortlicheOrganisation;
		this.typ = typ;
		this.radnetzKonform = radnetzKonform;
		this.kommentar = kommentar;
		this.knotenForm = knotenForm;
		this.musterloesung = musterloesung.orElse(null);
		this.lichtsignalAnlageEigenschaften = lichtsignalAnlageEigenschaften.orElse(null);
		this.querungshilfeDetails = querungshilfeDetails;
		this.bauwerksmangel = bauwerksmangel;
		this.bauwerksmangelArt = bauwerksmangelArt;
	}

	public void update(FurtKreuzungNetzBezug netzbezug, Verwaltungseinheit verantwortlicheOrganisation,
		FurtenKreuzungenTyp typ, Boolean konform, FurtenKreuzungenKommentar kommentar, KnotenForm knotenForm,
		Optional<FurtKreuzungMusterloesung> musterloesung,
		Optional<LichtsignalAnlageEigenschaften> lichtsignalAnlageEigenschaften,
		QuerungshilfeDetails querungshilfeDetails,
		Bauwerksmangel bauwerksmangel, Set<BauwerksmangelArt> bauwerksmangelArt) {
		require(netzbezug, notNullValue());
		require(verantwortlicheOrganisation, notNullValue());
		require(typ, notNullValue());
		require(konform, notNullValue());
		require(knotenForm, notNullValue());
		require(musterloesung, notNullValue());
		require(musterloesungErlaubt(musterloesung, konform), "musterloesungErlaubt");
		require(lichtsignalAnlageEigenschaftenFuerKnotenFormErlaubt(lichtsignalAnlageEigenschaften, knotenForm),
			"lichtsignalAnlageEigenschaftenFuerKnotenFormErlaubt");
		require(isBauwerksmangelValid(bauwerksmangel, bauwerksmangelArt, knotenForm));
		require(isQuerungshilfeDetailsValid(querungshilfeDetails, knotenForm));

		this.netzbezug = netzbezug;
		this.verantwortlicheOrganisation = verantwortlicheOrganisation;
		this.typ = typ;
		this.radnetzKonform = konform;
		this.kommentar = kommentar;
		this.knotenForm = knotenForm;
		this.musterloesung = musterloesung.orElse(null);
		this.lichtsignalAnlageEigenschaften = lichtsignalAnlageEigenschaften.orElse(null);
		this.querungshilfeDetails = querungshilfeDetails;
		this.bauwerksmangel = bauwerksmangel;
		this.bauwerksmangelArt = bauwerksmangelArt;
	}

	public Optional<FurtKreuzungMusterloesung> getMusterloesung() {
		return Optional.ofNullable(musterloesung);
	}

	public Optional<LichtsignalAnlageEigenschaften> getLichtsignalAnlageEigenschaften() {
		return Optional.ofNullable(lichtsignalAnlageEigenschaften);
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

	public static boolean musterloesungErlaubt(Optional<FurtKreuzungMusterloesung> furtKreuzungMusterloesung,
		boolean radnetzKonform) {
		return furtKreuzungMusterloesung.isEmpty() || radnetzKonform;
	}

	public static boolean lichtsignalAnlageEigenschaftenFuerKnotenFormErlaubt(
		Optional<LichtsignalAnlageEigenschaften> lichtsignalAnlageEigenschaften, KnotenForm knotenForm) {
		return (lichtsignalAnlageEigenschaften.isEmpty() && !knotenForm.isLSAKnotenForm()) ||
			(lichtsignalAnlageEigenschaften.isPresent() && knotenForm.isLSAKnotenForm());
	}

	public void removeKanteFromNetzbezug(Collection<Long> kantenIds) {
		require(kantenIds, notNullValue());
		netzbezug = netzbezug.withoutKanten(new HashSet<>(kantenIds));
	}

	public void removeKnotenFromNetzbezug(Collection<Long> knotenIds) {
		require(knotenIds, notNullValue());
		netzbezug = netzbezug.withoutKnoten(new HashSet<>(knotenIds));
	}

	public void ersetzeKanteInNetzbezug(Kante zuErsetzendeKante, Set<Kante> zuErsetzenDurch,
		double erlaubteAbweichung) {
		require(zuErsetzenDurch, notNullValue());
		require(!zuErsetzenDurch.isEmpty());

		netzbezug = netzbezug.withKanteErsetzt(zuErsetzendeKante, zuErsetzenDurch, erlaubteAbweichung);
	}

	/**
	 * Ersetzt Knoten anhand übergebener Abbildung
	 * 
	 * @param ersatzKnoten:
	 *     ID des zu ersetzenden Knoten -> Ersatz-Knoten
	 */
	public void ersetzeKnotenInNetzbezug(Map<Long, Knoten> ersatzKnoten) {
		netzbezug = netzbezug.withKnotenErsetzt(ersatzKnoten);
	}

	public static boolean isBauwerksmangelValid(Bauwerksmangel bauwerksmangel,
		Set<BauwerksmangelArt> bauwerksmangelArt, KnotenForm knotenForm) {
		require(knotenForm, notNullValue());
		if (Bauwerksmangel.isRequiredForKnotenform(knotenForm)) {
			if (bauwerksmangel == null) {
				return false;
			}
		} else {
			return bauwerksmangel == null && bauwerksmangelArt == null;
		}

		if (BauwerksmangelArt.isRequiredForBauwerksmangel(bauwerksmangel)) {
			if (bauwerksmangelArt == null || bauwerksmangelArt.isEmpty()) {
				return false;
			}

			return bauwerksmangelArt.stream().allMatch(ba -> ba.isValidForKnotenform(knotenForm));
		} else {
			return bauwerksmangelArt == null;
		}
	}

	public static boolean isQuerungshilfeDetailsValid(QuerungshilfeDetails querungshilfe,
		KnotenForm knotenform) {
		require(knotenform, notNullValue());

		if (QuerungshilfeDetails.isRequiredForKnotenform(knotenform)) {
			return querungshilfe != null && querungshilfe.isValidForKnotenform(knotenform);
		} else {
			return querungshilfe == null;
		}
	}

}
