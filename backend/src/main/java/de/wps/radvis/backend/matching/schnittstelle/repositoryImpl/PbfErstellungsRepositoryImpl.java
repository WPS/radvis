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

package de.wps.radvis.backend.matching.schnittstelle.repositoryImpl;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

import com.slimjars.dist.gnu.trove.list.TLongList;
import com.slimjars.dist.gnu.trove.list.array.TLongArrayList;

import de.topobyte.osm4j.core.access.OsmOutputStream;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Tag;
import de.topobyte.osm4j.core.model.impl.Way;
import de.topobyte.osm4j.pbf.seq.PbfWriter;
import de.wps.radvis.backend.barriere.domain.entity.Barriere;
import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.barriere.domain.valueObject.BarrierenForm;
import de.wps.radvis.backend.common.domain.RamUsageUtility;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.matching.domain.PbfErstellungsRepository;
import de.wps.radvis.backend.matching.domain.entity.NodeIndex;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.KommunalnetzAlltagEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.KommunalnetzFreizeitEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.KreisnetzAlltagEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.KreisnetzFreizeitEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadNetzAlltagEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadNetzFreizeitEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadNetzZielnetzEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadschnellverbindungEncodedValue;
import de.wps.radvis.backend.matching.schnittstelle.encodedValues.RadvorrangroutenEncodedValue;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.SeitenbezogeneProfilEigenschaften;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class PbfErstellungsRepositoryImpl implements PbfErstellungsRepository {
	private final CoordinateReferenceSystemConverter converter;
	private final EntityManager entityManager;
	private final BarriereRepository barriereRepository;
	private final KantenRepository kantenRepository;

	/**
	 * @param partitionToKantenMap
	 *     Jeder Stream der Liste beinhaltet die Kanten einer Spalte des in Spalten partitionierten Bereichs.
	 *     Dies ist nötig um bei großen Datenmengen keinen OutOfMemory zu erhalten. Es wird spaltenweise
	 *     partioniert, um die Handhabung der Indexe möglichst einfach zu gestalten. Für Jede Kante wird genau 1
	 *     Way in die PBF geschrieben. Wenn sich zwei Kanten eine Node teilen, dann ist dies eine topologische
	 *     Verbindung.
	 * @param outputFile
	 *     Zieldatei
	 */
	@Override
	public void writePbf(Map<Envelope, Stream<Kante>> partitionToKantenMap, File outputFile) {
		require(partitionToKantenMap, notNullValue());
		require(outputFile, notNullValue());

		try (OutputStream output = new FileOutputStream(outputFile)) {
			PbfWriter osmOutput = new PbfWriter(output, true);
			osmOutput.setBatchLimit(100_000);

			Set<Long> dlmKantenBereitsAbgearbeitet = new HashSet<>();

			NodeIndex nodeIndex = new NodeIndex();

			AtomicLong i = new AtomicLong(0);
			AtomicLong entityId = new AtomicLong(0);

			List<Map.Entry<Envelope, Stream<Kante>>> sortedMapEntries = partitionToKantenMap.entrySet().stream()
				.sorted(Comparator.comparing(entry -> entry.getKey().getMinX())).collect(Collectors.toList());

			double maxX = sortedMapEntries.get(sortedMapEntries.size() - 1).getKey().getMaxX();

			Map<Long, Set<Barriere>> kanteToBarrieren = getBarrierenForKanten();

			for (Map.Entry<Envelope, Stream<Kante>> spalte : sortedMapEntries) {
				Envelope envelope = spalte.getKey();

				RamUsageUtility.logCurrentRamUsage(
					String.format("Vor envelope %s in %s", envelope, this.getClass().getSimpleName()));

				Envelope envelopeWGS84 = converter.transformEnvelope(
					new Envelope(envelope.getMinX(),
						maxX,
						envelope.getMinY(),
						envelope.getMaxY()),
					KoordinatenReferenzSystem.ETRS89_UTM32_N, KoordinatenReferenzSystem.WGS84);

				log.info("Schreibe Pbf für Partition {}", envelope);
				Stream<Kante> kanten = spalte.getValue();
				kanten.forEach(kante -> {
					if (!dlmKantenBereitsAbgearbeitet.contains(kante.getId())) {
						dlmKantenBereitsAbgearbeitet.add(kante.getId());

						try {
							List<Long> newNodeIds = buildNodes(kante, entityId, nodeIndex, osmOutput);
							Way way = buildWay(newNodeIds, kante.getId());
							addTags(way, kante, kanteToBarrieren.getOrDefault(kante.getId(), new HashSet<>()));
							osmOutput.write(way);
						} catch (IOException e) {
							log.error(e.getMessage(), e);
						}
						if (i.incrementAndGet() % 100000 == 0) {
							log.info("Es wurden {} Kanten bearbeitet", i.get());
						}
					}
				});
				this.entityManager.flush();
				this.entityManager.clear();

				System.gc();
				RamUsageUtility.logCurrentRamUsage(
					String.format("Nach envelope %s in %s", envelope, this.getClass().getSimpleName()));

			}

			osmOutput.complete();

		} catch (IOException e) {
			log.error("Fehler beim Schreiben der .pbf-Datei. Konfigurierter Dateipfad: {}",
				outputFile.getAbsolutePath(), e);
		}
	}

	private Map<Long, Set<Barriere>> getBarrierenForKanten() {
		HashMap<Long, Set<Barriere>> result = new HashMap<>();

		barriereRepository.findAll().forEach(barriere -> {
			Set<Kante> kanten = new HashSet<>();

			// Ermittle Kanten aus linear referenzierten Abschnitten
			kanten.addAll(
				barriere.getNetzbezug()
					.getImmutableKantenAbschnittBezug()
					.stream()
					.map(AbschnittsweiserKantenBezug::getKante)
					.collect(Collectors.toSet())
			);

			// Ermittle Kanten aus linear referenzierten Punktuellen Netzbezügen
			kanten.addAll(
				barriere.getNetzbezug()
					.getImmutableKantenPunktBezug()
					.stream()
					.map(PunktuellerKantenSeitenBezug::getKante)
					.collect(Collectors.toSet())
			);

			// Ermittle adjazente Kanten zu Knoten auf denen Barrieren sind. Graphhopper kann keine Gewichtungen auf
			// Knoten definieren, daher attributieren wir die adjazenten Kanten des Knotens.
			kanten.addAll(
				barriere.getNetzbezug()
					.getImmutableKnotenBezug()
					.stream()
					.map(kantenRepository::getAdjazenteKanten)
					.flatMap(Collection::stream)
					.collect(Collectors.toSet())
			);

			kanten.forEach(kante -> {
				if (!result.containsKey(kante.getId())) {
					result.put(kante.getId(), new HashSet<>());
				}
				result.get(kante.getId()).add(barriere);
			});

		});

		return result;
	}

	private void addTags(Way way, Kante kante, Set<Barriere> barrieren) {
		List<Tag> tags = new ArrayList<>();
		tags.add(new Tag("highway", "track"));

		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = kante.getFahrtrichtungAttributGruppe();
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = kante.getFuehrungsformAttributGruppe();

		tags.addAll(buildFahrtrichtungTags(fahrtrichtungAttributGruppe));
		tags.addAll(buildNetzklassenTags(kante));
		tags.addAll(buildBelagartTags(kante));
		tags.addAll(buildFuehrungsformTags(kante));
		tags.addAll(buildOberflaechenbeschaffenheitTags(fuehrungsformAttributGruppe));
		tags.addAll(buildBreiteTags(fuehrungsformAttributGruppe));
		tags.add(buildBeleuchtungTag(kante));
		tags.addAll(buildDtvPkwTag(kante));
		tags.addAll(buildBarriereTag(kante, barrieren));

		way.setTags(tags);
	}

	private List<Tag> buildBarriereTag(Kante kante, Set<Barriere> barrieren) {
		List<Tag> tags = new ArrayList<>();

		BarrierenForm formRechts = null;
		BarrierenForm formLinks = null;

		// 1. Ermittle Barriere-Form aus Knoten-Barrieren, an die die übergebene Kante startet oder endet.
		Optional<Barriere> adjazenteBarriere = barrieren.stream()
			.filter(barriere -> {
				Set<Knoten> netzbezug = barriere.getNetzbezug().getImmutableKnotenBezug();
				return netzbezug.contains(kante.getVonKnoten()) || netzbezug.contains(kante.getNachKnoten());
			}
			)
			.findFirst();
		if (adjazenteBarriere.isPresent()) {
			formRechts = adjazenteBarriere.get().getBarrierenForm();
			formLinks = adjazenteBarriere.get().getBarrierenForm();
		}

		// 2. Ermittle Barriere-Form aus Punkt-Netzbezügen der übergebenen Kante. Überschreibt ggf. gefundene Barriere-
		// Formen aus obigen Ermittlungen.
		Optional<Barriere> punktuelleBarriereRechts = getPunktuelleBarriereForSeite(kante, barrieren,
			Seitenbezug.RECHTS);
		if (punktuelleBarriereRechts.isPresent()) {
			formRechts = punktuelleBarriereRechts.get().getBarrierenForm();
		}
		Optional<Barriere> punktuelleBarriereLinks = getPunktuelleBarriereForSeite(kante, barrieren,
			Seitenbezug.LINKS);
		if (punktuelleBarriereLinks.isPresent()) {
			formLinks = punktuelleBarriereLinks.get().getBarrierenForm();
		}

		// 3. Ermittle Barriere-Form aus linear referenzierten Abschnitten der übergebenen Kante. Überschreibt ggf.
		// gefundene Barriere-Formen aus obigen Ermittlungen.
		Optional<Barriere> laengsteBarriereRechts = getLaengsteBarriereForSeite(kante, barrieren, Seitenbezug.RECHTS);
		if (laengsteBarriereRechts.isPresent()) {
			formRechts = laengsteBarriereRechts.get().getBarrierenForm();
		}
		Optional<Barriere> laengsteBarriereLinks = getLaengsteBarriereForSeite(kante, barrieren, Seitenbezug.LINKS);
		if (laengsteBarriereLinks.isPresent()) {
			formLinks = laengsteBarriereLinks.get().getBarrierenForm();
		}

		// Tags aus oben ermittelter Barriere-Form erstellen
		if (formRechts != null) {
			tags.add(new Tag("barriere:right", formRechts.name()));
		}
		if (formLinks != null) {
			tags.add(new Tag("barriere:left", formLinks.name()));
		}

		return tags;
	}

	@NotNull
	private static Optional<Barriere> getPunktuelleBarriereForSeite(Kante kante, Set<Barriere> barrieren,
		Seitenbezug seitenbezug) {
		return barrieren.stream()
			.filter(barriere -> barriere.getNetzbezug()
				.getImmutableKantenPunktBezug()
				.stream()
				.anyMatch(netzbezug ->
				// Beidseitig mit betrachten, da ja eine beidseitige Barriere auch für die Seite "seitenbezug" gilt.
				(netzbezug.getSeitenbezug() == Seitenbezug.BEIDSEITIG || netzbezug.getSeitenbezug() == seitenbezug)
					&& netzbezug.getKante().getId().equals(kante.getId()))
			)
			.findFirst();
	}

	@NotNull
	private static Optional<Barriere> getLaengsteBarriereForSeite(Kante kante, Set<Barriere> barrieren,
		Seitenbezug seitenbezug) {
		return barrieren
			.stream()
			.filter(barriere -> barriere.getNetzbezug()
				.getImmutableKantenAbschnittBezug()
				.stream()
				.anyMatch(n -> n.getKante().getId().equals(kante.getId()) &&
				// Beidseitig mit betrachten, da ja eine beidseitige Barriere auch für die Seite "seitenbezug" gilt.
					(n.getSeitenbezug() == Seitenbezug.BEIDSEITIG || n.getSeitenbezug() == seitenbezug)
				)
			)
			.max((barriere1, barriere2) -> {
				Optional<Double> laengeVomLaengstenAbschnitt1 = barriere1.getNetzbezug()
					.getImmutableKantenAbschnittBezug()
					.stream()
					.map(n -> n.getLinearReferenzierterAbschnitt().relativeLaenge())
					.max(Double::compare);
				if (laengeVomLaengstenAbschnitt1.isEmpty()) {
					return -1;
				}

				Optional<Double> laengeVomLaengstenAbschnitt2 = barriere2.getNetzbezug()
					.getImmutableKantenAbschnittBezug()
					.stream()
					.map(n -> n.getLinearReferenzierterAbschnitt().relativeLaenge())
					.max(Double::compare);
				if (laengeVomLaengstenAbschnitt2.isEmpty()) {
					return 1;
				}

				return Double.compare(laengeVomLaengstenAbschnitt1.get(), laengeVomLaengstenAbschnitt2.get());
			}
			);
	}

	private List<Tag> buildFahrtrichtungTags(FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe) {
		List<Tag> tags = new ArrayList<>();

		switch (fahrtrichtungAttributGruppe.befahrbarIn()) {
		case BEIDE_RICHTUNGEN:
			tags.add(new Tag("oneway", "no"));
			break;
		case IN_RICHTUNG:
			tags.add(new Tag("oneway", "yes"));
			break;
		case GEGEN_RICHTUNG:
			tags.add(new Tag("oneway", "-1"));
		}

		return tags;
	}

	private List<Tag> buildNetzklassenTags(Kante kante) {
		List<Tag> tags = new ArrayList<>();

		boolean hasRadNetzAlltag = kante.getKantenAttributGruppe().getNetzklassen().contains(Netzklasse.RADNETZ_ALLTAG);
		tags.add(new Tag(RadNetzAlltagEncodedValue.getOsmKey(), Boolean.toString(hasRadNetzAlltag)));

		boolean hasRadNetzFreizeit = kante.getKantenAttributGruppe().getNetzklassen()
			.contains(Netzklasse.RADNETZ_FREIZEIT);
		tags.add(new Tag(RadNetzFreizeitEncodedValue.getOsmKey(), Boolean.toString(hasRadNetzFreizeit)));

		boolean hasRadNetzZielnetz = kante.getKantenAttributGruppe().getNetzklassen()
			.contains(Netzklasse.RADNETZ_ZIELNETZ);
		tags.add(new Tag(RadNetzZielnetzEncodedValue.getOsmKey(), Boolean.toString(hasRadNetzZielnetz)));

		boolean hasKreisnetzAlltag = kante.getKantenAttributGruppe().getNetzklassen()
			.contains(Netzklasse.KREISNETZ_ALLTAG);
		tags.add(new Tag(KreisnetzAlltagEncodedValue.getOsmKey(), Boolean.toString(hasKreisnetzAlltag)));

		boolean hasKreisnetzFreizeit = kante.getKantenAttributGruppe().getNetzklassen()
			.contains(Netzklasse.KREISNETZ_FREIZEIT);
		tags.add(new Tag(KreisnetzFreizeitEncodedValue.getOsmKey(), Boolean.toString(hasKreisnetzFreizeit)));

		boolean hasKommunalnetzAlltag = kante.getKantenAttributGruppe().getNetzklassen()
			.contains(Netzklasse.KOMMUNALNETZ_ALLTAG);
		tags.add(new Tag(KommunalnetzAlltagEncodedValue.getOsmKey(), Boolean.toString(hasKommunalnetzAlltag)));

		boolean hasKommunalnetzFreizeit = kante.getKantenAttributGruppe().getNetzklassen()
			.contains(Netzklasse.KOMMUNALNETZ_FREIZEIT);
		tags.add(new Tag(KommunalnetzFreizeitEncodedValue.getOsmKey(), Boolean.toString(hasKommunalnetzFreizeit)));

		boolean hasRadschnellverbindung = kante.getKantenAttributGruppe().getNetzklassen()
			.contains(Netzklasse.RADSCHNELLVERBINDUNG);
		tags.add(new Tag(RadschnellverbindungEncodedValue.getOsmKey(), Boolean.toString(hasRadschnellverbindung)));

		boolean hasRadvorrangroute = kante.getKantenAttributGruppe().getNetzklassen()
			.contains(Netzklasse.RADVORRANGROUTEN);
		tags.add(new Tag(RadvorrangroutenEncodedValue.getOsmKey(), Boolean.toString(hasRadvorrangroute)));

		return tags;
	}

	private List<Tag> buildBelagartTags(Kante kante) {
		SeitenbezogeneProfilEigenschaften seitenbezogeneProfilEigenschaften = kante.getProfilEigenschaften();
		return List.of(
			new Tag("belagart:left", seitenbezogeneProfilEigenschaften.getBelagArtLinks().name()),
			new Tag("belagart:right", seitenbezogeneProfilEigenschaften.getBelagArtRechts().name())
		);
	}

	private List<Tag> buildFuehrungsformTags(Kante kante) {
		SeitenbezogeneProfilEigenschaften seitenbezogeneProfilEigenschaften = kante.getProfilEigenschaften();
		return List.of(
			new Tag("fuehrung:left", seitenbezogeneProfilEigenschaften.getRadverkehrsfuehrungLinks().name()),
			new Tag("fuehrung:right", seitenbezogeneProfilEigenschaften.getRadverkehrsfuehrungRechts().name())
		);
	}

	private List<Tag> buildOberflaechenbeschaffenheitTags(FuehrungsformAttributGruppe fuehrungsformAttributGruppe) {
		return List.of(
			new Tag("oberflaeche:left",
				fuehrungsformAttributGruppe.getOberflaechenbeschaffenheitWertMitGroesstemAnteilLinks().name()),
			new Tag("oberflaeche:right",
				fuehrungsformAttributGruppe.getOberflaechenbeschaffenheitWertMitGroesstemAnteilRechts().name())
		);
	}

	private List<Tag> buildBreiteTags(FuehrungsformAttributGruppe fuehrungsformAttributGruppe) {
		List<Tag> tags = new ArrayList<>();

		fuehrungsformAttributGruppe.getBreiteWertMitGroesstemAnteilLinks()
			.ifPresent(laenge -> tags.add(new Tag("breite:left", laenge.getValue() + "")));
		fuehrungsformAttributGruppe.getBreiteWertMitGroesstemAnteilRechts()
			.ifPresent(laenge -> tags.add(new Tag("breite:right", laenge.getValue() + "")));

		return tags;
	}

	private Tag buildBeleuchtungTag(Kante kante) {
		return new Tag("beleuchtung", kante.getKantenAttributGruppe().getKantenAttribute().getBeleuchtung().name());
	}

	private List<Tag> buildDtvPkwTag(Kante kante) {
		List<Tag> tags = new ArrayList<>();

		kante.getKantenAttributGruppe().getKantenAttribute().getDtvPkw()
			.ifPresent(verkehrStaerke -> tags.add(new Tag("dtv:pkw", verkehrStaerke.getValue() + "")));

		return tags;
	}

	private List<Long> buildNodes(Kante kante, AtomicLong entityId, NodeIndex nodeIndex,
		OsmOutputStream osmOutputStream)
		throws IOException {
		List<Node> nodesOfKante = getNodes(kante, entityId);

		List<Long> topologischIntegrierteNodes = new ArrayList<>();

		Node startNode = nodesOfKante.get(0);
		topologischIntegrierteNodes.add(
			findExistingNodeOrWrite(startNode, nodeIndex, osmOutputStream));

		// Unser Kantenmodell bildet Topologie nur am Anfang und Ende der Kante ab, deshalb schreiben wir für
		// alle anderen Nodes immer neue Nodes, da an diesen Stellen nie eine topologische Verbindung existieren sollte
		for (Node node : nodesOfKante.subList(1, nodesOfKante.size() - 1)) {
			topologischIntegrierteNodes.add(node.getId());
			osmOutputStream.write(node);
		}

		Node endNode = nodesOfKante.get(nodesOfKante.size() - 1);
		topologischIntegrierteNodes.add(
			findExistingNodeOrWrite(endNode, nodeIndex, osmOutputStream));

		return topologischIntegrierteNodes;
	}

	private Long findExistingNodeOrWrite(Node node, NodeIndex nodeIndex, OsmOutputStream osmOutputStream)
		throws IOException {
		Optional<Long> existingNode = nodeIndex.finde(node);
		if (existingNode.isPresent()) {
			return existingNode.get();
		} else {
			nodeIndex.fuegeEin(node);
			osmOutputStream.write(node);
			return node.getId();
		}
	}

	private Way buildWay(List<Long> noteIds, long id) {
		TLongList nodeIds = new TLongArrayList();
		nodeIds.addAll(noteIds);
		return new Way(id, nodeIds);
	}

	private List<Node> getNodes(Kante kante, AtomicLong lastId) {
		Coordinate[] coordinates = kante.getGeometry().getCoordinates();
		List<Coordinate> unique = new ArrayList<>();
		for (Coordinate coordinate : coordinates) {
			if (!unique.contains(coordinate)) {
				unique.add(coordinate);
			}
		}

		return unique.stream()
			.map(coordinate -> converter
				.transformCoordinateUnsafe(coordinate, KoordinatenReferenzSystem.ETRS89_UTM32_N,
					KoordinatenReferenzSystem.WGS84))
			.map(coordinate -> new Node(lastId.incrementAndGet(), coordinate.y, coordinate.x))
			.collect(Collectors.toList());
	}
}
