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

package de.wps.radvis.backend.weitereKartenebenen.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.Objects;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.DateiLayerFormat;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverDatastoreName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverLayerName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverStyleName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Name;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Quellangabe;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Getter
public class DateiLayer extends AbstractEntity {

	private Name name;

	private Quellangabe quellangabe;

	private GeoserverLayerName geoserverLayerName;

	private GeoserverStyleName geoserverStyleName;

	private String sldFilename;

	private GeoserverDatastoreName geoserverDatastoreName;

	@ManyToOne
	private Benutzer benutzer;

	private LocalDateTime erstelltAm;

	@Enumerated(EnumType.STRING)
	private DateiLayerFormat dateiLayerFormat;

	@Builder
	private DateiLayer(Long id, Name name, Quellangabe quellangabe, GeoserverLayerName geoserverLayerName,
		GeoserverDatastoreName geoserverDatastoreName, Benutzer benutzer, LocalDateTime erstelltAm,
		DateiLayerFormat dateiLayerFormat) {
		require(name, notNullValue());
		require(quellangabe, notNullValue());
		require(geoserverLayerName, notNullValue());
		require(geoserverDatastoreName, notNullValue());
		require(benutzer, notNullValue());
		require(erstelltAm, notNullValue());
		require(dateiLayerFormat, notNullValue());

		this.id = id;
		this.name = name;
		this.quellangabe = quellangabe;
		this.geoserverLayerName = geoserverLayerName;
		this.geoserverDatastoreName = geoserverDatastoreName;
		this.benutzer = benutzer;
		this.erstelltAm = erstelltAm;
		this.dateiLayerFormat = dateiLayerFormat;
	}

	public DateiLayer(Name name, Quellangabe quellangabe, GeoserverLayerName geoserverLayerName,
		GeoserverDatastoreName geoserverDatastoreName, Benutzer benutzer, LocalDateTime erstelltAm,
		DateiLayerFormat dateiLayerFormat) {
		this(null, name, quellangabe, geoserverLayerName, geoserverDatastoreName, benutzer, erstelltAm,
			dateiLayerFormat);
	}

	public void removeStyle() {
		this.geoserverStyleName = null;
		this.sldFilename = null;
	}

	public void setStyle(GeoserverStyleName geoserverStyleName, String sldFilename) {
		require(geoserverStyleName, notNullValue());
		require(sldFilename, notNullValue());
		this.geoserverStyleName = geoserverStyleName;
		this.sldFilename = sldFilename;
	}

	public boolean hasStyle() {
		return !Objects.isNull(this.getGeoserverStyleName());
	}
}
