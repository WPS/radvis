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

package de.wps.radvis.backend.abfrage.netzausschnitt.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.geojson.Feature;
import org.geojson.GeoJsonObject;
import org.geojson.LngLatAlt;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.abfrage.netzausschnitt.AbfrageNetzausschnittConfiguration;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.NetzausschnittService;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingService;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.netzfehler.domain.entity.Netzfehler;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerBeschreibung;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerTyp;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;

@Tag("group4")
@ContextConfiguration(classes = {
	NetzConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	AbfrageNetzausschnittConfiguration.class,
	NetzausschnittControllerTestIT.TestConfiguration.class,
	CommonConfiguration.class,
	NetzfehlerConfiguration.class,
	BenutzerConfiguration.class, KommentarConfiguration.class,
	KonsistenzregelPruefungsConfiguration.class, KonsistenzregelnConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	CommonConfigurationProperties.class,
	DLMConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
@ActiveProfiles(profiles = "test")
public class NetzausschnittControllerTestIT extends DBIntegrationTestIT {

	public static class TestConfiguration {
		@Autowired
		private NetzToGeoJsonConverter netzToGeoJsonConverter;
		@Autowired
		private NetzService netzService;
		@Autowired
		private NetzausschnittService netzausschnittService;
		@Autowired
		private VerwaltungseinheitResolver verwaltungseinheitResolver;

		@MockBean
		private KantenMappingService kantenMappingService;
		@MockBean
		private NetzausschnittGuard netzausschnittGuard;

		@Bean
		public NetzausschnittController netzController() {
			return new NetzausschnittController(netzToGeoJsonConverter, netzService, netzausschnittService,
				kantenMappingService, verwaltungseinheitResolver, netzausschnittGuard);
		}
	}

	@Mock
	private Authentication authentication;

	@Autowired
	private NetzausschnittController netzausschnittController;

	@Autowired
	private NetzfehlerRepository netzfehlerRepository;

	GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@Test
	void getNetzfehlerGeoJson_netzfehlerVorhanden_netzfehlerWerdenGeliefert() {
		// arrange
		LineString geometrie1 = geometryFactory
			.createLineString(new Coordinate[] { new Coordinate(1.0, 2.0), new Coordinate(2.0, 4.0) });
		Netzfehler netzfehler1 = Netzfehler.builder().netzfehlerTyp(NetzfehlerTyp.SONSTIGER_FEHLER).jobZuordnung("job")
			.netzfehlerBeschreibung(
				NetzfehlerBeschreibung.of("a"))
			.geometry(geometrie1).build();
		LineString geometrie2 = geometryFactory
			.createLineString(new Coordinate[] { new Coordinate(3.0, 4.0), new Coordinate(5.0, 6.0) });
		Netzfehler netzfehler2 = Netzfehler.builder().netzfehlerTyp(NetzfehlerTyp.ATTRIBUT_PROJEKTION)
			.jobZuordnung("job")
			.netzfehlerBeschreibung(
				NetzfehlerBeschreibung.of("b"))
			.geometry(geometrie2).build();
		Point geometrie3 = geometryFactory.createPoint(new Coordinate(8.0, 9.0));
		Netzfehler netzfehler3 = Netzfehler.builder().netzfehlerTyp(NetzfehlerTyp.MATCHING).jobZuordnung("job")
			.netzfehlerBeschreibung(
				NetzfehlerBeschreibung.of("c"))
			.geometry(geometrie3).build();
		Point geometrie4 = geometryFactory.createPoint(new Coordinate(10.0, 11.0));
		Netzfehler netzfehler4 = Netzfehler.builder().netzfehlerTyp(NetzfehlerTyp.MATCHING).jobZuordnung("job")
			.netzfehlerBeschreibung(
				NetzfehlerBeschreibung.of("d"))
			.geometry(geometrie4).build();
		netzfehlerRepository.saveAll(List.of(netzfehler1, netzfehler2, netzfehler3, netzfehler4));

		// act
		List<Feature> resultFeatures = netzausschnittController
			.getNetzfehlerGeoJson(authentication, new Envelope(0, 11, 0, 11))
			.getFeatures();

		// assert
		assertThat(resultFeatures).hasSize(4);

		assertThat(resultFeatures)
			.anyMatch(feature -> this.areLineStringGeometriesEqual(geometrie1, feature.getGeometry()))
			.anyMatch(feature -> this.areLineStringGeometriesEqual(geometrie2, feature.getGeometry()))
			.anyMatch(feature -> this.arePointGeometriesEqual(geometrie3, feature.getGeometry()))
			.anyMatch(feature -> this.arePointGeometriesEqual(geometrie4, feature.getGeometry()));
	}

	private boolean areLineStringGeometriesEqual(LineString jtsGeometry, GeoJsonObject geoJsonObject) {
		if (!(geoJsonObject instanceof org.geojson.LineString)) {
			return false;
		}
		List<LngLatAlt> geoJsonCoordinates = ((org.geojson.LineString) geoJsonObject).getCoordinates();
		for (int i = 0; i < jtsGeometry.getCoordinates().length; i++) {
			Coordinate jtsCoordinate = jtsGeometry.getCoordinates()[i];
			LngLatAlt geoJsonCoordinate = geoJsonCoordinates.get(i);
			if (jtsCoordinate.getX() != geoJsonCoordinate.getLongitude() || jtsCoordinate.getY() != geoJsonCoordinate
				.getLatitude()) {
				return false;
			}
		}
		return true;
	}

	private boolean arePointGeometriesEqual(Point jtsGeometry, GeoJsonObject geoJsonObject) {
		if (!(geoJsonObject instanceof org.geojson.Point)) {
			return false;
		}
		LngLatAlt geoJsonCoordinate = ((org.geojson.Point) geoJsonObject).getCoordinates();
		Coordinate jtsCoordinate = jtsGeometry.getCoordinate();

		return jtsCoordinate.getX() == geoJsonCoordinate.getLongitude()
			&& jtsCoordinate.getY() == geoJsonCoordinate.getLatitude();
	}
}
