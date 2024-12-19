/*
 * Copyright (c) 2024 WPS - Workplace Solutions GmbH
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.google.common.collect.Lists;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenAttributeUebertragungService;
import de.wps.radvis.backend.integration.dlm.domain.entity.Attributluecke;
import de.wps.radvis.backend.integration.dlm.domain.entity.AttributlueckenSchliessenJobStatistik;
import de.wps.radvis.backend.integration.dlm.domain.entity.AttributlueckenSchliessenProblem;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteWithInitialStatesView;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.Hoechstgeschwindigkeit;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenkategorieRIN;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenquerschnittRASt06;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import de.wps.radvis.backend.netz.domain.valueObject.Umfeld;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.netz.domain.valueObject.VerkehrStaerke;
import de.wps.radvis.backend.netz.domain.valueObject.WegeNiveau;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

class AttributlueckenServiceTest {

	@Mock
	private AttributlueckenSchliessenProblemRepository attributlueckenSchliessenProblemRepository;

	@Mock
	private NetzService netzService;

	@Mock
	private Benutzer benutzer;

	AttributlueckenService attributlueckenService;
	ArgumentCaptor<AttributlueckenSchliessenProblem> attributSchliessenProblemCaptor;

	@BeforeEach
	public void setup() {
		MockitoAnnotations.openMocks(this);

		attributSchliessenProblemCaptor = ArgumentCaptor.forClass(AttributlueckenSchliessenProblem.class);
		when(attributlueckenSchliessenProblemRepository.save(attributSchliessenProblemCaptor.capture())).thenReturn(
			mock(AttributlueckenSchliessenProblem.class));

		when(netzService.getKnoten(any(Long.class))).thenReturn(mock(Knoten.class));

		attributlueckenService = new AttributlueckenService(
			new KantenAttributeUebertragungService(Laenge.of(3)),
			attributlueckenSchliessenProblemRepository,
			netzService,
			100,
			3,
			1);
	}

	@Test
	public void testErmittleLuecken_einfacheLuecke() {
		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2).build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3).build();
		Kante kante34 = buildNormaleKante(34L, knoten3, knoten4).build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 2, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View));

		HashMap<Long, List<Kante>> knotenToAlLKantenMap = new HashMap<>();
		knotenToAlLKantenMap.put(knoten1.getId(), List.of(kante12));
		knotenToAlLKantenMap.put(knoten2.getId(), List.of(kante12, kante23_luecke));
		knotenToAlLKantenMap.put(knoten3.getId(), List.of(kante23_luecke, kante34));
		knotenToAlLKantenMap.put(knoten4.getId(), List.of(kante34));

		AttributlueckenSchliessenJobStatistik statistik = new AttributlueckenSchliessenJobStatistik();

		// Act
		ArrayList<Attributluecke> attributluecken = attributlueckenService.ermittleLuecken(knotenToKantenViewMap,
			knotenToAlLKantenMap, statistik);

		// Assert
		assertThat(attributluecken).hasSize(1);
		assertThat(attributluecken.get(0).getStartKnoten()).isEqualTo(knoten2);
		assertThat(attributluecken.get(0).getEndKnoten()).isEqualTo(knoten3);
		assertThat(attributluecken.get(0).getAdjazenteKantenAmStart()).containsExactlyInAnyOrder(kante12,
			kante23_luecke);
		assertThat(attributluecken.get(0).getAdjazenteKantenAmEnde()).containsExactlyInAnyOrder(kante34,
			kante23_luecke);
		assertThat(attributluecken.get(0).getLueckeKantenPfad()).containsExactly(kante23_luecke);

		verify(attributlueckenSchliessenProblemRepository, never()).save(any());
	}

	@Test
	public void testErmittleLuecken_einfacheLuecke_zuLang() {
		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(930, 10).id(3L).build(); // <- Hohe Distanz zu Knoten2
		Knoten knoten4 = KnotenTestDataProvider.withPosition(940, 10).id(4L).build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2).build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3).build();
		Kante kante34 = buildNormaleKante(34L, knoten3, knoten4).build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 2, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View));

		HashMap<Long, List<Kante>> knotenToAlLKantenMap = new HashMap<>();
		knotenToAlLKantenMap.put(knoten1.getId(), List.of(kante12));
		knotenToAlLKantenMap.put(knoten2.getId(), List.of(kante12, kante23_luecke));
		knotenToAlLKantenMap.put(knoten3.getId(), List.of(kante23_luecke, kante34));
		knotenToAlLKantenMap.put(knoten4.getId(), List.of(kante34));

		AttributlueckenSchliessenJobStatistik statistik = new AttributlueckenSchliessenJobStatistik();

		// Act
		ArrayList<Attributluecke> attributluecken = attributlueckenService.ermittleLuecken(knotenToKantenViewMap,
			knotenToAlLKantenMap, statistik);

		// Assert
		assertThat(attributluecken).isEmpty();

		assertThat(attributSchliessenProblemCaptor.getAllValues()).hasSize(2);
		assertThat(statistik.anzahlPotentielleLueckenIgnoriertDaAllePfadeZuLang).isEqualTo(2);
	}

	@Test
	public void testErmittleLuecken_einfacheLuecke_zuVieleKanten() {
		//@formatter:off
		/*
		Situation:
		
		1 == 2 -- 3 -- 4 -- 5 -- 6 == 7
		     |____.____.____.____|        <- Lücke, die aber zu viele Kanten hat.
		 */
		//@formatter:on

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();
		Knoten knoten5 = KnotenTestDataProvider.withPosition(50, 10).id(5L).build();
		Knoten knoten6 = KnotenTestDataProvider.withPosition(60, 10).id(6L).build();
		Knoten knoten7 = KnotenTestDataProvider.withPosition(70, 10).id(7L).build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2).build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3).build();
		Kante kante34_luecke = buildLueckenKante(34L, knoten3, knoten4).build();
		Kante kante45_luecke = buildLueckenKante(45L, knoten4, knoten5).build();
		Kante kante56_luecke = buildLueckenKante(56L, knoten5, knoten6).build();
		Kante kante67 = buildNormaleKante(67L, knoten6, knoten7).build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 2, (short) 2);
		KanteWithInitialStatesView kante34View = getKanteWithInitialStatesView(kante34_luecke, (short) 2, (short) 2);
		KanteWithInitialStatesView kante45View = getKanteWithInitialStatesView(kante45_luecke, (short) 2, (short) 2);
		KanteWithInitialStatesView kante56View = getKanteWithInitialStatesView(kante56_luecke, (short) 2, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View, kante34View));
		knotenToKantenViewMap.put(knoten4.getId(), Lists.newArrayList(kante34View, kante45View));
		knotenToKantenViewMap.put(knoten5.getId(), Lists.newArrayList(kante45View, kante56View));
		knotenToKantenViewMap.put(knoten6.getId(), Lists.newArrayList(kante56View));

		HashMap<Long, List<Kante>> knotenToAlLKantenMap = new HashMap<>();
		knotenToAlLKantenMap.put(knoten1.getId(), List.of(kante12));
		knotenToAlLKantenMap.put(knoten2.getId(), List.of(kante12, kante23_luecke));
		knotenToAlLKantenMap.put(knoten3.getId(), List.of(kante23_luecke, kante34_luecke));
		knotenToAlLKantenMap.put(knoten4.getId(), List.of(kante34_luecke, kante45_luecke));
		knotenToAlLKantenMap.put(knoten5.getId(), List.of(kante45_luecke, kante56_luecke));
		knotenToAlLKantenMap.put(knoten6.getId(), List.of(kante56_luecke, kante67));
		knotenToAlLKantenMap.put(knoten7.getId(), List.of(kante67));

		AttributlueckenSchliessenJobStatistik statistik = new AttributlueckenSchliessenJobStatistik();

		// Act
		ArrayList<Attributluecke> attributluecken = attributlueckenService.ermittleLuecken(knotenToKantenViewMap,
			knotenToAlLKantenMap, statistik);

		// Assert
		assertThat(attributluecken).isEmpty();

		assertThat(attributSchliessenProblemCaptor.getAllValues()).hasSize(2);
		assertThat(statistik.anzahlPotentielleLueckenIgnoriertDaZuVieleKantenNoetig).isEqualTo(2);
	}

	@Test
	public void testErmittleLuecken_zweiGleicheKantenVerbunden() {
		//@formatter:off
		/*
		Lücke teilt sich auf, mehrere Pfade sind zu erwarten (s.u.).
		
			- sind Lücken Kanten
			= sind normale Kanten
		
			   1 ==== 4
			  /  \
			2 ==== 3
		
		Die Pfade 1-2 und 1-3 sind NICHT zu erwarten. Würde man bei 1 starten zu suchen, wäre der Lückenschluss nicht
		eindeutig. Fängt man aber bei 2 oder 3 an zu suchen, dann wäre der Lückenschluss eindeutig. In Wahrheit ist er
		aber in der Tat nicht eindeutig und daher ignorieren wir solche Konstrukte.
		 */
		//@formatter:on

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		Kante kante12_luecke = buildNormaleKante(12L, knoten1, knoten2).build();
		Kante kante13_luecke = buildLueckenKante(13L, knoten1, knoten3).build();
		Kante kante14 = buildLueckenKante(14L, knoten1, knoten4).build();
		Kante kante23 = buildLueckenKante(23L, knoten2, knoten3).build();

		KanteWithInitialStatesView kante12View = getKanteWithInitialStatesView(kante12_luecke, (short) 3, (short) 2);
		KanteWithInitialStatesView kante13View = getKanteWithInitialStatesView(kante13_luecke, (short) 3, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten1.getId(), Lists.newArrayList(kante12View, kante13View));
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante12View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante13View));

		HashMap<Long, List<Kante>> knotenToAlLKantenMap = new HashMap<>();
		knotenToAlLKantenMap.put(knoten1.getId(), List.of(kante12_luecke, kante13_luecke));
		knotenToAlLKantenMap.put(knoten2.getId(), List.of(kante12_luecke, kante23));
		knotenToAlLKantenMap.put(knoten3.getId(), List.of(kante13_luecke, kante23));
		knotenToAlLKantenMap.put(knoten4.getId(), List.of(kante14));

		AttributlueckenSchliessenJobStatistik statistik = new AttributlueckenSchliessenJobStatistik();

		// Act
		ArrayList<Attributluecke> attributluecken = attributlueckenService.ermittleLuecken(knotenToKantenViewMap,
			knotenToAlLKantenMap, statistik);

		// Assert
		assertThat(attributluecken).isEmpty();

		assertThat(attributSchliessenProblemCaptor.getAllValues()).hasSize(1);
		assertThat(statistik.anzahlLueckenIgnoriertDaGemeinsamerKnoten).isEqualTo(2);
	}

	@Test
	public void testErmittleLuecken_entferntSichBeruehrendeLuecken() {
		//@formatter:off
		/*
		Die beiden Lücken 1-4 und 1-5 sind zwar an sich ok, berühren sich aber. Wir wollen solche Lücken ignorieren,
		um wirklich sicherzugehen, dass übernommene Attribute korrekt sind. Bei sich berührenden Lücken besteht eine
		Unsicherheit (weil bspw. eine Lücke eine irrelevante Nebenstraße ist), dass die Attribute nur auf einer der
		Lücken korrekt sind.
		
			- sind Lücken Kanten
			= sind normale Kanten
		
			         1 === 2
			        / \
			3 === 4    5 === 6
		 */
		//@formatter:on

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(30, 30).id(10L).build(); // <- hohe Id, damit Knoten erst
																					 // spät betrachtet wird
		Knoten knoten2 = KnotenTestDataProvider.withPosition(40, 30).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(10, 20).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(20, 20).id(4L).build();
		Knoten knoten5 = KnotenTestDataProvider.withPosition(30, 20).id(5L).build();
		Knoten knoten6 = KnotenTestDataProvider.withPosition(40, 20).id(6L).build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2).build();
		Kante kante14_luecke = buildLueckenKante(14L, knoten1, knoten4).build();
		Kante kante15_luecke = buildLueckenKante(15L, knoten1, knoten5).build();
		Kante kante34 = buildNormaleKante(34L, knoten3, knoten4).build();
		Kante kante56 = buildNormaleKante(56L, knoten5, knoten6).build();

		KanteWithInitialStatesView kante14View = getKanteWithInitialStatesView(kante14_luecke, (short) 3, (short) 2);
		KanteWithInitialStatesView kante15View = getKanteWithInitialStatesView(kante15_luecke, (short) 3, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten1.getId(), Lists.newArrayList(kante14View, kante15View));
		knotenToKantenViewMap.put(knoten4.getId(), Lists.newArrayList(kante14View));
		knotenToKantenViewMap.put(knoten5.getId(), Lists.newArrayList(kante15View));

		HashMap<Long, List<Kante>> knotenToAllKantenMap = new HashMap<>();
		knotenToAllKantenMap.put(knoten1.getId(), List.of(kante12, kante14_luecke, kante15_luecke));
		knotenToAllKantenMap.put(knoten2.getId(), List.of(kante12));
		knotenToAllKantenMap.put(knoten3.getId(), List.of(kante34));
		knotenToAllKantenMap.put(knoten4.getId(), List.of(kante14_luecke, kante34));
		knotenToAllKantenMap.put(knoten5.getId(), List.of(kante15_luecke, kante56));
		knotenToAllKantenMap.put(knoten6.getId(), List.of(kante56));

		AttributlueckenSchliessenJobStatistik statistik = new AttributlueckenSchliessenJobStatistik();

		// Act
		ArrayList<Attributluecke> attributluecken = attributlueckenService.ermittleLuecken(knotenToKantenViewMap,
			knotenToAllKantenMap, statistik);

		// Assert
		assertThat(attributluecken).isEmpty();
		assertThat(statistik.anzahlLueckenIgnoriertDaGemeinsamerKnoten).isEqualTo(2);
		assertThat(attributSchliessenProblemCaptor.getAllValues()).hasSize(1);
	}

	@Test
	public void testErmittleLuecken_behaeltEinfacheLueckeAusMehrerenKanten() {
		//@formatter:off
		/*
			- sind Lücken Kanten
			= sind normale Kanten
		
			1 === 2 --- 3 --- 4 === 5
		
			Das Löschen sich berührender Lücken sollte keinen Einfluss auf solche Lücken haben, die aus mehreren sich
			berührenden Kanten bestehen.
		 */
		//@formatter:on

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();
		Knoten knoten5 = KnotenTestDataProvider.withPosition(50, 10).id(5L).build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2).build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3).build();
		Kante kante34_luecke = buildLueckenKante(34L, knoten3, knoten4).build();
		Kante kante45 = buildNormaleKante(34L, knoten4, knoten5).build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 2, (short) 2);
		KanteWithInitialStatesView kante34View = getKanteWithInitialStatesView(kante34_luecke, (short) 2, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View, kante34View));
		knotenToKantenViewMap.put(knoten4.getId(), Lists.newArrayList(kante34View));

		HashMap<Long, List<Kante>> knotenToAlLKantenMap = new HashMap<>();
		knotenToAlLKantenMap.put(knoten1.getId(), List.of(kante12));
		knotenToAlLKantenMap.put(knoten2.getId(), List.of(kante12, kante23_luecke));
		knotenToAlLKantenMap.put(knoten3.getId(), List.of(kante23_luecke, kante34_luecke));
		knotenToAlLKantenMap.put(knoten4.getId(), List.of(kante34_luecke, kante45));
		knotenToAlLKantenMap.put(knoten5.getId(), List.of(kante45));

		AttributlueckenSchliessenJobStatistik statistik = new AttributlueckenSchliessenJobStatistik();

		// Act
		ArrayList<Attributluecke> attributluecken = attributlueckenService.ermittleLuecken(knotenToKantenViewMap,
			knotenToAlLKantenMap, statistik);

		// Assert
		assertThat(attributluecken).hasSize(1);

		assertThat(attributluecken.get(0).getStartKnoten()).isEqualTo(knoten2);
		assertThat(attributluecken.get(0).getEndKnoten()).isEqualTo(knoten4);
		assertThat(attributluecken.get(0).getAdjazenteKantenAmStart()).containsExactlyInAnyOrder(kante12,
			kante23_luecke);
		assertThat(attributluecken.get(0).getAdjazenteKantenAmEnde()).containsExactlyInAnyOrder(kante34_luecke,
			kante45);
		assertThat(attributluecken.get(0).getLueckeKantenPfad()).containsExactly(kante23_luecke, kante34_luecke);

		verify(attributlueckenSchliessenProblemRepository, never()).save(any());
	}

	@Test
	public void testErmittleLuecken_mehrdeutigerLueckenpfad() {
		//@formatter:off
		/*
		Lücke teilt sich innerhalb des Lücken-Netzes (hier an Knoten 5) auf.
		
			- sind Lücken Kanten
			= sind normale Kanten
		
			       1 ==== 2
			              |
			3 ==== 4 ---- 5 ---- 6 ==== 7
		
		Kein Pfad ist zu erwarten, da der Lückenschluss nicht eindeutig ist, da alle Pfade gleich lang sind, egal wo man anfängt.
		 */
		//@formatter:on

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(20, 20).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(30, 20).id(2L).build();

		Knoten knoten3 = KnotenTestDataProvider.withPosition(10, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(20, 10).id(4L).build();
		Knoten knoten5 = KnotenTestDataProvider.withPosition(30, 10).id(5L).build();
		Knoten knoten6 = KnotenTestDataProvider.withPosition(40, 10).id(6L).build();
		Knoten knoten7 = KnotenTestDataProvider.withPosition(50, 10).id(7L).build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2).build();
		Kante kante25_luecke = buildLueckenKante(25L, knoten2, knoten5).build();

		Kante kante34 = buildNormaleKante(34L, knoten3, knoten4).build();
		Kante kante45_luecke = buildLueckenKante(45L, knoten4, knoten5).build();
		Kante kante56_luecke = buildNormaleKante(56L, knoten5, knoten6).build();
		Kante kante67 = buildNormaleKante(67L, knoten6, knoten7).build();

		KanteWithInitialStatesView kante25View = getKanteWithInitialStatesView(kante25_luecke, (short) 2, (short) 3);
		KanteWithInitialStatesView kante45View = getKanteWithInitialStatesView(kante45_luecke, (short) 2, (short) 3);
		KanteWithInitialStatesView kante56View = getKanteWithInitialStatesView(kante56_luecke, (short) 3, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante25View));
		knotenToKantenViewMap.put(knoten4.getId(), Lists.newArrayList(kante45View));
		knotenToKantenViewMap.put(knoten5.getId(), Lists.newArrayList(kante25View, kante45View, kante56View));
		knotenToKantenViewMap.put(knoten6.getId(), Lists.newArrayList(kante56View));

		HashMap<Long, List<Kante>> knotenToAlLKantenMap = new HashMap<>();
		knotenToAlLKantenMap.put(knoten1.getId(), List.of(kante12));
		knotenToAlLKantenMap.put(knoten2.getId(), List.of(kante12, kante25_luecke));
		knotenToAlLKantenMap.put(knoten3.getId(), List.of(kante34));
		knotenToAlLKantenMap.put(knoten4.getId(), List.of(kante34, kante45_luecke));
		knotenToAlLKantenMap.put(knoten5.getId(), List.of(kante45_luecke, kante56_luecke));
		knotenToAlLKantenMap.put(knoten6.getId(), List.of(kante56_luecke, kante67));
		knotenToAlLKantenMap.put(knoten7.getId(), List.of(kante67));

		AttributlueckenSchliessenJobStatistik statistik = new AttributlueckenSchliessenJobStatistik();

		// Act
		ArrayList<Attributluecke> attributluecken = attributlueckenService.ermittleLuecken(knotenToKantenViewMap,
			knotenToAlLKantenMap, statistik);

		// Assert
		assertThat(attributluecken).isEmpty();

		assertThat(attributSchliessenProblemCaptor.getAllValues()).hasSize(3);
		assertThat(statistik.anzahlLueckenIgnoriertDaMehrdeutig).isEqualTo(3);
	}

	@Test
	public void testErmittleLuecken_nichtEindeutigerLueckenpfad_trotzdemErgebnisWegenLaenge() {
		//@formatter:off
		/*
		Lücke teilt sich innerhalb des Lücken-Netzes (hier an Knoten 5) auf.
		
			- sind Lücken Kanten
			= sind normale Kanten
		
			       1 ==== 2
			              |
			3 ==== 4 ---- 5 ---------------- 6 ==== 7
		
		Lücke eigentlich nicht eindeutig, aber Pfad 2-4-5 wird gefunden, da alle anderen zu lang sind und somit die
		Lücke doch eindeutig geschlossen werden kann.
		 */
		//@formatter:on

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(20, 20).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(30, 20).id(2L).build();

		Knoten knoten3 = KnotenTestDataProvider.withPosition(10, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(20, 10).id(4L).build();
		Knoten knoten5 = KnotenTestDataProvider.withPosition(30, 10).id(5L).build();
		Knoten knoten6 = KnotenTestDataProvider.withPosition(940, 10).id(6L).build(); // <- Weit weg
		Knoten knoten7 = KnotenTestDataProvider.withPosition(950, 10).id(7L).build(); // <- Weit weg

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2).build();
		Kante kante25_luecke = buildLueckenKante(25L, knoten2, knoten5).build();

		Kante kante34 = buildNormaleKante(34L, knoten3, knoten4).build();
		Kante kante45_luecke = buildLueckenKante(45L, knoten4, knoten5).build();
		Kante kante56_luecke = buildNormaleKante(56L, knoten5, knoten6).build();
		Kante kante67 = buildNormaleKante(67L, knoten6, knoten7).build();

		KanteWithInitialStatesView kante25View = getKanteWithInitialStatesView(kante25_luecke, (short) 2, (short) 3);
		KanteWithInitialStatesView kante45View = getKanteWithInitialStatesView(kante45_luecke, (short) 2, (short) 3);
		KanteWithInitialStatesView kante56View = getKanteWithInitialStatesView(kante56_luecke, (short) 3, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante25View));
		knotenToKantenViewMap.put(knoten4.getId(), Lists.newArrayList(kante45View));
		knotenToKantenViewMap.put(knoten5.getId(), Lists.newArrayList(kante25View, kante45View, kante56View));
		knotenToKantenViewMap.put(knoten6.getId(), Lists.newArrayList(kante56View));

		HashMap<Long, List<Kante>> knotenToAlLKantenMap = new HashMap<>();
		knotenToAlLKantenMap.put(knoten1.getId(), List.of(kante12));
		knotenToAlLKantenMap.put(knoten2.getId(), List.of(kante12, kante25_luecke));
		knotenToAlLKantenMap.put(knoten3.getId(), List.of(kante34));
		knotenToAlLKantenMap.put(knoten4.getId(), List.of(kante34, kante45_luecke));
		knotenToAlLKantenMap.put(knoten5.getId(), List.of(kante45_luecke, kante56_luecke));
		knotenToAlLKantenMap.put(knoten6.getId(), List.of(kante56_luecke, kante67));
		knotenToAlLKantenMap.put(knoten7.getId(), List.of(kante67));

		AttributlueckenSchliessenJobStatistik statistik = new AttributlueckenSchliessenJobStatistik();

		// Act
		ArrayList<Attributluecke> attributluecken = attributlueckenService.ermittleLuecken(knotenToKantenViewMap,
			knotenToAlLKantenMap, statistik);

		// Assert
		assertThat(attributluecken).hasSize(1);

		Optional<Attributluecke> luecke254 = attributluecken.stream()
			.filter(l -> l.getLueckeKantenPfad().contains(kante45_luecke))
			.findFirst();
		assertThat(luecke254).isPresent();
		assertThat(luecke254.get().getStartKnoten()).isEqualTo(knoten2);
		assertThat(luecke254.get().getEndKnoten()).isEqualTo(knoten4);
		assertThat(luecke254.get().getAdjazenteKantenAmStart()).containsExactlyInAnyOrder(kante12, kante25_luecke);
		assertThat(luecke254.get().getAdjazenteKantenAmEnde()).containsExactlyInAnyOrder(kante34, kante45_luecke);
		assertThat(luecke254.get().getLueckeKantenPfad()).containsExactlyInAnyOrder(kante25_luecke, kante45_luecke);

		assertThat(attributSchliessenProblemCaptor.getAllValues()).hasSize(1);
		assertThat(statistik.anzahlPotentielleLueckenIgnoriertDaAllePfadeZuLang).isEqualTo(1);
	}

	@Test
	public void testErmittleLuecken_echteSackgasse() {
		// Echte Sackgassen, wo es also im Netz generell nicht weiter geht, stellen keine Lücken dar und werden
		// ignoriert.

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2).build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3).build();
		Kante kante34_luecke = buildLueckenKante(34L, knoten4, knoten3).build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 2, (short) 2);
		KanteWithInitialStatesView kante34View = getKanteWithInitialStatesView(kante34_luecke, (short) 1, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View, kante34View));
		knotenToKantenViewMap.put(knoten4.getId(), Lists.newArrayList(kante34View));

		HashMap<Long, List<Kante>> knotenToAlLKantenMap = new HashMap<>();
		knotenToAlLKantenMap.put(knoten1.getId(), List.of(kante12));
		knotenToAlLKantenMap.put(knoten2.getId(), List.of(kante12, kante23_luecke));
		knotenToAlLKantenMap.put(knoten3.getId(), List.of(kante23_luecke, kante34_luecke));
		knotenToAlLKantenMap.put(knoten4.getId(), List.of(kante34_luecke));

		AttributlueckenSchliessenJobStatistik statistik = new AttributlueckenSchliessenJobStatistik();

		// Act
		ArrayList<Attributluecke> attributluecken = attributlueckenService.ermittleLuecken(knotenToKantenViewMap,
			knotenToAlLKantenMap, statistik);

		// Assert
		assertThat(attributluecken).isEmpty();

		assertThat(attributSchliessenProblemCaptor.getAllValues()).hasSize(2);
		assertThat(statistik.anzahlPotentielleLueckenIgnoriertDaSackgasse).isEqualTo(1);
		assertThat(statistik.anzahlPotentielleLueckenIgnoriertSonstigerGrund).isEqualTo(1);
	}

	@Test
	public void testErmittleLuecken_mehrereKantenZwischenGleichenKnoten() {
		// Es kommt bei z.B. Verkehrsinseln schon mal vor, dass sich eine Straße aufspaltet und so zwischen zwei Knoten
		// zwei Kanten verlaufen. Beide Kanten (also z.B. rechts und links der Verkehrsinsel) können also eine Lücke
		// darstellen und sollen entsprechend erkannt werden. Heißt dann aber auch: Das Ergebnis ist nicht eindeutig,
		// es sollte hier keine Lücke ermittelt werden können.

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2).build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3).build();
		Kante kante32_luecke = buildLueckenKante(32L, knoten3, knoten2).build();
		Kante kante34 = buildNormaleKante(34L, knoten3, knoten4).build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 3, (short) 3);
		KanteWithInitialStatesView kante32View = getKanteWithInitialStatesView(kante32_luecke, (short) 3, (short) 3);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View, kante32View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View, kante32View));

		HashMap<Long, List<Kante>> knotenToAlLKantenMap = new HashMap<>();
		knotenToAlLKantenMap.put(knoten1.getId(), List.of(kante12));
		knotenToAlLKantenMap.put(knoten2.getId(), List.of(kante12, kante23_luecke, kante32_luecke));
		knotenToAlLKantenMap.put(knoten3.getId(), List.of(kante34, kante23_luecke, kante32_luecke));
		knotenToAlLKantenMap.put(knoten4.getId(), List.of(kante34));

		AttributlueckenSchliessenJobStatistik statistik = new AttributlueckenSchliessenJobStatistik();

		// Act
		ArrayList<Attributluecke> attributluecken = attributlueckenService.ermittleLuecken(knotenToKantenViewMap,
			knotenToAlLKantenMap, statistik);

		// Assert
		assertThat(attributluecken).isEmpty();

		assertThat(attributSchliessenProblemCaptor.getAllValues()).hasSize(2);
		assertThat(statistik.anzahlLueckenIgnoriertDaMehrdeutig).isEqualTo(2);
	}

	@Test
	public void testErmittleLuecken_lueckeMitGleichemStartUndEndKnoten() {
		// Lücken, die zurück zum Start-Knoten führen, wollen wir nicht finden. Das ist z.B. bei Wendekreisen der Fall.
		// Bei Solchen Lücken ist die Übernahme von Attributen, die sich auf eine Seite oder Fahrtrichtung beziehen,
		// nicht möglich, weswegen wir diese Art von lücken ignorieren. Auch bei Kreuzungen oder parallelen Straßen mit
		// Einmündungen könnte es passieren, dass wir einen Kreis eher finden (weil kürzer) als eine tatsächliche
		// lineare Lücke auf einer Straße.
		// Dieser Fall ist ein Spezialfall des allgemeinen "Lücke ist nicht eindeutig ermittelbar"-Falls (s. z.B. andere
		// Tests dazu).

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2).build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3).build();
		Kante kante32_luecke = buildLueckenKante(32L, knoten3, knoten2).build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 3, (short) 2);
		KanteWithInitialStatesView kante32View = getKanteWithInitialStatesView(kante32_luecke, (short) 2, (short) 3);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View, kante32View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View, kante32View));

		HashMap<Long, List<Kante>> knotenToAlLKantenMap = new HashMap<>();
		knotenToAlLKantenMap.put(knoten1.getId(), List.of(kante12));
		knotenToAlLKantenMap.put(knoten2.getId(), List.of(kante12, kante23_luecke, kante32_luecke));
		knotenToAlLKantenMap.put(knoten3.getId(), List.of(kante23_luecke, kante32_luecke));

		AttributlueckenSchliessenJobStatistik statistik = new AttributlueckenSchliessenJobStatistik();

		// Act
		ArrayList<Attributluecke> attributluecken = attributlueckenService.ermittleLuecken(knotenToKantenViewMap,
			knotenToAlLKantenMap, statistik);

		// Assert
		assertThat(attributluecken).isEmpty();

		assertThat(attributSchliessenProblemCaptor.getAllValues()).hasSize(1);
		assertThat(statistik.anzahlPotentielleLueckenIgnoriertSonstigerGrund).isEqualTo(1);
	}

	@Test
	public void testErmittleLuecken_lueckeMitGleicherStartUndEndKante() {
		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2).build();
		Kante kante21_luecke = buildLueckenKante(21L, knoten2, knoten1).build();

		KanteWithInitialStatesView kante21View = getKanteWithInitialStatesView(kante21_luecke, (short) 2, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten1.getId(), Lists.newArrayList(kante21View));
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante21View));

		HashMap<Long, List<Kante>> knotenToAlLKantenMap = new HashMap<>();
		knotenToAlLKantenMap.put(knoten1.getId(), List.of(kante12, kante21_luecke));
		knotenToAlLKantenMap.put(knoten2.getId(), List.of(kante12, kante21_luecke));

		AttributlueckenSchliessenJobStatistik statistik = new AttributlueckenSchliessenJobStatistik();

		// Act
		ArrayList<Attributluecke> attributluecken = attributlueckenService.ermittleLuecken(knotenToKantenViewMap,
			knotenToAlLKantenMap, statistik);

		// Assert
		assertThat(attributluecken).isEmpty();

		assertThat(attributSchliessenProblemCaptor.getAllValues()).hasSize(1);
		assertThat(statistik.anzahlLueckenIgnoriertDaGleicheKanteVerbunden).isEqualTo(1);
	}

	@Test
	public void testSchliesseLuecken_einfacheLuecke() {
		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		Gebietskoerperschaft gebietskoerperschaft = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Foostadt")
			.build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.ASPHALT)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.ASPHALT)
							.build()))
					.build())
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppe.builder()
					.isZweiseitig(false)
					.fahrtrichtungLinks(Richtung.IN_RICHTUNG)
					.fahrtrichtungRechts(Richtung.IN_RICHTUNG)
					.build())
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppe.builder()
					.zustaendigkeitAttribute(List.of(
						ZustaendigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
							.vereinbarungsKennung(VereinbarungsKennung.of("Vereinbaru"))
							.erhaltsZustaendiger(gebietskoerperschaft)
							.unterhaltsZustaendiger(gebietskoerperschaft)
							.baulastTraeger(gebietskoerperschaft)
							.build()))
					.build())
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder()
					.geschwindigkeitAttribute(List.of(
						GeschwindigkeitAttribute.builder()
							.ortslage(KantenOrtslage.INNERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH)
							.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
								Hoechstgeschwindigkeit.MAX_30_KMH)
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
							.build()))
					.build())
			.build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3).build();
		Kante kante34 = buildNormaleKante(34L, knoten3, knoten4)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.ASPHALT)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.ASPHALT)
							.build()))
					.build())
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppe.builder()
					.isZweiseitig(false)
					.fahrtrichtungLinks(Richtung.IN_RICHTUNG)
					.fahrtrichtungRechts(Richtung.IN_RICHTUNG)
					.build())
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppe.builder()
					.zustaendigkeitAttribute(List.of(
						ZustaendigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
							.vereinbarungsKennung(VereinbarungsKennung.of("Vereinbaru"))
							.erhaltsZustaendiger(gebietskoerperschaft)
							.unterhaltsZustaendiger(gebietskoerperschaft)
							.baulastTraeger(gebietskoerperschaft)
							.build()))
					.build())
			.geschwindigkeitAttributGruppe(
				GeschwindigkeitAttributGruppe.builder()
					.geschwindigkeitAttribute(List.of(
						GeschwindigkeitAttribute.builder()
							.ortslage(KantenOrtslage.INNERORTS)
							.hoechstgeschwindigkeit(Hoechstgeschwindigkeit.MAX_50_KMH)
							.abweichendeHoechstgeschwindigkeitGegenStationierungsrichtung(
								Hoechstgeschwindigkeit.MAX_30_KMH)
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
							.build()))
					.build())
			.build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 2, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View));

		Attributluecke attributluecke = new Attributluecke(
			knoten2,
			knoten3,
			List.of(kante23_luecke),
			List.of(kante12),
			List.of(kante34));

		// Act
		attributlueckenService.schliesseLuecken(List.of(attributluecke), knotenToKantenViewMap);

		// Assert
		assertThat(kante23_luecke.isZweiseitig()).isFalse();
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante23_luecke.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();

		assertThat(kante23_luecke.getKantenAttributGruppe()).isEqualTo(kante12.getKantenAttributGruppe());
		assertThat(kante23_luecke.getKantenAttributGruppe()).isEqualTo(kante34.getKantenAttributGruppe());

		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.isEqualTo(kante12.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts());
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.isEqualTo(kante12.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());

		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.isEqualTo(kante34.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts());
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.isEqualTo(kante34.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());

		assertThat(kante23_luecke.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts())
			.isEqualTo(Richtung.IN_RICHTUNG);
		assertThat(kante23_luecke.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks())
			.isEqualTo(Richtung.IN_RICHTUNG);

		ZustaendigkeitAttribute zustaendigkeitAttribute12 = kante12.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().get(0);
		ZustaendigkeitAttribute zustaendigkeitAttribute23 = kante23_luecke.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().get(0);
		assertThat(zustaendigkeitAttribute23.getVereinbarungsKennung()).isEqualTo(zustaendigkeitAttribute12
			.getVereinbarungsKennung());
		assertThat(zustaendigkeitAttribute23.getBaulastTraeger()).isEqualTo(zustaendigkeitAttribute12
			.getBaulastTraeger());
		assertThat(zustaendigkeitAttribute23.getErhaltsZustaendiger()).isEqualTo(zustaendigkeitAttribute12
			.getErhaltsZustaendiger());
		assertThat(zustaendigkeitAttribute23.getUnterhaltsZustaendiger()).isEqualTo(zustaendigkeitAttribute12
			.getUnterhaltsZustaendiger());
		assertThat(zustaendigkeitAttribute23.getLinearReferenzierterAbschnitt()).isEqualTo(zustaendigkeitAttribute12
			.getLinearReferenzierterAbschnitt());

		GeschwindigkeitAttribute geschwindigkeitAttribute12 = kante12.getGeschwindigkeitAttributGruppe()
			.getImmutableGeschwindigkeitAttribute().get(0);
		GeschwindigkeitAttribute geschwindigkeitAttribute23 = kante23_luecke.getGeschwindigkeitAttributGruppe()
			.getImmutableGeschwindigkeitAttribute().get(0);
		assertThat(geschwindigkeitAttribute23.getOrtslage()).isEqualTo(geschwindigkeitAttribute12.getOrtslage());
		assertThat(geschwindigkeitAttribute23.getHoechstgeschwindigkeit()).isEqualTo(geschwindigkeitAttribute12
			.getHoechstgeschwindigkeit());
		assertThat(geschwindigkeitAttribute23.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung())
			.isEqualTo(geschwindigkeitAttribute12.getAbweichendeHoechstgeschwindigkeitGegenStationierungsrichtung());
		assertThat(geschwindigkeitAttribute23.getLinearReferenzierterAbschnitt()).isEqualTo(geschwindigkeitAttribute12
			.getLinearReferenzierterAbschnitt());
	}

	@Test
	public void testSchliesseLuecken_einfacheLuecke_ungueltigeTrennstreifenKombination() {
		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		FuehrungsformAttribute.FuehrungsformAttributeBuilder fuehrungsformAttributeBuilder = FuehrungsformAttribute
			.builder()
			.belagArt(BelagArt.ASPHALT)
			.radverkehrsfuehrung(Radverkehrsfuehrung.GEH_RADWEG_GEMEINSAM_STRASSENBEGLEITEND)
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR)
			.trennstreifenBreiteLinks(Laenge.of(1.23))
			.trennstreifenFormRechts(TrennstreifenForm.TRENNUNG_DURCH_GRUENSTREIFEN)
			.trennstreifenTrennungZuRechts(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUR_FAHRBAHN)
			.trennstreifenBreiteRechts(Laenge.of(1.23));

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.fuehrungsformAttributeLinks(List.of(fuehrungsformAttributeBuilder.build()))
					.fuehrungsformAttributeRechts(List.of(fuehrungsformAttributeBuilder.build()))
					.build())
			.build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3).build();
		Kante kante34 = buildNormaleKante(34L, knoten3, knoten4)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.fuehrungsformAttributeLinks(List.of(
						fuehrungsformAttributeBuilder
							.trennstreifenBreiteLinks(Laenge.of(2.34)) // <-- Unterschied zu kante12
							.trennstreifenBreiteRechts(Laenge.of(2.34)) // <-- Unterschied zu kante12
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						fuehrungsformAttributeBuilder
							.trennstreifenBreiteLinks(Laenge.of(2.34)) // <-- Unterschied zu kante12
							.trennstreifenBreiteRechts(Laenge.of(2.34)) // <-- Unterschied zu kante12
							.build()))
					.build())
			.build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 2, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View));

		Attributluecke attributluecke = new Attributluecke(
			knoten2,
			knoten3,
			List.of(kante23_luecke),
			List.of(kante12),
			List.of(kante34));

		// Act
		attributlueckenService.schliesseLuecken(List.of(attributluecke), knotenToKantenViewMap);

		// Assert
		assertThat(kante23_luecke.isZweiseitig()).isFalse();
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante23_luecke.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();

		FuehrungsformAttribute fuehrungsformLinks = kante23_luecke.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks().get(0);
		assertThat(fuehrungsformLinks.getTrennstreifenFormLinks()).isEmpty();
		assertThat(fuehrungsformLinks.getTrennstreifenTrennungZuLinks()).isEmpty();
		assertThat(fuehrungsformLinks.getTrennstreifenBreiteLinks()).isEmpty();
		assertThat(fuehrungsformLinks.getTrennstreifenFormRechts()).isEmpty();
		assertThat(fuehrungsformLinks.getTrennstreifenTrennungZuRechts()).isEmpty();
		assertThat(fuehrungsformLinks.getTrennstreifenBreiteRechts()).isEmpty();

		FuehrungsformAttribute fuehrungsformRechts = kante23_luecke.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeRechts().get(0);
		assertThat(fuehrungsformRechts.getTrennstreifenFormLinks()).isEmpty();
		assertThat(fuehrungsformRechts.getTrennstreifenTrennungZuLinks()).isEmpty();
		assertThat(fuehrungsformRechts.getTrennstreifenBreiteLinks()).isEmpty();
		assertThat(fuehrungsformRechts.getTrennstreifenFormRechts()).isEmpty();
		assertThat(fuehrungsformRechts.getTrennstreifenTrennungZuRechts()).isEmpty();
		assertThat(fuehrungsformRechts.getTrennstreifenBreiteRechts()).isEmpty();
	}

	@Test
	public void testSchliesseLuecken_einfacheLuecke_lineareReferenzen() {
		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		Gebietskoerperschaft gebietskoerperschaftFoo = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Foostadt")
			.build();
		Gebietskoerperschaft gebietskoerperschaftBar = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Bardorf")
			.build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.9, 1.0))
							.belagArt(BelagArt.ASPHALT)
							.build(),
						FuehrungsformAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 0.9))
							.belagArt(BelagArt.NATURSTEINPFLASTER)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.9, 1.0))
							.belagArt(BelagArt.ASPHALT)
							.build(),
						FuehrungsformAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 0.9))
							.belagArt(BelagArt.NATURSTEINPFLASTER)
							.build()))
					.build())
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppe.builder()
					.zustaendigkeitAttribute(List.of(
						ZustaendigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
							.vereinbarungsKennung(VereinbarungsKennung.of("Vereinbaru"))
							.erhaltsZustaendiger(gebietskoerperschaftFoo)
							.unterhaltsZustaendiger(gebietskoerperschaftFoo)
							.baulastTraeger(gebietskoerperschaftFoo)
							.build(),
						ZustaendigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
							.vereinbarungsKennung(VereinbarungsKennung.of("Vereinbaruuuu"))
							.erhaltsZustaendiger(gebietskoerperschaftBar)
							.unterhaltsZustaendiger(gebietskoerperschaftBar)
							.baulastTraeger(gebietskoerperschaftBar)
							.build()))
					.build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().build())
			.build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3).build();
		Kante kante34 = buildNormaleKante(34L, knoten3, knoten4)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 0.1))
							.belagArt(BelagArt.ASPHALT)
							.build(),
						FuehrungsformAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.1, 1.0))
							.belagArt(BelagArt.NATURSTEINPFLASTER)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 0.1))
							.belagArt(BelagArt.ASPHALT)
							.build(),
						FuehrungsformAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.1, 1.0))
							.belagArt(BelagArt.NATURSTEINPFLASTER)
							.build()))
					.build())
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppe.builder()
					.zustaendigkeitAttribute(List.of(
						ZustaendigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5))
							.vereinbarungsKennung(VereinbarungsKennung.of("Vereinbaruuuu"))
							.erhaltsZustaendiger(gebietskoerperschaftBar)
							.unterhaltsZustaendiger(gebietskoerperschaftBar)
							.baulastTraeger(gebietskoerperschaftBar)
							.build(),
						ZustaendigkeitAttribute.builder()
							.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
							.vereinbarungsKennung(VereinbarungsKennung.of("Vereinbaru"))
							.erhaltsZustaendiger(gebietskoerperschaftFoo)
							.unterhaltsZustaendiger(gebietskoerperschaftFoo)
							.baulastTraeger(gebietskoerperschaftFoo)
							.build()))
					.build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().build())
			.build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 2, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View));

		Attributluecke attributluecke = new Attributluecke(
			knoten2,
			knoten3,
			List.of(kante23_luecke),
			List.of(kante12),
			List.of(kante34));

		// Act
		attributlueckenService.schliesseLuecken(List.of(attributluecke), knotenToKantenViewMap);

		// Assert
		assertThat(kante23_luecke.isZweiseitig()).isFalse();
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().isZweiseitig()).isFalse();
		assertThat(kante23_luecke.getFahrtrichtungAttributGruppe().isZweiseitig()).isFalse();

		assertThat(kante23_luecke.getKantenAttributGruppe()).isEqualTo(kante12.getKantenAttributGruppe());
		assertThat(kante23_luecke.getKantenAttributGruppe()).isEqualTo(kante34.getKantenAttributGruppe());

		FuehrungsformAttribute expectedFuehrungsformAttribute = FuehrungsformAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.0, 1.0))
			.belagArt(BelagArt.ASPHALT)
			.build();
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.containsExactly(expectedFuehrungsformAttribute);
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.containsExactly(expectedFuehrungsformAttribute);

		ZustaendigkeitAttribute expectedZustaendigkeit = ZustaendigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.vereinbarungsKennung(VereinbarungsKennung.of("Vereinbaruuuu"))
			.erhaltsZustaendiger(gebietskoerperschaftBar)
			.unterhaltsZustaendiger(gebietskoerperschaftBar)
			.baulastTraeger(gebietskoerperschaftBar)
			.build();
		ZustaendigkeitAttribute zustaendigkeitAttribute23 = kante23_luecke.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().get(0);
		assertThat(zustaendigkeitAttribute23.getVereinbarungsKennung()).isEqualTo(expectedZustaendigkeit
			.getVereinbarungsKennung());
		assertThat(zustaendigkeitAttribute23.getBaulastTraeger()).isEqualTo(expectedZustaendigkeit.getBaulastTraeger());
		assertThat(zustaendigkeitAttribute23.getErhaltsZustaendiger()).isEqualTo(expectedZustaendigkeit
			.getErhaltsZustaendiger());
		assertThat(zustaendigkeitAttribute23.getUnterhaltsZustaendiger()).isEqualTo(expectedZustaendigkeit
			.getUnterhaltsZustaendiger());
		assertThat(zustaendigkeitAttribute23.getLinearReferenzierterAbschnitt()).isEqualTo(expectedZustaendigkeit
			.getLinearReferenzierterAbschnitt());
	}

	@Test
	public void testSchliesseLuecken_einfacheLuecke_zuIgnorierendeAttributeWerdenIgnoriert() {
		// Es sollen nicht alle Attribute der allgemeinen Attributgruppe übernommen werden.

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		Verwaltungseinheit verwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.build();
		Verwaltungseinheit andereVerwaltungseinheit = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Verwaltungseinheit der Lücke")
			.build();
		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2)
			.dlmId(DlmId.of("12"))
			.kantenAttributGruppe(
				KantenAttributGruppe.builder()
					.kantenAttribute(
						KantenAttribute.builder()
							// Sollen nicht übernommen werden:
							.status(Status.IN_BAU)
							.strassenName(StrassenName.of("Test-Straße"))
							.strassenNummer(StrassenNummer.of("B1234"))
							.kommentar(Kommentar.of("Kommentar-1234"))
							.laengeManuellErfasst(Laenge.of(1234))
							.gemeinde(verwaltungseinheit)
							// Sollen übernommen werden:
							.dtvPkw(VerkehrStaerke.of(123))
							.dtvRadverkehr(VerkehrStaerke.of(234))
							.dtvFussverkehr(VerkehrStaerke.of(345))
							.beleuchtung(Beleuchtung.VORHANDEN)
							.sv(VerkehrStaerke.of(456))
							.strassenquerschnittRASt06(StrassenquerschnittRASt06.HAUPTGESCHAEFTSSTRASSE)
							.wegeNiveau(WegeNiveau.FAHRBAHN)
							.strassenkategorieRIN(StrassenkategorieRIN.NAHRAEUMIG)
							.umfeld(Umfeld.GEWERBEGEBIET)
							.build())
					.build())
			.build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3)
			.dlmId(DlmId.of("23"))
			.kantenAttributGruppe(
				KantenAttributGruppe.builder()
					.kantenAttribute(
						KantenAttribute.builder()
							// Sollen nicht überschrieben werden:
							.status(Status.UNTER_VERKEHR)
							.strassenName(StrassenName.of("Lücken-Straße"))
							.strassenNummer(StrassenNummer.of("B23"))
							.kommentar(Kommentar.of("Kommentar-23"))
							.laengeManuellErfasst(Laenge.of(23))
							.gemeinde(andereVerwaltungseinheit)
							// Sollen überschrieben werden:
							.dtvPkw(VerkehrStaerke.of(999))
							.dtvRadverkehr(VerkehrStaerke.of(999))
							.dtvFussverkehr(VerkehrStaerke.of(999))
							.beleuchtung(Beleuchtung.NICHT_VORHANDEN)
							.sv(VerkehrStaerke.of(999))
							.strassenquerschnittRASt06(StrassenquerschnittRASt06.ANBAUFREIE_STRASSE)
							.wegeNiveau(WegeNiveau.GEHWEG)
							.strassenkategorieRIN(StrassenkategorieRIN.GROSSRAEUMIG)
							.umfeld(Umfeld.STRASSE_MIT_GERINGER_BIS_MITTLERER_WOHNDICHTE)
							.build())
					.build())
			.build();
		Kante kante34 = buildNormaleKante(34L, knoten3, knoten4)
			.dlmId(DlmId.of("34"))
			.kantenAttributGruppe(
				KantenAttributGruppe.builder()
					.kantenAttribute(
						kante12.getKantenAttributGruppe().getKantenAttribute().toBuilder().build() // Identisch zu
																																																													 // kante12
					)
					.build())
			.build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 2, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View));

		Attributluecke attributluecke = new Attributluecke(
			knoten2,
			knoten3,
			List.of(kante23_luecke),
			List.of(kante12),
			List.of(kante34));

		// Act
		attributlueckenService.schliesseLuecken(List.of(attributluecke), knotenToKantenViewMap);

		// Assert
		KantenAttribute expectedKantenAttribute = kante12.getKantenAttributGruppe()
			.getKantenAttribute()
			.toBuilder()
			// Folgende Attribute der kante23_luecke sollen beibehalten werden:
			.status(kante23_luecke.getKantenAttributGruppe().getKantenAttribute().getStatus())
			.strassenName(kante23_luecke.getKantenAttributGruppe().getKantenAttribute().getStrassenName().get())
			.strassenNummer(kante23_luecke.getKantenAttributGruppe().getKantenAttribute().getStrassenNummer().get())
			.kommentar(kante23_luecke.getKantenAttributGruppe().getKantenAttribute().getKommentar().get())
			.laengeManuellErfasst(kante23_luecke.getKantenAttributGruppe().getKantenAttribute()
				.getLaengeManuellErfasst().get())
			.gemeinde(kante23_luecke.getKantenAttributGruppe().getKantenAttribute().getGemeinde().get())
			.build();

		assertThat(kante23_luecke.getKantenAttributGruppe().getKantenAttribute()).isEqualTo(expectedKantenAttribute);
	}

	@Test
	public void testSchliesseLuecken_einfacheLuecke_eineKanteZweiseitig() {
		// Eine Kante, von der Attribute übernommen werden sollen, ist zweiseitig. Dadurch sollen alle Lücken-Kanten
		// ebenfalls zweiseitig werden.

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.ASPHALT)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.ASPHALT)
							.build()))
					.build())
			.build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3).build();
		Kante kante34 = buildNormaleKante(34L, knoten3, knoten4)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.ASPHALT)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.BETON) // <- Anders als bei kante12
							.build()))
					.build())
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 2, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View));

		Attributluecke attributluecke = new Attributluecke(
			knoten2,
			knoten3,
			List.of(kante23_luecke),
			List.of(kante12),
			List.of(kante34));

		// Act
		attributlueckenService.schliesseLuecken(List.of(attributluecke), knotenToKantenViewMap);

		// Assert
		assertThat(kante23_luecke.isZweiseitig()).isTrue();
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante23_luecke.getFahrtrichtungAttributGruppe().isZweiseitig()).isTrue();

		assertThat(kante23_luecke.getKantenAttributGruppe()).isEqualTo(kante12.getKantenAttributGruppe());
		assertThat(kante23_luecke.getKantenAttributGruppe()).isEqualTo(kante34.getKantenAttributGruppe());

		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0))
			.isEqualTo(
				kante12.getFuehrungsformAttributGruppe()
					.getImmutableFuehrungsformAttributeRechts()
					.get(0)
					.toBuilder()
					.belagArt(BelagArt.UNBEKANNT)
					.build());
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.isEqualTo(kante12.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());

		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0))
			.isEqualTo(
				kante34.getFuehrungsformAttributGruppe()
					.getImmutableFuehrungsformAttributeRechts()
					.get(0)
					.toBuilder()
					.belagArt(BelagArt.UNBEKANNT)
					.build());
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.isEqualTo(kante34.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());
	}

	@Test
	public void testSchliesseLuecken_einfacheLuecke_kantenMitAttributenEntgegengesetzt() {
		// Die beiden Kanten am Anfang und Ende der Lücke, von denen Attribute übernommen werden sollen, haben entgegen-
		// gesetzte Stationierungsrichtungen. Die rechte Seite der einen ist also die linke Seite der anderen Kante, was
		// korrekt berücksichtigt werden soll.

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.NATURSTEINPFLASTER) // <- Matcht auf "BETON" von kante43
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.ASPHALT) // <- Matcht auf "ASPHALT" von kante43
							.build()))
					.build())
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppe.builder()
					.isZweiseitig(true)
					.fahrtrichtungLinks(Richtung.IN_RICHTUNG)
					.fahrtrichtungRechts(Richtung.IN_RICHTUNG)
					.build())
			.build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3).build();
		Kante kante43 = buildNormaleKante(43L, knoten4, knoten3) // <- Umgekehrte Stationierungsrichtung
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.ASPHALT)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.BETON)
							.build()))
					.build())
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppe.builder()
					.isZweiseitig(true)
					.fahrtrichtungLinks(Richtung.GEGEN_RICHTUNG)
					.fahrtrichtungRechts(Richtung.GEGEN_RICHTUNG)
					.build())
			.build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 2, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View));

		Attributluecke attributluecke = new Attributluecke(
			knoten2,
			knoten3,
			List.of(kante23_luecke),
			List.of(kante12),
			List.of(kante43));

		// Act
		attributlueckenService.schliesseLuecken(List.of(attributluecke), knotenToKantenViewMap);

		// Assert
		assertThat(kante23_luecke.isZweiseitig()).isTrue();
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante23_luecke.getFahrtrichtungAttributGruppe().isZweiseitig()).isTrue();

		assertThat(kante23_luecke.getKantenAttributGruppe()).isEqualTo(kante12.getKantenAttributGruppe());
		assertThat(kante23_luecke.getKantenAttributGruppe()).isEqualTo(kante43.getKantenAttributGruppe());

		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.isEqualTo(kante12.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts());
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0))
			.isEqualTo(
				kante12.getFuehrungsformAttributGruppe()
					.getImmutableFuehrungsformAttributeLinks()
					.get(0)
					.toBuilder()
					.belagArt(BelagArt.UNBEKANNT)
					.build());

		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.isEqualTo(kante43.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks());
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0))
			.isEqualTo(
				kante43.getFuehrungsformAttributGruppe()
					.getImmutableFuehrungsformAttributeRechts()
					.get(0)
					.toBuilder()
					.belagArt(BelagArt.UNBEKANNT)
					.build());

		assertThat(kante23_luecke.getFahrtrichtungAttributGruppe().getFahrtrichtungRechts())
			.isEqualTo(Richtung.IN_RICHTUNG);
		assertThat(kante23_luecke.getFahrtrichtungAttributGruppe().getFahrtrichtungLinks())
			.isEqualTo(Richtung.IN_RICHTUNG);
	}

	@Test
	public void testSchliesseLuecken_entgegengesetzteKanteInLuecke() {
		// Die Lücke besteht aus zwei Kanten, deren Stationierungsrichtungen entgegengesetzt sind. Die Übernahme von
		// Attributen soll dabei diese unterschiedlichen Richtungen korrekt betrachten.

		// Arrange
		Knoten knoten1 = KnotenTestDataProvider.withPosition(10, 10).id(1L).build();
		Knoten knoten2 = KnotenTestDataProvider.withPosition(20, 10).id(2L).build();
		Knoten knoten3 = KnotenTestDataProvider.withPosition(30, 10).id(3L).build();
		Knoten knoten4 = KnotenTestDataProvider.withPosition(40, 10).id(4L).build();
		Knoten knoten5 = KnotenTestDataProvider.withPosition(50, 10).id(5L).build();

		Kante kante12 = buildNormaleKante(12L, knoten1, knoten2)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.NATURSTEINPFLASTER) // <- Matcht auf "BETON" von kante45
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.ASPHALT) // <- Matcht auf "ASPHALT" von kante45
							.build()))
					.build())
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.build();
		Kante kante23_luecke = buildLueckenKante(23L, knoten2, knoten3).build(); // <- In Lücken-Richtung
		Kante kante43_luecke = buildLueckenKante(43L, knoten4, knoten3).build(); // <- Gegen Lücken-Richtung
		Kante kante45 = buildNormaleKante(45L, knoten4, knoten5)
			.isZweiseitig(true)
			.fuehrungsformAttributGruppe(
				FuehrungsformAttributGruppe.builder()
					.isZweiseitig(true)
					.fuehrungsformAttributeLinks(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.BETON)
							.build()))
					.fuehrungsformAttributeRechts(List.of(
						FuehrungsformAttribute.builder()
							.belagArt(BelagArt.ASPHALT)
							.build()))
					.build())
			.fahrtrichtungAttributGruppe(
				FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.build();

		KanteWithInitialStatesView kante23View = getKanteWithInitialStatesView(kante23_luecke, (short) 2, (short) 2);
		KanteWithInitialStatesView kante43View = getKanteWithInitialStatesView(kante43_luecke, (short) 2, (short) 2);
		HashMap<Long, List<KanteWithInitialStatesView>> knotenToKantenViewMap = new HashMap<>();
		knotenToKantenViewMap.put(knoten2.getId(), Lists.newArrayList(kante23View));
		knotenToKantenViewMap.put(knoten3.getId(), Lists.newArrayList(kante23View, kante43View));
		knotenToKantenViewMap.put(knoten4.getId(), Lists.newArrayList(kante43View));

		Attributluecke attributluecke = new Attributluecke(
			knoten2,
			knoten4,
			List.of(kante23_luecke, kante43_luecke),
			List.of(kante12),
			List.of(kante45));

		// Act
		attributlueckenService.schliesseLuecken(List.of(attributluecke), knotenToKantenViewMap);

		// Assert
		assertThat(kante23_luecke.isZweiseitig()).isTrue();
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().isZweiseitig()).isTrue();
		assertThat(kante23_luecke.getFahrtrichtungAttributGruppe().isZweiseitig()).isTrue();

		assertThat(kante23_luecke.getKantenAttributGruppe()).isEqualTo(kante12.getKantenAttributGruppe());
		assertThat(kante23_luecke.getKantenAttributGruppe()).isEqualTo(kante45.getKantenAttributGruppe());

		// Rechte Seite von Kante 23 = rechte Seite von kante 12
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts())
			.isEqualTo(kante12.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts());
		assertThat(kante23_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0))
			.isEqualTo(
				kante12.getFuehrungsformAttributGruppe()
					.getImmutableFuehrungsformAttributeLinks()
					.get(0)
					.toBuilder()
					.belagArt(BelagArt.UNBEKANNT)
					.build());

		// Rechte Seite von Kante 43 = linke(!) Seite von kante 12, da Stationierungsrichtung von Kante 43 umgekehrt ist
		assertThat(kante43_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks())
			.isEqualTo(kante12.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts());
		assertThat(kante43_luecke.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0))
			.isEqualTo(
				kante12.getFuehrungsformAttributGruppe()
					.getImmutableFuehrungsformAttributeLinks()
					.get(0)
					.toBuilder()
					.belagArt(BelagArt.UNBEKANNT)
					.build());
	}

	private static Kante.KanteBuilder buildNormaleKante(long kanteId, Knoten vonKnoten, Knoten nachKnoten) {
		return KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten)
			.id(kanteId)
			.kantenAttributGruppe(
				KantenAttributGruppe.builder()
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
					.build());
	}

	private static Kante.KanteBuilder buildLueckenKante(long kanteId, Knoten vonKnoten, Knoten nachKnoten) {
		return KanteTestDataProvider.fromKnoten(vonKnoten, nachKnoten).id(kanteId);
	}

	private static @NotNull KanteWithInitialStatesView getKanteWithInitialStatesView(Kante kante, short vonKnotenGrad,
		short nachKnotenGrad) {
		return new KanteWithInitialStatesView(
			kante.getId(),
			kante.getKantenAttributGruppe().getKantenAttribute().getStatus(),
			kante.getVonKnoten().getId(),
			vonKnotenGrad,
			kante.getNachKnoten().getId(),
			nachKnotenGrad,
			kante.getKantenLaengeInCm(),
			kante.getGeometry());
	}
}