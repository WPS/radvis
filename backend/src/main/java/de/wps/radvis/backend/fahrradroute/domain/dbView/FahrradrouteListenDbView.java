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

package de.wps.radvis.backend.fahrradroute.domain.dbView;

import java.time.LocalDateTime;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Kategorie;
import lombok.Getter;

@Entity
@Getter
@Table(name = "fahrradroute_list_view")
public class FahrradrouteListenDbView {
	@Id
	Long id;

	@Embedded
	FahrradrouteName name;

	@Enumerated(EnumType.STRING)
	Kategorie kategorie;

	@Enumerated(EnumType.STRING)
	FahrradrouteTyp fahrradrouteTyp;

	String verantwortlicheOrganisationName;

	Point iconLocation;
	Geometry geometry;

	Double anstieg;
	Double abstieg;

	String kurzbeschreibung;
	String homepage;

	String lizenz;
	String lizenzNamensnennung;

	LocalDateTime zuletztBearbeitet;
}
