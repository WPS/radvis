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

package de.wps.radvis.backend.common.domain.entity;

import java.time.LocalDateTime;

import de.wps.radvis.backend.common.domain.valueObject.ImportprotokollTyp;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Importprotokoll {
	private final Long id;
	private final LocalDateTime startZeit;
	private final String importQuelle;

	private final ImportprotokollTyp importprotokollTyp;

	public Importprotokoll(JobExecutionDescription jobExecutionDescription,
		ImportprotokollTyp importprotokollTyp,
		String importQuelle) {
		id = jobExecutionDescription.getId();
		startZeit = jobExecutionDescription.getExecutionStart();
		this.importQuelle = importQuelle;
		this.importprotokollTyp = importprotokollTyp;
	}
}
