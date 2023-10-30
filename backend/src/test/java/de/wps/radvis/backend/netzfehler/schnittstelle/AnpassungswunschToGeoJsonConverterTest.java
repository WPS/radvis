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

package de.wps.radvis.backend.netzfehler.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.test.util.ReflectionTestUtils;

import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import de.wps.radvis.backend.netzfehler.domain.entity.Anpassungswunsch;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschKategorie;
import de.wps.radvis.backend.netzfehler.domain.valueObject.AnpassungswunschStatus;

class AnpassungswunschToGeoJsonConverterTest {

	static GeometryFactory GEOM_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	static AnpassungswunschToGeoJsonConverter anpassungswunschToGeoJsonConverter = new AnpassungswunschToGeoJsonConverter();

	@Test
	void testConvertAnpassungswuensche() {
		// arrange
		Anpassungswunsch wunsch1 = Anpassungswunsch.builder()
			.geometrie(GEOM_FACTORY.createPoint(new Coordinate(1, 1)))
			.beschreibung("beschreibung 1")
			.status(AnpassungswunschStatus.OFFEN).kategorie(AnpassungswunschKategorie.DLM)
			.benutzerLetzteAenderung(BenutzerTestDataProvider.defaultBenutzer().build())
			.kommentarListe(new KommentarListe())
			.build();
		Anpassungswunsch wunsch2 = Anpassungswunsch.builder()
			.geometrie(GEOM_FACTORY.createPoint(new Coordinate(2, 2)))
			.beschreibung("beschreibung 2").kategorie(AnpassungswunschKategorie.DLM)
			.kommentarListe(new KommentarListe())
			.benutzerLetzteAenderung(BenutzerTestDataProvider.defaultBenutzer().build())
			.status(AnpassungswunschStatus.OFFEN)
			.build();

		ReflectionTestUtils.setField(wunsch1, "id", 1L);
		ReflectionTestUtils.setField(wunsch2, "id", 2L);

		List<Anpassungswunsch> wuensche = List.of(
			wunsch1,
			wunsch2);

		// act
		FeatureCollection featureCollection = anpassungswunschToGeoJsonConverter.convertAnpassungswuensche(wuensche);

		// assert
		Feature expectedFeature1 = GeoJsonConverter.createFeature(GEOM_FACTORY.createPoint(new Coordinate(1, 1)));
		expectedFeature1.setId("1");
		expectedFeature1.setProperty("beschreibung", "beschreibung 1");
		expectedFeature1.setProperty("status", AnpassungswunschStatus.OFFEN);

		Feature expectedFeature2 = GeoJsonConverter.createFeature(GEOM_FACTORY.createPoint(new Coordinate(2, 2)));
		expectedFeature2.setId("2");
		expectedFeature2.setProperty("beschreibung", "beschreibung 2");
		expectedFeature2.setProperty("status", AnpassungswunschStatus.OFFEN);

		assertThat(featureCollection).hasSize(2);
		assertThat(featureCollection).containsExactlyInAnyOrder(expectedFeature1, expectedFeature2);

	}

}