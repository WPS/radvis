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

package de.wps.radvis.backend.weitereKartenebenen.schnittstelle;

import java.time.LocalDateTime;

import de.wps.radvis.backend.benutzer.schnittstelle.BenutzerView;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.DateiLayerFormat;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.GeoserverLayerName;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Name;
import de.wps.radvis.backend.weitereKartenebenen.domain.valueobject.Quellangabe;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DateiLayerView {
	private final Long id;
	private final Name name;
	private final GeoserverLayerName geoserverLayerName;
	private final DateiLayerFormat format;
	private final Quellangabe quellangabe;
	private final BenutzerView benutzer;
	private final LocalDateTime erstelltAm;
	private final String sldFilename;
}
