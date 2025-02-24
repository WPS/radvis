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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.util.Pair;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.exception.AttributUebernahmeException;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.AttributUebernahmeFehler;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedAttributesProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
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
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
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
import de.wps.radvis.backend.organisation.domain.OrganisationsartUndNameNichtEindeutigException;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public class RadVISMapper extends AttributeMapper implements AttributivSeitenbezogenerMapper {
	private static final Set<String> LINEAR_REFERENZIERTE_ATTRIBUTE = Set.of(
		// FuehrungsformAttributGruppe
		"radverkehr",
		"breite",
		"parken_typ",
		"parken_for",
		"bordstein",
		"belag_art",
		"oberflaech",
		"benutzungs",
		"sts_b_l",
		"sts_b_r",
		"sts_f_l",
		"sts_f_r",
		"sts_t_l",
		"sts_t_r",
		"beschilder",
		"absenkung",
		"schaeden",

		// GeschwindigkeitsAttributGruppe
		"ortslage",
		"hoechstges",
		"abweichend",

		// ZustaendigkeitsAttributGruppe
		"baulast_tr",
		"unterhalts",
		"erhalts_zu",
		"vereinbaru");
	private static final Set<String> SEITENBEZOGENE_ATTRIBUTE = Set.of(
		// FuehrungsformAttributGruppe
		"radverkehr",
		"breite",
		"parken_typ",
		"parken_for",
		"bordstein",
		"belag_art",
		"oberflaech",
		"benutzungs",
		"sts_b_l",
		"sts_b_r",
		"sts_f_l",
		"sts_f_r",
		"sts_t_l",
		"sts_t_r",
		"beschilder",
		"absenkung",
		"schaeden",

		// FahrtrichtungsAttributGruppe
		"fahrtricht");
	static final List<String> UNTERSTUETZTE_ATTRIBUTE = List.of(
		// KantenAttributGruppe
		"dtv_fussve",
		"dtv_pkw",
		"dtv_radver",
		"kommentar",
		"laenge_man",
		"strassen_n",
		"strassen_0",
		"sv",
		"wege_nivea",
		"gemeinde_n", // Hier ist klar, dass es von der OrganisationsArt eine Gemeinde ist
		// "landkreis_", Kann nicht importiert werden, wird automatisch aus gemeinde_n errechnet
		"beleuchtun",
		"umfeld",
		"strassenka",
		"strassenqu",
		"status",
		"standards",

		// FuehrungsformAttributGruppe
		"radverkehr",
		"breite",
		"parken_typ",
		"parken_for",
		"bordstein",
		"belag_art",
		"oberflaech",
		"benutzungs",
		"sts_b_l",
		"sts_b_r",
		"sts_f_l",
		"sts_f_r",
		"sts_t_l",
		"sts_t_r",
		"beschilder",
		"absenkung",
		"schaeden",

		// GeschwindigkeitsAttributGruppe
		"ortslage",
		"hoechstges",
		"abweichend",

		// ZustaendigkeitsAttributGruppe
		"baulast_tr",
		"unterhalts",
		"erhalts_zu",
		"vereinbaru",

		// FahrtrichtungsAttributGruppe
		"fahrtricht");
	private final VerwaltungseinheitService verwaltungseinheitService;

	public RadVISMapper(VerwaltungseinheitService verwaltungseinheitService) {
		super();
		this.verwaltungseinheitService = verwaltungseinheitService;
	}

	@Override
	public void applyEinfach(String attributname, String attributwert, Kante kante) throws AttributUebernahmeException {
		require(isAttributNameValid(attributname));
		require(isAttributWertValid(attributname, attributwert));

		switch (attributname.toLowerCase()) {
		// KantenAttributGruppe
		case "dtv_fussve":
			super.applyFussverkehr(kante, VerkehrStaerke.of(attributwert));
			break;
		case "dtv_pkw":
			super.applyPkw(kante, VerkehrStaerke.of(attributwert));
			break;
		case "dtv_radver":
			super.applyRadverkehr(kante, VerkehrStaerke.of(attributwert));
			break;
		case "kommentar":
			super.applyKommentar(kante, Kommentar.of(attributwert));
			break;
		case "laenge_man":
			super.applyLaengeManuell(kante, Laenge.of(attributwert));
			break;
		case "strassen_n":
			super.applyStrassenName(kante, StrassenName.of(attributwert));
			break;
		case "strassen_0":
			super.applyStrassenNummer(kante, StrassenNummer.of(attributwert));
			break;
		case "sv":
			super.applySV(kante, VerkehrStaerke.of(attributwert));
			break;
		case "wege_nivea":
			super.applyWegeniveau(kante, mapStringToEnum(attributwert, WegeNiveau.class));
			break;
		case "gemeinde_n":
			Optional<Verwaltungseinheit> gemeindeOpt;
			try {
				gemeindeOpt = verwaltungseinheitService.getVerwaltungseinheitNachNameUndArt(
					attributwert, OrganisationsArt.GEMEINDE);
			} catch (OrganisationsartUndNameNichtEindeutigException e) {
				throw new AttributUebernahmeException(List.of(new AttributUebernahmeFehler(e.getMessage(),
					Set.of(attributwert))));
			}
			gemeindeOpt.ifPresent(gemeinde -> super.applyGemeinde(kante, gemeinde));
			break;
		case "beleuchtun":
			super.applyBeleuchtung(kante, mapStringToEnum(attributwert, Beleuchtung.class));
			break;
		case "umfeld":
			super.applyUmfeld(kante, mapStringToEnum(attributwert, Umfeld.class));
			break;
		case "strassenka":
			super.applyStrassenkategorieRIN(kante, mapStringToEnum(attributwert, StrassenkategorieRIN.class));
			break;
		case "strassenqu":
			super.applyStrassenquerschnittRASt06(kante, mapStringToEnum(attributwert, StrassenquerschnittRASt06.class));
			break;
		case "status":
			super.applyStatus(kante, mapStringToEnum(attributwert, Status.class));
			break;
		case "standards":
			Set<IstStandard> istStandards = splitValues(attributwert)
				.map(value -> mapStringToEnum(value, IstStandard.class))
				.collect(Collectors.toSet());
			checkIstStandardNetzklassenConsistency(istStandards, kante.getKantenAttributGruppe().getNetzklassen());
			super.applyIstStandards(kante, istStandards);
			break;
		// FuehrungsformAttributGruppe
		case "fahrtricht":
			Richtung richtung = mapStringToEnum(attributwert, Richtung.class);
			super.applyFahrtrichtung(kante, richtung);
			break;
		default:
			throw new RuntimeException("RadVIS-Attribut '" + attributname + "' unbekannt");
		}
	}

	private void checkIstStandardNetzklassenConsistency(Set<IstStandard> istStandards, Set<Netzklasse> netzklassen)
		throws AttributUebernahmeException {
		if (!KantenAttributGruppe.istStandardsAllowedForNetzklassen(netzklassen, istStandards)) {
			throw new AttributUebernahmeException(List.of(new AttributUebernahmeFehler(
				"RadNETZ-IstStandards konnten nicht gesetzt werden: Kante gehört nicht zum RadNETZ",
				istStandards.stream().map(IstStandard::toString).collect(
					Collectors.toSet()),
				LinearReferenzierterAbschnitt.of(0, 1))));
		}
	}

	@Override
	public void applyLinearReferenzierterAbschnitt(String attributname, MappedAttributesProperties attributesProperties,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, Kante kante) throws AttributUebernahmeException {
		require(isAttributNameValid(attributname));
		String attributwert = attributesProperties.getProperty(attributname);
		require(isAttributWertValid(attributname, attributwert));

		switch (attributname.toLowerCase()) {
		// FuehrungsformAttributGruppe
		case "radverkehr":
			// Da die Trennstreifen-Attribute von der Radverkehrsführung abhängen, wird das hier zusammen behandelt.
			applyRadverkehrsfuehrungUndAbhaengigeAttribute(attributesProperties, null, linearReferenzierterAbschnitt,
				kante);
			break;
		case "breite":
			Laenge laenge = Laenge.of(attributwert);
			super.applyBreite(kante, laenge, linearReferenzierterAbschnitt);
			break;
		case "parken_typ":
			KfzParkenTyp kfzParkenTyp = mapStringToEnum(attributwert, KfzParkenTyp.class);
			super.applyKfzParkenTyp(kante, kfzParkenTyp, linearReferenzierterAbschnitt);
			break;
		case "parken_for":
			KfzParkenForm kfzParkenForm = mapStringToEnum(attributwert, KfzParkenForm.class);
			super.applyKfzParkenForm(kante, kfzParkenForm, linearReferenzierterAbschnitt);
			break;
		case "bordstein":
			Bordstein bordstein = mapStringToEnum(attributwert, Bordstein.class);
			super.applyBordstein(kante, bordstein, linearReferenzierterAbschnitt);
			break;
		case "belag_art":
			BelagArt belagArt = mapStringToEnum(attributwert, BelagArt.class);
			super.applyBelagArt(kante, belagArt, linearReferenzierterAbschnitt);
			break;
		case "oberflaech":
			Oberflaechenbeschaffenheit oberflaechenbeschaffenheit = mapStringToEnum(attributwert,
				Oberflaechenbeschaffenheit.class);
			super.applyOberflaechenbeschaffenheit(kante, oberflaechenbeschaffenheit, linearReferenzierterAbschnitt);
			break;
		case "benutzungs":
			Benutzungspflicht benutzungspflicht = mapStringToEnum(attributwert, Benutzungspflicht.class);
			super.applyBenutzungspflicht(kante, benutzungspflicht, linearReferenzierterAbschnitt);
			break;
		case "sts_f_l":
		case "sts_f_r":
		case "sts_b_l":
		case "sts_b_r":
		case "sts_t_l":
		case "sts_t_r":
			// Für alle Trennstreifen-Attribute:
			// Nothing to do here, wird bei "radverkehr" bereits abgehandelt.
			break;
		case "beschilder":
			super.applyBeschilderung(linearReferenzierterAbschnitt, kante,
				mapStringToEnum(attributwert, Beschilderung.class));
			break;
		case "absenkung":
			super.applyAbsenkung(linearReferenzierterAbschnitt, kante, mapStringToEnum(attributwert, Absenkung.class));
			break;
		case "schaeden":
			Set<Schadenart> schaeden = splitValues(attributwert)
				.map(value -> mapStringToEnum(value, Schadenart.class))
				.collect(Collectors.toSet());
			super.applySchaeden(linearReferenzierterAbschnitt, kante, schaeden);
			break;

		// GeschwindigkeitsAttributGruppe
		case "ortslage":
			KantenOrtslage kantenOrtslage = mapStringToEnum(attributwert, KantenOrtslage.class);
			super.applyKantenOrtslage(kante, kantenOrtslage, linearReferenzierterAbschnitt);
			break;
		case "hoechstges":
			Hoechstgeschwindigkeit hoechstgeschwindigkeit = mapStringToEnum(attributwert, Hoechstgeschwindigkeit.class);
			super.applyHoechstgeschwindigkeit(kante, hoechstgeschwindigkeit, linearReferenzierterAbschnitt);
			break;
		case "abweichend":
			Hoechstgeschwindigkeit hoechstgeschwindigkeitGegenStatio = mapStringToEnum(attributwert,
				Hoechstgeschwindigkeit.class);
			super.applyHoechstgeschwindigkeitGegenStationierungsRichtung(kante, hoechstgeschwindigkeitGegenStatio,
				linearReferenzierterAbschnitt);
			break;

		// ZustaendigkeitsAttributGruppe
		case "baulast_tr":
			Optional<Verwaltungseinheit> baulastTrOpt;
			try {
				baulastTrOpt = getVerwaltungseinheit(attributwert);
			} catch (OrganisationsartUndNameNichtEindeutigException e) {
				throw new AttributUebernahmeException(List.of(new AttributUebernahmeFehler(e.getMessage(),
					Set.of(attributwert), linearReferenzierterAbschnitt)));
			}
			baulastTrOpt.ifPresent(
				baulastTr -> super.applyBaulastTraeger(kante, baulastTr, linearReferenzierterAbschnitt));
			break;
		case "unterhalts":
			Optional<Verwaltungseinheit> unterhaltsZustOpt;
			try {
				unterhaltsZustOpt = getVerwaltungseinheit(attributwert);
			} catch (OrganisationsartUndNameNichtEindeutigException e) {
				throw new AttributUebernahmeException(List.of(new AttributUebernahmeFehler(e.getMessage(),
					Set.of(attributwert), linearReferenzierterAbschnitt)));
			}
			unterhaltsZustOpt.ifPresent(
				unterhaltsZust -> super.applyUnterhaltsZustaendiger(kante, unterhaltsZust,
					linearReferenzierterAbschnitt));
			break;
		case "erhalts_zu":
			Optional<Verwaltungseinheit> erhaltsZustOpt;
			try {
				erhaltsZustOpt = getVerwaltungseinheit(attributwert);
			} catch (OrganisationsartUndNameNichtEindeutigException e) {
				throw new AttributUebernahmeException(List.of(new AttributUebernahmeFehler(e.getMessage(),
					Set.of(attributwert), linearReferenzierterAbschnitt)));
			}
			erhaltsZustOpt.ifPresent(
				erhaltsZust -> super.applyErhaltsZustaendiger(kante, erhaltsZust, linearReferenzierterAbschnitt));
			break;
		case "vereinbaru":
			VereinbarungsKennung vereinbarungsKennung = VereinbarungsKennung.of(attributwert);
			super.applyVereinbarungskennung(kante, vereinbarungsKennung, linearReferenzierterAbschnitt);
			break;
		default:
			throw new RuntimeException("LinearReferenziertes RadVIS-Attribut '" + attributname + "' unbekannt");
		}
	}

	@Override
	public void applyLinearReferenzierterAbschnittSeitenbezogen(String attributname,
		MappedAttributesProperties attributesProperties, LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		Seitenbezug seitenbezug, Kante kante) throws AttributUebernahmeException {
		require(isAttributNameValid(attributname));
		String attributwert = attributesProperties.getProperty(attributname);
		require(isAttributWertValid(attributname, attributwert));

		switch (attributname.toLowerCase()) {
		// FuehrungsformAttributGruppe
		case "radverkehr":
			// Da die Trennstreifen-Attribute von der Radverkehrsführung abhängen, wird das hier zusammen behandelt.
			applyRadverkehrsfuehrungUndAbhaengigeAttribute(attributesProperties, seitenbezug,
				linearReferenzierterAbschnitt, kante);
			break;
		case "breite":
			Laenge laenge = Laenge.of(attributwert);
			super.applyBreite(kante, laenge, seitenbezug, linearReferenzierterAbschnitt);
			break;
		case "parken_typ":
			KfzParkenTyp kfzParkenTyp = mapStringToEnum(attributwert, KfzParkenTyp.class);
			super.applyKfzParkenTyp(kante, kfzParkenTyp, seitenbezug, linearReferenzierterAbschnitt);
			break;
		case "parken_for":
			KfzParkenForm kfzParkenForm = mapStringToEnum(attributwert, KfzParkenForm.class);
			super.applyKfzParkenForm(kante, kfzParkenForm, seitenbezug, linearReferenzierterAbschnitt);
			break;
		case "bordstein":
			Bordstein bordstein = mapStringToEnum(attributwert, Bordstein.class);
			super.applyBordstein(kante, bordstein, seitenbezug, linearReferenzierterAbschnitt);
			break;
		case "belag_art":
			BelagArt belagArt = mapStringToEnum(attributwert, BelagArt.class);
			super.applyBelagArt(kante, belagArt, seitenbezug, linearReferenzierterAbschnitt);
			break;
		case "oberflaech":
			Oberflaechenbeschaffenheit oberflaechenbeschaffenheit = mapStringToEnum(attributwert,
				Oberflaechenbeschaffenheit.class);
			super.applyOberflaechenbeschaffenheit(kante, oberflaechenbeschaffenheit, seitenbezug,
				linearReferenzierterAbschnitt);
			break;
		case "benutzungs":
			Benutzungspflicht benutzungspflicht = mapStringToEnum(attributwert, Benutzungspflicht.class);
			super.applyBenutzungspflicht(kante, benutzungspflicht, seitenbezug, linearReferenzierterAbschnitt);
			break;
		case "sts_f_l":
		case "sts_f_r":
		case "sts_b_l":
		case "sts_b_r":
		case "sts_t_l":
		case "sts_t_r":
			// Für alle Trennstreifen-Attribute:
			// Nothing to do here, wird bei "radverkehr" bereits abgehandelt.
			break;
		case "beschilder":
			super.applyBeschilderung(kante,
				mapStringToEnum(attributwert, Beschilderung.class), seitenbezug, linearReferenzierterAbschnitt);
			break;
		case "absenkung":
			super.applyAbsenkung(kante, mapStringToEnum(attributwert, Absenkung.class), seitenbezug,
				linearReferenzierterAbschnitt);
			break;
		case "schaeden":
			Set<Schadenart> schaeden = splitValues(attributwert)
				.map(value -> mapStringToEnum(value, Schadenart.class))
				.collect(Collectors.toSet());
			super.applySchaeden(kante, schaeden, seitenbezug, linearReferenzierterAbschnitt);
			break;
		default:
			throw new RuntimeException(
				"LinearReferenziertes UND Seitenbezogenes RadVIS-Attribut '" + attributname + "' unbekannt");
		}
	}

	@Override
	public void applyBeideSeiten(String attributname, String attributwertLinks, String attributwertRechts,
		Kante kante) {
		switch (attributname.toLowerCase()) {
		case "fahrtricht":
			Richtung richtungLinks = mapStringToEnum(attributwertLinks, Richtung.class);
			Richtung richtungRechts = mapStringToEnum(attributwertRechts, Richtung.class);
			super.applyFahrtrichtung(kante, richtungLinks, richtungRechts);
			break;
		default:
			throw new RuntimeException("RadVIS-Attribut '" + attributname + "' unbekannt");
		}
	}

	@Override
	public boolean isAttributSeitenbezogen(String attributname) {
		return SEITENBEZOGENE_ATTRIBUTE.contains(attributname.toLowerCase());

	}

	@Override
	public boolean isLinearReferenziert(String attributname) {
		return LINEAR_REFERENZIERTE_ATTRIBUTE.contains(attributname.toLowerCase());
	}

	@Override
	protected String getUmgekehrteRichtung(String wertFuerRichtung) {
		require(isValidValueForEnum(wertFuerRichtung, Richtung.class));
		Richtung richtung = mapStringToEnum(wertFuerRichtung, Richtung.class);
		return richtung.umgedreht().name();
	}

	@Override
	public boolean isRichtung(String attributname) {
		return attributname.equalsIgnoreCase("fahrtricht");
	}

	@Override
	public String getSeiteAttributName() {
		return "seite";
	}

	@Override
	public Optional<Seitenbezug> mapSeiteIfPresentAndValid(String seiteAttribut) {
		if (seiteAttribut == null || !isValidValueForEnum(seiteAttribut, Seitenbezug.class)) {
			return Optional.empty();
		}
		return Optional.of(mapStringToEnum(seiteAttribut, Seitenbezug.class));
	}

	private void applyRadverkehrsfuehrungUndAbhaengigeAttribute(MappedAttributesProperties attributesProperties,
		Seitenbezug seitenbezug, LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, Kante kante)
		throws AttributUebernahmeException {

		Radverkehrsfuehrung radverkehrsfuehrung = mapStringToEnum(
			attributesProperties.getPropertyOrElse("radverkehr", "UNBEKANNT"), Radverkehrsfuehrung.class);

		// Linker Trennstreifen
		TrennstreifenForm trennstreifenFormLinks = mapStringToNullableEnum(
			attributesProperties.getPropertyOrElse("sts_f_l", null), TrennstreifenForm.class);
		Laenge trennstreifenBreiteLinks = Laenge.of(attributesProperties.getPropertyOrElse("sts_b_l", ""));
		TrennungZu trennungZuLinks = mapStringToNullableEnum(attributesProperties.getPropertyOrElse("sts_t_l", null),
			TrennungZu.class);

		// Rechter Trennstreifen
		TrennstreifenForm trennstreifenFormRechts = mapStringToNullableEnum(
			attributesProperties.getPropertyOrElse("sts_f_r", null), TrennstreifenForm.class);
		Laenge trennstreifenBreiteRechts = Laenge.of(attributesProperties.getPropertyOrElse("sts_b_r", ""));
		TrennungZu trennungZuRechts = mapStringToNullableEnum(attributesProperties.getPropertyOrElse("sts_t_r", null),
			TrennungZu.class);

		if (seitenbezug != null) {
			super.applyRadverkehrsfuehrungUndTrennstreifen(kante, radverkehrsfuehrung,
				trennstreifenFormLinks, trennstreifenBreiteLinks, trennungZuLinks,
				trennstreifenFormRechts, trennstreifenBreiteRechts, trennungZuRechts,
				seitenbezug, linearReferenzierterAbschnitt);
		} else {
			super.applyRadverkehrsfuehrungUndTrennstreifen(kante, radverkehrsfuehrung,
				trennstreifenFormLinks, trennstreifenBreiteLinks, trennungZuLinks,
				trennstreifenFormRechts, trennstreifenBreiteRechts, trennungZuRechts,
				linearReferenzierterAbschnitt);
		}
	}

	@Override
	public boolean isAttributNameValid(String attributName) {
		return UNTERSTUETZTE_ATTRIBUTE.contains(attributName.toLowerCase());
	}

	@Override
	public boolean isAttributWertValid(String attributName, String attributWert) {
		require(isAttributNameValid(attributName));
		if (attributWert == null || attributWert.isEmpty()) {
			return true;
		}
		switch (attributName.toLowerCase()) {
		case "dtv_fussve":
			return VerkehrStaerke.isValid(attributWert);
		case "dtv_pkw":
			return VerkehrStaerke.isValid(attributWert);
		case "dtv_radver":
			return VerkehrStaerke.isValid(attributWert);
		case "kommentar":
			return Kommentar.isValid(attributWert);
		case "laenge_man":
			return Laenge.isValid(attributWert);
		case "strassen_n":
			return StrassenName.isValid(attributWert);
		case "strassen_0":
			return StrassenNummer.isValid(attributWert);
		case "sv":
			return VerkehrStaerke.isValid(attributWert);
		case "wege_nivea":
			return isValidValueForEnum(attributWert, WegeNiveau.class);
		case "gemeinde_n":
			return verwaltungseinheitService.hasVerwaltungseinheitNachNameUndArt(
				attributWert, OrganisationsArt.GEMEINDE);
		case "beleuchtun":
			return isValidValueForEnum(attributWert, Beleuchtung.class);
		case "umfeld":
			return isValidValueForEnum(attributWert, Umfeld.class);
		case "strassenka":
			return isValidValueForEnum(attributWert, StrassenkategorieRIN.class);
		case "strassenqu":
			return isValidValueForEnum(attributWert, StrassenquerschnittRASt06.class);
		case "status":
			return isValidValueForEnum(attributWert, Status.class);
		case "standards":
			return splitValues(attributWert)
				.allMatch(standard -> isValidValueForEnum(standard, IstStandard.class));
		case "radverkehr":
			return isValidValueForEnum(attributWert, Radverkehrsfuehrung.class);
		case "breite":
			return Laenge.isValid(attributWert);
		case "parken_typ":
			return isValidValueForEnum(attributWert, KfzParkenTyp.class);
		case "parken_for":
			return isValidValueForEnum(attributWert, KfzParkenForm.class);
		case "bordstein":
			return isValidValueForEnum(attributWert, Bordstein.class);
		case "belag_art":
			return isValidValueForEnum(attributWert, BelagArt.class);
		case "oberflaech":
			return isValidValueForEnum(attributWert, Oberflaechenbeschaffenheit.class);
		case "benutzungs":
			return isValidValueForEnum(attributWert, Benutzungspflicht.class);
		case "ortslage":
			return isValidValueForEnum(attributWert, KantenOrtslage.class);
		case "hoechstges":
			return isValidValueForEnum(attributWert, Hoechstgeschwindigkeit.class);
		case "abweichend":
			return isValidValueForEnum(attributWert, Hoechstgeschwindigkeit.class);
		case "baulast_tr":
			return isVerwaltungseinheitValid(attributWert);
		case "unterhalts":
			return isVerwaltungseinheitValid(attributWert);
		case "erhalts_zu":
			return isVerwaltungseinheitValid(attributWert);
		case "vereinbaru":
			return VereinbarungsKennung.isValid(attributWert);
		case "fahrtricht":
			return isValidValueForEnum(attributWert, Richtung.class);
		case "sts_b_l":
			return Laenge.isValid(attributWert);
		case "sts_b_r":
			return Laenge.isValid(attributWert);
		case "sts_f_l":
			return isValidValueForEnum(attributWert, TrennstreifenForm.class);
		case "sts_f_r":
			return isValidValueForEnum(attributWert, TrennstreifenForm.class);
		case "sts_t_l":
			return isValidValueForEnum(attributWert, TrennungZu.class);
		case "sts_t_r":
			return isValidValueForEnum(attributWert, TrennungZu.class);
		case "beschilder":
			return isValidValueForEnum(attributWert, Beschilderung.class);
		case "absenkung":
			return isValidValueForEnum(attributWert, Absenkung.class);
		case "schaeden":
			return splitValues(attributWert)
				.allMatch(schaden -> isValidValueForEnum(schaden, Schadenart.class));
		default:
			throw new UnsupportedOperationException("isAttributWertValid not implemented for " + attributName);
		}
	}

	@Override
	public String getRadVisAttributName(String importedAttributName) {
		require(isAttributNameValid(importedAttributName));
		switch (importedAttributName.toLowerCase()) {
		case "dtv_fussve":
			return "DTV (Fußverkehr)";
		case "dtv_pkw":
			return "DTV (PkW)";
		case "dtv_radver":
			return "DTV (Radverkehr)";
		case "kommentar":
			return "Kommentar";
		case "laenge_man":
			return "Länge (manuell)";
		case "strassen_n":
			return "Straßenname";
		case "strassen_0":
			return "Straßennummer";
		case "sv":
			return "Schwerverkehr";
		case "wege_nivea":
			return "Wegeniveau";
		case "gemeinde_n":
			return "Gemeinde";
		case "beleuchtun":
			return "Beleuchtung";
		case "umfeld":
			return "Umfeld";
		case "strassenka":
			return "Straßenkategorie nach RIN";
		case "strassenqu":
			return "Straßenquerschnitte nach RASt 06";
		case "status":
			return "Status";
		case "standards":
			return "Ist-Standards";
		case "radverkehr":
			return "Radverkehrsführung";
		case "breite":
			return "Breite";
		case "parken_typ":
			return "Kfz-Parken-Typ";
		case "parken_for":
			return "Kfz-Parken-Form";
		case "bordstein":
			return "Bordstein";
		case "belag_art":
			return "Belagart";
		case "oberflaech":
			return "Oberflächenbeschaffenheit";
		case "benutzungs":
			return "Benutzungspflicht";
		case "ortslage":
			return "Ortslage";
		case "hoechstges":
			return "Höchstgeschwindigkeit";
		case "abweichend":
			return "Abweichende Höchstgeschwindigkeit gegen Stationierungsrichtung";
		case "baulast_tr":
			return "Baulastträger";
		case "unterhalts":
			return "Unterhaltszuständiger";
		case "erhalts_zu":
			return "Erhaltszuständiger";
		case "vereinbaru":
			return "Vereinbarungskennung";
		case "fahrtricht":
			return "Fahrtrichtung";
		case "sts_b_l":
			return "Sicherheitstrennstreifen Breite Links";
		case "sts_b_r":
			return "Sicherheitstrennstreifen Breite Rechts";
		case "sts_f_l":
			return "Sicherheitstrennstreifen Form Links";
		case "sts_f_r":
			return "Sicherheitstrennstreifen Form Rechts";
		case "sts_t_l":
			return "Sicherheitstrennstreifen Trennung Links";
		case "sts_t_r":
			return "Sicherheitstrennstreifen Trennung Rechts";
		case "beschilder":
			return "Beschilderung";
		case "absenkung":
			return "Absenkung";
		case "schaeden":
			return "Vorhandene Schäden";
		default:
			throw new UnsupportedOperationException(
				"getRadVisAttributName not implemented for " + importedAttributName);
		}
	}

	@Override
	public int sortAttribute(String attribut1, String attribut2) {
		require(isAttributNameValid(attribut1));
		require(isAttributNameValid(attribut2));

		// Wir importieren die Radverkehrsfuehrung zuerst, damit Validierungs-Konflikte mit abhängigen Feldern wie
		// Beschilderung korrekt aufgelöst werden.
		if (attribut1.equals("radverkehr")) {
			return -1;
		}

		if (attribut2.equals("radverkehr")) {
			return 1;
		}

		return 0;
	}

	/**
	 * false, wenn parsing nicht klappt
	 * 
	 * @param orgaNameUndArt
	 * @return
	 * @throws OrganisationsartUndNameNichtEindeutigException
	 */
	private boolean isVerwaltungseinheitValid(String orgaNameUndArt) {
		Optional<Pair<String, OrganisationsArt>> organisationsArtPairOpt = Verwaltungseinheit
			.parseBezeichnungWithOrgaArtAllCaps(orgaNameUndArt);
		if (organisationsArtPairOpt.isEmpty()) {
			return false;
		}

		return verwaltungseinheitService.hasVerwaltungseinheitNachNameUndArt(
			organisationsArtPairOpt.get().getFirst(), organisationsArtPairOpt.get().getSecond());
	}

	private Optional<Verwaltungseinheit> getVerwaltungseinheit(String orgaNameUndArt)
		throws OrganisationsartUndNameNichtEindeutigException {
		Optional<Pair<String, OrganisationsArt>> organisationsArtPairOpt = Verwaltungseinheit
			.parseBezeichnungWithOrgaArtAllCaps(orgaNameUndArt);
		if (organisationsArtPairOpt.isEmpty()) {
			return Optional.empty();
		}

		return verwaltungseinheitService.getVerwaltungseinheitNachNameUndArt(
			organisationsArtPairOpt.get().getFirst(), organisationsArtPairOpt.get().getSecond());
	}

	private static <T extends Enum<T>> T mapStringToEnum(String value, Class<T> enumClass) {
		require(isValidValueForEnum(value, enumClass));
		require(value, notNullValue());
		return Enum.valueOf(enumClass, value);
	}

	private static <T extends Enum<T>> T mapStringToNullableEnum(String value, Class<T> enumClass) {
		if (value == null || !isValidValueForEnum(value, enumClass)) {
			return null;
		}
		return Enum.valueOf(enumClass, value);
	}

	private static <T extends Enum<T>> boolean isValidValueForEnum(String attributWert, Class<T> enumClass) {
		return Arrays.stream(enumClass.getEnumConstants())
			.map(Enum::name)
			.collect(Collectors.toSet()).contains(attributWert);
	}

	private static Stream<String> splitValues(String attributwert) {
		return Arrays.stream(attributwert.split(";"))
			.map(String::strip);
	}
}