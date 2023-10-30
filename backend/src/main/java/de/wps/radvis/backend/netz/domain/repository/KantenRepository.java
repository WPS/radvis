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

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteElevationView;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;

public interface KantenRepository extends CrudRepository<Kante, Long>, CustomKantenRepository {

	String GEOSERVER_RADVISNETZ_MAT_VIEW_NAME = "geoserver_radvisnetz_kante_materialized_view";

	String GEOSERVER_RADVISNETZ_ABSCHNITTE_MAT_VIEW_NAME = "geoserver_radvisnetz_kante_abschnitte_materialized_view";
	String GEOSERVER_BALM_KANTEN_VIEW_NAME = "geoserver_balm_kanten_view";

	@Query("SELECT count(distinct kante.id) "
		+ "FROM Kante kante "
		+ "WHERE kante.vonKnoten = ?1 "
		+ "OR kante.nachKnoten = ?1 ")
	long getAnzahlAdjazenterKanten(Knoten knoten);

	@Query("SELECT kante "
		+ "FROM Kante kante "
		+ "WHERE CAST(kante.geometry as org.locationtech.jts.geom.Geometry)=?1 ")
	List<Kante> getKantenByLineString(LineString lineString);

	Stream<Kante> findKanteByQuelle(QuellSystem quelle);

	@Query(
		value =
			"SELECT kante.id AS id, kante.geometry AS geometry"
				+ " FROM Kante kante "
				+ "WHERE (kante.quelle = 'DLM' OR kante.quelle = 'RadVis')"
				+ " AND (kante.geometry3d IS NULL OR NOT st_force2d(kante.geometry3d) = kante.geometry)"
				+ " LIMIT 10000", nativeQuery = true)
	Slice<KanteElevationView> findFirst10ThousandByQuelleDLMOrQuelleRadVisAndOutdated3dGeometry();

	@Query(
		value =
			"SELECT COUNT(*)"
				+ " FROM Kante kante "
				+ "WHERE (kante.quelle = 'DLM' OR kante.quelle = 'RadVis')"
				+ " AND (kante.geometry3d IS NULL OR NOT st_force2d(kante.geometry3d) = kante.geometry)", nativeQuery = true)
	int countAllByQuelleDLMOrQuelleRadVisAndOutdated3dGeometry();

	@Query("SELECT kante "
		+ "FROM Kante kante "
		+ "WHERE kante.vonKnoten = ?1 "
		+ "OR kante.nachKnoten = ?1 ")
	List<Kante> getAdjazenteKanten(Knoten knoten);

	@Query("SELECT kante FROM Kante kante WHERE kante.kantenAttributGruppe.id = ?1")
	Kante findByKantenAttributGruppeId(long kantenAttributGruppeId);

	@Query("SELECT kante.geometry "
		+ "FROM Kante kante "
		+ "WHERE kante.id IN ?1")
	List<Geometry> getKanteGeometriesByIds(List<Long> kanteIds);

	@Query("SELECT id FROM Kante")
	List<Long> getAllKanteIds();

	List<Kante> findAllByDlmIdIn(List<DlmId> dlmId);

	List<Kante> findAllByQuelleEqualsAndKantenLaengeInCmLessThan(QuellSystem quelle, int laengeInCm);

	@Query(value = "Select kante FROM Kante kante JOIN kante.vonKnoten von_knoten JOIN kante.nachKnoten nach_knoten"
		+ " WHERE (kante.quelle = de.wps.radvis.backend.common.domain.valueObject.QuellSystem.DLM"
		+ " OR kante.quelle = de.wps.radvis.backend.common.domain.valueObject.QuellSystem.RadVis)"
		+ " AND (st_distance(st_endpoint(kante.geometry),nach_knoten.point)>?1"
		+ " OR st_distance(st_startpoint(kante.geometry),von_knoten.point)>?1)"
		+ " ORDER BY kante.id ASC")
	List<Kante> findKantenWhereLinestringEndsAreNotOnKnoten(double tolerance);

	@Query(value = "SELECT k.netzklassen FROM KanteNetzklassenView k "
		+ "WHERE k.id = ?1")
	Optional<String> getNetzklassenVonKante(Long kanteId);
}
