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

package de.wps.radvis.backend.organisation.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.organisation.domain.dbView.VerwaltungseinheitDbView;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.validation.constraints.NotNull;

public interface VerwaltungseinheitRepository
	extends Repository<Verwaltungseinheit, Long>, CustomVerwaltungseinheitRepository {
	/**
	 * Verwaltungseinheit nicht casten!
	 * Stattdessen dedizierte Repositories fuer Organisation / Gebietskoerperschaft verwenden!
	 */
	Optional<Verwaltungseinheit> findById(long id);

	@NotNull
	List<Verwaltungseinheit> findAll();

	@Query(
		"SELECT new de.wps.radvis.backend.organisation.domain.dbView.VerwaltungseinheitDbView(organisation.id, organisation.name, organisation.organisationsArt, organisation.uebergeordneteOrganisation.id, organisation.aktiv)"
			+ " FROM Verwaltungseinheit organisation")
	List<VerwaltungseinheitDbView> findAllAsView();

	@Query(
		"SELECT new de.wps.radvis.backend.organisation.domain.dbView.VerwaltungseinheitDbView(organisation.id, organisation.name, organisation.organisationsArt, organisation.uebergeordneteOrganisation.id, organisation.aktiv)"
			+ " FROM Verwaltungseinheit organisation"
			+ " WHERE organisation.aktiv = true")
	List<VerwaltungseinheitDbView> findAllAktiveAsView();

	List<Verwaltungseinheit> findByOrganisationsArt(OrganisationsArt organisationsArt);

	Verwaltungseinheit findByName(String name);

	Optional<Verwaltungseinheit> findByNameAndOrganisationsArt(String name, OrganisationsArt organisationsArt);

	List<Verwaltungseinheit> findAllByNameContainingAndOrganisationsArt(String name, OrganisationsArt organisationsArt);

	List<Verwaltungseinheit> findAllByName(String name);

	List<Verwaltungseinheit> findAllByNameContaining(String operatorString);

	@Query(
		"SELECT new de.wps.radvis.backend.organisation.domain.dbView.VerwaltungseinheitDbView(organisation.id, organisation.name, organisation.organisationsArt, organisation.uebergeordneteOrganisation.id, organisation.aktiv)"
			+ " FROM Verwaltungseinheit organisation WHERE organisation.id IN :gebietskoerperschaftIds")
	List<VerwaltungseinheitDbView> findAllDbViewsById(List<Long> gebietskoerperschaftIds);
}
