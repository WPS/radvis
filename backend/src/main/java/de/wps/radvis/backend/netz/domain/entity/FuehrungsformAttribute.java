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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.valid4j.Assertive.require;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.valueObject.Absenkung;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Beschilderung;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Schadenart;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class FuehrungsformAttribute extends LinearReferenzierteAttribute {
	@Getter
	@Enumerated(EnumType.STRING)
	private BelagArt belagArt;

	@Getter
	@Enumerated(EnumType.STRING)
	private Oberflaechenbeschaffenheit oberflaechenbeschaffenheit;

	@Getter
	@Enumerated(EnumType.STRING)
	private Bordstein bordstein;

	@Getter
	@Enumerated(EnumType.STRING)
	private Radverkehrsfuehrung radverkehrsfuehrung;

	@Getter
	@Enumerated(EnumType.STRING)
	private Benutzungspflicht benutzungspflicht;

	@Getter
	@Enumerated(EnumType.STRING)
	private KfzParkenTyp parkenTyp;

	@Getter
	@Enumerated(EnumType.STRING)
	private KfzParkenForm parkenForm;

	@Getter
	@Enumerated(EnumType.STRING)
	private Beschilderung beschilderung;

	@Getter
	@Type(value = ListArrayType.class, parameters = {
		@Parameter(name = ListArrayType.SQL_ARRAY_TYPE, value = "text")
	})
	@Column(name = "schaeden", columnDefinition = "text[]")
	private Set<Schadenart> schaeden;

	@Getter
	@Enumerated(EnumType.STRING)
	private Absenkung absenkung;

	private Laenge breite;

	private Laenge trennstreifenBreiteRechts;

	private Laenge trennstreifenBreiteLinks;

	@Enumerated(EnumType.STRING)
	private TrennungZu trennstreifenTrennungZuRechts;

	@Enumerated(EnumType.STRING)
	private TrennungZu trennstreifenTrennungZuLinks;

	@Enumerated(EnumType.STRING)
	private TrennstreifenForm trennstreifenFormRechts;

	@Enumerated(EnumType.STRING)
	private TrennstreifenForm trennstreifenFormLinks;

	@Builder(builderMethodName = "privateBuilder", toBuilder = true)
	public FuehrungsformAttribute(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		BelagArt belagArt,
		Oberflaechenbeschaffenheit oberflaechenbeschaffenheit,
		Bordstein bordstein,
		Radverkehrsfuehrung radverkehrsfuehrung,
		KfzParkenTyp parkenTyp,
		KfzParkenForm parkenForm,
		Laenge breite,
		Benutzungspflicht benutzungspflicht,
		Beschilderung beschilderung,
		Set<Schadenart> schaeden,
		Absenkung absenkung,
		Laenge trennstreifenBreiteRechts,
		Laenge trennstreifenBreiteLinks,
		TrennungZu trennstreifenTrennungZuRechts,
		TrennungZu trennstreifenTrennungZuLinks,
		TrennstreifenForm trennstreifenFormRechts,
		TrennstreifenForm trennstreifenFormLinks) {
		super(linearReferenzierterAbschnitt);

		require(belagArt, notNullValue());
		require(oberflaechenbeschaffenheit, notNullValue());
		require(bordstein, notNullValue());
		require(radverkehrsfuehrung, notNullValue());
		require(parkenTyp, notNullValue());
		require(parkenForm, notNullValue());
		require(benutzungspflicht, notNullValue());
		require(beschilderung, notNullValue());
		require(beschilderung.isValidForRadverkehrsfuehrung(radverkehrsfuehrung));
		require(schaeden, notNullValue());
		require(absenkung, notNullValue());

		assertTrennstreifenCorrectness(
			radverkehrsfuehrung,
			trennstreifenFormRechts,
			trennstreifenBreiteRechts,
			trennstreifenTrennungZuRechts);
		assertTrennstreifenCorrectness(
			radverkehrsfuehrung,
			trennstreifenFormLinks,
			trennstreifenBreiteLinks,
			trennstreifenTrennungZuLinks);

		this.belagArt = belagArt;
		this.oberflaechenbeschaffenheit = oberflaechenbeschaffenheit;
		this.bordstein = bordstein;
		this.radverkehrsfuehrung = radverkehrsfuehrung;
		this.parkenTyp = parkenTyp;
		this.parkenForm = parkenForm;
		this.breite = breite;
		this.benutzungspflicht = benutzungspflicht;
		this.beschilderung = beschilderung;
		this.absenkung = absenkung;
		this.schaeden = schaeden;
		this.trennstreifenBreiteRechts = trennstreifenBreiteRechts;
		this.trennstreifenBreiteLinks = trennstreifenBreiteLinks;
		this.trennstreifenTrennungZuRechts = trennstreifenTrennungZuRechts;
		this.trennstreifenTrennungZuLinks = trennstreifenTrennungZuLinks;
		this.trennstreifenFormRechts = trennstreifenFormRechts;
		this.trennstreifenFormLinks = trennstreifenFormLinks;
	}

	public static FuehrungsformAttributeBuilder builder() {
		return privateBuilder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.belagArt(BelagArt.UNBEKANNT)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.UNBEKANNT)
			.bordstein(Bordstein.UNBEKANNT)
			.radverkehrsfuehrung(Radverkehrsfuehrung.UNBEKANNT)
			.parkenForm(KfzParkenForm.UNBEKANNT)
			.parkenTyp(KfzParkenTyp.UNBEKANNT)
			.schaeden(Collections.emptySet())
			.absenkung(Absenkung.UNBEKANNT)
			.breite(null)
			.benutzungspflicht(Benutzungspflicht.UNBEKANNT)
			.beschilderung(Beschilderung.UNBEKANNT)
			.trennstreifenBreiteRechts(null)
			.trennstreifenBreiteLinks(null)
			.trennstreifenTrennungZuRechts(null)
			.trennstreifenTrennungZuLinks(null)
			.trennstreifenFormRechts(null)
			.trennstreifenFormLinks(null);
	}

	public Optional<Laenge> getBreite() {
		return Optional.ofNullable(breite);
	}

	@Override
	public FuehrungsformAttribute withLinearReferenzierterAbschnitt(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		return new FuehrungsformAttribute(
			linearReferenzierterAbschnitt,
			this.belagArt,
			this.oberflaechenbeschaffenheit,
			this.bordstein,
			this.radverkehrsfuehrung,
			this.parkenTyp,
			this.parkenForm,
			this.breite,
			this.benutzungspflicht,
			this.beschilderung,
			this.schaeden,
			this.absenkung,
			this.trennstreifenBreiteRechts,
			this.trennstreifenBreiteLinks,
			this.trennstreifenTrennungZuRechts,
			this.trennstreifenTrennungZuLinks,
			this.trennstreifenFormRechts,
			this.trennstreifenFormLinks);
	}

	@Override
	public LinearReferenzierteAttribute withDefaultValuesAndLineareReferenz(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		return FuehrungsformAttribute.builder().linearReferenzierterAbschnitt(linearReferenzierterAbschnitt).build();
	}

	@Override
	public FuehrungsformAttribute copyWithSameValues() {
		return new FuehrungsformAttribute(this.linearReferenzierterAbschnitt,
			this.belagArt,
			this.oberflaechenbeschaffenheit,
			this.bordstein,
			this.radverkehrsfuehrung,
			this.parkenTyp,
			this.parkenForm,
			this.breite,
			this.benutzungspflicht,
			this.beschilderung,
			this.schaeden,
			this.absenkung,
			this.trennstreifenBreiteRechts,
			this.trennstreifenBreiteLinks,
			this.trennstreifenTrennungZuRechts,
			this.trennstreifenTrennungZuLinks,
			this.trennstreifenFormRechts,
			this.trennstreifenFormLinks);
	}

	@Override
	protected Optional<FuehrungsformAttribute> union(LinearReferenzierteAttribute other) {
		if (!(other instanceof FuehrungsformAttribute)) {
			return Optional.empty();
		}

		Optional<LinearReferenzierterAbschnitt> union = linearReferenzierterAbschnitt.union(
			other.linearReferenzierterAbschnitt);

		if (union.isEmpty()) {
			return Optional.empty();
		}

		if (!sindAttributeGleich((FuehrungsformAttribute) other)) {
			return Optional.empty();
		}

		return Optional.of(new FuehrungsformAttribute(union.get(),
			this.belagArt,
			this.oberflaechenbeschaffenheit,
			this.bordstein,
			this.radverkehrsfuehrung,
			this.parkenTyp,
			this.parkenForm,
			this.breite,
			this.benutzungspflicht,
			this.beschilderung,
			this.schaeden,
			this.absenkung,
			this.trennstreifenBreiteRechts,
			this.trennstreifenBreiteLinks,
			this.trennstreifenTrennungZuRechts,
			this.trennstreifenTrennungZuLinks,
			this.trennstreifenFormRechts,
			this.trennstreifenFormLinks));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends LinearReferenzierteAttribute> T mergeAttributeNimmErstenNichtDefaultWert(T other,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		if (!(other instanceof FuehrungsformAttribute)) {
			throw new RuntimeException("Es lassen sich nur zwei Attribute der gleichen Klasse mergen");
		} else {
			return (T) mergeAttributeNimmErstenNichtDefaultWert((FuehrungsformAttribute) other,
				linearReferenzierterAbschnitt);
		}
	}

	/*
	 * Achtung: hier werden die Trennstreifen nicht beachtet, aber das ist ok da diese Methode nur aus dem (veralteten)
	 * RadwegeDBImport aufgerufen wird
	 */
	private FuehrungsformAttribute mergeAttributeNimmErstenNichtDefaultWert(FuehrungsformAttribute other,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		return FuehrungsformAttribute.builder()
			.belagArt(belagArt.nichtUnbekanntOrElse(other.belagArt))
			.oberflaechenbeschaffenheit(oberflaechenbeschaffenheit
				.nichtUnbekanntOrElse(other.oberflaechenbeschaffenheit))
			.bordstein(bordstein.nichtUnbekanntOrElse(other.bordstein))
			.radverkehrsfuehrung(radverkehrsfuehrung
				.nichtUnbekanntOrElse(other.radverkehrsfuehrung))
			.parkenTyp(parkenTyp.nichtUnbekanntOrElse(other.parkenTyp))
			.parkenForm(parkenForm.nichtUnbekanntOrElse(other.getParkenForm()))
			.breite(this.getBreite().orElse(other.getBreite().orElse(null)))
			.beschilderung(other.getBeschilderung())
			.schaeden(other.schaeden)
			.absenkung(other.absenkung)
			.benutzungspflicht(benutzungspflicht
				.nichtUnbekanntOrElse(other.benutzungspflicht))
			.linearReferenzierterAbschnitt(linearReferenzierterAbschnitt)
			.build();
	}

	@Override
	public boolean sindAttributeGleich(LinearReferenzierteAttribute other) {
		if (!(other instanceof FuehrungsformAttribute)) {
			return false;
		} else {
			return sindAttributeGleich((FuehrungsformAttribute) other);
		}
	}

	public boolean sindAttributeGleich(FuehrungsformAttribute other) {
		return Objects.equals(radverkehrsfuehrung, other.radverkehrsfuehrung)
			&& Objects.equals(belagArt, other.belagArt)
			&& Objects.equals(oberflaechenbeschaffenheit, other.oberflaechenbeschaffenheit)
			&& Objects.equals(bordstein, other.bordstein)
			&& Objects.equals(parkenTyp, other.parkenTyp)
			&& Objects.equals(parkenForm, other.parkenForm)
			&& Objects.equals(benutzungspflicht, other.benutzungspflicht)
			&& Objects.equals(breite, other.breite)
			&& Objects.equals(beschilderung, other.beschilderung)
			&& Objects.equals(absenkung, other.absenkung)
			&& Objects.equals(schaeden, other.schaeden)
			&& Objects.equals(trennstreifenBreiteRechts, other.trennstreifenBreiteRechts)
			&& Objects.equals(trennstreifenBreiteLinks, other.trennstreifenBreiteLinks)
			&& Objects.equals(trennstreifenTrennungZuRechts, other.trennstreifenTrennungZuRechts)
			&& Objects.equals(trennstreifenTrennungZuLinks, other.trennstreifenTrennungZuLinks)
			&& Objects.equals(trennstreifenFormRechts, other.trennstreifenFormRechts)
			&& Objects.equals(trennstreifenFormLinks, other.trennstreifenFormLinks);
	}

	@Override
	public boolean widersprechenSichAttribute(LinearReferenzierteAttribute other) {
		if (!(other instanceof FuehrungsformAttribute)) {
			return true;
		} else {
			return widersprechenSichAttribute((FuehrungsformAttribute) other);
		}
	}

	public boolean widersprechenSichAttribute(FuehrungsformAttribute other) {
		if (radverkehrsfuehrung.widerspruchZu(other.radverkehrsfuehrung)) {
			return true;
		} else if (belagArt.widerspruchZu(other.belagArt)) {
			return true;
		} else if (oberflaechenbeschaffenheit.widerspruchZu(other.oberflaechenbeschaffenheit)) {
			return true;
		} else if (bordstein.widerspruchZu(other.bordstein)) {
			return true;
		} else if (parkenTyp.widerspruchZu(other.parkenTyp)) {
			return true;
		} else if (parkenForm.widerspruchZu(other.parkenForm)) {
			return true;
		} else if (absenkung.widerspruchZu(other.absenkung)) {
			return true;
		} else if (benutzungspflicht.widerspruchZu(other.benutzungspflicht)) {
			return true;
		} else if (beschilderung.widerspruchZu(other.beschilderung)) {
			return true;
		} else if (!Objects.equals(schaeden, other.schaeden)) {
			return true;
		} else if (TrennstreifenForm.nullableWiderspruchZu(trennstreifenFormLinks, other.trennstreifenFormLinks)) {
			return true;
		} else if (TrennstreifenForm.nullableWiderspruchZu(trennstreifenFormRechts, other.trennstreifenFormRechts)) {
			return true;
		} else if (TrennungZu.nullableWiderspruchZu(trennstreifenTrennungZuLinks, other.trennstreifenTrennungZuLinks)) {
			return true;
		} else if (TrennungZu.nullableWiderspruchZu(trennstreifenTrennungZuRechts,
			other.trennstreifenTrennungZuRechts)) {
			return true;
		} else if (trennstreifenBreiteLinks == null && other.trennstreifenBreiteLinks != null ||
			trennstreifenBreiteLinks != null && other.trennstreifenBreiteLinks == null ||
			(trennstreifenBreiteLinks != null && other.trennstreifenBreiteLinks != null &&
				!trennstreifenBreiteLinks.equals(other.trennstreifenBreiteLinks))) {
			return true;
		} else if (trennstreifenBreiteRechts == null && other.trennstreifenBreiteRechts != null ||
			trennstreifenBreiteRechts != null && other.trennstreifenBreiteRechts == null ||
			(trennstreifenBreiteRechts != null && other.trennstreifenBreiteRechts != null &&
				!trennstreifenBreiteRechts.equals(other.trennstreifenBreiteRechts))) {
			return true;
		} else {
			return breite != null && other.breite != null && !this.breite.equals(other.breite);
		}
	}

	public Optional<Laenge> getTrennstreifenBreiteRechts() {
		return Optional.ofNullable(trennstreifenBreiteRechts);
	}

	public Optional<Laenge> getTrennstreifenBreiteLinks() {
		return Optional.ofNullable(trennstreifenBreiteLinks);
	}

	public Optional<TrennungZu> getTrennstreifenTrennungZuRechts() {
		return Optional.ofNullable(trennstreifenTrennungZuRechts);
	}

	public Optional<TrennungZu> getTrennstreifenTrennungZuLinks() {
		return Optional.ofNullable(trennstreifenTrennungZuLinks);
	}

	public Optional<TrennstreifenForm> getTrennstreifenFormRechts() {
		return Optional.ofNullable(trennstreifenFormRechts);
	}

	public Optional<TrennstreifenForm> getTrennstreifenFormLinks() {
		return Optional.ofNullable(trennstreifenFormLinks);
	}

	public static boolean isTrennstreifenCorrect(Radverkehrsfuehrung radverkehrsfuehrung,
		TrennstreifenForm trennstreifenForm, Laenge trennstreifenBreite, TrennungZu trennstreifenTrennungZu) {
		if (radverkehrsfuehrung == null) {
			return true;
		}
		try {
			assertTrennstreifenCorrectness(radverkehrsfuehrung, trennstreifenForm, trennstreifenBreite,
				trennstreifenTrennungZu);
		} catch (RequireViolation requireViolation) {
			return false;
		}
		return true;
	}

	private static void assertTrennstreifenCorrectness(Radverkehrsfuehrung radverkehrsfuehrung,
		TrennstreifenForm trennstreifenForm, Laenge trennstreifenBreite, TrennungZu trennstreifenTrennungZu) {
		require(radverkehrsfuehrung, notNullValue());
		boolean allFieldsAreNull = trennstreifenForm == null &&
			trennstreifenTrennungZu == null &&
			trennstreifenBreite == null;
		if (allFieldsAreNull) {
			/*
			 * Seit RAD-4719 gibt es Sicherheitstrennstreifen an jeder Kante, deren Werte aber initial "null" sind.
			 * Damit ein Nutzer nicht bei jeden Führungsform-Attributen gezwungen wird auch den Trennstreifen zu setzen
			 * (was vielleicht gar nicht möglich ist, weil nicht bekannt o.Ä.), erlauben wir explizit, dass alle drei
			 * Felder "null"-Werte halten. Eigentlich möchte man die Regel haben
			 * "wenn vorher UND hinterher alles null, dann OK". Allerdings können wir die Führungsform-Attribute der
			 * Kante und des command nicht aufeinander abbilden, da die Führungsform-Attribute keine ID haben. Daher
			 * erlauben wir hier generell die Regel "wenn alles null, dann OK".
			 */
			return;
		}

		if (!TrennstreifenForm.isRequiredForRadverkehrsfuehrung(radverkehrsfuehrung)) {
			require(trennstreifenForm, nullValue());
			require(trennstreifenBreite, nullValue());
			require(trennstreifenTrennungZu, nullValue());
		} else {
			require(trennstreifenForm, notNullValue());

			boolean hasTrennstreifenForm = trennstreifenForm != TrennstreifenForm.UNBEKANNT &&
				trennstreifenForm != TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN;
			if (hasTrennstreifenForm) {
				if (!trennstreifenForm.equals(TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART)) {
					require(trennstreifenBreite, notNullValue());
				}
				if (trennstreifenTrennungZu != null) {
					require(trennstreifenTrennungZu.isValidForRadverkehrsfuehrung(radverkehrsfuehrung),
						"Gewählte Trennung Zu-Option ist nicht für Radverkehrsführung %s erlaubt", radverkehrsfuehrung);
				}
			} else {
				require(trennstreifenTrennungZu, nullValue());
				require(trennstreifenBreite, nullValue());
			}
		}
	}

	public FuehrungsformAttribute withUmgekehrterStationierungsrichtung() {
		return toBuilder().trennstreifenBreiteLinks(trennstreifenBreiteRechts)
			.trennstreifenFormLinks(trennstreifenFormRechts)
			.trennstreifenTrennungZuLinks(trennstreifenTrennungZuRechts)
			.trennstreifenBreiteRechts(trennstreifenBreiteLinks).trennstreifenFormRechts(trennstreifenFormLinks)
			.trennstreifenTrennungZuRechts(trennstreifenTrennungZuLinks).build();
	}

	@Override
	protected boolean hasOnlyDefaultAttribute() {
		return sindAttributeGleich(FuehrungsformAttribute.builder().build());
	}
}
