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

package de.wps.radvis.backend.application.metrics;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j(topic = "ExecutionTimeLogger")
@ConditionalOnExpression("${aspect.enabled:true}")
public class ExecutionTimeMethodInterceptor {

	@Pointcut("bean(*Repository*) && within(de.wps.radvis.backend..*)")
	public void monitor() {
	}

	@Around("monitor()")
	@SuppressWarnings("rawtypes")
	public Object executionTime(ProceedingJoinPoint point) throws Throwable {
		long startTime = System.currentTimeMillis();
		Object object = point.proceed();
		long endtime = System.currentTimeMillis();
		Signature signature = point.getSignature();
		String args = Arrays.stream(point.getArgs())
			.map(obj -> Objects.isNull(obj) ? null : obj.toString())
			.collect(Collectors.joining(","));
		String size = null;
		if (object instanceof Collection) {
			size = String.valueOf(((Collection) object).size());
		}
		log.info(
			"Called: " + signature.getDeclaringType().getSimpleName() + "." + signature.getName()
				+ "(" + args + "). " + (size != null ? "Records: " + size + ". " : "")
				+ "Time taken for Execution is : " + (endtime - startTime) + "ms");
		return object;
	}
}