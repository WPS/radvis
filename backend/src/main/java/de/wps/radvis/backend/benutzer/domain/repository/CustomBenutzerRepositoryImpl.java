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

import java.util.List;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerDBListView;
import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;

public class CustomBenutzerRepositoryImpl implements CustomBenutzerRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@SuppressWarnings("unchecked")
	public List<BenutzerDBListView> findAllDBListViews() {
		String sqlString = """
			SELECT
				b.id as id,
				b.vorname as vorname,
				b.nachname as nachname,
				b.status as status,
				o.name as organisationName,
				o.organisations_art as organisationsArt,
				b.mailadresse as email,
				string_agg(br.rollen, ',') as rollenString
			FROM benutzer b
			LEFT JOIN organisation o on b.organisation_id = o.id
			LEFT JOIN benutzer_rollen br on b.id = br.benutzer_id
			GROUP BY b.id, o.id
			""";

		Stream<Tuple> resultStream = entityManager.createNativeQuery(sqlString, Tuple.class).getResultStream();
		return resultStream.map(CustomBenutzerRepositoryImpl::getBenutzerDBListView).toList();

	}

	@Override
	@SuppressWarnings("unchecked")
	public List<BenutzerDBListView> findAllDBListViewsInVerwaltungseinheitWithId(Long verwaltungseinheitId) {
		String sqlString = """
			SELECT
				b.id as id,
				b.vorname as vorname,
				b.nachname as nachname,
				b.status as status,
				o.name as organisationName,
				o.organisations_art as organisationsArt,
				b.mailadresse as email,
				string_agg(br.rollen, ',') as rollenString
			FROM benutzer b
			LEFT JOIN organisation o on b.organisation_id = o.id
			LEFT JOIN benutzer_rollen br on b.id = br.benutzer_id
			WHERE o.id = :verwaltungseinheitId
			GROUP BY b.id, o.id
			""";

		Stream<Tuple> resultStream = entityManager.createNativeQuery(sqlString, Tuple.class)
			.setParameter("verwaltungseinheitId", verwaltungseinheitId)
			.getResultStream();
		return resultStream.map(CustomBenutzerRepositoryImpl::getBenutzerDBListView).toList();
	}

	@NotNull
	private static BenutzerDBListView getBenutzerDBListView(Tuple tuple) {
		return new BenutzerDBListView(
			(Long) tuple.get("id"),
			Name.of((String) tuple.get("vorname")),
			Name.of((String) tuple.get("nachname")),
			BenutzerStatus.valueOf((String) tuple.get("status")),
			(String) tuple.get("organisationName"),
			OrganisationsArt.valueOf((String) tuple.get("organisationsArt")),
			Mailadresse.of((String) tuple.get("email")),
			(String) tuple.get("rollenString"));
	}
}
