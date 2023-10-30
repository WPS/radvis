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

package de.wps.radvis.backend.auditing.schnittstelle;

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.WithAuditing;

@Aspect
@Order(1)
@Component
public class WithAuditingAspect {

	@Pointcut("@annotation(de.wps.radvis.backend.auditing.domain.WithAuditing)")
	public void methodsWithAuditing() {
	}

	@Around("methodsWithAuditing()")
	public Object addAuditingContext(ProceedingJoinPoint point) throws Throwable {
		MethodSignature signature = (MethodSignature) point.getSignature();
		Method method = signature.getMethod();

		WithAuditing withAuditing = method.getAnnotation(WithAuditing.class);

		AdditionalRevInfoHolder.setAuditingContext(withAuditing.context());

		Object result;
		try {
			result = point.proceed();
		} finally {
			AdditionalRevInfoHolder.clear();
		}
		return result;
	}
}
