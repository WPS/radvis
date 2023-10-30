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

package de.wps.radvis.backend.integration.radwegedb.domain;

import de.wps.radvis.backend.integration.netzbildung.domain.NetzbildungsProtokollService;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.netzfehler.domain.entity.Netzfehler;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerBeschreibung;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerTyp;
import lombok.NonNull;

public class RadwegeDBNetzbildungProtokollService extends NetzbildungsProtokollService {

	public RadwegeDBNetzbildungProtokollService(
		@NonNull NetzfehlerRepository netzfehlerRepository) {
		super(netzfehlerRepository);
	}

	public void handle(MultilinestringHatMehrereLinestringsException e, String jobName) {
		Netzfehler netzfehler = new Netzfehler(NetzfehlerTyp.NETZBILDUNG,
			NetzfehlerBeschreibung.of(e.getMessage()),
			jobName,
			e.getGeometry());
		protokolliere(netzfehler);
	}
}
