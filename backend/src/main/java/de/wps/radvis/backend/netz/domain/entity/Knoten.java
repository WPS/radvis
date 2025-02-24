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

package de.wps.radvis.backend.netz.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;
import static org.valid4j.Assertive.require;

import java.util.Set;

import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.valueObject.Bauwerksmangel;
import de.wps.radvis.backend.netz.domain.valueObject.BauwerksmangelArt;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.QuerungshilfeDetails;
import de.wps.radvis.backend.netz.domain.valueObject.Zustandsbeschreibung;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
@Entity
@Audited
public class Knoten extends VersionierteEntity {

	@Getter
	@Enumerated(EnumType.STRING)
	private QuellSystem quelle;

	@Getter
	private Point point;

	private Kommentar kommentar;

	private Zustandsbeschreibung zustandsbeschreibung;

	@Enumerated(EnumType.STRING)
	private KnotenForm knotenForm;

	@Enumerated(EnumType.STRING)
	private QuerungshilfeDetails querungshilfeDetails;

	@Enumerated(EnumType.STRING)
	private Bauwerksmangel bauwerksmangel;

	@Type(value = ListArrayType.class, parameters = {
		@Parameter(name = ListArrayType.SQL_ARRAY_TYPE, value = "text")
	})
	@Column(name = "bauwerksmangel_art", columnDefinition = "text[]")
	private Set<BauwerksmangelArt> bauwerksmangelArt;

	@ManyToOne
	@Audited(targetAuditMode = NOT_AUDITED)
	private Verwaltungseinheit gemeinde;

	/**
	 * Für Grundnetz-Knoten
	 * 
	 * @param point
	 */
	public Knoten(Point point) {
		this(point, QuellSystem.DLM);
	}

	public Knoten(Point point, QuellSystem quelle) {
		require(point, notNullValue());
		require(point.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(),
			"Der Point muss in UTM32 kodiert sein");
		require(quelle, notNullValue());

		this.point = point;
		this.quelle = quelle;
	}

	public Coordinate getKoordinate() {
		return this.point.getCoordinate();
	}

	@Override
	public String toString() {
		return "Knoten[id=" + id + ", point=" + point.getCoordinate() + "]";
	}

	@Builder
	private Knoten(Long id, QuellSystem quelle, Point point, KnotenAttribute knotenAttribute, Long version) {
		super(id, version);
		this.quelle = quelle;
		this.point = point;
		this.setKnotenAttribute(knotenAttribute);
	}

	public KnotenAttribute getKnotenAttribute() {
		return new KnotenAttribute(this.kommentar, this.zustandsbeschreibung, this.knotenForm, this.gemeinde,
			this.querungshilfeDetails, this.bauwerksmangel, this.bauwerksmangelArt);
	}

	public void setKnotenAttribute(KnotenAttribute knotenAttribute) {
		if (knotenAttribute == null) {
			this.knotenForm = null;
			this.kommentar = null;
			this.zustandsbeschreibung = null;
			this.gemeinde = null;
			this.querungshilfeDetails = null;
			return;
		}
		this.knotenForm = knotenAttribute.getKnotenForm().orElse(null);
		this.querungshilfeDetails = knotenAttribute.getQuerungshilfeDetails().orElse(null);

		require(isQuerungshilfeDetailsValid(querungshilfeDetails, knotenForm));

		this.bauwerksmangel = knotenAttribute.getBauwerksmangel().orElse(null);
		this.bauwerksmangelArt = knotenAttribute.getBauwerksmangelArt().orElse(null);

		require(isBauwerksmangelValid(bauwerksmangel, bauwerksmangelArt, knotenForm));

		this.kommentar = knotenAttribute.getKommentar().orElse(null);
		this.zustandsbeschreibung = knotenAttribute.getZustandsbeschreibung().orElse(null);
		this.gemeinde = knotenAttribute.getGemeinde().orElse(null);
	}

	public static boolean isBauwerksmangelValid(Bauwerksmangel bauwerksmangel,
		Set<BauwerksmangelArt> bauwerksmangelArt, KnotenForm knotenForm) {
		if (knotenForm == null) {
			return bauwerksmangel == null && bauwerksmangelArt == null;
		}

		if (Bauwerksmangel.isRequiredForKnotenform(knotenForm)) {
			if (bauwerksmangel == null) {
				return false;
			}
		} else {
			return bauwerksmangel == null && bauwerksmangelArt == null;
		}

		if (BauwerksmangelArt.isRequiredForBauwerksmangel(bauwerksmangel)) {
			if (bauwerksmangelArt == null || bauwerksmangelArt.isEmpty()) {
				return false;
			}

			return bauwerksmangelArt.stream().allMatch(ba -> ba.isValidForKnotenform(knotenForm));
		} else {
			return bauwerksmangelArt == null;
		}
	}

	public static boolean isQuerungshilfeDetailsValid(QuerungshilfeDetails querungshilfe,
		KnotenForm knotenform) {
		if (knotenform == null) {
			return querungshilfe == null;
		}

		if (QuerungshilfeDetails.isRequiredForKnotenform(knotenform)) {
			return querungshilfe != null && querungshilfe.isValidForKnotenform(knotenform);
		} else {
			return querungshilfe == null;
		}
	}

	public void updatePoint(Point point) {
		require(point.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(),
			"Der Point muss in UTM32 kodiert sein");

		this.point = point;
	}
}
