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

	List<Verwaltungseinheit> findByNameAndOrganisationsArt(String name, OrganisationsArt organisationsArt);

	List<Verwaltungseinheit> findAllByNameContainingAndOrganisationsArt(String name, OrganisationsArt organisationsArt);

	List<Verwaltungseinheit> findAllByName(String name);

	List<Verwaltungseinheit> findAllByNameContaining(String operatorString);

	@Query(
		"SELECT new de.wps.radvis.backend.organisation.domain.dbView.VerwaltungseinheitDbView(organisation.id, organisation.name, organisation.organisationsArt, organisation.uebergeordneteOrganisation.id, organisation.aktiv)"
			+ " FROM Verwaltungseinheit organisation WHERE organisation.id IN :gebietskoerperschaftIds")
	List<VerwaltungseinheitDbView> findAllDbViewsById(List<Long> gebietskoerperschaftIds);

	long countByOrganisationsArt(OrganisationsArt organisationsArt);

	@Query(
		"SELECT COUNT(DISTINCT o.id) " +
			"FROM Verwaltungseinheit o " +
			"JOIN Benutzer b ON o.id = b.organisation.id " +
			"WHERE o.organisationsArt = :organisationsArt " +
			"AND b.status = de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus.AKTIV")
	long countVerwaltungseinheitenWithAtLeastOneActiveBenutzer(OrganisationsArt organisationsArt);

	@Query(value = "SELECT org.* FROM organisation org " +
		"WHERE org.organisations_art = 'KREIS' " +
		"AND ( " +
		"    SELECT COALESCE(SUM(st_length(abschnitt.geometry)), 0) " +
		"    FROM geoserver_radvisnetz_kante_abschnitte_materialized_view abschnitt " +
		"    WHERE abschnitt.status = 'UNTER_VERKEHR' " +
		"    AND st_intersects(abschnitt.geometry, org.bereich) " +
		"    AND ( " +
		"        (abschnitt.netzklassen IS NOT NULL AND position('KREISNETZ_FREIZEIT' IN abschnitt.netzklassen) > 0) " +
		"        OR (abschnitt.netzklassen IS NOT NULL AND position('KREISNETZ_ALLTAG' IN abschnitt.netzklassen) > 0) "
		+
		"    ) " +
		") >= :laengeInMetern", nativeQuery = true)
	List<Verwaltungseinheit> findAllKreiseWithKreisnetzGreaterOrEqual(Integer laengeInMetern);

	@Query(value = "SELECT org.* FROM organisation org " +
		"LEFT JOIN geoserver_radvisnetz_kante_abschnitte_materialized_view abschnitt " +
		"ON st_intersects(abschnitt.geometry, org.bereich) " +
		"AND abschnitt.status = 'UNTER_VERKEHR' " +
		"AND ( " +
		"   (abschnitt.netzklassen IS NOT NULL AND position('KOMMUNALNETZ_FREIZEIT' IN abschnitt.netzklassen) > 0) " +
		"   OR (abschnitt.netzklassen IS NOT NULL AND position('KOMMUNALNETZ_ALLTAG' IN abschnitt.netzklassen) > 0) " +
		") " +
		"WHERE org.organisations_art = 'GEMEINDE' " +
		"GROUP BY org.id " +
		"HAVING COALESCE(SUM(st_Length(abschnitt.geometry)), 0) >= :laengeInMetern", nativeQuery = true)
	List<Verwaltungseinheit> findAllKommunenWithKommunalnetzGreaterOrEqual(Integer laengeInMetern);
}
