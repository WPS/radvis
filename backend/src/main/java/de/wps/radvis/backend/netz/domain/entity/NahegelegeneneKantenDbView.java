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
import static org.valid4j.Assertive.require;

import org.locationtech.jts.geom.LineString;

import lombok.Getter;

@Getter
public class NahegelegeneneKantenDbView {
	private final Kante basisKante;
	private final LineString basisKanteSegment;

	private final Kante nahegelegeneKante;
	private final LineString nahegelegeneKanteSegment;

	public NahegelegeneneKantenDbView(Kante basisKante, LineString basisKanteSegment,
		Kante nahegelegeneKante, LineString nahegelegeneKanteSegment) {
		require(basisKante, notNullValue());
		require(nahegelegeneKante, notNullValue());
		require(!basisKante.equals(nahegelegeneKante));
		require(basisKanteSegment, notNullValue());
		require(nahegelegeneKanteSegment, notNullValue());

		this.basisKante = basisKante;
		this.basisKanteSegment = basisKanteSegment;
		this.nahegelegeneKante = nahegelegeneKante;
		this.nahegelegeneKanteSegment = nahegelegeneKanteSegment;
	}
}
