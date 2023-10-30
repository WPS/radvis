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

import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.valid4j.errors.RequireViolation;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.netz.domain.valueObject.Aequivalenzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StreckeVonKanten {
	private static final GeometryFactory GEOMETRY_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@Getter
	@NonNull
	private LineString strecke;

	@NonNull
	@Getter
	private List<Kante> kanten;

	@NonNull
	@Getter
	private Knoten vonKnoten;

	@NonNull
	@Getter
	private Knoten nachKnoten;

	@Getter
	@Setter
	private boolean vonKnotenEndpunkt;
	@Getter
	@Setter
	private boolean nachKnotenEndpunkt;

	public StreckeVonKanten(@NonNull Kante kante, boolean vonKnotenEndpunkt, boolean nachKnotenEndpunkt) {
		strecke = kante.getGeometry();
		kanten = new ArrayList<>();
		kanten.add(kante);
		vonKnoten = kante.getVonKnoten();
		nachKnoten = kante.getNachKnoten();

		this.vonKnotenEndpunkt = vonKnotenEndpunkt;
		this.nachKnotenEndpunkt = nachKnotenEndpunkt;
	}

	public StreckeVonKanten(@NonNull Kante kante, @NonNull LineString strecke, @NonNull Knoten von,
		@NonNull Knoten nach, boolean vonKnotenEndpunkt, boolean nachKnotenEndpunkt) {
		this.strecke = strecke;
		kanten = new ArrayList<>();
		kanten.add(kante);
		vonKnoten = von;
		nachKnoten = nach;

		this.vonKnotenEndpunkt = vonKnotenEndpunkt;
		this.nachKnotenEndpunkt = nachKnotenEndpunkt;
	}

	public StreckeVonKanten(@NonNull Kante kante) {
		strecke = kante.getGeometry();
		kanten = new ArrayList<>();
		kanten.add(kante);
		vonKnoten = kante.getVonKnoten();
		nachKnoten = kante.getNachKnoten();
		vonKnotenEndpunkt = false;
		nachKnotenEndpunkt = false;
	}

	public StreckeVonKanten(@NonNull List<Kante> kanten, @NonNull LineString strecke, @NonNull Knoten von,
		@NonNull Knoten nach, boolean vonKnotenEndpunkt, boolean nachKnotenEndpunkt) {
		this.strecke = strecke;
		this.kanten = kanten;
		vonKnoten = von;
		nachKnoten = nach;

		this.vonKnotenEndpunkt = vonKnotenEndpunkt;
		this.nachKnotenEndpunkt = nachKnotenEndpunkt;
	}

	public boolean abgeschlossen() {
		return vonKnotenEndpunkt && nachKnotenEndpunkt;
	}

	public boolean isLoop() {
		return vonKnoten.equals(nachKnoten);
	}

	/**
	 * Fügt eine Kante, die mit der Strecke topologisch zusammenhängt, an die Strecke an
	 * Doppelte Koordinaten sind kein Problem für den Graphhopper, da diese bei den Observations rausgefiltert werden
	 */
	public void addKante(Kante kante, boolean istNeuerKnotenEndpunkt) {
		add(kante, kante.getVonKnoten(), kante.getNachKnoten(), kante.getGeometry(), istNeuerKnotenEndpunkt);
	}

	public void add(Kante kante, Knoten neuVon, Knoten neuNach, LineString neuerLineString,
		boolean istNeuerKnotenEndpunkt) {
		// Hier stand mal dieses Require: require(!kanten.contains(kante));
		// Ich (Lasse) brauchte die merge-Methode die mit doppelten Kanten zurecht kommt und sehe keinen Grund,
		// warum das nicht moeglich sein sollte
		require(canAddZuStrecke(neuVon, neuNach),
			"Eine Strecke muss topologisch zusammenhängen");

		boolean vorneVorhaengen = vorneVorhaengen(neuVon, neuNach);

		if (vorneVorhaengen) {
			kanten.add(0, kante);
		} else {
			kanten.add(kante);
		}
		mergeTopologieUndGeometrie(neuVon, neuNach, neuerLineString, vorneVorhaengen);

		if (istNeuerKnotenEndpunkt) {
			vonKnotenEndpunkt = vonKnotenEndpunkt || vorneVorhaengen;
			nachKnotenEndpunkt = nachKnotenEndpunkt || !vorneVorhaengen;
		} else if (vonKnoten.equals(nachKnoten)) {
			vonKnotenEndpunkt = true;
			nachKnotenEndpunkt = true;
		}
	}

	public boolean canAddZuStrecke(Knoten neuVon, Knoten neuNach) {
		return (vonKnoten.equals(neuVon) || vonKnoten.equals(neuNach)
			&& !vonKnotenEndpunkt)
			|| (nachKnoten.equals(neuVon) || nachKnoten.equals(neuNach)
			&& !nachKnotenEndpunkt);
	}

	public void merge(StreckeVonKanten other) {
		require(other.getKanten().stream().noneMatch(kanten::contains));

		boolean vorneVorhaengen = vorneVorhaengen(other.vonKnoten, other.nachKnoten);

		merge(other, vorneVorhaengen ? this.vonKnoten : this.nachKnoten);
	}

	public void merge(StreckeVonKanten other, Knoten whereToConnect) {
		// Hier steht bei der anderen merge-Methode dieses Require:
		// require(other.getKanten().stream().noneMatch(kanten::contains));
		// Ich (Lasse) brauchte eine merge-Methode die mit doppelten Kanten zurecht kommt und sehe keinen Grund,
		// warum das nicht moeglich sein sollte
		require(canAddZuStrecke(other.vonKnoten, other.nachKnoten),
			"Eine Strecke muss topologisch zusammenhängen");

		LineString neueGeometry = other.strecke;

		Stream<Kante> otherStream;
		otherStream = getKantenInStreckenReihenfolge(other);

		boolean vorneVorhaengen = this.vonKnoten == whereToConnect;

		if (vorneVorhaengen) {
			kanten = Stream.concat(otherStream, this.kanten.stream()).collect(Collectors.toList());

			vonKnotenEndpunkt = hatUnterschiedlicheStationierungsrichtung(other.vonKnoten, other.nachKnoten) ?
				other.nachKnotenEndpunkt : other.vonKnotenEndpunkt;
		} else {
			kanten = Stream.concat(this.kanten.stream(), otherStream).collect(Collectors.toList());

			nachKnotenEndpunkt = hatUnterschiedlicheStationierungsrichtung(other.vonKnoten, other.nachKnoten) ?
				other.vonKnotenEndpunkt : other.nachKnotenEndpunkt;
		}

		mergeTopologieUndGeometrie(other.vonKnoten, other.nachKnoten, neueGeometry, vorneVorhaengen);
	}

	private void mergeTopologieUndGeometrie(Knoten vonKnotenNeuerGeometrie, Knoten nachKnotenNeuerGeometrie,
		LineString neueGeometry, boolean vorneRanhaengen) {
		Coordinate[] neueCoordinates = neueGeometry.getCoordinates();
		Coordinate[] streckenCoordinates = strecke.getCoordinates();

		Coordinate[] merged;
		if (!hatUnterschiedlicheStationierungsrichtung(vonKnotenNeuerGeometrie, nachKnotenNeuerGeometrie))
			if (vorneRanhaengen) { // vor vonKnoten hängen
				merged = concatArrays(neueCoordinates, streckenCoordinates);
				this.vonKnoten = vonKnotenNeuerGeometrie;
			} else { // an nachKnoten anhängen
				merged = concatArrays(streckenCoordinates, neueCoordinates);
				this.nachKnoten = nachKnotenNeuerGeometrie;
			}
		else { // Koordinaten müssen umgedreht werden
			Coordinate[] reversed = neueGeometry.reverse().getCoordinates();
			if (vorneRanhaengen) {
				merged = concatArrays(reversed, streckenCoordinates);
				this.vonKnoten = nachKnotenNeuerGeometrie;
			} else {
				merged = concatArrays(streckenCoordinates, reversed);
				this.nachKnoten = vonKnotenNeuerGeometrie;
			}
		}

		// Aufeinanderfolgende doppelte Koordinaten rausfiltern
		AtomicReference<Coordinate> previous = new AtomicReference<>(null);
		List<Coordinate> mergedOhneAufeinanderfolgendeDoppelte = Arrays.stream(merged)
			.filter(coordinate -> !coordinate.equals(previous.getAndSet(coordinate)))
			.collect(Collectors.toList());

		strecke = GEOMETRY_FACTORY.createLineString(mergedOhneAufeinanderfolgendeDoppelte.toArray(Coordinate[]::new));
	}

	private Stream<Kante> getKantenInStreckenReihenfolge(StreckeVonKanten other) {
		Stream<Kante> otherStream;
		boolean kantenReihenfolgeReversed = hatUnterschiedlicheStationierungsrichtung(other.vonKnoten,
			other.nachKnoten);

		if (!kantenReihenfolgeReversed) {
			otherStream = other.kanten.stream();
		} else {
			List<Kante> reversed = other.kanten;
			Collections.reverse(reversed);
			otherStream = reversed.stream();
		}
		return otherStream;
	}

	private boolean hatUnterschiedlicheStationierungsrichtung(Knoten otherVon, Knoten otherNach) {
		return this.vonKnoten.equals(otherVon) || this.nachKnoten.equals(otherNach);
	}

	private boolean vorneVorhaengen(Knoten von, Knoten nach) {
		return (this.vonKnoten.equals(von) || this.vonKnoten.equals(nach)) && !vonKnotenEndpunkt;
	}

	static <T> T[] concatArrays(T[] array1, T[] array2) {
		T[] result = Arrays.copyOf(array1, array1.length + array2.length);
		System.arraycopy(array2, 0, result, array1.length, array2.length);
		return result;
	}

	public void setzeVonKnotenAlsEndknoten() {
		vonKnotenEndpunkt = true;
	}

	public void setzeNachKnotenAlsEndknoten() {
		nachKnotenEndpunkt = true;
	}

	public void updateKanteInStrecke(Kante neueKante) {
		require(this.kanten.contains(neueKante));
		final var index = this.kanten.indexOf(neueKante);
		this.kanten.remove(index);
		this.kanten.add(index, neueKante);
		this.updateStreckenGeometrie();
	}

	/**
	 * Modifiziert bestehende Strecke und returned die von der Strecke abgespaltenen Kanten
	 */
	public List<Kante> splitAt(Kante kante) {
		final int index = this.kanten.indexOf(kante);
		if (index == 0 || index == this.kanten.size() - 1) {
			this.kanten.remove(index);
			this.updateStreckenGeometrie();
			return Collections.emptyList();
		}

		final List<Kante> splitKanten = IntStream.range(index + 1, this.kanten.size())
			.mapToObj(i -> this.kanten.get(i))
			.collect(Collectors.toList());
		this.kanten.remove(index);
		this.kanten.removeAll(splitKanten);
		this.updateStreckenGeometrie();
		return splitKanten;
	}

	private void updateStreckenGeometrie() {
		final var resultCoordinates = this.kanten.stream().map(kante -> kante.getGeometry().getCoordinates()).reduce(
			StreckeVonKanten::concatArrays);
		resultCoordinates.ifPresent(coordinates -> this.strecke = GEOMETRY_FACTORY.createLineString(coordinates));
	}

	public StreckeVonKanten reverse() {
		StreckeVonKanten reversed = new StreckeVonKanten(
			new ArrayList<>(this.kanten),
			this.strecke.reverse(),
			this.getNachKnoten(),
			this.getVonKnoten(),
			this.isNachKnotenEndpunkt(),
			this.isVonKnotenEndpunkt()
		);
		Collections.reverse(reversed.kanten);
		return reversed;
	}

	public Aequivalenzklasse getAequivalenzklasse() {
		return Aequivalenzklasse.of(vonKnoten.getKoordinate(), nachKnoten.getKoordinate());
	}

	public static boolean mergeTeilstreckenAnKnotenMitGrad2(List<StreckeVonKanten> teilstrecken,
		Map<Knoten, List<StreckeVonKanten>> topologischeMap) {
		boolean teilstreckenVeraendert = false;

		for (Map.Entry<Knoten, List<StreckeVonKanten>> entry : topologischeMap.entrySet()) {
			Knoten knoten = entry.getKey();
			List<StreckeVonKanten> streckenAnAktuellemKnoten = entry.getValue();

			if (streckenAnAktuellemKnoten.size() != 2) {
				// Knoten vom Grad != 2 interessieren uns an dieser Stelle nicht, die kommen spaeter
				continue;
			}

			StreckeVonKanten eingehendeStrecke = streckenAnAktuellemKnoten.get(0);
			StreckeVonKanten ausgehendeStrecke = streckenAnAktuellemKnoten.get(1);

			if (eingehendeStrecke.equals(ausgehendeStrecke)) {
				log.warn(
					"Die eingehende ist auch die ausgehende Strecke -> Es ist ein loop und kann nicht zusammengefügt werden.");
				continue;
			}

			try {
				mergeZweiStreckenAndUpdateMap(eingehendeStrecke, ausgehendeStrecke, knoten, teilstrecken,
					topologischeMap);

				teilstreckenVeraendert = true;
			} catch (RequireViolation rv) {
				log.warn("Zwei Strecken konnten nicht gemerged werden, da sie nicht topologisch zusammenhängen.");
			}
		}
		return teilstreckenVeraendert;
	}

	public static void mergeZweiStreckenAndUpdateMap(
		StreckeVonKanten eingehendeStrecke,
		StreckeVonKanten ausgehendeStrecke,
		Knoten connectingKnoten,
		List<StreckeVonKanten> teilstrecken,
		Map<Knoten, List<StreckeVonKanten>> topologischeMap) {
		eingehendeStrecke.merge(ausgehendeStrecke);
		teilstrecken.remove(ausgehendeStrecke);

		// topologischeMap aktualisieren
		// ausgehendeStrecke aus map entfernen
		Knoten andererKnotenDerAusgehendenStrecke =
			connectingKnoten == ausgehendeStrecke.getVonKnoten() ?
				ausgehendeStrecke.getNachKnoten() :
				ausgehendeStrecke.getVonKnoten();
		topologischeMap.get(andererKnotenDerAusgehendenStrecke).remove(ausgehendeStrecke);
		// eingehendeStrecke zu dem anderen Knoten der ausgehendeStrecke hinzufuegen
		topologischeMap.get(andererKnotenDerAusgehendenStrecke).add(eingehendeStrecke);
	}

	public boolean passtAnStreckeRan(Kante kante) {
		return Netzklasse.isRadNETZ(kante.getKantenAttributGruppe().getNetzklassen());
	}
}

