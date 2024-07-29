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

package de.wps.radvis.backend.matching.domain.entity;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import de.wps.radvis.backend.common.domain.entity.JobStatistik;

public class MatchingJobStatistik extends JobStatistik {
	// Kante
	public int anzahlLaengeMismatch;
	public int anzahlLaengeMismatchLessThan40m;
	public int anzahlLaengeMismatchLessThan50m;
	public int anzahlLaengeMismatchLessThan100m;
	public int anzahlLaengeMismatchMoreThan100m;
	public int laengenmismatchKanteKuerzer10m;
	public int laengenmismatchKanteKuerzer50m;
	public int laengenmismatchKanteKuerzer100m;
	public int laengenmismatchKanteKuerzer200m;
	public int laengenmismatchKanteKuerzer300m;
	public int laengenmismatchKanteGroesser300m;
	public int anzahlOhneMatch;
	public int anzahlZuWeitEntfernteMatches;

	public int anzahlKorrekturHatGeholfen;
	public int anzahlUmdrehenHatGeholfen;
	public int gesamtzahlKanten;
	public int anzahlKantenOhneGraphhopperMatch;
	public int anzahlKantenMitZuSchlechtemGraphhopperMatch;
	public int anzahlKantenOhneMatchInsgesamt;

	// Strecke
	public int anzahlStrecken;
	public int anzahlOhneValideStreckenmatchesInsgesamt;
	public int anzahlKeineStreckenmatches;
	public int anzahlStreckenmatchesZuLang;
	public int anzahlStreckenmatchesZuWeitEntfernt;

	public void reportLaengeMismatch(long mismatchLaenge) {
		if (mismatchLaenge < 40) {
			anzahlLaengeMismatchLessThan40m++;
		} else if (mismatchLaenge < 50) {
			anzahlLaengeMismatchLessThan50m++;
		} else if (mismatchLaenge < 100) {
			anzahlLaengeMismatchLessThan100m++;
		} else {
			anzahlLaengeMismatchMoreThan100m++;
		}
	}

	public void reportLaengeMismatchKanteLaenge(double kanteLaenge) {
		if (kanteLaenge < 10) {
			laengenmismatchKanteKuerzer10m++;
		} else if (kanteLaenge < 50) {
			laengenmismatchKanteKuerzer50m++;
		} else if (kanteLaenge < 100) {
			laengenmismatchKanteKuerzer100m++;
		} else if (kanteLaenge < 200) {
			laengenmismatchKanteKuerzer200m++;
		} else if (kanteLaenge < 300) {
			laengenmismatchKanteKuerzer300m++;
		} else {
			laengenmismatchKanteGroesser300m++;
		}
	}

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}
