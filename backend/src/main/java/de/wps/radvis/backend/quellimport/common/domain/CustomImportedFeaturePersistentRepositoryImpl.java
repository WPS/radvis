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

package de.wps.radvis.backend.quellimport.common.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeatureMapView;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

public class CustomImportedFeaturePersistentRepositoryImpl implements CustomImportedFeaturePersistentRepository {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	@Transactional
	public List<ImportedFeatureMapView> getFeaturesInBereich(QuellSystem quelle, Art art, Envelope bereich) {
		require(quelle, notNullValue());
		require(bereich, notNullValue());

		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		String hql =
			"SELECT new de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeatureMapView(importedFeature.id, importedFeature.geometrie, importedFeature.anteilProjiziert) "
				+ "FROM ImportedFeature importedFeature "
				+ "WHERE importedFeature.quelle = :quelle "
				+ "AND importedFeature.art = :art "
				+ "AND intersects(CAST(importedFeature.geometrie AS org.locationtech.jts.geom.Geometry), CAST(:bereich as org.locationtech.jts.geom.Geometry)) = true";

		return entityManager
			.createQuery(hql, de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeatureMapView.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("quelle", quelle)
			.setParameter("art", art)
			.getResultList();
	}

	@Override
	@Transactional
	public void buildIndex() {

		entityManager.createNativeQuery(
				"CREATE INDEX test_idx_tmp ON imported_feature USING GIST (geometrie, quelle, art)")
			.executeUpdate();

		entityManager.createNativeQuery(
				"DROP INDEX IF EXISTS test_idx")
			.executeUpdate();

		entityManager.createNativeQuery(
				"ALTER INDEX test_idx_tmp RENAME TO test_idx")
			.executeUpdate();

	}
}
