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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.EntityManager;

import org.locationtech.jts.geom.Envelope;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.NetzklassenStreckeVonKanten;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzklassenStreckenViewService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;

public class BuildNetzklassenStreckenSignaturViewJob
	extends RadNetzCacheViewJob<List<NetzklassenStreckenSignaturView>, NetzklassenStreckeVonKanten> {
	private final DLMConfigurationProperties dlmConfigurationProperties;

	public BuildNetzklassenStreckenSignaturViewJob(JobExecutionDescriptionRepository repository,
		KantenRepository kantenRepository, NetzklassenStreckenViewService streckenViewService,
		StreckeViewCacheRepository<List<NetzklassenStreckenSignaturView>, NetzklassenStreckeVonKanten> streckeViewCacheRepository,
		EntityManager entityManager,
		DLMConfigurationProperties dlmConfigurationProperties) {
		super(repository, kantenRepository, streckenViewService, streckeViewCacheRepository, entityManager);
		this.dlmConfigurationProperties = dlmConfigurationProperties;
	}

	@Override
	protected Optional<JobStatistik> doRun() {
		List<Envelope> partitions = getPartitionen(dlmConfigurationProperties.getExtentProperty(),
			dlmConfigurationProperties.getPartitionenX());

		final var vollstaendigeNetzklasseStreckeVonKanten = getStreckenVonKanten(partitions);
		streckeViewCacheRepository.loadCache(vollstaendigeNetzklasseStreckeVonKanten);

		return Optional.empty();
	}

	@Override
	protected NetzklassenStreckeVonKanten createStreckeVonKanten(Kante kante) {
		return new NetzklassenStreckeVonKanten(kante, false, false);
	}

	@Override
	protected Set<NetzklasseFilter> getNetzklassenFilter() {
		return Set.of(NetzklasseFilter.RADNETZ, NetzklasseFilter.KOMMUNALNETZ, NetzklasseFilter.KREISNETZ,
			NetzklasseFilter.RADSCHNELLVERBINDUNG, NetzklasseFilter.RADVORRANGROUTEN);
	}
}
