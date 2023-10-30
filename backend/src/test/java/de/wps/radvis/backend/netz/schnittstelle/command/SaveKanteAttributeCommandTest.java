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

package de.wps.radvis.backend.netz.schnittstelle.command;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;

class SaveKanteAttributeCommandTest {

	@Test
	public void testIsRadNetzIstStandardsOnlyForRadnetzTrue_EnthaeltRadNetzIstStandardUndRadNetzNetzklasse_true() {
		// Arrange
		SaveKanteAttributeCommand saveKanteAttributeCommand = new SaveKanteAttributeCommand();
		ReflectionTestUtils.setField(saveKanteAttributeCommand, "netzklassen",
			Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KREISNETZ_ALLTAG));
		ReflectionTestUtils.setField(saveKanteAttributeCommand, "istStandards",
			Set.of(IstStandard.STARTSTANDARD_RADNETZ, IstStandard.BASISSTANDARD));

		// Act
		boolean result = saveKanteAttributeCommand.isRadNetzIstStandardsOnlyForRadnetzTrue();

		// Assert
		assertThat(result).isTrue();
	}

	@Test
	public void testIsRadNetzIstStandardsOnlyForRadnetzTrue_EnthaeltRadNetzIstStandardUndKeineRadNetzNetzklasse_false() {
		// Arrange
		SaveKanteAttributeCommand saveKanteAttributeCommand = new SaveKanteAttributeCommand();
		ReflectionTestUtils.setField(saveKanteAttributeCommand, "netzklassen",
			Set.of(Netzklasse.RADSCHNELLVERBINDUNG, Netzklasse.KREISNETZ_ALLTAG));
		ReflectionTestUtils.setField(saveKanteAttributeCommand, "istStandards",
			Set.of(IstStandard.STARTSTANDARD_RADNETZ, IstStandard.BASISSTANDARD));

		// Act
		boolean result = saveKanteAttributeCommand.isRadNetzIstStandardsOnlyForRadnetzTrue();

		// Assert
		assertThat(result).isFalse();
	}

	@Test
	public void testIsRadNetzIstStandardsOnlyForRadnetzTrue_EnthaeltKeinRadNetzIstStandardUndKeineRadNetzNetzklasse_true() {
		// Arrange
		SaveKanteAttributeCommand saveKanteAttributeCommand = new SaveKanteAttributeCommand();
		ReflectionTestUtils.setField(saveKanteAttributeCommand, "netzklassen",
			Set.of(Netzklasse.RADVORRANGROUTEN, Netzklasse.KREISNETZ_ALLTAG));
		ReflectionTestUtils.setField(saveKanteAttributeCommand, "istStandards",
			Set.of(IstStandard.RADSCHNELLVERBINDUNG, IstStandard.BASISSTANDARD));

		// Act
		boolean result = saveKanteAttributeCommand.isRadNetzIstStandardsOnlyForRadnetzTrue();

		// Assert
		assertThat(result).isTrue();
	}

	@Test
	public void testIsRadNetzIstStandardsOnlyForRadnetzTrue_keineNetzklasseGesetztAberStandard_false() {
		// Arrange
		SaveKanteAttributeCommand saveKanteAttributeCommand = new SaveKanteAttributeCommand();
		ReflectionTestUtils.setField(saveKanteAttributeCommand, "netzklassen", Set.of());
		ReflectionTestUtils.setField(saveKanteAttributeCommand, "istStandards",
			Set.of(IstStandard.STARTSTANDARD_RADNETZ));

		// Act
		boolean result = saveKanteAttributeCommand.isRadNetzIstStandardsOnlyForRadnetzTrue();

		// Assert
		assertThat(result).isFalse();
	}

}