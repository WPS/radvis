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

package de.wps.radvis.backend.abfrage.signatur.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.NetzausschnittService;
import de.wps.radvis.backend.abfrage.netzausschnitt.schnittstelle.NetzToGeoJsonConverter;
import de.wps.radvis.backend.abfrage.signatur.domain.SignaturConfigurationProperties;
import de.wps.radvis.backend.abfrage.signatur.domain.SignaturService;
import de.wps.radvis.backend.abfrage.signatur.domain.valueObject.SignaturTyp;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.ExtentProperty;
import de.wps.radvis.backend.netz.domain.service.NetzService;

public class SignaturController_SignaturServiceIntegrationTest {

	private CommonConfigurationProperties commonConfigurationProperties;

	private SignaturController signaturController;
	@Mock
	private NetzToGeoJsonConverter netzToGeoJsonConverter;
	@Mock
	private NetzService netzService;
	@Mock
	private NetzausschnittService netzausschnittService;
	private SignaturService signaturService;
	private SignaturConfigurationProperties signaturConfigurationProperties;

	@BeforeEach
	public void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);
		ExtentProperty extent = new ExtentProperty(492846.960, 500021.252, 5400410.543, 5418644.476);
		commonConfigurationProperties = new CommonConfigurationProperties("src/test/resources/", 60, extent, null,
			"test", "https://radvis-dev.landbw.de/");
		signaturConfigurationProperties = new SignaturConfigurationProperties("/signaturen/",
			"/signaturen/Massnahmen/");

		signaturService = new SignaturService(commonConfigurationProperties, signaturConfigurationProperties);

		signaturController = new SignaturController(netzToGeoJsonConverter, netzService, netzausschnittService,
			signaturService);

	}

	@Test
	void test_getVerfuegbareSignaturen() {
		// Act + Assert
		assertThat(signaturController.getVerfuegbareSignaturen()).containsExactlyInAnyOrder(
			new SignaturView("test", SignaturTyp.NETZ),
			new SignaturView("massnahmen-test", SignaturTyp.MASSNAHME));
	}

	@Test
	void test_getStylingForSignatur_liefertSLDfile() {
		// Act
		String result = signaturController.getStylingForSignatur(SignaturTyp.NETZ, "test");

		// Assert
		assertThat(result.length()).isGreaterThan(500);
	}

	@Test
	void test_getStylingForSignatur_wirftFehlerBeiNichtvorhandenerDatei() {
		// Act + Assert
		assertThatThrownBy(() -> signaturController.getStylingForSignatur(SignaturTyp.NETZ, "gibtsnicht"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("404");
	}

	@Test
	void test_getStylingForSignatur_wirftFehlerBeiFalschemTyp() {
		// Act + Assert
		assertThatThrownBy(() -> signaturController.getStylingForSignatur(SignaturTyp.NETZ, "massnahme-test"))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("404");
	}
}
