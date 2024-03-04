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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.VernetzungKorrekturJobStatistik;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VernetzungKorrekturJob extends AbstractJob {
	/*
	Der hier verwendete Wert basiert auf dem Prod-Datenstand vom 16.02.23
	und ist so gewaehlt, dass bei allen zu diesem Zeitpunkt sinnvollen Stellen die Radvis Kante auf den naechsten
	Knoten snappt.
	 */
	public static final double TOLERANZ_RADVIS_KNOTEN = 18.0;

	public static final String JOB_NAME = "VernetzungKorrekturJob";

	private final EntityManager entityManager;
	private final KantenRepository kantenRepository;
	private final KnotenRepository knotenRepository;

	private final Envelope betrachteterExtent;
	private final int anzahlPartitionen;

	private final VernetzungService vernetzungService;
	private final NetzService netzService;

	public VernetzungKorrekturJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		KantenRepository kantenRepository, KnotenRepository knotenRepository, EntityManager entityManager,
		Envelope betrachteterExtent, double breitePartitionInM, VernetzungService vernetzungService,
		NetzService netzService) {
		super(jobExecutionDescriptionRepository);
		this.kantenRepository = kantenRepository;
		this.knotenRepository = knotenRepository;
		this.entityManager = entityManager;
		this.betrachteterExtent = betrachteterExtent;
		this.anzahlPartitionen = (int) Math.round(
			Math.ceil((betrachteterExtent.getMaxX() - betrachteterExtent.getMinX()) / breitePartitionInM));
		this.vernetzungService = vernetzungService;
		this.netzService = netzService;
	}

	@Override
	public String getName() {
		return VernetzungKorrekturJob.JOB_NAME;
	}

	@Override
	@Transactional
	@SuppressChangedEvents
	@WithAuditing(context = AuditingContext.VERNETZUNG_KORREKTUR_JOB)
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		log.info("Konfiguration: [TOLERANZ_RADVIS_KNOTEN=" + TOLERANZ_RADVIS_KNOTEN + ", betrachteterExtent="
			+ betrachteterExtent + ", anzahlPartitionen=" + anzahlPartitionen + "]");
		VernetzungKorrekturJobStatistik vernetzungKorrekturJobStatistik = new VernetzungKorrekturJobStatistik();

		logHeuristikenVorNetzkorrektur(vernetzungKorrekturJobStatistik);

		log.info("--------------------------- korrigiereDlmVernetzung -------------------------------");
		vernetzungKorrekturJobStatistik.anzahlDlmKantenVernetzungKorrigiert = korrigiereDlmVernetzung();
		entityManager.flush();
		entityManager.clear();

		log.info("--------------------------- entferneFalscheVernetzung -------------------------------");
		entferneFalscheVernetzung(vernetzungKorrekturJobStatistik);
		entityManager.flush();
		entityManager.clear();

		log.info("--------------------------- mergeDuplicateKnoten -------------------------------");
		mergeDuplicateKnoten(vernetzungKorrekturJobStatistik);
		entityManager.flush();
		entityManager.clear();

		log.info("--------------------------- korrigiereDlmKnotenGeometrie -------------------------------");
		vernetzungKorrekturJobStatistik.anzahlKorrigiereDlmKnotenGeometrieRowsAffected = korrigiereDlmKnotenGeometrie();
		log.info("korrigiereDlmKnotenGeometrie rows affected: "
			+ vernetzungKorrekturJobStatistik.anzahlKorrigiereDlmKnotenGeometrieRowsAffected);
		entityManager.flush();
		entityManager.clear();

		log.info("--------------------------- deleteVerwaisteDLMKnoten -------------------------------");
		vernetzungKorrekturJobStatistik.anzahlKnotenGeloescht += netzService
			.deleteVerwaisteDLMKnoten(NetzAenderungAusloeser.VERNETZUNG_KORREKTUR);
		logHeuristikOhneStatistikSchreiben();

		log.info("--------------------------- vernetzeAlleRadvisKantenNeu -------------------------------");
		vernetzungService.vernetzeAlleRadvisKantenNeu(
			vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik, TOLERANZ_RADVIS_KNOTEN,
			NetzAenderungAusloeser.VERNETZUNG_KORREKTUR);
		vernetzungKorrekturJobStatistik.anzahlKnotenGeloescht += vernetzungKorrekturJobStatistik.radvisKantenVernetzungStatistik.anzahlKnotenGeloescht;
		entityManager.flush();
		entityManager.clear();

		logHeuristikNachNetzkorrektur(vernetzungKorrekturJobStatistik);
		vernetzungKorrekturJobStatistik.anzahlVernetzungsfehlerNachJobausfuehrung = netzService.countAndLogVernetzungFehlerhaft();

		if (vernetzungKorrekturJobStatistik.anzahlVernetzungsfehlerNachJobausfuehrung > 0) {
			throw new RuntimeException(
				"Es liegen am Ende des Vernetzungskorrekturjobs noch Fehler vor -> Rollback....\n"
					+ vernetzungKorrekturJobStatistik.toString());
		}

		log.info("\nJobStatistik: " + vernetzungKorrekturJobStatistik);
		return Optional.of(vernetzungKorrekturJobStatistik);
	}

	protected void entferneFalscheVernetzung(VernetzungKorrekturJobStatistik statistik) {
		List<Knoten> knotenMitAuseinanderLiegendenKantenEnden = getKnotenMitAuseinanderLiegendenKantenEnden();

		statistik.anzahlKnotenMitAuseinanderLiegendenKantenEndenVorEntfernungFalscherVernetzung = knotenMitAuseinanderLiegendenKantenEnden.size();

		log.info("anzahlKnotenMitAuseinanderLiegendenKantenEndenVorEntfernungFalscherVernetzung: {} \n IDs: {}",
			statistik.anzahlKnotenMitAuseinanderLiegendenKantenEndenVorEntfernungFalscherVernetzung,
			knotenMitAuseinanderLiegendenKantenEnden.stream()
				.map(Knoten::getId)
				.map(Object::toString)
				.collect(Collectors.joining(",")));
		List<Long> kantenIdsFalscheVernetzungEntfernt = new ArrayList<>();

		knotenMitAuseinanderLiegendenKantenEnden.forEach(knoten -> {
			log.info("aktueller Knoten: " + knoten.getId());
			KnotenIndex knotenIndex = new KnotenIndex();
			List<Kante> adjazenteKanten = kantenRepository.getAdjazenteKanten(knoten);
			knotenIndex.fuegeEin(knoten);
			Optional<Point> nearestLineEnding = adjazenteKanten
				.stream()
				.map(kante -> kante.getVonKnoten().equals(knoten) ?
					kante.getGeometry().getStartPoint() :
					kante.getGeometry().getEndPoint())
				.min(Comparator.comparing(point -> point.distance(knoten.getPoint())));
			nearestLineEnding.ifPresent(knoten::updatePoint);
			knotenIndex.fuegeEin(knoten);
			adjazenteKanten
				.forEach(kante -> {
					log.info("aktuelle adjazenteKante: " + kante.getId());
					boolean isVonKnoten = kante.getVonKnoten().equals(knoten);
					Point lineEnding = isVonKnoten ?
						kante.getGeometry().getStartPoint() :
						kante.getGeometry().getEndPoint();
					Point otherLineEnding = isVonKnoten ?
						kante.getGeometry().getEndPoint() :
						kante.getGeometry().getStartPoint();
					Knoten otherKnoten = isVonKnoten ? kante.getNachKnoten() :
						kante.getVonKnoten();
					if (otherKnoten.getPoint().distance(otherLineEnding) > KnotenIndex.SNAPPING_DISTANCE) {
						otherKnoten.updatePoint(otherLineEnding);
						statistik.anzahlKnotenGeometrieDurchEntferneFalscheVernetzungKorrigiert++;
					}
					Optional<Knoten> existingKnoten = knotenIndex.finde(lineEnding);
					existingKnoten.ifPresentOrElse(
						k ->
							kante.updateTopologie(isVonKnoten ? k : kante.getVonKnoten(),
								isVonKnoten ? kante.getNachKnoten() : k),
						() -> {
							Knoten neu = new Knoten(lineEnding, QuellSystem.DLM);
							statistik.anzahlKnotenNeuAngelegt++;
							kante.updateTopologie(
								isVonKnoten ?
									neu :
									kante.getVonKnoten(),
								isVonKnoten ?
									kante.getNachKnoten() :
									neu
							);
							knotenIndex.fuegeEin(neu);
							knotenRepository.save(neu);
						}
					);

					if ((isVonKnoten && !kante.getVonKnoten().equals(knoten)) ||
						(!isVonKnoten && !kante.getNachKnoten().equals(knoten))) {
						statistik.anzahlFalscheVernetzungEntfernt++;
						kantenIdsFalscheVernetzungEntfernt.add(kante.getId());
					}
				});
		});
		log.info("anzahlFalscheVernetzungEntfernt: {} \n IDs: {}", statistik.anzahlFalscheVernetzungEntfernt,
			kantenIdsFalscheVernetzungEntfernt.stream()
				.map(Object::toString)
				.collect(Collectors.joining(","))
		);
	}

	protected void mergeDuplicateKnoten(VernetzungKorrekturJobStatistik vernetzungKorrekturJobStatistik) {
		Map<Integer, List<Long>> knotenCluster = getKnotenCluster();
		log.info("Es wurden {} Knoten-Cluster gefunden.\n Cluster: {}", knotenCluster.size(), knotenCluster);
		vernetzungKorrekturJobStatistik.anzahlKnotenCluster = knotenCluster.size();

		knotenCluster.forEach((cid, ids) -> {
			log.info("Cluster mit Knoten-IDs ({}) wird bearbeitet", ids.stream()
				.map(Object::toString).collect(Collectors.joining(",")));
			Knoten knotenBest = ids.stream().map(knotenRepository::findById)
				.map(Optional::orElseThrow)
				.max(Comparator.comparing(knoten -> kantenRepository.getAdjazenteKanten(knoten).size()))
				.orElseThrow();

			log.info("Knoten mit den meisten adjazenten Kanten: {}", knotenBest);

			ids.stream().map(knotenRepository::findById)
				.map(Optional::orElseThrow)
				.filter(knoten -> !knoten.equals(knotenBest))
				.forEach(knoten -> {
					List<Kante> adjazenteKanten = kantenRepository.getAdjazenteKanten(knoten);
					List<Kante> kantenDieZumLoopWerden = adjazenteKanten.stream()
						.filter(kante ->
							(kante.getVonKnoten().equals(knoten) && kante.getNachKnoten().equals(knotenBest))
								|| (kante.getNachKnoten().equals(knoten) && kante.getVonKnoten().equals(knotenBest)))
						.collect(Collectors.toList());

					if (!kantenDieZumLoopWerden.isEmpty()) {
						vernetzungKorrekturJobStatistik.anzahlKnotenKonntenNichtGemergedWerden++;
						log.info("Knoten {} konnte nicht auf Knoten {} übetragen werden, wegen Kanten ({})",
							knoten,
							knotenBest,
							kantenDieZumLoopWerden.stream()
								.map(Kante::getId)
								.map(Object::toString)
								.collect(Collectors.joining(",")));
						return;
					}
					adjazenteKanten.forEach(kante -> {
						log.info("Bearbeite Kante {}", kante.getId());
						vernetzungKorrekturJobStatistik.anzahlDurchMergeKnotenKorrigierteKanten++;
						if (kante.getVonKnoten().equals(knoten)) {
							log.info("Merge VonKnoten {} auf 'besten' Knoten {}", kante.getVonKnoten(), knotenBest);
							kante.updateTopologie(knotenBest, kante.getNachKnoten());
						} else {
							log.info("Merge NachKnoten {} auf 'besten' Knoten {}", kante.getNachKnoten(),
								knotenBest);
							kante.updateTopologie(kante.getVonKnoten(), knotenBest);
						}
					});
				});
		});
	}

	private int korrigiereDlmVernetzung() {
		String sqlStringBuilder = "UPDATE kante "
			+ "SET von_knoten_id = "
			+ "  (SELECT id FROM knoten WHERE knoten.quelle = 'DLM' "
			+ "    ORDER BY knoten.point <-> st_startpoint(kante.geometry) LIMIT 1),"
			+ "nach_knoten_id = "
			+ "  (SELECT id FROM knoten WHERE knoten.quelle = 'DLM' "
			+ "    ORDER BY knoten.point <-> st_endpoint(kante.geometry) LIMIT 1)"
			+ "WHERE kante.quelle = 'DLM' "
			+ "AND "
			+ "(NOT von_knoten_id = "
			+ "  (SELECT id FROM knoten WHERE knoten.quelle = 'DLM' "
			+ "    ORDER BY knoten.point <-> st_startpoint(kante.geometry) LIMIT 1) "
			+ "  OR NOT nach_knoten_id = "
			+ "  (SELECT id FROM knoten WHERE knoten.quelle = 'DLM' "
			+ "    ORDER BY knoten.point <-> st_endpoint(kante.geometry) LIMIT 1)"
			+ ");";
		return entityManager.createNativeQuery(sqlStringBuilder).executeUpdate();
	}

	private int korrigiereDlmKnotenGeometrie() {
		String sqlString = "UPDATE knoten "
			+ "SET point = COALESCE("
			+ "  (SELECT st_centroid(st_collect( "
			+ "    st_collect(st_endpoint(kante_nach.geometry)), "
			+ "    st_collect(st_startpoint(kante_von.geometry))"
			+ "    ))"
			+ "  FROM knoten k2 "
			+ "  LEFT JOIN kante kante_nach ON knoten.id = kante_nach.nach_knoten_id AND kante_nach.quelle = 'DLM' "
			+ "  LEFT JOIN kante kante_von ON knoten.id = kante_von.von_knoten_id AND kante_von.quelle = 'DLM' "
			+ "  WHERE k2.id = knoten.id AND k2.quelle = 'DLM' "
			+ "  GROUP BY k2.id )"
			+ "  , point) "
			+ "WHERE knoten.quelle = 'DLM';";
		return entityManager.createNativeQuery(sqlString).executeUpdate();
	}

	@SuppressWarnings("unchecked")
	private List<Knoten> getKnotenMitAuseinanderLiegendenKantenEnden() {
		String sqlString = "SELECT knoten.* "
			+ "FROM knoten "
			+ "    LEFT JOIN kante kante_nach ON knoten.id = kante_nach.nach_knoten_id AND kante_nach.quelle = 'DLM' "
			+ "    LEFT JOIN kante kante_von ON knoten.id = kante_von.von_knoten_id AND kante_von.quelle = 'DLM' "
			+ "WHERE knoten.quelle = 'DLM' "
			+ "GROUP BY knoten.id "
			+ "HAVING st_maxdistance( "
			+ "    st_collect( st_collect(st_endpoint(kante_nach.geometry)), st_collect(st_startpoint(kante_von.geometry))), "
			+ "     st_collect( st_collect(st_endpoint(kante_nach.geometry)), st_collect(st_startpoint(kante_von.geometry))) "
			+ ") > :snappingDistance ;";
		return (List<Knoten>) entityManager.createNativeQuery(sqlString, Knoten.class)
			.setParameter("snappingDistance", KnotenIndex.SNAPPING_DISTANCE)
			.getResultList();
	}

	@SuppressWarnings("unchecked")
	private Map<Integer, List<Long>> getKnotenCluster() {
		String sqlString = "SELECT id, cid FROM ("
			+ "SELECT id, ST_ClusterDBSCAN(point, :snappingdistance, 2) over () AS cid "
			+ "FROM knoten "
			+ "WHERE quelle = 'DLM') clustered "
			+ "WHERE cid IS NOT NULL ";
		return (Map<Integer, List<Long>>) entityManager.createNativeQuery(sqlString, Tuple.class)
			.setParameter("snappingdistance", KnotenIndex.SNAPPING_DISTANCE)
			.getResultStream()
			.collect(Collectors.groupingBy(
				(Tuple tuple) -> (Integer) tuple.get("cid"),
				HashMap::new,
				Collectors.collectingAndThen(
					Collectors.toList(),
					(List<Tuple> tuples) ->
						tuples.stream()
							.map((Tuple tuple) -> tuple.get("id"))
							.collect(Collectors.toList()))
			));
	}

	@SuppressWarnings("unchecked")
	private List<Long> findKomplettVerwaisteKnoten() {
		String sqlString = "SELECT k.id FROM knoten k WHERE NOT EXISTS"
			+ " (SELECT id FROM kante WHERE (quelle = 'DLM' OR quelle = 'RadVis')"
			+ " AND (von_knoten_id = k.id OR nach_knoten_id = k.id))"
			+ " AND k.quelle = 'DLM';";
		return (List<Long>) entityManager.createNativeQuery(sqlString).getResultStream()
			.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private List<Long> findVonDlmKantenVerwaisteKnoten() {
		String sqlString = "SELECT k.id FROM knoten k WHERE NOT EXISTS"
			+ " (SELECT id FROM kante WHERE (quelle = 'DLM')"
			+ " AND (von_knoten_id = k.id OR nach_knoten_id = k.id))"
			+ " AND k.quelle = 'DLM';";
		return (List<Long>) entityManager.createNativeQuery(sqlString).getResultStream()
			.collect(Collectors.toList());
	}

	@SuppressWarnings("unchecked")
	private List<Long> findKnotenMitNurRadvisKanten() {
		String sqlString = "SELECT k.id FROM knoten k WHERE\n"
			+ "    NOT EXISTS(SELECT id FROM kante WHERE (quelle = 'DLM')\n"
			+ "            AND (von_knoten_id = k.id OR nach_knoten_id = k.id))\n"
			+ "    AND EXISTS(SELECT id FROM kante WHERE (quelle = 'RadVis')\n"
			+ "            AND (von_knoten_id = k.id OR nach_knoten_id = k.id))\n"
			+ "    AND k.quelle = 'DLM';";
		return (List<Long>) entityManager.createNativeQuery(sqlString).getResultStream()
			.collect(Collectors.toList());
	}

	private void logHeuristikOhneStatistikSchreiben() {
		log.info("Von Dlm-Kanten verwaiste Knoten: " + findVonDlmKantenVerwaisteKnoten());
		log.info("Komplett verwaiste Knoten: " + findKomplettVerwaisteKnoten());
		log.info("Knoten an denen nur RadVis Kanten liegen: " + findKnotenMitNurRadvisKanten());
	}

	private void logHeuristikenVorNetzkorrektur(VernetzungKorrekturJobStatistik vernetzungKorrekturJobStatistik) {
		vernetzungKorrekturJobStatistik.anzahlVernetzungsfehlerVorJobausfuehrung = netzService.countAndLogVernetzungFehlerhaft();
		List<Long> vonDlmKantenVerwaisteKnotenVorher = findVonDlmKantenVerwaisteKnoten();
		List<Long> komplettVerwaisteKnotenVorher = findKomplettVerwaisteKnoten();
		List<Long> knotenMitNurRadvisKantenVorher = findKnotenMitNurRadvisKanten();
		vernetzungKorrekturJobStatistik.anzahlVonDlmKantenVerwaisteKnotenVorher = vonDlmKantenVerwaisteKnotenVorher.size();
		vernetzungKorrekturJobStatistik.anzahlKomplettVerwaisterKnotenVorher = komplettVerwaisteKnotenVorher.size();
		vernetzungKorrekturJobStatistik.anzahlKnotenMitNurRadvisKantenVorher = knotenMitNurRadvisKantenVorher.size();
		log.info("Von Dlm-Kanten verwaiste Knoten: " + vonDlmKantenVerwaisteKnotenVorher);
		log.info("Komplett verwaiste Knoten: " + komplettVerwaisteKnotenVorher);
		log.info("Knoten an denen nur RadVis Kanten liegen Vorher: " + knotenMitNurRadvisKantenVorher);
	}

	private void logHeuristikNachNetzkorrektur(VernetzungKorrekturJobStatistik vernetzungKorrekturJobStatistik) {
		List<Long> vonDlmKantenVerwaisteKnotenNachher = findVonDlmKantenVerwaisteKnoten();
		List<Long> komplettVerwaisteKnotenNachher = findKomplettVerwaisteKnoten();
		List<Long> knotenMitNurRadvisKantenNachher = findKnotenMitNurRadvisKanten();
		vernetzungKorrekturJobStatistik.anzahlVonDlmKantenVerwaisteKnotenNachher = vonDlmKantenVerwaisteKnotenNachher.size();
		vernetzungKorrekturJobStatistik.anzahlKomplettVerwaisterKnotenNachher = komplettVerwaisteKnotenNachher.size();
		vernetzungKorrekturJobStatistik.anzahlKnotenMitNurRadvisKantenNachher = knotenMitNurRadvisKantenNachher.size();
		log.info("Von Dlm-Kanten verwaiste Knoten: " + vonDlmKantenVerwaisteKnotenNachher);
		log.info("Komplett verwaiste Knoten: " + komplettVerwaisteKnotenNachher);
		log.info("Knoten an denen nur RadVis Kanten liegen Nachher: " + knotenMitNurRadvisKantenNachher);
	}
}
