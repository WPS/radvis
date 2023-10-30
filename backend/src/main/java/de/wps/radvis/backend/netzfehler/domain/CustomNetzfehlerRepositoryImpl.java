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

package de.wps.radvis.backend.netzfehler.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.netzfehler.domain.entity.Netzfehler;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerTyp;

public class CustomNetzfehlerRepositoryImpl implements CustomNetzfehlerRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public Iterable<Netzfehler> getNetzfehlerInBereich(Envelope bereich) {
		require(bereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		StringBuilder hqlStringBuilder = new StringBuilder();
		hqlStringBuilder.append(
			"SELECT netzfehler FROM Netzfehler netzfehler")
			.append(" WHERE")
			.append(whereClauseFuerBereich())
			.append(" AND netzfehler.erledigt = false");

		return entityManager.createQuery(hqlStringBuilder.toString(), Netzfehler.class)
			.setParameter("bereich", bereichAlsPolygon)
			.getResultStream().collect(Collectors.toSet());
	}

	@Override
	public Iterable<Netzfehler> getNetzfehlerInBereich(Envelope bereich, List<NetzfehlerTyp> netzfehlerTypen) {
		require(netzfehlerTypen, notNullValue());
		require(bereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		StringBuilder hqlStringBuilder = new StringBuilder();
		hqlStringBuilder.append(
			"SELECT netzfehler FROM Netzfehler netzfehler")
			.append(" WHERE").append(whereClauseFuerBereich())
			.append(" AND netzfehler.netzfehlerTyp IN :netzfehlerTypen")
			.append(" AND netzfehler.erledigt = false");

		return entityManager.createQuery(hqlStringBuilder.toString(), Netzfehler.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("netzfehlerTypen", netzfehlerTypen)
			.getResultStream().collect(Collectors.toSet());
	}

	private String whereClauseFuerBereich() {
		return " intersects(CAST(netzfehler.geometry AS org.locationtech.jts.geom.Geometry), CAST(:bereich as org.locationtech.jts.geom.Geometry)) = true";
	}
}
