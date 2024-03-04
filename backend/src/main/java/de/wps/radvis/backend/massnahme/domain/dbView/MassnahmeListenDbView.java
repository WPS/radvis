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
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;

import de.wps.radvis.backend.benutzer.domain.valueObject.BenutzerStatus;
import de.wps.radvis.backend.benutzer.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
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
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
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

	public String getMasshnahmenkategorienString() {
		return this.massnahmenkategorien;
	}

	public Geometry getDisplayGeometry() {
		if (geometry == null) {
			return null;
		}

		if (geometry.getNumGeometries() == 0) {
			return null;
		}

		if (geometry.getGeometryN(0).getGeometryType() == Geometry.TYPENAME_MULTIPOINT) {
			return geometry.getGeometryN(0).getGeometryN(0);
		}

		return geometry.getGeometryN(0);
	}
}
