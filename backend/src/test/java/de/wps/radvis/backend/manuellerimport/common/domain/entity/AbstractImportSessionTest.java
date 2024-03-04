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

package de.wps.radvis.backend.manuellerimport.common.domain.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.MultiPolygon;

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.ImportLogEintrag;

class AbstractImportSessionTest {

	private AbstractImportSession importSession;

	@BeforeEach
	void setup() {
		importSession = new AbstractImportSession(BenutzerTestDataProvider.defaultBenutzer().build()) {
			@Override
			public long getAnzahlFeaturesOhneMatch() {
				return 0;
			}

			@Override
			public MultiPolygon getBereich() {
				return null;
			}

			@Override
			public String getBereichName() {
				return null;
			}
		};
	}

	@Test
	void testHatFehler_keinLogEintrag_false() {
		// act & assert
		assertThat(importSession.hatFehler()).isFalse();
	}

	@Test
	void testHatFehler_nurWarnung_false() {
		// arrange
		importSession.addLogEintrag(ImportLogEintrag.ofWarnung("Achtung. Pass auf!"));

		// act & assert
		assertThat(importSession.hatFehler()).isFalse();
	}

	@Test
	void testHatFehler_fehlerVorhanden_true() {
		// arrange
		importSession.addLogEintrag(ImportLogEintrag.ofError("Stop. Geht gar nicht!"));

		// act & assert
		assertThat(importSession.hatFehler()).isTrue();
	}

	@Test
	void testAddLogEintrag() {
		// act
		importSession.addLogEintrag(ImportLogEintrag.ofError("Stop. Geht gar nicht!"));
		importSession.addLogEintrag(ImportLogEintrag.ofWarnung("Achtung. Pass auf!"));

		// assert
		assertThat(importSession.getLog()).containsExactlyInAnyOrder(
			ImportLogEintrag.ofError("Stop. Geht gar nicht!"), ImportLogEintrag.ofWarnung("Achtung. Pass auf!"));
	}
}
