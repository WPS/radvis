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

import static org.hamcrest.Matchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml4AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.metadata.OpenSaml4MetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.saml2.provider.service.web.authentication.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import de.wps.radvis.backend.authentication.domain.RadVisAuthentication;
import de.wps.radvis.backend.authentication.schnittstelle.RadVisUserDetailsService;
import de.wps.radvis.backend.basicAuthentication.domain.BasicAuthBenutzerRepository;
import de.wps.radvis.backend.basicAuthentication.domain.BasicAuthPasswortService;
import de.wps.radvis.backend.basicAuthentication.schnittstelle.BasicAuthenticationProvider;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.common.domain.SecurityConfigurationProperties;
import de.wps.radvis.backend.reverseproxy.ReverseproxyConfiguarationProperties;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfiguration {

	static RequestMatcher API_MATCHER = new AntPathRequestMatcher("/api/**");
	static RequestMatcher API_EXTERN_MATCHER = new AntPathRequestMatcher("/api/extern/**");
	static RequestMatcher REVERSEPROXY_GEOSERVER_MATCHER = new AntPathRequestMatcher("/api/geoserver/basic/**");
	static RequestMatcher BASIC_AUTH_GEOSERVER_MATCHER = new AntPathRequestMatcher("/api/geoserver/basicauth/**");
	static RequestMatcher NOT_API_MATCHER = new NegatedRequestMatcher(API_MATCHER);
	static RequestMatcher NOT_API_EXTERN_MATCHER = new NegatedRequestMatcher(API_EXTERN_MATCHER);
	static RequestMatcher NOT_REVERSEPROXY_GEOSERVER_MATCHER = new NegatedRequestMatcher(
		REVERSEPROXY_GEOSERVER_MATCHER);
	static RequestMatcher NOT_BASIC_AUTH_GEOSERVER_MATCHER = new NegatedRequestMatcher(
		BASIC_AUTH_GEOSERVER_MATCHER);

	@Configuration
	@Order(11)
	public static class ApplicationSamlSecurityConfiguration {
		@Autowired
		private SecurityConfigurationProperties securityConfigurationProperties;

		@Autowired
		private RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

		@Autowired
		private BenutzerService benutzerService;

		@Autowired
		private ApplicationEventPublisher applicationEventPublisher;

		// Nicht als @Bean bereitgestellt, damit der UserDetailsService nicht mit den
		// statischen Nutzern der externen
		// APIs interferiert.
		public RadVisUserDetailsService userDetailsService() {
			return new RadVisUserDetailsService(benutzerService);
		}

		@Bean
		public SecurityFilterChain samlFilterChain(HttpSecurity http) throws Exception {
			RadVisUserDetailsService userDetailsService = userDetailsService();

			OpenSaml4AuthenticationProvider authenticationProvider = new OpenSaml4AuthenticationProvider();
			authenticationProvider.setResponseAuthenticationConverter(responseToken -> {
				Saml2Authentication authentication = OpenSaml4AuthenticationProvider
					.createDefaultResponseAuthenticationConverter()
					.convert(responseToken);
				String serviceBWID = getAktuellerBenutzerServiceBwId(authentication);
				UserDetails userDetails = userDetailsService.loadUserByUsername(serviceBWID,
					((Saml2AuthenticatedPrincipal) authentication.getPrincipal()));
				return new RadVisAuthentication(userDetails);
			});

			CsrfTokenRequestAttributeHandler delegate = new CsrfTokenRequestAttributeHandler();
			delegate.setCsrfRequestAttributeName(null);

			http.sessionManagement(session -> session.maximumSessions(-1).sessionRegistry(sessionRegistry()))
				.securityMatcher(NOT_API_MATCHER)
				.authorizeHttpRequests(authorizer -> authorizer
					.requestMatchers("/health-check/**").permitAll()
					.requestMatchers("/actuator/health").permitAll()
					.requestMatchers("/actuator/prometheus")
					.access((authentication, c) -> new AuthorizationDecision(
						new IpAddressMatcher(securityConfigurationProperties.getPrometheusWhitelistIP())
							.matches(c.getRequest())))
					.requestMatchers("/manual/**").permitAll()
					.requestMatchers("/resources/**").permitAll()
					.requestMatchers("/favicon.ico").permitAll()
					.requestMatchers("/favicon-16x16.png").permitAll()
					.requestMatchers("/favicon-32x32.png").permitAll()
					.requestMatchers("/").authenticated()
					.requestMatchers("/app/favicon.ico").permitAll()
					.requestMatchers("/app/favicon-16x16.png").permitAll()
					.requestMatchers("/app/favicon-32x32.png").permitAll()
					.requestMatchers("/app/**").authenticated()
					.requestMatchers("/matomo/**").authenticated()
					.requestMatchers("/control.html").hasAuthority(Recht.JOBS_AUSFUEHREN.name())
					.requestMatchers("/logs/**").hasAuthority(Recht.LOGS_EINSEHEN.name())
					.requestMatchers("/**").hasAuthority(BenutzerStatus.AKTIV.name()))
				.csrf(configurer -> configurer
					.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
					.csrfTokenRequestHandler(delegate)
					.ignoringRequestMatchers("/logs/**")
					.ignoringRequestMatchers("/matomo/**")
					.ignoringRequestMatchers("/logout"))
				// Muss NACH csrf-Config erfolgen
				.requestCache(configurer -> configurer.requestCache(new HttpSessionRequestCache()))
				.saml2Login(saml2 -> {
					ProviderManager providerManager = new ProviderManager(authenticationProvider);
					providerManager.setAuthenticationEventPublisher(
						new DefaultAuthenticationEventPublisher(applicationEventPublisher));
					saml2.authenticationManager(providerManager);
					saml2.loginProcessingUrl(securityConfigurationProperties.getACS());
					if (securityConfigurationProperties.isLocalAuthSetup()) {
						saml2.defaultSuccessUrl(securityConfigurationProperties.getLocalAuthSuccessUrl(), true);
					}
				})
				.saml2Logout(Customizer.withDefaults())
				.addFilterBefore(createSaml2MetadataFilter(), Saml2WebSsoAuthenticationFilter.class)
				.logout(logout -> logout
					.permitAll()
					.logoutSuccessHandler(
						(request, response, authentication) -> response
							.setStatus(HttpServletResponse.SC_OK)));

			return http.build();
		}

		private Saml2MetadataFilter createSaml2MetadataFilter() {
			RelyingPartyRegistrationResolver relyingPartyRegistrationResolver = new DefaultRelyingPartyRegistrationResolver(
				this.relyingPartyRegistrationRepository);
			return new Saml2MetadataFilter(relyingPartyRegistrationResolver, new OpenSaml4MetadataResolver());
		}

		private String getAktuellerBenutzerServiceBwId(Authentication authentication) {
			Object principal = authentication.getPrincipal();

			require(principal instanceof DefaultSaml2AuthenticatedPrincipal,
				"Der aktuell angemeldete Service-BW Benutzer darf nicht \"anonymousUser\" sein");

			List<String> values = ((DefaultSaml2AuthenticatedPrincipal) principal)
				.getAttribute(securityConfigurationProperties.serviceBwIdKey);
			require(values, notNullValue());
			require(values.size() == 1,
				"Es muss genau eine Service-BW-ID des aktuell angemeldeten Benutzers gefunden werden");

			return values.get(0);
		}

		@Bean
		public SessionRegistry sessionRegistry() {
			return new SessionRegistryImpl();
		}
	}

	// ############## Api Security #############

	// Api - Intern
	@Configuration
	@Order(12)
	public static class InternalApiSamlSecurityConfiguration {

		@Bean
		public SecurityFilterChain internalApiFilterChain(HttpSecurity http) throws Exception {
			CsrfTokenRequestAttributeHandler delegate = new CsrfTokenRequestAttributeHandler();
			delegate.setCsrfRequestAttributeName(null);

			http.securityMatcher(
				new AndRequestMatcher(API_MATCHER, NOT_API_EXTERN_MATCHER, NOT_REVERSEPROXY_GEOSERVER_MATCHER,
					NOT_BASIC_AUTH_GEOSERVER_MATCHER))
				.authorizeHttpRequests(authorizer -> authorizer
					.requestMatchers("/api/organisationen/all").authenticated()
					.requestMatchers("/api/benutzer/registriere-benutzer").authenticated()
					.requestMatchers("/api/benutzerdetails").authenticated()
					.requestMatchers("/api/benutzer/reaktivierung/beantrage-reaktivierung").authenticated()
					.requestMatchers("/api/togglz").authenticated()
					.requestMatchers("/api/fehlerprotokoll/types").authenticated()
					.requestMatchers("/api/hintergrundkarte/layers").authenticated()
					.requestMatchers("/api/signaturen").authenticated()
					.requestMatchers("/api/weitere-kartenebenen/vordefiniert").authenticated()
					.requestMatchers("/api/weitere-kartenebenen/list").authenticated()
					.requestMatchers("/api/custom-routing-profile/list").authenticated()
					.requestMatchers("/api/**").hasAuthority(BenutzerStatus.AKTIV.name()));

			http
				// Anonymous-Authentifizierung ausschalten, da sonst nicht der EntryPoint (s.u.) aufgerufen wird.
				// Stattdessen würde eine AccessDeniedException fliegen, da der Anonymous-Nutzer natürlich nichts
				// darf. So wird aber der EntryPoint (s.u.) aufgerufen, der dann einen 401er ans Frontend gibt,
				// wodurch auf z.B. eine abgelaufene Session korrekt reagiert werden kann.
				.anonymous(configurer -> configurer.disable())
				.exceptionHandling(configurer -> configurer.authenticationEntryPoint((req, res, ex) -> {
					res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				}));

			http.csrf(configurer -> configurer
				.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
				.csrfTokenRequestHandler(delegate)
				.ignoringRequestMatchers("/logs/**"));

			return http.build();
		}
	}

	// Api - Extern
	@Configuration
	@Order(13)
	public static class ExternalApiBasicAuthSecurityConfiguration {
		@Autowired
		private SecurityConfigurationProperties securityConfigurationProperties;

		@Bean
		public SecurityFilterChain externalApiFilterChain(HttpSecurity http) throws Exception {

			// Dies ist notwendig, damit die Authentifizierung bei jedem Aufruf eines
			// externen Api-Endpunkts neu überprüft wird
			http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

			http.securityMatcher(API_EXTERN_MATCHER)
				.authenticationProvider(externAuthenticationProviderExternalApi())
				.authorizeHttpRequests(authorizer -> authorizer
					.requestMatchers(API_EXTERN_MATCHER).authenticated())
				.httpBasic(configurer -> {
					// hier wird der status auf 401 gesetzt, statt sendError aufzurufen, da
					// sendError mit BasicAuth zu einem Redirect zum Saml-Login führt.
					configurer.authenticationEntryPoint((req, res, ex) -> {
						res.addHeader("WWW-Authenticate", "Basic");
						res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					});
				});

			return http.build();
		}

		@Bean
		public AuthenticationProvider externAuthenticationProviderExternalApi() {
			return new ExternAuthenticationProvider(securityConfigurationProperties.getExternerApiUserName(),
				securityConfigurationProperties.getExternerApiUserPassword());
		}
	}

	// Api - Geoserver abgesichert durch basic auth für einen technischen Benutzer
	@Configuration
	@Order(2)
	public static class GeoserverBasicAuthSecurityConfiguration {
		@Autowired
		private ReverseproxyConfiguarationProperties reverseproxyConfiguarationProperties;

		@Bean
		public SecurityFilterChain geoserverFilterChain(HttpSecurity http) throws Exception {
			if (reverseproxyConfiguarationProperties.getGeoserverApiUserName() == null
				|| reverseproxyConfiguarationProperties.getGeoserverApiUserPassword() == null) {
				log.warn("geoserver basic auth Benuzer und Passwort nicht konfiguriert");

				http.securityMatcher(REVERSEPROXY_GEOSERVER_MATCHER)
					.authorizeHttpRequests(authorizer -> authorizer.anyRequest().denyAll());
				return http.build();
			}

			// Dies ist notwendig, damit die Authentifizierung bei jedem Aufruf eines
			// externen Api-Endpunkts neu überprüft wird
			http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

			http.securityMatcher(REVERSEPROXY_GEOSERVER_MATCHER)
				.authenticationProvider(externAuthenticationProviderGeoServer())
				.authorizeHttpRequests(authorizer -> authorizer
					.requestMatchers(REVERSEPROXY_GEOSERVER_MATCHER).authenticated())
				.httpBasic(configurer -> {
					// hier wird der status auf 401 gesetzt, statt sendError aufzurufen, da
					// sendError mit BasicAuth zu einem Redirect zum Saml-Login führt.
					configurer.authenticationEntryPoint((req, res, ex) -> {
						res.addHeader("WWW-Authenticate", "Basic");
						res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					});
				});

			return http.build();
		}

		@Bean
		public AuthenticationProvider externAuthenticationProviderGeoServer() {
			return new ExternAuthenticationProvider(
				reverseproxyConfiguarationProperties.getGeoserverApiUserName(),
				reverseproxyConfiguarationProperties.getGeoserverApiUserPassword());
		}
	}

	static class ExternAuthenticationProvider implements AuthenticationProvider {
		String externerApiUserName;
		String externerApiUserPassword;

		public ExternAuthenticationProvider(String externerApiUserName, String externerApiUserPassword) {
			this.externerApiUserName = externerApiUserName;
			this.externerApiUserPassword = externerApiUserPassword;
		}

		@Override
		public Authentication authenticate(Authentication authentication) throws AuthenticationException {
			String name = authentication.getName();

			if (authentication.getCredentials() != null) {
				String pass = authentication.getCredentials().toString();

				if (name.equals(externerApiUserName) && pass.equals(externerApiUserPassword)) {
					return new UsernamePasswordAuthenticationToken(name, pass, new ArrayList<>());
				}
			}
			throw new BadCredentialsException("Ungültige Credentials.");
		}

		@Override
		public boolean supports(Class<?> authentication) {
			return authentication.equals(UsernamePasswordAuthenticationToken.class);
		}
	}

	// Api - Geoserver abgesichert durch basic auth für RadVIS Benutzer mit
	// pro Benutzer generierten BasicAuth Zugangsdaten
	@Configuration
	@Order(1)
	public static class GeoserverBasicAuthSecurityConfiguration2 {
		@Autowired
		private BasicAuthBenutzerRepository basicAuthBenutzerRepository;

		@Autowired
		private BasicAuthPasswortService basicAuthPasswortService;

		@Bean
		public SecurityFilterChain basicAuthFilterChain(HttpSecurity http) throws Exception {
			// Dies ist notwendig, damit die Authentifizierung bei jedem Aufruf eines
			// externen Api-Endpunkts neu überprüft wird
			http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
			http.securityMatcher(BASIC_AUTH_GEOSERVER_MATCHER)
				.authenticationProvider(
					new BasicAuthenticationProvider(basicAuthBenutzerRepository, basicAuthPasswortService))
				.authorizeHttpRequests(authorizer -> authorizer
					.requestMatchers(BASIC_AUTH_GEOSERVER_MATCHER).authenticated())
				.httpBasic(configurer -> {
					// hier wird der status auf 401 gesetzt, statt sendError aufzurufen, da
					// sendError mit BasicAuth zu einem Redirect zum Saml-Login führt.
					configurer.authenticationEntryPoint((req, res, ex) -> {
						res.addHeader("WWW-Authenticate", "Basic");
						res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					});
				});

			return http.build();
		}
	}
}
