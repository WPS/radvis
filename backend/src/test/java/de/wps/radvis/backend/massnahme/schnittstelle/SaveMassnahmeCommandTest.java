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

package de.wps.radvis.backend.massnahme.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.massnahme.domain.valueObject.BegruendungZurueckstellung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.ZurueckstellungsGrund;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public class SaveMassnahmeCommandTest {
	Validator javaxValidator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void validate_default() {
		assertThat(javaxValidator.validate(SaveMassnahmeCommandTestDataProvider.defaultValue().build())).isEmpty();
	}

	@Test
	void validate_begruendungZurueckstellung() {
		assertThat(javaxValidator.validate(SaveMassnahmeCommandTestDataProvider.defaultValue()
			.umsetzungsstatus(Umsetzungsstatus.ZURUECKGESTELLT).zurueckstellungsGrund(null).build())).isNotEmpty();
		assertThat(javaxValidator.validate(SaveMassnahmeCommandTestDataProvider.defaultValue()
			.umsetzungsstatus(Umsetzungsstatus.ZURUECKGESTELLT)
			.zurueckstellungsGrund(ZurueckstellungsGrund.FINANZIELLE_RESSOURCEN).build())).isEmpty();
		assertThat(javaxValidator.validate(SaveMassnahmeCommandTestDataProvider.defaultValue()
			.umsetzungsstatus(Umsetzungsstatus.PLANUNG)
			.zurueckstellungsGrund(ZurueckstellungsGrund.FINANZIELLE_RESSOURCEN).build())).isNotEmpty();

		assertThat(javaxValidator.validate(SaveMassnahmeCommandTestDataProvider.defaultValue()
			.umsetzungsstatus(Umsetzungsstatus.ZURUECKGESTELLT)
			.zurueckstellungsGrund(ZurueckstellungsGrund.WEITERE_GRUENDE).begruendungZurueckstellung(null).build()))
				.isNotEmpty();
		assertThat(javaxValidator.validate(SaveMassnahmeCommandTestDataProvider.defaultValue()
			.umsetzungsstatus(Umsetzungsstatus.ZURUECKGESTELLT)
			.zurueckstellungsGrund(ZurueckstellungsGrund.WEITERE_GRUENDE)
			.begruendungZurueckstellung(BegruendungZurueckstellung.of("Test")).build()))
				.isEmpty();
		assertThat(javaxValidator.validate(SaveMassnahmeCommandTestDataProvider.defaultValue()
			.umsetzungsstatus(Umsetzungsstatus.ZURUECKGESTELLT)
			.zurueckstellungsGrund(ZurueckstellungsGrund.WEITERE_PLANUNGEN_IM_ZUSAMMENHANG)
			.begruendungZurueckstellung(BegruendungZurueckstellung.of("Test")).build()))
				.isNotEmpty();

		assertThat(javaxValidator
			.validate(SaveMassnahmeCommandTestDataProvider.defaultValue().umsetzungsstatus(Umsetzungsstatus.IDEE)
				.zurueckstellungsGrund(null).begruendungZurueckstellung(BegruendungZurueckstellung.of("Test")).build()))
					.isNotEmpty();
		assertThat(javaxValidator
			.validate(SaveMassnahmeCommandTestDataProvider.defaultValue().umsetzungsstatus(Umsetzungsstatus.IDEE)
				.zurueckstellungsGrund(null).begruendungZurueckstellung(null).build())).isEmpty();
	}

}
