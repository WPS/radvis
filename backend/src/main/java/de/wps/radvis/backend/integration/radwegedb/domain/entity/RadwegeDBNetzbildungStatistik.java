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

package de.wps.radvis.backend.integration.radwegedb.domain.entity;

import de.wps.radvis.backend.common.domain.entity.JobStatistik;

public class RadwegeDBNetzbildungStatistik extends JobStatistik {
	public int anzahlKantenErstellt;
	public int anzahlSollenNichtUbernommenWerden;
	public int anzahlKnotenpunkte;
	public int anzahlAbgebildeterKnotenpunkte;
	public int multilinestringHatMehrereLinestrings;
	public int geometrieTypNichtUnterstuetzt;

	public RadwegeDBNetzbildungStatistik() {
		anzahlSollenNichtUbernommenWerden = 0;
		anzahlKantenErstellt = 0;
		anzahlAbgebildeterKnotenpunkte = 0;
		anzahlKnotenpunkte = 0;
		multilinestringHatMehrereLinestrings = 0;
		geometrieTypNichtUnterstuetzt = 0;
	}
}
