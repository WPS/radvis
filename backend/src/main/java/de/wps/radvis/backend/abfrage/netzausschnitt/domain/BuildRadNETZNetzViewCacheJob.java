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

package de.wps.radvis.backend.abfrage.netzausschnitt.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.locationtech.jts.geom.Envelope;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzMapView;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.StreckeVonKanten;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.StreckenViewService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;

public class BuildRadNETZNetzViewCacheJob extends RadNetzCacheViewJob<NetzMapView, StreckeVonKanten> {
	private final DLMConfigurationProperties dlmConfigurationProperties;

	public BuildRadNETZNetzViewCacheJob(JobExecutionDescriptionRepository jobExecutionDescriptionRepository,
		KantenRepository kantenRepository, StreckenViewService streckenViewService,
		StreckeViewCacheRepository<NetzMapView, StreckeVonKanten> streckeViewCacheRepository,
		EntityManager entityManager, DLMConfigurationProperties dlmConfigurationProperties) {
		super(jobExecutionDescriptionRepository, kantenRepository, streckenViewService, streckeViewCacheRepository,
			entityManager);
		this.dlmConfigurationProperties = dlmConfigurationProperties;
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		List<Envelope> partitions = getPartitionen(dlmConfigurationProperties.getExtentProperty(),
			dlmConfigurationProperties.getPartitionenX());

		final Set<StreckeVonKanten> streckenVonKanten = new HashSet<>(getStreckenVonKanten(partitions));
		streckeViewCacheRepository.loadCache(streckenVonKanten);

		return Optional.empty();
	}

	@Override
	protected StreckeVonKanten createStreckeVonKanten(Kante kante) {
		return new StreckeVonKanten(kante);
	}

	@Override
	protected Set<NetzklasseFilter> getNetzklassenFilter() {
		return Set.of(NetzklasseFilter.RADNETZ);
	}
}
