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

package de.wps.radvis.backend.netzfehler.schnittstelle.view;

import de.wps.radvis.backend.netzfehler.domain.entity.Netzfehler;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerBeschreibung;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerTyp;
import lombok.Getter;

@Getter
public class NetzfehlerView {

	private Long id;
	private NetzfehlerTyp netzfehlerTyp;
	private NetzfehlerBeschreibung netzfehlerBeschreibung;
	private String jobZuordnung;

	public NetzfehlerView(Netzfehler netzfehler) {
		this.id = netzfehler.getId();
		this.netzfehlerTyp = netzfehler.getNetzfehlerTyp();
		this.netzfehlerBeschreibung = netzfehler.getNetzfehlerBeschreibung();
		this.jobZuordnung = netzfehler.getJobZuordnung();
	}

	// Damit serialisiert Jackson NetzfehlerTyp mit toString() statt mit dem enum-Key
	public String getNetzfehlerTyp() {
		return this.netzfehlerTyp.toString();
	}

}
