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

package de.wps.radvis.backend.dokument.schnittstelle.view;

import java.time.LocalDateTime;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import lombok.Getter;

@Getter
public class DokumentListView {

	private final Long dokumentId;
	private final String dateiname;
	private final int dateigroesseInBytes;
	private final String benutzerVorname;
	private final String benutzerNachname;
	private final LocalDateTime datum;

	public DokumentListView(Dokument dokument) {
		this.dokumentId = dokument.getId();
		this.dateiname = dokument.getDateiname();
		this.dateigroesseInBytes = dokument.getDateigroesseInBytes();
		this.datum = dokument.getDatum();

		Benutzer benutzer = dokument.getBenutzer();
		this.benutzerNachname = benutzer.getNachname().toString();
		this.benutzerVorname = benutzer.getVorname().toString();
	}
}
