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

package de.wps.radvis.backend.wegweisendeBeschilderung.domain;

import org.locationtech.jts.geom.Coordinate;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.entity.WegweisendeBeschilderung;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Defizit;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Gemeinde;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Kreis;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Land;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.PfostenNr;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.PfostenTyp;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Pfostendefizit;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Pfostenzustand;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.WegweiserTyp;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.valueObject.Zustandsbewertung;

public class WegweisendeBeschilderungTestDataProvider {

	public static WegweisendeBeschilderung.WegweisendeBeschilderungBuilder withDefaultValuesGeometrieAndVerwaltungseinheit(
		Coordinate coordinate, Verwaltungseinheit verwaltungseinheit) {
		return WegweisendeBeschilderung.builder()
			.geometrie(GeometryTestdataProvider.createPoint(coordinate))
			.pfostenNr(PfostenNr.of("PfostenNr"))
			.wegweiserTyp(WegweiserTyp.of("Beschilderung WegweiserTyp"))
			.pfostenTyp(PfostenTyp.of("Beschilderung PfostenTyp"))
			.zustandsbewertung(Zustandsbewertung.of("Beschilderung Zustandsbewertung"))
			.defizit(Defizit.of("Beschilderung Defizit"))
			.pfostenzustand(Pfostenzustand.of("Beschilderung Pfostenzustand"))
			.pfostendefizit(Pfostendefizit.of("Beschilderung Pfostendefizit"))
			.gemeinde(Gemeinde.of("Beschilderung Gemeinde"))
			.kreis(Kreis.of("Beschilderung Kreis"))
			.land(Land.of("Beschilderung Land"))
			.zustaendigeVerwaltungseinheit(verwaltungseinheit);
	}
}
