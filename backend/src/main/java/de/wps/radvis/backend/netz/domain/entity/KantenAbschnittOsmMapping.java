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

import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Subselect;
import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
@Immutable
@Subselect("SELECT * FROM " + KantenRepository.GEOSERVER_RADVISNETZ_ABSCHNITTE_MAT_VIEW_NAME)
public class KantenAbschnittOsmMapping {
	@Id
	private String id;

	private Long kanteId;

	private Geometry geometry;

	private String status;

	private String radverkehrsfuehrung;
	private String breite;
	private String belagArt;
	private String oberflaechenbeschaffenheit;
	private String netzklassen;

}
