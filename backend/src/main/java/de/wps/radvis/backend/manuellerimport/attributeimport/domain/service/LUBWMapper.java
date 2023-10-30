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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.springframework.data.util.Pair;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.exception.AttributUebernahmeException;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.AttributUebernahmeFehler;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedAttributesProperties;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LUBWMapper extends AttributeMapper {

	@Override
	public void applyEinfach(String attributname, String attributwert, Kante kante) {
		switch (attributname.toLowerCase()) {
		case "beleuchtun":
			Beleuchtung beleuchtung = mapBeleuchtung(attributwert);
			super.applyBeleuchtung(kante, beleuchtung);
			break;
		case "richtung":
			Richtung richtung = mapRichtung(attributwert);
			super.applyFahrtrichtung(kante, richtung, richtung);
			break;
		default:
			throw new RuntimeException("LUBW-Attribut '" + attributname + "' unbekannt");
		}
	}

	@Override
	public void applyBeideSeiten(String attributname, String attributwertLinks, String attributwertRechts,
		Kante kante) {
		switch (attributname.toLowerCase()) {
		case "richtung":
			Richtung richtungLinks = mapRichtung(attributwertLinks);
			Richtung richtungRechts = mapRichtung(attributwertRechts);
			super.applyFahrtrichtung(kante, richtungLinks, richtungRechts);
			break;
		default:
			throw new RuntimeException("LUBW-Attribut '" + attributname + "' unbekannt");
		}
	}

	@Override
	public void applyLinearReferenzierterAbschnitt(String attributname, MappedAttributesProperties attributesProperties,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, Kante kante) throws AttributUebernahmeException {
		String attributwert = attributesProperties.getProperty(attributname);
		switch (attributname.toLowerCase()) {
		case "vereinbaru":
			VereinbarungsKennung vereinbarungsKennung = mapVereinbarkennung(attributwert);
			super.applyVereinbarungskennung(kante, vereinbarungsKennung, linearReferenzierterAbschnitt);
			break;
		case "belag":
			BelagArt belagArt = mapBelagArt(attributwert);
			super.applyBelagArt(kante, belagArt, linearReferenzierterAbschnitt);
			break;
		case "breite":
			Laenge breite = mapBreite(attributwert);
			super.applyBreite(kante, breite, linearReferenzierterAbschnitt);
			break;
		case "wegart":
			Radverkehrsfuehrung radverkehrsfuehrung = mapRadverkehrsfuehrung(attributwert);
			super.applyRadverkehrsfuehrung(kante, radverkehrsfuehrung, linearReferenzierterAbschnitt);
			break;
		case "st":
			mapTrennstreifenInfo(attributesProperties, linearReferenzierterAbschnitt, kante, null);
			break;
		default:
			throw new RuntimeException("LinearReferenziertes LUBW-Attribut '" + attributname + "' unbekannt");
		}
	}

	private void mapTrennstreifenInfo(MappedAttributesProperties attributesProperties,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, Kante kante, Seitenbezug seitenbezug)
		throws AttributUebernahmeException {
		TrennstreifenForm trennstreifenForm = mapTrennstreifenForm(attributesProperties);
		Laenge trennstreifenBreite = mapTrennstreifenBreite(attributesProperties);
		TrennungZu trennungZu = mapTrennungZu(attributesProperties.getProperty("st"));

		List<Pair<FuehrungsformAttribute, Seitenbezug>> aufLineareReferenzZugeschnitten = new ArrayList<>();

		if (seitenbezug == Seitenbezug.RECHTS) {
			LinearReferenzierteAttribute.getIntersecting(linearReferenzierterAbschnitt,
					kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.forEach(fa -> aufLineareReferenzZugeschnitten.add(Pair.of(fa, Seitenbezug.RECHTS)));
		} else if (seitenbezug == Seitenbezug.LINKS) {
			LinearReferenzierteAttribute.getIntersecting(linearReferenzierterAbschnitt,
					kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.forEach(fa -> aufLineareReferenzZugeschnitten.add(Pair.of(fa, Seitenbezug.LINKS)));
		} else {
			LinearReferenzierteAttribute.getIntersecting(linearReferenzierterAbschnitt,
					kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.forEach(fa -> aufLineareReferenzZugeschnitten.add(Pair.of(fa, Seitenbezug.RECHTS)));
			LinearReferenzierteAttribute.getIntersecting(linearReferenzierterAbschnitt,
					kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.forEach(fa -> aufLineareReferenzZugeschnitten.add(Pair.of(fa, Seitenbezug.LINKS)));
		}

		List<Pair<FuehrungsformAttribute, Seitenbezug>> valideAbschnitte = aufLineareReferenzZugeschnitten.stream()
			.filter(pairFaSeitenbezug ->
				FuehrungsformAttribute.isTrennstreifenCorrect(
					pairFaSeitenbezug.getFirst().getRadverkehrsfuehrung(),
					trennstreifenForm,
					trennstreifenBreite,
					trennungZu)
			).toList();

		if (!valideAbschnitte.isEmpty() && !Objects.isNull(trennstreifenForm)) {
			valideAbschnitte.forEach(
				pairFaSeitenbezug -> {
					if (!Objects.isNull(seitenbezug) || kante.isZweiseitig()) {
						Trennstreifenseite trennstreifenseite = mapTrennstreifenseite(pairFaSeitenbezug.getFirst()
							.getRadverkehrsfuehrung(), attributesProperties.getProperty("st"));

						if (trennstreifenseite == Trennstreifenseite.UNBEKANNT) {
							// Bei unbekannt, loggen und auf links schreiben
							log.warn(
								"Trennstreifenseite nicht ermittelbar für Kante mit Id {} auf LRS {}. Schreibe Trennstreifen links",
								kante.getId(),
								pairFaSeitenbezug.getFirst().getLinearReferenzierterAbschnitt());
							super.applyTrennstreifenInfoLinks(kante, trennstreifenForm, trennstreifenBreite,
								trennungZu, pairFaSeitenbezug.getSecond(),
								pairFaSeitenbezug.getFirst().getLinearReferenzierterAbschnitt());
						} else {
							boolean trennstreifenLinks = isTrennstreifenLinks(pairFaSeitenbezug.getSecond(),
								trennstreifenseite);
							log.info(
								"Trennstreifenseite ermittelbar für Kante mit Id {} auf LRS {}. Schreibe Trennstreifen {}",
								kante.getId(),
								pairFaSeitenbezug.getFirst().getLinearReferenzierterAbschnitt(),
								trennstreifenLinks ? "links" : "rechts");
							if (trennstreifenLinks) {
								super.applyTrennstreifenInfoLinks(kante, trennstreifenForm, trennstreifenBreite,
									trennungZu, pairFaSeitenbezug.getSecond(),
									pairFaSeitenbezug.getFirst().getLinearReferenzierterAbschnitt());
							} else {
								super.applyTrennstreifenInfoRechts(kante, trennstreifenForm, trennstreifenBreite,
									trennungZu, pairFaSeitenbezug.getSecond(),
									pairFaSeitenbezug.getFirst().getLinearReferenzierterAbschnitt());
							}
						}

					} else {
						// Bei fehlendem Seitenbezug immer links schreiben und loggen
						log.warn(
							"Trennstreifenseite nicht ermittelbar ohne Seitenbezug für Kante mit Id {} auf LRS {}. Schreibe Trennstreifen links",
							kante.getId(),
							pairFaSeitenbezug.getFirst().getLinearReferenzierterAbschnitt());
						super.applyTrennstreifenInfoLinks(kante, trennstreifenForm, trennstreifenBreite,
							trennungZu, pairFaSeitenbezug.getFirst().getLinearReferenzierterAbschnitt());
					}
				});
		}

		if (!Objects.isNull(trennstreifenBreite) || !Objects.isNull(trennungZu) || (
			!Objects.isNull(trennstreifenForm) && trennstreifenForm != TrennstreifenForm.UNBEKANNT)) {

			List<Pair<FuehrungsformAttribute, Seitenbezug>> nichtValideAbschnitteGroesser1m = aufLineareReferenzZugeschnitten.stream()
				.filter(
					pairFaSeitenbezug ->
						pairFaSeitenbezug.getFirst().getLinearReferenzierterAbschnitt().relativeLaenge()
							* kante.getLaengeBerechnet().getValue() > 1
							&& !valideAbschnitte.contains(pairFaSeitenbezug))
				.toList();

			if (nichtValideAbschnitteGroesser1m.isEmpty()) {
				return;
			}

			Collection<FuehrungsformAttribute> nichtValide;

			if (!Objects.isNull(seitenbezug) || kante.isZweiseitig()) {
				nichtValide = nichtValideAbschnitteGroesser1m.stream().map(Pair::getFirst).collect(Collectors.toList());
			} else {
				nichtValide = nichtValideAbschnitteGroesser1m.stream().map(Pair::getFirst).collect(Collectors.toSet());
			}

			List<AttributUebernahmeFehler> fehler = nichtValide.stream()
				.map(fa ->
					new AttributUebernahmeFehler(
						String.format("""
								Es konnten keine TrennstreifenInformationen geschrieben werden.
								Radverkehrsführung: %s
								KanteId: %s""",
							fa.getRadverkehrsfuehrung(),
							kante.getId()
						),
						Set.of(
							"TrennstreifenForm: " + trennstreifenForm,
							"TrennstreifenBreite: " + trennstreifenBreite,
							"TrennungZu: " + trennungZu),
						fa.getLinearReferenzierterAbschnitt()
					))
				.toList();

			throw new AttributUebernahmeException(fehler);

		}
	}

	private static boolean isTrennstreifenLinks(Seitenbezug seitenbezug,
		Trennstreifenseite trennstreifenseite) {
		require(trennstreifenseite != Trennstreifenseite.UNBEKANNT);
		boolean links;
		if (seitenbezug == Seitenbezug.LINKS) {
			links = trennstreifenseite == Trennstreifenseite.GEHWEG;
		} else {
			links = trennstreifenseite == Trennstreifenseite.FAHRBAHN;
		}
		return links;
	}

	private Trennstreifenseite mapTrennstreifenseite(Radverkehrsfuehrung radverkehrsfuehrung,
		String stProperty) {

		switch (stProperty) {
		case "Sicherheitstrennstreifen innerorts ohne Parken":
		case "Sicherheitstrennstreifen außerorts":
			return Trennstreifenseite.FAHRBAHN;
		case "Sicherheitstrennstreifen innerorts mit Längsparken":
		case "Sicherheitstrennstreifen innerorts mit Schräg-/Senkrechtparken":
			switch (radverkehrsfuehrung) {
			case RADFAHRSTREIFEN:
			case RADFAHRSTREIFEN_MIT_FREIGABE_BUSVERKEHR:
			case SCHUTZSTREIFEN:
				return Trennstreifenseite.GEHWEG;
			case SONDERWEG_RADWEG_STRASSENBEGLEITEND:
			case GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND:
			case GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND:
			case GEHWEG_RAD_FREI_STRASSENBEGLEITEND:
			case GEM_RAD_GEHWEG_MIT_GEHWEG_GEGENRICHTUNG_FREI_STRASSENBEGLEITEND:
			case BETRIEBSWEG_LANDWIRDSCHAFT_STRASSENBEGLEITEND:
				return Trennstreifenseite.FAHRBAHN;
			default:
				return Trennstreifenseite.UNBEKANNT;
			}
		default:
			return Trennstreifenseite.UNBEKANNT;

		}

	}

	private TrennungZu mapTrennungZu(String stProperty) {
		return switch (stProperty) {
			case "Sicherheitstrennstreifen innerorts ohne Parken",
				"Sicherheitstrennstreifen außerorts" -> TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN;
			case "Sicherheitstrennstreifen innerorts mit Längsparken",
				"Sicherheitstrennstreifen innerorts mit Schräg-/Senkrechtparken" ->
				TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN;
			default -> null;
		};
	}

	public Laenge mapTrennstreifenBreite(MappedAttributesProperties attributesProperties) {
		Laenge result;
		String breitst2 = attributesProperties.getProperty("breitst2");
		try {
			result = Laenge.of(Integer.parseInt(breitst2) / 100.0);
		} catch (NumberFormatException e) {
			String breitst = attributesProperties.getProperty("breitst");
			Matcher matcher = Pattern.compile(
				"(:?< )?(\\d+(:?,\\d+)?)(:?.*)?").matcher(breitst);
			if (matcher.find()) {
				double value = Double.parseDouble(matcher.group(2).replace(",", "."));
				result = Laenge.of(value);
			} else {
				result = null;
			}

		}
		return result;
	}

	@Override
	public void applyLinearReferenzierterAbschnittSeitenbezogen(String attributname,
		MappedAttributesProperties attributesProperties,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
		Seitenbezug seitenbezug,
		Kante kante) throws AttributUebernahmeException {
		String attributwert = attributesProperties.getProperty(attributname);
		switch (attributname.toLowerCase()) {
		case "belag":
			BelagArt belagArt = mapBelagArt(attributwert);
			super.applyBelagArt(kante, belagArt, seitenbezug, linearReferenzierterAbschnitt);
			break;
		case "breite":
			Laenge breite = mapBreite(attributwert);
			super.applyBreite(kante, breite, seitenbezug, linearReferenzierterAbschnitt);
			break;
		case "wegart":
			Radverkehrsfuehrung radverkehrsfuehrung = mapRadverkehrsfuehrung(attributwert);
			super.applyRadverkehrsfuehrung(kante, radverkehrsfuehrung, seitenbezug, linearReferenzierterAbschnitt);
			break;
		case "st":
			mapTrennstreifenInfo(attributesProperties, linearReferenzierterAbschnitt, kante, seitenbezug);
			break;
		default:
			throw new RuntimeException("LinearReferenziertes LUBW-Attribut '" + attributname + "' unbekannt");
		}
	}

	private TrennstreifenForm mapTrennstreifenForm(MappedAttributesProperties attributesProperties) {
		String stProperty = attributesProperties.getProperty("ST");

		if (Objects.isNull(stProperty) || stProperty.isBlank()) {
			return TrennstreifenForm.UNBEKANNT;
		}

		String breitstProperty = attributesProperties.getProperty("breitst");

		if (!Objects.isNull(breitstProperty)) {
			switch (breitstProperty) {
			case "Kein Sicherheitstrennstreifen, aber Bordstein (Hochbord)":
			case "Kein Sicherheitstrennstreifen, aber andere Abgrenzung (z.B. Rinne zw. Fahrbahn und Anlage)":
				return TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART;
			}
		}

		boolean innerorts = attributesProperties.getProperty("ORTSLAGE").equals("Innerorts");
		if (innerorts) {
			switch (stProperty) {
			case "Kein Sicherheitstrennstreifen vorhanden":
				return TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN;
			case "Sicherheitstrennstreifen innerorts ohne Parken":
			case "Sicherheitstrennstreifen innerorts mit Längsparken":
			case "Sicherheitstrennstreifen innerorts mit Schräg-/Senkrechtparken":
				return TrennstreifenForm.TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG;
			default:
				return null;
			}
		} else {
			switch (stProperty) {
			case "Kein Sicherheitstrennstreifen vorhanden":
				return TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN;
			case "Sicherheitstrennstreifen außerorts":
				return TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN;
			default:
				return null;
			}
		}
	}

	@Override
	public boolean isAttributSeitenbezogen(String attributname) {
		switch (attributname.toLowerCase()) {
		case "richtung":
		case "belag":
		case "breite":
		case "wegart":
		case "st":
			return true;
		default:
			return false;
		}
	}

	@Override
	public boolean isLinearReferenziert(String attributname) {
		switch (attributname.toLowerCase()) {
		case "vereinbaru":
		case "belag":
		case "breite":
		case "wegart":
		case "st":
			return true;
		default:
			return false;
		}
	}

	@Override
	protected String getUmgekehrteRichtung(String wertFuerRichtung) {
		switch (wertFuerRichtung) {
		case "2":
			return "3";
		case "3":
			return "2";
		case "0":
		case "1":
			return wertFuerRichtung;
		default:
			throw new RuntimeException("'" + wertFuerRichtung + "' ist kein gültiger Wert für LUBW-Richtung");
		}
	}

	@Override
	public boolean isRichtung(String attributname) {
		return attributname.equalsIgnoreCase("richtung");
	}

	final Set<String> belagArten = createAllowedBelagarten();

	private static Set<String> createAllowedBelagarten() {
		Set<String> ungebundeneDeckeCodes = IntStream.rangeClosed(50, 72).mapToObj(String::valueOf)
			.collect(Collectors.toSet());
		Set<String> otherCodes = Set.of("0", "10", "20", "31", "33", "32", "30", "40", "80");
		Set<String> resultSet = new HashSet<String>();
		resultSet.addAll(ungebundeneDeckeCodes);
		resultSet.addAll(otherCodes);
		return resultSet;
	}

	BelagArt mapBelagArt(String belag) {
		require(isAttributWertValid("belag", belag));
		switch (belag) {
		case "0":
			return BelagArt.UNBEKANNT;
		case "10":
			return BelagArt.ASPHALT;
		case "20":
			return BelagArt.BETON;
		case "31":
			return BelagArt.NATURSTEINPFLASTER;
		case "30":
		case "32":
		case "33":
			return BelagArt.BETONSTEINPFLASTER_PLATTENBELAG;
		case "40":
			return BelagArt.WASSERGEBUNDENE_DECKE;
		case "80":
			return BelagArt.SONSTIGER_BELAG;
		default:
			if (isParsable(belag) && Integer.parseInt(belag) >= 50 && Integer.parseInt(belag) <= 72) {
				return BelagArt.UNGEBUNDENE_DECKE;
			} else {
				throw new RuntimeException(
					"Der Wert '" + belag + "' für das Attribut Belagart konnte nicht gemappt werden");
			}
		}
	}

	private VereinbarungsKennung mapVereinbarkennung(String vereinbarung) {
		require(isAttributWertValid("vereinbaru", vereinbarung));
		if (vereinbarung.isEmpty() || vereinbarung.isBlank()) {
			return null;
		} else {
			Set.of();
			return VereinbarungsKennung.of(vereinbarung);
		}
	}

	final Set<String> richtungen = Set.of("0", "1", "2", "3");

	Richtung mapRichtung(String richtung) {
		require(isAttributWertValid("richtung", richtung));
		switch (richtung) {
		case "0":
			return Richtung.defaultWert();
		case "1":
			return Richtung.BEIDE_RICHTUNGEN;
		case "2":
			return Richtung.IN_RICHTUNG;
		case "3":
			return Richtung.GEGEN_RICHTUNG;
		default:
			throw new RuntimeException(
				"Der Wert '" + richtung + "' für das LUBW Attribut Richtung konnte nicht gemappt werden");
		}
	}

	final Set<String> beleuchtungen = Set.of("1", "2", "0", "3", "10", "22");

	Beleuchtung mapBeleuchtung(String beleuchtung) {
		require(isAttributWertValid("beleuchtun", beleuchtung));
		switch (beleuchtung) {
		case "1":
			return Beleuchtung.VORHANDEN;
		case "2":
			return Beleuchtung.NICHT_VORHANDEN;
		case "0":
		case "3":
		case "10":
		case "22":
			return Beleuchtung.UNBEKANNT;
		default:
			throw new RuntimeException(
				"Der Wert '" + beleuchtung + "' für das LUBW Attribut Beleuchtung konnte nicht gemappt werden");
		}
	}

	final Set<String> radverkehrsfuehrungen = Stream.of(
		"110", "121", "122", "130", "210", "221", "222", "230", "310", "320", "330", "370", "372", "373", "374", "3737",
		"371", "360", "350", "340", "500", "510", "520", "0", "100", "120", "200", "220", "300", "343", "400", "410",
		"411", "412", "420").collect(Collectors.toSet());

	Radverkehrsfuehrung mapRadverkehrsfuehrung(String wegart) {
		switch (wegart) {
		case "110":
			return Radverkehrsfuehrung.SONDERWEG_RADWEG_SELBSTSTAENDIG;
		case "121":
			return Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_SELBSTSTAENDIG;
		case "122":
			return Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_SELBSTSTAENDIG;
		case "130":
			return Radverkehrsfuehrung.SONSTIGER_BETRIEBSWEG;
		case "210":
			return Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND;
		case "221":
			return Radverkehrsfuehrung.GEH_RADWEG_GETRENNT_STRASSENBEGLEITEND;
		case "222":
			return Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND;
		case "230":
			return Radverkehrsfuehrung.GEHWEG_RAD_FREI_STRASSENBEGLEITEND;
		case "310":
			return Radverkehrsfuehrung.RADFAHRSTREIFEN;
		case "320":
			return Radverkehrsfuehrung.SCHUTZSTREIFEN;
		case "330":
			return Radverkehrsfuehrung.BUSFAHRSTREIFEN_MIT_FREIGABE_RADVERKEHR;
		case "370":
		case "372":
		case "373":
		case "374":
		case "3737":
			return Radverkehrsfuehrung.FUEHRUNG_AUF_FAHRBAHN_ZWEISTREIFIGE_FAHRBAHN;
		case "371":
			return Radverkehrsfuehrung.FUEHRUNG_IN_T30_ZONE;
		case "360":
			return Radverkehrsfuehrung.FUEHRUNG_IN_VERKEHRSBERUHIGTER_BEREICH;
		case "350":
			return Radverkehrsfuehrung.FUEHRUNG_IN_FUSSG_ZONE_RAD_FREI;
		case "340":
			return Radverkehrsfuehrung.FUEHRUNG_IN_FAHRRADSTRASSE;
		case "500":
		case "510":
		case "520":
			return Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG;
		case "0":
		case "100":
		case "120":
		case "200":
		case "220":
		case "300":
		case "343":
		case "400":
		case "410":
		case "411":
		case "412":
		case "420":
			return Radverkehrsfuehrung.UNBEKANNT;
		default:
			throw new RuntimeException(
				"Der Wert '" + wegart + "' für das Attribut Radverkehrsfuehrung konnte nicht gemappt werden");
		}
	}

	final Set<String> breiten = Stream.of(
		"1", "2", "3", "4", "5", "6", "7", "10", "11", "0").collect(Collectors.toSet());

	final Set<String> st = Stream.of(
			"Kein Sicherheitstrennstreifen vorhanden",
			"Sicherheitstrennstreifen außerorts",
			"Sicherheitstrennstreifen innerorts ohne Parken",
			"Sicherheitstrennstreifen innerorts mit Längsparken",
			"Sicherheitstrennstreifen innerorts mit Schräg-/Senkrechtparken")
		.collect(Collectors.toSet());

	final Set<String> breitst = Stream.of(
			"Kein Sicherheitstrennstreifen, aber Bordstein (Hochbord)",
			"Kein Sicherheitstrennstreifen, aber andere Abgrenzung (z.B. Rinne zw. Fahrbahn und Anlage)")
		.collect(Collectors.toSet());

	Laenge mapBreite(String breite) {
		switch (breite) {
		case "1":
			return Laenge.of(1.49);
		case "2":
			return Laenge.of(1.5);
		case "3":
			return Laenge.of(2.5);
		case "4":
		case "5":
		case "6":
		case "7":
		case "10":
		case "11":
		case "0":
			return null;
		default:
			throw new RuntimeException(
				"Der Wert '" + breite + "' für das Attribut Breite konnte nicht gemappt werden");
		}
	}

	public static boolean isParsable(String input) {
		try {
			Integer.parseInt(input);
			return true;
		} catch (final NumberFormatException e) {
			return false;
		}
	}

	private List<String> getUnterstuetzteAttribute() {
		return List.of("beleuchtun", "richtung", "vereinbaru", "belag", "breite", "wegart", "st", "breitst",
			"breitst2");
	}

	@Override
	public boolean isAttributNameValid(String attributName) {
		return getUnterstuetzteAttribute().contains(attributName.toLowerCase());
	}

	@Override
	public boolean isAttributWertValid(String attributName, String attributWert) {
		require(isAttributNameValid(attributName));
		if (attributWert == null) {
			return true;
		}
		switch (attributName.toLowerCase()) {
		case "belag":
			return belagArten.contains(attributWert);
		case "beleuchtun":
			return beleuchtungen.contains(attributWert);
		case "richtung":
			return richtungen.contains(attributWert);
		case "vereinbaru":
			return true;
		case "wegart":
			return radverkehrsfuehrungen.contains(attributWert);
		case "breite":
			return breiten.contains(attributWert);
		case "st":
			return attributWert.isBlank() || st.contains(attributWert);
		case "breitst":
			return attributWert.isBlank() || breitst.contains(attributWert) || attributWert.matches(
				"(:?< )?(\\d+(:?,\\d+)?)(:?.*)?");
		case "breitst2":
			if (attributWert.isBlank())
				return true;
			try {
				Integer.parseInt(attributWert);
			} catch (NumberFormatException e) {
				return false;
			}
			return true;
		default:
			return false;
		}
	}

	@Override
	public boolean shouldFilterNullValues(String attrName) {
		switch (attrName.toLowerCase()) {
		case "breitst":
		case "breitst2":
		case "st":
			return false;
		default:
			return super.shouldFilterNullValues(attrName);
		}
	}

	@Override
	public String getImportGruppe(String attrName) {
		switch (attrName.toLowerCase()) {
		case "breitst":
		case "breitst2":
		case "st":
			return "ST";
		default:
			return super.getImportGruppe(attrName);
		}
	}

	@Override
	public String getRadVisAttributName(String importedAttributName) {
		require(isAttributNameValid(importedAttributName));
		switch (importedAttributName.toLowerCase()) {
		case "belag":
			return "Belag";
		case "beleuchtun":
			return "Beleuchtung";
		case "richtung":
			return "Richtung";
		case "vereinbaru":
			return "Vereinbarungskennung";
		case "wegart":
			return "Radverkehrsführung";
		case "breite":
			return "Breite";
		case "st":
			return "Sicherheitstrennstreifeninformationen";
		default:
			return "";
		}
	}

	private enum Trennstreifenseite {
		FAHRBAHN,
		GEHWEG,
		UNBEKANNT
	}
}