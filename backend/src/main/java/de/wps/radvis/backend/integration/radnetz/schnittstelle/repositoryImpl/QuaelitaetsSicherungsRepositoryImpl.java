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

package de.wps.radvis.backend.integration.radnetz.schnittstelle.repositoryImpl;

import java.util.List;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.radnetz.domain.repository.QualitaetsSicherungsRepository;
import de.wps.radvis.backend.netz.domain.repository.CommonQueryLibrary;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

public class QuaelitaetsSicherungsRepositoryImpl implements QualitaetsSicherungsRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional
	public void setzeDLMAlsGrundnetz(Verwaltungseinheit landkreis) {
		if (landkreis.getBereich().isEmpty()) {
			return;
		}
		entityManager.createQuery(
				"UPDATE Kante kante "
					+ "SET kante.isGrundnetz = false "
					+ "WHERE kante.quelle = :quelleRadNETZ "
					+ "AND " + CommonQueryLibrary.whereClauseFuerBereichKante())
			.setParameter("quelleRadNETZ", QuellSystem.RadNETZ)
			.setParameter("bereich", landkreis.getBereich().get())
			.executeUpdate();

		entityManager.createQuery(
				"UPDATE Kante kante "
					+ "SET kante.isGrundnetz = true "
					+ "WHERE kante.quelle = :quelleDLM "
					+ "AND " + CommonQueryLibrary.whereClauseFuerBereichKante())
			.setParameter("quelleDLM", QuellSystem.DLM)
			.setParameter("bereich", landkreis.getBereich().get())
			.executeUpdate();
	}

	/**
	 * Überprüft für eine Menge von Kanten, ob alle in qualitaetsgesicherten Landkreisen liegen
	 *
	 * @param kanteIds
	 * 	Die Ids der zu prüfenden Kanten
	 * @return true, falls jede Kante in mindestens einem qualitaetsgesicherten Landkreis liegt
	 */
	@Override
	public boolean liegenAlleInQualitaetsgesichertenLandkreisen(List<Long> kanteIds) {
		return entityManager.createQuery(
				"SELECT max(CAST(organisation.istQualitaetsgesichert AS java.lang.Integer)) "
					+ "FROM Kante kante, Verwaltungseinheit organisation "
					+ "WHERE intersects(CAST(kante.geometry AS org.locationtech.jts.geom.Geometry), CAST(organisation.bereich as org.locationtech.jts.geom.Geometry)) = true "
					+ "AND organisation.organisationsArt = :organisationsArtKreis "
					+ "AND kante.id IN :ids "
					+ "GROUP BY kante.id",
				Integer.class)
			.setParameter("organisationsArtKreis", OrganisationsArt.KREIS)
			.setParameter("ids", kanteIds)
			.getResultList().stream().allMatch(i -> i > 0);
	}
}
