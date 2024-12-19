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
import org.springframework.data.repository.history.RevisionRepository;

import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;

public interface GeschwindigkeitAttributGruppeRepository extends CrudRepository<GeschwindigkeitAttributGruppe, Long>,
	RevisionRepository<GeschwindigkeitAttributGruppe, Long, Long> {
	@Query(value = "with "
		+ "	abschnitte AS "
		+ "		(SELECT ga.geschwindigkeit_attribut_gruppe_id, ST_Length(st_linesubstring(k.geometry, ga.von, ga.bis)) as laenge "
		+ "		 	FROM geschwindigkeit_attribut_gruppe_geschwindigkeit_attribute ga JOIN kante k ON k.geschwindigkeit_attributgruppe_id=ga.geschwindigkeit_attribut_gruppe_id "
		+ "		 	WHERE (k.quelle='DLM' OR k.quelle='RadVis')"
		+ "		)"
		+ "SELECT * from geschwindigkeit_attribut_gruppe where id in (select geschwindigkeit_attribut_gruppe_id from abschnitte WHERE laenge < ?1)", nativeQuery = true)
	List<GeschwindigkeitAttributGruppe> findAllWithSegmenteKleinerAls(double maximaleSegmentLaenge);
}
