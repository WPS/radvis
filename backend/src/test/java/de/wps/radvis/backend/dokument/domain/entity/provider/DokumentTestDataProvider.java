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

package de.wps.radvis.backend.dokument.domain.entity.provider;

import java.time.LocalDateTime;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class DokumentTestDataProvider {
	public static Dokument.DokumentBuilder withDefaultValues() {
		Gebietskoerperschaft gebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();

		Benutzer benutzer = BenutzerTestDataProvider.defaultBenutzer()
			.organisation(gebietskoerperschaft)
			.build();

		return Dokument.builder()
			.dateiname("Default Datei")
			.datei("Default Datei".getBytes())
			.benutzer(benutzer)
			.datum(LocalDateTime.of(2023, 1, 26, 14, 31));
	}
}
