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

package de.wps.radvis.backend.netz.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.auditing.AuditingConfiguration;
import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.schnittstelle.WithAuditingAspect;
import de.wps.radvis.backend.authentication.domain.RadVisAuthentication;
import de.wps.radvis.backend.authentication.domain.entity.RadVisUserDetails;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.AuditingTestIT;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

@Tag("group7")
@ContextConfiguration(classes = {
	AuditingConfiguration.class,
	NetzConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerConfiguration.class,
	WithAuditingAspect.class,
	CommonConfiguration.class,
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
public class NetzServiceAuditingTestIT extends AuditingTestIT {

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	NetzService netzService;

	@Autowired
	KantenRepository kantenRepository;

	@Autowired
	KnotenRepository knotenRepository;

	@Autowired
	VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	BenutzerRepository benutzerRepository;

	private Verwaltungseinheit verwaltungseinheit;

	@BeforeEach
	void setUp() {
		verwaltungseinheit = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.build());
	}

	@Test
	void testWurdeAngelegtVon_RevisionVorhanden_trueFuerAnleger() {
		Benutzer benutzerDerDieKanteAngelegtHat = benutzerRepository.save(
			BenutzerTestDataProvider.kreiskoordinator(verwaltungseinheit).serviceBwId(ServiceBwId.of("sbwid1"))
				.build());
		Benutzer jemandAnderes = benutzerRepository.save(
			BenutzerTestDataProvider.kreiskoordinator(verwaltungseinheit).serviceBwId(ServiceBwId.of("sbwid2"))
				.build());

		// Wir setzen den User im SecurtiyContext (entsprechend gewrappt) damit er beim Auditing
		// an der RevInfo gesetzt wird
		UserDetails userDetails = new RadVisUserDetails(benutzerDerDieKanteAngelegtHat, Collections.emptyList());
		Authentication authentication = new RadVisAuthentication(userDetails);
		SecurityContext securityContext = Mockito.mock(SecurityContext.class);
		Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);

		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);
		Kante kante = kantenRepository.save(
			KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).build());
		AdditionalRevInfoHolder.clear();

		// act & assert
		assertThat(netzService.wurdeAngelegtVon(kante, benutzerDerDieKanteAngelegtHat)).isTrue();
		assertThat(netzService.wurdeAngelegtVon(kante, jemandAnderes)).isFalse();
	}

	@Test
	void testWurdeAngelegtVon_keineRevisionVorhanden_false() {
		Benutzer benutzerDerDieKanteAngelegtHat = benutzerRepository.save(
			BenutzerTestDataProvider.kreiskoordinator(verwaltungseinheit).serviceBwId(ServiceBwId.of("sbwid1"))
				.build());
		Benutzer jemandAnderes = benutzerRepository.save(
			BenutzerTestDataProvider.kreiskoordinator(verwaltungseinheit).serviceBwId(ServiceBwId.of("sbwid2"))
				.build());

		// Wir setzen den User im SecurtiyContext (entsprechend gewrappt) damit er beim Auditing
		// an der RevInfo gesetzt wird.
		// Auch wenn wir die Auditing-Daten später löschen, machen wir das hier
		// damit der Test fehlschlägt, falls das setup bricht und die Auditing-Daten
		// nicht korrekt entfernt werden.
		UserDetails userDetails = new RadVisUserDetails(benutzerDerDieKanteAngelegtHat, Collections.emptyList());
		Authentication authentication = new RadVisAuthentication(userDetails);
		SecurityContext securityContext = Mockito.mock(SecurityContext.class);
		Mockito.when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);

		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.CREATE_KANTE_COMMAND);
		Kante kante = kantenRepository.save(
			KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadVis).build());
		AdditionalRevInfoHolder.clear();

		// Wir entfernen die relevanten Auditing-Daten
		jdbcTemplate.execute("DELETE FROM kante_aud");
		jdbcTemplate.execute("DELETE FROM rev_info");

		// act & assert
		assertThat(netzService.wurdeAngelegtVon(kante, benutzerDerDieKanteAngelegtHat)).isFalse();
		assertThat(netzService.wurdeAngelegtVon(kante, jemandAnderes)).isFalse();
	}
}
