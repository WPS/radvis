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

package de.wps.radvis.backend.konsistenz.pruefung.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Polygon;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.konsistenz.pruefung.domain.entity.KonsistenzregelVerletzung;

public interface KonsistenzregelVerletzungsRepository extends CrudRepository<KonsistenzregelVerletzung, Long> {

	@Query("SELECT kv FROM KonsistenzregelVerletzung kv "
		+ "WHERE kv.typ IN :typen "
		+ "AND intersects(CAST(kv.konsistenzregelVerletzungsDetails.position AS org.locationtech.jts.geom.Geometry), CAST(:bereich AS org.locationtech.jts.geom.Geometry)) = true")
	Stream<KonsistenzregelVerletzung> findAllByTypInAndInBereich(Set<String> typen, Polygon bereich);

	@Query("SELECT kv FROM KonsistenzregelVerletzung kv "
		+ "WHERE kv.typ = :typ "
		+ "AND kv.konsistenzregelVerletzungsDetails.identity = :identity")
	Optional<KonsistenzregelVerletzung> findByTypAndIdentity(String typ, String identity);

	List<KonsistenzregelVerletzung> findAllByTyp(String typ);

	@Modifying
	@Query("DELETE FROM KonsistenzregelVerletzung "
		+ "WHERE typ = :typ "
		+ "AND konsistenzregelVerletzungsDetails.identity IN :identity")
	int deleteAllByTypAndIdentityIn(String typ, List<String> identity);
}
