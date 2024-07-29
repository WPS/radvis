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

package de.wps.radvis.backend.massnahme.domain;

import static org.locationtech.jts.geom.Geometry.TYPENAME_MULTILINESTRING;
import static org.locationtech.jts.geom.Geometry.TYPENAME_POINT;
import static org.valid4j.Assertive.require;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.api.feature.simple.SimpleFeature;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmenImportProtokoll;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmenPaketId;
import de.wps.radvis.backend.matching.domain.entity.MappedGrundnetzkante;
import de.wps.radvis.backend.matching.domain.entity.MatchingStatistik;
import de.wps.radvis.backend.matching.domain.service.SimpleMatchingService;
import de.wps.radvis.backend.matching.domain.valueObject.OsmMatchResult;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.OsmWayId;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * @deprecated RAD-6071: Das Zuständigkeitsfeld an Massnahmen ist ein Pflichtfeld. Der MappingService ist deprecated,
 *     da er einen veralteten Import implementiert, der nicht mehr genutzt wird. Dieser ImportJob wird langfristig entfernt.
 */
@Slf4j
@Deprecated
public class MassnahmenImportJob extends AbstractJob {
	private static final double KNOTEN_PREFERENCE_TOLERANZ = 2;
	private final ShapeFileRepository shapeFileRepository;
	private final SimpleMatchingService simpleMatchingService;
	private final NetzService netzService;
	private final MassnahmenMappingService massnahmenMappingService;
	private final MassnahmeService massnahmeService;
	private final BenutzerService benutzerService;

	private final Path shpFilesRootFolderPath;

	public MassnahmenImportJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		Path shpFilesRootFolderPath,
		ShapeFileRepository shapeFileRepository, SimpleMatchingService simpleMatchingService,
		NetzService netzService, MassnahmenMappingService massnahmenMappingService,
		MassnahmeService massnahmeService, BenutzerService benutzerService) {
		super(jobExecutionDescriptionRepository);
		this.shapeFileRepository = shapeFileRepository;
		this.shpFilesRootFolderPath = shpFilesRootFolderPath;
		this.simpleMatchingService = simpleMatchingService;
		this.massnahmenMappingService = massnahmenMappingService;
		this.netzService = netzService;
		this.massnahmeService = massnahmeService;
		this.benutzerService = benutzerService;

	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.MASSNAHME_IMPORT_JOB)
	public JobExecutionDescription run() {
		return super.run(false);
	}

	@Override
	@Transactional
	@WithAuditing(context = AuditingContext.MASSNAHME_IMPORT_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		Benutzer technischerBenutzer = benutzerService.getTechnischerBenutzer();

		MassnahmenImportProtokoll massnahmenImportProtokoll = new MassnahmenImportProtokoll();

		Set<String> massnahmenPaketIdsAktuellerImport = new HashSet<>();

		Set<MassnahmenPaketId> massnahmenPaketIdsVorherigeImporte = massnahmeService.findAllMassnahmenPaketIds();

		getShpFiles().forEach(shpFile -> {
			massnahmenImportProtokoll.countFiles.incrementAndGet();
			log.info("Extrahiere Maßnahmen aus {}", shpFile.getName());
			try (Stream<SimpleFeature> simpleFeatureStream = this.shapeFileRepository.readShape(shpFile)) {
				AtomicInteger anzahlMassnahmenInShapefile = new AtomicInteger();
				AtomicInteger anzahlFeaturesInShapefile = new AtomicInteger();

				simpleFeatureStream
					.map(shapeFileRepository::transformGeometryToUTM32)
					.forEach(simpleFeature -> {
						anzahlFeaturesInShapefile.incrementAndGet();
						String massnahmenPaketId = MassnahmenPaketIdExtractor.getMassnahmenPaketId(simpleFeature);
						if (massnahmenPaketIdsVorherigeImporte.contains(MassnahmenPaketId.of(massnahmenPaketId))) {
							log.info(
								"Massnahme mit MassnahmenPaketId {} wurde in vorherigen Importvorgängen bereits importiert. Wird übersprungen.",
								massnahmenPaketId);
							return;
						}

						if (!isMassnahmeFeatureValid(massnahmenImportProtokoll, massnahmenPaketIdsAktuellerImport,
							shpFile.getName(),
							simpleFeature, massnahmenPaketId)) {
							return;
						}
						Optional<MassnahmeNetzBezug> netzbezug = bestimmeNetzbezug(massnahmenImportProtokoll,
							simpleFeature,
							massnahmenPaketId);

						if (netzbezug.isEmpty()) {
							return;
						}

						List<Massnahme> massnahmen = massnahmenMappingService.createMassnahmen(simpleFeature,
							netzbezug.get(), massnahmenImportProtokoll, technischerBenutzer);

						massnahmen.forEach(massnahmeService::saveMassnahme);
						anzahlMassnahmenInShapefile.addAndGet(massnahmen.size());
					});
				log.info("Anzahl Features: {}", anzahlFeaturesInShapefile.get());
				log.info("Anzahl erstellter Massnahmen: {}", anzahlMassnahmenInShapefile.get());

				massnahmenImportProtokoll.gesamtanzahlMassnahmen.addAndGet(anzahlMassnahmenInShapefile.get());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		massnahmenImportProtokoll.log();

		return Optional.of(massnahmenImportProtokoll.matchingStatistik);
	}

	private Optional<MassnahmeNetzBezug> bestimmeNetzbezug(MassnahmenImportProtokoll massnahmenImportProtokoll,
		SimpleFeature simpleFeature, String massnahmenPaketId) {
		Geometry geometry = (Geometry) simpleFeature.getDefaultGeometry();

		switch (geometry.getGeometryType()) {
		case TYPENAME_MULTILINESTRING:
			return bestimmeStreckenNetzbezug(simpleFeature, massnahmenPaketId,
				massnahmenImportProtokoll);
		case TYPENAME_POINT:
			return bestimmePunktNetzbezugForPoint((Point) geometry, massnahmenPaketId, massnahmenImportProtokoll);
		default:
			throw new RuntimeException(
				"Die Massnahme enthält einen unerwarteten Geometrietyp: " + geometry.getGeometryType());
		}
	}

	private Optional<MassnahmeNetzBezug> bestimmeStreckenNetzbezug(SimpleFeature simpleFeature,
		String massnahmenPaketId,
		MassnahmenImportProtokoll massnahmenImportProtokoll) {
		List<OsmMatchResult> matchResults = matchSimpleFeature(simpleFeature,
			massnahmenImportProtokoll.matchingStatistik);

		if (matchResults.size() != ((Geometry) simpleFeature.getDefaultGeometry()).getNumGeometries()) {
			massnahmenImportProtokoll.reportMatchUnvollstaendig(massnahmenPaketId);
			return Optional.empty();
		}

		Optional<MassnahmeNetzBezug> netzbezug = createNetzbezug(matchResults);
		if (netzbezug.isEmpty()) {
			massnahmenImportProtokoll.reportEmptyNetzbezug(massnahmenPaketId);
		}

		return netzbezug;
	}

	Optional<MassnahmeNetzBezug> bestimmePunktNetzbezugForPoint(Point geometry, String massnahmenPaketId,
		MassnahmenImportProtokoll massnahmenImportProtokoll) {

		Envelope envelope = geometry.getEnvelopeInternal();
		envelope.expandBy(30);
		List<Kante> radVisNetzKantenInBereich = netzService.getRadVisNetzKantenInBereich(envelope);
		List<Knoten> radVisNetzKnotenInBereich = netzService.getRadVisNetzKnotenInBereich(envelope);

		Set<Knoten> knotenNetzbezug = new HashSet<>();
		Set<PunktuellerKantenSeitenBezug> punktuellerKantenbezug = new HashSet<>();

		radVisNetzKantenInBereich.sort(Comparator.comparing(Kante::getGeometry,
			Comparator.comparingDouble(linestring -> linestring.distance(geometry))));
		radVisNetzKnotenInBereich.sort(Comparator.comparing(Knoten::getPoint,
			Comparator.comparingDouble(point -> point.distance(geometry))));

		if (radVisNetzKantenInBereich.isEmpty() && radVisNetzKnotenInBereich.isEmpty()) {
			massnahmenImportProtokoll.reportKeineRadVisNetzGeometrieInReichweite(massnahmenPaketId);
			return Optional.empty();
		}
		if (radVisNetzKantenInBereich.isEmpty()) {
			Knoten closestKnoten = radVisNetzKnotenInBereich.get(0);
			knotenNetzbezug.add(closestKnoten);
		} else if (radVisNetzKnotenInBereich.isEmpty()) {
			Kante closestKante = radVisNetzKantenInBereich.get(0);
			LineareReferenz LR = LineareReferenz.of(closestKante.getGeometry(), geometry.getCoordinate());
			PunktuellerKantenSeitenBezug pKSB = new PunktuellerKantenSeitenBezug(
				closestKante, LR, Seitenbezug.BEIDSEITIG);
			punktuellerKantenbezug.add(pKSB);
		} else {
			Kante closestKante = radVisNetzKantenInBereich.get(0);
			Knoten closestKnoten = radVisNetzKnotenInBereich.get(0);
			// nimm nächste Geometrie aber bevorzuge Knoten
			if (closestKante.getGeometry().distance(geometry) <= closestKnoten.getPoint().distance(geometry)
				- KNOTEN_PREFERENCE_TOLERANZ) {
				LineareReferenz LR = LineareReferenz.of(closestKante.getGeometry(),
					geometry.getCoordinate());
				PunktuellerKantenSeitenBezug pKSB = new PunktuellerKantenSeitenBezug(
					closestKante, LR, Seitenbezug.BEIDSEITIG);
				punktuellerKantenbezug.add(pKSB);
			} else {
				knotenNetzbezug.add(closestKnoten);
			}
		}

		return Optional.of(new MassnahmeNetzBezug(Collections.emptySet(), punktuellerKantenbezug, knotenNetzbezug));
	}

	private boolean isMassnahmeFeatureValid(MassnahmenImportProtokoll massnahmenImportProtokoll,
		Set<String> massnahmenPaketIds, String shpfileName, SimpleFeature simpleFeature, String massnahmenPaketId) {
		if (massnahmenPaketId == null || massnahmenPaketId.isEmpty()) {
			massnahmenImportProtokoll.reportKeineMassnahmePaketId(
				shpfileName + simpleFeature.getDefaultGeometry().toString());
			return false;
		}

		if (!MassnahmenPaketId.isValid(massnahmenPaketId)) {
			massnahmenImportProtokoll.reportUngueltigeMassnahmePaketId(
				massnahmenPaketId);
			return false;
		}

		if (!massnahmenPaketIds.add(massnahmenPaketId)) {
			massnahmenImportProtokoll.reportDoppelteMassnahmenPaketId(massnahmenPaketId);
			return false;
		}

		if (((Geometry) simpleFeature.getDefaultGeometry()).getNumGeometries() == 0) {
			massnahmenImportProtokoll.reportLeererMultiLinestring(massnahmenPaketId);
			return false;
		}

		return true;
	}

	private Optional<MassnahmeNetzBezug> createNetzbezug(List<OsmMatchResult> matchResults) {
		Map<Long, Kante> kantenInMatching = new HashMap<>();

		List<List<MappedGrundnetzkante>> mappings = matchResults.stream()
			.map(osmMatchResult -> osmMatchResult.getOsmWayIds()
				.stream()
				.map(OsmWayId::getValue)
				.map(netzService::getKante)
				.peek(kante -> kantenInMatching.put(kante.getId(), kante))
				.filter(kante -> LineStrings.calculateUeberschneidungslinestring(kante.getGeometry(),
					osmMatchResult.getGeometrie()).isPresent())
				.map(kante -> new MappedGrundnetzkante(kante.getGeometry(), kante.getId(),
					osmMatchResult.getGeometrie()))
				.collect(Collectors.toList()))
			.collect(Collectors.toList());

		if (mappings.stream().anyMatch(List::isEmpty)) {
			return Optional.empty();
		}

		Set<AbschnittsweiserKantenSeitenBezug> seitenabschnittsKantenSeitenAbschnitte = mappings.stream()
			.flatMap(List::stream)
			.map(mappedGrundnetzkante -> new AbschnittsweiserKantenSeitenBezug(
				kantenInMatching.get(mappedGrundnetzkante.getKanteId()),
				mappedGrundnetzkante.getLinearReferenzierterAbschnitt(), Seitenbezug.BEIDSEITIG))
			.collect(Collectors.toSet());

		if (seitenabschnittsKantenSeitenAbschnitte.isEmpty()) {
			return Optional.empty();
		}

		return Optional.of(
			new MassnahmeNetzBezug(filtereUeberlappungen(seitenabschnittsKantenSeitenAbschnitte), Set.of(), Set.of()));
	}

	Set<AbschnittsweiserKantenSeitenBezug> filtereUeberlappungen(
		Set<AbschnittsweiserKantenSeitenBezug> seitenabschnittsKantenSeitenAbschnitte) {
		require(seitenabschnittsKantenSeitenAbschnitte.stream()
			.allMatch(abschnitt -> abschnitt.getSeitenbezug().equals(Seitenbezug.BEIDSEITIG)));
		return AbschnittsweiserKantenSeitenBezug.groupByKante(seitenabschnittsKantenSeitenAbschnitte).values()
			.stream().map(AbschnittsweiserKantenSeitenBezug::fasseUeberlappendeBezuegeZusammen).flatMap(List::stream)
			.collect(Collectors.toSet());
	}

	private List<OsmMatchResult> matchSimpleFeature(SimpleFeature simpleFeature,
		MatchingStatistik matchingStatistik) {

		Geometry geometry = (Geometry) simpleFeature.getDefaultGeometry();

		if (!geometry.getGeometryType().equals(TYPENAME_MULTILINESTRING)) {
			throw new RuntimeException("Unerwarteter Geometrytype: " + geometry.getGeometryType());
		}

		return simpleMatchingService.matchMultiLinestring((MultiLineString) geometry, matchingStatistik);
	}

	private Stream<File> getShpFiles() {
		File rootLevelDirectory = shpFilesRootFolderPath.toFile();
		if (!rootLevelDirectory.exists()) {
			throw new RuntimeException("rootLevelDirectory does not not exist");
		}

		File[] shpFiles = Objects.requireNonNull(
			rootLevelDirectory.listFiles(
				(dir, name) -> (name.contains("_Pkt") || name.contains("_Punkt") || name.contains("_Str"))
					&& name.endsWith(".shp")));

		return Arrays.stream(shpFiles);
	}
}
