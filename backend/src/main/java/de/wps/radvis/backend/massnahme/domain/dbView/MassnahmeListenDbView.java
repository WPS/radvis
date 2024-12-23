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

package de.wps.radvis.backend.massnahme.domain.dbView;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;

import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Kostenannahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.LGVFGID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MaViSID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Prioritaet;
import de.wps.radvis.backend.massnahme.domain.valueObject.Realisierungshilfe;
import de.wps.radvis.backend.massnahme.domain.valueObject.UmsetzungsstandStatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.VerbaID;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "massnahme_list_view")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MassnahmeListenDbView {
	@Id
	Long id;
	Bezeichnung bezeichnung;
	MassnahmeKonzeptID massnahmeKonzeptId;
	// Massnahmenkategorien, ";"-separiert
	String massnahmenkategorien;
	@Embedded
	Durchfuehrungszeitraum durchfuehrungszeitraum;

	Long baulastId;
	String baulastName;
	@Enumerated(EnumType.STRING)
	OrganisationsArt baulastOrganisationsArt;
	Long baulastUebergeordneteOrganisationId;

	Long zustaendigId;
	String zustaendigName;
	@Enumerated(EnumType.STRING)
	OrganisationsArt zustaendigOrganisationsArt;
	Long zustaendigUebergeordneteOrganisationId;

	Long unterhaltId;
	String unterhaltName;
	@Enumerated(EnumType.STRING)
	OrganisationsArt unterhaltOrganisationsArt;
	Long unterhaltUebergeordneteOrganisationId;

	Prioritaet prioritaet;
	// Netzklassen, ";"-separiert
	String netzklassen;
	@Enumerated(EnumType.STRING)
	Umsetzungsstatus umsetzungsstatus;
	Boolean veroeffentlicht;
	Boolean planungErforderlich;
	LocalDateTime letzteAenderung;
	Long benutzerLetzteAenderungId;
	Name benutzerLetzteAenderungVorname;
	Name benutzerLetzteAenderungNachname;
	@Enumerated(EnumType.STRING)
	BenutzerStatus benutzerLetzteAenderungStatus;
	String benutzerLetzteAenderungOrganisationName;
	Mailadresse benutzerLetzteAenderungEmail;
	@Enumerated(EnumType.STRING)
	SollStandard sollStandard;
	@Enumerated(EnumType.STRING)
	Handlungsverantwortlicher handlungsverantwortlicher;
	GeometryCollection geometry;
	@Enumerated(EnumType.STRING)
	UmsetzungsstandStatus umsetzungsstandStatus;
	@Enumerated(EnumType.STRING)
	Realisierungshilfe realisierungshilfe;
	Kostenannahme kostenannahme;
	MaViSID maViSID;
	VerbaID verbaID;
	LGVFGID lgvfgID;
	@Enumerated(EnumType.STRING)
	Konzeptionsquelle konzeptionsquelle;
	boolean archiviert;
	private MultiPoint netzbezugSnapshotPoints;
	private MultiLineString netzbezugSnapshotLines;

	public Set<Netzklasse> getNetzklassen() {
		return netzklassen == null ? Collections.emptySet()
			: Stream.of(netzklassen.split(";"))
				.map(Netzklasse::valueOf)
				.collect(Collectors.toSet());
	}

	public String getNetzklassenString() {
		return this.netzklassen;
	}

	public Set<Massnahmenkategorie> getMassnahmenkategorien() {
		return Stream.of(massnahmenkategorien.split(";"))
			.map(Massnahmenkategorie::valueOf)
			.collect(Collectors.toSet());
	}

	public GeometryCollection getGeometry() {
		if (!archiviert) {
			return geometry;
		}

		List<Geometry> netzbezugSnapshots = new ArrayList<>();
		if (netzbezugSnapshotLines != null) {
			netzbezugSnapshots.add(netzbezugSnapshotLines);
		}
		if (netzbezugSnapshotPoints != null) {
			netzbezugSnapshots.add(netzbezugSnapshotPoints);
		}

		return new GeometryCollection(netzbezugSnapshots.toArray(new Geometry[] {}),
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory());
	}

	public String getMasshnahmenkategorienString() {
		return this.massnahmenkategorien;
	}

	/**
	 * Vom FE erwartet: LineString | MultiLineString | Point
	 *
	 * @return
	 */
	public Geometry getDisplayGeometry() {
		if (getGeometry() == null) {
			return null;
		}

		if (getGeometry().getNumGeometries() == 0) {
			return null;
		}

		if (getGeometry().getGeometryN(0).getGeometryType() == Geometry.TYPENAME_MULTIPOINT) {
			return getGeometry().getGeometryN(0).getGeometryN(0);
		}

		return getGeometry().getGeometryN(0);
	}
}
