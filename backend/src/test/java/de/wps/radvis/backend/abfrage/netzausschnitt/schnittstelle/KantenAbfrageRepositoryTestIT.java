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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.abfrage.netzausschnitt.AbfrageNetzausschnittConfiguration;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.KantenAbfrageRepository;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.AbstractKanteLinearReferenzierteAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.GeometrienVerlaufMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteFuehrungsformAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteGeschwindigkeitAttributeView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteNetzklasseMapView;
import de.wps.radvis.backend.abfrage.netzausschnitt.domain.entity.KanteZustaendigkeitAttributeView;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.integration.attributAbbildung.IntegrationAttributAbbildungConfiguration;
import de.wps.radvis.backend.integration.attributAbbildung.domain.KantenMappingRepository;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.KantenMapping;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.LineareReferenzProjektionsergebnis;
import de.wps.radvis.backend.integration.attributAbbildung.domain.entity.MappedKante;
import de.wps.radvis.backend.integration.radnetz.domain.RadNetzNetzbildungService;
import de.wps.radvis.backend.integration.radwegedb.domain.RadwegeDBNetzbildungService;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.pruefung.KonsistenzregelPruefungsConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.KonsistenzregelnConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.KonsistenzregelnConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.netzfehler.NetzfehlerConfiguration;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.quellimport.common.domain.ImportedFeaturePersistentRepository;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group3")
@ContextConfiguration(classes = { AbfrageNetzausschnittConfiguration.class, NetzConfiguration.class,
	OrganisationConfiguration.class, GeoConverterConfiguration.class, CommonConfiguration.class,
	KommentarConfiguration.class,
	NetzfehlerConfiguration.class, BenutzerConfiguration.class, IntegrationAttributAbbildungConfiguration.class,
	KonsistenzregelPruefungsConfiguration.class, KonsistenzregelnConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	DLMConfigurationProperties.class,
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	KonsistenzregelnConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
@ActiveProfiles(profiles = "test")
class KantenAbfrageRepositoryTestIT extends DBIntegrationTestIT {
	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();

	@MockBean
	private ImportedFeaturePersistentRepository importedFeaturePersistentRepository;
	@MockBean
	private RadNetzNetzbildungService radNetzNetzbildungService;
	@MockBean
	private RadwegeDBNetzbildungService radwegeDBNetzbildungService;

	@Autowired
	private KantenAbfrageRepository abfrageRepository;
	@Autowired
	private KantenRepository kantenRepository;
	@Autowired
	private KantenMappingRepository kantenMappingRepository;

	@PersistenceContext
	EntityManager entityManager;

	@Test
	void testgetKantenMapViewInBereich() {
		// Arrange
		Kante kanteInnerhalb = kantenRepository.save(
			kantenRepository.save(createKante(new Coordinate(1, 10), new Coordinate(2, 20), false, true)));
		Kante kanteTeilweiseAusserhalb = kantenRepository.save(
			createKante(new Coordinate(1, 10), new Coordinate(2, 40), true, true));
		// kanteKomplettAusserhalb
		kantenRepository.save(
			createKante(new Coordinate(31, 31), new Coordinate(40, 40), true, true));
		Kante radNetzNetzklasseInnerhalb = kantenRepository.save(
			KanteTestDataProvider.withDefaultValues().kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(
							Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ZIELNETZ))
						.build())
				.isGrundnetz(true)
				.geometry(GeometryTestdataProvider.createLineString(new Coordinate(5, 5), new Coordinate(10, 5)))
				.build());

		entityManager.flush();
		entityManager.clear();

		// Act
		Set<KanteMapView> result = abfrageRepository
			.getKantenMapViewInBereich(new Envelope(0, 30, 0, 30),
				Set.of(NetzklasseFilter.RADNETZ, NetzklasseFilter.NICHT_KLASSIFIZIERT));

		// Assert
		assertThat(result).hasSize(3);
		assertThat(result).extracting(AbstractEntity::getId)
			.containsExactlyInAnyOrder(kanteInnerhalb.getId(), kanteTeilweiseAusserhalb.getId(),
				radNetzNetzklasseInnerhalb.getId());
		assertThat(result).extracting(KanteMapView::getGeometrie)
			.containsExactlyInAnyOrder(kanteInnerhalb.getGeometry(),
				kanteTeilweiseAusserhalb.getGeometry(), radNetzNetzklasseInnerhalb.getGeometry());
		assertThat(result).extracting(KanteMapView::isZweiseitig)
			.containsExactlyInAnyOrder(kanteInnerhalb.isZweiseitig(),
				kanteTeilweiseAusserhalb.isZweiseitig(), radNetzNetzklasseInnerhalb.isZweiseitig());
	}

	@Test
	void getKantenMapViewInBereichDlm() {
		// Arrange

		Kante kanteDLM = createKante(new Coordinate(1, 10), new Coordinate(2, 20), false, false);
		Kante kanteDLMRadnetz = createKante(new Coordinate(2, 10), new Coordinate(3, 20), false, false);
		kanteDLMRadnetz.getKantenAttributGruppe().getNetzklassen().add(Netzklasse.RADNETZ_ALLTAG);
		Kante kanteRadNETZ = createKante(new Coordinate(3, 10), new Coordinate(4, 20), QuellSystem.RadNETZ,
			true, false);
		kantenRepository.save(kanteDLM);
		kantenRepository.save(kanteDLMRadnetz);
		kantenRepository.save(kanteRadNETZ);

		entityManager.flush();
		entityManager.clear();

		// Act
		Set<KanteNetzklasseMapView> result = abfrageRepository
			.getKantenMapViewInBereichDlm(new Envelope(0, 30, 0, 30));

		// Assert
		assertThat(result).isNotEmpty();
		assertThat(result).extracting(KanteMapView::getGeometrie)
			.containsExactlyInAnyOrder(kanteDLM.getGeometry(),
				kanteDLMRadnetz.getGeometry());
		assertThat(result).extracting(KanteMapView::isZweiseitig)
			.containsExactlyInAnyOrder(kanteDLM.isZweiseitig(),
				kanteDLMRadnetz.isZweiseitig());
	}

	@Test
	void getKantenMapViewInBereichDlmIstRadNETZZugeordnet() {
		// Arrange
		Kante kanteDLM = createKante(new Coordinate(1, 10), new Coordinate(2, 20), false, false);
		Kante kanteDLMRadnetz = createKante(new Coordinate(2, 10), new Coordinate(3, 20), false, false);
		kanteDLMRadnetz.getKantenAttributGruppe().getNetzklassen().add(Netzklasse.RADNETZ_ALLTAG);
		Kante kanteRadNETZ = createKante(new Coordinate(3, 10), new Coordinate(4, 20), QuellSystem.RadNETZ,
			true, false);
		Kante gespeicherteKanteDLM = kantenRepository.save(kanteDLM);
		Kante gespeicherteDLMRadNETZKante = kantenRepository.save(kanteDLMRadnetz);
		Kante gespeicherteRadNETZKante = kantenRepository.save(kanteRadNETZ);

		LineareReferenzProjektionsergebnis lineareReferenzProjektionsergebnis = new LineareReferenzProjektionsergebnis(
			null, null, null, false);
		MappedKante mappedKante = new MappedKante(lineareReferenzProjektionsergebnis,
			lineareReferenzProjektionsergebnis, gespeicherteRadNETZKante.getId());

		KantenMapping kantenMapping = new KantenMapping(gespeicherteDLMRadNETZKante.getId(), QuellSystem.RadNETZ,
			List.of(mappedKante));

		kantenMappingRepository.save(kantenMapping);

		entityManager.flush();
		entityManager.clear();

		// Act
		Set<KanteNetzklasseMapView> result = abfrageRepository
			.getKantenMapViewInBereichDlmIstRadNETZZugeordnet(new Envelope(0, 30, 0, 30));

		// Assert
		assertThat(result).isNotEmpty();
		assertThat(result).extracting(KanteMapView::getId)
			.containsExactlyInAnyOrder(
				gespeicherteDLMRadNETZKante.getId());
		assertThat(result).extracting(KanteMapView::getGeometrie)
			.containsExactlyInAnyOrder(
				kanteDLMRadnetz.getGeometry());
		assertThat(result).extracting(KanteMapView::isZweiseitig)
			.containsExactlyInAnyOrder(
				kanteDLMRadnetz.isZweiseitig());
		assertThat(result).extracting(KanteMapView::getId)
			.doesNotContain(
				gespeicherteKanteDLM.getId());
	}

	@Test
	void testgetKantenMapViewInBereich_netzKlasse() {
		// Arrange
		Kante kanteRadnetz = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 10, 1, 20, QuellSystem.RadNETZ)
				.kantenAttributGruppe(
					new KantenAttributGruppe(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
						.build(), Set.of(Netzklasse.RADNETZ_ALLTAG), new HashSet<>()))
				.isGrundnetz(true)
				.build());
		Kante kanteNichtklassifiziert = kantenRepository.save(KanteTestDataProvider
			.withCoordinatesAndQuelle(0, 10, 1, 20, QuellSystem.DLM)
			.kantenAttributGruppe(
				new KantenAttributGruppe(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute().build(),
					new HashSet<>(), new HashSet<>()))
			.isGrundnetz(true)
			.build());
		Kante kanteRadnetzUndKommunal = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(3, 3, 7, 7, QuellSystem.RadNETZ)
				.kantenAttributGruppe(
					new KantenAttributGruppe(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
						.build(), Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_ALLTAG), new HashSet<>()))
				.isGrundnetz(true)
				.build());

		entityManager.flush();
		entityManager.clear();

		// Act
		Set<KanteMapView> noFilter = abfrageRepository
			.getKantenMapViewInBereich(new Envelope(0, 30, 0, 30), new HashSet<>());
		Set<KanteMapView> radNetz = abfrageRepository
			.getKantenMapViewInBereich(new Envelope(0, 30, 0, 30), Set.of(NetzklasseFilter.RADNETZ));
		Set<KanteMapView> nichtKlassifiziert = abfrageRepository
			.getKantenMapViewInBereich(new Envelope(0, 30, 0, 30), Set.of(NetzklasseFilter.NICHT_KLASSIFIZIERT));
		Set<KanteMapView> nichtKlassifiziertUndRadNetz = abfrageRepository
			.getKantenMapViewInBereich(new Envelope(0, 30, 0, 30),
				Set.of(NetzklasseFilter.NICHT_KLASSIFIZIERT, NetzklasseFilter.RADNETZ));
		Set<KanteMapView> alles = abfrageRepository
			.getKantenMapViewInBereich(new Envelope(0, 30, 0, 30),
				Set.of(NetzklasseFilter.NICHT_KLASSIFIZIERT, NetzklasseFilter.RADNETZ, NetzklasseFilter.KOMMUNALNETZ,
					NetzklasseFilter.KREISNETZ));

		// Assert
		assertThat(noFilter).isEmpty();
		assertThat(radNetz).hasSize(2);
		assertThat(radNetz).flatExtracting(AbstractEntity::getId)
			.containsExactlyInAnyOrder(kanteRadnetz.getId(), kanteRadnetzUndKommunal.getId());
		assertThat(nichtKlassifiziert).hasSize(1);
		assertThat(nichtKlassifiziert).flatExtracting(AbstractEntity::getId)
			.containsExactlyInAnyOrder(kanteNichtklassifiziert.getId());
		assertThat(nichtKlassifiziertUndRadNetz).hasSize(3);
		assertThat(nichtKlassifiziertUndRadNetz).flatExtracting(AbstractEntity::getId)
			.containsExactlyInAnyOrder(kanteRadnetz.getId(), kanteNichtklassifiziert.getId(),
				kanteRadnetzUndKommunal.getId());

		assertThat(alles).size().isEqualTo(3);
		assertThat(alles).flatExtracting(AbstractEntity::getId)
			.containsExactlyInAnyOrder(kanteRadnetz.getId(), kanteNichtklassifiziert.getId(),
				kanteRadnetzUndKommunal.getId());
	}

	@Test
	void getGeometrienVerlaufMapViewInBereich() {
		// arrange
		Kante ohne = KanteTestDataProvider.withCoordinatesAndQuelle(1, 1, 1, 20, QuellSystem.RadNETZ)
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.build(),
				Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.KOMMUNALNETZ_ALLTAG),
				new HashSet<>()))
			.isGrundnetz(true)
			.build();

		LineString nurlinkslinks = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(1, 1), new Coordinate(1, 20) });
		Kante nurlinks = KanteTestDataProvider.withCoordinatesAndQuelle(2, 1, 2, 20, QuellSystem.RadNETZ)
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.build(),
				Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.KOMMUNALNETZ_ALLTAG),
				new HashSet<>()))
			.isZweiseitig(true)
			.isGrundnetz(true)
			.verlaufLinks(nurlinkslinks)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true).build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.build();

		LineString nurrechtsrechts = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(4, 1), new Coordinate(4, 20) });
		Kante nurrechts = KanteTestDataProvider.withCoordinatesAndQuelle(3, 1, 3, 20, QuellSystem.RadNETZ)
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.build(),
				Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.KOMMUNALNETZ_ALLTAG),
				new HashSet<>()))
			.isZweiseitig(true)
			.isGrundnetz(true)
			.verlaufRechts(nurrechtsrechts)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true).build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.build();

		LineString beidelinks = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(3, 1), new Coordinate(3, 20) });
		LineString beiderechts = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(5, 1), new Coordinate(5, 20) });
		Kante beide = KanteTestDataProvider.withCoordinatesAndQuelle(4, 1, 4, 20, QuellSystem.RadNETZ)
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.build(),
				Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.KOMMUNALNETZ_ALLTAG),
				new HashSet<>()))
			.isZweiseitig(true)
			.isGrundnetz(true)
			.verlaufLinks(beidelinks).verlaufRechts(beiderechts)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true).build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.build();

		LineString halblinks = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(4, 1), new Coordinate(4, 40) });
		Kante halbdrinn = KanteTestDataProvider.withCoordinatesAndQuelle(5, 1, 5, 40, QuellSystem.RadNETZ)
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.build(),
				Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.KOMMUNALNETZ_ALLTAG),
				new HashSet<>()))
			.isZweiseitig(true)
			.isGrundnetz(true)
			.verlaufLinks(halblinks)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true).build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.build();

		LineString nichtdrinlinks = GEO_FACTORY
			.createLineString(new Coordinate[] { new Coordinate(45, 50), new Coordinate(45, 60) });
		Kante nichtdrinn = KanteTestDataProvider.withCoordinatesAndQuelle(50, 50, 50, 60, QuellSystem.RadNETZ)
			.kantenAttributGruppe(new KantenAttributGruppe(KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
				.build(),
				Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_ZIELNETZ, Netzklasse.KOMMUNALNETZ_ALLTAG),
				new HashSet<>()))
			.isZweiseitig(true)
			.verlaufLinks(nichtdrinlinks)
			.isGrundnetz(true)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true).build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.build();

		kantenRepository.save(ohne);
		kantenRepository.save(nurlinks);
		kantenRepository.save(nurrechts);
		kantenRepository.save(beide);
		kantenRepository.save(halbdrinn);
		kantenRepository.save(nichtdrinn);
		entityManager.flush();
		entityManager.clear();

		Envelope bereich = new Envelope(0, 30, 0, 30);
		Set<NetzklasseFilter> netzklassen = Set.of(NetzklasseFilter.RADNETZ);

		// act
		Set<GeometrienVerlaufMapView> result = abfrageRepository.getGeometrienVerlaufMapViewInBereich(bereich,
			netzklassen);

		// assert
		assertThat(result.size()).isEqualTo(4);
		assertThat(result)
			.flatExtracting(GeometrienVerlaufMapView::getGeometrieLinks,
				GeometrienVerlaufMapView::getGeometrieRechts)
			.contains(Optional.of(nurlinkslinks), Optional.of(nurrechtsrechts), Optional.of(beidelinks),
				Optional.of(beiderechts), Optional.of(halblinks))
			.doesNotContain(Optional.of(nichtdrinlinks));

	}

	@Test
	void testgetKanteAttributeViewInBereichNachNetzklasse() {
		// Arrange
		kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 10, 1, 20, QuellSystem.DLM)
				.kantenAttributGruppe(
					new KantenAttributGruppe(
						KantenAttribute.builder()
							.kommentar(Kommentar.of("comment"))
							.build(),
						new HashSet<>(), new HashSet<>()))
				.isGrundnetz(true)
				.build());

		entityManager.flush();
		entityManager.clear();

		// Act
		Set<KanteFuehrungsformAttributeView> resultFuehrung = abfrageRepository
			.getKanteFuehrungsformAttributeViewInBereichNachNetzklasse(new Envelope(0, 30, 0, 30),
				Set.of(NetzklasseFilter.RADNETZ, NetzklasseFilter.NICHT_KLASSIFIZIERT), false);
		Set<KanteZustaendigkeitAttributeView> resultZustaendigkeit = abfrageRepository
			.getKanteZustaendigkeitAttributeViewInBereichNachNetzklasse(new Envelope(0, 30, 0, 30),
				Set.of(NetzklasseFilter.RADNETZ, NetzklasseFilter.NICHT_KLASSIFIZIERT), false);

		// Assert
		assertThat(resultFuehrung).hasSize(1);
		assertThat(resultFuehrung.iterator().next().getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks().size()).isEqualTo(1);
		assertThat(resultZustaendigkeit).hasSize(1);
		assertThat(resultZustaendigkeit.iterator().next().getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute()).hasSize(1);
	}

	@Test
	public void testGetKantenMapViewInBereichFuerQuelle() {
		// Arrange
		Kante kanteDLMInBereich = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 10, 1, 20, QuellSystem.DLM)
				.build());
		kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(40, 40, 45, 45, QuellSystem.DLM)
				.build());
		kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(5, 20, 12, 15, QuellSystem.RadNETZ)
				.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();

		// Act
		Set<KanteMapView> kantenMapViewInBereichFuerQuelle = abfrageRepository
			.getKantenMapViewInBereichFuerQuelle(bereich, QuellSystem.DLM);

		assertThat(kantenMapViewInBereichFuerQuelle.stream().map(KanteMapView::getGeometrie))
			.containsExactlyInAnyOrderElementsOf(
				List.of(kanteDLMInBereich).stream().map(Kante::getGeometry).collect(
					Collectors.toList()));
	}

	@Test
	void getKanteFuehrungsformAttributeViewInBereichNachNetzklasse_NichtKlassifiziert() {
		Kante kanteNichtKlassifiziertInnerhalbBereich = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(5, 20, 12, 15, QuellSystem.RadVis)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(Set.of()).build())
				.isGrundnetz(true)
				.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();

		Set<KanteFuehrungsformAttributeView> views = abfrageRepository
			.getKanteFuehrungsformAttributeViewInBereichNachNetzklasse(bereich,
				Set.of(NetzklasseFilter.NICHT_KLASSIFIZIERT), false);

		assertThat(views).extracting(AbstractKanteLinearReferenzierteAttributeView::getGeometry)
			.containsExactlyInAnyOrder(kanteNichtKlassifiziertInnerhalbBereich.getGeometry());
	}

	@Test
	void getKanteFuehrungsformAttributeViewInBereichNachNetzklasse_RadNETZ() {
		Kante kanteRadNETZInBereich = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 10, 1, 20, QuellSystem.RadNETZ)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG)).build())
				.isGrundnetz(true)
				.build());
		// ausserhalb radnetz
		kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(40, 40, 45, 45, QuellSystem.RadNETZ)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG)).build())
				.isGrundnetz(true)
				.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();

		Set<KanteFuehrungsformAttributeView> views = abfrageRepository
			.getKanteFuehrungsformAttributeViewInBereichNachNetzklasse(bereich,
				Set.of(NetzklasseFilter.RADNETZ), false);

		assertThat(views).extracting(AbstractKanteLinearReferenzierteAttributeView::getGeometry)
			.containsExactlyInAnyOrder(kanteRadNETZInBereich.getGeometry());
	}

	@Test
	void getKanteZustaendigkeitAttributeViewInBereichNachNetzklasse_NichtKlassifiziert() {
		Kante kanteNichtKlassifiziertInnerhalbBereich = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(5, 20, 12, 15, QuellSystem.RadVis)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(Set.of()).build())
				.isGrundnetz(true)
				.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();

		Set<KanteZustaendigkeitAttributeView> views = abfrageRepository
			.getKanteZustaendigkeitAttributeViewInBereichNachNetzklasse(bereich,
				Set.of(NetzklasseFilter.NICHT_KLASSIFIZIERT), false);

		assertThat(views).extracting(AbstractKanteLinearReferenzierteAttributeView::getGeometry)
			.containsExactlyInAnyOrder(kanteNichtKlassifiziertInnerhalbBereich.getGeometry());
	}

	@Test
	void getKanteZustaendigkeitAttributeViewInBereichNachNetzklasse_RadNETZ() {
		Kante kanteRadNETZInBereich = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 10, 1, 20, QuellSystem.RadNETZ)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG)).build())
				.isGrundnetz(true)
				.build());
		// ausserhalb radnetz
		kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(40, 40, 45, 45, QuellSystem.RadNETZ)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG)).build())
				.isGrundnetz(true)
				.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();

		Set<KanteZustaendigkeitAttributeView> views = abfrageRepository
			.getKanteZustaendigkeitAttributeViewInBereichNachNetzklasse(bereich,
				Set.of(NetzklasseFilter.RADNETZ), false);

		assertThat(views).extracting(AbstractKanteLinearReferenzierteAttributeView::getGeometry)
			.containsExactlyInAnyOrder(kanteRadNETZInBereich.getGeometry());
	}

	@Test
	void getKanteGeschwindigkeitAttributeViewInBereichNachNetzklasse_RadNETZ() {
		Kante kanteRadNETZInBereich = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 10, 1, 20, QuellSystem.RadNETZ)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG)).build())
				.isGrundnetz(true)
				.build());
		// ausserhalb radnetz
		kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(40, 40, 45, 45, QuellSystem.RadNETZ)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG)).build())
				.isGrundnetz(true)
				.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();

		Set<KanteGeschwindigkeitAttributeView> views = abfrageRepository
			.getKanteGeschwindigkeitAttributeViewInBereichNachNetzklasse(bereich,
				Set.of(NetzklasseFilter.RADNETZ), false);

		assertThat(views).extracting(AbstractKanteLinearReferenzierteAttributeView::getGeometry)
			.containsExactlyInAnyOrder(kanteRadNETZInBereich.getGeometry());
	}

	@Test
	void getKanteAttributeViewInBereichNachNetzklasse_KeineKantenDuplikate() {
		Kante kante1 = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(0, 10, 1, 20, QuellSystem.DLM)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(
						Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ZIELNETZ,
							Netzklasse.KOMMUNALNETZ_ALLTAG,
							Netzklasse.RADVORRANGROUTEN))
					.build())
				.isGrundnetz(true)
				.build());
		Kante kante2 = kantenRepository
			.save(KanteTestDataProvider.withCoordinatesAndQuelle(10, 10, 20, 20, QuellSystem.DLM)
				.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(
						Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT, Netzklasse.KOMMUNALNETZ_ALLTAG,
							Netzklasse.RADVORRANGROUTEN))
					.build())
				.isGrundnetz(true)
				.build());

		Envelope bereich = new Envelope(0, 30, 0, 30);

		entityManager.flush();
		entityManager.clear();

		Set<KanteGeschwindigkeitAttributeView> viewsGeschwindigkeit = abfrageRepository
			.getKanteGeschwindigkeitAttributeViewInBereichNachNetzklasse(bereich,
				Set.of(NetzklasseFilter.RADNETZ), true);
		Set<KanteFuehrungsformAttributeView> viewsFuehrungsform = abfrageRepository
			.getKanteFuehrungsformAttributeViewInBereichNachNetzklasse(bereich,
				Set.of(NetzklasseFilter.RADNETZ), true);
		Set<KanteZustaendigkeitAttributeView> viewsZustaendigkeit = abfrageRepository
			.getKanteZustaendigkeitAttributeViewInBereichNachNetzklasse(bereich,
				Set.of(NetzklasseFilter.RADNETZ), true);

		assertThat(viewsGeschwindigkeit).hasSize(2);
		assertThat(viewsGeschwindigkeit.stream().filter(view -> view.getId().equals(kante1.getId())).findFirst().get()
			.getNetzklassen())
			.containsExactlyInAnyOrderElementsOf(kante1.getKantenAttributGruppe().getNetzklassen());
		assertThat(viewsGeschwindigkeit.stream().filter(view -> view.getId().equals(kante2.getId())).findFirst().get()
			.getNetzklassen())
			.containsExactlyInAnyOrderElementsOf(kante2.getKantenAttributGruppe().getNetzklassen());

		assertThat(viewsFuehrungsform).hasSize(2);
		assertThat(viewsFuehrungsform.stream().filter(view -> view.getId().equals(kante1.getId())).findFirst().get()
			.getNetzklassen())
			.containsExactlyInAnyOrderElementsOf(kante1.getKantenAttributGruppe().getNetzklassen());
		assertThat(viewsFuehrungsform.stream().filter(view -> view.getId().equals(kante2.getId())).findFirst().get()
			.getNetzklassen())
			.containsExactlyInAnyOrderElementsOf(kante2.getKantenAttributGruppe().getNetzklassen());

		assertThat(viewsZustaendigkeit).hasSize(2);
		assertThat(viewsZustaendigkeit.stream().filter(view -> view.getId().equals(kante1.getId())).findFirst().get()
			.getNetzklassen())
			.containsExactlyInAnyOrderElementsOf(kante1.getKantenAttributGruppe().getNetzklassen());
		assertThat(viewsZustaendigkeit.stream().filter(view -> view.getId().equals(kante2.getId())).findFirst().get()
			.getNetzklassen())
			.containsExactlyInAnyOrderElementsOf(kante2.getKantenAttributGruppe().getNetzklassen());
	}

	private Kante createKante(Coordinate vonKoordinate, Coordinate nachKoordinate, boolean isZweiseitig,
		boolean isGrundnetz) {
		return createKante(vonKoordinate, nachKoordinate, QuellSystem.DLM, isZweiseitig, isGrundnetz);
	}

	private Kante createKante(Coordinate vonKoordinate, Coordinate nachKoordinate, QuellSystem quellSystem,
		boolean isZweiseitig, boolean isGrundnetz) {
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(vonKoordinate, quellSystem).build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(nachKoordinate, quellSystem).build();

		LineString lineStringInnerhalb = GEO_FACTORY
			.createLineString(new Coordinate[] { vonKnoten.getKoordinate(), nachKnoten.getKoordinate() });

		Kante.KanteBuilder grundnetz = KanteTestDataProvider.withDefaultValues().vonKnoten(vonKnoten)
			.nachKnoten(nachKnoten)
			.geometry(lineStringInnerhalb).quelle(quellSystem).aufDlmAbgebildeteGeometry(null)
			.isZweiseitig(isZweiseitig)
			.isGrundnetz(isGrundnetz)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(isZweiseitig).build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(isZweiseitig).build());

		if (!quellSystem.equals(QuellSystem.DLM)) {
			grundnetz = grundnetz.dlmId(null);
		}

		return grundnetz.build();
	}
}
