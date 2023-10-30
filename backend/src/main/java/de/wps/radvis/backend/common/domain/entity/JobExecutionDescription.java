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

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobExecutionDescription extends AbstractEntity {

	// @Column(nullable = false)
	@NonNull
	private String name;

	@Column(nullable = false)
	@NonNull
	private String inputSummary;

	@Column(columnDefinition = "TIMESTAMP", nullable = false)
	@NonNull
	private LocalDateTime executionStart;

	@Column(columnDefinition = "TIMESTAMP", nullable = false)
	@NonNull
	private LocalDateTime executionEnd;

	private String statistic;

	public Duration getDuration() {
		return Duration.between(executionStart, executionEnd);
	}

	@Builder
	private JobExecutionDescription(Long id, @NonNull String name, @NonNull String inputSummary,
		@NonNull LocalDateTime executionStart, @NonNull LocalDateTime executionEnd, String statistic) {
		super(id);
		this.name = name;
		this.inputSummary = inputSummary;
		this.executionStart = executionStart;
		this.executionEnd = executionEnd;
		this.statistic = statistic;
	}
}
