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

package de.wps.radvis.backend.administration.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.dbView.VerwaltungseinheitDbView;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AdministrationService {

	VerwaltungseinheitRepository verwaltungseinheitRepository;
	OrganisationRepository organisationRepository;

	public List<VerwaltungseinheitDbView> getAllZuweisbareOrganisationenForBenutzer(Benutzer benutzer) {
		if (benutzer.hatRecht(Recht.ALLE_BENUTZER_UND_ORGANISATIONEN_BEARBEITEN)) {
			return verwaltungseinheitRepository.findAllAsView().stream()
				.filter(org -> org.getOrganisationsArt().istGebietskoerperschaft())
				.collect(Collectors.toList());
		}

		if (benutzer.hatRecht(Recht.BENUTZER_UND_ORGANISATIONEN_MEINES_VERWALTUNGSBEREICHS_BEARBEITEN)) {
			return this.verwaltungseinheitRepository.findAllUntergeordnet(benutzer.getOrganisation()).stream()
				.filter(org -> org.getOrganisationsArt().istGebietskoerperschaft())
				.map(VerwaltungseinheitDbView::new)
				.collect(Collectors.toList());
		}

		if (benutzer.hatRecht(Recht.EIGENEN_BEREICH_EINER_ORGANISATION_ZUORDNEN)
			&& benutzer.getOrganisation().getOrganisationsArt().istGebietskoerperschaft()) {
			return List.of(new VerwaltungseinheitDbView(benutzer.getOrganisation()));
		}

		return List.of();
	}

	public Organisation loadForModification(Long id, Long version) {
		require(id, notNullValue());
		require(version, notNullValue());

		Organisation verwaltungseinheit = organisationRepository.findById(id)
			.orElseThrow(EntityNotFoundException::new);

		if (!Objects.equals(verwaltungseinheit.getVersion(), version)) {
			throw new OptimisticLockException(
				"Ein/e andere Nutzer/in hat dieses Objekt bearbeitet. "
					+ "Bitte laden Sie das Objekt neu und führen Sie Ihre Änderungen erneut durch.");
		}

		return verwaltungseinheit;
	}
}
