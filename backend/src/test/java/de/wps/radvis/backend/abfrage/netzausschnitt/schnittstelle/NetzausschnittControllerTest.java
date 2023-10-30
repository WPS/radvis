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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.abfrage.netzausschnitt.domain.NetzausschnittService;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteNetzklasseMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.NetzNetzklasseMapView;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingRepository;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingService;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KantenMapping;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.MappedKante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class NetzausschnittControllerTest {

	private NetzausschnittController netzausschnittController;

	@Mock
	private NetzausschnittService netzausschnittService;
	@Mock
	private NetzService netzService;
	@Mock
	private KantenMappingRepository kantenMappingRepository;
	@Mock
	private KantenRepository kantenRepository;
	@Mock
	private VerwaltungseinheitResolver verwaltungseinheitResolver;
	@Mock
	private NetzausschnittGuard netzausschnittGuard;

	private final Envelope envelope = new Envelope();
	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
	private final Long radNETZKanteId = 2L;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);

		List<MappedKante> abgebildeteKanten = new ArrayList<>();
		MappedKante mappedKante = new MappedKante(
			LinearReferenzierterAbschnitt.of(0, 1), LinearReferenzierterAbschnitt.of(0, 1), false,
			radNETZKanteId);
		abgebildeteKanten.add(mappedKante);
		KantenMapping kantenMapping = new KantenMapping(1L, QuellSystem.DLM, abgebildeteKanten);
		when(kantenMappingRepository.findAllById(List.of(1L)))
			.thenReturn(Set.of(kantenMapping));

		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppe.builder()
			.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG)).build();
		Set<KanteNetzklasseMapView> kantenMapView = new HashSet<>();
		LineString geometry = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(0, 1), new Coordinate(0, 2) });
		KanteNetzklasseMapView kantenNetzklasseMapView = new KanteNetzklasseMapView(1L, geometry, null, null, false,
			kantenAttributGruppe);
		kantenMapView.add(kantenNetzklasseMapView);
		NetzNetzklasseMapView netzNetzklasseMapView = new NetzNetzklasseMapView(kantenMapView, Collections.emptyList());
		when(netzausschnittService.findNetzAusschnittDLMIstRadNETZZugeordnet(envelope))
			.thenReturn(netzNetzklasseMapView);
		when(netzausschnittService.findNetzAusschnittDLM(envelope))
			.thenReturn(netzNetzklasseMapView);

		KantenMappingService kantenMappingService = new KantenMappingService(kantenMappingRepository, kantenRepository);
		NetzToGeoJsonConverter netzToGeoJsonConverter = new NetzToGeoJsonConverter();
		netzausschnittController = new NetzausschnittController(netzToGeoJsonConverter, netzService,
			netzausschnittService, kantenMappingService, verwaltungseinheitResolver, netzausschnittGuard);
	}

	@Test
	void getKantenGeoJsonDLM() {
		// arrange

		// act
		FeatureCollection result = netzausschnittController.getKantenGeoJsonDLM(envelope);

		// assert
		assertThat(result).isNotNull();

		List<Feature> features = result.getFeatures();
		assertThat(features).hasSize(1);
		Map<String, Object> properties = features.get(0).getProperties();

		assertThat(properties).containsEntry("netzKlassen", Set.of(Netzklasse.RADNETZ_ALLTAG));
		assertThat(properties).containsEntry("zugeordneteRadNETZKanten", List.of(radNETZKanteId));
	}

	@Test
	void getKantenGeoJsonDLMIstRadNETZZugeordnet() {
		// arrange

		// act
		FeatureCollection result = netzausschnittController.getKantenGeoJsonDLMIstRadNETZZugeordnet(envelope);

		// assert
		assertThat(result).isNotNull();

		List<Feature> features = result.getFeatures();
		assertThat(features).hasSize(1);
		Map<String, Object> properties = features.get(0).getProperties();

		assertThat(properties).containsEntry("netzKlassen", Set.of(Netzklasse.RADNETZ_ALLTAG));
	}

	@Test
	void getKantenInOrganisationsbereich_eagerIfHasNetzklasse() {
		// arrange
		Verwaltungseinheit wps = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build();
		when(this.verwaltungseinheitResolver.resolve(1L)).thenReturn(wps);

		// act
		netzausschnittController.getKantenInOrganisationsbereich(1L, Netzklasse.KREISNETZ_FREIZEIT);

		// assert
		verify(netzService).getKantenInOrganisationsbereichEagerFetchNetzklassen(wps);
	}

	@Test
	void getKantenInOrganisationsbereich_notEagerIfHasNotNetzklasse() {
		// arrange
		Verwaltungseinheit wps = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build();
		when(this.verwaltungseinheitResolver.resolve(1L)).thenReturn(wps);

		// act
		netzausschnittController.getKantenInOrganisationsbereich(1L, null);

		verify(netzService).getKantenInOrganisationsbereich(wps);
	}
}
