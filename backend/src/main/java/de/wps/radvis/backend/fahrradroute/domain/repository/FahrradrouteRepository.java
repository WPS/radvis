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

package de.wps.radvis.backend.fahrradroute.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.common.domain.repository.FahrradrouteFilterRepository;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.TfisImportProblem;
import de.wps.radvis.backend.fahrradroute.domain.entity.ToubizImportProblem;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;
import de.wps.radvis.backend.matching.domain.repository.GraphhopperRoutingRepository;
import de.wps.radvis.backend.netz.domain.entity.Kante;

public interface FahrradrouteRepository
	extends CrudRepository<Fahrradroute, Long>, CustomFahrradrouteRepository, FahrradrouteFilterRepository {

	String GEOSERVER_BALM_FAHRRADROUTEN_VIEW_NAME = "geoserver_balm_fahrradrouten_view";

	@Query("SELECT distinct fahrradroute FROM Fahrradroute fahrradroute " +
		"WHERE fahrradroute.id IN (" +
		"	SELECT f1.id AS fid FROM Fahrradroute f1" +
		"	LEFT JOIN f1.abschnittsweiserKantenBezug route_kanten " +
		"	WHERE route_kanten.kante.id IN :kantenIds" +
		"	UNION" +
		"	SELECT f2.id AS fid FROM Fahrradroute f2" +
		"	LEFT JOIN f2.varianten varianten " +
		"	LEFT JOIN varianten.abschnittsweiserKantenBezug varianten_kanten " +
		"	WHERE varianten_kanten.kante.id IN :kantenIds" +
		")")
	List<Fahrradroute> findByKanteIdInNetzBezug(Collection<Long> kantenIds);

	Stream<Fahrradroute> findAllByFahrradrouteTypNot(FahrradrouteTyp fahrradrouteTyp);

	Stream<Fahrradroute> findAllByKategorie(Kategorie kategorie);

	Optional<Fahrradroute> findByToubizId(ToubizId toubizId);

	Optional<Fahrradroute> findByTfisId(TfisId tfisId);

	Optional<Fahrradroute> findByIdAndGeloeschtFalse(Long fahrradrouteId);

	@Query("SELECT distinct fahrradroute.toubizId FROM Fahrradroute fahrradroute WHERE fahrradroute.fahrradrouteTyp = 'TOUBIZ_ROUTE'")
	Set<ToubizId> findAllToubizIdsWithoutLandesradfernwege();

	@Query("SELECT distinct fahrradroute.toubizId FROM Fahrradroute fahrradroute "
		+ "WHERE fahrradroute.toubizId IS NOT NULL "
		+ "AND fahrradroute.kategorie = de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie.LANDESRADFERNWEG")
	Set<ToubizId> findAllToubizIdsOfLandesradfernwege();

	@Query("SELECT fahrradroute FROM Fahrradroute fahrradroute WHERE fahrradroute.netzbezugLineString IS NULL")
	Stream<Fahrradroute> findAllWithoutNetzbezugLineString();

	void deleteAllByToubizIdIn(Set<ToubizId> toubizIds);

	long deleteAllByFahrradrouteTypAndTfisIdNotIn(FahrradrouteTyp fahrradrouteTyp, Set<TfisId> TfisIds);

	@Query("SELECT new de.wps.radvis.backend.fahrradroute.domain.entity.ToubizImportProblem("
		+ "f.id, f.name.name, f.iconLocation, f.originalGeometrie, f.zuletztBearbeitet, CASE WHEN f.netzbezugLineString IS NULL THEN false ELSE true END) "
		+ "FROM Fahrradroute f "
		+ "WHERE f.fahrradrouteTyp = de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp.TOUBIZ_ROUTE "
		+ "AND (f.netzbezugLineString IS NULL OR f.fahrradroutenMatchingAndRoutingInformation.abweichendeSegmente IS NOT NULL)"
		+ "AND f.originalGeometrie IS NOT NULL AND f.iconLocation IS NOT NULL")
	List<ToubizImportProblem> findAllToubizImportProbleme();

	@Query("SELECT new de.wps.radvis.backend.fahrradroute.domain.entity.ToubizImportProblem("
		+ "f.id, f.name.name, f.iconLocation, f.originalGeometrie, f.zuletztBearbeitet, CASE WHEN f.netzbezugLineString IS NULL THEN false ELSE true END) "
		+ "FROM Fahrradroute f "
		+ "WHERE f.fahrradrouteTyp = de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp.TOUBIZ_ROUTE "
		+ "AND (f.netzbezugLineString IS NULL OR f.fahrradroutenMatchingAndRoutingInformation.abweichendeSegmente IS NOT NULL) "
		+ "AND f.originalGeometrie IS NOT NULL AND f.iconLocation IS NOT NULL "
		+ "AND intersects(CAST(f.iconLocation AS org.locationtech.jts.geom.Geometry), CAST(:bereich AS org.locationtech.jts.geom.Geometry)) = true")
	List<ToubizImportProblem> findAllToubizImportProblemeInBereich(Polygon bereich);

	@Query("SELECT new de.wps.radvis.backend.fahrradroute.domain.entity.TfisImportProblem("
		+ "f.id, f.name.name, f.originalGeometrie, f.zuletztBearbeitet, 'TFIS-Import Fehler', CASE WHEN f.abschnittsweiserKantenBezug IS EMPTY THEN false ELSE true END) "
		+ "FROM Fahrradroute f "
		+ "WHERE f.fahrradrouteTyp = de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp.TFIS_ROUTE "
		+ "AND f.originalGeometrie IS NOT NULL "
		+ "AND f.netzbezugLineString IS NULL")
	List<TfisImportProblem> findAllTfisImportProbleme();

	@Query("SELECT new de.wps.radvis.backend.fahrradroute.domain.entity.TfisImportProblem("
		+ "f.id, f.name.name, f.originalGeometrie, f.zuletztBearbeitet, 'TFIS-Import Fehler', CASE WHEN f.abschnittsweiserKantenBezug IS EMPTY THEN false ELSE true END) "
		+ "FROM Fahrradroute f "
		+ "WHERE f.fahrradrouteTyp = de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp.TFIS_ROUTE "
		+ "AND f.originalGeometrie IS NOT NULL "
		+ "AND f.netzbezugLineString IS NULL "
		+ "AND intersects(CAST(f.originalGeometrie AS org.locationtech.jts.geom.Geometry), CAST(:bereich AS org.locationtech.jts.geom.Geometry)) = true")
	List<TfisImportProblem> findAllTfisImportProblemeInBereich(Polygon bereich);

	// Wir gehen davon aus, dass der NetzbezugLinestring für nicht importierte LRFW nicht NULL sein kann
	@Query("SELECT new de.wps.radvis.backend.fahrradroute.domain.entity.TfisImportProblem("
		+ "f.id, f.name.name, f.originalGeometrie, f.zuletztBearbeitet, 'Landesradfernweg: Fehler beim Import aus TFIS', CASE WHEN f.abschnittsweiserKantenBezug IS EMPTY THEN false ELSE true END) "
		+ "FROM Fahrradroute f "
		+ "WHERE f.kategorie = de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie.LANDESRADFERNWEG "
		+ "AND f.originalGeometrie IS NOT NULL "
		+ "AND f.netzbezugLineString IS NULL")
	List<TfisImportProblem> findAllLrfwImportProbleme();

	// Wir gehen davon aus, dass der NetzbezugLinestring für nicht importierte LRFW nicht NULL sein kann
	@Query("SELECT new de.wps.radvis.backend.fahrradroute.domain.entity.TfisImportProblem("
		+ "f.id, f.name.name, f.originalGeometrie, f.zuletztBearbeitet, 'Landesradfernweg: Fehler beim Import aus TFIS', CASE WHEN f.abschnittsweiserKantenBezug IS EMPTY THEN false ELSE true END) "
		+ "FROM Fahrradroute f "
		+ "WHERE f.kategorie = de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie.LANDESRADFERNWEG "
		+ "AND f.originalGeometrie IS NOT NULL "
		+ "AND f.netzbezugLineString IS NULL "
		+ "AND intersects(CAST(f.originalGeometrie AS org.locationtech.jts.geom.Geometry), CAST(:bereich AS org.locationtech.jts.geom.Geometry)) = true")
	List<TfisImportProblem> findAllLrfwImportProblemeInBereich(Polygon bereich);

	@Query("SELECT fahrradroute.tfisId FROM Fahrradroute fahrradroute WHERE fahrradroute.netzbezugLineString IS NULL AND fahrradroute.tfisId IS NOT NULL")
	Set<TfisId> findAllTfisIdsWithoutNetzbezugLineString();

	@Query("SELECT fahrradroute.tfisId FROM Fahrradroute fahrradroute WHERE fahrradroute.tfisId IS NOT NULL"
		+ " AND fahrradroute.fahrradrouteTyp = de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp.TFIS_ROUTE")
	Set<TfisId> findAllTfisIdsWithoutLandesradfernwege();

	@Query("SELECT fahrradroute.tfisId FROM Fahrradroute fahrradroute WHERE fahrradroute.tfisId IS NOT NULL"
		+ " AND fahrradroute.kategorie = de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie.LANDESRADFERNWEG")
	Set<TfisId> findAllTfisIdsOfLandesradfernwege();

	@Query("SELECT fahrradroute.tfisId FROM Fahrradroute fahrradroute JOIN fahrradroute.varianten variante WHERE fahrradroute.tfisId IS NOT NULL"
		+ " AND variante.geometrie IS NULL AND variante.tfisId IS NOT NULL")
	Set<TfisId> findAllTfisIdsWithVariantenWithoutGeometrie();

	@Query(nativeQuery = true, value = "SELECT name FROM fahrradroute_aud AS f, rev_info AS r WHERE f.revtype=0 AND r.id=f.rev AND r.job_execution_description_id=?1")
	List<String> findAllNamesOfInsertedByJobId(Long jobId);

	@Query(nativeQuery = true, value = "SELECT f_vorher.name FROM fahrradroute_aud AS f_vorher, fahrradroute_aud AS f_nachher, rev_info AS r"
		+ " WHERE f_nachher.revtype=2 AND r.id=f_nachher.rev AND r.job_execution_description_id=?1 AND f_vorher.id=f_nachher.id AND "
		+ "f_vorher.rev=(SELECT MAX(f.rev) FROM fahrradroute_aud AS f WHERE f.id=f_nachher.id AND f.rev < f_nachher.rev)")
	List<String> findAllNamesOfDeletedByJobId(Long jobId);

	List<Fahrradroute> findAllByFahrradrouteTypAndTfisIdNotIn(FahrradrouteTyp tfisRoute, Set<TfisId> alreadySaved);

	@Query("Select k FROM Fahrradroute f JOIN f.abschnittsweiserKantenBezug kb JOIN kb.kante k LEFT OUTER JOIN FETCH k.vonKnoten as k1 LEFT OUTER JOIN FETCH k.nachKnoten as k2 WHERE f.id=?1")
	Set<Kante> getKantenWithKnotenByFahrradroute(Long id);

	@Modifying
	@Query("UPDATE Fahrradroute f SET f.customProfileId = " + GraphhopperRoutingRepository.DEFAULT_PROFILE_ID
		+ " WHERE f.customProfileId IN (:customProfileIds)")
	void setCustomRoutingProfileIdToDefaultWhereCustomRoutingProfileIdIn(Collection<Long> customProfileIds);

	/**
	 * @param fahrradrouteIds
	 * @return Alle Geometrien von Fahrradrouten gemäß IDs, die Geometrie besitzen (also möglicherweise leer).
	 */
	@Query("SELECT COALESCE(f.netzbezugLineString, f.originalGeometrie) FROM Fahrradroute f WHERE f.id IN ?1 AND (f.netzbezugLineString IS NOT NULL OR f.originalGeometrie IS NOT NULL)")
	List<Geometry> getAllGeometries(List<Long> fahrradrouteIds);
}
