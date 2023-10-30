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

import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.fahrradroute.domain.AbbildungAufKantenStatistik;

public abstract class AbstractTfisImportStatistik extends JobStatistik {
	public int anzahlFeaturesInShapefile = 0;
	public int anzahlDavonHauptstreckenFeaturesInShapefile = 0;
	public int anzahlDavonZuImportierendeFeatures = 0;
	public int anzahlGefundeneRouten = 0;
	public int anzahlRoutenInBw = 0;
	public int anzahlProfilinformationenNichtErmittelbar = 0;
	public int anzahlNetzbezugLineStringErfolgreich = 0;

	public AbbildungAufKantenStatistik abbildungAufKantenStatistik = new AbbildungAufKantenStatistik();
}
