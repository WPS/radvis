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

package de.wps.radvis.backend.leihstation.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import org.hibernate.envers.Audited;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.leihstation.domain.valueObject.Anzahl;
import de.wps.radvis.backend.leihstation.domain.valueObject.ExterneLeihstationenId;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationQuellSystem;
import de.wps.radvis.backend.leihstation.domain.valueObject.LeihstationStatus;
import de.wps.radvis.backend.leihstation.domain.valueObject.UrlAdresse;
import de.wps.radvis.backend.servicestation.domain.valueObject.Betreiber;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Audited
public class Leihstation extends VersionierteEntity {
	public static class CsvHeader {
		public static final String STATUS = "Status";
		public static final String BUCHUNGS_URL = "Buchungs-URL";
		public static final String FREIES_ABSTELLEN_MOEGLICH = "Freies Abstellen möglich";
		public static final String ANZAHL_ABSTELLMOEGLICHKEITEN = "Anzahl Abstellmöglichkeiten";
		public static final String ANZAHL_PEDELECS = "Anzahl Pedelecs";
		public static final String ANZAHL_FAHRRAEDER = "Anzahl Fahrräder";
		public static final String BETREIBER = "Betreiber";
		public static final String POSITION_Y_UTM32_N = "Position Y (UTM32_N)";
		public static final String POSITION_X_UTM32_N = "Position X (UTM32_N)";
		public static final String RAD_VIS_ID = "RadVIS-ID";
		public static final String QUELLSYSTEM = "Quellsystem";

		public static final List<String> ALL = List.of(
			RAD_VIS_ID,
			POSITION_X_UTM32_N,
			POSITION_Y_UTM32_N,
			BETREIBER,
			STATUS,
			ANZAHL_FAHRRAEDER,
			ANZAHL_PEDELECS,
			ANZAHL_ABSTELLMOEGLICHKEITEN,
			FREIES_ABSTELLEN_MOEGLICH,
			BUCHUNGS_URL,
			QUELLSYSTEM);
	}

	@Getter
	@Setter
	private Point geometrie;

	@Getter
	private String betreiber;

	@Setter
	private Anzahl anzahlFahrraeder;

	private Anzahl anzahlPedelecs;

	private Anzahl anzahlAbstellmoeglichkeiten;

	@Getter
	private boolean freiesAbstellen;

	private UrlAdresse buchungsUrl;

	@Getter
	@Enumerated(EnumType.STRING)
	private LeihstationStatus status;

	@Getter
	@Enumerated(EnumType.STRING)
	private LeihstationQuellSystem quellSystem;

	private ExterneLeihstationenId externeId;

	public Leihstation(
		Point geometrie,
		String betreiber,
		Anzahl anzahlFahrraeder,
		Anzahl anzahlPedelecs,
		Anzahl anzahlAbstellmoeglichkeiten,
		boolean freiesAbstellen,
		UrlAdresse buchungsUrl,
		LeihstationStatus status,
		LeihstationQuellSystem quellSystem,
		ExterneLeihstationenId externeId) {
		this(null, null, geometrie, betreiber, anzahlFahrraeder, anzahlPedelecs, anzahlAbstellmoeglichkeiten,
			freiesAbstellen, buchungsUrl, status, quellSystem, externeId);
	}

	@Builder(toBuilder = true)
	private Leihstation(Long id, Long version, Point geometrie, String betreiber, Anzahl anzahlFahrraeder,
		Anzahl anzahlPedelecs, Anzahl anzahlAbstellmoeglichkeiten, boolean freiesAbstellen, UrlAdresse buchungsUrl,
		LeihstationStatus status, LeihstationQuellSystem quellSystem, ExterneLeihstationenId externeId) {
		super(id, version);

		require(geometrie, notNullValue());
		require(betreiber, notNullValue());
		require(betreiber.length() <= Betreiber.MAX_LENGTH, "Betreiber darf max. 255 Zeichen haben");
		require(status, notNullValue());
		require(quellSystem == LeihstationQuellSystem.RADVIS || quellSystem == LeihstationQuellSystem.MOBIDATABW,
			"Quellsystem darf nur RadVis oder MobiData sein");
		require(quellSystem != LeihstationQuellSystem.MOBIDATABW || externeId != null,
			"MobiData-Leistationen müssen eine Externe ID haben");

		this.geometrie = geometrie;
		this.betreiber = betreiber;
		this.anzahlFahrraeder = anzahlFahrraeder;
		this.anzahlPedelecs = anzahlPedelecs;
		this.anzahlAbstellmoeglichkeiten = anzahlAbstellmoeglichkeiten;
		this.freiesAbstellen = freiesAbstellen;
		this.buchungsUrl = buchungsUrl;
		this.status = status;
		this.quellSystem = quellSystem;
		this.externeId = externeId;
	}

	public void update(
		Point geometrie,
		String betreiber,
		Anzahl anzahlFahrraeder,
		Anzahl anzahlPedelecs,
		Anzahl anzahlAbstellmoeglichkeiten,
		boolean freiesAbstellen,
		UrlAdresse buchungsUrl,
		LeihstationStatus status) {
		require(geometrie, notNullValue());
		require(betreiber, notNullValue());
		require(betreiber.length() <= 255, "Betreiber darf max. 255 Zeichen haben");
		require(status, notNullValue());

		this.geometrie = geometrie;
		this.betreiber = betreiber;
		this.anzahlFahrraeder = anzahlFahrraeder;
		this.anzahlPedelecs = anzahlPedelecs;
		this.anzahlAbstellmoeglichkeiten = anzahlAbstellmoeglichkeiten;
		this.freiesAbstellen = freiesAbstellen;
		this.buchungsUrl = buchungsUrl;
		this.status = status;
	}

	public Optional<Anzahl> getAnzahlFahrraeder() {
		return Optional.ofNullable(this.anzahlFahrraeder);
	}

	public Optional<Anzahl> getAnzahlPedelecs() {
		return Optional.ofNullable(this.anzahlPedelecs);
	}

	public Optional<ExterneLeihstationenId> getExterneId() {
		return Optional.ofNullable(externeId);
	}

	public Optional<Anzahl> getAnzahlAbstellmoeglichkeiten() {
		return Optional.ofNullable(this.anzahlAbstellmoeglichkeiten);
	}

	public Optional<UrlAdresse> getBuchungsUrl() {
		return Optional.ofNullable(this.buchungsUrl);
	}
}
