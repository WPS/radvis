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

package de.wps.radvis.backend.administration.schnittstelle.view;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerDBListView;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BenutzerListView {
	private final Long id;
	private final Name vorname;
	private final Name nachname;
	private final BenutzerStatus status;
	private final String organisation;
	private final Mailadresse email;

	public BenutzerListView(Benutzer benutzer) {
		id = benutzer.getId();
		vorname = benutzer.getVorname();
		nachname = benutzer.getNachname();
		status = benutzer.getStatus();
		organisation = benutzer.getOrganisation().getDisplayText();
		email = benutzer.getMailadresse();
	}

	public BenutzerListView(BenutzerDBListView benutzer) {
		id = benutzer.getId();
		vorname = benutzer.getVorname();
		nachname = benutzer.getNachname();
		status = benutzer.getStatus();
		organisation = benutzer.getOrganisation();
		email = benutzer.getEmail();
	}
}
