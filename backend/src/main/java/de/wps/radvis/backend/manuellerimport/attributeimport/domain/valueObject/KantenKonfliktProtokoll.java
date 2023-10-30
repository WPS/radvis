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

package de.wps.radvis.backend.manuellerimport.attributeimport.domain.valueObject;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.HashSet;
import java.util.Set;

import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.Konflikt;
import lombok.Getter;

@Getter
public class KantenKonfliktProtokoll {
	private final LineString kantenGeometrie;
	private final Long kanteId;
	private final Set<Konflikt> konflikte;

	public KantenKonfliktProtokoll(Long kanteId, LineString kantenGeometrie) {
		require(kanteId, notNullValue());
		require(kantenGeometrie, notNullValue());

		this.kanteId = kanteId;
		this.kantenGeometrie = kantenGeometrie;
		this.konflikte = new HashSet<>();
	}

	public void add(Konflikt konflikt) {
		require(konflikt, notNullValue());
		this.konflikte.add(konflikt);
	}
}
