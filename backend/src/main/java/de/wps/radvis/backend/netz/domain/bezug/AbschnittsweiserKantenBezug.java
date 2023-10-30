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

package de.wps.radvis.backend.netz.domain.bezug;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;

import org.springframework.data.util.Pair;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.entity.Topologie;
import de.wps.radvis.backend.netz.domain.valueObject.Aequivalenzklasse;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@EqualsAndHashCode
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@MappedSuperclass
@Slf4j
public class AbschnittsweiserKantenBezug {

	public AbschnittsweiserKantenBezug(Kante kante, LinearReferenzierterAbschnitt linearReferenzierterAbschnitt) {
		require(kante, notNullValue());
		require(linearReferenzierterAbschnitt, notNullValue());
		this.kante = kante;
		this.linearReferenzierterAbschnitt = linearReferenzierterAbschnitt;
	}

	@ManyToOne
	private Kante kante;

	@Embedded
	private LinearReferenzierterAbschnitt linearReferenzierterAbschnitt;

	public Optional<AbschnittsweiserKantenBezug> intersection(AbschnittsweiserKantenBezug other) {
		if (!kante.equals(other.getKante())) {
			return Optional.empty();
		}

		Optional<LinearReferenzierterAbschnitt> intersection = linearReferenzierterAbschnitt.intersection(
			other.linearReferenzierterAbschnitt);

		return intersection.map(LR -> new AbschnittsweiserKantenBezug(kante, LR));
	}

	// Achtung, diese Methode berücksichtigt keine linearen Referenzen
	public static Optional<StreckeVonKanten> erstelleNetzbezugLineString(
		List<AbschnittsweiserKantenBezug> alleAbschnittsweisenKanten) {

		if (alleAbschnittsweisenKanten.isEmpty()) {
			return Optional.empty();
		} else if (alleAbschnittsweisenKanten.size() == 1) {
			return Optional.of(new StreckeVonKanten(alleAbschnittsweisenKanten.get(0).getKante()));
		}

		// ----------------------- Try Kanten mit Reihenfolge in Liste aneinanderzuhaengen -----------------------------
		// Wenn der Netzbezug durch matching oder Routing erstellt wurde, befinden sich die Kanten oft schon in der
		// richtigen Reihenfolge in der alleAbschnittsweisenKanten-Liste. Deshalb wird hier ersteinmal probiert
		// sie der Reihe nach aneinanderzuhaengen.
		StreckeVonKanten result = new StreckeVonKanten(alleAbschnittsweisenKanten.get(0).getKante());
		for (int i = 1; i < alleAbschnittsweisenKanten.size(); i++) {
			Kante hinzuzufuegendeKante = alleAbschnittsweisenKanten.get(i).getKante();
			if (!result.canAddZuStrecke(hinzuzufuegendeKante.getVonKnoten(), hinzuzufuegendeKante.getNachKnoten())) {
				break;
			}
			result.addKante(hinzuzufuegendeKante, false);
		}
		if (result.getKanten().size() == alleAbschnittsweisenKanten.size()) {
			return Optional.of(result);
		}

		// --------------------------------- Kanten zu Teilstrecken zusammensetzten ------------------------------------
		Map<Knoten, List<Kante>> topologischeMapkanten = Topologie.erstelleTopologieMapAusKanten(
			alleAbschnittsweisenKanten.stream()
				.map(AbschnittsweiserKantenBezug::getKante)
				.collect(Collectors.toList())
		);

		// Sackgassen oder Kreuzungspunkte (Knoten mit Grad!=2) sammeln
		Set<Knoten> endpunkteVonTeilstrecken = new HashSet<>();
		topologischeMapkanten.forEach((knoten, kantes) -> {
			if (kantes.size() != 2) {
				endpunkteVonTeilstrecken.add(knoten);
			}
		});

		List<Kante> abzuarbeitendeKanten = alleAbschnittsweisenKanten.stream()
			.map(AbschnittsweiserKantenBezug::getKante)
			.collect(Collectors.toList());

		// Die verschiedenen Teilstrecken (Strecken zwischen Knoten mit Grad!=2) durchgehen:
		// Ausgehend von der ersten Kante in alleAbschnittsweisenKanten jeweils nach vorne und hinten bis zu einem
		// Endpunktknoten (Grad != 2) gehen und dies zu einer Teilstrecke zusammensetzten.
		List<StreckeVonKanten> teilstrecken = new ArrayList<>();
		while (!abzuarbeitendeKanten.isEmpty()) {
			Kante currentKante = abzuarbeitendeKanten.get(0);
			abzuarbeitendeKanten.remove(currentKante);

			StreckeVonKanten streckeVonKanten = new StreckeVonKanten(currentKante,
				endpunkteVonTeilstrecken.contains(currentKante.getVonKnoten()),
				endpunkteVonTeilstrecken.contains(currentKante.getNachKnoten()));

			sucheBisEndpunkt(topologischeMapkanten, abzuarbeitendeKanten, endpunkteVonTeilstrecken, streckeVonKanten,
				true);
			sucheBisEndpunkt(topologischeMapkanten, abzuarbeitendeKanten, endpunkteVonTeilstrecken, streckeVonKanten,
				false);

			teilstrecken.add(streckeVonKanten);
		}

		// --------------------------------- Teilstrecken zusammensetzten ----------------------------------
		// Die uebrig gebliebenen Teilstrecken probieren moeglichst gut zusammenzusetzten, unter Beruecksichtigung von
		// Wurmfortsaetzen, Loops und doppelten Verbindungen zwischen Knoten.
		mergeTeilstrecken(teilstrecken);
		if (teilstrecken.size() == 1) {
			return Optional.of(teilstrecken.get(0));
		}
		log.info(
			"Der NetzbezugLineString konnte für diese Route nicht eindeutig zusammengesetzt werden: Es gibt insgesamt {} Teilstrecken.",
			teilstrecken.size());

		// Isolierte Teilstrecken finden
		Map<Knoten, List<StreckeVonKanten>> knotenListMap = Topologie.erstelleTopologieMapAusStrecken(teilstrecken);
		AtomicInteger isolierteTeilstrecken = new AtomicInteger();
		teilstrecken.forEach(strecke -> {
			int vonKnotenGrad = knotenListMap.get(strecke.getVonKnoten()).size();
			int nachKnotenGrad = knotenListMap.get(strecke.getNachKnoten()).size();
			if (vonKnotenGrad == 1 && nachKnotenGrad == 1) {
				isolierteTeilstrecken.getAndIncrement();
			}
		});
		if (isolierteTeilstrecken.get() > 0) {
			log.info("Diese Route enthaelt {} isolierte Teilstrecken", isolierteTeilstrecken);
		}

		double minLaengeTeilstrecke = 10;
		List<StreckeVonKanten> teilstreckenMitMindestLaenge = teilstrecken.stream()
			.filter(streckeVonKanten -> streckeVonKanten.getStrecke().getLength() > minLaengeTeilstrecke)
			.collect(Collectors.toList());

		if (teilstreckenMitMindestLaenge.size() == 1) {
			log.info("Es wurden {} Teilstrecken mit einer Laenge kleiner als {}m aussortiert",
				teilstrecken.size() - 1, minLaengeTeilstrecke);
			return Optional.of(teilstreckenMitMindestLaenge.get(0));
		}

		return Optional.empty();
	}

	/*
	 * Erklärung zum Vorgehen:
	 * Am Anfang wird wieder eine topologischeMap erstellt und dann durch alle Knoten in dieser Iteriert.
	 * Es wird für diesen Knoten eine sog. "eingehendeStrecke" und eine "ausgehendeStrecke" bestimmt.
	 * Auf die Eingehende Strecke werden alle weiteren Strecken an diesem Knoten hinzugemerged.
	 * Zuerst Wurmfortsaetze zu Loops konvertieren.
	 * Dann alle Loops auf die Eingehende Strecke mergen.
	 * Am Ende die einzig uebrig bleibende Strecke (ausgehende Strecke) auf Eingehende Strecke mergen.
	 * -> Es sollten keine Strecken mehr am Knoten uebrig sein
	 * -> Zu naechstem Knoten springen
	 */
	private static void mergeTeilstrecken(
		List<StreckeVonKanten> teilstrecken) {
		require(!teilstrecken.isEmpty());
		if (teilstrecken.size() == 1) {
			return;
		}

		// --------------------------------- Teilstrecken vorvereinfachen ----------------------------------------------
		// Strecke eindeutig machen (verzweigungen rausnehmen), schon eindeutige Teilstrecken zusammensetzten
		// und auf Streckenebene bestimmen ob die Von- und Nachknoten Enden sind
		// -> Iterativ solange bis nichts mehr vereinfach + zusammengesetzt werden konnte

		// Eventuell ist hier noch Potential fuer Verbesserung: wenn ich alle aequivalenten Strecken entferne,
		// entferne ich ja auch doppelte, die ich vielleicht brauch zum Netzbezug erstellen.

		Map<Knoten, List<StreckeVonKanten>> topologischeMap = Topologie.erstelleTopologieMapAusStrecken(teilstrecken);
		boolean teilstreckenVeraendert = true;
		while (teilstreckenVeraendert && teilstrecken.size() > 1) {
			teilstreckenVeraendert = false;
			Topologie.updateStreckenEndenStatus(teilstrecken, topologischeMap);

			// Strecken die die gleichen zwei Knoten verbinden gehoeren zu einer hier sogenannten Aequivalenzklasse
			// Diese Strecken werden zuerst gesammelt und anschließend pro Aequivalenzklasse alle Strecken bis auf eine geloescht
			Map<Aequivalenzklasse, List<StreckeVonKanten>> aequivalenzMap = new HashMap<>();
			teilstrecken.forEach(strecke -> {
				Aequivalenzklasse curr = strecke.getAequivalenzklasse();
				aequivalenzMap.merge(curr, new ArrayList<>(List.of(strecke)), (existingList, newList) -> {
					existingList.addAll(newList);
					return existingList;
				});
			});
			for (List<StreckeVonKanten> strecken : aequivalenzMap.values()) {
				if (strecken.size() > 1) {
					strecken.stream().skip(1).forEach(teilstrecken::remove);
					teilstreckenVeraendert = true;
				}
			}
			topologischeMap = Topologie.erstelleTopologieMapAusStrecken(teilstrecken);
			Topologie.updateStreckenEndenStatus(teilstrecken, topologischeMap);

			// Achtung, nach dem aussortieren aequivalenter Kanten kann es wieder Knoten vom Grad 2 geben
			// -> diese Strecken zusammenfuegen und nochmal nach aequivalenten Strecken suchen
			teilstreckenVeraendert =
				StreckeVonKanten.mergeTeilstreckenAnKnotenMitGrad2(teilstrecken, topologischeMap)
					|| teilstreckenVeraendert;
		}
		topologischeMap = Topologie.erstelleTopologieMapAusStrecken(teilstrecken);
		Topologie.updateStreckenEndenStatus(teilstrecken, topologischeMap);

		// ----------------------------------- Ueber Knoten der Map Iterieren ------------------------------------------
		// und alle daran angrenzenden Strecken zusammenfuegen
		for (Map.Entry<Knoten, List<StreckeVonKanten>> entry : topologischeMap.entrySet()) {
			Knoten knoten = entry.getKey();
			List<StreckeVonKanten> streckenAnAktuellemKnoten = entry.getValue();

			int initialKnotenGrad = streckenAnAktuellemKnoten.size();

			if (initialKnotenGrad <= 1) {
				// Grad=1: Dieser Knoten ist ein Endpunkt, hier muss nichts gemerged werden
				// Oder Grad=0: Dieser Knoten besitzt keine Strecken mehr
				// (Das kann dadurch passieren, dass dieser Knoten mal ein Ende eines Wurmfortsatzes gewesen ist und
				// dieser Wurmfortsatz zu einem Loop konvertiert wurde)
				continue;
			}
			if (teilstrecken.size() == 1) {
				// Wenn schon alle Strecken zusammengesetzt wurden, dann muss an diesem Knoten auch nichts mehr gemacht werden
				// Das kann passieren, wenn die gesamtroute ein Kreis ist.
				// (ein Linestring muss aber einen Anfang und ein Ende haben)
				continue;
			}

			// ------------------------------ Ein und ausgehende Strecken bestimmen ------------------------------------
			Pair<Optional<StreckeVonKanten>, Optional<StreckeVonKanten>> einUndAusgehendeStrecke = findeEinUndAusgehendeStrecken(
				streckenAnAktuellemKnoten);

			if (einUndAusgehendeStrecke.getFirst().isEmpty()) {
				// An dem Knoten lassen sich keine ein und Ausgehenden Strecken festlegen
				// (z.B. befindet sich hier nur ein Loop)
				// -> Diesen Knoten ueberspringen, die Teilstrecke also nicht zu dem rest mergen
				// und spaeter dann die laengste Teilstrecke nehmen
				continue;
			}
			StreckeVonKanten eingehendeStrecke = einUndAusgehendeStrecke.getFirst().get();

			// Die ausgehende Strecke hingegen kann fehlen, wenn z.B. die Strecke auf einen Loop endet
			Optional<StreckeVonKanten> ausgehendeStreckeOpt = einUndAusgehendeStrecke.getSecond();

			// wird hiermit gleichzeitig aus Value Liste der Topologischen Map vom aktuellen knoten Key entfernt
			streckenAnAktuellemKnoten.remove(eingehendeStrecke);
			ausgehendeStreckeOpt.ifPresent(streckenAnAktuellemKnoten::remove);

			// -------------------------------- Wurmfortsaetze in loops umwandeln --------------------------------------
			// Die Aktion geschieht auf der Liste streckenAnAktuellemKnoten
			// und es wird auch die topologischeMap darauf angepasst
			convertWurmfortsaetzeToLoops(streckenAnAktuellemKnoten, topologischeMap);

			// ------------------------------- Loops an eingehendeStrecke ranmergen ------------------------------------
			// (Dabei Loops nur einmal berücksichtigen, obwohl sie mit ihrem Anfang und ihrem Ende an dem Knoten haengen)
			// Das muss hier nochmal aufgesammelt werden, weil zuvor Wurmfortsaetze in loops konvertiert wurden
			Set<StreckeVonKanten> loopsAnKnoten = streckenAnAktuellemKnoten.stream()
				.filter(StreckeVonKanten::isLoop)
				.collect(Collectors.toSet());

			for (StreckeVonKanten loop : loopsAnKnoten) {
				try {
					eingehendeStrecke.merge(loop);
					teilstrecken.remove(loop);

					// Der Loop ist doppelt in der Liste drin (weil er hier anfaengt und aufhoert)
					topologischeMap.get(knoten).remove(loop);
					topologischeMap.get(knoten).remove(loop);
				} catch (RequireViolation rv) {
					log.warn("Ein Loop konnte nicht zu der Gesamtstrecke hinzugefügt werden.");
				}
			}

			// ------------------------ ausgehendeStrecke an eingehendeStrecke ranmergen -------------------------------

			// Es kann sein, dass es an diesem Knoten keine ausgehende Strecke gibt, wenn z.B. eine Strecke in einen
			// Loop endet.
			if (ausgehendeStreckeOpt.isPresent()) {
				StreckeVonKanten.mergeZweiStreckenAndUpdateMap(eingehendeStrecke, ausgehendeStreckeOpt.get(),
					knoten, teilstrecken, topologischeMap);
			}
		}
	}

	/*
	 * Ein- und ausgehende Strecken an einem Knoten duerfen keine Loops sein und keine Kehrtwenden,
	 * ausser sie sind Anfang bzw. Ende der Gesamtstrecke.
	 */
	private static Pair<Optional<StreckeVonKanten>, Optional<StreckeVonKanten>> findeEinUndAusgehendeStrecken(
		List<StreckeVonKanten> streckenAnAktuellemKnoten) {
		// Loops können nicht ein bzw. ausgehende Strecken sein.
		List<StreckeVonKanten> streckenAnAktuellemKnotenOhneLoops = streckenAnAktuellemKnoten.stream()
			.filter(streckeVonKanten -> !streckeVonKanten.isLoop())
			.collect(Collectors.toList());
		if (streckenAnAktuellemKnotenOhneLoops.isEmpty()) {
			log.warn("Es muss am Knoten Strecken geben, die keine Loops sind");
			return Pair.of(Optional.empty(), Optional.empty());
		} else if (streckenAnAktuellemKnotenOhneLoops.size() == 1) {
			return Pair.of(Optional.of(streckenAnAktuellemKnotenOhneLoops.get(0)), Optional.empty());
		}
		// Ab hier gibt es nun mindestens zwei Strecken, die sich als eingehende bzw. ausgehende Strecke eignen

		List<StreckeVonKanten> streckenAnAktuellemKnotenOhneEndpunkte = streckenAnAktuellemKnotenOhneLoops.stream()
			.filter(streckeVonKanten -> !streckeVonKanten.isVonKnotenEndpunkt()
				&& !streckeVonKanten.isNachKnotenEndpunkt())
			.collect(Collectors.toList());

		List<StreckeVonKanten> wurmfortsaetze = streckenAnAktuellemKnotenOhneLoops.stream()
			.filter(streckeVonKanten -> streckeVonKanten.isVonKnotenEndpunkt()
				|| streckeVonKanten.isNachKnotenEndpunkt())
			.sorted(Comparator.comparingDouble((StreckeVonKanten s) -> s.getStrecke().getLength()).reversed())
			.collect(Collectors.toList());

		StreckeVonKanten eingehendeStrecke;
		StreckeVonKanten ausgehendeStrecke;
		if (streckenAnAktuellemKnotenOhneEndpunkte.size() == 2) {
			// Von diesem Knoten gehen genau zwei strecken ohne endpunkte ab -> eindeutig: Eine eingehend, eine ausgehend
			eingehendeStrecke = streckenAnAktuellemKnotenOhneEndpunkte.get(0);
			ausgehendeStrecke = streckenAnAktuellemKnotenOhneEndpunkte.get(1);
		} else if (streckenAnAktuellemKnotenOhneEndpunkte.size() == 1) {
			// Von diesem Knoten geht eine Strecke ohne Endpukt ab
			eingehendeStrecke = streckenAnAktuellemKnotenOhneEndpunkte.get(0);
			// Die Wurmfortsatzliste ist nach Laenge absteigend sortiert -> der laengste Wurmfortsatz ist wahrscheinlich
			// kein unerwuenschter Wurmfortsatz, sonder der Anfang/ Ende der Gesamtstrecke -> als ausgehende Strecke verwenden
			ausgehendeStrecke = wurmfortsaetze.get(0);
		} else if (streckenAnAktuellemKnotenOhneEndpunkte.size() == 0) {
			// Von diesem Knoten geht keine Strecke ohne Endpunkt ab (nur wurmfortsaetze oder valide Strecken anfänge/ Enden)
			eingehendeStrecke = wurmfortsaetze.get(0);
			ausgehendeStrecke = wurmfortsaetze.get(1);
		} else {
			// Von diesem Knoten gehen mehr als 2 Strecken ohne endpunkte ab -> unklar was zu tun ist.
			log.warn(
				"Strecken können nicht eindeutig zusammengemerged werden, weil es mehr als 2 Strecken am Knoten ohne Endpunkte gibt");
			return Pair.of(Optional.empty(), Optional.empty());
		}
		return Pair.of(Optional.of(eingehendeStrecke), Optional.of(ausgehendeStrecke));
	}

	/*
	 * Wurmfortsaetze werden zu einem Loop konvertiert, indem der Wurmfortsatz mit einer reversed Copy von sich selber
	 * gemerged wird.
	 * Dabei arbeitet die Methode auf der Liste streckenAnAktuellemKnoten und passt dabei auch die
	 * topologischeMap an (indirekt topologischeMap.get(aktuellerKnoten) ueber veraenderung von streckenAnAktuellemKnoten)
	 * und explizit an den Endknoten der Wurmfortsaetze.
	 * Wurmfortsaetze sind alle von dem aktuellen Knoten abgehenden Strecken mit einem Endpunkt.
	 */
	private static void convertWurmfortsaetzeToLoops(List<StreckeVonKanten> streckenAnAktuellemKnoten,
		Map<Knoten, List<StreckeVonKanten>> topologischeMap) {

		List<StreckeVonKanten> neueStreckenAmAktuellenKnoten = new ArrayList<>();
		streckenAnAktuellemKnoten.stream()
			.filter(streckeVonKanten -> streckeVonKanten.isVonKnotenEndpunkt()
				|| streckeVonKanten.isNachKnotenEndpunkt())
			.forEach(wurmfortsatz -> {
				Knoten endeDesWurmfortsatzes = wurmfortsatz.isVonKnotenEndpunkt() ?
					wurmfortsatz.getVonKnoten() :
					wurmfortsatz.getNachKnoten();

				// wenn der Wurmfortsatz zu einem Loop werden soll hat er keine Endstuecke mehr
				wurmfortsatz.setVonKnotenEndpunkt(false);
				wurmfortsatz.setNachKnotenEndpunkt(false);

				// merge reversed wurmfortsatz into wurmfortsatz to get a loop
				wurmfortsatz.merge(wurmfortsatz.reverse(), endeDesWurmfortsatzes);

				// Der Wurmfortsatz ist jetzt ein Loop und endet deshalb nicht mehr auf seinem ehemaligen endKnoten
				topologischeMap.get(endeDesWurmfortsatzes).remove(wurmfortsatz);
				// Loops kommen dafür doppelt in der Liste des aktuellenKnotens vor
				neueStreckenAmAktuellenKnoten.add(wurmfortsatz);
			});
		// update topologische Map
		streckenAnAktuellemKnoten.addAll(neueStreckenAmAktuellenKnoten);
	}

	private static void sucheBisEndpunkt(
		Map<Knoten, List<Kante>> knotenListMap,
		List<Kante> abzuarbeitendeKanten, Set<Knoten> endpunkteVonStrecken,
		StreckeVonKanten streckeVonKanten, boolean rueckwaerts) {
		Optional<Kante> next = getNextKanteInRichtung(knotenListMap, streckeVonKanten, rueckwaerts);
		while (next.isPresent() && abzuarbeitendeKanten.contains(next.get())) {
			Kante nextKante = next.get();
			streckeVonKanten.addKante(nextKante,
				endpunkteVonStrecken.contains(nextKante.getVonKnoten()) || endpunkteVonStrecken
					.contains(nextKante.getNachKnoten()));
			abzuarbeitendeKanten.remove(nextKante);
			next = getNextKanteInRichtung(knotenListMap, streckeVonKanten, rueckwaerts);
		}
	}

	private static Optional<Kante> getNextKanteInRichtung(Map<Knoten, List<Kante>> topologischeMap,
		StreckeVonKanten streckeVonKanten,
		boolean rueckwaerts) {
		if (rueckwaerts && !streckeVonKanten.isVonKnotenEndpunkt()) {
			List<Kante> naechsteKanten = topologischeMap.get(streckeVonKanten.getVonKnoten());

			naechsteKanten.remove(streckeVonKanten.getKanten().get(0));
			if (naechsteKanten.isEmpty()) {
				throw new RuntimeException(
					"getNextKanteInRichtung - Das hier kann nicht sein - naechsteKanten.isEmpty");
			}
			return Optional.of(naechsteKanten.get(0));
		} else if (!rueckwaerts && !streckeVonKanten.isNachKnotenEndpunkt()) {
			List<Kante> naechsteKanten = topologischeMap.get(streckeVonKanten.getNachKnoten());
			naechsteKanten.remove(streckeVonKanten.getKanten().get(streckeVonKanten.getKanten().size() - 1));
			if (naechsteKanten.isEmpty()) {
				throw new RuntimeException(
					"getNextKanteInRichtung - Das hier kann nicht sein - naechsteKanten.isEmpty");
			}
			return Optional.of(naechsteKanten.get(0));
		} else {
			return Optional.empty();
		}
	}
}
