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

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
@Getter
public class OrganisationView {
	private final boolean aktuellerBenutzerDarfBearbeiten;
	private final Set<VerwaltungseinheitView> zustaendigFuerBereichOf;
	private final String name;
	private final OrganisationsArt organisationsArt;
	private final Optional<VerwaltungseinheitView> uebergeordneteOrganisation;
	private final boolean aktiv;
	private final Long version;
	private final Long id;

	public OrganisationView(Organisation organisation, boolean aktuellerBenutzerDarfBearbeiten) {
		name = organisation.getName();
		organisationsArt = organisation.getOrganisationsArt();
		uebergeordneteOrganisation = organisation.getUebergeordneteVerwaltungseinheit()
			.map(VerwaltungseinheitView::new);
		this.aktuellerBenutzerDarfBearbeiten = aktuellerBenutzerDarfBearbeiten;
		this.zustaendigFuerBereichOf = organisation.getZustaendigFuerBereichOf().stream()
			.map(VerwaltungseinheitView::new)
			.collect(Collectors.toSet());
		aktiv = organisation.getAktiv();
		version = organisation.getVersion();
		id = organisation.getId();
	}
}
