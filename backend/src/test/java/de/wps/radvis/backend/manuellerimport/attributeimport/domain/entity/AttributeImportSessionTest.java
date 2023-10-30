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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AttributeImportFormat;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class AttributeImportSessionTest {

	@Test
	void testComputedValues_neueSession_liefern0() {
		// arrange
		AttributeImportSession attributeImportSession = new AttributeImportSession(
			BenutzerTestDataProvider.defaultBenutzer().build(),
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build(),
			List.of("attribut1, attribut2"), AttributeImportFormat.LUBW);

		// act & assert
		assertThat(attributeImportSession.getAnzahlFeaturesOhneMatch()).isEqualTo(0);
		assertThat(attributeImportSession.getAnzahlKantenMitUneindeutigerAttributzuordnung()).isEqualTo(0);

	}

}
