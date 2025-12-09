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

package de.wps.radvis.backend.massnahme.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.MultiPolygon;

import de.wps.radvis.backend.massnahme.domain.dbView.MassnahmeListenDbView;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

public class CustomMassnahmeViewRepositoryImpl implements CustomMassnahmeViewRepository {
	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public List<MassnahmeListenDbView> findAllWithFilters(Optional<MultiPolygon> innerhalbBereich,
		Boolean historischeMassnahmenAnzeigen) {

		StringBuilder hqlStringBuilder = new StringBuilder();
		hqlStringBuilder.append("SELECT massnahmeListenDbView FROM MassnahmeListenDbView massnahmeListenDbView");

		if (innerhalbBereich.isPresent() || !historischeMassnahmenAnzeigen) {
			hqlStringBuilder.append(" WHERE ");
		}

		if (innerhalbBereich.isPresent()) {
			hqlStringBuilder.append(
				"intersects(CAST(massnahmeListenDbView.geometry AS org.locationtech.jts.geom.Geometry), CAST(:bereich as org.locationtech.jts.geom.Geometry)) = true");
		}

		if (innerhalbBereich.isPresent() && !historischeMassnahmenAnzeigen) {
			hqlStringBuilder.append(" AND ");
		}

		if (!historischeMassnahmenAnzeigen) {
			hqlStringBuilder.append(
				"massnahmeListenDbView.umsetzungsstatus NOT IN ("
					+ String.join(",", Umsetzungsstatus.HISTORISCH_STATUS.stream().map(u -> u.name()).toList())
					+ ") AND massnahmeListenDbView.archiviert = false");
		}

		TypedQuery<MassnahmeListenDbView> query = entityManager.createQuery(hqlStringBuilder.toString(),
			MassnahmeListenDbView.class);

		if (innerhalbBereich.isPresent()) {
			query.setParameter("bereich", innerhalbBereich.get());
		}

		return query.getResultStream().collect(Collectors.toList());
	}
}
