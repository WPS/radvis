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

package de.wps.radvis.backend.quellimport.common.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.ensure;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.valueObject.Attribute;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@ToString
@Entity
public class ImportedFeature extends AbstractEntity {
	@Getter
	private String technischeId;

	@Getter
	private Geometry geometrie;

	private Attribute attribute;

	@Getter
	private LocalDateTime importDatum;

	@Getter
	@Enumerated(EnumType.STRING)
	private QuellSystem quelle;

	@Getter
	@Enumerated(EnumType.STRING)
	private Art art;

	@Getter
	@Setter
	private Double anteilProjiziert;

	public ImportedFeature(String technischeId, Geometry geometrie, Map<String, Object> attribute,
		LocalDateTime importDatum, QuellSystem quelle, Art art) {
		require(technischeId, notNullValue());
		require(geometrie, notNullValue());
		require(geometrie.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(),
			"Erwartete SRID '" + KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()
				+ "' entspricht nicht geometry.getSRID() '" + geometrie.getSRID() + "'");
		require(geometrie.getGeometryType().equals(Geometry.TYPENAME_LINESTRING)
			|| geometrie.getGeometryType().equals(Geometry.TYPENAME_POINT)
			|| geometrie.getGeometryType().equals(Geometry.TYPENAME_MULTILINESTRING), "Geometrietyp nicht zulässig");
		require(attribute, notNullValue());
		require(importDatum, notNullValue());
		require(quelle, notNullValue());
		require(art, notNullValue());

		this.art = art;
		this.quelle = quelle;
		this.importDatum = importDatum;
		this.technischeId = technischeId;
		this.geometrie = geometrie;
		this.attribute = Attribute.of(attribute);

		ensure(getTechnischeId(), notNullValue());
		ensure(getGeometrie(), notNullValue());
		ensure(getGeometrie().getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid());
		ensure(getAttribute(), notNullValue());
	}

	public Map<String, Object> getAttribute() {
		return Collections.unmodifiableMap(attribute.getAttribute());
	}

	public boolean hasAttribut(String key) {
		return attribute.hasAttribut(key);
	}

	public Object getAttribut(String key) {
		require(hasAttribut(key));
		return attribute.getAttributValue(key);
	}

	public void addAttribut(String key, Object val) {
		attribute.addAttribute(key, val);
	}
}
