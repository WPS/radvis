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

package de.wps.radvis.backend.quellimport.ttsib.domain;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.quellimport.ttsib.domain.entity.TtSibAbschnittOderAst;

public interface TtSibRepository extends CrudRepository<TtSibAbschnittOderAst, Long> {

	// Aufgrund von seltsamen Foreign-Key-Beziehungen benötigt es zwei TRUNCATES, um
	// wirklich alle TT-SIB-Elemente zu erwischen
	@Modifying
	@Query(nativeQuery = true, value = "TRUNCATE TABLE tt_sib_querschnitt CASCADE;"
		+ "TRUNCATE TABLE tt_sib_abschnitt_oder_ast CASCADE;")
	void truncateCascadeAllAoAs();
}
