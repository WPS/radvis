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

import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Order(1)
@Component
@Slf4j
public class WithFehlercodeAspect {
	/**
	 * s. patterns in logback-spring.xml
	 */
	private static final String FEHLERCODE_MDC_KEY = "fehlercode";

	@Pointcut("@annotation(de.wps.radvis.backend.common.domain.annotation.WithFehlercode)")
	public void methodsWithFehlercodes() {
	}

	@Pointcut("@within(de.wps.radvis.backend.common.domain.annotation.WithFehlercode)")
	public void inClassesWithFehlercodes() {
	}

	@Around("methodsWithFehlercodes() || inClassesWithFehlercodes()")
	public Object addFehlercode(ProceedingJoinPoint point) throws Throwable {
		String fehlercodeBefore = MDC.get(FEHLERCODE_MDC_KEY);

		MethodSignature signature = (MethodSignature) point.getSignature();
		Method method = signature.getMethod();

		WithFehlercode withFehlercode = method.getAnnotation(WithFehlercode.class);
		if (withFehlercode == null) {
			withFehlercode = method.getDeclaringClass().getAnnotation(WithFehlercode.class);
		}
		MDC.put(FEHLERCODE_MDC_KEY, withFehlercode.value().getCodeNumber());

		Object result;
		try {
			result = point.proceed();
		} catch (Throwable t) {
			log.error(t.getMessage(), t);
			throw t;
		} finally {
			MDC.put(FEHLERCODE_MDC_KEY, fehlercodeBefore);
		}
		return result;
	}
}
