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

package de.wps.radvis.backend.reverseproxy;

import static com.github.mkopylec.charon.configuration.CharonConfigurer.charonConfiguration;
import static com.github.mkopylec.charon.configuration.RequestMappingConfigurer.requestMapping;
import static com.github.mkopylec.charon.forwarding.RestTemplateConfigurer.restTemplate;
import static com.github.mkopylec.charon.forwarding.TimeoutConfigurer.timeout;
import static com.github.mkopylec.charon.forwarding.Utils.copyHeaders;
import static com.github.mkopylec.charon.forwarding.interceptors.log.ForwardingLoggerConfigurer.forwardingLogger;
import static com.github.mkopylec.charon.forwarding.interceptors.log.LogLevel.DEBUG;
import static com.github.mkopylec.charon.forwarding.interceptors.log.LogLevel.WARN;
import static com.github.mkopylec.charon.forwarding.interceptors.rewrite.RegexRequestPathRewriterConfigurer.regexRequestPathRewriter;
import static com.github.mkopylec.charon.forwarding.interceptors.rewrite.RequestProxyHeadersRewriterConfigurer.requestProxyHeadersRewriter;
import static com.github.mkopylec.charon.forwarding.interceptors.rewrite.RequestServerNameRewriterConfigurer.requestServerNameRewriter;
import static de.wps.radvis.backend.reverseproxy.CharonConfiguration.HttpBasicAuthHeaderRemoverInterceptorConfigurer.httpBasicAuthHeaderRemoverInterceptor;
import static de.wps.radvis.backend.reverseproxy.CharonConfiguration.WrapExceptionToHttpStatusNotFoundInterceptorConfigurer.wrapExceptionToHttpStatusNotFound;
import static de.wps.radvis.backend.reverseproxy.CharonConfiguration.XForwardedPathHeaderAdderInterceptorConfigurer.xForwardedPathHeaderAdderInterceptor;
import static de.wps.radvis.backend.reverseproxy.CharonConfiguration.XForwardedUriHeaderInterceptorConfigurer.xForwardedUriHeaderInterceptor;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import com.github.mkopylec.charon.configuration.CharonConfigurer;
import com.github.mkopylec.charon.forwarding.RequestForwardingException;
import com.github.mkopylec.charon.forwarding.interceptors.HttpRequest;
import com.github.mkopylec.charon.forwarding.interceptors.HttpRequestExecution;
import com.github.mkopylec.charon.forwarding.interceptors.HttpResponse;
import com.github.mkopylec.charon.forwarding.interceptors.RequestForwardingInterceptor;
import com.github.mkopylec.charon.forwarding.interceptors.RequestForwardingInterceptorConfigurer;
import com.github.mkopylec.charon.forwarding.interceptors.RequestForwardingInterceptorType;

@Configuration
@ComponentScan
class CharonConfiguration {

	@Autowired
	ReverseproxyConfiguarationProperties reverseproxyConfiguarationProperties;

	@Bean
	public FilterRegistrationBean<WriteFormFieldsToBodyFilter> writeFormFieldsToBodyFilter() {
		FilterRegistrationBean<WriteFormFieldsToBodyFilter> registrationBean = new FilterRegistrationBean<>();

		registrationBean.setFilter(new WriteFormFieldsToBodyFilter());

		registrationBean.addUrlPatterns("/matomo/*");
		registrationBean.setOrder(1);

		return registrationBean;
	}

	@Bean
	@Order(20)
	CharonConfigurer charonConfigurer() {
		return charonConfiguration()
			.filterOrder(20)
			.set(forwardingLogger()
				.successLogLevel(DEBUG)
				.clientErrorLogLevel(WARN)
				.serverErrorLogLevel(WARN)
				.unexpectedErrorLogLevel(WARN))
			.set(restTemplate().set(timeout().connection(ofSeconds(5)).read(ofMinutes(10)).write(ofMinutes(10))))
			.set(httpBasicAuthHeaderRemoverInterceptor())
			.add(
				requestMapping("geoserver saml")
					.pathRegex("/api/geoserver/saml/(radvis|balm)/.*")
					.set(requestServerNameRewriter().outgoingServers(
						reverseproxyConfiguarationProperties.getGeoserverUrl()))
					.set(
						regexRequestPathRewriter()
							.paths("api/geoserver/saml/(?<path>.*)", "geoserver/<path>"))
					.set(xForwardedPathHeaderAdderInterceptor().basePath("api/geoserver/saml")))
			.add(
				requestMapping("geoserver status")
					.pathRegex("/health-check/geoserver")
					.set(requestServerNameRewriter().outgoingServers(
						reverseproxyConfiguarationProperties.getGeoserverUrl()))
					.set(
						regexRequestPathRewriter()
							.paths(".*",
								"geoserver/" + reverseproxyConfiguarationProperties.getGeoserverHealthCheckUrl()))
					.set(wrapExceptionToHttpStatusNotFound()))
			.add(
				requestMapping("geoserver saml (datei-layer)")
					.pathRegex("/api/geoserver/saml/datei-layer/.*")
					.set(requestServerNameRewriter().outgoingServers(
						reverseproxyConfiguarationProperties.getGeoserverDateiLayerUrl()))
					.set(
						regexRequestPathRewriter()
							.paths("api/geoserver/saml/(?<path>.*)", "geoserver/<path>"))
					.set(xForwardedPathHeaderAdderInterceptor().basePath("api/geoserver/saml")))
			.add(
				requestMapping("geoserver status (datei-layer)")
					.pathRegex("/health-check/geoserver-datei-layer")
					.set(requestServerNameRewriter().outgoingServers(
						reverseproxyConfiguarationProperties.getGeoserverDateiLayerUrl()))
					.set(
						regexRequestPathRewriter()
							.paths(".*",
								"geoserver/" + reverseproxyConfiguarationProperties.getGeoserverHealthCheckUrl()))
					.set(wrapExceptionToHttpStatusNotFound()))
			.add(
				requestMapping("geoserver toubiz")
					.pathRegex("/api/geoserver/basic/toubiz/.*")
					.set(requestServerNameRewriter().outgoingServers(
						reverseproxyConfiguarationProperties.getGeoserverUrl()))
					.set(
						regexRequestPathRewriter()
							.paths("api/geoserver/basic/toubiz/(?<path>.*)",
								"geoserver/toubiz/<path>"))
					.set(xForwardedPathHeaderAdderInterceptor().basePath("api/geoserver/basic")))
			.add(
				requestMapping("geoserver basicAuth externe Schnittstelle")
					.pathRegex("/api/geoserver/basicauth/.*")
					.set(requestServerNameRewriter().outgoingServers(
						reverseproxyConfiguarationProperties.getGeoserverUrl()))
					.set(
						regexRequestPathRewriter()
							.paths("api/geoserver/basicauth/(?<path>.*)", "geoserver/<path>"))
					.set(xForwardedPathHeaderAdderInterceptor().basePath("api/geoserver/basicauth")))
			.add(
				requestMapping("grafana redirection")
					.pathRegex("/logs.*")
					.set(requestServerNameRewriter().outgoingServers(
						reverseproxyConfiguarationProperties.getGrafanaUrl()))
					.set(regexRequestPathRewriter()
						.paths("/logs(?<path>.*)", "<path>")))
			.add(
				requestMapping("beschilderung")
					.pathRegex("/api/wegweisendebeschilderung/kataster.*")
					.set(requestServerNameRewriter().outgoingServers(
						reverseproxyConfiguarationProperties.getBeschilderungsKatasterDomain()))
					.set(regexRequestPathRewriter()
						.paths("/api/wegweisendebeschilderung/kataster/(?<path>.*)",
							reverseproxyConfiguarationProperties.getBeschilderungsKatasterPath()
								+ "/<path>")))
			.add(
				requestMapping("matomo redirection")
					.pathRegex("/matomo/.*")
					.set(requestServerNameRewriter().outgoingServers(
						reverseproxyConfiguarationProperties.getMatomoUrl()))
					.set(regexRequestPathRewriter()
						.paths("/matomo/(?<path>.*)", "/<path>"))
					.set(requestProxyHeadersRewriter())
					.set(xForwardedUriHeaderInterceptor().basePath("/matomo")));
	}

	static class HttpBasicAuthHeaderRemoverInterceptor implements RequestForwardingInterceptor {

		@Override
		public HttpResponse forward(HttpRequest request, HttpRequestExecution execution) {
			HttpHeaders rewrittenHeaders = copyHeaders(request.getHeaders());
			rewrittenHeaders.remove("Authorization");
			request.setHeaders(rewrittenHeaders);
			return execution.execute(request);
		}

		@Override
		public RequestForwardingInterceptorType getType() {
			return new RequestForwardingInterceptorType(1500);
		}
	}

	static class HttpBasicAuthHeaderRemoverInterceptorConfigurer
		extends RequestForwardingInterceptorConfigurer<HttpBasicAuthHeaderRemoverInterceptor> {

		private HttpBasicAuthHeaderRemoverInterceptorConfigurer() {
			super(new HttpBasicAuthHeaderRemoverInterceptor());
		}

		static HttpBasicAuthHeaderRemoverInterceptorConfigurer httpBasicAuthHeaderRemoverInterceptor() {
			return new HttpBasicAuthHeaderRemoverInterceptorConfigurer();
		}
	}

	static class XForwardedPathHeaderAdderInterceptor implements RequestForwardingInterceptor {

		private String basePath;

		@Override
		public HttpResponse forward(HttpRequest request, HttpRequestExecution execution) {
			HttpHeaders rewrittenHeaders = copyHeaders(request.getHeaders());
			rewrittenHeaders.set("X-Forwarded-Proto", "https");
			rewrittenHeaders.add("X-Forwarded-Path", this.basePath);
			rewrittenHeaders.set("X-Forwarded-Host", request.getHeaders().get("host").get(0));
			request.setHeaders(rewrittenHeaders);
			return execution.execute(request);
		}

		@Override
		public RequestForwardingInterceptorType getType() {
			return new RequestForwardingInterceptorType(1600);
		}

		public void setBasePath(String basePath) {
			this.basePath = basePath;
		}
	}

	static class XForwardedPathHeaderAdderInterceptorConfigurer
		extends RequestForwardingInterceptorConfigurer<XForwardedPathHeaderAdderInterceptor> {

		private XForwardedPathHeaderAdderInterceptorConfigurer() {
			super(new XForwardedPathHeaderAdderInterceptor());
		}

		static XForwardedPathHeaderAdderInterceptorConfigurer xForwardedPathHeaderAdderInterceptor() {
			return new XForwardedPathHeaderAdderInterceptorConfigurer();
		}

		XForwardedPathHeaderAdderInterceptorConfigurer basePath(String basePath) {
			configuredObject.setBasePath(basePath);
			return this;
		}
	}

	static class XForwardedUriHeaderInterceptor implements RequestForwardingInterceptor {

		private String basePath;

		@Override
		public HttpResponse forward(HttpRequest request, HttpRequestExecution execution) {
			HttpHeaders rewrittenHeaders = copyHeaders(request.getHeaders());
			rewrittenHeaders.add("X-Forwarded-Uri", this.basePath);
			request.setHeaders(rewrittenHeaders);
			return execution.execute(request);
		}

		@Override
		public RequestForwardingInterceptorType getType() {
			return new RequestForwardingInterceptorType(1800);
		}

		public void setBasePath(String basePath) {
			this.basePath = basePath;
		}

	}

	static class XForwardedUriHeaderInterceptorConfigurer
		extends RequestForwardingInterceptorConfigurer<XForwardedUriHeaderInterceptor> {

		private XForwardedUriHeaderInterceptorConfigurer() {
			super(new XForwardedUriHeaderInterceptor());
		}

		static XForwardedUriHeaderInterceptorConfigurer xForwardedUriHeaderInterceptor() {
			return new XForwardedUriHeaderInterceptorConfigurer();
		}

		XForwardedUriHeaderInterceptorConfigurer basePath(String basePath) {
			configuredObject.setBasePath(basePath);
			return this;
		}
	}

	static class WrapExceptionToHttpStatusNotFoundInterceptor implements RequestForwardingInterceptor {

		@Override
		public HttpResponse forward(HttpRequest request, HttpRequestExecution execution) {
			try {
				return execution.execute(request);
			} catch (RequestForwardingException e) {
				return new HttpResponse(HttpStatus.NOT_FOUND);
			}
		}

		@Override
		public RequestForwardingInterceptorType getType() {
			return new RequestForwardingInterceptorType(2000);
		}
	}

	static class WrapExceptionToHttpStatusNotFoundInterceptorConfigurer
		extends RequestForwardingInterceptorConfigurer<WrapExceptionToHttpStatusNotFoundInterceptor> {

		private WrapExceptionToHttpStatusNotFoundInterceptorConfigurer() {
			super(new WrapExceptionToHttpStatusNotFoundInterceptor());
		}

		static WrapExceptionToHttpStatusNotFoundInterceptorConfigurer wrapExceptionToHttpStatusNotFound() {
			return new WrapExceptionToHttpStatusNotFoundInterceptorConfigurer();
		}
	}

}
