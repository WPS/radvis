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

package de.wps.radvis.backend.fahrradzaehlstelle.domain;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.Channel;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.entity.Fahrradzaehlstelle;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.BetreiberEigeneId;
import de.wps.radvis.backend.fahrradzaehlstelle.domain.valueObject.Zeitstempel;

public interface FahrradzaehlstelleRepository extends CrudRepository<Fahrradzaehlstelle, Long> {

	List<Fahrradzaehlstelle> findAllByBetreiberEigeneIdIn(Set<BetreiberEigeneId> betreiberEigeneIds);

	@Query(value = "SELECT MAX(index(channel.fahrradzaehlDaten)) FROM Channel channel")
	Optional<Zeitstempel> findeLetztesImportDatum();

	@Query("SELECT channel FROM Channel channel WHERE channel.id IN (:channelIds)")
	List<Channel> getChannels(List<Long> channelIds);
}
