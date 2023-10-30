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

package de.wps.radvis.backend.konsistenz.regeln.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisherSensitiveTest;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.repository.ShapeFileRepository;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.fahrradroute.FahrradrouteConfiguration;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenTfisImportJob;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenTfisUpdateJob;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenToubizImportJob;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenVariantenTfisImportJob;
import de.wps.radvis.backend.fahrradroute.domain.FahrradroutenVariantenTfisUpdateJob;
import de.wps.radvis.backend.fahrradroute.domain.LandesradfernwegeTfisImportJob;
import de.wps.radvis.backend.fahrradroute.domain.LandesradfernwegeVariantenTfisImportJob;
import de.wps.radvis.backend.fahrradroute.schnittstelle.ToubizConfigurationProperties;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.wegweisendeBeschilderung.WegweisendeBeschilderungConfiguration;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.WegweisendeBeschilderungConfigurationProperties;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.WegweisendeBeschilderungTestDataProvider;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.entity.WegweisendeBeschilderung;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.repository.WegweisendeBeschilderungRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Getter;
import lombok.Setter;

@Tag("group7")
@ContextConfiguration(classes = {
	FahrradrouteConfiguration.class,
	WegweisendeBeschilderungConfiguration.class
})
@MockBeans({
	@MockBean(KonsistenzregelnConfigurationProperties.class),
	@MockBean(ToubizConfigurationProperties.class),
	@MockBean(ShapeFileRepository.class),
	@MockBean(FahrradroutenTfisImportJob.class),
	@MockBean(FahrradroutenTfisUpdateJob.class),
	@MockBean(LandesradfernwegeTfisImportJob.class),
	@MockBean(FahrradroutenVariantenTfisImportJob.class),
	@MockBean(FahrradroutenVariantenTfisUpdateJob.class),
	@MockBean(LandesradfernwegeVariantenTfisImportJob.class),
	@MockBean(FahrradroutenToubizImportJob.class),
	@MockBean(WegweisendeBeschilderungConfigurationProperties.class),
	@MockBean(OrganisationConfigurationProperties.class)
})
class BeschilderungRadNETZKonsistenzregelTestIT extends AbstractKonsistenzregelTestIT implements
	RadVisDomainEventPublisherSensitiveTest {

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private WegweisendeBeschilderungRepository wegweisendeBeschilderungRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	private BeschilderungRadNETZKonsistenzregel beschilderungRadNETZKonsistenzregel;
	private final int beschilderungMaxEntfernungVonRoute = 15;
	private Gebietskoerperschaft badenWuerttemberg;

	@BeforeEach
	void setUp() {
		beschilderungRadNETZKonsistenzregel = new BeschilderungRadNETZKonsistenzregel(entityManager,
			beschilderungMaxEntfernungVonRoute);

		badenWuerttemberg = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Baden-Württemberg")
				.organisationsArt(
					OrganisationsArt.BUNDESLAND)
				.build());
	}

	@Test
	void pruefen_NetzklasseRadNETZ_nichtZielnetz_DLMundRadVISKanten_Beruecksichtigt() {
		// Arrange
		List<Kante> kanten = StreamSupport.stream(kantenRepository.saveAll(List.of(
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM).build(),
			KanteTestDataProvider.withCoordinatesAndQuelle(100, 0, 200, 0, QuellSystem.DLM).build(),
			KanteTestDataProvider.withCoordinatesAndQuelle(200, 0, 300, 0, QuellSystem.RadVis).build()
		)).spliterator(), false).collect(Collectors.toList());

		// folgende Kanten werden berücksichtigt:
		kanten.get(0).getKantenAttributGruppe()
			.updateNetzklassen(new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)));
		kanten.get(1).getKantenAttributGruppe()
			.updateNetzklassen(new HashSet<>(Set.of(Netzklasse.RADNETZ_FREIZEIT)));
		// Zielnetz soll explizit nicht dabei sein
		kanten.get(2).getKantenAttributGruppe()
			.updateNetzklassen(new HashSet<>(Set.of(Netzklasse.RADNETZ_ZIELNETZ)));

		List<WegweisendeBeschilderung> zuWeitWeg = generateWBProKanteMitYAbstand(kanten,
			beschilderungMaxEntfernungVonRoute + 5);
		List<WegweisendeBeschilderung> nahGenug = generateWBProKanteMitYAbstand(kanten,
			beschilderungMaxEntfernungVonRoute - 5);

		// Act
		List<KonsistenzregelVerletzungsDetails> verletzungen = beschilderungRadNETZKonsistenzregel.pruefen();

		List<WegweisendeBeschilderung> verletztKonsistenzregel = new ArrayList<>();
		// Alle zu weit entfernt von den RADNETZ Kanten
		verletztKonsistenzregel.addAll(zuWeitWeg);
		// Beschilderungen, die eig. nah genug an Kanten sind, aber diese kanten sind kein RADNETZ -> auch zuWeitWeg
		verletztKonsistenzregel.addAll(nahGenug.subList(2, 3));

		assertThat(verletzungen)
			.extracting(KonsistenzregelVerletzungsDetails::getIdentity)
			.extracting(Long::parseLong)
			.containsExactlyInAnyOrderElementsOf(
				verletztKonsistenzregel.stream().map(AbstractEntity::getId).collect(Collectors.toList()));
	}

	@Test
	void pruefen_NetzklasseRadNETZ_andereKantenQuelle_Ignorieren() {
		// Arrange
		List<Kante> kanten = StreamSupport.stream(kantenRepository.saveAll(List.of(
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM).build(),
			KanteTestDataProvider.withCoordinatesAndQuelle(100, 0, 200, 0, QuellSystem.RadwegeDB).build(),
			KanteTestDataProvider.withCoordinatesAndQuelle(200, 0, 300, 0, QuellSystem.RadNETZ).build()
		)).spliterator(), false).collect(Collectors.toList());

		// folgende Kanten werden berücksichtigt:
		kanten.get(0).getKantenAttributGruppe()
			.updateNetzklassen(new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)));
		kanten.get(1).getKantenAttributGruppe()
			.updateNetzklassen(new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)));
		kanten.get(2).getKantenAttributGruppe()
			.updateNetzklassen(new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)));

		List<WegweisendeBeschilderung> zuWeitWeg = generateWBProKanteMitYAbstand(kanten,
			beschilderungMaxEntfernungVonRoute + 5);
		List<WegweisendeBeschilderung> nahGenug = generateWBProKanteMitYAbstand(kanten,
			beschilderungMaxEntfernungVonRoute - 5);

		// Act
		List<KonsistenzregelVerletzungsDetails> verletzungen = beschilderungRadNETZKonsistenzregel.pruefen();

		List<WegweisendeBeschilderung> verletztKonsistenzregel = new ArrayList<>();
		// Alle zu weit entfernt von den RADNETZ Kanten
		verletztKonsistenzregel.addAll(zuWeitWeg);
		// Beschilderungen, die eig. nah genug an Kanten sind, aber diese kanten sind kein RADNETZ -> auch zuWeitWeg
		verletztKonsistenzregel.addAll(nahGenug.subList(1, 3));

		assertThat(verletzungen)
			.extracting(KonsistenzregelVerletzungsDetails::getIdentity)
			.extracting(Long::parseLong)
			.containsExactlyInAnyOrderElementsOf(
				verletztKonsistenzregel.stream().map(AbstractEntity::getId).collect(Collectors.toList()));
	}

	@Test
	void pruefen_KommunalnetzUndLeereNetzklasse_ignoriert() {
		// Arrange
		List<Kante> kanten = StreamSupport.stream(kantenRepository.saveAll(List.of(
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM).build(),
			KanteTestDataProvider.withCoordinatesAndQuelle(100, 0, 200, 0, QuellSystem.DLM).build(),
			KanteTestDataProvider.withCoordinatesAndQuelle(200, 0, 300, 0, QuellSystem.DLM).build()
		)).spliterator(), false).collect(Collectors.toList());

		// folgende Kanten werden berücksichtigt:
		kanten.get(0).getKantenAttributGruppe()
			.updateNetzklassen(new HashSet<>(Set.of(Netzklasse.RADNETZ_ALLTAG)));
		kanten.get(1).getKantenAttributGruppe()
			.updateNetzklassen(new HashSet<>(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG)));
		kanten.get(2).getKantenAttributGruppe()
			.updateNetzklassen(new HashSet<>(Set.of()));

		List<WegweisendeBeschilderung> zuWeitWeg = generateWBProKanteMitYAbstand(kanten,
			beschilderungMaxEntfernungVonRoute + 5);
		List<WegweisendeBeschilderung> nahGenug = generateWBProKanteMitYAbstand(kanten,
			beschilderungMaxEntfernungVonRoute - 5);

		// Act
		List<KonsistenzregelVerletzungsDetails> verletzungen = beschilderungRadNETZKonsistenzregel.pruefen();

		List<WegweisendeBeschilderung> verletztKonsistenzregel = new ArrayList<>();
		// Alle zu weit entfernt von den RADNETZ Kanten
		verletztKonsistenzregel.addAll(zuWeitWeg);
		// Beschilderungen, die eig. nah genug an Kanten sind, aber diese kanten sind kein RADNETZ -> auch zuWeitWeg
		verletztKonsistenzregel.addAll(nahGenug.subList(1, 3));

		assertThat(verletzungen)
			.extracting(KonsistenzregelVerletzungsDetails::getIdentity)
			.extracting(Long::parseLong)
			.containsExactlyInAnyOrderElementsOf(
				verletztKonsistenzregel.stream().map(AbstractEntity::getId).collect(Collectors.toList()));
	}

	@Test
	void pruefen_mehrereNetzklassen() {
		// Arrange
		List<Kante> kanten = StreamSupport.stream(kantenRepository.saveAll(List.of(
			KanteTestDataProvider.withCoordinatesAndQuelle(0, 0, 100, 0, QuellSystem.DLM).build()
		)).spliterator(), false).collect(Collectors.toList());

		// folgende Kanten werden berücksichtigt:
		kanten.get(0).getKantenAttributGruppe().updateNetzklassen(
			new HashSet<>(Set.of(Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT)));

		List<WegweisendeBeschilderung> zuWeitWeg = generateWBProKanteMitYAbstand(kanten,
			beschilderungMaxEntfernungVonRoute + 5);
		List<WegweisendeBeschilderung> nahGenug = generateWBProKanteMitYAbstand(kanten,
			beschilderungMaxEntfernungVonRoute - 5);

		// Act
		List<KonsistenzregelVerletzungsDetails> verletzungen = beschilderungRadNETZKonsistenzregel.pruefen();

		assertThat(verletzungen)
			.extracting(KonsistenzregelVerletzungsDetails::getIdentity)
			.extracting(Long::parseLong)
			.containsExactlyInAnyOrderElementsOf(
				zuWeitWeg.stream().map(AbstractEntity::getId).collect(Collectors.toList()));
	}

	private List<WegweisendeBeschilderung> generateWBProKanteMitYAbstand(List<Kante> kanten, double abstand) {
		List<WegweisendeBeschilderung> wegweisendeBeschilderungs = new ArrayList<>();
		kanten.forEach(kante -> {
			Coordinate midPoint = LineStrings.getMidPoint(kante.getGeometry());
			wegweisendeBeschilderungs.add(wegweisendeBeschilderungRepository.save(
				WegweisendeBeschilderungTestDataProvider
					.withDefaultValuesGeometrieAndVerwaltungseinheit(
						new Coordinate(midPoint.getX(), midPoint.getY() + abstand),
						badenWuerttemberg)
					.build())
			);
		});
		return wegweisendeBeschilderungs;
	}

	@Getter
	@Setter
	MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;
}