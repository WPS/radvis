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

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import de.wps.radvis.backend.fahrradroute.domain.dbView.FahrradrouteListenDbOhneGeomView;
import de.wps.radvis.backend.fahrradroute.domain.dbView.FahrradrouteListenDbView;

public interface FahrradrouteViewRepository extends Repository<FahrradrouteListenDbView, Long> {
	List<FahrradrouteListenDbView> findAll();

	@Query("""
		SELECT dbView.id as id,
			dbView.name as name,
			dbView.kategorie as kategorie,
			dbView.fahrradrouteTyp as fahrradrouteTyp,
			dbView.verantwortlicheOrganisationName as verantwortlicheOrganisationName,
			dbView.abstieg as abstieg,
			dbView.anstieg as anstieg,
			dbView.kurzbeschreibung as kurzbeschreibung,
			dbView.homepage as homepage,
			dbView.lizenz as lizenz
		 FROM FahrradrouteListenDbView dbView
		""")
	List<FahrradrouteListenDbOhneGeomView> findAllWithoutFetchingGeom();

	List<FahrradrouteListenDbView> findAllByIdIn(List<Long> ids);
}
