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

package de.wps.radvis.backend.application;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import de.wps.radvis.backend.application.metrics.ExecutionTimeRequestInterceptor;

@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

	private final ExecutionTimeRequestInterceptor executionTimeRequestInterceptor;

	public WebMvcConfiguration(ExecutionTimeRequestInterceptor executionTimeRequestInterceptor) {
		this.executionTimeRequestInterceptor = executionTimeRequestInterceptor;
	}

	@Override
	public void addViewControllers(ViewControllerRegistry registry) {
		registry.addViewController("/apple-touch-icon.png").setViewName("forward:/app/apple-touch-icon.png");
		registry.addViewController("/favicon.ico").setViewName("forward:/app/favicon.ico");
		registry.addViewController("/favicon-16x16.png").setViewName("forward:/app/favicon-16x16.png");
		registry.addViewController("/favicon-32x32.png").setViewName("forward:/app/favicon-32x32.png");
		registry.addViewController("/").setViewName("redirect:/app");
		registry.addViewController("/app").setViewName("forward:/app/index.html");
		registry.addViewController("/app/**/{:[^.]+}").setViewName("forward:/app/index.html");
		registry.addViewController("/manual").setViewName("redirect:/manual/");
		registry.addViewController("/manual/").setViewName("forward:/manual/index.html");
		registry.addViewController("/manual/**/{:[^.]+}").setViewName("forward:/manual/index.html");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(executionTimeRequestInterceptor);
	}

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer
			.defaultContentType(MediaType.APPLICATION_JSON)
			.strategies(
				List.of(webRequest -> List.of(MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.ALL)));
	}

}
