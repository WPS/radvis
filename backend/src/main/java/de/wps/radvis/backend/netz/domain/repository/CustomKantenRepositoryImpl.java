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

package de.wps.radvis.backend.netz.domain.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.jetbrains.annotations.NotNull;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKBWriter;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;
import org.springframework.data.domain.Slice;

import com.google.common.collect.Lists;

import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.dbView.KanteOsmMatchWithAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteGeometryView;
import de.wps.radvis.backend.netz.domain.entity.KanteOsmWayIdsInsert;
import de.wps.radvis.backend.netz.domain.entity.NahegelegeneneKantenDbView;
import de.wps.radvis.backend.netz.domain.valueObject.KanteElevationUpdate;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

@Slf4j
public class CustomKantenRepositoryImpl implements CustomKantenRepository {

	@PersistenceContext
	private EntityManager entityManager;

	private final FeatureToggleProperties featureToggleProperties;

	public CustomKantenRepositoryImpl(FeatureToggleProperties featureToggleProperties) {
		require(featureToggleProperties, notNullValue());
		this.featureToggleProperties = featureToggleProperties;
	}

	@Override
	public Set<Kante> getKantenInBereichNachQuelleUndIsAbgebildet(Envelope bereich, QuellSystem quelle) {
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		String hqlStringBuilder = "SELECT kante FROM Kante kante " + " WHERE "
			+ CommonQueryLibrary.whereClauseFuerBereichKante()
			+ " AND " + "kante.quelle = :quelle"
			+ " AND " + "kante.aufDlmAbgebildeteGeometry != null ";

		return entityManager.createQuery(hqlStringBuilder, Kante.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("quelle", quelle)
			.getResultStream().collect(Collectors.toSet());
	}

	@Override
	public Stream<Kante> getKantenInBereichNachQuellen(Envelope bereich, Collection<QuellSystem> quellen) {
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		StringBuilder hqlStringBuilder = queryFuerBereichUndMehrereQuellSysteme();

		return entityManager.createQuery(hqlStringBuilder.toString(), Kante.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("quellen", quellen)
			.getResultStream();
	}

	@Override
	public List<Kante> getKantenForNetzklassenEagerFetchKnoten(
		Set<Netzklasse> netzklassen) {
		String hqlStringBuilder = "SELECT DISTINCT kante FROM Kante kante"
			+ CommonQueryLibrary.joinNetzklassen()
			+ CommonQueryLibrary.eagerFetchVonKnoten()
			+ CommonQueryLibrary.eagerFetchNachKnoten()
			+ " WHERE "
			+ CommonQueryLibrary.whereClauseGrundnetz(true) + " AND "
			+ CommonQueryLibrary.whereClauseFuerNetzklassen(false);

		return entityManager.createQuery(hqlStringBuilder, Kante.class)
			.setParameter("netzklassen", netzklassen)
			.getResultList();
	}

	@Override
	public Stream<Kante> getKantenInBereichNachQuellenEagerFetchFahrtrichtungEagerFetchFuehrungsformAttributeLinks(
		Envelope bereich,
		Set<QuellSystem> quellen) {
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		String hqlStringBuilder = "SELECT kante FROM Kante kante"
			+ CommonQueryLibrary.eagerFetchFahrtrichtung()
			+ CommonQueryLibrary.eagerFetchFuehrungsform()
			+ CommonQueryLibrary.eagerFetchFuehrungsformAttributeLinks()
			+ CommonQueryLibrary.eagerFetchFuehrungsformAttributeRechts()
			+ " WHERE "
			+ CommonQueryLibrary.whereClauseFuerBereichKante()
			+ " AND " + "kante.quelle IN :quellen";

		return entityManager.createQuery(hqlStringBuilder, Kante.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("quellen", quellen)
			.getResultStream();
	}

	private StringBuilder queryFuerBereichUndMehrereQuellSysteme() {
		StringBuilder hqlStringBuilder = new StringBuilder();
		hqlStringBuilder.append("SELECT DISTINCT kante FROM Kante kante ").append(" WHERE ")
			.append(CommonQueryLibrary.whereClauseFuerBereichKante())
			.append(" AND ").append("kante.quelle IN :quellen");
		return hqlStringBuilder;
	}

	@Override
	@Transactional
	public void buildIndex() {

		entityManager.createNativeQuery(
			"CREATE INDEX kante_idx_tmp ON kante USING GIST (geometry, quelle)")
			.executeUpdate();

		entityManager.createNativeQuery(
			"DROP INDEX IF EXISTS kante_idx")
			.executeUpdate();

		entityManager.createNativeQuery(
			"ALTER INDEX kante_idx_tmp RENAME TO kante_idx")
			.executeUpdate();

	}

	@Override
	public Set<Kante> getKantenInBereichNachNetzklasse(Envelope bereich, Set<NetzklasseFilter> netzklassenFilter,
		boolean showDLM) {

		require(bereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		Set<Netzklasse> netzklassen = CommonQueryLibrary.getNetzklassenParameter(netzklassenFilter);

		boolean orNichtKlassifiziert = netzklassenFilter.contains(NetzklasseFilter.NICHT_KLASSIFIZIERT);

		String hqlStringBuilder = "SELECT DISTINCT kante FROM Kante kante"
			+ " LEFT JOIN kante.kantenAttributGruppe as kag"
			+ " LEFT JOIN kag.netzklassen as nk"
			+ " WHERE"
			+ CommonQueryLibrary.whereClauseFuerNetzklassen(orNichtKlassifiziert)
			+ " AND "
			+ CommonQueryLibrary.whereClauseFuerBereichKante()
			+ " AND "
			+ CommonQueryLibrary.whereClauseGrundnetz(featureToggleProperties.isShowDlm() || showDLM);

		return entityManager.createQuery(hqlStringBuilder, Kante.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("netzklassen", netzklassen)
			.getResultStream().collect(Collectors.toSet());
	}

	@Override
	public Set<Kante> getKantenInBereich(Geometry bereich) {
		if (bereich.isEmpty()) {
			return new HashSet<>();
		}

		String hqlStringBuilder = "SELECT kante FROM Kante kante "
			+ " WHERE "
			+ CommonQueryLibrary.whereClauseFuerBereichKante()
			+ " AND "
			+ CommonQueryLibrary.whereClauseGrundnetz(true);

		return entityManager.createQuery(hqlStringBuilder, Kante.class)
			.setParameter("bereich", bereich)
			.getResultStream().collect(Collectors.toSet());
	}

	@Override
	public List<NahegelegeneneKantenDbView> getNahegelegeneKantenAufSeite(Kante basiskante,
		LinearReferenzierterAbschnitt abschnitt, Seitenbezug seite, Laenge abstandInM) {
		require(seite == Seitenbezug.LINKS || seite == Seitenbezug.RECHTS);
		require(abstandInM.getValue() > 0);

		double bufferAbstandValue = abstandInM.getValue();

		// Gemäß BufferParameters Doku: Positive Abstände erzeugen einen Buffer nach links, negative nach rechts.
		if (seite == Seitenbezug.RECHTS) {
			bufferAbstandValue = -bufferAbstandValue;
		}

		LineString basiskanteAbschnittGeometry = abschnitt.toSegment(basiskante.getGeometry());

		BufferParameters bufParams = new BufferParameters();
		bufParams.setSingleSided(true);
		Geometry bufferGeometry = BufferOp.bufferOp(basiskanteAbschnittGeometry, bufferAbstandValue, bufParams);

		// Rein theoretisch kann beim BufferOp hier eine nicht-Polygon Geometrie raus kommen. Zumindest sichert uns der
		// Rückgabetyp "Geometry" nichts genaueres zu. Daher Prüfung, ob es ein (Multi)Polygon ist. Wenn nicht, würde
		// der Rest vom Code ggf. zwar laufen, aber es ist ein Indiz dafür, dass etwas schief gelaufen ist.
		if (!(bufferGeometry instanceof MultiPolygon) && !(bufferGeometry instanceof Polygon)) {
			throw new RuntimeException("Puffer für Kante " + basiskante.getId() + " nach " + seite.name()
				+ " im Abstand von " + bufferAbstandValue + "m ist kein (Multi)Polygon, sondern " + bufferGeometry
					.getGeometryType());
		}

		List<NahegelegeneneKantenDbView> result = new ArrayList<>();
		Set<Kante> kantenInBuffer = getKantenInBereich(bufferGeometry);
		for (Kante nahegelegeneKante : kantenInBuffer) {
			if (nahegelegeneKante.equals(basiskante)) {
				// Natürlich berührt die Basiskante ihren eigenen Buffer, deswegen kriegen wir sie hier immer. Sie ist
				// aber zu sich selbst natürlich keine nahegelegene Kante, weswegen wir die hier ignorieren.
				continue;
			}

			// Abschnitt auf nahegelegener Kante finden. Dies kann zu anderen Geometrien als LineString führen, insb. zu
			// MultiLineString, wenn die nahegelegene Kante aus dem Buffer raus und wieder rein kommt. Diese Geometein
			// entpacken wir daher und betrachten jedes Teilstück, was sich im Buffer befindet.
			Geometry nahegelegeneKanteAbschnittGeometry = nahegelegeneKante.getGeometry().intersection(bufferGeometry);
			for (Geometry nahegelegeneSubAbschnittGeometry : unwrapGeometry(nahegelegeneKanteAbschnittGeometry)) {
				boolean nahegelegeneSubAbschnittIsEmpty = nahegelegeneSubAbschnittGeometry.isEmpty();
				if (!(nahegelegeneSubAbschnittGeometry instanceof LineString) || nahegelegeneSubAbschnittIsEmpty) {
					// Punkt-Geometrien sind sehr sehr häufig, da alle angrenzenten Kanten als solche erkannt werden.
					// Andere Geometrien jedoch sind ungewöhnlich.
					if (!(nahegelegeneSubAbschnittGeometry instanceof Point)) {
						log.warn(
							"Beim Suchen nach nahegelegenen Kanten von Kante {} war eine Abschnitt-Geometrie von Kante {} ungültig (Geometrietyp: {}, leere Geometrie? {})",
							basiskante.getId(),
							nahegelegeneKante.getId(),
							nahegelegeneSubAbschnittGeometry.getGeometryType(),
							nahegelegeneSubAbschnittIsEmpty
						);
					}
					continue;
				}

				// Abschnitt auf Basiskante finden
				Geometry bufferedIntersectionLineString = nahegelegeneSubAbschnittGeometry.buffer(abstandInM
					.getValue());
				Geometry basiskanteAbgedeckterAbschnittGeometry = basiskanteAbschnittGeometry.intersection(
					bufferedIntersectionLineString);
				boolean basiskanteAbgedeckterAbschnittIsEmpty = basiskanteAbgedeckterAbschnittGeometry.isEmpty();
				if (!(basiskanteAbgedeckterAbschnittGeometry instanceof LineString)
					|| basiskanteAbgedeckterAbschnittIsEmpty) {
					log.warn(
						"Beim Suchen nach nahegelegenen Kanten von Kante {} war die Basiskante-Abschnitt-Geometrie (der durch Kante {} abgedeckte Teil) ungültig (Geometrietyp: {}, leere Geometrie? {})",
						basiskante.getId(),
						nahegelegeneKante.getId(),
						basiskanteAbgedeckterAbschnittGeometry.getGeometryType(),
						basiskanteAbgedeckterAbschnittIsEmpty
					);
					continue;
				}

				result.add(new NahegelegeneneKantenDbView(basiskante,
					(LineString) basiskanteAbgedeckterAbschnittGeometry, nahegelegeneKante,
					(LineString) nahegelegeneSubAbschnittGeometry));
			}
		}

		return result;
	}

	/**
	 * Entpackt die übergebene Geometrie (egal ob einfache Geometrie, oder Multi-Geometrie wie MultiPoint,
	 * MultiLineString, GeometryCollection, etc.) in ihre einzelnen "primitiven" Bestandteile. Also ein MultiLineString
	 * wird in die enthaltenen LineStrings entpackt.
	 *
	 * Das Entpacken passiert rekursiv, enthält also z.B. eine GeometryCollection eine weitere GeometryCollection, wird
	 * auch diese entpackt.
	 *
	 * @return Eine Liste aller simplen Geometrien. Die Liste enthält also keine Multi-Geometrien oder GeometryCollections mehr.
	 */
	private List<Geometry> unwrapGeometry(Geometry geometry) {
		List<Geometry> result = new ArrayList<>();

		for (int i = 0; i < geometry.getNumGeometries(); i++) {
			Geometry childGeometry = geometry.getGeometryN(i);
			// Multi-Geometrien (MultiPoint, etc.) erben von GeometryCollection und werden hier entsprechend unwrapped.
			if (childGeometry instanceof GeometryCollection) {
				result.addAll(unwrapGeometry((GeometryCollection) childGeometry));
			} else {
				result.add(childGeometry);
			}
		}

		return result;
	}

	@Override
	public Set<Kante> getKantenInOrganisationsbereichEagerFetchNetzklassen(Verwaltungseinheit organisation) {
		if (organisation.getBereich().isEmpty()) {
			return new HashSet<>();
		}
		String hqlStringBuilder = "SELECT DISTINCT kante FROM Kante kante "
			+ " WHERE "
			+ CommonQueryLibrary.whereClauseFuerBereichKante()
			+ " AND "
			+ CommonQueryLibrary.whereClauseGrundnetz(true);

		Set<Kante> kanten = entityManager.createQuery(hqlStringBuilder, Kante.class)
			.setParameter("bereich", organisation.getBereich().get())
			.getResultStream().collect(Collectors.toSet());

		return kanten;
	}

	@Override
	public Set<Kante> getKantenInBereich(Envelope bereich) {
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		return getKantenInBereich(bereichAlsPolygon);
	}

	@Override
	public Stream<Kante> getKantenInOrganisationsbereichEagerFetchKnoten(Verwaltungseinheit organisation) {
		if (organisation.getBereich().isEmpty()) {
			return Stream.empty();
		}
		String hqlStringBuilder = "SELECT kante FROM Kante kante"
			+ CommonQueryLibrary.eagerFetchVonKnoten()
			+ CommonQueryLibrary.eagerFetchNachKnoten()
			+ " WHERE "
			+ CommonQueryLibrary.whereClauseFuerBereichKante()
			+ " AND "
			+ CommonQueryLibrary.whereClauseGrundnetz(true);

		return entityManager.createQuery(hqlStringBuilder, Kante.class)
			.setParameter("bereich", organisation.getBereich().get())
			.getResultStream();
	}

	@Override
	public void insertOsmWayIds(List<KanteOsmWayIdsInsert> inserts) {
		List<List<KanteOsmWayIdsInsert>> kantenBatches = Lists.partition(inserts, 1000);
		for (List<KanteOsmWayIdsInsert> batch : kantenBatches) {
			StringBuilder sqlStringBuilder = new StringBuilder();
			sqlStringBuilder.append("INSERT INTO kante_osm_way_ids (kante_id, value, von, bis) VALUES ");
			for (KanteOsmWayIdsInsert update : batch) {
				for (LinearReferenzierteOsmWayId linearReferenzierteOsmWayId : update.getOsmWayIds()) {
					sqlStringBuilder.append(
						"(" + update.getKanteId() + "," + linearReferenzierteOsmWayId.getValue() + ","
							+ linearReferenzierteOsmWayId.getLinearReferenzierterAbschnitt().getVonValue() + ","
							+ linearReferenzierteOsmWayId.getLinearReferenzierterAbschnitt().getBisValue() + "),");
				}
			}

			String query = sqlStringBuilder.substring(0, sqlStringBuilder.length() - 1);
			entityManager.createNativeQuery(query).executeUpdate();
		}
	}

	@Override
	public void truncateOsmWayIds() {
		entityManager.createNativeQuery("TRUNCATE TABLE kante_osm_way_ids").executeUpdate();
	}

	@Override
	public List<KanteGeometryView> getFuerOsmAbbildungRelevanteKanten(Envelope envelope) {
		Polygon bereich = EnvelopeAdapter.toPolygon(envelope, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		// Relevant bedeutet, dass die Kanten in den, fuer die OSM-Ausleitung relevanten Attributen, vom Default
		// abweichen

		String hqlString = """
			SELECT new de.wps.radvis.backend.netz.domain.entity.KanteGeometryView(k.id, k.geometry)
			FROM Kante k
			WHERE
				k.id IN (
				SELECT ka.kanteId
					FROM KantenAbschnittOsmMapping ka
				WHERE
					intersects(CAST(:bereich as org.locationtech.jts.geom.Geometry), CAST(ka.geometry as org.locationtech.jts.geom.Geometry)) = true
						AND ka.status NOT LIKE 'FIKTIV'
						AND (
							ka.radverkehrsfuehrung NOT LIKE 'UNBEKANNT'
							OR ka.breite IS NOT NULL
							OR ka.belagArt NOT LIKE 'UNBEKANNT'
							OR ka.oberflaechenbeschaffenheit NOT LIKE 'UNBEKANNT'
							OR ka.status NOT LIKE 'UNTER_VERKEHR'
							OR ka.netzklassen IS NOT NULL OR ka.netzklassen NOT LIKE ''
						)
				)
				OR (
					EXISTS (
						SELECT 1
						FROM Fahrradroute f
						JOIN f.abschnittsweiserKantenBezug AS fka
						WHERE
							f.drouteId IS NOT NULL
							AND fka.kante.id = k.id
					)
					AND intersects(CAST(:bereich as org.locationtech.jts.geom.Geometry), CAST(k.geometry as org.locationtech.jts.geom.Geometry)) = true
					AND k.kantenAttributGruppe.kantenAttribute.status NOT LIKE 'FIKTIV'
			)""";

		return entityManager.createQuery(hqlString, KanteGeometryView.class)
			.setParameter("bereich", bereich)
			.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Stream<KanteOsmMatchWithAttribute> getKanteOsmMatchesWithOsmAttributes(
		double minimaleUeberdeckungFuerAttributAuszeichnung) {
		String sqlString = "WITH drouteKantenIds AS (SELECT DISTINCT fk.kante_id AS id FROM fahrradroute f JOIN fahrradroute_kantenabschnitte fk on f.id = fk.fahrradroute_id WHERE f.droute_id IS NOT NULL) "
			+ "SELECT "
			+ "    most_relevant_mapping.kante_id, "
			+ "    most_relevant_mapping.value, "
			+ "    kante.status, "
			+ "    kante.netzklassen, "
			+ "    kante.radverkehrsfuehrung, "
			+ "    kante.breite, "
			+ "    kante.belag_art, "
			+ "    kante.oberflaechenbeschaffenheit, "
			+ "	   kante.id IN (SELECT * FROM drouteKantenIds) AS dRoute "
			+ "FROM ("
			+ "    SELECT "
			+ "        relevant_mapping.kante_id, "
			+ "        relevant_mapping.value "
			+ "    FROM ("
			+ "        SELECT"
			+ "            kowi.kante_id, "
			+ "            kowi.value, "
			+ "            RANK() OVER ("
			+ "                PARTITION BY kowi.value"
			+ "                ORDER BY SUM(kowi.bis-kowi.von) DESC"
			+ "            ) AS pos"
			+ "        FROM ( "
			+ "            SELECT k.value FROM kante_osm_way_ids k "
			+ "            GROUP BY k.value "
			+ "            HAVING SUM(k.bis-k.von) >= :minimaleUeberdeckungFuerAttributAuszeichnung "
			+ "        ) relevant_way "
			+ "        JOIN kante_osm_way_ids kowi ON kowi.value = relevant_way.value "
			+ "        GROUP BY kowi.kante_id, kowi.value "
			+ "    ) relevant_mapping "
			+ "    WHERE pos = 1 "
			+ ") AS most_relevant_mapping "
			+ "JOIN geoserver_radvisnetz_kante_materialized_view kante ON kante.id = most_relevant_mapping.kante_id;";

		Stream<Object[]> resultStream = entityManager.createNativeQuery(sqlString)
			.setParameter("minimaleUeberdeckungFuerAttributAuszeichnung", minimaleUeberdeckungFuerAttributAuszeichnung)
			.getResultStream();
		return resultStream.map(objects -> new KanteOsmMatchWithAttribute(
			(Long) objects[0], // kanteId
			(Long) objects[1], // osmWayId
			(String) objects[2], // Status name des Enums
			(String) objects[3], // netzklassen ; separiert zu String gejoint
			(String) objects[4], // Radverkehrsfuehrung name des Enums
			objects[5] != null ? ((BigDecimal) objects[5]).doubleValue() : null, // Breite
			(String) objects[6], // BelagArt name des Enums
			(String) objects[7], // Oberflaechenbeschaffenheit name des Enums
			(Boolean) objects[8] // DRoutenzugehörigkeit
		));
	}

	@Override
	public Stream<Kante> getEinseitigBefahrbareKanten() {
		String hqlStringBuilder = "SELECT DISTINCT kante FROM Kante kante"
			+ CommonQueryLibrary.eagerFetchFahrtrichtung()
			+ CommonQueryLibrary.eagerFetchVonKnoten()
			+ CommonQueryLibrary.eagerFetchNachKnoten()
			+ " WHERE ((fahrtrichtung.fahrtrichtungLinks = 'IN_RICHTUNG' AND fahrtrichtung.fahrtrichtungRechts='IN_RICHTUNG')"
			+ " OR (fahrtrichtung.fahrtrichtungLinks = 'GEGEN_RICHTUNG' AND fahrtrichtung.fahrtrichtungRechts='GEGEN_RICHTUNG'))"
			+ " AND "
			+ CommonQueryLibrary.whereClauseGrundnetz(true);

		return entityManager.createQuery(hqlStringBuilder, Kante.class)
			.getResultStream();
	}

	@Override
	public void refreshNetzMaterializedViews() {
		// Reihenfolge beachten, da Views tlw. aufeinander aufbauen, was gerade bei materialized Views relevant ist.
		List<String> materializedViews = List.of(
			"netzklassen_materialized_view",
			"standards_materialized_view",
			"fuehrungsform_attribute_maxanteil_materialized_view",
			"geschwindigkeit_attribute_maxanteil_materialized_view",
			"zustaendigkeit_attribute_maxanteil_materialized_view",
			"geoserver_radvisnetz_kante_materialized_view",
			"kante_lr_materialized_view",
			"kante_lr_interpolated_materialized_view",
			"geoserver_radvisnetz_kante_abschnitte_materialized_view",
			"geoserver_radvisnetz_kante_abschnitte_balm_materialized_view",
			"geoserver_balm_knoten_view",
			"geoserver_balm_kanten_view",
			"geoserver_balm_fahrradrouten_view",
			"geoserver_balm_wegweisende_beschilderung_view");
		materializedViews.forEach(materializedView -> {
			log.info("Refreshing Materialized View {}", materializedView);
			entityManager.createNativeQuery("REFRESH MATERIALIZED VIEW " + materializedView).executeUpdate();
		});
		log.info("Done!");
	}

	@Override
	public void updateKanteElevation(Slice<KanteElevationUpdate> kanteElevationInserts) {
		if (kanteElevationInserts.isEmpty()) {
			return;
		}
		WKBWriter wkbWriter = new WKBWriter(3, 1, true);
		String sql = String.format(
			"UPDATE kante k SET geometry3d = kanteEle.geometry3d FROM (values %s) AS kanteEle(id, geometry3d) WHERE k.id = kanteEle.id",
			kanteElevationInserts.stream()
				.map(insert -> {
					String hex;
					try {
						hex = WKBWriter.toHex(wkbWriter.write(insert.getGeometry3d()));
					} catch (Exception e) {
						log.error("Couldn't serialize Geometry as hex: {}", insert.getGeometry3d(), e);
						hex = WKBWriter.toHex(wkbWriter.write(
							KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString()));
					}
					return String.format("(%s,'%s'\\:\\:geometry)",
						insert.getId(),
						hex);
				})
				.collect(Collectors.joining(",")));
		entityManager.createNativeQuery(sql).executeUpdate();
	}

	@Override
	public HashMap<String, Integer> addMissingAuditingEntries(Long benutzerId, int batchSize,
		String auditingContextName, Long jobExecutionDescriptionId) {
		require(auditingContextName != null, "auditingContextName darf nicht null sein");

		final int enversEntityAddedRevtype = 0;
		final int initialVersionOfEntities = 0;

		HashMap<String, Integer> tableToNewAuditingEntries = new HashMap<>();
		HashMap<String, String> tableToIdColumnMap = new HashMap<>();
		tableToIdColumnMap.put("kante", "id");
		tableToIdColumnMap.put("kanten_attribut_gruppe", "id");
		tableToIdColumnMap.put("kanten_attribut_gruppe_ist_standards", "kanten_attribut_gruppe_id");
		tableToIdColumnMap.put("kanten_attribut_gruppe_netzklassen", "kanten_attribut_gruppe_id");
		tableToIdColumnMap.put("fahrtrichtung_attribut_gruppe", "id");
		tableToIdColumnMap.put("fuehrungsform_attribut_gruppe", "id");
		tableToIdColumnMap.put("fuehrungsform_attribut_gruppe_attribute_rechts", "fuehrungsform_attribut_gruppe_id");
		tableToIdColumnMap.put("fuehrungsform_attribut_gruppe_attribute_links", "fuehrungsform_attribut_gruppe_id");
		tableToIdColumnMap.put("geschwindigkeit_attribut_gruppe", "id");
		tableToIdColumnMap.put("geschwindigkeit_attribut_gruppe_geschwindigkeit_attribute",
			"geschwindigkeit_attribut_gruppe_id");
		tableToIdColumnMap.put("zustaendigkeit_attribut_gruppe", "id");
		tableToIdColumnMap.put("zustaendigkeit_attribut_gruppe_zustaendigkeit_attribute",
			"zustaendigkeit_attribut_gruppe_id");

		log.debug("Legt rev_info Eintrag an");
		Long revId = createRevInfoEntry(benutzerId, auditingContextName, jobExecutionDescriptionId);
		log.debug("Nutze rev_info Eintrag mit ID {}", revId);

		AtomicInteger i = new AtomicInteger();
		for (Map.Entry<String, String> entry : tableToIdColumnMap.entrySet()) {
			String entityTableName = entry.getKey();
			String entityIdColumnName = entry.getValue();

			log.info("Starte Anlegen von Auditing-Einträgen für Tabelle {} ({} / {})", entityTableName, i
				.incrementAndGet(), tableToIdColumnMap.size());

			int newEntries = addMissingAuditingEntriesForTable(entityTableName, entityIdColumnName, batchSize,
				enversEntityAddedRevtype, initialVersionOfEntities, revId);
			log.info("Für Tabelle {} wurden {} fehlende Auditing Einträge ergänzt", entityTableName, newEntries);

			tableToNewAuditingEntries.put(entityTableName, newEntries);
		}

		return tableToNewAuditingEntries;
	}

	/**
	 * Legt für die angegebene Tabelle Auditing-Einträge für die Elemente an, für die es noch keine gibt. Dabei wird
	 * pro Batch ein INSERT-Statement für die Auditing-Tabelle ausgeführt.
	 *
	 * Es wird NICHT auf Envers zurückgegriffen, da es bei größeren Tabellen viel zu viele Entities sind und es mehrere
	 * Tage dauern könnte. Zudem bietet Envers keine Standard-Funktion an einen Auditing Eintrag zu forcieren, daher
	 * wäre hier auch Hacks nötig, um das zu erreichen. Daher einfach direkt per SQL.
	 *
	 * @return Anzahl an ergänzen Auditing-Einträgen.
	 */
	private int addMissingAuditingEntriesForTable(String entityTableName, String entityIdColumnName, int batchSize,
		int enversEntityAddedRevtype, int initialVersionOfEntities, Long revId) {
		require(batchSize > 0, "Batch-Größe muss mindestens 1 betragen");
		// Durch Postgres vorgegeben, da dies die maximale Anzahl an Elementen in "IN"-Statements ist:
		require(batchSize <= 32767, "Batch-Größe darf maximal 32767 betragen");

		log.debug("Hole IDs von Entities aus Datenbank-Tabelle {}, die keine Auditing-Einträge haben", entityTableName);
		List<Long> entityIds = getEntityIdsWithoutAuditingEntries(entityTableName, entityIdColumnName);
		if (entityIds.size() == 0) {
			log.debug("Keine Entities in der Tabelle {} gefunden, deren Auditing-Einträge fehlen - Fertig",
				entityTableName);
			return 0;
		}
		log.debug("{} Entities in der Tabelle {} gefunden, deren Auditing-Einträge fehlen", entityIds.size(),
			entityTableName);

		log.debug("Ermittel Spaltennamen, die in Auditing-Tabelle zu setzen sind");
		List<String> columnNames = getAuditedColumnsOfTable(entityTableName);
		boolean hasVersionColumn = columnNames.remove("version");
		log.debug("Ermittelte gemeinsame Spalten aus Daten- und Auditing-Tabelle: {}", columnNames);

		// Spalten-Listen für die INSERT- und SELECT-Statements bauen
		String columnNamesParamList = columnNames.stream()
			.map(columnName -> String.format("\"%s\"", columnName))
			.collect(Collectors.joining(", "));
		String columnValuesParamList = columnNames.stream()
			.map(columnName -> String.format("entity.%s", columnName))
			.collect(Collectors.joining(", "));

		log.debug("Starte das batchweise Einfügen neuer Auditing-Einträge");
		int lastBatch = entityIds.size() / batchSize;
		for (int batch = 0; batch <= lastBatch; batch++) {
			int fromIdx = batch * batchSize;
			int toIdx = Math.min((batch + 1) * batchSize, entityIds.size());
			List<Long> entityIdsOfBatch = entityIds.subList(fromIdx, toIdx);

			if (entityIdsOfBatch.size() == 0) {
				// Wenn die Anzahl an Entities glatt durch die batchSize teilbar ist, hat der letzte Batch die Größe 0.
				break;
			}

			log.trace("Verarbeite batch {} / {} mit {} Entities für Tabelle {}", batch + 1, lastBatch, entityIdsOfBatch
				.size(), entityTableName);

			String entityIdsOfBatchListSql = entityIdsOfBatch.stream().map(id -> id.toString()).collect(Collectors
				.joining(", "));

			// Eigentliche INSERT-Statements für die auditing-Tabellen bauen. Einzelne Statements wären sehr langsam.
			String auditingTableInsertStatement;
			if (hasVersionColumn) {
				auditingTableInsertStatement = String.format(
					"""
						INSERT INTO %s_aud("rev", "revtype", "version", %s)
						SELECT %d, %d, %d, %s
						FROM %s entity
						WHERE %s IN (%s);
						""",
					entityTableName, columnNamesParamList, revId, enversEntityAddedRevtype, initialVersionOfEntities,
					columnValuesParamList, entityTableName, entityIdColumnName, entityIdsOfBatchListSql);
			} else {
				auditingTableInsertStatement = String.format(
					"""
						INSERT INTO %s_aud("rev", "revtype", %s)
						SELECT %d, %d, %s
						FROM %s entity
						WHERE %s IN (%s);
						""",
					entityTableName, columnNamesParamList, revId, enversEntityAddedRevtype, columnValuesParamList,
					entityTableName, entityIdColumnName, entityIdsOfBatchListSql);
			}

			entityManager.createNativeQuery(auditingTableInsertStatement).executeUpdate();

			int bisherVerarbeiteteEntities = batch * batchSize + entityIdsOfBatch.size();
			log.trace("Für {} von {} ({}%) Entities wurden fehlende Auditing-Einträge ergänzt",
				bisherVerarbeiteteEntities, entityIds.size(), (int) ((float) bisherVerarbeiteteEntities
					/ (float) entityIds.size() * 100));
		}

		return entityIds.size();
	}

	/**
	 * Ermittelt alle IDs der gegebenen Entity-Tabelle, die keine dazugehörigen Auditing-Einträge haben. Hier wird aus
	 * Performance-Gründen nicht Envers genutzt, sondern direkt auf die "..._aud"-Tabelle gegangen.
	 */
	@SuppressWarnings({ "unchecked" })
	private List<Long> getEntityIdsWithoutAuditingEntries(String entityTableName, String entityIdColumnName) {
		return entityManager.createNativeQuery(String.format("""
			SELECT DISTINCT entity.%s FROM %s entity
			LEFT JOIN %s_aud aud ON aud.%s = entity.%s
			WHERE aud.%s IS NULL""", entityIdColumnName, entityTableName, entityTableName, entityIdColumnName,
			entityIdColumnName, entityIdColumnName), Long.class)
			.getResultList();
	}

	/**
	 * Ermittelt die Liste an Spaltennamen der angegebenen Tabelle, die auch in der Auditing-Tabelle enthalten sind.
	 */
	@SuppressWarnings({ "unchecked" })
	private @NotNull List<String> getAuditedColumnsOfTable(String entityTableName) {
		log.debug("Ermittle Spaltennamen von Auditing-Tabelle '{}_aud'", entityTableName);
		List<String> columnsOfAuditingTable = entityManager.createNativeQuery(String.format("""
			SELECT column_name
			FROM information_schema.columns
			WHERE table_schema = 'public' AND table_name = '%s_aud';
			""", entityTableName), String.class)
			.getResultList();

		log.debug("Ermittle Spaltennamen von Daten-Tabelle '{}'", entityTableName);
		List<String> columnsOfDataTable = entityManager.createNativeQuery(String.format("""
			SELECT column_name
			FROM information_schema.columns
			WHERE table_schema = 'public' AND table_name = '%s';
			""", entityTableName), String.class)
			.getResultList();

		// Schnittmenge bilden, also nur die Spalten behalten, die in beiden Tabellen enthalten sind.
		columnsOfDataTable.retainAll(columnsOfAuditingTable);

		return columnsOfDataTable;
	}

	/**
	 * Legt einen Eintrag in der rev_info Tabelle mit den übergebenen Werten an und gibt dessen ID zurück
	 */
	@SuppressWarnings({ "unchecked" })
	private @NotNull Long createRevInfoEntry(Long benutzerId, String auditingContextName,
		Long jobExecutionDescriptionId) {
		log.debug("Erzeugt neue IDs");
		String generateRevInfoIdsQuery = "SELECT nextval('hibernate_sequence');";
		List<Long> revIdValues = entityManager.createNativeQuery(generateRevInfoIdsQuery, Long.class).getResultList();
		require(revIdValues.size() == 1, "Es muss exakt eine rev-ID erzeugt worden sein");
		Long revId = revIdValues.get(0);

		log.debug("Schreibt neuen rev-Eintrag in die Datenbank");
		String revsInsertStatement = String.format(
			"""
				INSERT INTO rev_info("id", "timestamp", "auditing_context", "benutzer_id", "job_execution_description_id") VALUES(%d, %d, '%s', %d, %d)""",
			revId, System.currentTimeMillis(), auditingContextName, benutzerId, jobExecutionDescriptionId);

		entityManager.createNativeQuery(revsInsertStatement).executeUpdate();

		return revId;
	}
}
