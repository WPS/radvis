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

package de.wps.radvis.backend.netzfehler.domain;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.netzfehler.domain.entity.Anpassungswunsch;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschStatus;
import de.wps.radvis.backend.netzfehler.domain.valueObject.KonsistenzregelVerletzungReferenz;

public interface AnpassungswunschRepository
	extends CrudRepository<Anpassungswunsch, Long> {

	Iterable<Anpassungswunsch> findAllByStatusIsNotIn(List<AnpassungswunschStatus> anpassungswunschStatusList);

	Stream<Anpassungswunsch> findByKonsistenzregelVerletzungReferenzIn(
		List<KonsistenzregelVerletzungReferenz> referenzen);
}
