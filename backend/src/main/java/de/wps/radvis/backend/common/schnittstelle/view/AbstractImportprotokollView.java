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

package de.wps.radvis.backend.common.schnittstelle.view;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonRawValue;

import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractImportprotokollView {
	private final Long id;
	private final LocalDateTime startZeit;
	private final LocalDateTime endZeit;
	@JsonRawValue
	private final String statistik;

	public AbstractImportprotokollView(JobExecutionDescription jobExecutionDescription) {
		id = jobExecutionDescription.getId();
		startZeit = jobExecutionDescription.getExecutionStart();
		endZeit = jobExecutionDescription.getExecutionEnd();
		statistik = jobExecutionDescription.getStatistic();
	}
}
