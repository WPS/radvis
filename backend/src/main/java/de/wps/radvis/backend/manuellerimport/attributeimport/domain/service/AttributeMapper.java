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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.service;

import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.exception.AttributUebernahmeException;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.AttributUebernahmeFehler;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedAttributes;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedAttributesProperties;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute.FuehrungsformAttributeBuilder;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.Absenkung;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Benutzungspflicht;
import de.wps.radvis.backend.netz.domain.valueObject.Beschilderung;
import de.wps.radvis.backend.netz.domain.valueObject.Bordstein;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KfzParkenTyp;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.Schadenart;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenkategorieRIN;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netz.domain.valueObject.WegeNiveau;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

abstract public class AttributeMapper {

	private static final String RADVERKEHRSFUEHRUNG_BESCHILDERUNG_KONFLIKT_BESCHREIBUNG = "Beschilderung und Radverkehrsführung passen nicht zusammen. "
		+ "Beschilderung wurde auf " + Beschilderung.UNBEKANNT.name()
		+ " gesetzt, Radverkehrsführung wurde übernommen. "
		+ "Wenn Sie beide Attribute zugleich importieren, wurde die Beschilderung im weiteren Verlaub auf den neuen, validen Wert gesetzt.";

	public abstract void applyEinfach(String attribut, String attributwert, Kante kante)
		throws AttributUebernahmeException;

	public abstract void applyBeideSeiten(String attribut, String attributwertLinks, String attributwertRechts,
		Kante kante);

	public abstract void applyLinearReferenzierterAbschnitt(String attribut, MappedAttributesProperties properties,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, Kante kante) throws AttributUebernahmeException;

	public abstract void applyLinearReferenzierterAbschnittSeitenbezogen(String attribut,
		MappedAttributesProperties attributesProperties,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		Seitenbezug seitenbezug, Kante kante) throws AttributUebernahmeException;

	public abstract boolean isAttributSeitenbezogen(String attribut);

	public abstract boolean isLinearReferenziert(String attribut);

	public abstract boolean isRichtung(String attribut);

	protected abstract String getUmgekehrteRichtung(String wertFuerRichtung);

	public abstract String getRadVisAttributName(String importedAttributName);

	public abstract boolean isAttributNameValid(String attributName);

	public abstract boolean isAttributWertValid(String attributName, String attributWert);

	/**
	 * Importreihenfolge festlegen, falls reihenfolge-abhängige Attribute existieren
	 * 
	 * @param attribut1
	 * @param attribut2
	 * @return
	 */
	public int sortAttribute(String attribut1, String attribut2) {
		return 0;
	}

	public String getImportGruppe(String attrName) {
		return attrName;
	}

	public MappedAttributes dreheRichtungUm(MappedAttributes mappedAttributes, String richtungsAttribut) {
		require(isRichtung(richtungsAttribut));
		if (!mappedAttributes.isOrientierungUmgedrehtZurKante()) {
			return mappedAttributes;
		}
		Map<String, Object> newProperties = new HashMap<>();
		mappedAttributes.getProperties()
			.forEach(
				(key, value) -> newProperties.put(key,
					isRichtung(key) ? getUmgekehrteRichtung(value.toString()) : value));
		return MappedAttributes.of(newProperties, mappedAttributes.getLinearReferenzierterAbschnitt(),
			mappedAttributes.getSeitenbezug(), false);
	}

	// ----------------------------------------- KantenAttributGruppe --------------------------------------------------
	protected void applyBeleuchtung(Kante kante, Beleuchtung beleuchtung) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().beleuchtung(beleuchtung).build());
	}

	protected void applyFussverkehr(Kante kante, VerkehrStaerke dtvFussverkehr) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().dtvFussverkehr(dtvFussverkehr).build());
	}

	protected void applyPkw(Kante kante, VerkehrStaerke dtvPkw) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().dtvPkw(dtvPkw).build());
	}

	protected void applyRadverkehr(Kante kante, VerkehrStaerke dtvRadverkehr) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().dtvRadverkehr(dtvRadverkehr).build());
	}

	protected void applySV(Kante kante, VerkehrStaerke sv) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().sv(sv).build());
	}

	protected void applyWegeniveau(Kante kante, WegeNiveau wegeniveau) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().wegeNiveau(wegeniveau).build());
	}

	protected void applyKommentar(Kante kante, Kommentar kommentar) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().kommentar(kommentar).build());
	}

	protected void applyLaengeManuell(Kante kante, Laenge laenge) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().laengeManuellErfasst(laenge).build());
	}

	protected void applyStrassenName(Kante kante, StrassenName strassenname) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().strassenName(strassenname).build());
	}

	protected void applyStrassenNummer(Kante kante, StrassenNummer strassennummer) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().strassenNummer(strassennummer).build());
	}

	protected void applyUmfeld(Kante kante, Umfeld umfeld) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().umfeld(umfeld).build());
	}

	protected void applyStrassenkategorieRIN(Kante kante, StrassenkategorieRIN strassenkategorieRIN) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().strassenkategorieRIN(strassenkategorieRIN)
					.build());
	}

	protected void applyStrassenquerschnittRASt06(Kante kante, StrassenquerschnittRASt06 strassenquerschnittrast06) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder()
					.strassenquerschnittRASt06(strassenquerschnittrast06).build());
	}

	protected void applyStatus(Kante kante, Status status) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().status(status).build());
	}

	protected void applyIstStandards(Kante kante, Set<IstStandard> istStandards) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), istStandards, kantenAttributGruppe.getKantenAttribute());
	}

	protected void applyGemeinde(Kante kante, Verwaltungseinheit gemeinde) {
		KantenAttributGruppe kantenAttributGruppe = kante.getKantenAttributGruppe();
		kantenAttributGruppe
			.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
				kantenAttributGruppe.getKantenAttribute().toBuilder().gemeinde(gemeinde).build());
	}

	// ---------------------------------- FuehrungsformAttributGruppe Linear Ref ---------------------------------------
	protected void applyBelagArt(Kante kante, BelagArt belagArt,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyFuehrungsformAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().belagArt(belagArt).build());
	}

	protected void applyBreite(Kante kante, Laenge breite,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyFuehrungsformAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().breite(breite).build());
	}

	protected void applySchaeden(LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, Kante kante,
		Set<Schadenart> schaeden) {
		applyFuehrungsformAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().schaeden(schaeden).build());
	}

	protected void applyAbsenkung(LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, Kante kante,
		Absenkung absenkung) {
		applyFuehrungsformAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().absenkung(absenkung).build());
	}

	protected void applyBeschilderung(LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, Kante kante,
		Beschilderung beschilderung) throws AttributUebernahmeException {
		List<AttributUebernahmeFehler> attributUebernahmeFehler = new ArrayList<>();
		applyFuehrungsformAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> {
				if (beschilderung.isValidForRadverkehrsfuehrung(attribute.getRadverkehrsfuehrung())) {
					return attribute.toBuilder().beschilderung(beschilderung).build();
				} else {
					attributUebernahmeFehler.add(new AttributUebernahmeFehler(
						"Gewählte Beschilderung passt nicht zu Radverkehrsführung: nur für Betriebswege erlaubt",
						Set.of(beschilderung.name()), linearReferenzierterAbschnitt));
					return attribute;
				}
			});

		if (!attributUebernahmeFehler.isEmpty()) {
			throw new AttributUebernahmeException(attributUebernahmeFehler);
		}
	}

	protected void applyRadverkehrsfuehrung(Kante kante, Radverkehrsfuehrung radverkehrsfuehrung,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) throws AttributUebernahmeException {
		ArrayList<AttributUebernahmeFehler> attributUebernahmeFehler = new ArrayList<>();

		applyFuehrungsformAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> {
				boolean trennstreifenAreCorrect = verifyTrennstreifenCorrectness(kante, radverkehrsfuehrung,
					linearReferenzierterAbschnitt, attribute, attributUebernahmeFehler);

				if (!trennstreifenAreCorrect) {
					return attribute;
				}

				FuehrungsformAttributeBuilder builder = attribute.toBuilder();
				if (!attribute.getBeschilderung().isValidForRadverkehrsfuehrung(radverkehrsfuehrung)) {
					builder.beschilderung(Beschilderung.UNBEKANNT);
					attributUebernahmeFehler
						.add(new AttributUebernahmeFehler(RADVERKEHRSFUEHRUNG_BESCHILDERUNG_KONFLIKT_BESCHREIBUNG,
							Set.of(attribute.getBeschilderung().name()), linearReferenzierterAbschnitt));
				}
				return builder.radverkehrsfuehrung(radverkehrsfuehrung).build();
			});

		if (!attributUebernahmeFehler.isEmpty()) {
			throw new AttributUebernahmeException(attributUebernahmeFehler);
		}
	}

	private static boolean verifyTrennstreifenCorrectness(Kante kante, Radverkehrsfuehrung radverkehrsfuehrung,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, FuehrungsformAttribute attribute,
		ArrayList<AttributUebernahmeFehler> attributUebernahmeFehler) {

		TrennstreifenForm trennstreifenFormLinks = attribute.getTrennstreifenFormLinks().orElse(null);
		Laenge trennstreifenBreiteLinks = attribute.getTrennstreifenBreiteLinks().orElse(null);
		TrennungZu trennstreifenTrennungZuLinks = attribute.getTrennstreifenTrennungZuLinks().orElse(null);
		boolean trennstreifenLinksCorrect = FuehrungsformAttribute.isTrennstreifenCorrect(radverkehrsfuehrung,
			trennstreifenFormLinks, trennstreifenBreiteLinks, trennstreifenTrennungZuLinks);

		if (!trennstreifenLinksCorrect) {
			String message = String.format(
				"Neue Radverkehrsführung %s (derzeit: %s) auf Kante %d führt zu inkompatiblen Attributen des linken Sicherheitstrennstreifens (derzeit: Form = %s, Trennung zu = %s, Breite = %s).",
				radverkehrsfuehrung.toString(),
				attribute.getRadverkehrsfuehrung().toString(),
				kante.getId(),
				trennstreifenFormLinks != null ? trennstreifenFormLinks.toString() : "-",
				trennstreifenTrennungZuLinks != null ? trennstreifenTrennungZuLinks.toString() : "-",
				trennstreifenBreiteLinks != null ? trennstreifenBreiteLinks.toString() : "-");
			attributUebernahmeFehler.add(new AttributUebernahmeFehler(message, Set.of(radverkehrsfuehrung
				.toString()), linearReferenzierterAbschnitt));
		}

		TrennstreifenForm trennstreifenFormRechts = attribute.getTrennstreifenFormRechts().orElse(null);
		Laenge trennstreifenBreiteRechts = attribute.getTrennstreifenBreiteRechts().orElse(null);
		TrennungZu trennstreifenTrennungZuRechts = attribute.getTrennstreifenTrennungZuRechts().orElse(null);
		boolean trennstreifenRechtsCorrect = FuehrungsformAttribute.isTrennstreifenCorrect(radverkehrsfuehrung,
			trennstreifenFormRechts, trennstreifenBreiteRechts, trennstreifenTrennungZuRechts);

		if (!trennstreifenRechtsCorrect) {
			String message = String.format(
				"Neue Radverkehrsführung %s (derzeit: %s) auf Kante %d führt zu inkompatiblen Attributen des rechten Sicherheitstrennstreifens (derzeit: Form = %s, Trennung zu = %s, Breite = %s).",
				radverkehrsfuehrung.toString(),
				attribute.getRadverkehrsfuehrung().toString(),
				kante.getId(),
				trennstreifenFormRechts != null ? trennstreifenFormRechts.toString() : "-",
				trennstreifenTrennungZuRechts != null ? trennstreifenTrennungZuRechts.toString() : "-",
				trennstreifenBreiteRechts != null ? trennstreifenBreiteRechts.toString() : "-");
			attributUebernahmeFehler.add(new AttributUebernahmeFehler(message, Set.of(radverkehrsfuehrung
				.toString()), linearReferenzierterAbschnitt));
		}

		return trennstreifenLinksCorrect && trennstreifenRechtsCorrect;
	}

	protected void applyRadverkehrsfuehrungUndTrennstreifen(Kante kante, Radverkehrsfuehrung radverkehrsfuehrung,
		TrennstreifenForm trennstreifenFormLinks, Laenge trennstreifenBreiteLinks, TrennungZu trennungZuLinks,
		TrennstreifenForm trennstreifenFormRechts, Laenge trennstreifenBreiteRechts, TrennungZu trennungZuRechts,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) throws AttributUebernahmeException {
		ArrayList<AttributUebernahmeFehler> attributUebernahmeFehler = new ArrayList<>();

		applyFuehrungsformAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> {

				FuehrungsformAttributeBuilder builder = attribute.toBuilder();
				if (!attribute.getBeschilderung().isValidForRadverkehrsfuehrung(radverkehrsfuehrung)) {
					builder.beschilderung(Beschilderung.UNBEKANNT);
					attributUebernahmeFehler
						.add(new AttributUebernahmeFehler(RADVERKEHRSFUEHRUNG_BESCHILDERUNG_KONFLIKT_BESCHREIBUNG,
							Set.of(attribute.getBeschilderung().name()), linearReferenzierterAbschnitt));
				}

				return builder
					.radverkehrsfuehrung(radverkehrsfuehrung)
					.trennstreifenFormLinks(trennstreifenFormLinks)
					.trennstreifenBreiteLinks(trennstreifenBreiteLinks)
					.trennstreifenTrennungZuLinks(trennungZuLinks)
					.trennstreifenFormRechts(trennstreifenFormRechts)
					.trennstreifenBreiteRechts(trennstreifenBreiteRechts)
					.trennstreifenTrennungZuRechts(trennungZuRechts)
					.build();
			});

		if (!attributUebernahmeFehler.isEmpty()) {
			throw new AttributUebernahmeException(attributUebernahmeFehler);
		}
	}

	protected void applyKfzParkenTyp(Kante kante, KfzParkenTyp kfzParkenTyp,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyFuehrungsformAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().parkenTyp(kfzParkenTyp).build());
	}

	protected void applyKfzParkenForm(Kante kante, KfzParkenForm kfzParkenForm,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyFuehrungsformAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().parkenForm(kfzParkenForm).build());
	}

	protected void applyBordstein(Kante kante, Bordstein bordstein,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyFuehrungsformAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().bordstein(bordstein).build());
	}

	protected void applyOberflaechenbeschaffenheit(Kante kante, Oberflaechenbeschaffenheit oberflaechenbeschaffenheit,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyFuehrungsformAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().oberflaechenbeschaffenheit(oberflaechenbeschaffenheit).build());
	}

	protected void applyBenutzungspflicht(Kante kante, Benutzungspflicht benutzungspflicht,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyFuehrungsformAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().benutzungspflicht(benutzungspflicht).build());
	}

	protected void applyTrennstreifenInfoLinks(Kante kante, TrennstreifenForm trennstreifenForm,
		Laenge trennstreifenBreite, TrennungZu trennungZu,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {

		applyFuehrungsformAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder()
				.trennstreifenFormLinks(trennstreifenForm)
				.trennstreifenBreiteLinks(trennstreifenBreite)
				.trennstreifenTrennungZuLinks(trennungZu)
				.build());
	}

	protected void applyTrennstreifenInfoLinksUndRechts(Kante kante, TrennstreifenForm trennstreifenForm,
		Laenge trennstreifenBreite, TrennungZu trennungZu, Seitenbezug seitenbezug,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {

		applyTrennstreifenInfoLinks(kante, trennstreifenForm, trennstreifenBreite, trennungZu, seitenbezug,
			linearReferenzierterAbschnitt);
		applyTrennstreifenInfoRechts(kante, trennstreifenForm, trennstreifenBreite, trennungZu, seitenbezug,
			linearReferenzierterAbschnitt);
	}

	protected void applyTrennstreifenInfoLinks(Kante kante, TrennstreifenForm trennstreifenForm,
		Laenge trennstreifenBreite, TrennungZu trennungZu, Seitenbezug seitenbezug,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {

		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder()
				.trennstreifenFormLinks(trennstreifenForm)
				.trennstreifenBreiteLinks(trennstreifenBreite)
				.trennstreifenTrennungZuLinks(trennungZu)
				.build());
	}

	protected void applyTrennstreifenInfoRechts(Kante kante, TrennstreifenForm trennstreifenForm,
		Laenge trennstreifenBreite, TrennungZu trennungZu,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {

		applyFuehrungsformAttributgruppeLinearReferenziert(kante,
			linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder()
				.trennstreifenFormRechts(trennstreifenForm)
				.trennstreifenBreiteRechts(trennstreifenBreite)
				.trennstreifenTrennungZuRechts(trennungZu)
				.build());
	}

	protected void applyTrennstreifenInfoRechts(Kante kante, TrennstreifenForm trennstreifenForm,
		Laenge trennstreifenBreite, TrennungZu trennungZu, Seitenbezug seitenbezug,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {

		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder()
				.trennstreifenFormRechts(trennstreifenForm)
				.trennstreifenBreiteRechts(trennstreifenBreite)
				.trennstreifenTrennungZuRechts(trennungZu)
				.build());
	}

	private void applyFuehrungsformAttributgruppeLinearReferenziert(Kante kante,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		UnaryOperator<FuehrungsformAttribute> updateFunction) {
		if (kante.isZweiseitig()) {
			this.applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, Seitenbezug.LINKS,
				linearReferenzierterAbschnitt, updateFunction);
			this.applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, Seitenbezug.RECHTS,
				linearReferenzierterAbschnitt, updateFunction);
		} else {
			List<FuehrungsformAttribute> fuehrungsformAttributeBeideSeiten = kante.getFuehrungsformAttributGruppe()
				.getImmutableFuehrungsformAttributeLinks();

			List<FuehrungsformAttribute> neueAttribute = updateLinearReferenzierteAttribute(
				fuehrungsformAttributeBeideSeiten, linearReferenzierterAbschnitt, updateFunction);

			kante.getFuehrungsformAttributGruppe()
				.replaceFuehrungsformAttribute(neueAttribute);
		}
	}

	// ------------------------- FuehrungsformAttributGruppe Linear Ref + Seitenbezogen---------------------------------
	protected void applyBelagArt(Kante kante, BelagArt belagArt, Seitenbezug seitenbezug,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		require(!Seitenbezug.BEIDSEITIG.equals(seitenbezug));
		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().belagArt(belagArt).build());
	}

	protected void applyBreite(Kante kante, Laenge breite, Seitenbezug seitenbezug,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		require(!Seitenbezug.BEIDSEITIG.equals(seitenbezug));
		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().breite(breite).build());
	}

	protected void applyRadverkehrsfuehrung(Kante kante, Radverkehrsfuehrung radverkehrsfuehrung,
		Seitenbezug seitenbezug, LinearReferenzierterAbschnitt linearReferenzierterAbschnitt)
		throws AttributUebernahmeException {
		require(!Seitenbezug.BEIDSEITIG.equals(seitenbezug));
		ArrayList<AttributUebernahmeFehler> attributUebernahmeFehler = new ArrayList<>();
		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> {
				FuehrungsformAttributeBuilder builder = attribute.toBuilder();
				if (!attribute.getBeschilderung().isValidForRadverkehrsfuehrung(radverkehrsfuehrung)) {
					builder.beschilderung(Beschilderung.UNBEKANNT);
					attributUebernahmeFehler
						.add(new AttributUebernahmeFehler(RADVERKEHRSFUEHRUNG_BESCHILDERUNG_KONFLIKT_BESCHREIBUNG,
							Set.of(attribute.getBeschilderung().name()), linearReferenzierterAbschnitt, seitenbezug));
				}
				return builder.radverkehrsfuehrung(radverkehrsfuehrung).build();
			});

		if (!attributUebernahmeFehler.isEmpty()) {
			throw new AttributUebernahmeException(attributUebernahmeFehler);
		}
	}

	protected void applyRadverkehrsfuehrungUndTrennstreifen(Kante kante, Radverkehrsfuehrung radverkehrsfuehrung,
		TrennstreifenForm trennstreifenFormLinks, Laenge trennstreifenBreiteLinks, TrennungZu trennungZuLinks,
		TrennstreifenForm trennstreifenFormRechts, Laenge trennstreifenBreiteRechts, TrennungZu trennungZuRechts,
		Seitenbezug seitenbezug, LinearReferenzierterAbschnitt linearReferenzierterAbschnitt)
		throws AttributUebernahmeException {
		require(!Seitenbezug.BEIDSEITIG.equals(seitenbezug));
		ArrayList<AttributUebernahmeFehler> attributUebernahmeFehler = new ArrayList<>();

		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> {

				FuehrungsformAttributeBuilder builder = attribute.toBuilder();
				if (!attribute.getBeschilderung().isValidForRadverkehrsfuehrung(radverkehrsfuehrung)) {
					builder.beschilderung(Beschilderung.UNBEKANNT);
					attributUebernahmeFehler
						.add(new AttributUebernahmeFehler(RADVERKEHRSFUEHRUNG_BESCHILDERUNG_KONFLIKT_BESCHREIBUNG,
							Set.of(attribute.getBeschilderung().name()), linearReferenzierterAbschnitt, seitenbezug));
				}

				return builder
					.radverkehrsfuehrung(radverkehrsfuehrung)
					.trennstreifenFormLinks(trennstreifenFormLinks)
					.trennstreifenBreiteLinks(trennstreifenBreiteLinks)
					.trennstreifenTrennungZuLinks(trennungZuLinks)
					.trennstreifenFormRechts(trennstreifenFormRechts)
					.trennstreifenBreiteRechts(trennstreifenBreiteRechts)
					.trennstreifenTrennungZuRechts(trennungZuRechts)
					.build();
			});

		if (!attributUebernahmeFehler.isEmpty()) {
			throw new AttributUebernahmeException(attributUebernahmeFehler);
		}
	}

	protected void applyKfzParkenTyp(Kante kante, KfzParkenTyp kfzParkenTyp, Seitenbezug seitenbezug,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		require(!Seitenbezug.BEIDSEITIG.equals(seitenbezug));
		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().parkenTyp(kfzParkenTyp).build());
	}

	protected void applyKfzParkenForm(Kante kante, KfzParkenForm kfzParkenForm, Seitenbezug seitenbezug,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		require(!Seitenbezug.BEIDSEITIG.equals(seitenbezug));
		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().parkenForm(kfzParkenForm).build());
	}

	protected void applyBordstein(Kante kante, Bordstein bordstein, Seitenbezug seitenbezug,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		require(!Seitenbezug.BEIDSEITIG.equals(seitenbezug));
		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().bordstein(bordstein).build());
	}

	protected void applyOberflaechenbeschaffenheit(Kante kante, Oberflaechenbeschaffenheit oberflaechenbeschaffenheit,
		Seitenbezug seitenbezug, LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		require(!Seitenbezug.BEIDSEITIG.equals(seitenbezug));
		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().oberflaechenbeschaffenheit(oberflaechenbeschaffenheit).build());
	}

	protected void applyBenutzungspflicht(Kante kante, Benutzungspflicht benutzungspflicht, Seitenbezug seitenbezug,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		require(!Seitenbezug.BEIDSEITIG.equals(seitenbezug));
		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().benutzungspflicht(benutzungspflicht).build());
	}

	protected void applySchaeden(Kante kante, Set<Schadenart> schaeden, Seitenbezug seitenbezug,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		require(!Seitenbezug.BEIDSEITIG.equals(seitenbezug));
		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().schaeden(schaeden).build());
	}

	protected void applyBeschilderung(Kante kante, Beschilderung beschilderung, Seitenbezug seitenbezug,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) throws AttributUebernahmeException {
		require(!Seitenbezug.BEIDSEITIG.equals(seitenbezug));
		List<AttributUebernahmeFehler> attributUebernahmeFehler = new ArrayList<>();

		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> {

				if (beschilderung.isValidForRadverkehrsfuehrung(attribute.getRadverkehrsfuehrung())) {
					return attribute.toBuilder().beschilderung(beschilderung).build();
				} else {
					attributUebernahmeFehler.add(new AttributUebernahmeFehler(
						"Gewählte Beschilderung passt nicht zu Radverkehrsführung: nur für Betriebswege erlaubt",
						Set.of(beschilderung.name()), linearReferenzierterAbschnitt, seitenbezug));
					return attribute;
				}
			});

		if (!attributUebernahmeFehler.isEmpty()) {
			throw new AttributUebernahmeException(attributUebernahmeFehler);
		}
	}

	protected void applyAbsenkung(Kante kante, Absenkung absenkung, Seitenbezug seitenbezug,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		require(!Seitenbezug.BEIDSEITIG.equals(seitenbezug));
		applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(kante, seitenbezug,
			linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().absenkung(absenkung).build());
	}

	private void applyFuehrungsformAttributgruppeLinearReferenziertundSeitenbezogen(Kante kante,
		Seitenbezug seitenbezug, LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		UnaryOperator<FuehrungsformAttribute> updateFunction) {
		kante.changeSeitenbezug(true);

		FuehrungsformAttributGruppe attributgruppe = kante.getFuehrungsformAttributGruppe();

		List<FuehrungsformAttribute> fuerSeitenbezug = seitenbezug.equals(Seitenbezug.LINKS) ? attributgruppe
			.getImmutableFuehrungsformAttributeLinks() : attributgruppe.getImmutableFuehrungsformAttributeRechts();

		List<FuehrungsformAttribute> neueAttribute = updateLinearReferenzierteAttribute(fuerSeitenbezug,
			linearReferenzierterAbschnitt, updateFunction);

		if (seitenbezug.equals(Seitenbezug.LINKS)) {
			attributgruppe.replaceFuehrungsformAttribute(neueAttribute,
				attributgruppe.getImmutableFuehrungsformAttributeRechts());
		} else {
			attributgruppe.replaceFuehrungsformAttribute(
				attributgruppe.getImmutableFuehrungsformAttributeLinks(), neueAttribute);
		}
	}

	// ------------------------------ GeschwindigkeitsAttributGruppe Linear Ref ----------------------------------------
	protected void applyKantenOrtslage(Kante kante, KantenOrtslage kantenOrtslage,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyGeschwindigkeitAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().ortslage(kantenOrtslage).build());
	}

	protected void applyHoechstgeschwindigkeit(Kante kante, Hoechstgeschwindigkeit hoechstgeschwindigkeit,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyGeschwindigkeitAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().hoechstgeschwindigkeit(hoechstgeschwindigkeit).build());
	}

	protected void applyHoechstgeschwindigkeitGegenStationierungsRichtung(Kante kante,
		Hoechstgeschwindigkeit hoechstgeschwindigkeitGegenStatio,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyGeschwindigkeitAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder()
				.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(hoechstgeschwindigkeitGegenStatio)
				.build());
	}

	private void applyGeschwindigkeitAttributgruppeLinearReferenziert(Kante kante,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		UnaryOperator<GeschwindigkeitAttribute> updateFunction) {
		List<GeschwindigkeitAttribute> geschwindigkeitAttributeBeideSeiten = kante.getGeschwindigkeitAttributGruppe()
			.getImmutableGeschwindigkeitAttribute();

		List<GeschwindigkeitAttribute> neueAttribute = updateLinearReferenzierteAttribute(
			geschwindigkeitAttributeBeideSeiten, linearReferenzierterAbschnitt, updateFunction);

		kante.getGeschwindigkeitAttributGruppe().replaceGeschwindigkeitAttribute(neueAttribute);
	}

	// ------------------------------ ZustaendigkeitsAttributGruppe Linear Ref ----------------------------------------

	protected void applyBaulastTraeger(Kante kante, Verwaltungseinheit baulastTr,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyZustaendigkeitAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().baulastTraeger(baulastTr).build());
	}

	protected void applyUnterhaltsZustaendiger(Kante kante, Verwaltungseinheit unterhaltsZust,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyZustaendigkeitAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().unterhaltsZustaendiger(unterhaltsZust).build());
	}

	protected void applyErhaltsZustaendiger(Kante kante, Verwaltungseinheit erhaltsZust,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyZustaendigkeitAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().erhaltsZustaendiger(erhaltsZust).build());
	}

	protected void applyVereinbarungskennung(Kante kante, VereinbarungsKennung vereinbarungsKennung,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		applyZustaendigkeitAttributgruppeLinearReferenziert(kante, linearReferenzierterAbschnitt,
			attribute -> attribute.toBuilder().vereinbarungsKennung(vereinbarungsKennung).build());
	}

	private void applyZustaendigkeitAttributgruppeLinearReferenziert(Kante kante,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		UnaryOperator<ZustaendigkeitAttribute> updateFunction) {
		List<ZustaendigkeitAttribute> zustaendigkeitAttributeBeideSeiten = kante.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute();

		List<ZustaendigkeitAttribute> neueAttribute = updateLinearReferenzierteAttribute(
			zustaendigkeitAttributeBeideSeiten, linearReferenzierterAbschnitt, updateFunction);

		kante.getZustaendigkeitAttributGruppe().replaceZustaendigkeitAttribute(neueAttribute);
	}

	private <T extends LinearReferenzierteAttribute> List<T> updateLinearReferenzierteAttribute(
		List<T> linearReferenzierteAttribute, LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		UnaryOperator<T> updateFunction) {
		List<T> zugeschnitteneAttribute = LinearReferenzierteAttribute.getAufLineareReferenzZugeschnitten(
			linearReferenzierteAttribute, linearReferenzierterAbschnitt);

		return zugeschnitteneAttribute.stream()
			.map(attribute -> {
				if (linearReferenzierterAbschnitt.contains(attribute.getLinearReferenzierterAbschnitt())) {
					return updateFunction.apply(attribute);
				} else {
					return attribute;
				}
			})
			.collect(Collectors.toList());
	}

	// --------------------------------------- FahrtrichtungsAttributGruppe --------------------------------------------
	@SuppressWarnings("deprecation")
	protected void applyFahrtrichtung(Kante kante, Richtung richtung) {
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = kante.getFahrtrichtungAttributGruppe();
		fahrtrichtungAttributGruppe.setRichtung(richtung);
	}

	protected void applyFahrtrichtung(Kante kante, Richtung richtungLinks, Richtung richtungRechts) {
		if (!richtungLinks.equals(richtungRechts) && !kante.isZweiseitig()) {
			kante.changeSeitenbezug(true);
		}
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = kante.getFahrtrichtungAttributGruppe();
		fahrtrichtungAttributGruppe.update(richtungLinks, richtungRechts);
	}

	public boolean shouldFilterNullValues(String attribut) {
		return true;
	}

}
