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

package de.wps.radvis.backend.servicestation.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;
import static org.valid4j.Assertive.require;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.servicestation.domain.valueObject.Betreiber;
import de.wps.radvis.backend.servicestation.domain.valueObject.Fahrradhalterung;
import de.wps.radvis.backend.servicestation.domain.valueObject.Gebuehren;
import de.wps.radvis.backend.servicestation.domain.valueObject.Kettenwerkzeug;
import de.wps.radvis.backend.servicestation.domain.valueObject.Luftpumpe;
import de.wps.radvis.backend.servicestation.domain.valueObject.Marke;
import de.wps.radvis.backend.servicestation.domain.valueObject.Oeffnungszeiten;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationBeschreibung;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationName;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationStatus;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationTyp;
import de.wps.radvis.backend.servicestation.domain.valueObject.Werkzeug;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Audited
public class Servicestation extends VersionierteEntity {
	public static class CsvHeader {
		public static final String RAD_VIS_ID = "RadVIS-ID";
		public static final String POSITION_X_UTM32_N = "Position X (UTM32_N)";
		public static final String POSITION_Y_UTM32_N = "Position Y (UTM32_N)";
		public static final String NAME = "Name";
		public static final String GEBUEHREN = "Gebühren";
		public static final String OEFFNUNGSZEITEN = "Öffnungszeiten";
		public static final String BETREIBER = "Betreiber";
		public static final String MARKE = "Marke";
		public static final String LUFTPUMPE = "Luftpumpe";
		public static final String KETTENWERKZEUG = "Kettenwerkzeug";
		public static final String WERKZEUG = "Werkzeug";
		public static final String FAHRRADHALTERUNG = "Fahrradhalterung";
		public static final String BESCHREIBUNG = "Beschreibung";
		public static final String ZUSTAENDIG_IN_RAD_VIS = "Zuständig in RadVIS";
		public static final String TYP = "Typ";
		public static final String STATUS = "Status";

		public static final List<String> ALL = List.of(RAD_VIS_ID,
			POSITION_X_UTM32_N,
			POSITION_Y_UTM32_N,
			NAME,
			GEBUEHREN,
			OEFFNUNGSZEITEN,
			BETREIBER,
			MARKE,
			LUFTPUMPE,
			KETTENWERKZEUG,
			WERKZEUG,
			FAHRRADHALTERUNG,
			BESCHREIBUNG,
			ZUSTAENDIG_IN_RAD_VIS,
			TYP,
			STATUS);
	}

	@Getter
	private Point geometrie;

	@Getter
	private ServicestationName name;

	@Getter
	private Gebuehren gebuehren;

	// kein Pflichtfeld
	private Oeffnungszeiten oeffnungszeiten;

	@Getter
	private Betreiber betreiber;

	// kein Pflichtfeld
	private Marke marke;

	@Getter
	private Luftpumpe luftpumpe;

	@Getter
	private Kettenwerkzeug kettenwerkzeug;

	@Getter
	private Werkzeug werkzeug;

	@Getter
	private Fahrradhalterung fahrradhalterung;

	// keine Pflicht
	private ServicestationBeschreibung beschreibung;

	@Getter
	@Enumerated(EnumType.STRING)
	private ServicestationTyp typ;

	@Getter
	@Enumerated(EnumType.STRING)
	private ServicestationStatus status;

	@Audited(targetAuditMode = NOT_AUDITED)
	@ManyToOne
	@Getter
	private Verwaltungseinheit organisation;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@Getter
	@NotAudited
	private DokumentListe dokumentListe;

	@Builder(toBuilder = true)
	private Servicestation(
		Long id,
		Long version,
		Point geometrie,
		ServicestationName name,
		Gebuehren gebuehren,
		Oeffnungszeiten oeffnungszeiten,
		Betreiber betreiber,
		Marke marke,
		Luftpumpe luftpumpe,
		Kettenwerkzeug kettenwerkzeug,
		Werkzeug werkzeug,
		Fahrradhalterung fahrradhalterung,
		ServicestationBeschreibung beschreibung,
		ServicestationTyp typ,
		ServicestationStatus status,
		Verwaltungseinheit organisation,
		DokumentListe dokumentListe) {
		super(id, version);
		require(geometrie, notNullValue());
		require(name, notNullValue());
		require(gebuehren, notNullValue());
		require(betreiber, notNullValue());
		require(luftpumpe, notNullValue());
		require(kettenwerkzeug, notNullValue());
		require(werkzeug, notNullValue());
		require(fahrradhalterung, notNullValue());
		require(organisation, notNullValue());
		require(typ, notNullValue());
		require(status, notNullValue());
		require(dokumentListe, notNullValue());
		this.geometrie = geometrie;
		this.name = name;
		this.gebuehren = gebuehren;
		this.oeffnungszeiten = oeffnungszeiten;
		this.betreiber = betreiber;
		this.marke = marke;
		this.luftpumpe = luftpumpe;
		this.kettenwerkzeug = kettenwerkzeug;
		this.werkzeug = werkzeug;
		this.fahrradhalterung = fahrradhalterung;
		this.beschreibung = beschreibung;
		this.typ = typ;
		this.status = status;
		this.organisation = organisation;
		this.dokumentListe = dokumentListe;
	}

	public Servicestation(
		Point geometrie,
		ServicestationName name,
		Gebuehren gebuehren,
		Oeffnungszeiten oeffnungszeiten,
		Betreiber betreiber,
		Marke marke,
		Luftpumpe luftpumpe,
		Kettenwerkzeug kettenwerkzeug,
		Werkzeug werkzeug,
		Fahrradhalterung fahrradhalterung,
		ServicestationBeschreibung beschreibung,
		Verwaltungseinheit organisation,
		ServicestationTyp typ,
		ServicestationStatus status,
		DokumentListe dokumentListe
	) {
		this(
			null,
			null,
			geometrie,
			name,
			gebuehren,
			oeffnungszeiten,
			betreiber,
			marke,
			luftpumpe,
			kettenwerkzeug,
			werkzeug,
			fahrradhalterung,
			beschreibung,
			typ,
			status,
			organisation,
			dokumentListe
		);
	}

	public void updateAttribute(
		Point geometrie,
		ServicestationName name,
		Gebuehren gebuehren,
		Oeffnungszeiten oeffnungszeiten,
		Betreiber betreiber,
		Marke marke,
		Luftpumpe luftpumpe,
		Kettenwerkzeug kettenwerkzeug,
		Werkzeug werkzeug,
		Fahrradhalterung fahrradhalterung,
		ServicestationBeschreibung beschreibung,
		Verwaltungseinheit organisation,
		ServicestationTyp typ,
		ServicestationStatus status
	) {
		require(geometrie, notNullValue());
		require(name, notNullValue());
		require(gebuehren, notNullValue());
		require(betreiber, notNullValue());
		require(luftpumpe, notNullValue());
		require(kettenwerkzeug, notNullValue());
		require(werkzeug, notNullValue());
		require(fahrradhalterung, notNullValue());
		require(organisation, notNullValue());
		require(typ, notNullValue());
		require(status, notNullValue());

		this.gebuehren = gebuehren;
		this.oeffnungszeiten = oeffnungszeiten;
		this.betreiber = betreiber;
		this.marke = marke;
		this.luftpumpe = luftpumpe;
		this.kettenwerkzeug = kettenwerkzeug;
		this.werkzeug = werkzeug;
		this.fahrradhalterung = fahrradhalterung;
		this.beschreibung = beschreibung;
		this.organisation = organisation;
		this.typ = typ;
		this.status = status;
		this.geometrie = geometrie;
		this.name = name;
	}

	public void addDokument(Dokument dokument) {
		this.dokumentListe.addDokument(dokument);
	}

	public void deleteDokument(Long dokumentId) {
		this.dokumentListe.deleteDokument(dokumentId);
	}

	public Optional<Oeffnungszeiten> getOeffnungszeiten() {
		return Optional.ofNullable(oeffnungszeiten);
	}

	public Optional<Marke> getMarke() {
		return Optional.ofNullable(marke);
	}

	public Optional<ServicestationBeschreibung> getBeschreibung() {
		return Optional.ofNullable(beschreibung);
	}
}
