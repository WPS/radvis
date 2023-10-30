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

package de.wps.radvis.backend.abfrage.fehlerprotokoll.domain;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import de.wps.radvis.backend.abfrage.fehlerprotokoll.domain.entity.GeoserverFehlerprotokoll;
import de.wps.radvis.backend.abfrage.fehlerprotokoll.domain.repository.GeoserverFehlerprotokollRepository;
import de.wps.radvis.backend.abfrage.fehlerprotokoll.domain.service.FehlerprotokollAbfrageService;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractJob;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;

public class GeoserverFehlerprotokolleUpdateJob extends AbstractJob {

	private final FehlerprotokollAbfrageService fehlerprotokollAbfrageService;

	private final GeoserverFehlerprotokollRepository geoserverFehlerprotokollRepository;

	public GeoserverFehlerprotokolleUpdateJob(
		JobExecutionDescriptionRepository repository,
		FehlerprotokollAbfrageService fehlerprotokollAbfrageService,
		GeoserverFehlerprotokollRepository geoserverFehlerprotokollRepository) {
		super(repository);
		this.fehlerprotokollAbfrageService = fehlerprotokollAbfrageService;
		this.geoserverFehlerprotokollRepository = geoserverFehlerprotokollRepository;
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		geoserverFehlerprotokollRepository.deleteAll();
		List<FehlerprotokollEintrag> alleFehlerprotokolle = fehlerprotokollAbfrageService.getAlleFehlerprotokolle();
		geoserverFehlerprotokollRepository.saveAll(
			alleFehlerprotokolle.stream()
				.map(GeoserverFehlerprotokoll::new)
				.collect(Collectors.toList()));

		return Optional.empty();
	}
}
