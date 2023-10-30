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

import java.util.Set;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.Getter;

@Getter
public class BenutzerEditView {
	private final Name vorname;
	private final Name nachname;
	private final BenutzerStatus status;
	private final VerwaltungseinheitView organisation;
	private final Mailadresse email;
	private final long version;
	private final Set<Rolle> rollen;

	public BenutzerEditView(Benutzer benutzer) {
		this.vorname = benutzer.getVorname();
		this.nachname = benutzer.getNachname();
		this.status = benutzer.getStatus();
		this.organisation = new VerwaltungseinheitView(benutzer.getOrganisation());
		this.email = benutzer.getMailadresse();
		this.version = benutzer.getVersion();
		this.rollen = benutzer.getRollen();
	}
}
