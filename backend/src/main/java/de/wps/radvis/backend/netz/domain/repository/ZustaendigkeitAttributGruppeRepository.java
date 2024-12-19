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

import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;

public interface ZustaendigkeitAttributGruppeRepository extends CrudRepository<ZustaendigkeitAttributGruppe, Long>,
	RevisionRepository<ZustaendigkeitAttributGruppe, Long, Long> {

	@Query(value = "with "
		+ "	abschnitte AS "
		+ "		(SELECT za.zustaendigkeit_attribut_gruppe_id, ST_Length(st_linesubstring(k.geometry, za.von, za.bis)) as laenge "
		+ "		 	FROM zustaendigkeit_attribut_gruppe_zustaendigkeit_attribute za JOIN kante k ON k.zustaendigkeit_attributgruppe_id=za.zustaendigkeit_attribut_gruppe_id "
		+ "		 	WHERE (k.quelle='DLM' OR k.quelle='RadVis')"
		+ "		)"
		+ "SELECT * from zustaendigkeit_attribut_gruppe where id in (select zustaendigkeit_attribut_gruppe_id from abschnitte WHERE laenge < ?1)", nativeQuery = true)
	List<ZustaendigkeitAttributGruppe> findAllWithSegmenteKleinerAls(double maximaleSegmentLaenge);

}
