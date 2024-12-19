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

package de.wps.radvis.backend.integration.dlm.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeatureTestDataProvider;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;
import jakarta.persistence.EntityManager;

public class DLMNetzbildungServiceTest {

	private DLMNetzbildungService dlmNetzBildungService;

	@Mock
	private NetzService netzService;

	@Mock
	private EntityManager entityManager;

	@Mock
	private DLMNetzbildungProtokollService protokollService;

	@Captor
	private ArgumentCaptor<Kante> kanteCaptor;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
		this.dlmNetzBildungService = new DLMNetzbildungService(protokollService, netzService, entityManager);
		when(entityManager.merge(any())).thenAnswer(answer -> answer.getArgument(0));
	}

	@Test
	public void testBildeDLMBasisNetz_keineFeatures_netzIstLeer() {
		// arrange
		List<ImportedFeature> features = new ArrayList<>();

		// act
		this.dlmNetzBildungService.bildeDLMBasisNetz(features.stream());

		// assert
		verify(netzService, never()).saveKante(any());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testBildeDLMBasisNetz_Geometrie_und_KnotenKorrekt() {
		// arrange
		List<ImportedFeature> features = new ArrayList<>();

		Coordinate coordinate1 = new Coordinate(10, 10);
		Coordinate coordinate2 = new Coordinate(15, 15);
		ImportedFeature feature = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2)
			.quelle(QuellSystem.DLM)
			.fachId("dlmId_mit16Chars")
			.build();
		LineString lineString = (LineString) feature.getGeometrie();
		features.add(feature);

		// act
		this.dlmNetzBildungService.bildeDLMBasisNetz(features.stream());

		// assert
		verify(netzService).saveKante(kanteCaptor.capture());

		Kante result = kanteCaptor.getValue();
		assertThat(result.getDlmId()).isEqualTo(DlmId.of("dlmId_mit16Chars"));
		assertThat(result.getGeometry()).isEqualTo(lineString);
		assertThat(result.getQuelle()).isEqualTo(QuellSystem.DLM);
	}

	@Test
	public void testBildeDLMBasisNetz_KanteMitVollstaendigenAttributen() {
		// arrange
		Coordinate coordinate1 = new Coordinate(10, 10);
		Coordinate coordinate2 = new Coordinate(15, 15);
		ImportedFeature feature = ImportedFeatureTestDataProvider.withLineString(coordinate1, coordinate2)
			.quelle(QuellSystem.DLM)
			.addAttribut("eigenname", "Hans-Henny-Jahn-Weg")
			.addAttribut("bezeichnung", "A7")
			.fachId("dlmId_mit16Chars")
			.build();
		List<ImportedFeature> features = new ArrayList<>();
		features.add(feature);

		// act
		this.dlmNetzBildungService.bildeDLMBasisNetz(features.stream());

		// assert
		verify(netzService).saveKante(kanteCaptor.capture());

		Kante kante = kanteCaptor.getValue();
		assertThat(kante.getDlmId()).isEqualTo(DlmId.of("dlmId_mit16Chars"));

		KantenAttribute expectedKantenAttribute = KantenAttribute.builder()
			.strassenName(StrassenName.of("Hans-Henny-Jahn-Weg"))
			.strassenNummer(StrassenNummer.of("A7"))
			.beleuchtung(Beleuchtung.UNBEKANNT)
			.strassenquerschnittRASt06(StrassenquerschnittRASt06.UNBEKANNT)
			.umfeld(Umfeld.UNBEKANNT)
			.status(Status.defaultWert())
			.build();

		assertThat(kante.getKantenAttributGruppe().getKantenAttribute()).usingRecursiveComparison()
			.isEqualTo(expectedKantenAttribute);
	}

	@Test
	public void testBildeDLMBasisNetz_kurzerLineStringUndFuerBeideEndpunktePassenderKnotenBereitsVorhanden_VonUndNachKnotenNichtIdentisch() {
		// arrange
		List<ImportedFeature> features = new ArrayList<>();

		ImportedFeature lineStringFeature1 = ImportedFeatureTestDataProvider.withLineString()
			.lineString(new Coordinate(1.75, 1.75), new Coordinate(100, 100))
			.fachId("dlmId1mit16Chars")
			.build();
		features.add(lineStringFeature1);

		ImportedFeature lineStringFeature2 = ImportedFeatureTestDataProvider.withLineString()
			.lineString(new Coordinate(1, 1), new Coordinate(2.5, 2.5))
			.fachId("dlmId2mit16Chars")
			.build();
		features.add(lineStringFeature2);

		// act
		this.dlmNetzBildungService.bildeDLMBasisNetz(features.stream());

		// assert
		verify(netzService, times(2)).saveKante(kanteCaptor.capture());
		List<Kante> kanten = kanteCaptor.getAllValues();
		Kante kurzeKante = kanten.get(1);
		assertThat(kurzeKante.getVonKnoten().getKoordinate()).isNotEqualTo(kurzeKante.getNachKnoten().getKoordinate());
	}

}
