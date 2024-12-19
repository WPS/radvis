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

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.netz.domain.entity.Knoten;

public interface KnotenRepository extends CrudRepository<Knoten, Long>, CustomKnotenRepository {
	String GEOSERVER_BALM_KNOTEN_VIEW_NAME = "geoserver_balm_knoten_view";

	@Query("From Knoten knoten where knoten.quelle=de.wps.radvis.backend.common.domain.valueObject.QuellSystem.DLM AND not exists (select kante from Kante kante where kante.quelle = de.wps.radvis.backend.common.domain.valueObject.QuellSystem.DLM and (kante.vonKnoten = knoten or kante.nachKnoten = knoten))")
	List<Knoten> findDlmKnotenWithoutDlmKanten();

	@Query(value = "select * from knoten where id!=?1 AND st_dwithin(point, (select point from knoten where id=?1), ?2) ORDER BY point <-> (select point from knoten where id=?1)", nativeQuery = true)
	List<Knoten> findErsatzKnotenCandidates(Long fuerKnotenId, double maximaleAbweichung);
}
