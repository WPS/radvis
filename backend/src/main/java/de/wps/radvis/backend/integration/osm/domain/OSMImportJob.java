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

package de.wps.radvis.backend.integration.osm.domain;

import static org.valid4j.Assertive.require;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StopWatch;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmTag;
import de.topobyte.osm4j.core.model.impl.Node;
import de.topobyte.osm4j.core.model.impl.Way;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.common.domain.CoordinateReferenceSystemConverterUtility;
import de.wps.radvis.backend.common.domain.JobDescription;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.JobExecutionDurationEstimate;
import de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.valueObject.BasisnetzImportSource;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.osm.domain.entity.OSMImportStatistik;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Manueller Import-Job um das OSM-Netz einer Region als Basisnetz zu importieren. Dieser Job ist NICHT dazu gedacht,
 * das Netz zu aktualisieren.
 * <p>
 * Anforderungen an OSM-PBF:
 * <p>
 * 1. Sie muss nach dem Standard sortiert sein, sprich: erst Nodes, dann Ways, dann Relationen. Bei Exporten der Geofabrik
 * bereits der Fall.
 * 2. IDs müssen gesetzt und gültig sein. Auch das ist bei Exporten der Geofabrik der Fall.
 * <p>
 * Um die Performance zu verbessern sind folgende Eigenschaften der PBF hilfreich:
 * <p>
 * 1. Es sind grob nur Ways vorhanden, die als Straßen importiert werden sollen. Also bspw. keine Gebäude. Beim Import
 * passiert zwar auch eine Filterung, eine Vorfilterung der Datei hilft aber bzgl. Geschwindigkeit und Speicherverbrauch.
 * 2. Alle Nodes sind Teil eines zu importierenden Weges. Also z.B. keine Sitzbänke neben der Straße o.Ä. Auch hier ist
 * es hauptsächlich hilfreich, um den Speicherverbrauch zu reduzieren.
 * <p>
 * Eine Vorfilterung kann mit folgendem osmium-Befehlen durchgeführt werden:
 * osmium tags-filter input.osm.pbf --overwrite -o _1.osm.pbf w/highway
 * osmium tags-filter _1.osm.pbf --overwrite -o _2.osm.pbf -i w/highway=motorway*
 * osmium tags-filter _2.osm.pbf --overwrite -o _3.osm.pbf -i w/highway=trunk*
 * osmium tags-filter _3.osm.pbf --overwrite -o _4.osm.pbf -i w/area=yes
 * osmium tags-filter _4.osm.pbf --overwrite -o osm-import.osm.pbf -i w/indoor=yes
 * <p>
 * Vorgang des Imports:
 * <p>
 * 1. Alle relevanten OSM-Nodes (Nodes auf Ways) in Knoten umwandeln.
 * 2. Dabei zu importierende Knoten ermitteln. Das sind alle Startpunkte von Ways, Endpunkte von Ways und
 * Kreuzungspunkte. Nodes dazwischen sind für unser Knoten-Kanten-Modell irrelevant und werden nicht als Knoten in der
 * DB gespeichert, sondern nur als Koordinaten innerhalb der LineString-Geometrie der Kante.
 * 3. In einem zweiten Durchlauf durch die Datei aus Ways Kanten bilden. Ein Way muss dabei ggf. aufgeteilt werden, wenn
 * neben Start- und Endpunkt noch Kreuzungspunkte auf dem Way existieren.
 * <p>
 * Hierbei wird die PBF-Datei zweimal eingelesen. Das ist nötig, weil man im ersten Durchlauf die relevanten Nodes
 * ermitteln kann und erst nach dem Durchlauf aller Ways sicher darauf Kanten bilden kann.
 * <p>
 * Performance Optimierungen:
 * <p>
 * 1. Verringerung der Datei durch Vorfilterung (s.o.)
 * 2. Nutzung von Sessions um Batching zu aktivieren, was bei normalem Hibernate über ein Repository nicht zu
 * funktionieren scheint.
 * 3. Setzen von spring.datasource.jpa.properties.hibernate.order_inserts=true in der application.yml um Inserts von
 * Knoten zu beschleunigen.
 * 4. Keine Nutzung von Hibernate bei Kanten. Stattdessen eigene prepared statements um das Bauen diverser Java-Objekte
 * und komplexes Speichern in die DB zu umgehen.
 * 5. Nutzung einer stored procedure um effizient die Inserts zu bauen, da Tabellen sich ja referenzieren. Ansonsten
 * braucht man sub-select statements um die IDs von Attributgruppen herauszubekommen, um diese IDs dann in der Kante-
 * Tabelle zu speichern.
 * 6. Entfernen/Deaktivieren aller relevanten Indices und Trigger. Bei jedem Insert würde sonst eine Reihe von
 * Überprüfungen und Updates losgetreten, die allesamt Zeit kosten und nicht nötig sind. Indizes werden entsprechend
 * beim Neuerzeugen ganz am Ende wieder aufgebaut.
 * 7. Auch bei Kanten wird Batching vorgenommen, hier dann auf Ebene der prepared statements.
 */
@Slf4j
@Transactional
public class OSMImportJob extends AbstractJob {
	private final File osmBasisnetzDaten;
	private final BasisnetzImportSource basisnetzImportSource;
	private final EntityManager entityManager;
	private final JdbcTemplate jdbcTemplate;

	private final NumberFormat numberFormat = NumberFormat.getInstance(Locale.GERMAN);

	public OSMImportJob(JobExecutionDescriptionRepository repository,
		File osmBasisnetzDaten,
		BasisnetzImportSource basisnetzImportSource,
		EntityManager entityManager,
		JdbcTemplate jdbcTemplate) {
		super(repository);
		this.osmBasisnetzDaten = osmBasisnetzDaten;
		this.basisnetzImportSource = basisnetzImportSource;
		this.entityManager = entityManager;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	@WithAuditing(context = AuditingContext.OSM_IMPORT_JOB)
	@SuppressChangedEvents
	public JobExecutionDescription run() {
		return super.run(false);
	}

	@Override
	@WithAuditing(context = AuditingContext.OSM_IMPORT_JOB)
	@SuppressChangedEvents
	public JobExecutionDescription run(boolean force) {
		return super.run(force);
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		require(basisnetzImportSource == BasisnetzImportSource.OSM);

		Session session = entityManager.unwrap(Session.class);
		session.setJdbcBatchSize(1000);
		session.setFlushMode(FlushModeType.COMMIT);

		OSMImportStatistik statistik = new OSMImportStatistik();

		// Mapping von OSM-Node zu RadVIS-Knoten.
		// Nicht alle diese Knoten werden in der DB gespeichert (s. Knotenerstellung weiter unten).
		HashMap<Long, Knoten> nodeIdToKnoten = new HashMap<>();

		// Node-IDs, die Grad 1 (Sackgasse) oder >2 (echte Kreuzung zwischen mehreren Ways) haben.
		// Also alle Nodes, die später tatsächlich zu Knoten werden sollen.
		Set<Long> nodeIdsForImportedKnoten = new HashSet<>();

		// Schritt 1: Erzeugen aller relevanten Knoten (Startpunkte, Endpunkte und Kreuzungspunkte).
		determineKnoten(statistik, nodeIdToKnoten, nodeIdsForImportedKnoten, session);

		// Schritt 2: Erzeugen aller Kanten zwischen den gefundenen Knoten.
		try {
			importKnotenAndKanten(statistik, nodeIdToKnoten, nodeIdsForImportedKnoten, session.getJdbcBatchSize());
		} catch (Exception e) {
			throw new RuntimeException("Import der Kanten fehlgeschlagen", e);
		}

		log.info("JobStatistik:\n{}", statistik.toPrettyJSON());

		return Optional.empty();
	}

	/**
	 * Füllt die übergebenen Datenstrukturen mit Leben. Das heißt es werden Knoten gebildet, deren Positionen gemerkt
	 * (weil OSM-Ways nur die Knoten-IDs aber keine Geometrien halten) und es werden Nodes mit Grad 1 (Sackgassen) oder
	 * >2 (echte Kreuzungen zwischen mehreren Ways) ermittelt, also alle Nodes, die später zu Knoten werden sollen.
	 */
	private void determineKnoten(OSMImportStatistik statistik, HashMap<Long, Knoten> nodeIdToKnoten,
		Set<Long> nodeIdsForImportedKnoten, Session session) {
		log.info("Erstelle Knoten aus OSM-Nodes");

		// Ways enthalten nur die IDs der Nodes aber nicht deren Position. Daher müssen wir uns - um unten Knoten bauen
		// zu können - die Positionen merken. Das Bauen der Knoten passiert erst beim Durchlaufen der Ways und nicht
		// schon beim Durchlaufen der Nodes. Dort bräuchte man diese Map nicht, aber später bei den Ways können
		// Kreuzungspunkte und Sackgassen ermittelt werden.
		HashMap<Long, Coordinate> nodeToLocation = new HashMap<>();

		try (InputStream input = new FileInputStream(osmBasisnetzDaten)) {
			OsmIterator iterator = new PbfIterator(input, false);

			for (EntityContainer container : iterator) {
				if (container.getType() == EntityType.Node) {
					statistik.anzahlNodesVerarbeitet++;
					Node osmNode = (Node) container.getEntity();
					nodeToLocation.put(osmNode.getId(), new Coordinate(osmNode.getLatitude(), osmNode.getLongitude()));
				} else if (container.getType() == EntityType.Way) {
					if (statistik.anzahlWaysGesamt == 0) {
						log.info("Starte Verarbeitung der OSM-Ways");
					}

					Way osmWay = (Way) container.getEntity();
					statistik.anzahlWaysGesamt++;

					if (!shouldImportWay(osmWay)) {
						continue;
					}

					long[] nodeIdArray = osmWay.getNodes().toArray();
					for (int i = 0; i < nodeIdArray.length; i++) {
						long nodeId = nodeIdArray[i];
						boolean hasBeenVisitedBefore = nodeIdToKnoten.containsKey(nodeId);
						if (!hasBeenVisitedBefore) {
							// Node wurde zum ersten Mal besucht, also wird ein Knoten erstellt. Ob dieser Knoten später
							// auch in der DB landet, wird im "if" unten ermittelt. Es gibt dann also Knoten, die hier
							// erzeugt und nie gebraucht werden. Für Speicherverbrauch und Performance ist das ziemlich
							// egal, da Knoten recht klein sind.

							Point point = (Point) CoordinateReferenceSystemConverterUtility.transformGeometry(
								KoordinatenReferenzSystem.WGS84.getGeometryFactory()
									.createPoint(nodeToLocation.get(nodeId)),
								KoordinatenReferenzSystem.ETRS89_UTM32_N
							);
							// Wir importieren OSM-Daten als DLM-Kanten, damit alle Jobs, Services, Views, etc., die
							// auf das Quellsystem prüfen, wie gewohnt funktionieren.
							Knoten knoten = Knoten.builder()
								.point(point)
								.quelle(QuellSystem.DLM)
								.build();
							nodeIdToKnoten.put(nodeId, knoten);
							nodeToLocation.remove(nodeId);
						}

						boolean isBeginningOrEndOfWay = i == 0 || i == nodeIdArray.length - 1;
						if (hasBeenVisitedBefore || isBeginningOrEndOfWay) {
							// Wenn Node bereits in Set war, dann haben wir hier den Node zum wiederholten Male gefunden,
							// also handelt es sich um einen Kreuzungspunkt. Auch Start- bzw. Endpunkte von Ways wollen
							// wir mir einem Knoten versehen, daher merken wir uns hier diese Node-IDs ebenfalls.
							if (!nodeIdsForImportedKnoten.contains(nodeId)) {
								nodeIdsForImportedKnoten.add(nodeId);
								session.persist(nodeIdToKnoten.get(nodeId));
								statistik.anzahlKnotenImportiert++;
							}
						}
					}
				} else if (container.getType() == EntityType.Relation) {
					// PBF-Dateien sind sortiert, Relationen kommen zum Schluss. Daher können wir hier aufhören.
					break;
				}

				statistik.anzahlOsmObjekteGesamt++;
				if (statistik.anzahlOsmObjekteGesamt % 1_000_000 == 0) {
					session.flush();
					session.clear();
					log.info(
						"Knoten-Import: {} OSM Objekte verarbeitet, davon {} Nodes wovon {} Knoten importiert wurden",
						numberFormat.format(statistik.anzahlOsmObjekteGesamt),
						numberFormat.format(statistik.anzahlNodesVerarbeitet),
						numberFormat.format(statistik.anzahlKnotenImportiert)
					);
				}
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		session.flush();
		session.clear();

		log.info(
			"Knoten-Import abgeschlossen: {} OSM Objekte verarbeitet, davon {} Ways und {} Nodes wovon {} Knoten importiert wurden",
			numberFormat.format(statistik.anzahlOsmObjekteGesamt),
			numberFormat.format(statistik.anzahlWaysGesamt),
			numberFormat.format(statistik.anzahlNodesVerarbeitet),
			numberFormat.format(nodeIdsForImportedKnoten.size())
		);
	}

	/**
	 * Erstellt Kanten aus OSM-Ways und speichert diese samt der Knoten und Attributen ab.
	 */
	private void importKnotenAndKanten(OSMImportStatistik statistik, HashMap<Long, Knoten> nodeIdToKnoten,
		Set<Long> nodeIdsForImportedKnoten, int batchSize) throws SQLException {
		log.info("Importiere OSM-Ways als Kanten");
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();

		PreparedStatement preparedStatement = jdbcTemplate.getDataSource().getConnection().prepareStatement(
			"CALL add_kante(?, ?, ?, ?, ?, ?, ?, ?);");

		dropIndicesAndDeactivateTrigger();
		createKanteCreationProcedure();

		try (InputStream input = new FileInputStream(osmBasisnetzDaten)) {
			OsmIterator iterator = new PbfIterator(input, false);

			for (EntityContainer container : iterator) {
				if (container.getType() == EntityType.Way) {
					statistik.anzahlWaysVerarbeitet++;
					Way osmWay = (Way) container.getEntity();

					if (!shouldImportWay(osmWay)) {
						continue;
					}

					long[] nodeIdArray = osmWay.getNodes().toArray();
					int lastVonNodeIndex = 0;
					for (int i = 0; i < nodeIdArray.length; i++) {
						long nodeIdCurrentNode = nodeIdArray[i];
						if (!nodeIdsForImportedKnoten.contains(nodeIdCurrentNode)) {
							continue;
						}
						long nodeIdLastVonKnoten = nodeIdArray[lastVonNodeIndex];

						Knoten vonKnoten = nodeIdToKnoten.get(nodeIdLastVonKnoten);
						Knoten nachKnoten = nodeIdToKnoten.get(nodeIdCurrentNode);

						if (nodeIdLastVonKnoten == nodeIdCurrentNode || vonKnoten.getKoordinate().equals(
							nachKnoten.getKoordinate())) {
							// Kreise werden momentan ignoriert, da es ein require() bei Kanten gibt. Dies tritt meistens
							// bei Wendekreisen und parkplätzen o.Ä. auf (letztere importieren wir eh nicht), ist also
							// eher die Ausnahme und betrifft weniger wichtige Abschnitte. Daher erst mal keine
							// Sonderbehandlung und Auflösung der Kanten.
							lastVonNodeIndex = i;
							continue;
						}

						LineString lineString = getLineStringBetweenNodes(lastVonNodeIndex, i, nodeIdToKnoten,
							nodeIdArray);

						String strassenName = null;
						String strassenNummer = null;
						String beleuchtung = Beleuchtung.UNBEKANNT.name();
						String belagArt = BelagArt.UNBEKANNT.name();
						for (OsmTag tag : osmWay.getTags()) {
							if ("name".equals(tag.getKey())) {
								strassenName = tag.getValue();
							} else if ("ref".equals(tag.getKey())) {
								strassenNummer = tag.getValue();
							} else if ("lit".equals(tag.getKey()) && "yes".equals(tag.getValue())) {
								beleuchtung = Beleuchtung.VORHANDEN.name();
							} else if ("surface".equals(tag.getKey())) {
								belagArt = getBelagArtFromOsmTag(tag);
							}
						}

						preparedStatement.setLong(1, vonKnoten.getId());
						preparedStatement.setLong(2, nachKnoten.getId());
						preparedStatement.setString(3, "SRID=25832; " + lineString.toText());
						preparedStatement.setInt(4, (int) Math.round(lineString.getLength() * 100));
						preparedStatement.setString(5, strassenName);
						preparedStatement.setString(6, strassenNummer);
						preparedStatement.setString(7, beleuchtung);
						preparedStatement.setString(8, belagArt);
						preparedStatement.addBatch();

						statistik.anzahlKantenImportiert++;

						if (statistik.anzahlKantenImportiert % batchSize == 0) {
							int[] results = preparedStatement.executeBatch();
							if (Arrays.stream(results).anyMatch(result -> result < 0)) {
								throw new RuntimeException("Ausführen von Batch fehlgeschlagen");
							}
						}

						if (statistik.anzahlKantenImportiert % 100_000 == 0) {
							stopWatch.stop();
							stopWatch.start();
							float msPerWay = stopWatch.getTotalTimeMillis() / (float) statistik.anzahlWaysVerarbeitet;
							int numberWaysToGo = statistik.anzahlWaysGesamt - statistik.anzahlWaysVerarbeitet;
							log.info(
								"Kanten-Import: {} Ways (von {}) wovon {} in Form von {} Kanten importiert wurden ({} ms/way; fertig in ca. {} s)",
								numberFormat.format(statistik.anzahlWaysVerarbeitet),
								numberFormat.format(statistik.anzahlWaysGesamt),
								numberFormat.format(statistik.anzahlWaysImportiert),
								numberFormat.format(statistik.anzahlKantenImportiert),
								msPerWay, (numberWaysToGo * msPerWay) / 1000f);
						}

						lastVonNodeIndex = i;
					}

					statistik.anzahlWaysImportiert++;
				} else if (container.getType() == EntityType.Relation) {
					// PBF-Dateien sind sortiert, Relationen kommen zum Schluss. Daher können wir hier aufhören.
					break;
				}
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		int[] results = preparedStatement.executeBatch();
		if (Arrays.stream(results).anyMatch(result -> result < 0)) {
			throw new RuntimeException("Ausführen von Batch fehlgeschlagen");
		}
		createIndicesAndEnableTrigger();

		stopWatch.stop();
		float msPerWay = stopWatch.getTotalTimeMillis() / (float) statistik.anzahlWaysVerarbeitet;
		log.info(
			"OSM-Import abgeschlossen: {} OSM-Objekte insgesamt verarbeitet, davon {} Ways in Form von {} Kanten importiert ({} s gesamt, {} ms/way)",
			numberFormat.format(statistik.anzahlOsmObjekteGesamt),
			numberFormat.format(statistik.anzahlWaysVerarbeitet),
			numberFormat.format(statistik.anzahlKantenImportiert),
			stopWatch.getTotalTimeSeconds(),
			msPerWay
		);
	}

	private void createKanteCreationProcedure() throws SQLException {
		jdbcTemplate.getDataSource().getConnection().createStatement().execute(
			"""
				CREATE OR REPLACE PROCEDURE add_kante(
					von_knoten_id INT8,
					nach_knoten_id INT8,
					geometry TEXT,
					laenge_in_cm INT4,
					strassenname TEXT,
					strassennummer TEXT,
					beleuchtung TEXT,
					belagArt TEXT
				) AS $$
				DECLARE
					zustaendigkeit_attribut_gruppe_id INT8;
					geschwindigkeit_attribut_gruppe_id INT8;
					fahrtrichtung_attribut_gruppe_id INT8;
					fuehrungsform_attribut_gruppe_id INT8;
					kanten_attribut_gruppe_id INT8;
				BEGIN
					zustaendigkeit_attribut_gruppe_id := nextval('hibernate_sequence');
					geschwindigkeit_attribut_gruppe_id := nextval('hibernate_sequence');
					fahrtrichtung_attribut_gruppe_id := nextval('hibernate_sequence');
					fuehrungsform_attribut_gruppe_id := nextval('hibernate_sequence');
					kanten_attribut_gruppe_id := nextval('hibernate_sequence');

					INSERT INTO zustaendigkeit_attribut_gruppe (id, version)
					VALUES(zustaendigkeit_attribut_gruppe_id, 0);

					INSERT INTO geschwindigkeit_attribut_gruppe (id, version)
					VALUES(geschwindigkeit_attribut_gruppe_id, 0);

					INSERT INTO fahrtrichtung_attribut_gruppe (id, "version", fahrtrichtung_links, fahrtrichtung_rechts, is_zweiseitig)
					VALUES(fahrtrichtung_attribut_gruppe_id, 0, 'UNBEKANNT', 'UNBEKANNT', false);

					INSERT INTO fuehrungsform_attribut_gruppe (id, "version", is_zweiseitig)
					VALUES(fuehrungsform_attribut_gruppe_id, 0, false);

					INSERT INTO kanten_attribut_gruppe (id, "version", dtv_fussverkehr, dtv_pkw, dtv_radverkehr, kommentar, laenge_manuell_erfasst, strassen_name, strassen_nummer, sv, vereinbarungs_kennung, wege_niveau, gemeinde_id, beleuchtung, strassenquerschnittrast06, umfeld, status, strassenkategorierin)
					VALUES(kanten_attribut_gruppe_id, 0, NULL, NULL, NULL, NULL, NULL, strassenname, strassennummer, NULL, NULL, NULL, NULL, beleuchtung, 'UNBEKANNT', 'UNBEKANNT', 'UNTER_VERKEHR', NULL);

					INSERT INTO zustaendigkeit_attribut_gruppe_zustaendigkeit_attribute
						(id, zustaendigkeit_attribut_gruppe_id, von, bis, vereinbarungs_kennung, baulast_traeger_id, unterhalts_zustaendiger_id, erhalts_zustaendiger_id)
						VALUES(NULL, zustaendigkeit_attribut_gruppe_id, 0.0, 1.0, NULL, NULL, NULL, NULL);

					INSERT INTO geschwindigkeit_attribut_gruppe_geschwindigkeit_attribute
						(geschwindigkeit_attribut_gruppe_id, von, bis, ortslage, hoechstgeschwindigkeit, abweichende_hoechstgeschwindigkeit_gegen_stationierungsrichtung)
						VALUES(geschwindigkeit_attribut_gruppe_id, 0.0, 1.0, NULL, 'UNBEKANNT', NULL);

					INSERT INTO fuehrungsform_attribut_gruppe_attribute_links
						(id, fuehrungsform_attribut_gruppe_id, von, bis, radverkehrsfuehrung, breite, parken_typ, parken_form, bordstein, belag_art, oberflaechenbeschaffenheit, benutzungspflicht, trennstreifen_breite_rechts, trennstreifen_breite_links, trennstreifen_trennung_zu_rechts, trennstreifen_trennung_zu_links, trennstreifen_form_rechts, trennstreifen_form_links)
						VALUES(NULL, fuehrungsform_attribut_gruppe_id, 0.0, 1.0, 'UNBEKANNT', NULL, 'UNBEKANNT', 'UNBEKANNT', 'UNBEKANNT', belagArt, 'UNBEKANNT', 'UNBEKANNT', NULL, NULL, NULL, NULL, NULL, NULL);

					INSERT INTO fuehrungsform_attribut_gruppe_attribute_rechts
						(id, fuehrungsform_attribut_gruppe_id, von, bis, radverkehrsfuehrung, breite, parken_typ, parken_form, bordstein, belag_art, oberflaechenbeschaffenheit, benutzungspflicht, trennstreifen_breite_rechts, trennstreifen_breite_links, trennstreifen_trennung_zu_rechts, trennstreifen_trennung_zu_links, trennstreifen_form_rechts, trennstreifen_form_links)
						VALUES(NULL, fuehrungsform_attribut_gruppe_id, 0.0, 1.0, 'UNBEKANNT', NULL, 'UNBEKANNT', 'UNBEKANNT', 'UNBEKANNT', belagArt, 'UNBEKANNT', 'UNBEKANNT', NULL, NULL, NULL, NULL, NULL, NULL);

					INSERT INTO kante
						(id, auf_dlm_abgebildete_geometry, geometry, quelle, von_knoten_id, nach_knoten_id, "version", dlm_id, verlauf_links, verlauf_rechts, zustaendigkeit_attributgruppe_id, geschwindigkeit_attributgruppe_id, fahrtrichtung_attributgruppe_id, ursprungsfeature_technischeid, fuehrungsform_attribut_gruppe_id, kanten_attributgruppe_id, is_zweiseitig, kanten_laenge_in_cm, is_grundnetz, geometry3d)
						VALUES(
							nextval('hibernate_sequence'),
							NULL,
							geometry::geometry,
							'OSM',
							von_knoten_id,
							nach_knoten_id,
							0,
							NULL,
							NULL,
							NULL,
							zustaendigkeit_attribut_gruppe_id,
							geschwindigkeit_attribut_gruppe_id,
							fahrtrichtung_attribut_gruppe_id,
							NULL,
							fuehrungsform_attribut_gruppe_id,
							kanten_attribut_gruppe_id,
							false,
							laenge_in_cm,
							true,
							NULL
						);
				END;
				$$ LANGUAGE plpgsql;
				""");
	}

	private void dropIndicesAndDeactivateTrigger() throws SQLException {
		log.info("Entferne alle relevanten Indizes und Trigger");
		jdbcTemplate.getDataSource().getConnection().createStatement().execute("""
			DROP INDEX IF EXISTS kante_dlm_id_idx;
			DROP INDEX IF EXISTS kante_fahrtrichtung_attributgruppe_id_idx;
			DROP INDEX IF EXISTS kante_fuehrungsform_attribut_gruppe_id_idx;
			DROP INDEX IF EXISTS kante_geometry_idx;
			DROP INDEX IF EXISTS kante_geschwindigkeit_attributgruppe_id_idx;
			DROP INDEX IF EXISTS kante_idx;
			DROP INDEX IF EXISTS kante_kanten_attributgruppe_id_idx;
			DROP INDEX IF EXISTS kante_nach_knoten_id_idx;
			DROP INDEX IF EXISTS kante_quelle_idx;
			DROP INDEX IF EXISTS kante_von_knoten_id_idx;
			DROP INDEX IF EXISTS kante_zustaendigkeit_attributgruppe_id_idx;

			ALTER TABLE kante DISABLE TRIGGER ALL;
			ALTER TABLE zustaendigkeit_attribut_gruppe DISABLE TRIGGER ALL;
			ALTER TABLE zustaendigkeit_attribut_gruppe_zustaendigkeit_attribute DISABLE TRIGGER ALL;
			ALTER TABLE geschwindigkeit_attribut_gruppe DISABLE TRIGGER ALL;
			ALTER TABLE geschwindigkeit_attribut_gruppe_geschwindigkeit_attribute DISABLE TRIGGER ALL;
			ALTER TABLE fahrtrichtung_attribut_gruppe DISABLE TRIGGER ALL;
			ALTER TABLE fuehrungsform_attribut_gruppe DISABLE TRIGGER ALL;
			ALTER TABLE fuehrungsform_attribut_gruppe_attribute_links DISABLE TRIGGER ALL;
			ALTER TABLE fuehrungsform_attribut_gruppe_attribute_rechts DISABLE TRIGGER ALL;
			ALTER TABLE kanten_attribut_gruppe DISABLE TRIGGER ALL;
			""");
	}

	private void createIndicesAndEnableTrigger() throws SQLException {
		log.info("Erstelle alle zuvor gelöschten Indizes und Trigger neu");
		jdbcTemplate.getDataSource().getConnection().createStatement().execute(
			"""
				ALTER TABLE kante ENABLE TRIGGER ALL;
				ALTER TABLE zustaendigkeit_attribut_gruppe ENABLE TRIGGER ALL;
				ALTER TABLE zustaendigkeit_attribut_gruppe_zustaendigkeit_attribute ENABLE TRIGGER ALL;
				ALTER TABLE geschwindigkeit_attribut_gruppe ENABLE TRIGGER ALL;
				ALTER TABLE geschwindigkeit_attribut_gruppe_geschwindigkeit_attribute ENABLE TRIGGER ALL;
				ALTER TABLE fahrtrichtung_attribut_gruppe ENABLE TRIGGER ALL;
				ALTER TABLE fuehrungsform_attribut_gruppe ENABLE TRIGGER ALL;
				ALTER TABLE fuehrungsform_attribut_gruppe_attribute_links ENABLE TRIGGER ALL;
				ALTER TABLE fuehrungsform_attribut_gruppe_attribute_rechts ENABLE TRIGGER ALL;
				ALTER TABLE kanten_attribut_gruppe ENABLE TRIGGER ALL;

				CREATE INDEX kante_dlm_id_idx ON kante USING btree (dlm_id);
				CREATE INDEX kante_fahrtrichtung_attributgruppe_id_idx ON kante USING btree (fahrtrichtung_attributgruppe_id);
				CREATE INDEX kante_fuehrungsform_attribut_gruppe_id_idx ON kante USING btree (fuehrungsform_attribut_gruppe_id);
				CREATE INDEX kante_geometry_idx ON kante USING gist (geometry);
				CREATE INDEX kante_geschwindigkeit_attributgruppe_id_idx ON kante USING btree (geschwindigkeit_attributgruppe_id);
				CREATE INDEX kante_idx ON kante USING gist (geometry, quelle);
				CREATE INDEX kante_kanten_attributgruppe_id_idx ON kante USING btree (kanten_attributgruppe_id);
				CREATE INDEX kante_nach_knoten_id_idx ON kante USING btree (nach_knoten_id);
				CREATE INDEX kante_quelle_idx ON kante USING btree (quelle);
				CREATE INDEX kante_von_knoten_id_idx ON kante USING btree (von_knoten_id);
				CREATE INDEX kante_zustaendigkeit_attributgruppe_id_idx ON kante USING btree (zustaendigkeit_attributgruppe_id);
				""");
	}

	private static @NotNull String getBelagArtFromOsmTag(OsmTag tag) {
		return switch (tag.getValue()) {
		case "asphalt" -> BelagArt.ASPHALT.name();
		case "concrete", "concrete:plates", "concrete:lanes" -> BelagArt.BETON.name();
		case "sett", "cobblestone", "unhewn_cobblestone" -> BelagArt.NATURSTEINPFLASTER.name();
		case "paving_stones" -> BelagArt.BETONSTEINPFLASTER_PLATTENBELAG.name();
		case "compacted", "fine_gravel" -> BelagArt.WASSERGEBUNDENE_DECKE.name();
		case "gravel", "sand", "pebblestone", "ground", "dirt", "earth", "grass", "mud" -> BelagArt.UNGEBUNDENE_DECKE
			.name();
		default -> BelagArt.SONSTIGER_BELAG.name();
		};
	}

	private static @NotNull LineString getLineStringBetweenNodes(int fromNode, int toNode,
		HashMap<Long, Knoten> nodeIdToKnoten, long[] nodeIdArray) {
		Coordinate[] kanteCoordinates = new Coordinate[toNode - fromNode + 1];
		for (int i = fromNode; i <= toNode; i++) {
			kanteCoordinates[i - fromNode] = nodeIdToKnoten.get(nodeIdArray[i]).getKoordinate();
		}

		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(kanteCoordinates);
	}

	private boolean shouldImportWay(Way osmWay) {
		String highwayValue = null;
		String serviceValue = null;
		String levelValue = null;
		String tunnelValue = null;

		for (OsmTag tag : osmWay.getTags()) {
			String key = tag.getKey();
			String value = tag.getValue();

			// Early exits, damit der ganze Rest nicht weiter geprüft werden muss.
			if ("area".equals(key) && "yes".equals(value)) {
				// area=yes sind z.B. Polygone von Fußgängerzonen. Solche Objekte wollen wir ignorieren, da es keine
				// klassischen Wege für ein Netz sind.
				return false;
			}
			if ("indoor".equals(key) && "yes".equals(value)) {
				// Kommt viel in Einkaufszentren oder Bahnhöfen vor. Wollen wir auch nicht haben, da nicht Teil eines
				// befahrbaren Straßen- und Wegenetzes.
				return false;
			}

			if ("highway".equals(key)) {
				highwayValue = value;
			} else if ("service".equals(key)) {
				serviceValue = value;
			} else if ("level".equals(key)) {
				levelValue = value;
			} else if ("tunnel".equals(key)) {
				tunnelValue = value;
			}
		}

		boolean isUndergroundFootway = ("path".equals(highwayValue) || "footway".equals(highwayValue)) &&
			tunnelValue != null && !"building_passage".equals(tunnelValue) && !"no".equals(tunnelValue) &&
			levelValue != null;
		if (isUndergroundFootway) {
			return false;
		}

		boolean hasValidHighwayTag = "primary".equals(highwayValue) ||
			"primary_link".equals(highwayValue) ||
			"secondary".equals(highwayValue) ||
			"secondary_link".equals(highwayValue) ||
			"tertiary".equals(highwayValue) ||
			"tertiary_link".equals(highwayValue) ||
			"residential".equals(highwayValue) ||
			"living_street".equals(highwayValue) ||
			"road".equals(highwayValue) ||
			"unclassified".equals(highwayValue) ||
			"track".equals(highwayValue) ||
			"path".equals(highwayValue) ||
			"cycleway".equals(highwayValue) ||
			"footway".equals(highwayValue) ||
			"pedestrian".equals(highwayValue) ||
			"steps".equals(highwayValue) && levelValue == null ||
			"service".equals(highwayValue) && (serviceValue == null || "alley".equals(serviceValue) || "parking_aisle"
				.equals(serviceValue));

		return hasValidHighwayTag;
	}

	@Override
	public JobDescription getDescription() {
		return new JobDescription(
			"Importiert das Wegenetz der konfigurierten OSM-PBF-Datei als DLM-Kanten. Siehe Klassenkommentar für Details.",
			"Die Wege aus OSM sind in Form von normalen DLM-Kanten in der DB gespeichert.",
			"",
			"Nicht auf produktiven Systemen mit echten DLM-Kanten ausführen! Kanten werden zudem nur ergänzt, es findet keine Duplikaterkennung oder Aktualisierung bestehender Kanten statt!",
			JobExecutionDurationEstimate.UNKNOWN
		);
	}
}
