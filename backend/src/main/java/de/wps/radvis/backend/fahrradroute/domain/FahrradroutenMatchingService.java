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

package de.wps.radvis.backend.fahrradroute.domain;

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.springframework.data.util.Lazy;
import org.springframework.util.StopWatch;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteMatchingStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteNetzbezugResult;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradroutenMatchingAndRoutingInformation.FahrradroutenMatchingAndRoutingInformationBuilder;
import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.exception.KeineRouteGefundenException;
import de.wps.radvis.backend.matching.domain.repository.DlmMatchingRepository;
import de.wps.radvis.backend.matching.domain.repository.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilMatchResult;
import de.wps.radvis.backend.matching.domain.valueObject.RoutingResult;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FahrradroutenMatchingService {

	private final KantenRepository kantenRepository;
	private final Lazy<DlmMatchingRepository> dlmMatchingRepositorySupplier;
	private final Lazy<GraphhopperRoutingRepository> graphhopperRoutingRepositorySupplier;
	private final VerwaltungseinheitService verwaltungseinheitService;

	public FahrradroutenMatchingService(
		KantenRepository kantenRepository,
		Lazy<DlmMatchingRepository> dlmMatchingRepositorySupplier,
		Lazy<GraphhopperRoutingRepository> graphhopperRoutingRepositorySupplier,
		VerwaltungseinheitService verwaltungseinheitService) {
		this.kantenRepository = kantenRepository;
		this.dlmMatchingRepositorySupplier = dlmMatchingRepositorySupplier;
		this.graphhopperRoutingRepositorySupplier = graphhopperRoutingRepositorySupplier;
		this.verwaltungseinheitService = verwaltungseinheitService;
	}

	@NotNull
	public Optional<FahrradrouteNetzbezugResult> getFahrradrouteNetzbezugResult(
		LineString zuMatchendeGeometrie, FahrradrouteMatchingStatistik fahrradrouteMatchingStatistik,
		FahrradroutenMatchingAndRoutingInformationBuilder fahrradroutenMatchingAndRoutingInformationBuilder,
		boolean fahrtrichtungBeruecksichtigen) {
		StopWatch watch = new StopWatch("getFahrradrouteNetzbezugResult");
		// Erster Versuch
		Optional<FahrradrouteNetzbezugResult> netzbezugResult = createFahrradrouteNetzbezug(
			fahrradrouteMatchingStatistik, fahrradroutenMatchingAndRoutingInformationBuilder, zuMatchendeGeometrie,
			fahrtrichtungBeruecksichtigen, watch);
		// Zweiter Versuch nach Schneiden der Route
		if (netzbezugResult.isEmpty()) {
			fahrradrouteMatchingStatistik.anzahlRoutenTeilweiseAusserhalbBW++;
			log.info("Route wird auf den Bereich BW zugeschnitten.");
			Optional<LineString> zusammenhaengenderVerlaufInBw = findZusammenhaengendenVerlaufInBw(
				zuMatchendeGeometrie);
			if (zusammenhaengenderVerlaufInBw.isPresent()) {
				netzbezugResult = createFahrradrouteNetzbezug(fahrradrouteMatchingStatistik,
					fahrradroutenMatchingAndRoutingInformationBuilder,
					zusammenhaengenderVerlaufInBw.get(), fahrtrichtungBeruecksichtigen, watch);
			} else {
				fahrradrouteMatchingStatistik.anzahlNachZuschnittVieleTeilstrecken++;
				log.warn(
					"Es konnte kein Netzbezug erstellt werden, da die Route nach dem Zuschnitt auf BW aus mehreren, nicht verbundenen, Teilstrecken besteht");
			}

			if (netzbezugResult.isPresent()) {
				fahrradrouteMatchingStatistik.anzahlRoutenNachBwZuschnittErfolgreich++;
			}
		}
		log.info(watch.prettyPrint(TimeUnit.MILLISECONDS));
		return netzbezugResult;
	}

	/**
	 * Gibt den Teil der Geometrie (echt) innerhalb von BW zurück oder leer, wenn es mehr als einen oder keinen gibt
	 *
	 * @param originalGeometrie
	 * @return
	 */
	private Optional<LineString> findZusammenhaengendenVerlaufInBw(LineString originalGeometrie) {
		PreparedGeometry bw = verwaltungseinheitService.getBundeslandBereichPrepared();
		Coordinate[] originalCoordinates = originalGeometrie.getCoordinates();
		List<Coordinate> coordinatesInsideBw = new ArrayList<>();
		boolean isInside = isInsideBw(bw, originalCoordinates[0]);

		if (isInside) {
			coordinatesInsideBw.add(originalCoordinates[0]);
		}

		for (int i = 1; i < originalCoordinates.length; i++) {
			Coordinate nextCoordinate = originalCoordinates[i];
			boolean isNextInside = isInsideBw(bw, nextCoordinate);

			if (isNextInside) {
				if (!isInside && coordinatesInsideBw.size() > 0) {
					// Wir treten das zweite Mal in BW ein
					return Optional.empty();
				}

				coordinatesInsideBw.add(nextCoordinate);
			}

			isInside = isNextInside;
		}

		if (coordinatesInsideBw.size() > 1) {
			return Optional.of(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createLineString(coordinatesInsideBw.toArray(new Coordinate[coordinatesInsideBw.size()])));
		}

		return Optional.empty();
	}

	public Optional<LineString> schneideAnfangUndEndeAusserhalbBWsAb(LineString lineString) {
		PreparedGeometry bw = verwaltungseinheitService.getBundeslandBereichPrepared();
		Coordinate[] coordinates = lineString.getCoordinates();

		int firstIndexInBw = 0;
		boolean isStartOutside = !isInsideBw(bw, coordinates[0]);
		if (isStartOutside) {
			for (int i = 1; i < coordinates.length; i++) {
				if (isInsideBw(bw, coordinates[i])) {
					firstIndexInBw = i;
					break;
				}
			}
		}

		int lastIndexInBw = coordinates.length - 1;
		boolean isEndeOutside = !isInsideBw(bw, coordinates[0]);
		if (isEndeOutside) {
			for (int i = coordinates.length - 1; i >= 0; i--) {
				if (isInsideBw(bw, coordinates[i])) {
					lastIndexInBw = i;
					break;
				}
			}
		}

		List<Coordinate> anfangUndEndeAbgeschnitten = List.of(coordinates).subList(firstIndexInBw, lastIndexInBw + 1);
		return Optional.of(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(anfangUndEndeAbgeschnitten.toArray(new Coordinate[0])));
	}

	private static boolean isInsideBw(PreparedGeometry bw, Coordinate coordinates) {
		return bw.contains(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(coordinates));
	}

	private Optional<FahrradrouteNetzbezugResult> createFahrradrouteNetzbezug(
		FahrradrouteMatchingStatistik fahrradrouteMatchingStatistik,
		FahrradroutenMatchingAndRoutingInformationBuilder fahrradroutenMatchingAndRoutingInformationBuilder,
		LineString originalGeometrie,
		boolean fahrtrichtungBeruecksichtigen, StopWatch watch) {
		watch.start("try matching");
		try {
			FahrradrouteNetzbezugResult netzbezugResult = tryToCreateNetzbezugWithMatching(
				fahrradrouteMatchingStatistik,
				fahrradroutenMatchingAndRoutingInformationBuilder, originalGeometrie,
				fahrtrichtungBeruecksichtigen);
			log.info("Netzbezug durch Matching erstellt");
			return Optional.of(netzbezugResult);
		} catch (KeinMatchGefundenException e) {
			log.warn("Probiere Routing, da über Matching kein Netzbezug erstellt werden konnte: " + e.getMessage());
			fahrradrouteMatchingStatistik.anzahlMatchingFehlgeschlagen++;
		} finally {
			watch.stop();
		}

		watch.start("try routing");
		try {
			FahrradrouteNetzbezugResult netzbezugResult = tryToCreateNetzbezugWithRouting(
				fahrradrouteMatchingStatistik,
				fahrradroutenMatchingAndRoutingInformationBuilder, originalGeometrie,
				fahrtrichtungBeruecksichtigen);
			log.info("Netzbezug durch Routing erstellt");
			return Optional.of(netzbezugResult);
		} catch (KeineRouteGefundenException routingException) {
			log.warn("Routing Fehlgeschlagen: " + routingException.getMessage());
			fahrradrouteMatchingStatistik.anzahlRoutingFehlgeschlagen++;
		} finally {
			watch.stop();
		}

		return Optional.empty();
	}

	private FahrradrouteNetzbezugResult tryToCreateNetzbezugWithMatching(
		FahrradrouteMatchingStatistik fahrradrouteMatchingStatistik,
		FahrradroutenMatchingAndRoutingInformationBuilder fahrradroutenMatchingAndRoutingInformationBuilder,
		LineString originalGeometrie, boolean fahrtrichtungBeruecksichtigen)
		throws KeinMatchGefundenException {
		DlmMatchingRepository dlmMatchingRepository = dlmMatchingRepositorySupplier.get();

		String profileName = fahrtrichtungBeruecksichtigen ? "bike" : "foot";
		OsmMatchResult match = dlmMatchingRepository.matchGeometry(originalGeometrie, profileName);

		LineString bereinigt = LineStrings.entferneArtifizielleKehrtwenden(match.getGeometrie(), originalGeometrie);

		log.info("matche bereinigten LS");
		ProfilMatchResult profilMatch = dlmMatchingRepository.matchGeometryUndDetails(
			bereinigt, profileName);

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug = createNetzbezug(
			profilMatch.getOsmWayIdsUnwrapped());
		fahrradrouteMatchingStatistik.anzahlMatchingErfolgreich++;

		fuelleFahradroutenMatchingAndRoutingInformation(fahrradroutenMatchingAndRoutingInformationBuilder,
			profilMatch.getGeometrie(), fahrradrouteMatchingStatistik, originalGeometrie, false);

		return new FahrradrouteNetzbezugResult(profilMatch.getGeometrie(), abschnittsweiserKantenBezug,
			profilMatch.getProfilEigenschaften());
	}

	private FahrradrouteNetzbezugResult tryToCreateNetzbezugWithRouting(
		FahrradrouteMatchingStatistik fahrradrouteMatchingStatistik,
		FahrradroutenMatchingAndRoutingInformationBuilder fahrradroutenMatchingAndRoutingInformationBuilder,
		LineString originalGeometrie,
		boolean fahrtrichtungBeruecksichtigen)
		throws KeineRouteGefundenException {

		RoutingResult routingResult = routeGeometryAufDLM(originalGeometrie,
			graphhopperRoutingRepositorySupplier.get(), fahrtrichtungBeruecksichtigen);

		LineString bereinigt = LineStrings.entferneArtifizielleKehrtwenden(routingResult.getRoutenGeometrie(),
			originalGeometrie);

		List<Long> kantenIDs;
		ProfilMatchResult match;
		try {
			log.info("matche bereinigten LS");
			match = dlmMatchingRepositorySupplier.get().matchGeometryUndDetails(bereinigt, "bike");
			kantenIDs = match.getOsmWayIdsUnwrapped();
		} catch (KeinMatchGefundenException e) {
			match = null;
			log.error("Kein match für Geometrie ohne Kehrtwenden!");
			kantenIDs = routingResult.getKantenIDs();
		}

		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug = createNetzbezug(kantenIDs);

		LineString matchedOrRoutedGeometry = match != null ? match.getGeometrie() : routingResult.getRoutenGeometrie();
		fuelleFahradroutenMatchingAndRoutingInformation(fahrradroutenMatchingAndRoutingInformationBuilder,
			matchedOrRoutedGeometry, fahrradrouteMatchingStatistik, originalGeometrie, true);

		fahrradrouteMatchingStatistik.anzahlRoutingErfolgreich++;
		return new FahrradrouteNetzbezugResult(matchedOrRoutedGeometry, abschnittsweiserKantenBezug,
			match != null ? match.getProfilEigenschaften() : new ArrayList<>());
	}

	private void fuelleFahradroutenMatchingAndRoutingInformation(
		FahrradroutenMatchingAndRoutingInformationBuilder fahrradroutenMatchingAndRoutingInformationBuilder,
		LineString routingResult, FahrradrouteMatchingStatistik fahrradrouteMatchingStatistik,
		LineString originalGeometry, boolean abbildungDurchRoutingErfolgreich) {

		fahrradroutenMatchingAndRoutingInformationBuilder.routedOrMatchedGeometry(routingResult);

		// Kehrtwenden
		MultiPoint kehrtwenden = LineStrings.findeKehrtwenden(routingResult);
		if (!kehrtwenden.isEmpty()) {
			fahrradrouteMatchingStatistik.anzahlRoutenMitKehrtwenden++;
			fahrradrouteMatchingStatistik.anzahlKehrtwendenGesamt += kehrtwenden.getNumGeometries();
		}
		fahrradroutenMatchingAndRoutingInformationBuilder.kehrtwenden(kehrtwenden);

		// Abweichende Segmente
		MultiLineString abweichendeSegmente = LineStrings.findeSegmenteZweierLinestringsMitAbstandGroesserAls(
			originalGeometry, routingResult, 20);
		if (!abweichendeSegmente.isEmpty()) {
			fahrradrouteMatchingStatistik.anzahlRoutenMitAbweichungen++;
			fahrradroutenMatchingAndRoutingInformationBuilder.abweichendeSegmente(abweichendeSegmente);
		}

		// loggen ob matching oder routing
		fahrradroutenMatchingAndRoutingInformationBuilder.abbildungDurchRouting(abbildungDurchRoutingErfolgreich);
	}

	private List<AbschnittsweiserKantenBezug> createNetzbezug(List<Long> kantenIDs) {
		/*
		 * Aus den KantenIds die kantenAbschnitte zusammensetzten Datei als LineareReferenz immer 0 bis 1 setzten, da
		 * das Ermitteln des Teilabschnittes (MappedGrundNetzKante) durch "calculateUeberschneidungsLineString" nicht
		 * fuer nicht-simple Geometrien moeglich ist, wie es bei den importierten Fahrradrouten normalerweise der Fall
		 * ist.
		 */
		return kantenIDs.stream()
			.map(kantenRepository::findById)
			.map(Optional::orElseThrow)
			.map(kante -> new AbschnittsweiserKantenBezug(kante, LinearReferenzierterAbschnitt.of(0, 1)))
			.collect(Collectors.toList());
	}

	private RoutingResult routeGeometryAufDLM(Geometry originalGeometrie,
		GraphhopperRoutingRepository graphhopperRoutingRepository, boolean fahrtrichtungBeruecksichtigen)
		throws KeineRouteGefundenException {
		require(originalGeometrie, notNullValue());

		Geometry simplifiedGeometry = originalGeometrie;
		// 1 Stützpunkt pro 500 meter, durchschnittlich
		AtomicInteger count = new AtomicInteger();
		while (simplifiedGeometry.getCoordinates().length > originalGeometrie.getLength() / 200 && count.get() < 10) {
			simplifiedGeometry = TopologyPreservingSimplifier
				.simplify(originalGeometrie, count.incrementAndGet() * 10);
		}

		return graphhopperRoutingRepository.route(Arrays.asList(simplifiedGeometry.getCoordinates()),
			GraphhopperRoutingRepository.DEFAULT_PROFILE_ID,
			fahrtrichtungBeruecksichtigen);
	}
}
