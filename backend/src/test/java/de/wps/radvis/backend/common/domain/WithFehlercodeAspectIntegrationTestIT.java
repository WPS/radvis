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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import lombok.extern.slf4j.Slf4j;

@Tag("group5")
@ContextConfiguration(classes = { WithFehlercodeAspectIntegrationTestIT.TestConfiguration.class })
@SpringBootTest(classes = { AnnotationAwareAspectJAutoProxyCreator.class, WithFehlercodeAspect.class })
@ExtendWith(OutputCaptureExtension.class)
@Slf4j
// Tests abh. von Log-Pattern, s. logback-test.xml
class WithFehlercodeAspectIntegrationTestIT {
	@Autowired
	private TestBean testBean;

	@Configuration
	static class TestConfiguration {
		@Bean
		public TestBean testBean() {
			return new TestBean();
		}
	}

	@WithFehlercode(Fehlercode.ABSTELLANLAGEN_IMPORT)
	@Slf4j
	static class TestBean {
		public void logWithClassAnnotation(String msg) {
			log.debug(msg);
		}

		@WithFehlercode(Fehlercode.HINTERGRUNDKARTEN)
		public void logWithMethodAnnotation(String msg) {
			log.debug(msg);
		}

		public void logException(String msg) {
			throw new RuntimeException(msg);
		}
	}

	@BeforeEach
	public void setup() {
		((Logger) log).setLevel(Level.DEBUG);
	}

	@Test
	void test_classAnnotation(CapturedOutput output) {
		// act
		testBean.logWithClassAnnotation("TEST");

		// assert
		assertThat(output.getOut())
			.containsPattern("\\[" + Fehlercode.ABSTELLANLAGEN_IMPORT.getCodeNumber() + "\\] .*? - TEST");
	}

	@Test
	void test_exception(CapturedOutput output) {
		// act
		assertThrows(RuntimeException.class, () -> testBean.logException("TEST"));

		// assert
		assertThat(output.getOut())
			.containsPattern("ERROR \\[" + Fehlercode.ABSTELLANLAGEN_IMPORT.getCodeNumber() + "\\] .*? - TEST");
	}

	@Test
	void test_methodAnnotation(CapturedOutput output) {
		// act
		testBean.logWithMethodAnnotation("TEST");

		// assert
		assertThat(output.getOut())
			.containsPattern("\\[" + Fehlercode.HINTERGRUNDKARTEN.getCodeNumber() + "\\] .*? - TEST");
	}

	@Test
	void test_restorePreviousFehlercodeWhenChainingAnnotations(CapturedOutput output) {
		// arrange
		testBean.logWithMethodAnnotation("TEST");

		// act
		testBean.logWithClassAnnotation("TEST");

		// assert
		assertThat(output.getOut())
			.containsPattern("\\[" + Fehlercode.ABSTELLANLAGEN_IMPORT.getCodeNumber() + "\\] .*? - TEST");
	}

	@Test
	void test_clear(CapturedOutput output) {
		// arrange
		testBean.logWithMethodAnnotation("TEST");

		// act
		log.debug("TEST");

		// assert
		assertThat(output.getOut())
			.containsPattern("\\[" + Fehlercode.SONSTIGES.getCodeNumber() + "\\] .*? - TEST");
	}
}
