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

package de.wps.radvis.backend.benutzer.domain.entity;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.valueObject.Mailadresse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@AllArgsConstructor
public class BenutzerDBListView {

	@Getter
	private final Long id;
	@Getter
	private final Name vorname;
	@Getter
	private final Name nachname;
	@Getter
	private final BenutzerStatus status;
	private final String organisationName;

	private final OrganisationsArt organisationsArt;
	@Getter
	private final Mailadresse email;

	private final String rollenString;

	public Set<Rolle> getRollen() {
		return Arrays.stream(this.rollenString.split(",")).map(Rolle::valueOf).collect(Collectors.toSet());
	}

	public String getOrganisation() {
		return Verwaltungseinheit.combineNameAndArt(organisationName, organisationsArt);
	}
}