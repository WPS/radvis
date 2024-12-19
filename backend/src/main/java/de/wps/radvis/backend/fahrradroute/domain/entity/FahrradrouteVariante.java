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

package de.wps.radvis.backend.fahrradroute.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Hoehenunterschied;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.LinearReferenzierteProfilEigenschaften;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.TfisId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.VarianteKategorie;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenBezug;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import jakarta.annotation.Nullable;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OrderColumn;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Audited
@Getter
@Entity
public class FahrradrouteVariante extends AbstractEntity {

	@Enumerated(EnumType.STRING)
	private VarianteKategorie kategorie;

	@ElementCollection(fetch = FetchType.LAZY)
	@CollectionTable(name = "fahrradroute_variante_kantenabschnitte")
	@OrderColumn(name = "fahrradroute_variante_kantenabschnitte_order")
	private List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug;

	@Nullable
	private Geometry stuetzpunkte;

	@Nullable
	private Geometry geometrie;

	private Hoehenunterschied anstieg;

	private Hoehenunterschied abstieg;

	@NotAudited
	@ElementCollection(fetch = FetchType.LAZY)
	@OrderColumn(name = "fahrradroute_variante_profil_eigenschaften_order")
	@CollectionTable(name = "fahrradroute_variante_profil_eigenschaften")
	private List<LinearReferenzierteProfilEigenschaften> linearReferenzierteProfilEigenschaften;

	private TfisId tfisId;

	private Long customProfileId;

	@Builder()
	private FahrradrouteVariante(
		Long id,
		VarianteKategorie kategorie,
		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug,
		Geometry stuetzpunkte,
		Geometry geometrie,
		List<LinearReferenzierteProfilEigenschaften> linearReferenzierteProfilEigenschaften,
		Long customProfileId) {
		super(id);
		require(kategorie, notNullValue());
		require(abschnittsweiserKantenBezug, notNullValue());
		require((Objects.isNull(stuetzpunkte) && Objects.isNull(geometrie)) || (Objects.nonNull(stuetzpunkte)
			&& Objects.nonNull(geometrie)), "geometrie und stuetzpunkte müssen beide null oder beide non-null sein");
		require(linearReferenzierteProfilEigenschaften, notNullValue());
		this.kategorie = kategorie;
		this.abschnittsweiserKantenBezug = abschnittsweiserKantenBezug;
		this.stuetzpunkte = stuetzpunkte;
		this.geometrie = geometrie;
		this.linearReferenzierteProfilEigenschaften = linearReferenzierteProfilEigenschaften;
		this.customProfileId = customProfileId;
	}

	@Builder(builderClassName = "FahrradrouteVarianteTfisBuilder", builderMethodName = "tfisVarianteBuilder", toBuilder = true)
	private FahrradrouteVariante(
		Long id,
		VarianteKategorie kategorie,
		List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug,
		Geometry geometrie,
		List<LinearReferenzierteProfilEigenschaften> linearReferenzierteProfilEigenschaften,
		TfisId tfisId) {
		super(id);
		require(kategorie, notNullValue());
		require(abschnittsweiserKantenBezug, notNullValue());
		require(tfisId, notNullValue());
		require(linearReferenzierteProfilEigenschaften, notNullValue());
		this.kategorie = kategorie;
		this.abschnittsweiserKantenBezug = abschnittsweiserKantenBezug;
		this.geometrie = geometrie;
		this.linearReferenzierteProfilEigenschaften = linearReferenzierteProfilEigenschaften;
		this.tfisId = tfisId;
		this.stuetzpunkte = geometrie != null && geometrie.getGeometryType().equals("LineString")
			? FahrradrouteVariante.createDefaultStuetzpunkte(abschnittsweiserKantenBezug,
				(LineString) geometrie)
			: null;
	}

	public List<AbschnittsweiserKantenBezug> getAbschnittsweiserKantenBezug() {
		return Collections.unmodifiableList(abschnittsweiserKantenBezug);
	}

	public boolean containsKante(Long kanteId) {
		require(kanteId, notNullValue());
		return abschnittsweiserKantenBezug.stream().anyMatch(kantenBezug -> kantenBezug.getKante().getId().equals(
			kanteId));
	}

	public void removeKantenFromNetzbezug(Collection<Long> kantenIds) {
		require(kantenIds, notNullValue());
		abschnittsweiserKantenBezug.removeIf(kantenBezug -> kantenIds.contains(kantenBezug.getKante().getId()));
	}

	public void updateAbgeleiteteRoutenInformationen(Hoehenunterschied anstieg, Hoehenunterschied abstieg) {
		this.anstieg = anstieg;
		this.abstieg = abstieg;
	}

	static Geometry createDefaultStuetzpunkte(List<AbschnittsweiserKantenBezug> abschnittsweiserKantenBezug,
		LineString netzbezugLineString) {
		require(abschnittsweiserKantenBezug, notNullValue());
		require(!abschnittsweiserKantenBezug.isEmpty());
		List<Coordinate> stuetzpunkteCoordinates = abschnittsweiserKantenBezug.stream().map(akb -> {
			return LineStrings.getMidPoint(akb.getKante().getGeometry());
		}).collect(Collectors.toList());

		// Vorne und hinten von der Liste noch den Start- und Endpunkt hinzufügen

		// Der Linestring ist ein Routing-Ergebnis und enthaelt deshalb 3D-Koordinaten.
		// Als stuetzpunkte muessen jedoch nur 2D-Koordinaten gesetzt werden, da wir sie ans Backend
		// zurueckschicken und dieses nicht mit 3D umgehen kann.
		Coordinate start3D = netzbezugLineString.getStartPoint().getCoordinate();
		Coordinate end3D = netzbezugLineString.getEndPoint().getCoordinate();
		stuetzpunkteCoordinates.add(0, new Coordinate(start3D.getX(), start3D.getY()));
		stuetzpunkteCoordinates.add(new Coordinate(end3D.getX(), end3D.getY()));

		return KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(stuetzpunkteCoordinates.toArray(new Coordinate[stuetzpunkteCoordinates.size()]));
	}

	public Optional<Hoehenunterschied> getAnstieg() {
		return Optional.ofNullable(this.anstieg);
	}

	public Optional<Hoehenunterschied> getAbstieg() {
		return Optional.ofNullable(this.abstieg);
	}

	public Optional<Geometry> getGeometrie() {
		return Optional.ofNullable(this.geometrie);
	}

	public Optional<Geometry> getStuetzpunkte() {
		return Optional.ofNullable(this.stuetzpunkte);
	}

	public Optional<Long> getCustomProfileId() {
		return Optional.ofNullable(this.customProfileId);
	}
}
