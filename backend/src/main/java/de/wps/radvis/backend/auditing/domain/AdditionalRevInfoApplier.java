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

import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.context.SecurityContextHolder;

import de.wps.radvis.backend.auditing.domain.entity.RevInfo;
import de.wps.radvis.backend.authentication.domain.RadVisAuthentication;
import de.wps.radvis.backend.authentication.domain.entity.RadVisUserDetails;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;

public class AdditionalRevInfoApplier implements RevisionListener {

	@Override
	public void newRevision(Object revisionEntity) {
		RevInfo revInfo = (RevInfo) revisionEntity;

		RadVisAuthentication authentication = (RadVisAuthentication) SecurityContextHolder.getContext()
			.getAuthentication();

		if (authentication != null) {
			RadVisUserDetails userDetails = (RadVisUserDetails) authentication.getPrincipal();
			revInfo.setBenutzer(userDetails.getBenutzer());
		}
		AuditingContext auditingContext = AdditionalRevInfoHolder.getAuditingContext();

		if (auditingContext == null) {
			throw new RuntimeException("Auditing Context MUSS bei Änderungen an geauditen Entity gesetzt sein!");
		}

		revInfo.setAuditingContext(auditingContext);

		JobExecutionDescription jobExecutionDescription = AdditionalRevInfoHolder.getJobExecutionDescription();

		revInfo.setJobExecutionDescription(jobExecutionDescription);
	}
}
