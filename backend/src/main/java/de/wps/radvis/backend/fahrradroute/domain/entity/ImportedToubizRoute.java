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

package de.wps.radvis.backend.fahrradroute.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.List;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteName;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.ToubizId;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.Tourenkategorie;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ImportedToubizRoute {
	private ToubizId toubizId;
	private FahrradrouteName name;

	@NotNull
	private Geometry originalGeometrie;

	private String kurzbezeichnung;

	private String beschreibung;

	private String info;

	private Laenge offizielleLaenge;

	private Tourenkategorie tourenkategorie;

	private String homepage;

	private String emailAnsprechpartner;

	private String lizenz;

	private LocalDateTime zuletztBearbeitet;

	private List<String> linksZuWeiterenMedien;

	private String lizenzNamensnennung;

	@Getter
	private boolean landesradfernweg;

	@Builder
	public ImportedToubizRoute(ToubizId toubizId,
		FahrradrouteName name, String beschreibung, Geometry originalGeometrie, String kurzbezeichnung, String info,
		Laenge offizielleLaenge, Tourenkategorie tourenkategorie, String homepage,
		String emailAnsprechpartner, String lizenz,
		LocalDateTime zuletztBearbeitet, List<String> linksZuWeiterenMedien, String lizenzNamensnennung,
		boolean landesradfernweg) {
		require(originalGeometrie, notNullValue());
		this.toubizId = toubizId;
		this.name = name;
		this.beschreibung = beschreibung;
		this.originalGeometrie = originalGeometrie;
		this.kurzbezeichnung = kurzbezeichnung;
		this.info = info;
		this.offizielleLaenge = offizielleLaenge;
		this.tourenkategorie = tourenkategorie;
		this.homepage = homepage;
		this.emailAnsprechpartner = emailAnsprechpartner;
		this.lizenz = lizenz;
		this.zuletztBearbeitet = zuletztBearbeitet;
		this.linksZuWeiterenMedien = linksZuWeiterenMedien;
		this.lizenzNamensnennung = lizenzNamensnennung;
		this.landesradfernweg = landesradfernweg;
	}
}
