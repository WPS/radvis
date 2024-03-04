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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKBWriter;
import org.springframework.data.domain.Slice;

import com.google.common.collect.Lists;

import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.dbView.KanteOsmMatchWithAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteGeometryView;
import de.wps.radvis.backend.netz.domain.entity.KanteOsmWayIdsInsert;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.valueObject.KanteElevationUpdate;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomKantenRepositoryImpl implements CustomKantenRepository {

	@PersistenceContext
	private EntityManager entityManager;

	private final FeatureToggleProperties featureToggleProperties;

	private final PostgisConfigurationProperties postgisConfigurationProperties;

	public CustomKantenRepositoryImpl(FeatureToggleProperties featureToggleProperties,
		PostgisConfigurationProperties postgisConfigurationProperties) {
		require(featureToggleProperties, notNullValue());
		require(postgisConfigurationProperties, notNullValue());
		this.featureToggleProperties = featureToggleProperties;
		this.postgisConfigurationProperties = postgisConfigurationProperties;
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
	public Stream<Kante> getKantenInBereichNachQuelle(Envelope bereich, QuellSystem quelle) {
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		StringBuilder hqlStringBuilder = queryFuerBereichUndQuellSystem();

		return entityManager.createQuery(hqlStringBuilder.toString(), Kante.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("quelle", quelle)
			.getResultStream();
	}

	@Override
	public Stream<Kante> getKantenInBereichNachQuellen(Envelope bereich, Set<QuellSystem> quellen) {
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

	@Override
	public Stream<Kante> getKantenInBereichNachQuelleEagerFetchKnoten(Envelope bereich, QuellSystem quelle) {
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		String hqlStringBuilder = "SELECT kante FROM Kante kante"
			+ CommonQueryLibrary.eagerFetchVonKnoten()
			+ CommonQueryLibrary.eagerFetchNachKnoten()
			+ " WHERE "
			+ CommonQueryLibrary.whereClauseFuerBereichKante()
			+ " AND " + "kante.quelle = :quelle";

		return entityManager.createQuery(hqlStringBuilder, Kante.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("quelle", quelle)
			.getResultStream();
	}

	@Override
	public Stream<Kante> getKantenInBereichNachQuelleEagerFetchKantenAttribute(Envelope bereich, QuellSystem quelle) {
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		String hqlStringBuilder = "SELECT DISTINCT kante FROM Kante kante"
			+ " LEFT OUTER JOIN FETCH kante.kantenAttributGruppe as kag"
			+ " WHERE "
			+ CommonQueryLibrary.whereClauseFuerBereichKante()
			+ " AND " + "kante.quelle = :quelle";

		return entityManager.createQuery(hqlStringBuilder, Kante.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("quelle", quelle)
			.getResultStream();
	}

	@Override
	public List<Kante> getKantenInBereichNachQuelleList(Envelope bereich, QuellSystem quelle) {
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		StringBuilder hqlStringBuilder = queryFuerBereichUndQuellSystem();

		return entityManager.createQuery(hqlStringBuilder.toString(), Kante.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("quelle", quelle)
			.getResultList();
	}

	private StringBuilder queryFuerBereichUndQuellSystem() {
		StringBuilder hqlStringBuilder = new StringBuilder();
		hqlStringBuilder.append("SELECT kante FROM Kante kante ").append(" WHERE ")
			.append(CommonQueryLibrary.whereClauseFuerBereichKante())
			.append(" AND ").append("kante.quelle = :quelle");
		return hqlStringBuilder;
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
	public Set<Kante> getAlleKantenEinesKnotens(Knoten knoten) {
		String hqlStringBuilder = "SELECT DISTINCT kante FROM Kante kante"
			+ " WHERE "
			+ " kante.vonKnoten = :knoten OR kante.nachKnoten = :knoten";

		return entityManager.createQuery(hqlStringBuilder, Kante.class)
			.setParameter("knoten", knoten)
			.getResultStream().collect(Collectors.toSet());
	}

	@Override
	public Set<Kante> getKantenInBereich(MultiPolygon bereich) {
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
	public Set<Kante> getKantenimBereich(Envelope bereich) {
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		String hqlStringBuilder = "SELECT kante FROM Kante kante "
			+ " WHERE "
			+ CommonQueryLibrary.whereClauseFuerBereichKante()
			+ " AND "
			+ CommonQueryLibrary.whereClauseGrundnetz(true);

		return entityManager.createQuery(hqlStringBuilder, Kante.class)
			.setParameter("bereich", bereichAlsPolygon)
			.getResultStream().collect(Collectors.toSet());
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
		// Relevant bedeutet, dass die Kanten in den, fuer die OSM-Ausleitung relevanten Attributen, vom Default abweichen

		String hqlString =
			"SELECT new de.wps.radvis.backend.netz.domain.entity.KanteGeometryView(k.id, k.geometry) "
				+ " FROM Kante k "
				+ " WHERE k.id IN ("
				+ " 	SELECT ka.kanteId FROM KantenAbschnittOsmMapping ka"
				+ " 	WHERE "
				+ " 		intersects(CAST(:bereich as org.locationtech.jts.geom.Geometry), CAST(ka.geometry as org.locationtech.jts.geom.Geometry)) = true "
				+ " 	AND ka.status NOT LIKE 'FIKTIV' "
				+ " 	AND ( ka.radverkehrsfuehrung NOT LIKE 'UNBEKANNT' "
				+ " 	   OR ka.breite IS NOT NULL "
				+ "  	   OR ka.belagArt NOT LIKE 'UNBEKANNT' "
				+ " 	   OR ka.oberflaechenbeschaffenheit NOT LIKE 'UNBEKANNT' "
				+ " 	   OR ka.status NOT LIKE 'UNTER_VERKEHR' "
				+ "  	   OR ka.netzklassen IS NOT NULL OR ka.netzklassen NOT LIKE '' )"
				+ "		) "
				+ "	OR (EXISTS (SELECT 1 FROM Fahrradroute f JOIN f.abschnittsweiserKantenBezug AS fka WHERE f.drouteId IS NOT NULL AND fka.kante.id = k.id)"
				+ "		AND intersects(CAST(:bereich as org.locationtech.jts.geom.Geometry), CAST(k.geometry as org.locationtech.jts.geom.Geometry)) = true "
				+ "		AND k.kantenAttributGruppe.kantenAttribute.status NOT LIKE 'FIKTIV'"
				+ "		)";

		return entityManager.createQuery(hqlString, KanteGeometryView.class)
			.setParameter("bereich", bereich)
			.getResultList();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Stream<KanteOsmMatchWithAttribute> getKanteOsmMatchesWithOsmAttributes(
		double minimaleUeberdeckungFuerAttributAuszeichnung) {
		String sqlString =
			"WITH drouteKantenIds AS (SELECT DISTINCT fk.kante_id AS id FROM fahrradroute f JOIN fahrradroute_kantenabschnitte fk on f.id = fk.fahrradroute_id WHERE f.droute_id IS NOT NULL) "
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
		return resultStream.map(objects ->
			new KanteOsmMatchWithAttribute(
				(Long) objects[0], // kanteId
				(Long) objects[1], // osmWayId
				(String) objects[2], // Status name des Enums
				(String) objects[3], // netzklassen ; separiert zu String gejoint
				(String) objects[4],// Radverkehrsfuehrung name des Enums
				objects[5] != null ? ((BigDecimal) objects[5]).doubleValue() : null, // Breite
				(String) objects[6],// BelagArt name des Enums
				(String) objects[7], // Oberflaechenbeschaffenheit name des Enums
				(Boolean) objects[8] // DRoutenzugehörigkeit
			)
		);
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
			"geoserver_balm_wegweisende_beschilderung_view"
		);
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
}
