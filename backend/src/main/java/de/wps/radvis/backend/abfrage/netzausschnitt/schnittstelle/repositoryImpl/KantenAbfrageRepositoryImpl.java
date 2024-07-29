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

package de.wps.radvis.backend.abfrage.netzausschnitt.schnittstelle.repositoryImpl;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Polygon;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.KantenAbfrageRepository;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.GeometrienVerlaufMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteFuehrungsformAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteGeschwindigkeitAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteNetzklasseMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteZustaendigkeitAttributeView;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.repository.CommonQueryLibrary;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

public class KantenAbfrageRepositoryImpl implements KantenAbfrageRepository {

	@PersistenceContext
	private EntityManager entityManager;

	private FeatureToggleProperties featureToggleProperties;

	public KantenAbfrageRepositoryImpl(FeatureToggleProperties featureToggleProperties) {
		require(featureToggleProperties, notNullValue());
		this.featureToggleProperties = featureToggleProperties;
	}

	@Override
	@Transactional
	public Set<KanteMapView> getKantenMapViewInBereich(Envelope bereich, Set<NetzklasseFilter> netzklassenFilter) {
		require(bereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		Set<Netzklasse> netzklassen = CommonQueryLibrary.getNetzklassenParameter(netzklassenFilter);

		boolean orNichtKlassifiziert = netzklassenFilter.contains(NetzklasseFilter.NICHT_KLASSIFIZIERT);

		StringBuilder hqlStringBuilder = new StringBuilder();
		hqlStringBuilder.append(
			"SELECT DISTINCT new de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteMapView(kante.id, kante.geometry, kante.verlaufLinks, kante.verlaufRechts, kante.isZweiseitig)"
				+ " FROM Kante kante")
			.append(" LEFT OUTER JOIN kante.kantenAttributGruppe as kag")
			.append(" LEFT OUTER JOIN kag.netzklassen as nk")
			.append(" WHERE")
			.append(CommonQueryLibrary.whereClauseFuerNetzklassen(orNichtKlassifiziert))
			.append(" AND ").append(CommonQueryLibrary.whereClauseFuerBereichKante())
			.append(" AND ")
			.append(CommonQueryLibrary.whereClauseGrundnetz(featureToggleProperties.isShowDlm()));

		return entityManager.createQuery(hqlStringBuilder.toString(), KanteMapView.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("netzklassen", netzklassen)
			.getResultStream().collect(Collectors.toSet());
	}

	@Override
	public Set<KanteNetzklasseMapView> getKantenMapViewInBereichDlm(Envelope bereich) {
		require(bereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		StringBuilder hqlStringBuilder = new StringBuilder();
		hqlStringBuilder.append(
			"SELECT new de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteNetzklasseMapView(kante.id, kante.geometry, kante.verlaufLinks, kante.verlaufRechts, kante.isZweiseitig, kante.kantenAttributGruppe)")
			.append(" FROM Kante kante")
			.append(" WHERE")
			.append(" kante.quelle = ").append("'").append(QuellSystem.DLM.toString()).append("'")
			.append(" AND ").append(CommonQueryLibrary.whereClauseFuerBereichKante());

		return entityManager
			.createQuery(hqlStringBuilder.toString(), KanteNetzklasseMapView.class)
			.setParameter("bereich", bereichAlsPolygon)
			.getResultStream().collect(Collectors.toSet());
	}

	@Override
	public Set<KanteNetzklasseMapView> getKantenMapViewInBereichDlmIstRadNETZZugeordnet(Envelope bereich) {
		require(bereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		StringBuilder hqlStringBuilder = new StringBuilder();
		hqlStringBuilder.append(
			"SELECT new de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteNetzklasseMapView(kante.id, kante.geometry, kante.verlaufLinks, kante.verlaufRechts, kante.isZweiseitig, kante.kantenAttributGruppe)")
			.append(" FROM Kante kante, KantenMapping kantenMapping")
			.append(" WHERE")
			.append(" kantenMapping.grundnetzKantenId = kante.id")
			.append(" AND kante.quelle = ").append("'").append(QuellSystem.DLM.toString()).append("'")
			.append(" AND kantenMapping.quellsystem = ").append("'").append(QuellSystem.RadNETZ.toString()).append("'")
			.append(" AND ").append(CommonQueryLibrary.whereClauseFuerBereichKante());

		return entityManager
			.createQuery(hqlStringBuilder.toString(), KanteNetzklasseMapView.class)
			.setParameter("bereich", bereichAlsPolygon)
			.getResultStream().collect(Collectors.toSet());
	}

	@Override
	public Set<KanteMapView> getKantenMapViewInBereichFuerQuelle(Envelope bereich, QuellSystem quelle) {
		require(bereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		StringBuilder hqlStringBuilder = new StringBuilder();
		hqlStringBuilder.append(
			"SELECT new de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteMapView(kante.id, kante.geometry, kante.verlaufLinks, kante.verlaufRechts, kante.isZweiseitig)"
				+ " FROM Kante kante")
			.append(" WHERE ").append(CommonQueryLibrary.whereClauseFuerBereichKante())
			.append(" AND ").append("kante.quelle = :quelle");

		return entityManager.createQuery(hqlStringBuilder.toString(), KanteMapView.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("quelle", quelle)
			.getResultStream().collect(Collectors.toSet());
	}

	@Override
	public Set<GeometrienVerlaufMapView> getGeometrienVerlaufMapViewInBereich(Envelope bereich,
		Set<NetzklasseFilter> netzklassenFilter) {
		require(bereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		Set<Netzklasse> netzklassen = CommonQueryLibrary.getNetzklassenParameter(netzklassenFilter);

		boolean orNichtKlassifiziert = netzklassenFilter.contains(NetzklasseFilter.NICHT_KLASSIFIZIERT);

		StringBuilder hqlStringBuilder = new StringBuilder();
		hqlStringBuilder.append(
			"SELECT DISTINCT new de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.GeometrienVerlaufMapView(kante.id, kante.verlaufLinks, kante.verlaufRechts)"
				+ " FROM Kante kante"
				+ " LEFT OUTER JOIN kante.kantenAttributGruppe as kag"
				+ " LEFT OUTER JOIN kag.netzklassen as nk")
			.append(" WHERE")
			.append(CommonQueryLibrary.whereClauseFuerNetzklassen(orNichtKlassifiziert))
			.append(" AND ").append(CommonQueryLibrary.whereClauseFuerBereichKante())
			.append(" AND ")
			.append(CommonQueryLibrary.whereClauseGrundnetz(featureToggleProperties.isShowDlm()))
			.append(" AND (kante.verlaufLinks != null OR kante.verlaufRechts != null)");

		return entityManager.createQuery(hqlStringBuilder.toString(), GeometrienVerlaufMapView.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("netzklassen", netzklassen)
			.getResultStream().collect(Collectors.toSet());
	}

	@Override
	public Set<KanteGeschwindigkeitAttributeView> getKanteGeschwindigkeitAttributeViewInBereichNachNetzklasse(
		Envelope bereich,
		Set<NetzklasseFilter> netzklassenFilter, boolean showDLM) {

		require(bereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		Set<Netzklasse> netzklassen = CommonQueryLibrary.getNetzklassenParameter(netzklassenFilter);

		boolean orNichtKlassifiziert = netzklassenFilter.contains(NetzklasseFilter.NICHT_KLASSIFIZIERT);

		StringBuilder hqlStringBuilder = new StringBuilder();
		hqlStringBuilder.append(
			"SELECT DISTINCT kante "
				+ " FROM KanteGeschwindigkeitAttributeView kante LEFT OUTER JOIN kante.netzklassen as nk"
				+ " JOIN FETCH kante.geschwindigkeitAttributGruppe")
			.append(" WHERE")
			.append(CommonQueryLibrary.whereClauseFuerNetzklassenInView(orNichtKlassifiziert)).append(" AND ")
			.append(CommonQueryLibrary.whereClauseFuerBereichKante())
			.append(" AND ")
			.append(CommonQueryLibrary.whereClauseGrundnetzInView(featureToggleProperties.isShowDlm() || showDLM));

		return entityManager.createQuery(hqlStringBuilder.toString(), KanteGeschwindigkeitAttributeView.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("netzklassen", netzklassen)
			.getResultStream().collect(Collectors.toSet());
	}

	@Override
	public Set<KanteFuehrungsformAttributeView> getKanteFuehrungsformAttributeViewInBereichNachNetzklasse(
		Envelope bereich,
		Set<NetzklasseFilter> netzklassenFilter, boolean showDLM) {

		require(bereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		Set<Netzklasse> netzklassen = CommonQueryLibrary.getNetzklassenParameter(netzklassenFilter);

		boolean orNichtKlassifiziert = netzklassenFilter.contains(NetzklasseFilter.NICHT_KLASSIFIZIERT);

		String hqlStringBuilder = "SELECT DISTINCT kante "
			+ " FROM KanteFuehrungsformAttributeView kante LEFT OUTER JOIN kante.netzklassen as nk"
			+ " JOIN FETCH kante.fuehrungsformAttributGruppe"
			+ " WHERE "
			+ CommonQueryLibrary.whereClauseFuerNetzklassenInView(orNichtKlassifiziert) + " AND "
			+ CommonQueryLibrary.whereClauseFuerBereichKante()
			+ " AND " + CommonQueryLibrary.whereClauseGrundnetzInView(
				featureToggleProperties.isShowDlm() || showDLM);
		return entityManager.createQuery(hqlStringBuilder, KanteFuehrungsformAttributeView.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("netzklassen", netzklassen)
			.getResultStream().collect(Collectors.toSet());
	}

	@Override
	public Set<KanteZustaendigkeitAttributeView> getKanteZustaendigkeitAttributeViewInBereichNachNetzklasse(
		Envelope bereich,
		Set<NetzklasseFilter> netzklassenFilter, boolean showDLM) {

		require(bereich, notNullValue());
		Polygon bereichAlsPolygon = EnvelopeAdapter
			.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());

		Set<Netzklasse> netzklassen = CommonQueryLibrary.getNetzklassenParameter(netzklassenFilter);

		boolean orNichtKlassifiziert = netzklassenFilter.contains(NetzklasseFilter.NICHT_KLASSIFIZIERT);

		String hqlStringBuilder = "SELECT DISTINCT kante "
			+ " FROM KanteZustaendigkeitAttributeView kante LEFT OUTER JOIN kante.netzklassen as nk"
			+ " JOIN FETCH kante.zustaendigkeitAttributGruppe"
			+ " WHERE "
			+ CommonQueryLibrary.whereClauseFuerNetzklassenInView(orNichtKlassifiziert) + " AND "
			+ CommonQueryLibrary.whereClauseFuerBereichKante()
			+ " AND " + CommonQueryLibrary.whereClauseGrundnetzInView(
				featureToggleProperties.isShowDlm() || showDLM);
		return entityManager.createQuery(hqlStringBuilder, KanteZustaendigkeitAttributeView.class)
			.setParameter("bereich", bereichAlsPolygon)
			.setParameter("netzklassen", netzklassen)
			.getResultStream().collect(Collectors.toSet());
	}
}
