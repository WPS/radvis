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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.exception.AttributUebernahmeException;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.AttributUebernahmeFehler;
import de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject.MappedAttributesProperties;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.service.NetzService;
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

	private record FuehrungsformAttributeSeitenbezogen(FuehrungsformAttribute fuehrungsformAttribute,
		LinearReferenzierterAbschnitt abschnitt, Seitenbezug seitenbezug) {
	}

	private final NetzService netzService;

	public LUBWMapper(NetzService netzService) {
		this.netzService = netzService;
	}

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
			mapTrennstreifenInfo(attributesProperties, linearReferenzierterAbschnitt, kante, Seitenbezug.BEIDSEITIG);
			break;
		default:
			throw new RuntimeException("LinearReferenziertes LUBW-Attribut '" + attributname + "' unbekannt");
		}
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

	private void mapTrennstreifenInfo(MappedAttributesProperties attributesProperties,
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, Kante kante, Seitenbezug seitenbezug)
		throws AttributUebernahmeException {
		require(seitenbezug, notNullValue());

		TrennstreifenForm trennstreifenForm = mapTrennstreifenForm(attributesProperties);
		Laenge trennstreifenBreite = mapTrennstreifenBreite(attributesProperties);
		TrennungZu trennungZu = mapTrennungZu(attributesProperties);

		boolean kanteIstVorImportSchonZweiseitig = kante.isZweiseitig();
		boolean zuImportierendeInformationenSindSeitenbezogen = seitenbezug != Seitenbezug.BEIDSEITIG;
		boolean kanteIstOderWirdZweiseitig = zuImportierendeInformationenSindSeitenbezogen
			|| kanteIstVorImportSchonZweiseitig;

		List<FuehrungsformAttributeSeitenbezogen> aufLineareReferenzZugeschnitten = getFuehrungsformAttributeFuerAbschnittSeitenbezogen(
			linearReferenzierterAbschnitt, kante, seitenbezug, kanteIstOderWirdZweiseitig);

		// Nur die Abschnitt nehmen, die nach einer Attributübernahme gültige STS-Werte halten würden.
		List<FuehrungsformAttributeSeitenbezogen> valideAbschnitte = aufLineareReferenzZugeschnitten.stream()
			.filter(fuehrungsformSeitenbezogen -> fuehrungsformFuerAttributuebernahmeValide(
				fuehrungsformSeitenbezogen.fuehrungsformAttribute, trennstreifenForm, trennstreifenBreite, trennungZu))
			.toList();

		List<AttributUebernahmeFehler> attributUebernahmeFehler = new ArrayList<>();

		if (!valideAbschnitte.isEmpty() && !Objects.isNull(trennstreifenForm)) {
			valideAbschnitte.forEach(
				fuehrungsformSeitenbezogen -> {
					if (kanteIstOderWirdZweiseitig) {
						mapTrennstreifenInfoKanteIstOderWirdZweiseitig(
							attributesProperties,
							kante,
							fuehrungsformSeitenbezogen.fuehrungsformAttribute,
							trennstreifenForm,
							trennstreifenBreite,
							trennungZu,
							attributUebernahmeFehler,
							fuehrungsformSeitenbezogen.seitenbezug,
							kanteIstVorImportSchonZweiseitig);
					} else {
						mapTrennstreifenInfoAufEinseitigeKante(kante, fuehrungsformSeitenbezogen.abschnitt,
							attributUebernahmeFehler, fuehrungsformSeitenbezogen.fuehrungsformAttribute,
							trennstreifenForm, trennstreifenBreite, trennungZu);
					}
				});
		}

		boolean hatTrennstreifenForm = !Objects.isNull(trennstreifenForm)
			&& trennstreifenForm != TrennstreifenForm.UNBEKANNT;
		if (!Objects.isNull(trennstreifenBreite) || !Objects.isNull(trennungZu) || hatTrennstreifenForm) {
			List<FuehrungsformAttributeSeitenbezogen> nichtValideAbschnitteGroesser1m = aufLineareReferenzZugeschnitten
				.stream()
				.filter(fuehrungsformSeitenbezogen -> {
					boolean abschnittIstLaengerAls1m = fuehrungsformSeitenbezogen.fuehrungsformAttribute
						.getLinearReferenzierterAbschnitt()
						.relativeLaenge() * kante.getLaengeBerechnet().getValue() > 1;
					boolean abschnittIstNichtValide = !valideAbschnitte.contains(fuehrungsformSeitenbezogen);
					return abschnittIstLaengerAls1m && abschnittIstNichtValide;
				})
				.toList();

			if (nichtValideAbschnitteGroesser1m.isEmpty() && attributUebernahmeFehler.isEmpty()) {
				return;
			}

			List<AttributUebernahmeFehler> fehler = nichtValideAbschnitteGroesser1m.stream()
				.map(fuehrungsformSeitenbezogen -> new AttributUebernahmeFehler(
					String.format("""
						Es konnten keine TrennstreifenInformationen geschrieben werden.
						Radverkehrsführung: %s
						KanteId: %s""",
						fuehrungsformSeitenbezogen.fuehrungsformAttribute.getRadverkehrsfuehrung(),
						kante.getId()),
					Set.of(
						"TrennstreifenForm: " + trennstreifenForm,
						"TrennstreifenBreite: " + trennstreifenBreite,
						"TrennungZu: " + trennungZu),
					fuehrungsformSeitenbezogen.fuehrungsformAttribute.getLinearReferenzierterAbschnitt(),
					fuehrungsformSeitenbezogen.seitenbezug))
				.collect(Collectors.toList());

			fehler.addAll(attributUebernahmeFehler);

			throw new AttributUebernahmeException(fehler);
		}
	}

	/**
	 * Übertrögt die STS Attribute auf eine Kante, die zweiseitig wird oder es bereits ist. Die Trennstreifen-Seite wird
	 * aus der Radverkehrsführung, Trennstreifenform und Ortslage ermittelt.
	 */
	private void mapTrennstreifenInfoKanteIstOderWirdZweiseitig(MappedAttributesProperties attributesProperties,
		Kante kante, FuehrungsformAttribute abschnittFuehrungsformAttribute,
		TrennstreifenForm trennstreifenFormZuUebertragen, Laenge trennstreifenBreite, TrennungZu trennungZu,
		List<AttributUebernahmeFehler> attributUebernahmeFehler,
		Seitenbezug abschnittSeitenbezug, boolean kanteIstVorImportSchonZweiseitig) {

		require(fuehrungsformFuerAttributuebernahmeValide(abschnittFuehrungsformAttribute,
			trennstreifenFormZuUebertragen, trennstreifenBreite, trennungZu));

		log.debug("Versuche Trennstreifen Attribute auf Kante {} (ist oder wird zweiseitig) zu übertragen.", kante
			.getId());

		if (abschnittFuehrungsformAttribute.getRadverkehrsfuehrung() == Radverkehrsfuehrung.UNBEKANNT) {
			super.applyTrennstreifenInfoLinks(kante, null, null, null, abschnittSeitenbezug,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());

			super.applyTrennstreifenInfoRechts(kante, null, null, null, abschnittSeitenbezug,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());

			return;
		}

		Trennstreifenseite trennstreifenseite = mapTrennstreifenseite(
			abschnittFuehrungsformAttribute.getRadverkehrsfuehrung(),
			attributesProperties.getProperty("st"),
			trennstreifenFormZuUebertragen,
			attributesProperties.getProperty("ORTSLAGE"));

		if (trennstreifenseite == Trennstreifenseite.UNBEKANNT) {
			// Bei unbekannt, loggen und auf links schreiben
			createAttributUebernahmeFehlerFalscheTrennstreifenSeite(kante.getId(), trennstreifenFormZuUebertragen,
				attributUebernahmeFehler, trennstreifenBreite, trennungZu, abschnittFuehrungsformAttribute,
				abschnittSeitenbezug);
			log.warn(
				"Zweiseitige Kante oder seitenBezugNotNull: Trennstreifenseite nicht ermittelbar für Kante mit Id {} auf LRS {}; Kante zweiseitig? {}; Seitenbezug: {}. Schreibe Trennstreifen links: {}, {}, {}",
				kante.getId(),
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt(),
				kanteIstVorImportSchonZweiseitig,
				abschnittSeitenbezug,
				trennstreifenFormZuUebertragen,
				trennstreifenBreite,
				trennungZu);

			super.applyTrennstreifenInfoLinks(kante,
				trennstreifenFormZuUebertragen,
				trennstreifenBreite,
				trennungZu,
				abschnittSeitenbezug,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());

			super.applyTrennstreifenInfoRechts(kante,
				TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN,
				null,
				null,
				abschnittSeitenbezug,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());
		} else if (trennstreifenseite == Trennstreifenseite.BEIDSEITIG) {
			// Beide Seiten haben die gleichen STS-Infos
			super.applyTrennstreifenInfoLinksUndRechts(kante,
				trennstreifenFormZuUebertragen,
				trennstreifenBreite,
				trennungZu,
				abschnittSeitenbezug,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());

			log.debug(
				"Trennstreifenseite ermittelbar für Kante mit Id {} auf LRS {}. Schreibe Trennstreifen beidseitig",
				kante.getId(),
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());
		} else if (isTrennstreifenLinks(abschnittSeitenbezug, trennstreifenseite)) {
			super.applyTrennstreifenInfoLinks(kante,
				trennstreifenFormZuUebertragen,
				trennstreifenBreite,
				trennungZu,
				abschnittSeitenbezug,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());
			super.applyTrennstreifenInfoRechts(kante,
				TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN,
				null,
				null,
				abschnittSeitenbezug,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());

			log.debug(
				"Trennstreifenseite ermittelbar für Kante mit Id {} auf LRS {}. Schreibe Trennstreifen links",
				kante.getId(),
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());

		} else {
			super.applyTrennstreifenInfoRechts(kante,
				trennstreifenFormZuUebertragen,
				trennstreifenBreite,
				trennungZu,
				abschnittSeitenbezug,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());
			super.applyTrennstreifenInfoLinks(kante,
				TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN,
				null,
				null,
				abschnittSeitenbezug,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());
			log.debug(
				"Trennstreifenseite ermittelbar für Kante mit Id {} auf LRS {}. Schreibe Trennstreifen rechts",
				kante.getId(),
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());
		}
	}

	/**
	 * Überträgt die STS Attribute auf eine Kante, die einseitig ist und bleibt. Die Trennstreifen-Seite wird über
	 * nahegelegene parallele Kanten geometrisch versucht zu ermitteln. Dies kann also fehlschlagen und Fehler erzeugen.
	 */
	private void mapTrennstreifenInfoAufEinseitigeKante(Kante kante,
		LinearReferenzierterAbschnitt abschnitt, List<AttributUebernahmeFehler> attributUebernahmeFehler,
		FuehrungsformAttribute abschnittFuehrungsformAttribute, TrennstreifenForm trennstreifenFormZuUebertragen,
		Laenge trennstreifenBreite,
		TrennungZu trennungZu) {

		require(!kante.isZweiseitig());

		if (abschnittFuehrungsformAttribute.getRadverkehrsfuehrung() == Radverkehrsfuehrung.UNBEKANNT) {
			super.applyTrennstreifenInfoLinks(kante, null, null, null,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());

			super.applyTrennstreifenInfoRechts(kante, null, null, null,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());

			return;
		}

		require(fuehrungsformFuerAttributuebernahmeValide(abschnittFuehrungsformAttribute,
			trennstreifenFormZuUebertragen, trennstreifenBreite, trennungZu));

		log.debug("Versuche Trennstreifen Attribute auf Kante {} (ist und bleibt einseitig) zu übertragen.", kante
			.getId());

		// Seitenerkennung umgehen, wenn "null" oder "Kein Trennstreifen vorhanden" geschrieben werden soll. In diesem
		// Fall ist es sinnlos eine Seite zu erkennen, wenn hinterher eh auf "A" und "B" jeweils das gleiche geschrieben
		// wird.
		if (trennstreifenFormZuUebertragen == null
			|| trennstreifenFormZuUebertragen == TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN) {
			super.applyTrennstreifenInfoRechts(kante, trennstreifenFormZuUebertragen, null, null,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());
			super.applyTrennstreifenInfoLinks(kante, TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN, null,
				null, abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());

			log.debug(
				"Überspringe Seitenerkennung und übertrage Trennstreifen-Form {} auf beide Trennstreifen von Kante {}",
				trennstreifenFormZuUebertragen == null ? "'null'" : trennstreifenFormZuUebertragen.name(), kante
					.getId());
			return;
		}

		Seitenbezug seiteMitStrassen = netzService.getSeiteMitParallelenStrassenKanten(kante, abschnitt).orElse(null);

		if (seiteMitStrassen == null || Seitenbezug.BEIDSEITIG.equals(seiteMitStrassen)) {
			attributUebernahmeFehler.add(
				new AttributUebernahmeFehler(
					String.format(
						"""
							Es konnte keine Trennstreifenseite ermittelt werden, da in der Nähe von Kante %s auf keiner oder auf beiden Seiten parallele Kanten gefunden wurden.

							Zusatzinformationen:
							Radverkehrsführung: "%s"
							KanteId: %s""",
						kante.getId(),
						abschnittFuehrungsformAttribute.getRadverkehrsfuehrung(),
						kante.getId()),
					Set.of(
						"TrennstreifenForm: " + trennstreifenFormZuUebertragen,
						"TrennstreifenBreite: " + trennstreifenBreite,
						"TrennungZu: " + trennungZu),
					abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt(),
					Seitenbezug.BEIDSEITIG));

			log.debug(
				"Übertrage keine Trennstreifen Attribute auf Kante {}, da auf keiner oder auf beiden Seiten parallele Kanten gefunden wurden",
				kante.getId());
		} else if (Seitenbezug.RECHTS.equals(seiteMitStrassen)) {
			// Die parallele Straße verläuft rechts, also schreiben wir auch dort die Trennstreifen-Infos hin.
			// Konsistenterweise schreiben wir auf die linke Seite "Kein Trennstreifen vorhanden", da die LUBW-Daten nur
			// Trennstreifen-Infos für genau eine Seite enthalten.
			super.applyTrennstreifenInfoRechts(kante,
				trennstreifenFormZuUebertragen,
				trennstreifenBreite,
				trennungZu,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());
			super.applyTrennstreifenInfoLinks(kante, TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN,
				null, null, abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());

			log.debug("Trennstreifen Attribute auf rechte Seite von Kante {} übertragen.", kante.getId());
		} else {
			// Die parallele Straße verläuft links, also schreiben wir auch dort die Trennstreifen-Infos hin.
			// Konsistenterweise schreiben wir auf die rechte Seite "Kein Trennstreifen vorhanden", da die LUBW-Daten
			// nur
			// Trennstreifen-Infos für genau eine Seite enthalten.

			super.applyTrennstreifenInfoLinks(kante,
				trennstreifenFormZuUebertragen,
				trennstreifenBreite,
				trennungZu,
				abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());
			super.applyTrennstreifenInfoRechts(kante, TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN, null,
				null, abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt());

			log.debug("Trennstreifen Attribute auf linke Seite von Kante {} übertragen.", kante.getId());
		}
	}

	/**
	 * Ermittelt alle Führungsform Attribute der Kante, die auf den übergebenen Abschnitt zugeschnitten sind. Hierbei
	 * wird der gewünschte Seitenbezug genutzt (also bspw. nur alle rechten Attribute berücksichtigt) und bei
	 * "Beidseitig" geschaut, wie sich die Zweiseitigkeit der Kante verhält. Ist oder wird die Kante zweiseitig, dann
	 * betrachten wir Attribute rechts und links, ansonsten nur links.
	 */
	private static @NotNull List<FuehrungsformAttributeSeitenbezogen> getFuehrungsformAttributeFuerAbschnittSeitenbezogen(
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt, Kante kante, Seitenbezug seitenbezug,
		boolean kanteIstOderWirdZweiseitig) {

		List<FuehrungsformAttributeSeitenbezogen> aufLineareReferenzZugeschnitten = new ArrayList<>();

		if (seitenbezug == Seitenbezug.RECHTS) {
			LinearReferenzierteAttribute.getIntersecting(linearReferenzierterAbschnitt,
				kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.forEach(fa -> aufLineareReferenzZugeschnitten.add(new FuehrungsformAttributeSeitenbezogen(fa,
					fa.getLinearReferenzierterAbschnitt(), Seitenbezug.RECHTS)));
		} else if (seitenbezug == Seitenbezug.LINKS) {
			LinearReferenzierteAttribute.getIntersecting(linearReferenzierterAbschnitt,
				kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.forEach(fa -> aufLineareReferenzZugeschnitten.add(new FuehrungsformAttributeSeitenbezogen(fa,
					fa.getLinearReferenzierterAbschnitt(), Seitenbezug.LINKS)));
		} else if (kanteIstOderWirdZweiseitig) {
			LinearReferenzierteAttribute.getIntersecting(linearReferenzierterAbschnitt,
				kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
				.forEach(fa -> aufLineareReferenzZugeschnitten.add(new FuehrungsformAttributeSeitenbezogen(fa,
					fa.getLinearReferenzierterAbschnitt(), Seitenbezug.RECHTS)));
			LinearReferenzierteAttribute.getIntersecting(linearReferenzierterAbschnitt,
				kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.forEach(fa -> aufLineareReferenzZugeschnitten.add(new FuehrungsformAttributeSeitenbezogen(fa,
					fa.getLinearReferenzierterAbschnitt(), Seitenbezug.LINKS)));
		} else {
			// Kante ist und bleibt einseitig -> STS-Infos beziehen sich auf beide Seiten gleichzeitig
			// -> Seitenbezug.Beidseitig
			LinearReferenzierteAttribute.getIntersecting(linearReferenzierterAbschnitt,
				kante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
				.forEach(fa -> aufLineareReferenzZugeschnitten.add(new FuehrungsformAttributeSeitenbezogen(fa,
					fa.getLinearReferenzierterAbschnitt(), Seitenbezug.BEIDSEITIG)));
		}

		return aufLineareReferenzZugeschnitten;
	}

	/**
	 * Im wesentlichen die Konsistenzprüfung aus der Führungsform selbst, aber mit einer Ausnahme: Unbekannte
	 * Radverkehrsführung und "Kein Trennstreifen vorhanden" zählen hier als valide.
	 */
	private static boolean fuehrungsformFuerAttributuebernahmeValide(
		FuehrungsformAttribute fuehrungsformAttribute,
		TrennstreifenForm trennstreifenForm, Laenge trennstreifenBreite, TrennungZu trennungZu) {

		Radverkehrsfuehrung radverkehrsfuehrung = fuehrungsformAttribute.getRadverkehrsfuehrung();
		boolean isTrennstreifenCorrect = FuehrungsformAttribute
			.isTrennstreifenCorrect(radverkehrsfuehrung, trennstreifenForm, trennstreifenBreite, trennungZu);

		return isTrennstreifenCorrect || radverkehrsfuehrung == Radverkehrsfuehrung.UNBEKANNT;
	}

	private static void createAttributUebernahmeFehlerFalscheTrennstreifenSeite(Long kanteId,
		TrennstreifenForm trennstreifenFormZuUebertragen,
		List<AttributUebernahmeFehler> attributUebernahmeFehler, Laenge trennstreifenBreite,
		TrennungZu trennungZu, FuehrungsformAttribute abschnittFuehrungsformAttribute,
		Seitenbezug abschnittSeitenbezug) {
		if (trennstreifenFormZuUebertragen != null
			&& !trennstreifenFormZuUebertragen.equals(TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN)
			&& !trennstreifenFormZuUebertragen.equals(TrennstreifenForm.UNBEKANNT)) {
			attributUebernahmeFehler.add(
				new AttributUebernahmeFehler(
					String.format(
						"""
							Es konnte keine Trennstreifenseite ermittelt werden, da unklar ist auf welcher Seite der Straße der Radweg liegt.
							Die Trennstreifeninformationen
							------------------------------------------
							Form: "%s"
							Breite: "%s"
							Trennung zu: "%s"
							------------------------------------------
							wurden nun auf die linke Seite geschrieben.
							.
							Zusatzinformationen:
							Radverkehrsführung: "%s"
							KanteId: %s""",
						trennstreifenFormZuUebertragen,
						trennstreifenBreite,
						trennungZu,
						abschnittFuehrungsformAttribute.getRadverkehrsfuehrung(),
						kanteId),
					Set.of(
						String.format(
							"Bitte Überprüfen Sie insbesondere, ob \"Trennung zu %s\" zu der Lage der Kante passt und passen Sie gegebenenfalls die beschriebene Trennstreifenseite (links (A) oder rechts (B)) an.",
							trennungZu)),
					abschnittFuehrungsformAttribute.getLinearReferenzierterAbschnitt(),
					abschnittSeitenbezug));
		}
	}

	private static boolean isTrennstreifenLinks(Seitenbezug seitenbezug,
		Trennstreifenseite trennstreifenseite) {
		require(trennstreifenseite != Trennstreifenseite.UNBEKANNT);

		if (seitenbezug == Seitenbezug.LINKS) {
			return trennstreifenseite == Trennstreifenseite.GEHWEG;
		}

		return trennstreifenseite == Trennstreifenseite.FAHRBAHN;
	}

	private Trennstreifenseite mapTrennstreifenseite(Radverkehrsfuehrung radverkehrsfuehrung,
		String stProperty, TrennstreifenForm trennstreifenFormZuUebertragen, String ortslage) {

		if (trennstreifenFormZuUebertragen == TrennstreifenForm.KEIN_SICHERHEITSTRENNSTREIFEN_VORHANDEN) {
			return Trennstreifenseite.BEIDSEITIG;
		} else if (trennstreifenFormZuUebertragen == TrennstreifenForm.TRENNUNG_DURCH_ANDERE_ART
			&& ortslage.equals("Außerorts")) {
			return Trennstreifenseite.FAHRBAHN;
		}

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

	private TrennungZu mapTrennungZu(MappedAttributesProperties properties) {
		String stProperty = properties.getProperty("st");
		return switch (stProperty) {
		case "Sicherheitstrennstreifen innerorts ohne Parken", "Sicherheitstrennstreifen außerorts" -> TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN;
		case "Sicherheitstrennstreifen innerorts mit Längsparken", "Sicherheitstrennstreifen innerorts mit Schräg-/Senkrechtparken" -> TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_PARKEN;
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
		UNBEKANNT,
		BEIDSEITIG
	}
}