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

package de.wps.radvis.backend.konsistenz.pruefung.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Audited
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = true)
public class KonsistenzregelVerletzung extends AbstractEntity implements FehlerprotokollEintrag {

	@Embedded
	private KonsistenzregelVerletzungsDetails konsistenzregelVerletzungsDetails;

	@Getter
	@NotAudited
	private LocalDateTime datum;

	@Getter
	private String titel;

	@Getter
	private String typ;

	public KonsistenzregelVerletzung(KonsistenzregelVerletzungsDetails details, LocalDateTime datum,
		String titel, String typ) {
		this(null, details, datum, titel, typ);
	}

	@Builder(builderMethodName = "testbuilder")
	private KonsistenzregelVerletzung(Long id, KonsistenzregelVerletzungsDetails details, LocalDateTime datum,
		String titel, String typ) {
		super(id);
		require(details, notNullValue());
		require(datum, notNullValue());
		require(titel, notNullValue());
		require(typ, notNullValue());
		this.konsistenzregelVerletzungsDetails = details;
		this.datum = datum;
		this.titel = titel;
		this.typ = typ;
	}

	public void update(KonsistenzregelVerletzungsDetails details, LocalDateTime datum) {
		require(details, notNullValue());
		require(datum, notNullValue());
		this.konsistenzregelVerletzungsDetails = details;
		this.datum = datum;
	}

	@Override
	public MultiPoint getIconPosition() {
		return new MultiPoint(new Point[] { konsistenzregelVerletzungsDetails.getPosition() },
			konsistenzregelVerletzungsDetails.getPosition().getFactory());
	}

	@Override
	public Geometry getOriginalGeometry() {
		return konsistenzregelVerletzungsDetails.getOriginalGeometry().orElse(null);
	}

	@Override
	public String getBeschreibung() {
		return konsistenzregelVerletzungsDetails.getBeschreibung();
	}

	@Override
	public String getEntityLink() {
		return "";
	}

	public String getIdentity() {
		return konsistenzregelVerletzungsDetails.getIdentity();
	}

	public boolean hasEqualDetails(KonsistenzregelVerletzungsDetails otherDetails) {
		return otherDetails.equals(this.konsistenzregelVerletzungsDetails);
	}
}
