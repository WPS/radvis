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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "ExecutionTimeLogger")
public class ExecutionTimeRequestInterceptor implements HandlerInterceptor {


	@Autowired
	@Qualifier("averageResponseTime")
	private MovingAverageSupplier averageResponseTime;

	@Autowired
	@Qualifier("maxResponseTime")
	private MaxSupplier maxResponseTime;

	// before the actual handler will be executed
	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		long startTime = System.currentTimeMillis();
		request.setAttribute("startTime", startTime);

		return true;
	}

	// after the handler is executed
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
		ModelAndView modelAndView) {

		long startTime = (Long) request.getAttribute("startTime");
		long endTime = System.currentTimeMillis();
		long executeTime = endTime - startTime;

		averageResponseTime.record((double) executeTime / 1000d);
		maxResponseTime.record((double) executeTime / 1000d);

		log.trace("[" + request.getRequestURL().toString() + "] executeTime : " + executeTime + "ms");

		if (executeTime > 2000) {
			log.info(
				"Long request: [" + request.getRequestURL().toString() + "] executeTime : " + executeTime + "ms");
		}
	}
}
