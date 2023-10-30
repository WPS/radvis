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

package de.wps.radvis.backend.integration.grundnetzReimport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.grundnetz.domain.DLMAttributMapper;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.DLMReimportJobStatistik;
import de.wps.radvis.backend.integration.grundnetzReimport.domain.entity.KnotenTupel;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeatureBuilder;
import de.wps.radvis.backend.quellimport.common.domain.valueObject.Art;

public class CreateKantenServiceTest {

	private CreateKantenService createKantenService;
	private DLMReimportJobStatistik dlmReimportJobStatistik;
	@Mock
	private DLMAttributMapper dlmAttributMapper;
	@Mock
	private NetzService netzService;
	@Mock
	private FindKnotenFromIndexService findKnotenFromIndexService;
	@Captor
	private ArgumentCaptor<Kante> kantenCaptor;
	GeometryFactory geometryFactory = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		createKantenService = new CreateKantenService(dlmAttributMapper, netzService, findKnotenFromIndexService);
		dlmReimportJobStatistik = new DLMReimportJobStatistik();
	}

	@Test
	void testeCreateNewDLMKante_NichtUnterstuetzterGeometrieTyp() {
		// arrange
		Point point = geometryFactory.createPoint();
		ImportedFeature importedFeature = ImportedFeatureBuilder.empty().importDatum(LocalDateTime.now())
			.quelle(QuellSystem.DLM).art(
				Art.Strecke)
			.fachId("123").geometry(point).build();

		// act
		createKantenService.createNewDLMKante(importedFeature, dlmReimportJobStatistik, null);

		// assert
		verify(netzService, Mockito.never()).saveKante(any());
	}

	@Test
	void testeCreateNewDLMKante_geometrieTypKorrekt_korrekteKanteGespeichert() throws Exception {
		// arrange
		KantenAttributGruppe kantenAttributGruppe = KantenAttributGruppe.builder()
			.kantenAttribute(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute().build())
			.build();
		when(dlmAttributMapper.mapKantenAttributGruppe(any())).thenReturn(kantenAttributGruppe);
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = FahrtrichtungAttributGruppe.builder().build();
		when(dlmAttributMapper.mapFahrtrichtungAttributGruppe(any())).thenReturn(fahrtrichtungAttributGruppe);
		ZustaendigkeitAttribute zustaendigkeitAttribute = ZustaendigkeitAttribute.builder()
			.build();
		when(dlmAttributMapper.mapZustaendigkeitAttribute(any())).thenReturn(zustaendigkeitAttribute);
		FuehrungsformAttribute fuehrungsformAttribute = FuehrungsformAttribute.builder()
			.build();
		when(dlmAttributMapper.mapFuehrungsformAttribute(any())).thenReturn(fuehrungsformAttribute);
		GeschwindigkeitAttribute geschwindigkeitAttribute = GeschwindigkeitAttribute.builder()
			.build();
		when(dlmAttributMapper.mapGeschindigkeitAttribute(any())).thenReturn(geschwindigkeitAttribute);
		Coordinate a = new Coordinate(0, 0);
		Coordinate b = new Coordinate(1, 1);
		Knoten knotenA = new Knoten(geometryFactory.createPoint(a), QuellSystem.DLM);
		Knoten knotenB = new Knoten(geometryFactory.createPoint(b), QuellSystem.DLM);
		LineString lineString = geometryFactory.createLineString(new Coordinate[] { a, b });
		when(findKnotenFromIndexService.findOrCreateKnotenTupel(any(Point.class), any(Point.class), any()))
			.thenReturn(new KnotenTupel(knotenA, knotenB));
		ImportedFeature importedFeature = ImportedFeatureBuilder.empty().importDatum(LocalDateTime.now())
			.quelle(QuellSystem.DLM).art(
				Art.Strecke)
			.fachId("123").geometry(lineString).build();

		// act
		createKantenService.createNewDLMKante(importedFeature, dlmReimportJobStatistik, null);

		// assert
		verify(netzService).saveKante(kantenCaptor.capture());
		Kante capturedKante = kantenCaptor.getValue();
		assertThat(capturedKante.getUrsprungsfeatureTechnischeID()).isEqualTo("123");
		assertThat(capturedKante.getVonKnoten()).isEqualTo(knotenA);
		assertThat(capturedKante.getNachKnoten()).isEqualTo(knotenB);
		assertThat(capturedKante.isZweiseitig()).isFalse();
		assertThat(capturedKante.getQuelle()).isEqualTo(QuellSystem.DLM);
		assertThat(capturedKante.getKantenAttributGruppe()).isEqualTo(kantenAttributGruppe);
		assertThat(capturedKante.getFahrtrichtungAttributGruppe()).isEqualTo(fahrtrichtungAttributGruppe);
		assertThat(capturedKante.getZustaendigkeitAttributeAnPunkt(a)).isEqualTo(zustaendigkeitAttribute);
		assertThat(capturedKante.getGeschwindigkeitAttributeAnPunkt(a)).isEqualTo(geschwindigkeitAttribute);
		assertThat(capturedKante.getFuehrungsformAttributeAnPunkt(a)).isEqualTo(fuehrungsformAttribute);
	}

}
