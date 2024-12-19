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

package de.wps.radvis.backend.benutzer.domain.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public interface BenutzerRepository extends CrudRepository<Benutzer, Long>, CustomBenutzerRepository {
	boolean existsByServiceBwId(ServiceBwId serviceBwId);

	Optional<Benutzer> findByServiceBwId(ServiceBwId serviceBwId);

	List<Benutzer> findByOrganisationAndStatus(Verwaltungseinheit verwaltungseinheit,
		BenutzerStatus benutzerStatus);

	List<Benutzer> findByStatusAndAblaufdatumBefore(BenutzerStatus benutzerStatus, LocalDate localDate);

	List<Benutzer> findByStatusAndRollenIsNotContainingAndLetzteAktivitaetBefore(BenutzerStatus benutzerStatus,
		Rolle rolle, LocalDate localDate);

	List<Benutzer> findByRollenAndStatus(Rolle rolle, BenutzerStatus benutzerStatus);

	List<Benutzer> findByOrganisationAndRollen(Verwaltungseinheit verwaltungseinheit, Rolle rolle);

	long countByStatus(BenutzerStatus status);
}
