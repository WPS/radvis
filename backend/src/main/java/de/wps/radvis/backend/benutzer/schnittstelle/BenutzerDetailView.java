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

package de.wps.radvis.backend.benutzer.schnittstelle;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BenutzerDetailView {

	private boolean registriert;

	private String name;

	private String vorname;

	private String basicAuthAnmeldename;

	private VerwaltungseinheitView organisation;

	private Set<Recht> rechte;

	private BenutzerStatus status;

	public BenutzerDetailView(Benutzer benutzer) {
		registriert = true;
		status = benutzer.getStatus();
		name = benutzer.getNachname().getValue();
		vorname = benutzer.getVorname().getValue();
		basicAuthAnmeldename = benutzer.getBasicAuthAnmeldeName();
		organisation = new VerwaltungseinheitView(benutzer.getOrganisation());
		rechte = benutzer.getRollen().stream()
			.map(Rolle::getRechte)
			.flatMap(Arrays::stream)
			.collect(Collectors.toSet());
	}

	public static BenutzerDetailView ofUnregisteredUser() {
		return new BenutzerDetailView();
	}
}
