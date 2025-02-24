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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import de.wps.radvis.backend.netz.domain.entity.LinearReferenzierteAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenSeite;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KantenAttributeUebertragungService {

	private final Laenge minimaleSegmentLaenge;

	public KantenAttributeUebertragungService(Laenge minimaleSegmentLaenge) {
		this.minimaleSegmentLaenge = minimaleSegmentLaenge;
	}

	/**
	 * Überträgt alle Attribute von einer Kante auf andere Kanten
	 * 
	 * @param vonKante
	 * @param aufKanten
	 *     Kanten, auf die Attribute übertragen werden sollen. Diese müssen die gegebene Kante matchen
	 */
	@SuppressChangedEvents
	public void uebertrageAttribute(Kante vonKante, Set<Kante> aufKanten) {
		require(vonKante, notNullValue());
		require(aufKanten, notNullValue());
		require(!aufKanten.isEmpty());

		log.trace("Übertrage Attribute von Kante {} auf Kanten {}", vonKante, aufKanten.stream().map(Kante::toString)
			.collect(Collectors.joining(", ")));

		aufKanten.forEach(kante -> {
			if (vonKante.isZweiseitig()) {
				kante.changeSeitenbezug(true);
			}

			uebertrageFahrtrichtungAttribute(vonKante, kante);

			uebertrageZustaendigkeitAttribute(vonKante, kante);

			uebertrageGeschwindigkeitAttribute(vonKante, kante);

			uebertrageAllgemeineAttribute(vonKante, kante);

			uebertrageFuehrungsformAttribute(vonKante, kante);

			kante.defragmentiereLinearReferenzierteAttribute(minimaleSegmentLaenge);
		});
	}

	private void uebertrageAllgemeineAttribute(Kante vonKante, Kante aufKante) {
		aufKante.getKantenAttributGruppe().update(
			new HashSet<>(vonKante.getKantenAttributGruppe().getNetzklassen()),
			new HashSet<>(vonKante.getKantenAttributGruppe().getIstStandards()),
			vonKante.getKantenAttributGruppe().getKantenAttribute());
	}

	private void uebertrageGeschwindigkeitAttribute(Kante vonKante, Kante aufKante) {
		boolean stationierungsrichtungUmgekehrt = !LineStrings.haveSameStationierungsrichtung(vonKante.getGeometry(),
			aufKante.getGeometry());
		vonKante.getGeschwindigkeitAttributGruppe().getGeschwindigkeitAttribute().stream()
			.filter(attr -> liegtAttributAufNeuerKante(vonKante, aufKante, attr))
			.map(attr -> (GeschwindigkeitAttribute) passeLineareReferenzenAn(vonKante, aufKante, attr))
			.forEach(attr -> {
				if (stationierungsrichtungUmgekehrt) {
					aufKante.getGeschwindigkeitAttributGruppe().insert(attr.withUmgekehrterStationierungsrichtung());
				} else {
					aufKante.getGeschwindigkeitAttributGruppe().insert(attr);
				}
			});
	}

	private void uebertrageFuehrungsformAttribute(Kante vonKante, Kante aufKante) {
		boolean stationierungsrichtungUmgekehrt = !LineStrings.haveSameStationierungsrichtung(
			vonKante.getGeometry(),
			aufKante.getGeometry());

		if (aufKante.isZweiseitig()) {
			vonKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().stream()
				.filter(attr -> liegtAttributAufNeuerKante(vonKante, aufKante, attr))
				.map(attr -> (FuehrungsformAttribute) passeLineareReferenzenAn(vonKante, aufKante, attr))
				.forEach(attr -> {
					if (stationierungsrichtungUmgekehrt) {
						aufKante.getFuehrungsformAttributGruppe()
							.insertRechts(attr.withUmgekehrterStationierungsrichtung());
					} else {
						aufKante.getFuehrungsformAttributGruppe().insertLinks(attr);
					}
				});

			vonKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().stream()
				.filter(attr -> liegtAttributAufNeuerKante(vonKante, aufKante, attr))
				.map(attr -> (FuehrungsformAttribute) passeLineareReferenzenAn(vonKante, aufKante, attr))
				.forEach(attr -> {
					if (stationierungsrichtungUmgekehrt) {
						aufKante.getFuehrungsformAttributGruppe()
							.insertLinks(attr.withUmgekehrterStationierungsrichtung());
					} else {
						aufKante.getFuehrungsformAttributGruppe().insertRechts(attr);
					}
				});
		} else {
			vonKante.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().stream()
				.filter(attr -> liegtAttributAufNeuerKante(vonKante, aufKante, attr))
				.map(attr -> (FuehrungsformAttribute) passeLineareReferenzenAn(vonKante, aufKante, attr))
				.forEach(attr -> {
					if (stationierungsrichtungUmgekehrt) {
						aufKante.getFuehrungsformAttributGruppe().insert(attr.withUmgekehrterStationierungsrichtung());
					} else {
						aufKante.getFuehrungsformAttributGruppe().insert(attr);
					}
				});

		}
	}

	private void uebertrageZustaendigkeitAttribute(Kante vonKante, Kante aufKante) {
		vonKante.getZustaendigkeitAttributGruppe().getImmutableZustaendigkeitAttribute().stream()
			.filter(attr -> liegtAttributAufNeuerKante(vonKante, aufKante, attr))
			.map(attr -> (ZustaendigkeitAttribute) passeLineareReferenzenAn(vonKante, aufKante, attr))
			.forEach(attr -> {
				aufKante.getZustaendigkeitAttributGruppe().insert(attr);
			});
	}

	private void uebertrageFahrtrichtungAttribute(Kante vonKante, Kante aufKante) {
		boolean stationierungsrichtungUmgekehrt = !LineStrings.haveSameStationierungsrichtung(vonKante.getGeometry(),
			aufKante.getGeometry());
		if (aufKante.isZweiseitig()) {
			if (stationierungsrichtungUmgekehrt) {
				aufKante.getFahrtrichtungAttributGruppe().update(
					vonKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts(),
					vonKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks());
			} else {
				aufKante.getFahrtrichtungAttributGruppe().update(
					vonKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks(),
					vonKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts());
			}
		} else {
			if (stationierungsrichtungUmgekehrt) {
				aufKante.getFahrtrichtungAttributGruppe().update(
					vonKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks().umgedreht(),
					vonKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks().umgedreht());
			} else {
				aufKante.getFahrtrichtungAttributGruppe().update(
					vonKante.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks(),
					vonKante.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts());
			}
		}
	}

	private LinearReferenzierteAttribute passeLineareReferenzenAn(Kante vonKante, Kante aufKante,
		LinearReferenzierteAttribute attribute) {
		LineString segment = attribute.getLinearReferenzierterAbschnitt()
			.toSegment(vonKante.getGeometry());
		LinearReferenzierterAbschnitt angepassterLRAbschnitt = LinearReferenzierterAbschnitt
			.of(aufKante.getGeometry(), segment);
		return attribute.withLinearReferenzierterAbschnitt(angepassterLRAbschnitt);
	}

	private boolean liegtAttributAufNeuerKante(Kante vonKante, Kante aufKante,
		LinearReferenzierteAttribute attribute) {
		LinearReferenzierterAbschnitt kanteUeberschneidungAufAlterKante = LinearReferenzierterAbschnitt
			.of(vonKante.getGeometry(), aufKante.getGeometry());
		return attribute.getLinearReferenzierterAbschnitt()
			.intersects(kanteUeberschneidungAufAlterKante);
	}

	/**
	 * Überträgt alle Attribute, die bei allen "vonKanten" an dem angegebenen Knoten gleich sind auf die "aufKante". Die
	 * auf-Kante muss zudem in dem übergebenen Knoten anfangen.
	 *
	 * Ausnahme: Netzklassen und Ist-Standards werden gemerged, da dies bei diesen Attributen möglich ist, was z.B. bei
	 * der Belagart nicht geht. Hintergrund ist das Schließen von Attributlücken, wo das Mergen für die
	 * Attributübernahme wünschenswert ist. Kommt durch eine von-Kante "RadNETZ Alltag" und durch eine andere "RadNETZ
	 * Freizeit" an, dann übertragen wir das auf die "aufKante" und die Attributübernahme kann dann entscheiden ob diese
	 * gemergten Netzklassen dann weiter betrachtet werden.
	 *
	 * Der angegebene Knoten muss dabei entweder von- oder nach-Knoten <i>aller</i> von-Kanten sein. Heißt es darf keine
	 * von-Kante geben die mit dem angegebenen Knoten nicht verbunden ist.
	 *
	 * Die Idee mit dem Knoten: Es werden nur Attribut übernommen, die am Start oder Ende der von-Kanten gesetzt sind,
	 * was natürlich nur die linear referenzierbaren Attribute betrifft. Die auf-Kante erhält also alle Attribute, die
	 * an der Stelle vom Knoten an allen von-Kanten gleich sind.
	 *
	 * @param netzklassenIntersection
	 *     Wenn "true", dann werden Netzklassen verschnitten, also nur die gleichen Netzklassen aus allen
	 *     von-Kanten übernommen. Wenn "false", dann werden die Netzklassen aller von-Kanten vereinigt.
	 */
	public void uebertrageGleicheAttribute(List<Kante> vonKanten, Knoten knoten, Kante aufKante,
		boolean netzklassenIntersection) {
		require(aufKante.getVonKnoten().equals(knoten));

		uebertrageGleicheKantenAttribute(vonKanten, aufKante, netzklassenIntersection);

		uebertrageGleicheFuehrungsformAttribute(vonKanten, knoten, aufKante);

		uebertrageGleicheGeschwindigkeitsAttribute(vonKanten, knoten, aufKante);

		uebertrageGleicheFahrtrichtungsAttribute(vonKanten, aufKante);

		uebertrageGleicheZustaendigkeitsAttribute(vonKanten, knoten, aufKante);
	}

	/**
	 * Überträgt alle allgemeinen Attribute, die bei allen "vonKanten" gleich sind, auf die "aufKante".
	 *
	 * Ausnahme: Netzklassen und Ist-Standards werden gemerged, da dies bei diesen Attributen möglich ist, was z.B. bei
	 * der Belagart nicht geht. Hintergrund ist das Schließen von Attributlücken, wo das Mergen für die
	 * Attributübernahme wünschenswert ist. Kommt durch eine von-Kante "RadNETZ Alltag" und durch eine andere "RadNETZ
	 * Freizeit" an, dann übertragen wir das auf die "aufKante" und die Attributübernahme kann dann entscheiden, ob
	 * diese gemergten Netzklassen dann weiter betrachtet werden.
	 */
	private void uebertrageGleicheKantenAttribute(List<Kante> vonKanten, Kante aufKante,
		boolean netzklassenIntersection) {

		Set<Netzklasse> netzklassen;
		Set<IstStandard> istStandards;
		if (netzklassenIntersection) {
			netzklassen = new HashSet<>(vonKanten.get(0).getKantenAttributGruppe().getNetzklassen());
			for (int i = 1; i < vonKanten.size(); i++) {
				netzklassen.retainAll(vonKanten.get(i).getKantenAttributGruppe().getNetzklassen());
			}

			istStandards = new HashSet<>(vonKanten.get(0).getKantenAttributGruppe().getIstStandards());
			for (int i = 1; i < vonKanten.size(); i++) {
				istStandards.retainAll(vonKanten.get(i).getKantenAttributGruppe().getIstStandards());
			}

			if (!KantenAttributGruppe.istStandardsAllowedForNetzklassen(netzklassen, istStandards)) {
				istStandards = Collections.emptySet();
			}
		} else {
			netzklassen = vonKanten.stream()
				.flatMap(k -> k.getKantenAttributGruppe().getNetzklassen().stream())
				.collect(Collectors.toSet());

			istStandards = vonKanten.stream()
				.flatMap(k -> k.getKantenAttributGruppe().getIstStandards().stream())
				.collect(Collectors.toSet());
		}
		aufKante.getKantenAttributGruppe().getNetzklassen().addAll(netzklassen);
		aufKante.getKantenAttributGruppe().getIstStandards().addAll(istStandards);

		uebertrage(vonKanten, k -> k.getKantenAttributGruppe().getKantenAttribute().getWegeNiveau().orElse(null),
			value -> aufKante.getKantenAttributGruppe().getKantenAttribute().setWegeNiveau(value));

		uebertrage(vonKanten, k -> k.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung(),
			value -> aufKante.getKantenAttributGruppe().getKantenAttribute().setBeleuchtung(value));

		uebertrage(vonKanten, k -> k.getKantenAttributGruppe().getKantenAttribute().getUmfeld(),
			value -> aufKante.getKantenAttributGruppe().getKantenAttribute().setUmfeld(value));

		uebertrage(vonKanten, k -> k.getKantenAttributGruppe().getKantenAttribute().getStrassenkategorieRIN().orElse(
			null),
			value -> aufKante.getKantenAttributGruppe().getKantenAttribute().setStrassenkategorieRIN(value));

		uebertrage(vonKanten, k -> k.getKantenAttributGruppe().getKantenAttribute().getStrassenquerschnittRASt06(),
			value -> aufKante.getKantenAttributGruppe().getKantenAttribute().setStrassenquerschnittRASt06(value));

		uebertrage(vonKanten, k -> k.getKantenAttributGruppe().getKantenAttribute().getDtvFussverkehr().orElse(null),
			value -> aufKante.getKantenAttributGruppe().getKantenAttribute().setDtvFussverkehr(value));

		uebertrage(vonKanten, k -> k.getKantenAttributGruppe().getKantenAttribute().getDtvRadverkehr().orElse(null),
			value -> aufKante.getKantenAttributGruppe().getKantenAttribute().setDtvRadverkehr(value));

		uebertrage(vonKanten, k -> k.getKantenAttributGruppe().getKantenAttribute().getDtvPkw().orElse(null),
			value -> aufKante.getKantenAttributGruppe().getKantenAttribute().setDtvPkw(value));

		uebertrage(vonKanten, k -> k.getKantenAttributGruppe().getKantenAttribute().getSv().orElse(null),
			value -> aufKante.getKantenAttributGruppe().getKantenAttribute().setSv(value));
	}

	private void uebertrageGleicheFuehrungsformAttribute(List<Kante> vonKanten, Knoten knoten, Kante aufKante) {
		FuehrungsformAttribute.FuehrungsformAttributeBuilder fuehrungsformRechts = FuehrungsformAttribute.builder();
		FuehrungsformAttribute.FuehrungsformAttributeBuilder fuehrungsformLinks = FuehrungsformAttribute.builder();

		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.RECHTS : KantenSeite.LINKS).getBelagArt(),
			value -> fuehrungsformRechts.belagArt(value));
		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.LINKS : KantenSeite.RECHTS).getBelagArt(),
			value -> fuehrungsformLinks.belagArt(value));

		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.RECHTS : KantenSeite.LINKS)
				.getOberflaechenbeschaffenheit(),
			value -> fuehrungsformRechts.oberflaechenbeschaffenheit(value));
		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.LINKS : KantenSeite.RECHTS)
				.getOberflaechenbeschaffenheit(),
			value -> fuehrungsformLinks.oberflaechenbeschaffenheit(value));

		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.RECHTS : KantenSeite.LINKS).getBordstein(),
			value -> fuehrungsformRechts.bordstein(value));
		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.LINKS : KantenSeite.RECHTS).getBordstein(),
			value -> fuehrungsformLinks.bordstein(value));

		AtomicReference<Radverkehrsfuehrung> radverkehrsfuehrungLinks = new AtomicReference<>(
			Radverkehrsfuehrung.UNBEKANNT);
		AtomicReference<Radverkehrsfuehrung> radverkehrsfuehrungRechts = new AtomicReference<>(
			Radverkehrsfuehrung.UNBEKANNT);
		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.RECHTS : KantenSeite.LINKS)
				.getRadverkehrsfuehrung(),
			value -> {
				fuehrungsformRechts.radverkehrsfuehrung(value);
				radverkehrsfuehrungRechts.set(value);
			});
		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.LINKS : KantenSeite.RECHTS)
				.getRadverkehrsfuehrung(),
			value -> {
				fuehrungsformLinks.radverkehrsfuehrung(value);
				radverkehrsfuehrungLinks.set(value);
			});

		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.RECHTS : KantenSeite.LINKS)
				.getBenutzungspflicht(),
			value -> fuehrungsformRechts.benutzungspflicht(value));
		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.LINKS : KantenSeite.RECHTS)
				.getBenutzungspflicht(),
			value -> fuehrungsformLinks.benutzungspflicht(value));

		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.RECHTS : KantenSeite.LINKS).getParkenForm(),
			value -> fuehrungsformRechts.parkenForm(value));
		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.LINKS : KantenSeite.RECHTS).getParkenForm(),
			value -> fuehrungsformLinks.parkenForm(value));

		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.RECHTS : KantenSeite.LINKS).getParkenTyp(),
			value -> fuehrungsformRechts.parkenTyp(value));
		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.LINKS : KantenSeite.RECHTS).getParkenTyp(),
			value -> fuehrungsformLinks.parkenTyp(value));

		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.RECHTS : KantenSeite.LINKS).getBreite().orElse(
					null),
			value -> fuehrungsformRechts.breite(value));
		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.LINKS : KantenSeite.RECHTS).getBreite().orElse(
					null),
			value -> fuehrungsformLinks.breite(value));

		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.RECHTS : KantenSeite.LINKS).getAbsenkung(),
			value -> fuehrungsformRechts.absenkung(value));
		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.LINKS : KantenSeite.RECHTS).getAbsenkung(),
			value -> fuehrungsformLinks.absenkung(value));

		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.RECHTS : KantenSeite.LINKS).getSchaeden(),
			value -> fuehrungsformRechts.schaeden(value));
		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.LINKS : KantenSeite.RECHTS).getSchaeden(),
			value -> fuehrungsformLinks.schaeden(value));

		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.RECHTS : KantenSeite.LINKS).getBeschilderung(),
			value -> {
				if (value.isValidForRadverkehrsfuehrung(radverkehrsfuehrungRechts.get())) {
					fuehrungsformRechts.beschilderung(value);
				}
			});
		uebertrage(
			vonKanten,
			k -> k.getFuehrungsformAttributeAnKnoten(
				knoten, k.getNachKnoten().equals(knoten) ? KantenSeite.LINKS : KantenSeite.RECHTS).getBeschilderung(),
			value -> {
				if (value.isValidForRadverkehrsfuehrung(radverkehrsfuehrungRechts.get())) {
					fuehrungsformLinks.beschilderung(value);
				}
			});

		// Übertrage STS Informationen nur, wenn sie zusammen mit der Radverkehrsführung gültig sind. Daher diese
		// Attribute erst einmal sammeln, dann prüfen und erst dann in den Builder schreiben.
		AtomicReference<TrennstreifenForm> stsForm = new AtomicReference<>();
		AtomicReference<Laenge> stsBreite = new AtomicReference<>();
		AtomicReference<TrennungZu> stsTrennungZu = new AtomicReference<>();

		// Trennstreifen A
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.LINKS).getTrennstreifenFormLinks().orElse(
					null)
				: k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.RECHTS).getTrennstreifenFormRechts().orElse(
					null),
			value -> stsForm.set(value));
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.LINKS).getTrennstreifenBreiteLinks().orElse(
					null)
				: k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.RECHTS).getTrennstreifenBreiteRechts().orElse(
					null),
			value -> stsBreite.set(value));
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.LINKS).getTrennstreifenTrennungZuLinks()
					.orElse(null)
				: k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.RECHTS).getTrennstreifenTrennungZuRechts()
					.orElse(null),
			value -> stsTrennungZu.set(value));
		if (FuehrungsformAttribute.isTrennstreifenCorrect(radverkehrsfuehrungLinks.get(), stsForm.get(), stsBreite
			.get(), stsTrennungZu.get())) {
			fuehrungsformLinks.trennstreifenFormLinks(stsForm.get());
			fuehrungsformLinks.trennstreifenBreiteLinks(stsBreite.get());
			fuehrungsformLinks.trennstreifenTrennungZuLinks(stsTrennungZu.get());
		}

		// Trennstreifen B
		stsForm.set(null);
		stsBreite.set(null);
		stsTrennungZu.set(null);
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.LINKS).getTrennstreifenFormRechts().orElse(
					null)
				: k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.RECHTS).getTrennstreifenFormLinks().orElse(
					null),
			value -> stsForm.set(value));
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.LINKS).getTrennstreifenBreiteRechts().orElse(
					null)
				: k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.RECHTS).getTrennstreifenBreiteLinks().orElse(
					null),
			value -> stsBreite.set(value));
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.LINKS).getTrennstreifenTrennungZuRechts()
					.orElse(null)
				: k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.RECHTS).getTrennstreifenTrennungZuLinks()
					.orElse(null),
			value -> stsTrennungZu.set(value));
		if (FuehrungsformAttribute.isTrennstreifenCorrect(radverkehrsfuehrungLinks.get(), stsForm.get(), stsBreite
			.get(), stsTrennungZu.get())) {
			fuehrungsformLinks.trennstreifenFormRechts(stsForm.get());
			fuehrungsformLinks.trennstreifenBreiteRechts(stsBreite.get());
			fuehrungsformLinks.trennstreifenTrennungZuRechts(stsTrennungZu.get());
		}

		// Trennstreifen C
		stsForm.set(null);
		stsBreite.set(null);
		stsTrennungZu.set(null);
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.RECHTS).getTrennstreifenFormLinks().orElse(
					null)
				: k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.LINKS).getTrennstreifenFormRechts().orElse(
					null),
			value -> stsForm.set(value));
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.RECHTS).getTrennstreifenBreiteLinks().orElse(
					null)
				: k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.LINKS).getTrennstreifenBreiteRechts().orElse(
					null),
			value -> stsBreite.set(value));
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.RECHTS).getTrennstreifenTrennungZuLinks()
					.orElse(null)
				: k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.LINKS).getTrennstreifenTrennungZuRechts()
					.orElse(null),
			value -> stsTrennungZu.set(value));
		if (FuehrungsformAttribute.isTrennstreifenCorrect(radverkehrsfuehrungRechts.get(), stsForm.get(), stsBreite
			.get(), stsTrennungZu.get())) {
			fuehrungsformRechts.trennstreifenFormLinks(stsForm.get());
			fuehrungsformRechts.trennstreifenBreiteLinks(stsBreite.get());
			fuehrungsformRechts.trennstreifenTrennungZuLinks(stsTrennungZu.get());
		}

		// Trennstreifen D
		stsForm.set(null);
		stsBreite.set(null);
		stsTrennungZu.set(null);
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.RECHTS).getTrennstreifenFormRechts().orElse(
					null)
				: k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.LINKS).getTrennstreifenFormLinks().orElse(
					null),
			value -> stsForm.set(value));
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.RECHTS).getTrennstreifenBreiteRechts().orElse(
					null)
				: k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.LINKS).getTrennstreifenBreiteLinks().orElse(
					null),
			value -> stsBreite.set(value));
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.RECHTS).getTrennstreifenTrennungZuRechts()
					.orElse(null)
				: k.getFuehrungsformAttributeAnKnoten(knoten, KantenSeite.LINKS).getTrennstreifenTrennungZuLinks()
					.orElse(null),
			value -> stsTrennungZu.set(value));
		if (FuehrungsformAttribute.isTrennstreifenCorrect(radverkehrsfuehrungRechts.get(), stsForm.get(), stsBreite
			.get(), stsTrennungZu.get())) {
			fuehrungsformRechts.trennstreifenFormRechts(stsForm.get());
			fuehrungsformRechts.trennstreifenBreiteRechts(stsBreite.get());
			fuehrungsformRechts.trennstreifenTrennungZuRechts(stsTrennungZu.get());
		}

		aufKante.getFuehrungsformAttributGruppe().replaceFuehrungsformAttribute(List.of(fuehrungsformLinks.build()),
			List.of(fuehrungsformRechts.build()));
	}

	private void uebertrageGleicheGeschwindigkeitsAttribute(List<Kante> vonKanten, Knoten knoten, Kante aufKante) {
		GeschwindigkeitAttribute.GeschwindigkeitAttributeBuilder geschwindigkeit = GeschwindigkeitAttribute.builder();

		uebertrage(vonKanten, k -> k.getGeschwindigkeitAttributeAnKnoten(knoten).getOrtslage().orElse(null),
			value -> geschwindigkeit.ortslage(value));

		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getGeschwindigkeitAttributeAnKnoten(knoten).getHoechstgeschwindigkeit()
				: k.getGeschwindigkeitAttributeAnKnoten(knoten)
					.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung().orElse(null),
			value -> geschwindigkeit.hoechstgeschwindigkeit(value));
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(knoten)
				? k.getGeschwindigkeitAttributeAnKnoten(knoten)
					.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung().orElse(null)
				: k.getGeschwindigkeitAttributeAnKnoten(knoten).getHoechstgeschwindigkeit(),
			value -> geschwindigkeit.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(value));

		aufKante.getGeschwindigkeitAttributGruppe().replaceGeschwindigkeitAttribute(List.of(geschwindigkeit.build()));
	}

	private void uebertrageGleicheFahrtrichtungsAttribute(List<Kante> vonKanten, Kante aufKante) {
		FahrtrichtungAttributGruppe.FahrtrichtungAttributGruppeBuilder fahrtrichtung = FahrtrichtungAttributGruppe
			.builder();

		fahrtrichtung.isZweiseitig(vonKanten.stream().anyMatch(k -> k.isZweiseitig()));

		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(aufKante.getVonKnoten()) || k.getVonKnoten().equals(aufKante.getNachKnoten())
				? k.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts()
				: k.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks().umgedreht(),
			value -> fahrtrichtung.fahrtrichtungRechts(value));
		uebertrage(
			vonKanten,
			k -> k.getNachKnoten().equals(aufKante.getVonKnoten()) || k.getVonKnoten().equals(aufKante.getNachKnoten())
				? k.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks()
				: k.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts().umgedreht(),
			value -> fahrtrichtung.fahrtrichtungLinks(value));

		FahrtrichtungAttributGruppe f = fahrtrichtung.build();
		aufKante.getFahrtrichtungAttributGruppe().changeSeitenbezug(f.isZweiseitig());
		aufKante.getFahrtrichtungAttributGruppe().update(f.getFahrtrichtungLinks(), f.getFahrtrichtungRechts());
	}

	private void uebertrageGleicheZustaendigkeitsAttribute(List<Kante> vonKanten, Knoten knoten, Kante aufKante) {
		ZustaendigkeitAttribute.ZustaendigkeitAttributeBuilder zustaendigkeit = ZustaendigkeitAttribute.builder();

		uebertrage(
			vonKanten,
			k -> k.getZustaendigkeitAttributeAnKnoten(knoten).getBaulastTraeger().orElse(null),
			value -> zustaendigkeit.baulastTraeger(value));
		uebertrage(
			vonKanten,
			k -> k.getZustaendigkeitAttributeAnKnoten(knoten).getErhaltsZustaendiger().orElse(null),
			value -> zustaendigkeit.erhaltsZustaendiger(value));
		uebertrage(
			vonKanten,
			k -> k.getZustaendigkeitAttributeAnKnoten(knoten).getUnterhaltsZustaendiger().orElse(null),
			value -> zustaendigkeit.unterhaltsZustaendiger(value));
		uebertrage(
			vonKanten,
			k -> k.getZustaendigkeitAttributeAnKnoten(knoten).getVereinbarungsKennung().orElse(null),
			value -> zustaendigkeit.vereinbarungsKennung(value));

		aufKante.getZustaendigkeitAttributGruppe().replaceZustaendigkeitAttribute(List.of(zustaendigkeit.build()));
	}

	/**
	 * Wendet ein Attribut der vonKanten auf den Applier an, wenn das Attribut bei allen Kanten gleich ist. Das Attribut
	 * wird dabei von dem attributeGetter geholt und mittels Streaming .distinct() auf Gleichheit überhrpüft.
	 *
	 * @param vonKanten
	 *     Kanten von denen ein gewisses Attribut geholt werden soll.
	 * @param attributeGetter
	 *     Funktion um von einer Kante einen Attributwert zu holen.
	 * @param attributApplier
	 *     Funktion, die aufgerufen wird, wenn die geholten Attribute bei allen Kanten gleich sind.
	 * @param <T>
	 *     Typ des Attributwertes.
	 */
	private <T> void uebertrage(
		List<Kante> vonKanten,
		Function<Kante, T> attributeGetter,
		Consumer<T> attributApplier) {

		List<T> attribute = vonKanten.stream()
			.map(kante -> attributeGetter.apply(kante))
			.distinct()
			.toList();

		if (attribute.size() == 1 && attribute.get(0) != null) {
			// Attribut bei allen Kanten gleich und kann entsprechend übertragen werden
			attributApplier.accept(attribute.get(0));
		}
	}
}
