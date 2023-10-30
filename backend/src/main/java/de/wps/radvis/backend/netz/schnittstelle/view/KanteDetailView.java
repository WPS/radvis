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

package de.wps.radvis.backend.netz.schnittstelle.view;

import java.util.Map;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class KanteDetailView {
	private final Long id;
	private final Geometry geometrie;
	private final Map<String, String> attributeAufGanzerLaenge;
	private final Map<String, String> attributeAnPosition;
	private final Map<String, String> trennstreifenAttribute;
	private final String seite;
	private final Geometry verlaufLinks;
	private final Geometry verlaufRechts;
	private final Boolean trennstreifenEinseitig;
	private final Richtung trennstreifenRichtungRechts;
	private final Richtung trennstreifenRichtungLinks;

	public KanteDetailView(Long id, Geometry geometrie, Map<String, String> attributeAufGanzerLaenge,
		Map<String, String> attributeAnPosition, Map<String, String> trennstreifenAttribute, String seite,
		Geometry verlaufLinks, Geometry verlaufRechts, Boolean trennstreifenEinseitig,
		Richtung trennstreifenRichtungRechts, Richtung trennstreifenRichtungLinks) {
		this.id = id;
		this.geometrie = geometrie;
		this.attributeAufGanzerLaenge = attributeAufGanzerLaenge;
		this.attributeAnPosition = attributeAnPosition;
		this.trennstreifenAttribute = trennstreifenAttribute;
		this.seite = seite;
		this.verlaufLinks = verlaufLinks;
		this.verlaufRechts = verlaufRechts;
		this.trennstreifenEinseitig = trennstreifenEinseitig;
		this.trennstreifenRichtungRechts = trennstreifenRichtungRechts;
		this.trennstreifenRichtungLinks = trennstreifenRichtungLinks;
	}
}
