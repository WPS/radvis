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

package de.wps.radvis.backend.wegweisendeBeschilderung.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.Importprotokoll;
import de.wps.radvis.backend.common.domain.service.ImportprotokollService;
import de.wps.radvis.backend.common.domain.valueObject.ImportprotokollTyp;

public class WegweisendeBeschilderungService implements ImportprotokollService {

	private final JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	public WegweisendeBeschilderungService(JobExecutionDescriptionRepository jobExecutionDescriptionRepository) {
		this.jobExecutionDescriptionRepository = jobExecutionDescriptionRepository;
	}

	@Override
	public List<Importprotokoll> getAllImportprotokolleAfter(LocalDateTime after) {
		List<String> names = List.of(WegweisendeBeschilderungImportJob.JOB_NAME);

		return jobExecutionDescriptionRepository.findAllByNameInAfterOrderByExecutionStartDesc(after, names).stream()
			.map(
				jobExecutionDescription -> {
					return new Importprotokoll(
						jobExecutionDescription,
						ImportprotokollTyp.WEGWEISENDE_BESCHILDERUNG,
						"VP-Info");
				})
			.collect(Collectors.toList());
	}

}
