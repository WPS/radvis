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

package de.wps.radvis.backend.netz.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.auditing.domain.WithAuditing;
import de.wps.radvis.backend.auditing.schnittstelle.WithAuditingAspect;
import de.wps.radvis.backend.authentication.domain.RadVisAuthentication;
import de.wps.radvis.backend.authentication.domain.entity.RadVisUserDetails;
import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteAttributeCommand;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

@Tag("group7")
@SpringBootTest(classes = { AnnotationAwareAspectJAutoProxyCreator.class, WithAuditingAspect.class })
@ContextConfiguration(classes = WithAuditingAspectTestIT.TestConfig.class)
class WithAuditingAspectTestIT {

	NetzController netzController;

	NetzService netzService;

	static class TestConfig {

		class TestBean {
			@WithAuditing(context = AuditingContext.SAVE_KANTE_ATTRIBUTE_COMMAND)
			public AuditingContext testAuditingContext() {
				return AdditionalRevInfoHolder.getAuditingContext();
			}

		}

		@Bean
		TestBean testBean() {
			return new TestBean();
		}
	}

	@Autowired
	TestConfig.TestBean testBean;

	private NetzGuard netzAutorisierungsService;

	@BeforeEach
	void setup() {
		netzService = Mockito.mock(NetzService.class);
		netzAutorisierungsService = Mockito.mock(NetzGuard.class);
		BenutzerResolver benutzerResolver = Mockito.mock(BenutzerResolver.class);
		Mockito.when(benutzerResolver.fromAuthentication(Mockito.any()))
			.thenReturn(
				BenutzerTestDataProvider.admin(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
					.build());
		netzController = new NetzController(netzService, netzAutorisierungsService, benutzerResolver,
			Mockito.mock(ZustaendigkeitsService.class), Mockito.mock(SaveKanteCommandConverter.class),
			Mockito.mock(NetzToFeatureDetailsConverter.class), Mockito.mock(NetzConfigurationProperties.class));

	}

	@Test
	void testAddAuditingContext() {
		assertThat(AdditionalRevInfoHolder.getAuditingContext()).isNull();

		Mockito.when(netzService.loadKantenAttributGruppeForModification(Mockito.any(), Mockito.any())).thenReturn(
			KantenAttributGruppeTestDataProvider.defaultValue().build());

		Mockito.when(netzService.getKante(Mockito.any())).thenReturn(KanteTestDataProvider.withDefaultValues().build());

		Mockito.when(netzService.saveKante(Mockito.any()))
			.thenReturn(KanteTestDataProvider.withDefaultValues().build());
		SaveKanteAttributeCommand command = Mockito.mock(SaveKanteAttributeCommand.class);

		Mockito.when(command.getGruppenId()).thenReturn(1L);
		Mockito.when(command.getNetzklassen()).thenReturn(Collections.emptySet());
		netzController
			.saveKanteAllgemein(
				new RadVisAuthentication(
					new RadVisUserDetails(
						BenutzerTestDataProvider
							.externerDienstleister(
								VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build())
							.build(),
						new ArrayList<>())),
				List.of(command));

		assertThat(AdditionalRevInfoHolder.getAuditingContext()).isNull();

	}

	@Test
	void testAuditingContext_contextWirdGesetztUndGeleert() {
		assertThat(AdditionalRevInfoHolder.getAuditingContext()).isNull();

		AuditingContext result = testBean.testAuditingContext();

		assertThat(result).isEqualTo(AuditingContext.SAVE_KANTE_ATTRIBUTE_COMMAND);
		assertThat(AdditionalRevInfoHolder.getAuditingContext()).isNull();

	}

}
