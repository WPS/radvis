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

package de.wps.radvis.backend.netz.domain.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LinearLocation;
import org.locationtech.jts.linearref.LocationIndexedLine;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.entity.StreckenEinerPartition;
import lombok.NonNull;

public abstract class StreckenViewAbstractService<StreckenTyp extends StreckeVonKanten> {

	/**
	 * Diese Methode existiert, um folgende Problemstelle anzugehen: Wenn wir partitioniert Strecken von Kreuzungspunkt
	 * zu Kreuzungspunkt erstellen, so liegen die Endpunkte (Kreuzung/ Sackgasse) nicht zwangsläufig innerhalb einer
	 * Partition. Das bedeutet, wir haben unvollständige Strecken. Den "Rest" dieser Strecke liegt dann ebenfalls als
	 * unvollständige Strecke in einer anderen Partition vor. Deshalb schauen wir nach der Generierung von Strecken von jeder Partition,
	 * ob wir von den neuen unvollständigen Strecken und den alten unvollständigen Strecken diese mergen können.
	 * Am Ende dieser Methode kann eine Strecke entweder vollständig sein, und ist bereit abgearbeitet zu werden,
	 * oder sie kann weiterhin unvollständig sein, und muss auf weitere Partitionen warten.
	 * Funktionsweise des Algorithmus: Unvollständige Strecken sind entweder an einem oder an beiden Enden unvollständig.
	 * Für jeden Von- und Nachknoten einer Strecke, der nicht schon ein Endpunkt ist, suchen wir eine weitere Strecke,
	 * mit der wir diese verbinden können. Zwei Strecken können genau dann verbunden werden, wenn sie sich einen Knoten
	 * ihrer Von- und Nachknoten teilen (die kein Endknoten sind). Dadurch dass wir von Kreuzungspunkt zu Kreuzungspunkt
	 * matchen, haben wir weiterhin die Eigenschaft, dass sämtliche dieser nicht-Endknoten Grad 1 oder 2 haben.
	 * Wir sammeln also für jeden nicht-Endknoten die Anzahl anliegenden Strecken in einer Liste.
	 * Anschließend iterieren wir über alle Einträge der Map. Innerhalb des loops lassen wir das Mapping unberührt,
	 * updaten aber dynamisch die Listen, wenn wir zwei Strecken mergen. Das hat den Effekt, dass ein einziger Durchgang
	 * über alle Strecken ausreichend ist, um alle Strecken zu mergen, die mergbar sind. Funktionieren tut das, weil
	 * über alle Verbindungspunkte zwischen Strecken iteriert wird, und es egal ist, in welcher Reihenfolge wir diese
	 * erreichen. Beispiel: Wenn wir Strecken A -> B -> C haben, mergen wir entweder erst AB zusammen und dann AB und C,
	 * oder BC und A und BC. Das passiert dadurch, dass die "Verbindungen" (also die topologische Map) dynamisch
	 * geupdatet wird.
	 *
	 * @param unvollstaendig
	 *     Strecken, die noch nicht von Endpunkt zu Endpunkt gehen
	 */
	public StreckenEinerPartition<StreckenTyp> mergeUnvollstaendigeStrecken(List<StreckenTyp> unvollstaendig) {
		Map<Knoten, List<StreckenTyp>> topologischeMap = new HashMap<>();

		StreckenEinerPartition<StreckenTyp> result = new StreckenEinerPartition<StreckenTyp>();

		unvollstaendig.forEach(strecke -> {
			if (!strecke.isVonKnotenEndpunkt()) {
				topologischeMap
					.merge(strecke.getVonKnoten(), new ArrayList<>(List.of(strecke)), (existingList, newList) -> {
						existingList.addAll(newList);
						return existingList;
					});
			}
			if (!strecke.isNachKnotenEndpunkt()) {
				topologischeMap
					.merge(strecke.getNachKnoten(), new ArrayList<>(List.of(strecke)), (existingList, newList) -> {
						existingList.addAll(newList);
						return existingList;
					});
			}
		});

		topologischeMap.forEach(((knoten, strecken) -> {
			if (strecken.size() != 2) {
				return;
			}

			var merged = strecken.get(0);
			var toBeRemoved = strecken.get(1);
			strecken.remove(merged);
			strecken.remove(toBeRemoved);

			merged.merge(toBeRemoved);

			if (!merged.isVonKnotenEndpunkt()) {
				List<StreckenTyp> vonKnotenStrecken = topologischeMap.get(merged.getVonKnoten());
				vonKnotenStrecken.remove(toBeRemoved);
				if (!vonKnotenStrecken.contains(merged)) {
					vonKnotenStrecken.add(merged);
				}
			}
			if (!merged.isNachKnotenEndpunkt()) {
				List<StreckenTyp> nachKnotenStrecken = topologischeMap.get(merged.getNachKnoten());
				nachKnotenStrecken.remove(toBeRemoved);
				if (!nachKnotenStrecken.contains(merged)) {
					nachKnotenStrecken.add(merged);
				}
			}
			if (merged.abgeschlossen()) {
				result.vollstaendig.add(merged);
			}
		}));

		result.unvollstaendig.addAll(topologischeMap.values().stream().flatMap(List::stream).distinct()
			.collect(Collectors.toList()));
		return result;
	}

	/**
	 * @param kantenAusGroesseremAusschnitt
	 *     Wir brauchen die Kanten aus einem größeren Ausschnitt, um den tatsächlichen
	 *     Grad aller Knoten von Kanten, die die Partition schneiden, zu ermitteln.
	 * @param ausschnittAusDemStreckenErstelltWerdenSollen
	 *     Da wir Strecken aber nur aus Kanten erstellen wollen, die die
	 *     Partition schneiden, brauchen wir diese als Filterkriterium.
	 * @param bereitsEingeordnet
	 *     Jede Kante soll nur einmal besucht werden. Deshalb brauchen wir das Set, was die IDs
	 *     von bereits besuchten Kanten hält, um zu vermeiden, dass wir Kanten, die mehrere
	 *     Partitionen schneiden, mehrfach zu besuchen (und in Strecken zu packen).
	 * @return Vollständige und nicht Vollständige Strecken. Vollständig bedeutet von Endpunkt zu Endpunkt.
	 */
	public StreckenEinerPartition<StreckenTyp> createStreckenEinerPartition(List<Kante> kantenAusGroesseremAusschnitt,
		Envelope ausschnittAusDemStreckenErstelltWerdenSollen, Set<Long> bereitsEingeordnet) {
		Map<Knoten, Integer> knotenAufGlobalemGrad = new HashMap<>();
		Map<Knoten, List<Kante>> topologieInnerhalbVonAusschnitt = new HashMap<>();

		kantenAusGroesseremAusschnitt
			.forEach(kante -> {
				Knoten von = kante.getVonKnoten();
				Knoten nach = kante.getNachKnoten();

				knotenAufGlobalemGrad.merge(von, 1, Integer::sum);
				knotenAufGlobalemGrad.merge(nach, 1, Integer::sum);

				// topologische Map nur mit Kanten füllen, die in dem Envelope liegen und somit
				// die wir in dem Suchprozess abarbeiten wollen
				if (kante.getGeometry().getEnvelopeInternal()
					.intersects(ausschnittAusDemStreckenErstelltWerdenSollen) && !bereitsEingeordnet
						.contains(kante.getId())) {
					topologieInnerhalbVonAusschnitt
						.merge(von, new ArrayList<>(List.of(kante)), (existingList, newList) -> {
							existingList.addAll(newList);
							return existingList;
						});
					topologieInnerhalbVonAusschnitt
						.merge(nach, new ArrayList<>(List.of(kante)), (existingList, newList) -> {
							existingList.addAll(newList);
							return existingList;
						});
				}
			});

		Iterator<Kante> kantenNochNichtAbgearbeitet = kantenAusGroesseremAusschnitt.stream().filter(
			kante -> kante.getGeometry().getEnvelopeInternal().intersects(ausschnittAusDemStreckenErstelltWerdenSollen))
			.filter(kante -> !bereitsEingeordnet.contains(kante.getId()))
			.iterator();

		Set<Knoten> endpunkteVonStrecken = knotenAufGlobalemGrad.entrySet().stream()
			.filter(entry -> entry.getValue() != 2)
			.map(Map.Entry::getKey)
			.collect(Collectors.toCollection(HashSet::new));

		StreckenEinerPartition<StreckenTyp> streckenDerPartition = new StreckenEinerPartition<StreckenTyp>();

		while (kantenNochNichtAbgearbeitet.hasNext()) {
			Kante startKante = kantenNochNichtAbgearbeitet.next();
			bereitsEingeordnet.add(startKante.getId());

			final StreckenTyp strecke = this.createStreckeTyp(startKante, endpunkteVonStrecken
				.contains(startKante.getVonKnoten()), endpunkteVonStrecken.contains(startKante.getNachKnoten()));

			sucheBisEndpunktOderPartitionsende(topologieInnerhalbVonAusschnitt, bereitsEingeordnet,
				endpunkteVonStrecken, strecke,
				false);
			sucheBisEndpunktOderPartitionsende(topologieInnerhalbVonAusschnitt, bereitsEingeordnet,
				endpunkteVonStrecken, strecke,
				true);

			if (strecke.isVonKnotenEndpunkt() && strecke.isNachKnotenEndpunkt()) {
				streckenDerPartition.vollstaendig.add(strecke);
			} else {
				streckenDerPartition.unvollstaendig.add(strecke);
			}
		}
		return streckenDerPartition;
	}

	public List<LineString> getSublinestringsDurchProjektionVonKnoten(LineString lineString, List<Kante> kanten,
		Knoten start) {
		LocationIndexedLine locationIndexedGrundnetzGeometrie = new LocationIndexedLine(lineString);

		List<Knoten> knotenInReihenfolge = new ArrayList<>();
		knotenInReihenfolge.add(start);
		for (Kante kante : kanten) {
			Knoten letzterKnoten = knotenInReihenfolge.get(knotenInReihenfolge.size() - 1);
			if (kante.getVonKnoten().equals(letzterKnoten)) {
				knotenInReihenfolge.add(kante.getNachKnoten());
			} else {
				knotenInReihenfolge.add(kante.getVonKnoten());
			}
		}

		List<LineString> matches = new ArrayList<>();

		LinearLocation previous = locationIndexedGrundnetzGeometrie.project(start.getKoordinate());
		for (int i = 1; i < knotenInReihenfolge.size(); i++) {
			Knoten knoten = knotenInReihenfolge.get(i);
			LinearLocation current = locationIndexedGrundnetzGeometrie.project(knoten.getKoordinate());
			LineString projection = (LineString) locationIndexedGrundnetzGeometrie.extractLine(previous, current);
			projection.setSRID(KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
			if (kanten.get(i - 1).getNachKnoten().equals(knoten)) {
				matches.add(projection);
			} else {
				matches.add(projection.reverse());
			}
			previous = current;
		}

		return matches;
	}

	protected void sucheBisEndpunktOderPartitionsende(
		Map<Knoten, List<Kante>> topologischeMap,
		Set<Long> bereitsEingeordnet, Set<Knoten> endpunkteVonStrecken,
		StreckenTyp streckeVonKanten, boolean rueckwaerts) {
		Optional<Kante> next = getNextKanteInRichtung(topologischeMap, streckeVonKanten, rueckwaerts);
		while (next.isPresent() && !bereitsEingeordnet.contains(next.get().getId())) {
			Kante nextKante = next.get();
			streckeVonKanten.addKante(nextKante,
				endpunkteVonStrecken.contains(nextKante.getVonKnoten()) || endpunkteVonStrecken
					.contains(nextKante.getNachKnoten()));
			bereitsEingeordnet.add(nextKante.getId());
			next = getNextKanteInRichtung(topologischeMap, streckeVonKanten, rueckwaerts);
		}
	}

	protected Optional<Kante> getNextKanteInRichtung(Map<Knoten, List<Kante>> topologischeMap, StreckenTyp strecke,
		boolean rueckwaerts) {
		if (rueckwaerts && !strecke.isVonKnotenEndpunkt()) {
			List<Kante> naechsteKanten = topologischeMap.get(strecke.getVonKnoten());

			naechsteKanten.remove(strecke.getKanten().get(0));
			if (naechsteKanten.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(naechsteKanten.get(0));
		} else if (!rueckwaerts && !strecke.isNachKnotenEndpunkt()) {
			List<Kante> naechsteKanten = topologischeMap.get(strecke.getNachKnoten());
			naechsteKanten.remove(strecke.getKanten().get(strecke.getKanten().size() - 1));
			if (naechsteKanten.isEmpty()) {
				return Optional.empty();
			}
			return Optional.of(naechsteKanten.get(0));
		} else {
			return Optional.empty();
		}
	}

	protected abstract StreckenTyp createStreckeTyp(@NonNull Kante startKante, boolean vonKnotenEndpunkt,
		boolean nachKnotenEndpunkt);

}
