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

package de.wps.radvis.backend.auditing.domain;

import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;

public class AdditionalRevInfoHolder {

	private static final ThreadLocal<AuditingContext> AUDITING_CONTEXT = new ThreadLocal<>();

	private static final ThreadLocal<JobExecutionDescription> JOB_EXECUTION_DESCRIPTION = new ThreadLocal<>();

	public static void setAuditingContext(AuditingContext auditingContext) {
		AUDITING_CONTEXT.set(auditingContext);
	}

	public static AuditingContext getAuditingContext() {
		return AUDITING_CONTEXT.get();
	}

	public static void setJobExecutionDescription(JobExecutionDescription jobExecutionDescription) {
		JOB_EXECUTION_DESCRIPTION.set(jobExecutionDescription);
	}

	public static JobExecutionDescription getJobExecutionDescription() {
		return JOB_EXECUTION_DESCRIPTION.get();
	}

	public static void clear() {
		AUDITING_CONTEXT.remove();
		JOB_EXECUTION_DESCRIPTION.remove();
	}
}
