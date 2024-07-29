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

package de.wps.radvis.backend.integration.grundnetzReimport.domain;

import static org.valid4j.Assertive.ensure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.grundnetz.domain.DLMAttributMapper;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.DLMReimportJobStatistik;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.SplitUpdate;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.TopologischesUpdate;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpdateKantenService {

	private final DLMAttributMapper dlmAttributMapper;
	private final KantenRepository kantenRepository;

	public UpdateKantenService(DLMAttributMapper dlmAttributMapper, KantenRepository kantenRepository) {
		this.dlmAttributMapper = dlmAttributMapper;
		this.kantenRepository = kantenRepository;
	}

	public Optional<TopologischesUpdate> updateBestehendeDLMKante(ImportedFeature importedFeature, Kante kante,
		DLMReimportJobStatistik dlmReimportJobStatistik) {

		// update Attribute
		KantenAttribute alteAttribute = kante.getKantenAttributGruppe().getKantenAttribute();
		KantenAttribute neueAttribute = getUpdatedAttribute(importedFeature, alteAttribute);
		boolean wurdeStrassenNummerGeaendert = !alteAttribute.getStrassenNummer()
			.equals(neueAttribute.getStrassenNummer());
		boolean wurdeStrassenNameGeaendert = !alteAttribute.getStrassenName().equals(neueAttribute.getStrassenName());

		if (wurdeStrassenNummerGeaendert) {
			dlmReimportJobStatistik.strassenNummerAenderung++;
		}

		if (wurdeStrassenNameGeaendert) {
			dlmReimportJobStatistik.strassenNamenAenderung++;
		}

		if (wurdeStrassenNummerGeaendert || wurdeStrassenNameGeaendert) {
			kante.ueberschreibeKantenAttribute(neueAttribute);
		}

		if (importedFeature.getGeometrie().equals(kante.getGeometry())) {
			return Optional.empty();
		}
		dlmReimportJobStatistik.geometrieAenderungen++;

		LineString neueGeometrie = (LineString) importedFeature.getGeometrie();
		Knoten vonKnoten = kante.getVonKnoten();
		Knoten nachKnoten = kante.getNachKnoten();

		// keine Topologische Änderung
		if (neueGeometrie.getStartPoint().distance(vonKnoten.getPoint()) <= KnotenIndex.SNAPPING_DISTANCE
			&& neueGeometrie.getEndPoint().distance(nachKnoten.getPoint()) <= KnotenIndex.SNAPPING_DISTANCE) {
			kante.updateDLMGeometry(neueGeometrie);
		} else { // Topologische Änderung
			dlmReimportJobStatistik.geometrieAenderungenMitTopologischerKonsequenz++;
			return Optional
				.of(new TopologischesUpdate(kante, neueGeometrie.getStartPoint(), neueGeometrie.getEndPoint(),
					neueGeometrie));
		}
		return Optional.empty();
	}

	/**
	 * Annahmen der Methode: Ein Strecke von Kanten, die die gesplittete Kante ersetzt, geht vom alten Vonknoten der
	 * gesplitteten Kante bis alten Nachknoten der gesplitteten Kante. Splits, deren Strecke gleichzeitig auch noch
	 * andere Von- und Nachknoten bekommen, werden nicht als Splits erkannt. Weiterhin wird ein Split nur dann erkannt,
	 * wenn es eine Strecke aus der bestehenden DLM-Kante und neuen DLM-Kanten gibt. Zum Zeitpunkt dieser Methode
	 * sollten noch keine topologischen Updates bestehender DLM-Kanten ausgeführt wurden sein.
	 *
	 * @param topologischesUpdate
	 *     ein Geometrieänderung mit topologischer Konsequenz, die ein Split sein könnte
	 * @return Ein SplitUpdate, was die neuen Von
	 */
	public Optional<SplitUpdate> findSplitIfExists(TopologischesUpdate topologischesUpdate,
		Set<Kante> nichtReimportierteKanten,
		DLMReimportJobStatistik statistik) {
		Kante originalKante = topologischesUpdate.getKante();

		// Prüfen ob Von- und NachKnoten der Kante, die beim Split erhalten bleibt (ie. orginalKante), nach dem
		// Update noch im engeren Bereich der alten Geometrie liegen. Ansonsten early exit, da
		// die Kante "größer" geworden ist und somit kein Split vorliegt
		Geometry buffer = originalKante.getGeometry().buffer(2);
		if (!buffer.contains(topologischesUpdate.getNeuVon())
			|| !buffer.contains(topologischesUpdate.getNeuNach())) {
			statistik.topologischesUpdateLiegtAusserhalbVonSplitBuffer++;
			return Optional.empty();
		}

		// check if Weg existiert, der alte topologie wieder herstellt
		Envelope bereichDesSplits = originalKante.getGeometry().getEnvelopeInternal();
		bereichDesSplits.expandBy(2);

		// Komplett neue DLM-Kanten wurden zu diesem Zeitpunkt bereits erstellt und sind hier verfügbar
		Stream<Kante> kantenInBereichNachQuelle = kantenRepository
			.getKantenInBereichNachQuelle(bereichDesSplits, QuellSystem.DLM);

		// Wir bauen eine topologische Map auf, die jedem Knoten im Bereich
		// seine inzidenten Kanten zuordnet
		HashMap<Knoten, List<KantenViewMitPotentiellAbweichenderTopologie>> topologischeMapInBereich = new HashMap<>();
		// Um Knoten mit mittels unscharfer Coordinaten zu finden, nutzen wir einen KnotenIndex
		KnotenIndex knotenIndex = new KnotenIndex();
		AtomicInteger anzahlKanten = new AtomicInteger();
		anzahlKanten.set(1);
		// Zunächst fügen Wir alle Kanten, samt Knoten, in die Map und den Index ein,
		// außer der original Kante und denjenigen Kanten, die durch den aktuellen
		// Import gelöscht werden
		kantenInBereichNachQuelle
			.filter(k -> !k.equals(originalKante))
			.filter(k -> !nichtReimportierteKanten.contains(k))
			.map(KantenViewMitPotentiellAbweichenderTopologie::new)
			.forEach(k -> {
				insertInTopologieMap(topologischeMapInBereich, k);
				knotenIndex.fuegeEin(k.getVonKnoten());
				knotenIndex.fuegeEin(k.getNachKnoten());
				anzahlKanten.incrementAndGet();
			});

		// Prüfen, ob die originalKante nach dem Update immernoch
		// denselben Nach- bzw. VonKnoten haben wird
		boolean isVonKnotenNachUpdateIdentisch = topologischesUpdate.getNeueGeometry().getStartPoint().distance(
			originalKante.getVonKnoten().getPoint()) <= KnotenIndex.SNAPPING_DISTANCE;
		boolean isNachKnotenNachUpdateIdentisch = topologischesUpdate.getNeueGeometry().getEndPoint().distance(
			originalKante.getNachKnoten().getPoint()) <= KnotenIndex.SNAPPING_DISTANCE;

		// Neue (ggf. sind dies die alten) Knoten der originalKante bestimmen
		Optional<Knoten> neuerVonKnoten = isVonKnotenNachUpdateIdentisch ? Optional.of(originalKante.getVonKnoten())
			: knotenIndex.finde(topologischesUpdate.getNeueGeometry().getStartPoint());
		Optional<Knoten> neuerNachKnoten = isNachKnotenNachUpdateIdentisch ? Optional.of(originalKante.getNachKnoten())
			: knotenIndex.finde(topologischesUpdate.getNeueGeometry().getEndPoint());

		if (neuerVonKnoten.isEmpty() || neuerNachKnoten.isEmpty()) {
			// Lücke existiert und kann nicht aufgefüllt werden
			return Optional.empty();
		}

		// Jetzt fügen wir die originalKante in die Map ein (die Knoten befinden sich bereits im Index)
		// aber mit neuer Geometrie und den neuen Knoten
		KantenViewMitPotentiellAbweichenderTopologie zuUpdatendeKanteMitNeuerTopologie = new KantenViewMitPotentiellAbweichenderTopologie(
			originalKante, topologischesUpdate.getNeueGeometry(), neuerVonKnoten.get(), neuerNachKnoten.get());
		insertInTopologieMap(topologischeMapInBereich, zuUpdatendeKanteMitNeuerTopologie);

		Set<KantenViewMitPotentiellAbweichenderTopologie> visited = new HashSet<>();

		// Originalkante wird nicht in Topologie-Map inserted, sondern nur die topologisch geupdatete Version.
		// Deshalb kann es passieren, dass die Von- oder Nachknoten der Originalkante nicht in der Topologie-Map
		// existiert.
		// Wenn dem so ist, können wir kein Split finden.
		if (!topologischeMapInBereich.containsKey(originalKante.getVonKnoten())
			|| !topologischeMapInBereich.containsKey(originalKante.getNachKnoten())) {
			statistik.anzahlTopologischeUpdatesOhneValideKanteAnVonUndNachKnoten++;
			return Optional.empty();
		}

		// Versuche Split zu finden startend mit vonKnoten
		KantenViewMitPotentiellAbweichenderTopologie currentKante;
		try {
			currentKante = findBesteKanteFuerSplit(
				originalKante.getGeometry(),
				null,
				topologischeMapInBereich.get(originalKante.getVonKnoten()),
				buffer)
					.orElseThrow();
		} catch (NoSuchElementException exception) {
			return Optional.empty();
		}

		visited.add(currentKante);

		boolean isKanteReversed = !currentKante.getVonKnoten().equals(originalKante.getVonKnoten());

		StreckeVonKanten splitStrecke = new StreckeVonKanten(currentKante.getKante(),
			isKanteReversed ? currentKante.getNeueGeometry().reverse() : currentKante.getNeueGeometry(),
			originalKante.getVonKnoten(),
			isKanteReversed ? currentKante.getVonKnoten() : currentKante.getNachKnoten(),
			true,
			isKanteReversed ? currentKante.getVonKnoten().equals(originalKante.getNachKnoten())
				: currentKante.getNachKnoten().equals(originalKante.getNachKnoten()));

		while (!splitStrecke.abgeschlossen() && visited.size() < anzahlKanten.get()) {

			Optional<KantenViewMitPotentiellAbweichenderTopologie> potentialNext = findBesteKanteFuerSplit(
				originalKante.getGeometry(),
				currentKante,
				topologischeMapInBereich.get(splitStrecke.getNachKnoten()),
				buffer);

			if (potentialNext.isEmpty()) { // Streckenende ohne abgeschlossese Splitstrecke
				return Optional.empty();
			}

			currentKante = potentialNext.get();
			if (!visited.add(currentKante)) { // Loop detektiert
				statistik.anzahlLoopsWaehrendSplitSuche++;
				return Optional.empty();
			}

			splitStrecke.add(currentKante.getKante(), currentKante.getVonKnoten(), currentKante.getNachKnoten(),
				currentKante.getNeueGeometry(),
				currentKante.getNachKnoten().equals(originalKante.getNachKnoten())
					|| currentKante.getVonKnoten().equals(originalKante.getNachKnoten()));
		}

		if (splitStrecke.isLoop()) {
			// Loop zum Anfangsknoten wird eventuell nicht detektiert, da die Strecke
			// abgeschlossen sein kann bevor eine Kante zweimal besichtigt wird
			statistik.anzahlLoopsWaehrendSplitSuche++;
			return Optional.empty();
		}

		if (splitStrecke.getKanten().size() == 1) {
			// ein Split braucht per Definition mehr als eine Kante
			statistik.anzahlSplitsMitEinerKanteGefunden++;
			return Optional.empty();
		}

		ensure(splitStrecke.abgeschlossen());
		ensure((splitStrecke.getVonKnoten().equals(originalKante.getVonKnoten()) && splitStrecke.getNachKnoten()
			.equals(originalKante.getNachKnoten()))
			|| (splitStrecke.getVonKnoten().equals(originalKante.getNachKnoten()) && splitStrecke.getNachKnoten()
				.equals(originalKante.getVonKnoten())),
			"Endpunkte der SplitStrecke müssen den Endpunkten der OriginalKante entsprechen");
		ensure(buffer.contains(splitStrecke.getStrecke()));

		statistik.anzahlSplits++;
		statistik.anzahlKantenInSplits += splitStrecke.getKanten().size();

		return Optional.of(new SplitUpdate(zuUpdatendeKanteMitNeuerTopologie.getKante(),
			zuUpdatendeKanteMitNeuerTopologie.getNeueGeometry(), zuUpdatendeKanteMitNeuerTopologie.getVonKnoten(),
			zuUpdatendeKanteMitNeuerTopologie.getNachKnoten(), splitStrecke.getKanten()));
	}

	private void insertInTopologieMap(
		HashMap<Knoten, List<KantenViewMitPotentiellAbweichenderTopologie>> topologischeMapInBereich,
		KantenViewMitPotentiellAbweichenderTopologie zuUpdatendeKanteMitNeuerTopologie) {
		topologischeMapInBereich
			.merge(zuUpdatendeKanteMitNeuerTopologie.getVonKnoten(),
				new ArrayList<>(List.of(zuUpdatendeKanteMitNeuerTopologie)), (existingList, newList) -> {
					existingList.addAll(newList);
					return existingList;
				});
		topologischeMapInBereich
			.merge(zuUpdatendeKanteMitNeuerTopologie.getNachKnoten(),
				new ArrayList<>(List.of(zuUpdatendeKanteMitNeuerTopologie)), (existingList, newList) -> {
					existingList.addAll(newList);
					return existingList;
				});
	}

	private Optional<KantenViewMitPotentiellAbweichenderTopologie> findBesteKanteFuerSplit(
		LineString ursprungsGeometrie,
		KantenViewMitPotentiellAbweichenderTopologie previous,
		List<KantenViewMitPotentiellAbweichenderTopologie> kandidaten,
		Geometry buffer) {
		Stream<KantenViewMitPotentiellAbweichenderTopologie> kandidatenStream = kandidaten.stream()
			.filter(k -> buffer.contains(k.getNeueGeometry()));

		if (previous != null) {
			kandidatenStream = kandidatenStream.filter(k -> !k.equals(previous));
		}

		// Bei mehreren validen kandidaten, nehme denen, der am nächsten an der Ursprungsgeometrie dran ist
		return kandidatenStream.reduce(
			(k1, k2) -> (k1.getVonKnoten().getPoint().distance(ursprungsGeometrie)
				+ k1.getNachKnoten().getPoint().distance(ursprungsGeometrie)) <= (k2.getVonKnoten().getPoint()
					.distance(ursprungsGeometrie)
					+ k2.getNachKnoten().getPoint().distance(ursprungsGeometrie))
						? k1
						: k2);
	}

	private KantenAttribute getUpdatedAttribute(ImportedFeature importedFeature, KantenAttribute bestehendeAttribute) {
		KantenAttribute reimportedKantenattribute = dlmAttributMapper.mapKantenAttributGruppe(importedFeature)
			.getKantenAttribute();

		return bestehendeAttribute.getBuilderMitGleichenAttributen()
			.strassenName(reimportedKantenattribute.getStrassenName().orElse(null))
			.strassenNummer(reimportedKantenattribute.getStrassenNummer().orElse(null))
			.build();
	}

	@AllArgsConstructor
	@Getter
	private static class KantenViewMitPotentiellAbweichenderTopologie {
		private final Kante kante;
		private final LineString neueGeometry;
		private final Knoten vonKnoten;
		private final Knoten nachKnoten;

		public KantenViewMitPotentiellAbweichenderTopologie(Kante kante) {
			this.kante = kante;
			neueGeometry = kante.getGeometry();
			vonKnoten = kante.getVonKnoten();
			nachKnoten = kante.getNachKnoten();
		}
	}
}
