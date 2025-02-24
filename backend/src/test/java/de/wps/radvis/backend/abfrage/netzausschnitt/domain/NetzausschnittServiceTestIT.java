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

package de.wps.radvis.backend.abfrage.netzausschnitt.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import de.wps.radvis.backend.abfrage.netzausschnitt.AbfrageNetzausschnittConfiguration;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteNetzklasseMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzNetzklasseMapView;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;

@Tag("group2")
@ContextConfiguration(classes = { NetzConfiguration.class, AbfrageNetzausschnittConfiguration.class,
	OrganisationConfiguration.class, GeoConverterConfiguration.class, CommonConfiguration.class,
	BenutzerConfiguration.class,
	KommentarConfiguration.class, KonsistenzregelPruefungsConfiguration.class, KonsistenzregelnConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	CommonConfigurationProperties.class,
	DLMConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@ActiveProfiles(profiles = "test")
class NetzausschnittServiceTestIT extends DBIntegrationTestIT {
	@Autowired
	private NetzService netzService;
	@Autowired
	private NetzausschnittService netzausschnittService;
	@MockitoBean
	private NetzfehlerRepository netzfehlerRepository;

	private static GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@Test
	void findNetzAusschnitt() {
		// arrange
		QuellSystem quelle = QuellSystem.DLM;
		LineString lineString1 = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(1, 2), new Coordinate(3, 4) });

		LineString lineString2 = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(3, 4), new Coordinate(8, 8) });

		LineString verlaufLinks = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(20, 3), new Coordinate(10, 10) });
		LineString verlaufRechts = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(20, 9), new Coordinate(12, 12) });

		Knoten knoten1 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString1.getStartPoint().getCoordinate(), quelle).build();
		Knoten knoten2 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString1.getEndPoint().getCoordinate(), quelle).build();
		Knoten knoten3 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString2.getEndPoint().getCoordinate(), quelle).build();

		Kante kante1 = netzService.saveKante(KanteTestDataProvider.withDefaultValuesAndZweiseitig().vonKnoten(knoten1)
			.nachKnoten(knoten2).geometry(lineString1).quelle(quelle).verlaufRechts(verlaufRechts)
			.isGrundnetz(true)
			.build());
		Kante kante2 = netzService.saveKante(KanteTestDataProvider.withDefaultValues().vonKnoten(knoten2)
			.nachKnoten(knoten3).geometry(lineString2).quelle(quelle).verlaufLinks(verlaufLinks)
			.isGrundnetz(true)
			.verlaufRechts(verlaufLinks).isZweiseitig(false).build());

		// sollen nicht geholt werden
		netzService.saveKante(
			KanteTestDataProvider.withCoordinatesAndQuelle(100, 100, 100, 110, QuellSystem.DLM).build());
		netzService.saveKante(
			KanteTestDataProvider.withCoordinatesAndQuelle(5, 5, 10, 10, QuellSystem.RadwegeDB).build());

		// act
		NetzMapView netzAusschnitt = netzausschnittService
			.findNetzAusschnitt(new Envelope(0, 22, 0, 22), Set.of(NetzklasseFilter.NICHT_KLASSIFIZIERT));

		Set<KanteMapView> kanten = netzAusschnitt.getKanten();

		// assert
		assertThat(kanten).hasSize(2);
		assertThat(kanten).extracting(p -> p.getGeometrie())
			.containsExactlyInAnyOrder(kante1.getGeometry(), kante2.getGeometry());
		assertThat(kanten).extracting(p -> p.getVerlaufLinks().orElse(null))
			.containsExactlyInAnyOrder(null, kante2.getVerlaufLinks().get());
		assertThat(kanten).extracting(p -> p.getVerlaufRechts().orElse(null))
			.containsExactlyInAnyOrder(kante1.getVerlaufRechts().get(), kante2.getVerlaufRechts().get());
	}

	@Test
	void findNetzAusschnittMitDlm() {
		// arrange
		QuellSystem quelle = QuellSystem.DLM;
		LineString lineString1 = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(1, 2), new Coordinate(3, 4) });

		LineString lineString2 = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(3, 4), new Coordinate(8, 8) });

		LineString verlaufLinks = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(20, 3), new Coordinate(10, 10) });
		LineString verlaufRechts = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(20, 9), new Coordinate(12, 12) });

		LineString lineString3 = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(8, 8), new Coordinate(22, 55) });

		Knoten knoten1 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString1.getStartPoint().getCoordinate(), quelle).build();
		Knoten knoten2 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString1.getEndPoint().getCoordinate(), quelle).build();
		Knoten knoten3 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString2.getEndPoint().getCoordinate(), quelle).build();

		Knoten knoten4 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString2.getEndPoint().getCoordinate(), QuellSystem.RadNETZ).build();
		Knoten knoten5 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString3.getEndPoint().getCoordinate(), QuellSystem.RadNETZ).build();

		Kante kanteOhneNetzKlasse = netzService.saveKante(
			KanteTestDataProvider.withDefaultValuesAndZweiseitig().vonKnoten(knoten1)
				.nachKnoten(knoten2).geometry(lineString1).quelle(quelle).verlaufRechts(verlaufRechts)
				.build());

		KantenAttribute kantenAttribute = KantenAttribute.builder().build();
		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppeTestDataProvider.defaultValue()
			.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG)).kantenAttribute(kantenAttribute).build();
		Kante kanteRadNetzNetzKlasse = netzService
			.saveKante(KanteTestDataProvider.withDefaultValues().vonKnoten(knoten2)
				.nachKnoten(knoten3).geometry(lineString2).quelle(quelle).verlaufLinks(verlaufLinks)
				.verlaufRechts(verlaufLinks).isZweiseitig(false).kantenAttributGruppe(kantenAttributGruppe).build());

		// sollen nicht geholt werden
		netzService.saveKante(KanteTestDataProvider.withDefaultValuesAndQuelle(QuellSystem.RadNETZ)
			.vonKnoten(knoten4)
			.nachKnoten(knoten5).geometry(lineString1)
			.isZweiseitig(false)
			.build());
		netzService.saveKante(
			KanteTestDataProvider.withCoordinatesAndQuelle(5, 5, 10, 10, QuellSystem.RadwegeDB).build());

		// act
		NetzNetzklasseMapView netzAusschnitt = netzausschnittService
			.findNetzAusschnittDLM(new Envelope(0, 1000, 0, 1000));

		// assert
		assertThat(netzAusschnitt.getKanten()).hasSize(2);
		assertThat(netzAusschnitt.getKanten()).containsExactlyInAnyOrder(
			new KanteNetzklasseMapView(kanteRadNetzNetzKlasse.getId(), kanteRadNetzNetzKlasse.getGeometry(),
				verlaufLinks, null,
				kanteRadNetzNetzKlasse.isZweiseitig(),
				KantenAttributGruppe.builder()
					.netzklassen(kanteRadNetzNetzKlasse.getKantenAttributGruppe().getNetzklassen()).build()),
			new KanteNetzklasseMapView(kanteOhneNetzKlasse.getId(), kanteOhneNetzKlasse.getGeometry(),
				null, null,
				kanteOhneNetzKlasse.isZweiseitig(), KantenAttributGruppe.builder().build()));
		assertThat(netzAusschnitt.getKnoten()).hasSize(3);
		assertThat(netzAusschnitt.getKnoten())
			.containsExactlyInAnyOrder(kanteOhneNetzKlasse.getVonKnoten(), kanteOhneNetzKlasse.getNachKnoten(),
				kanteRadNetzNetzKlasse.getNachKnoten());
	}

	@Test
	void findNetzAusschnittNurKnoten() {
		// arrange
		QuellSystem quelle = QuellSystem.DLM;
		LineString lineString1 = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(1, 2), new Coordinate(3, 4) });

		LineString lineString2 = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(3, 4), new Coordinate(8, 8) });

		LineString verlaufLinks = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(20, 3), new Coordinate(10, 10) });
		LineString verlaufRechts = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(20, 9), new Coordinate(12, 12) });

		Knoten knoten1 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString1.getStartPoint().getCoordinate(), quelle).build();
		Knoten knoten2 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString1.getEndPoint().getCoordinate(), quelle).build();
		Knoten knoten3 = KnotenTestDataProvider
			.withCoordinateAndQuelle(lineString2.getEndPoint().getCoordinate(), quelle).build();

		Kante kante1 = netzService.saveKante(KanteTestDataProvider.withDefaultValuesAndZweiseitig().vonKnoten(knoten1)
			.nachKnoten(knoten2).geometry(lineString1).quelle(quelle).verlaufRechts(verlaufRechts)
			.isGrundnetz(true)
			.build());
		Kante kante2 = netzService.saveKante(KanteTestDataProvider.withDefaultValues().vonKnoten(knoten2)
			.nachKnoten(knoten3).geometry(lineString2).quelle(quelle).verlaufLinks(verlaufLinks)
			.isGrundnetz(true)
			.verlaufRechts(verlaufLinks).isZweiseitig(false).build());

		// sollen nicht geholt werden
		netzService.saveKante(
			KanteTestDataProvider.withCoordinatesAndQuelle(100, 100, 100, 110, QuellSystem.DLM)
				.isGrundnetz(true)
				.build());
		netzService.saveKante(
			KanteTestDataProvider.withCoordinatesAndQuelle(5, 5, 10, 10, QuellSystem.RadwegeDB).build());

		// act
		NetzMapView netzAusschnitt = netzausschnittService
			.findNetzAusschnittNurKnoten(new Envelope(0, 22, 0, 22), Set.of(NetzklasseFilter.NICHT_KLASSIFIZIERT));

		// assert
		assertThat(netzAusschnitt.getKnoten()).hasSize(3);
		assertThat(netzAusschnitt.getKnoten())
			.containsExactlyInAnyOrder(kante1.getVonKnoten(), kante1.getNachKnoten(), kante2.getNachKnoten());

	}
}
