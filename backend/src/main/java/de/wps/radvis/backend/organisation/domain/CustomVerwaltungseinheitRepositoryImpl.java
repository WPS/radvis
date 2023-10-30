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
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

public class CustomVerwaltungseinheitRepositoryImpl implements CustomVerwaltungseinheitRepository {
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<Verwaltungseinheit> findAllByOrganisationsArtContainingGeometry(OrganisationsArt organisationsArt,
		Geometry geometry) {

		String hqlString = "SELECT DISTINCT organisation FROM Verwaltungseinheit organisation"
			+ " WHERE "
			+ "intersects(CAST(organisation.bereich AS org.locationtech.jts.geom.Geometry), CAST(:geometry as org.locationtech.jts.geom.Geometry)) = true"
			+ " AND " + "organisation.organisationsArt = :organisationsArt";

		return entityManager.createQuery(hqlString, Verwaltungseinheit.class)
			.setParameter("geometry", geometry)
			.setParameter("organisationsArt", organisationsArt)
			.getResultList();
	}

	@Override
	public List<Verwaltungseinheit> findAllUntergeordnet(Verwaltungseinheit verwaltungseinheit) {
		List<Long> ids = this.findAllUntergeordnetIds(verwaltungseinheit.getId());

		return entityManager.createQuery(
				"Select organisation FROM Verwaltungseinheit organisation WHERE organisation.id IN :ids",
				Verwaltungseinheit.class)
			.setParameter("ids", ids)
			.getResultList();
	}

	@Override
	public List<Long> findAllUntergeordnetIds(Long verwaltungseinheitId) {
		// Für Recursive-Queries siehe z.B.:
		// https://www.postgresqltutorial.com/postgresql-tutorial/postgresql-recursive-query/
		// und https://www.postgresql.org/docs/current/queries-with.html#QUERIES-WITH-RECURSIVE
		@SuppressWarnings("unchecked")
		List<Long> ids = (List<Long>) entityManager.createNativeQuery("""
				WITH RECURSIVE untergeordnete_orgas AS (
				    SELECT
				        id,
				        uebergeordnete_organisation_id
				    FROM
				        organisation
				    WHERE
				            id = :id
				    UNION
				    SELECT
				        o.id,
				        o.uebergeordnete_organisation_id
				    FROM
				        organisation o
				            INNER JOIN untergeordnete_orgas uo ON uo.id = o.uebergeordnete_organisation_id
				) SELECT
				    id
				FROM
				    untergeordnete_orgas;
				""")
			.setParameter("id", verwaltungseinheitId)
			.getResultStream()
			.collect(Collectors.toList());

		return ids;
	}
}
