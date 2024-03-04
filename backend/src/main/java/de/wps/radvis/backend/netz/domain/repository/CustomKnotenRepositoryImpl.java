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

package de.wps.radvis.backend.netz.domain.repository;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;
import java.util.Set;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;
import org.springframework.beans.factory.annotation.Autowired;

import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

public class CustomKnotenRepositoryImpl implements CustomKnotenRepository {

	private static final String IS_VERWAISTER_DLM_KNOTEN_CLAUSE = "not exists (select kante from Kante kante where"
		+ " (kante.quelle = :dlmQuelle or kante.quelle = :radvisQuelle)"
		+ " and (kante.vonKnoten = knoten or kante.nachKnoten = knoten))";

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * Wir nutzen hier DependencyInjection via Autowired außerhalb einer @Configuration-Klasse, weil es sich um eine
	 * temporäre Lösung handelt und diese Custom Implementation für das Repo via @EnableJpaRepositories und @Autowired
	 * in der @Configuration-Klasse unter das Interface geschoben wird. Siehe Kommentar in
	 * {@link FeatureToggleProperties}
	 */
	@Autowired
	private FeatureToggleProperties radNetzGematchtesDlmStattRadNetzService;

	@Override
	@Transactional
	public void buildIndex() {

		entityManager.createNativeQuery(
				"CREATE INDEX knoten_idx_tmp ON knoten USING GIST (point, quelle)")
			.executeUpdate();

		entityManager.createNativeQuery(
				"DROP INDEX IF EXISTS knoten_idx")
			.executeUpdate();

		entityManager.createNativeQuery(
				"ALTER INDEX knoten_idx_tmp RENAME TO knoten_idx")
			.executeUpdate();

	}

	@Override
	public List<Knoten> getKnotenFuerKanteIds(Set<Long> ids) {
		StringBuilder HQLBuilder = new StringBuilder();

		HQLBuilder.append("SELECT DISTINCT(knoten) FROM Knoten knoten")
			.append(" INNER JOIN Kante kante ON (knoten.id = kante.vonKnoten.id OR knoten.id = kante.nachKnoten.id)")
			.append(" AND kante.id IN :kanteIds ");

		return entityManager.createQuery(HQLBuilder.toString(), Knoten.class)
			.setParameter("kanteIds", ids)
			.getResultList();
	}

	@Override
	public List<Knoten> getKnotenInBereichNachNetzklassen(Envelope sichtbereich,
		Set<NetzklasseFilter> netzklassenFilter) {
		require(sichtbereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(sichtbereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		Set<Netzklasse> netzklassen = CommonQueryLibrary.getNetzklassenParameter(netzklassenFilter);

		boolean orNichtKlassifiziert = netzklassenFilter.contains(NetzklasseFilter.NICHT_KLASSIFIZIERT);

		StringBuilder HQLBuilder = new StringBuilder();
		HQLBuilder.append("SELECT DISTINCT knoten FROM Knoten knoten")
			.append(" JOIN Kante kante ON")
			.append(
				" intersects(CAST(knoten.point AS org.locationtech.jts.geom.Geometry), CAST(:bereich as org.locationtech.jts.geom.Geometry)) = true")
			.append(" AND (knoten.id = kante.vonKnoten.id OR knoten.id = kante.nachKnoten.id)")
			.append(" LEFT OUTER JOIN kante.kantenAttributGruppe as kag")
			.append(" LEFT OUTER JOIN kag.netzklassen as nk")
			.append(" WHERE")
			.append(CommonQueryLibrary.whereClauseFuerBereichKante())
			.append(" AND").append(CommonQueryLibrary.whereClauseFuerNetzklassen(orNichtKlassifiziert))
			.append(" AND ")
			.append(CommonQueryLibrary.whereClauseGrundnetz(radNetzGematchtesDlmStattRadNetzService.isShowDlm()));

		return entityManager.createQuery(HQLBuilder.toString(), Knoten.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("netzklassen", netzklassen)
			.getResultList();
	}

	@Override
	public List<Knoten> getKnotenInBereichFuerQuelle(Envelope sichtbereich, QuellSystem quelle) {
		require(sichtbereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(sichtbereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		StringBuilder HQLBuilder = new StringBuilder();
		HQLBuilder.append("SELECT DISTINCT knoten FROM Knoten knoten")
			.append(" WHERE")
			.append(CommonQueryLibrary.whereClauseFuerBereichKnoten())
			.append(" AND ").append("knoten.quelle = :quelle");

		return entityManager.createQuery(HQLBuilder.toString(), Knoten.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("quelle", quelle)
			.getResultList();
	}

	@Override
	public int deleteVerwaisteDLMKnoten() {
		return entityManager.createQuery(
				"delete from Knoten knoten where " + IS_VERWAISTER_DLM_KNOTEN_CLAUSE
					+ "	 and knoten.quelle = :dlmQuelle")
			.setParameter("dlmQuelle", QuellSystem.DLM)
			.setParameter("radvisQuelle", QuellSystem.RadVis)
			.executeUpdate();
	}

	@Override
	public List<Knoten> findVerwaisteDLMKnoten() {
		return entityManager.createQuery(
				"SELECT knoten from Knoten knoten where " + IS_VERWAISTER_DLM_KNOTEN_CLAUSE
					+ "	 and knoten.quelle = :dlmQuelle",
				Knoten.class).setParameter("dlmQuelle", QuellSystem.DLM)
			.setParameter("radvisQuelle", QuellSystem.RadVis)
			.getResultList();
	}

	@Override
	public List<Knoten> getKnotenInBereichNachQuellen(Envelope sichtbereich, Set<QuellSystem> quellsysteme) {
		require(sichtbereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(sichtbereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		StringBuilder HQLBuilder = new StringBuilder();
		HQLBuilder.append("SELECT DISTINCT knoten FROM Knoten knoten")
			.append(" WHERE")
			.append(CommonQueryLibrary.whereClauseFuerBereichKnoten())
			.append(" AND ").append("knoten.quelle in :quellen");

		return entityManager.createQuery(HQLBuilder.toString(), Knoten.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("quellen", quellsysteme)
			.getResultList();
	}
}
