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

package de.wps.radvis.backend.common.domain;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Order(1)
@Component
public class SuppressChangedEventsAspect {
	@Pointcut("@annotation(de.wps.radvis.backend.common.domain.annotation.SuppressChangedEvents)")
	public void methodsWithAnnotation() {
	}

	@Around("methodsWithAnnotation()")
	public Object suppressChangedEvents(ProceedingJoinPoint point) throws Throwable {
		RadVisDomainEventPublisher.suppressEvents();

		Object result;
		try {
			result = point.proceed();
		} finally {
			RadVisDomainEventPublisher.reset();
		}
		return result;
	}
}
