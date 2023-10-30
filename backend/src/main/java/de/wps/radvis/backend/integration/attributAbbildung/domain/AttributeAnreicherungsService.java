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

package de.wps.radvis.backend.integration.attributAbbildung.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.AttributProjektionsJobStatistik;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.AttributeMergeFehlersammlung;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.Attributprojektionsbeschreibung;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.Haendigkeit;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.SeitenbezogeneProjizierteAttribute;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.AttributgruppenanteilZuKleinException;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.MehrdeutigeAttributgruppeException;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.MehrdeutigeLinearReferenzierteAttributeException;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class AttributeAnreicherungsService {

	public static final double MIN_SEGMENT_LENGTH_METER = 2.;
	private static final double ATTRIBUTE_INTERSECTION_THRESHHOLD = 0.8;

	private static final double DIFFERENZ_IN_HAENDIGKEIT_WAHRSCHEINLICHKEITEN_FUER_SEITENZUORDNUNG = 0.35;
	private static final double DIFFERENZ_IN_HAENDIGKEIT_WAHRSCHEINLICHKEITEN_FUER_SEITENBEZUG = 0.5;

	private KantenAttributeMergeService attributeMergeService;

	private AttributeProjektionsProtokollService attributeProjektionsProtokollService;

	/**
	 * Der Finale Schritt des Algorithmuses, in dem die Attributprojektionsbeschreibungen aufgelöst werden und das
	 * Ergebnis (soweit möglich) auf die Grundnetzkante geschrieben wird. Für die Linear Referenzierten Attribute teilt
	 * der Algorithmus die Kante in Segmente (Lineare Referenzen) für die jeweils Attribute vollständig gelten (den
	 * Prozess nennen wir "normalisieren"). Danach wird für jedes dieser Segmente geschaut, ob wir alle darauf
	 * projizierten Attribute und die der Grundnetzkante mergen können (Mehrdeutigkeiten auflösen). Wenn ja, schreiben
	 * wir die gemergten Attribute für dieses Segment auf die Grundnetzkante, ansonsten belassen wir die bestehenden
	 * Attribute der Grundnetzkante auf diesem Segment der Grundnetzkante. Um zu kleine Segmente zu verhindern und um
	 * die Projektionsungenaugikeit der Attributprojektionsbeschreibung auszugleichen, interpolieren wir die
	 * projizierten Attribute vorher, soweit möglich. Sind zwei Segmente mit identischen LR-Attribute aneinander
	 * anliegen, führen wir diese zu einem Segment zusammen.
	 *
	 * @return Eine Map von Quellnetz ID auf den Anteil, der erfolgreich projiziert wurde
	 */
	@SuppressChangedEvents
	public Map<Long, Double> reichereGrundnetzKantenMitAttributenAn(
		Collection<Attributprojektionsbeschreibung> attributprojektionsbeschreibungen,
		QuellSystem quelleDerProjiziertenAttribute, AttributProjektionsJobStatistik statistik,
		String jobZuordnung) {

		HashMap<Long, Double> quellnetzIDAufAnteil = new HashMap<>();

		int indexFuerFortschritt = 0;
		int size = attributprojektionsbeschreibungen.size();
		for (Iterator<Attributprojektionsbeschreibung> iterator = attributprojektionsbeschreibungen.iterator(); iterator
			.hasNext(); ) {
			Attributprojektionsbeschreibung attributprojektionsbeschreibung = iterator.next();

			Kante grundnetzKante = attributprojektionsbeschreibung.getZielnetzKante();

			List<Long> zuProjizierendeKanteIds = new ArrayList<>(
				attributprojektionsbeschreibung.getProjizierteKanteIds().keySet());

			try {
				KantenAttribute neueKantenAttribute = getNeueAttributeNachThreshhold(
					attributprojektionsbeschreibung.getPotentiellInkonsistenteProjizierteKantenattributeZuAnteil(),
					KantenAttribute.class.getSimpleName());

				KantenAttribute mergedKantenAttribute = attributeMergeService
					.mergeKantenAttribute(grundnetzKante.getKantenAttributGruppe().getKantenAttribute(),
						neueKantenAttribute,
						quelleDerProjiziertenAttribute);
				grundnetzKante.getKantenAttributGruppe().update(
					grundnetzKante.getKantenAttributGruppe().getNetzklassen(),
					grundnetzKante.getKantenAttributGruppe().getIstStandards(),
					mergedKantenAttribute);

				statistik.laengeDesDLMAufDasKantenattributeErfolgreichProjiziertWurden += grundnetzKante.getGeometry()
					.getLength();

				if (QuellSystem.RadwegeDB.equals(quelleDerProjiziertenAttribute)) {
					attributprojektionsbeschreibung.getProjizierteKanteIds().forEach((projizierteKanteId, anteil) -> {
						quellnetzIDAufAnteil.merge(projizierteKanteId, anteil, Double::sum);
					});
				}
			} catch (MehrdeutigeAttributgruppeException | AttributgruppenanteilZuKleinException e) {
				e.setGrundnetzKantenID(grundnetzKante.getId());
				e.setProjizierteKanteIds(zuProjizierendeKanteIds);
				e.setGrundnetzKanteGeometry(grundnetzKante.getGeometry());
				attributeProjektionsProtokollService.handle(e, jobZuordnung);
				statistik.mehrdeutigeKantenAttribute++;
			}

			boolean wurdenNetzklassenProjiziert = false;
			try {
				Map<Set<Netzklasse>, Double> netzklassenZuAnteil = attributprojektionsbeschreibung
					.getPotentiellInkonsistenteProjizierteNetzklassen().entrySet().stream()
					.map(entry -> Map.entry(entry.getKey(),
						entry.getValue().stream().mapToDouble(LinearReferenzierterAbschnitt::relativeLaenge).sum()))
					.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

				Set<Netzklasse> neueNetzklassen = getNeueAttributeNachThreshhold(netzklassenZuAnteil,
					Netzklasse.class.getSimpleName());

				Set<Netzklasse> mergedNetzklassen = attributeMergeService
					.mergeNetzklassen(grundnetzKante.getKantenAttributGruppe().getNetzklassen(),
						neueNetzklassen,
						quelleDerProjiziertenAttribute);

				grundnetzKante.getKantenAttributGruppe().update(mergedNetzklassen,
					grundnetzKante.getKantenAttributGruppe().getIstStandards(),
					grundnetzKante.getKantenAttributGruppe().getKantenAttribute());

				statistik.laengeDesDLMAufDasNetzklassenErfolgreichProjiziertWurden += grundnetzKante.getGeometry()
					.getLength();

				if (QuellSystem.RadNETZ.equals(quelleDerProjiziertenAttribute)) {
					attributprojektionsbeschreibung.getProjizierteKanteIds().forEach((projizierteKanteId, anteil) -> {
						quellnetzIDAufAnteil.merge(projizierteKanteId, anteil, Double::sum);
					});
					// Wenn bei der Anreicherung des DLM mit den RadNETZ-Kanten eine DLM kannte eine RadNETZ-Klasse
					// erhält,
					// hat sie zunächst keinen GrundnetzStatus mehr
					grundnetzKante.setGrundnetz(false);
				}
				wurdenNetzklassenProjiziert = true;
			} catch (MehrdeutigeAttributgruppeException | AttributgruppenanteilZuKleinException e) {
				e.setGrundnetzKantenID(grundnetzKante.getId());
				e.setProjizierteKanteIds(zuProjizierendeKanteIds);
				e.setGrundnetzKanteGeometry(grundnetzKante.getGeometry());
				attributeProjektionsProtokollService.handle(e, jobZuordnung);
				statistik.mehrdeutigeNetzklassen++;
			}

			try {
				if (wurdenNetzklassenProjiziert) {
					Set<IstStandard> neueIstStandards = getNeueAttributeNachThreshhold(
						attributprojektionsbeschreibung.getPotentiellInkonsistenteProjizierteIstStandards(),
						IstStandard.class.getSimpleName());

					Set<IstStandard> mergedIstStandards = attributeMergeService
						.mergeIstStandards(grundnetzKante.getKantenAttributGruppe().getIstStandards(),
							neueIstStandards, quelleDerProjiziertenAttribute);

					grundnetzKante.getKantenAttributGruppe().update(
						grundnetzKante.getKantenAttributGruppe().getNetzklassen(),
						mergedIstStandards,
						grundnetzKante.getKantenAttributGruppe().getKantenAttribute());
				}
			} catch (MehrdeutigeAttributgruppeException | AttributgruppenanteilZuKleinException e) {
				e.setGrundnetzKantenID(grundnetzKante.getId());
				e.setProjizierteKanteIds(zuProjizierendeKanteIds);
				e.setGrundnetzKanteGeometry(grundnetzKante.getGeometry());
				attributeProjektionsProtokollService.handle(e, jobZuordnung);
				statistik.mehrdeutigeIstStandards++;
			}

			// Linear Referenzierte Attribute und Seitenbezogene Attribute
			double kanteLaenge = grundnetzKante.getGeometry().getLength();

			AttributeMergeFehlersammlung fehlersammlung = new AttributeMergeFehlersammlung(grundnetzKante.getId(),
				grundnetzKante.getGeometry(), zuProjizierendeKanteIds);

			reichereKanteMitSeitenbezogenenAttributenAn(quelleDerProjiziertenAttribute, statistik, jobZuordnung,
				attributprojektionsbeschreibung,
				grundnetzKante,
				zuProjizierendeKanteIds, kanteLaenge, fehlersammlung);

			ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = grundnetzKante
				.getZustaendigkeitAttributGruppe();
			List<ZustaendigkeitAttribute> neueZustaendigkeitAttribute = mergeZustaendigkeitAttribute(
				zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute(),
				attributprojektionsbeschreibung.getPotentiellInkonsistenteProjizierteZustaendigkeitAttribute(),
				quelleDerProjiziertenAttribute,
				kanteLaenge,
				fehlersammlung);
			zustaendigkeitAttributGruppe.replaceZustaendigkeitAttribute(neueZustaendigkeitAttribute);

			GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = grundnetzKante
				.getGeschwindigkeitAttributGruppe();
			List<GeschwindigkeitAttribute> neueGeschwindigkeitAttribute = mergeGeschwindigkeitsAttribute(
				geschwindigkeitAttributGruppe.getImmutableGeschwindigkeitAttribute(),
				attributprojektionsbeschreibung.getPotentiellInkonsistenteProjizierteGeschwindigkeitsattribute(),
				quelleDerProjiziertenAttribute,
				kanteLaenge,
				fehlersammlung);
			geschwindigkeitAttributGruppe.replaceGeschwindigkeitAttribute(neueGeschwindigkeitAttribute);

			fehlersammlung.getExceptions().forEach(e -> {
				statistik.addMehrdeutigeLinearReferenzierteAttribute(e.getAttributGruppe());
				attributeProjektionsProtokollService.handle(e, jobZuordnung);
			});

			int fortschrittsrate = 4;
			if (size >= fortschrittsrate
				&& indexFuerFortschritt % (size / fortschrittsrate) == 0) {
				log.info("Fortschritt {}%", (indexFuerFortschritt / (double) size) * 100);
			}
			indexFuerFortschritt++;

			iterator.remove();
		}

		return quellnetzIDAufAnteil;
	}

	private void reichereKanteMitSeitenbezogenenAttributenAn(QuellSystem quelleDerProjiziertenAttribute,
		AttributProjektionsJobStatistik statistik,
		String jobZuordnung, Attributprojektionsbeschreibung attributprojektionsbeschreibung,
		Kante grundnetzKante,
		List<Long> zuProjizierendeKanteIds, double kanteLaenge, AttributeMergeFehlersammlung fehlersammlung) {
		// seitenbezug

		List<SeitenbezogeneProjizierteAttribute> seitenbezogeneProjizierteAttribute = attributprojektionsbeschreibung
			.getSeitenbezogeneProjizierteAttribute();

		List<LinearReferenzierterAbschnitt> lineareReferenzen = normalisiereLineareReferenzen(
			seitenbezogeneProjizierteAttribute.stream()
				.map(SeitenbezogeneProjizierteAttribute::getLinearReferenzierterAbschnittAufZielnetzkante)
				.collect(Collectors.toList()));

		Map<LinearReferenzierterAbschnitt, List<SeitenbezogeneProjizierteAttribute>> mapAufAttribute = new HashMap<>();

		for (LinearReferenzierterAbschnitt linearReferenzierterAbschnitt : lineareReferenzen) {
			List<SeitenbezogeneProjizierteAttribute> matchingAttribute = seitenbezogeneProjizierteAttribute
				.stream()
				// keine Punktintersection ist an dieser Stelle wichtig
				.filter(attribut -> linearReferenzierterAbschnitt.intersection(
						attribut.getLinearReferenzierterAbschnittAufZielnetzkante())
					.isPresent())
				.collect(Collectors.toList());
			mapAufAttribute.put(linearReferenzierterAbschnitt, matchingAttribute);
		}

		if (brauchKanteSeitenbezug(lineareReferenzen, mapAufAttribute)) {
			reichereMitSeitenbezugAn(grundnetzKante, lineareReferenzen, mapAufAttribute, quelleDerProjiziertenAttribute,
				statistik, fehlersammlung, jobZuordnung, zuProjizierendeKanteIds);
		} else {
			FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = grundnetzKante.getFahrtrichtungAttributGruppe();

			try {
				Richtung neueFahrtrichtung = getNeueAttributeNachThreshhold(
					attributprojektionsbeschreibung.getAllePotentiellInkonsistenteRichtungen(),
					FahrtrichtungAttributGruppe.class.getSimpleName());

				if (fahrtrichtungAttributGruppe.isZweiseitig()) {
					Richtung mergedlinks = attributeMergeService.mergeFahrtrichtung(
						fahrtrichtungAttributGruppe.getFahrtrichtungLinks(),
						neueFahrtrichtung, quelleDerProjiziertenAttribute);
					fahrtrichtungAttributGruppe.setRichtung(
						mergedlinks,
						fahrtrichtungAttributGruppe.getFahrtrichtungRechts());
					Richtung mergeRechts = attributeMergeService.mergeFahrtrichtung(
						fahrtrichtungAttributGruppe.getFahrtrichtungRechts(),
						neueFahrtrichtung, quelleDerProjiziertenAttribute);
					fahrtrichtungAttributGruppe.setRichtung(
						fahrtrichtungAttributGruppe.getFahrtrichtungLinks(),
						mergeRechts);

				} else {
					fahrtrichtungAttributGruppe.setRichtung(attributeMergeService
						.mergeFahrtrichtung(
							fahrtrichtungAttributGruppe.getFahrtrichtungLinks(),
							neueFahrtrichtung, quelleDerProjiziertenAttribute));
				}
			} catch (MehrdeutigeAttributgruppeException | AttributgruppenanteilZuKleinException e) {
				e.setGrundnetzKantenID(grundnetzKante.getId());
				e.setProjizierteKanteIds(zuProjizierendeKanteIds);
				e.setGrundnetzKanteGeometry(grundnetzKante.getGeometry());
				attributeProjektionsProtokollService.handle(e, jobZuordnung);
				statistik.mehrdeutigeRichtungsattribute++;
			}

			FuehrungsformAttributGruppe fuehrungsformAttributGruppe = grundnetzKante
				.getFuehrungsformAttributGruppe();

			if (fuehrungsformAttributGruppe.isZweiseitig()) {
				List<FuehrungsformAttribute> neueFuehrungsformAttributeLinks = mergeFuehrungsformAttribute(
					fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks(),
					attributprojektionsbeschreibung.getAllePotentiellInkonsistenteFuehrungsformAttribute(),
					quelleDerProjiziertenAttribute,
					kanteLaenge,
					fehlersammlung,
					"");
				List<FuehrungsformAttribute> neueFuehrungsformAttributeRechts = mergeFuehrungsformAttribute(
					fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts(),
					attributprojektionsbeschreibung.getAllePotentiellInkonsistenteFuehrungsformAttribute(),
					quelleDerProjiziertenAttribute,
					kanteLaenge,
					fehlersammlung,
					"");
				fuehrungsformAttributGruppe.replaceFuehrungsformAttribute(neueFuehrungsformAttributeLinks,
					neueFuehrungsformAttributeRechts);

			} else {
				List<FuehrungsformAttribute> neueFuehrungsformAttribute = mergeFuehrungsformAttribute(
					fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks(),
					attributprojektionsbeschreibung.getAllePotentiellInkonsistenteFuehrungsformAttribute(),
					quelleDerProjiziertenAttribute,
					kanteLaenge,
					fehlersammlung,
					"");
				fuehrungsformAttributGruppe.replaceFuehrungsformAttribute(neueFuehrungsformAttribute);
			}
		}
	}

	private void reichereMitSeitenbezugAn(Kante grundnetzKante, List<LinearReferenzierterAbschnitt> lineareReferenzen,
		Map<LinearReferenzierterAbschnitt, List<SeitenbezogeneProjizierteAttribute>> mapAufAttribute,
		QuellSystem quelleDerProjiziertenAttribute, AttributProjektionsJobStatistik statistik,
		AttributeMergeFehlersammlung fehlersammlung, String jobZuordnung, List<Long> zuProjizierendeKanteIds) {

		grundnetzKante.changeSeitenbezug(true);
		List<FuehrungsformAttribute> potentiellInkonsistenteFuehrungsformLinks = new ArrayList<>();
		List<FuehrungsformAttribute> potentiellInkonsistenteFuehrungsformRechts = new ArrayList<>();
		Map<Richtung, Double> potentiellInkonsistenteRichtungAufAnteilLinks = new HashMap<>();
		Map<Richtung, Double> potentiellInkonsistenteRichtungAufAnteilRechts = new HashMap<>();

		erstellePotentiellInkonsistenteAttributgruppenLinksUndRechts(lineareReferenzen, mapAufAttribute,
			potentiellInkonsistenteFuehrungsformLinks,
			potentiellInkonsistenteFuehrungsformRechts,
			potentiellInkonsistenteRichtungAufAnteilLinks,
			potentiellInkonsistenteRichtungAufAnteilRechts);

		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = grundnetzKante.getFahrtrichtungAttributGruppe();

		try {
			Richtung neueRichtungLinks = getNeueAttributeNachThreshhold(
				potentiellInkonsistenteRichtungAufAnteilLinks,
				FahrtrichtungAttributGruppe.class.getSimpleName());

			fahrtrichtungAttributGruppe.setRichtung(attributeMergeService
					.mergeFahrtrichtung(
						fahrtrichtungAttributGruppe.getFahrtrichtungLinks(),
						neueRichtungLinks, quelleDerProjiziertenAttribute),
				fahrtrichtungAttributGruppe.getFahrtrichtungRechts());

		} catch (MehrdeutigeAttributgruppeException | AttributgruppenanteilZuKleinException e) {
			e.setGrundnetzKantenID(grundnetzKante.getId());
			e.setProjizierteKanteIds(zuProjizierendeKanteIds);
			e.setGrundnetzKanteGeometry(grundnetzKante.getGeometry());
			e.setSeite("links");
			attributeProjektionsProtokollService.handle(e, jobZuordnung);
			statistik.mehrdeutigeRichtungsattribute++;
		}

		try {
			Richtung neueRichtungRechts = getNeueAttributeNachThreshhold(
				potentiellInkonsistenteRichtungAufAnteilRechts,
				FahrtrichtungAttributGruppe.class.getSimpleName());

			fahrtrichtungAttributGruppe
				.setRichtung(fahrtrichtungAttributGruppe.getFahrtrichtungLinks(),
					attributeMergeService
						.mergeFahrtrichtung(fahrtrichtungAttributGruppe.getFahrtrichtungRechts(), neueRichtungRechts,
							quelleDerProjiziertenAttribute));

		} catch (MehrdeutigeAttributgruppeException | AttributgruppenanteilZuKleinException e) {
			e.setGrundnetzKantenID(grundnetzKante.getId());
			e.setProjizierteKanteIds(zuProjizierendeKanteIds);
			e.setGrundnetzKanteGeometry(grundnetzKante.getGeometry());
			e.setSeite("rechts");
			attributeProjektionsProtokollService.handle(e, jobZuordnung);
			statistik.mehrdeutigeRichtungsattribute++;
		}

		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = grundnetzKante.getFuehrungsformAttributGruppe();
		double kanteLaenge = grundnetzKante.getGeometry().getLength();

		List<FuehrungsformAttribute> neueFuehrungsformAttributeLinks = mergeFuehrungsformAttribute(
			fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks(),
			potentiellInkonsistenteFuehrungsformLinks,
			quelleDerProjiziertenAttribute,
			kanteLaenge,
			fehlersammlung, "links");
		List<FuehrungsformAttribute> neueFuehrungsformAttributeRechts = mergeFuehrungsformAttribute(
			fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts(),
			potentiellInkonsistenteFuehrungsformRechts,
			quelleDerProjiziertenAttribute,
			kanteLaenge,
			fehlersammlung, "rechts");
		fuehrungsformAttributGruppe
			.replaceFuehrungsformAttribute(neueFuehrungsformAttributeLinks,
				neueFuehrungsformAttributeRechts);
	}

	private void erstellePotentiellInkonsistenteAttributgruppenLinksUndRechts(
		List<LinearReferenzierterAbschnitt> lineareReferenzen,
		Map<LinearReferenzierterAbschnitt, List<SeitenbezogeneProjizierteAttribute>> mapAufAttribute,
		List<FuehrungsformAttribute> potentiellInkonsistenteFuehrungsformLinks,
		List<FuehrungsformAttribute> potentiellInkonsistenteFuehrungsformRechts,
		Map<Richtung, Double> potentiellInkonsistenteRichtungAufAnteilLinks,
		Map<Richtung, Double> potentiellInkonsistenteRichtungAufAnteilRechts) {
		for (LinearReferenzierterAbschnitt linearReferenzierterAbschnitt : lineareReferenzen) {
			List<SeitenbezogeneProjizierteAttribute> matchingAttribute = mapAufAttribute.get(
				linearReferenzierterAbschnitt);
			if (matchingAttribute.isEmpty()) {
				continue;
			}

			matchingAttribute.sort(Comparator.comparing(SeitenbezogeneProjizierteAttribute::getHaendigkeit,
				Haendigkeit.vonLinksNachRechts));

			SeitenbezogeneProjizierteAttribute leftMost = matchingAttribute.get(0);
			SeitenbezogeneProjizierteAttribute rightMost = matchingAttribute.get(matchingAttribute.size() - 1);

			for (SeitenbezogeneProjizierteAttribute richtungsbasierteProjizierteAttribut : matchingAttribute) {
				double differenzLinks = richtungsbasierteProjizierteAttribut.getHaendigkeit()
					.getVorzeichenbehafteteWahrscheinlichkeit()
					- leftMost.getHaendigkeit().getVorzeichenbehafteteWahrscheinlichkeit();

				double differenzRechts = richtungsbasierteProjizierteAttribut.getHaendigkeit()
					.getVorzeichenbehafteteWahrscheinlichkeit()
					- rightMost.getHaendigkeit().getVorzeichenbehafteteWahrscheinlichkeit();

				double groessteDifferenz = Math.abs(differenzLinks) > Math.abs(differenzRechts) ? differenzLinks
					: differenzRechts;

				List<FuehrungsformAttribute> fuehrungsformAttribute = richtungsbasierteProjizierteAttribut
					.getFuehrungsformAttribute().stream().map(FuehrungsformAttribute::copyWithSameValues)
					.sorted(Comparator.comparing(FuehrungsformAttribute::getLinearReferenzierterAbschnitt,
						LinearReferenzierterAbschnitt.vonZuerst))
					.collect(Collectors.toList());

				List<FuehrungsformAttribute> fuehrungsformAttributeZugeschnitten = LinearReferenzierteAttribute
					.schneideAttributeAufLineareReferenzZu(linearReferenzierterAbschnitt, fuehrungsformAttribute);

				if (Math.abs(groessteDifferenz) > DIFFERENZ_IN_HAENDIGKEIT_WAHRSCHEINLICHKEITEN_FUER_SEITENZUORDNUNG) {
					if (groessteDifferenz > 0) {
						potentiellInkonsistenteFuehrungsformLinks
							.addAll(fuehrungsformAttributeZugeschnitten);
						potentiellInkonsistenteRichtungAufAnteilLinks.merge(
							richtungsbasierteProjizierteAttribut.getRichtung(),
							linearReferenzierterAbschnitt.relativeLaenge(),
							Double::sum);
					} else {
						potentiellInkonsistenteFuehrungsformRechts
							.addAll(fuehrungsformAttributeZugeschnitten);
						potentiellInkonsistenteRichtungAufAnteilRechts.merge(
							richtungsbasierteProjizierteAttribut.getRichtung(),
							linearReferenzierterAbschnitt.relativeLaenge(),
							Double::sum);
					}
				} else {
					potentiellInkonsistenteFuehrungsformLinks
						.addAll(fuehrungsformAttributeZugeschnitten);
					potentiellInkonsistenteFuehrungsformRechts
						.addAll(fuehrungsformAttributeZugeschnitten);
					potentiellInkonsistenteRichtungAufAnteilLinks.merge(
						richtungsbasierteProjizierteAttribut.getRichtung(),
						linearReferenzierterAbschnitt.relativeLaenge(),
						Double::sum);
					potentiellInkonsistenteRichtungAufAnteilRechts.merge(
						richtungsbasierteProjizierteAttribut.getRichtung(),
						linearReferenzierterAbschnitt.relativeLaenge(),
						Double::sum);
				}

			}
		}
	}

	private boolean brauchKanteSeitenbezug(List<LinearReferenzierterAbschnitt> lineareReferenzen,
		Map<LinearReferenzierterAbschnitt, List<SeitenbezogeneProjizierteAttribute>> mapAufAttribute) {
		double confidenceInSeitenbezug = 0.;
		for (LinearReferenzierterAbschnitt linearReferenzierterAbschnitt : lineareReferenzen) {
			List<SeitenbezogeneProjizierteAttribute> matchingAttribute = mapAufAttribute.get(
				linearReferenzierterAbschnitt);
			if (matchingAttribute.isEmpty()) {
				continue;
			}

			matchingAttribute.sort(Comparator.comparing(SeitenbezogeneProjizierteAttribute::getHaendigkeit,
				Haendigkeit.vonLinksNachRechts));

			SeitenbezogeneProjizierteAttribute leftMost = matchingAttribute.get(0);
			SeitenbezogeneProjizierteAttribute rightMost = matchingAttribute.get(matchingAttribute.size() - 1);

			double differenz = Math.abs(
				leftMost.getHaendigkeit().getVorzeichenbehafteteWahrscheinlichkeit() - rightMost.getHaendigkeit()
					.getVorzeichenbehafteteWahrscheinlichkeit());

			confidenceInSeitenbezug += differenz * linearReferenzierterAbschnitt.relativeLaenge();
		}
		return confidenceInSeitenbezug > DIFFERENZ_IN_HAENDIGKEIT_WAHRSCHEINLICHKEITEN_FUER_SEITENBEZUG;
	}

	/**
	 * @param lineareReferenzen
	 * 	Liste and Linearen Referenzen die sich potentiell überlappen
	 * @return Konsolidierte List an Linearen Referenzen die sich nicht mehr überlappen
	 */
	private List<LinearReferenzierterAbschnitt> normalisiereLineareReferenzen(
		List<LinearReferenzierterAbschnitt> lineareReferenzen) {
		List<Double> metermarken = new ArrayList<>();
		for (LinearReferenzierterAbschnitt linearReferenzierterAbschnitt : lineareReferenzen) {
			metermarken.add(linearReferenzierterAbschnitt.getVonValue());
			metermarken.add(linearReferenzierterAbschnitt.getBisValue());
		}

		metermarken.sort(Double::compareTo);
		double previousDouble = metermarken.get(0);
		List<LinearReferenzierterAbschnitt> normalisierteLineareReferenzen = new ArrayList<>();
		for (int i = 1; i < metermarken.size(); i++) {
			Double currentDouble = metermarken.get(i);
			if (currentDouble != previousDouble) {
				normalisierteLineareReferenzen.add(LinearReferenzierterAbschnitt.of(previousDouble, currentDouble));
				previousDouble = currentDouble;
			} else if (i == metermarken.size() - 1) {
				int finalIndex = normalisierteLineareReferenzen.size() - 1;
				normalisierteLineareReferenzen
					.set(finalIndex,
						LinearReferenzierterAbschnitt.of(normalisierteLineareReferenzen.get(finalIndex).getVonValue(),
							1.));
			}
		}
		return normalisierteLineareReferenzen;
	}

	private List<LinearReferenzierterAbschnitt> erstelleNormalisiereLineareReferenzen(
		List<? extends LinearReferenzierteAttribute> grundnetzAttribute,
		List<? extends LinearReferenzierteAttribute> projizierteAttribute) {

		List<LinearReferenzierterAbschnitt> inkonsistenteLineareReferenzen = new ArrayList<>();
		inkonsistenteLineareReferenzen
			.addAll(grundnetzAttribute.stream().map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList()));
		inkonsistenteLineareReferenzen
			.addAll(projizierteAttribute.stream().map(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt)
				.collect(Collectors.toList()));

		return normalisiereLineareReferenzen(inkonsistenteLineareReferenzen);
	}

	private <T> T getNeueAttributeNachThreshhold(Map<T, Double> attribute, String gruppenName)
		throws MehrdeutigeAttributgruppeException, AttributgruppenanteilZuKleinException {
		Optional<T> mergedAttribute = attribute.entrySet().stream()
			.filter((Map.Entry<T, Double> e) -> e.getValue() > ATTRIBUTE_INTERSECTION_THRESHHOLD)
			.max(Map.Entry.comparingByValue())
			.map(Map.Entry::getKey);

		if (mergedAttribute.isEmpty()) {
			Collection<Double> anteile = attribute.values();
			if (anteile.stream().mapToDouble(x -> x).sum() > ATTRIBUTE_INTERSECTION_THRESHHOLD) {
				throw new MehrdeutigeAttributgruppeException(anteile, gruppenName);
			} else {
				throw new AttributgruppenanteilZuKleinException(anteile,
					gruppenName);
			}
		}
		return mergedAttribute.get();
	}

	protected List<FuehrungsformAttribute> mergeFuehrungsformAttribute(
		List<FuehrungsformAttribute> grundnetzAttribute, List<FuehrungsformAttribute> projizierteAttribute,
		QuellSystem quelleDerProjiziertenAttribute, double kanteLaenge, AttributeMergeFehlersammlung fehlersammlung,
		String seite) {

		interpoliereKleineLueckenUndKleineUeberschneidungen(
			projizierteAttribute, MIN_SEGMENT_LENGTH_METER / kanteLaenge);

		return mergeAttribute(grundnetzAttribute, projizierteAttribute, quelleDerProjiziertenAttribute,
			attributeMergeService::mergeFuehrungsformAttribute, fehlersammlung, seite);
	}

	protected List<ZustaendigkeitAttribute> mergeZustaendigkeitAttribute(
		List<ZustaendigkeitAttribute> grundnetzAttribute, List<ZustaendigkeitAttribute> projizierteAttribute,
		QuellSystem quelleDerProjiziertenAttribute, double kanteLaenge, AttributeMergeFehlersammlung fehlersammlung) {

		interpoliereKleineLueckenUndKleineUeberschneidungen(
			projizierteAttribute, MIN_SEGMENT_LENGTH_METER / kanteLaenge);

		return mergeAttribute(grundnetzAttribute, projizierteAttribute, quelleDerProjiziertenAttribute,
			attributeMergeService::mergeZustaendigkeitAttribute, fehlersammlung, "");
	}

	protected List<GeschwindigkeitAttribute> mergeGeschwindigkeitsAttribute(
		List<GeschwindigkeitAttribute> grundnetzAttribute, List<GeschwindigkeitAttribute> projizierteAttribute,
		QuellSystem quelleDerProjiziertenAttribute, double kanteLaenge, AttributeMergeFehlersammlung fehlersammlung) {

		interpoliereKleineLueckenUndKleineUeberschneidungen(
			projizierteAttribute, MIN_SEGMENT_LENGTH_METER / kanteLaenge);

		return mergeAttribute(grundnetzAttribute, projizierteAttribute, quelleDerProjiziertenAttribute,
			attributeMergeService::mergeGeschwindigkeitAttribute, fehlersammlung, "");
	}

	@SuppressWarnings("unchecked")
	private <T extends LinearReferenzierteAttribute> List<T> mergeAttribute(List<T> grundnetzAttribute,
		List<T> projizierteAttribute, QuellSystem quelleDerProjiziertenAttribute,
		MergeFunction<T> mergeFunction,
		AttributeMergeFehlersammlung fehlersammlung, String seite) {

		List<LinearReferenzierterAbschnitt> lineareReferenzen = erstelleNormalisiereLineareReferenzen(
			grundnetzAttribute,
			projizierteAttribute);

		Map<LinearReferenzierterAbschnitt, List<T>> grundnetzAttributeProSegment = findeAttributeFuerSegment(
			grundnetzAttribute, lineareReferenzen);
		Map<LinearReferenzierterAbschnitt, List<T>> projizierteAttributeProSegment = findeAttributeFuerSegment(
			projizierteAttribute, lineareReferenzen);

		List<T> nichtMinimierteAttribute = new ArrayList<>();
		for (LinearReferenzierterAbschnitt linearReferenzierterAbschnitt : lineareReferenzen) {
			// Es kann nur ein Grundnetzattribut pro segment geben
			T grundnetzValue = grundnetzAttributeProSegment.get(linearReferenzierterAbschnitt).get(0);
			try {
				T mergedValue = mergeFunction
					.merge(grundnetzValue, linearReferenzierterAbschnitt,
						projizierteAttributeProSegment.get(linearReferenzierterAbschnitt),
						quelleDerProjiziertenAttribute);
				nichtMinimierteAttribute.add(mergedValue);
			} catch (MehrdeutigeLinearReferenzierteAttributeException e) {
				e.setLinearReferenzierterAbschnitt(linearReferenzierterAbschnitt);
				e.setSeite(seite);
				fehlersammlung.addException(e);
				nichtMinimierteAttribute.add((T) grundnetzValue.withLinearReferenzierterAbschnitt(
					linearReferenzierterAbschnitt));
			}
		}

		return minimiereLinearReferenzierteAttribute(nichtMinimierteAttribute);
	}

	@SuppressWarnings("unchecked")
	private <T extends LinearReferenzierteAttribute> List<T> minimiereLinearReferenzierteAttribute(
		List<T> nichtMinimierteAttribute) {
		List<T> minimierteLinearReferenzierteAttribute = new ArrayList<>();
		for (T attributGruppe : nichtMinimierteAttribute) {

			if (minimierteLinearReferenzierteAttribute.isEmpty()) {
				minimierteLinearReferenzierteAttribute.add(attributGruppe);
			} else {
				T letztesAttribut = minimierteLinearReferenzierteAttribute
					.get(minimierteLinearReferenzierteAttribute.size() - 1);
				Optional<?> unionAttribute = LinearReferenzierteAttribute
					.union(letztesAttribut, attributGruppe);

				if (unionAttribute.isPresent()) {
					minimierteLinearReferenzierteAttribute.set(minimierteLinearReferenzierteAttribute.size() - 1,
						(T) unionAttribute.get());
				} else {
					minimierteLinearReferenzierteAttribute.add(attributGruppe);
				}
			}
		}
		return minimierteLinearReferenzierteAttribute;
	}

	private <T extends LinearReferenzierteAttribute> Map<LinearReferenzierterAbschnitt, List<T>> findeAttributeFuerSegment(
		List<T> attribute, List<LinearReferenzierterAbschnitt> lineareReferenzen) {

		Map<LinearReferenzierterAbschnitt, List<T>> mapAufAttributGruppen = new HashMap<>();

		for (LinearReferenzierterAbschnitt linearReferenzierterAbschnitt : lineareReferenzen) {

			List<T> matchingAttribute = attribute.stream()
				.filter(grundnetzAttribut -> grundnetzAttribut.getLinearReferenzierterAbschnitt().contains(
					linearReferenzierterAbschnitt, 0))
				.collect(Collectors.toList());
			mapAufAttributGruppen.put(linearReferenzierterAbschnitt, matchingAttribute);
		}
		return mapAufAttributGruppen;
	}

	@SuppressWarnings("unchecked")
	public <T extends LinearReferenzierteAttribute> void interpoliereKleineLueckenUndKleineUeberschneidungen(
		List<T> projizierteAttribute,
		double relativeMinimaleLaenge) {
		if (projizierteAttribute.isEmpty()) {
			return;
		}

		projizierteAttribute
			.sort(Comparator.comparing(LinearReferenzierteAttribute::getLinearReferenzierterAbschnitt,
				LinearReferenzierterAbschnitt.vonZuerst));

		T firstSegment = projizierteAttribute.get(0);
		if (firstSegment.getLinearReferenzierterAbschnitt().getVonValue() < relativeMinimaleLaenge) {
			projizierteAttribute.set(0,
				(T) firstSegment
					.withLinearReferenzierterAbschnitt(
						LinearReferenzierterAbschnitt.of(0,
							firstSegment.getLinearReferenzierterAbschnitt().getBisValue())));
		}

		T seg = projizierteAttribute.get(0);
		for (int i = 1; i < projizierteAttribute.size(); i++) {
			T nextSeg = projizierteAttribute.get(i);
			if (seg.getLinearReferenzierterAbschnitt().contains(nextSeg.getLinearReferenzierterAbschnitt(), 0)) {
				// Nochmal prüfen ob das auftritt, wenn nicht kann man den Algorithmus vereinfachen
				continue;
			}
			Optional<LinearReferenzierterAbschnitt> intersection = seg.getLinearReferenzierterAbschnitt()
				.intersection(nextSeg.getLinearReferenzierterAbschnitt());
			if (intersection.isPresent()) {
				// kleine Ueberlappungen interpolieren
				double intersectionLength = intersection.get().relativeLaenge();
				if (intersectionLength < relativeMinimaleLaenge) {
					double interpolationspunkt =
						seg.getLinearReferenzierterAbschnitt().getBisValue() - intersectionLength / 2;
					LinearReferenzierterAbschnitt interpoliertSeg = LinearReferenzierterAbschnitt
						.of(seg.getLinearReferenzierterAbschnitt().getVonValue(), interpolationspunkt);
					LinearReferenzierterAbschnitt interpoliertNext = LinearReferenzierterAbschnitt
						.of(interpolationspunkt, nextSeg.getLinearReferenzierterAbschnitt().getBisValue());

					projizierteAttribute
						.set(projizierteAttribute.indexOf(seg),
							(T) seg.withLinearReferenzierterAbschnitt(interpoliertSeg));
					projizierteAttribute.set(i, (T) nextSeg.withLinearReferenzierterAbschnitt(interpoliertNext));
				}
				seg = projizierteAttribute.get(i); // hole potentiell updated nextSeg
				continue;
			}
			if (nextSeg.getLinearReferenzierterAbschnitt().getVonValue() - seg.getLinearReferenzierterAbschnitt()
				.getBisValue() > relativeMinimaleLaenge) {
				seg = nextSeg;
				continue;
			}
			// interpolate
			double interpolationspunkt = seg.getLinearReferenzierterAbschnitt().getBisValue()
				+ (nextSeg.getLinearReferenzierterAbschnitt().getVonValue()
				- seg.getLinearReferenzierterAbschnitt().getBisValue()) / 2;
			LinearReferenzierterAbschnitt interpoliertSeg = LinearReferenzierterAbschnitt
				.of(seg.getLinearReferenzierterAbschnitt().getVonValue(), interpolationspunkt);
			LinearReferenzierterAbschnitt interpoliertNext = LinearReferenzierterAbschnitt
				.of(interpolationspunkt, nextSeg.getLinearReferenzierterAbschnitt().getBisValue());

			projizierteAttribute
				.set(projizierteAttribute.indexOf(seg), (T) seg.withLinearReferenzierterAbschnitt(interpoliertSeg));
			projizierteAttribute.set(i, (T) nextSeg.withLinearReferenzierterAbschnitt(interpoliertNext));

			seg = projizierteAttribute.get(i); // hole updated nextSeg
		}

		T lastSegment = projizierteAttribute.get(projizierteAttribute.size() - 1);
		if (1 - lastSegment.getLinearReferenzierterAbschnitt().getBisValue() < relativeMinimaleLaenge) {
			projizierteAttribute.set(projizierteAttribute.size() - 1,
				(T) lastSegment.withLinearReferenzierterAbschnitt(
					LinearReferenzierterAbschnitt.of(lastSegment.getLinearReferenzierterAbschnitt().getVonValue(), 1)));
		}
	}

	public interface MergeFunction<T extends LinearReferenzierteAttribute> {
		T merge(T grundnetzAttribute, LinearReferenzierterAbschnitt linearReferenzierterAbschnitt,
			List<T> projizierteAttribute,
			QuellSystem quelleDerZuMergendenAttribute) throws MehrdeutigeLinearReferenzierteAttributeException;
	}
}
