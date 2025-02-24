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

import java.util.List;
import java.util.Set;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.valid4j.Assertive;

import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.OsmPbfConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.integration.attributAbbildung.IntegrationAttributAbbildungConfiguration;
import de.wps.radvis.backend.integration.dlm.IntegrationDlmConfiguration;
import de.wps.radvis.backend.integration.radnetz.IntegrationRadNetzConfiguration;
import de.wps.radvis.backend.integration.radwegedb.IntegrationRadwegeDBConfiguration;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.matching.MatchingConfiguration;
import de.wps.radvis.backend.matching.domain.GraphhopperDlmConfigurationProperties;
import de.wps.radvis.backend.matching.domain.GraphhopperOsmConfigurationProperties;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FahrtrichtungAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.GeschwindigkeitsAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KantenAttributeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenWithInitialStatesRepository;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.Beleuchtung;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Oberflaechenbeschaffenheit;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.TrennstreifenForm;
import de.wps.radvis.backend.netz.domain.valueObject.TrennungZu;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.quellimport.common.ImportsCommonConfiguration;
import de.wps.radvis.backend.quellimport.grundnetz.ImportsGrundnetzConfiguration;
import de.wps.radvis.backend.quellimport.grundnetz.domain.DLMConfigurationProperties;
import jakarta.persistence.EntityManager;

@Tag("group6")
@ContextConfiguration(classes = {
	NetzConfiguration.class,
	OrganisationConfiguration.class,
	CommonConfiguration.class,
	GeoConverterConfiguration.class,
	BenutzerConfiguration.class,
	MatchingConfiguration.class,
	ImportsCommonConfiguration.class,
	ImportsGrundnetzConfiguration.class,
	IntegrationDlmConfiguration.class,
	IntegrationAttributAbbildungConfiguration.class,
	IntegrationRadNetzConfiguration.class,
	IntegrationRadwegeDBConfiguration.class,
})
@EntityScan(basePackageClasses = { DokumentConfiguration.class, KommentarConfiguration.class })
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzkorrekturConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	FeatureToggleProperties.class,
	PostgisConfigurationProperties.class,
	GraphhopperDlmConfigurationProperties.class,
	GraphhopperOsmConfigurationProperties.class,
	OsmPbfConfigurationProperties.class,
	DLMConfigurationProperties.class,
	NetzConfigurationProperties.class
})
class AttributlueckenSchliessenJobTestIT extends DBIntegrationTestIT {
	@MockitoBean
	private NetzfehlerRepository netzfehlerRepository;
	@MockitoBean
	private BarriereRepository barriereRepository;

	private RecursiveComparisonConfiguration kanteComparisonConfiguration;

	private AttributlueckenSchliessenJob attributlueckenSchliessenJob;

	private Gebietskoerperschaft gebietskoerperschaftKreis;
	private Gebietskoerperschaft gebietskoerperschaftGemeinde;

	@Autowired
	private JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Autowired
	private KantenWithInitialStatesRepository kantenWithInitialStatesRepository;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;
	@Autowired
	private NetzService netzService;
	@Autowired
	private AttributlueckenService attributlueckenService;
	@Autowired
	private NetzkorrekturConfigurationProperties netzkorrekturConfigurationProperties;
	@Autowired
	private EntityManager entityManager;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		kanteComparisonConfiguration = RecursiveComparisonConfiguration.builder()
			.withIgnoredFieldsOfTypes(Geometry.class, LineString.class, MultiLineString.class, Envelope.class)
			.withIgnoredFields(
				"id", "dlmId", "vonKnoten", "nachKnoten", "geometry", "kantenLaengeInCm",
				"kantenAttributGruppe.id",
				"kantenAttributGruppe.kantenAttribute.kommentar",
				"kantenAttributGruppe.kantenAttribute.gemeinde",
				"kantenAttributGruppe.kantenAttribute.strassenName",
				"kantenAttributGruppe.kantenAttribute.strassenNummer",
				"kantenAttributGruppe.kantenAttribute.laengeManuellErfasst",
				"fuehrungsformAttributGruppe.id",
				"fahrtrichtungAttributGruppe.id",
				"geschwindigkeitAttributGruppe.id",
				"zustaendigkeitAttributGruppe.id")
			.withIgnoredFieldsMatchingRegexes(".*\\.version")
			.withIgnoreAllOverriddenEquals(true)
			.build();

		gebietskoerperschaftKreis = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Dingenskirchen")
				.organisationsArt(OrganisationsArt.KREIS)
				.build());
		gebietskoerperschaftGemeinde = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.name("Westoberniederostdorf")
				.organisationsArt(OrganisationsArt.GEMEINDE)
				.build());

		attributlueckenSchliessenJob = new AttributlueckenSchliessenJob(
			jobExecutionDescriptionRepository,
			kantenWithInitialStatesRepository,
			netzService,
			attributlueckenService,
			false);
	}

	@Test
	public void testEinfacheLuecke_einseitigeKanten() {
		//@formatter:off
		/*
		 * Kanten:
		 * 
		 *	1 ---> 2 ---> 3 ---> 4 ---> 5
		 *         |_____________|
		 *             Lücke
		 */
		//@formatter:on

		// Arrange
		Knoten knoten1 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(10, 0).build());
		Knoten knoten2 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(20, 0).build());
		Knoten knoten3 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(30, 0).build());
		Knoten knoten4 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(40, 0).build());
		Knoten knoten5 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(50, 0).build());

		KantenAttribute.KantenAttributeBuilder kantenAttributeBuilder = KantenAttributeTestDataProvider
			.createWithValues(gebietskoerperschaftGemeinde);
		FuehrungsformAttribute.FuehrungsformAttributeBuilder fuehrungsformAttributeBuilder = FuehrungsformAttributeTestDataProvider
			.createWithValues();
		GeschwindigkeitAttribute.GeschwindigkeitAttributeBuilder geschwindigkeitAttributeBuilder = GeschwindigkeitsAttributeTestDataProvider
			.createWithValues();
		FahrtrichtungAttributGruppe.FahrtrichtungAttributGruppeBuilder fahrtrichtungAttributGruppeBuilder = FahrtrichtungAttributGruppeTestDataProvider
			.createWithValues();
		ZustaendigkeitAttribute.ZustaendigkeitAttributeBuilder zustaendigkeitAttributeBuilder = ZustaendigkeitAttributGruppeTestDataProvider
			.createWithValues(
				gebietskoerperschaftGemeinde,
				gebietskoerperschaftGemeinde,
				gebietskoerperschaftKreis);

		Kante kante12 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten1, knoten2)
				.isZweiseitig(false)
				.kantenAttributGruppe(
					KantenAttributGruppe.builder()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
						.kantenAttribute(kantenAttributeBuilder.build())
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppe.builder()
						.isZweiseitig(false)
						.fuehrungsformAttributeLinks(List.of(fuehrungsformAttributeBuilder.build()))
						.fuehrungsformAttributeRechts(List.of(fuehrungsformAttributeBuilder.build()))
						.build())
				.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeBuilder.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppe.builder()
						.geschwindigkeitAttribute(List.of(geschwindigkeitAttributeBuilder.build()))
						.build())
				.zustaendigkeitAttributGruppe(
					ZustaendigkeitAttributGruppe.builder()
						.zustaendigkeitAttribute(List.of(zustaendigkeitAttributeBuilder.build()))
						.build())
				.build());
		Kante kante23 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten2, knoten3).build());
		Kante kante34 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten3, knoten4).build());
		Kante kante45 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten4, knoten5)
				.isZweiseitig(false)
				.kantenAttributGruppe(
					KantenAttributGruppe.builder()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
						.kantenAttribute(kantenAttributeBuilder.build())
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppe.builder()
						.isZweiseitig(false)
						.fuehrungsformAttributeLinks(List.of(fuehrungsformAttributeBuilder.build()))
						.fuehrungsformAttributeRechts(List.of(fuehrungsformAttributeBuilder.build()))
						.build())
				.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeBuilder.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppe.builder()
						.geschwindigkeitAttribute(List.of(geschwindigkeitAttributeBuilder.build()))
						.build())
				.zustaendigkeitAttributGruppe(
					ZustaendigkeitAttributGruppe.builder()
						.zustaendigkeitAttribute(List.of(zustaendigkeitAttributeBuilder.build()))
						.build())
				.build());

		entityManager.flush();

		// Act
		attributlueckenSchliessenJob.doRun();

		// Assert
		assertThat(kante23)
			.usingRecursiveComparison(kanteComparisonConfiguration)
			.isEqualTo(kante12);
		assertThat(kante34)
			.usingRecursiveComparison(kanteComparisonConfiguration)
			.isEqualTo(kante12);
	}

	@Test
	public void testEinfacheLuecke_einseitigeKanten_lineareReferenzenAmStartUndEnde() {
		//@formatter:off
		/*
		 * Kanten:
		 *
		 *  1 ---> 2 ---> 3 ---> 4
		 *         |______|
		 *           Lücke
		 *
		 * Kanten 1-2 und 3-4 haben linear referenzierte Attribute, die bei der Übernahme entsprechend beachtet werden sollen.
		 */
		//@formatter:on

		// Arrange
		Knoten knoten1 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(10, 0).build());
		Knoten knoten2 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(20, 0).build());
		Knoten knoten3 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(30, 0).build());
		Knoten knoten4 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(40, 0).build());

		KantenAttribute.KantenAttributeBuilder kantenAttributeBuilder = KantenAttributeTestDataProvider
			.createWithValues(gebietskoerperschaftGemeinde);
		FuehrungsformAttribute.FuehrungsformAttributeBuilder fuehrungsformAttributeBuilder = FuehrungsformAttributeTestDataProvider
			.createWithValues();
		GeschwindigkeitAttribute.GeschwindigkeitAttributeBuilder geschwindigkeitAttributeBuilder = GeschwindigkeitsAttributeTestDataProvider
			.createWithValues();
		FahrtrichtungAttributGruppe.FahrtrichtungAttributGruppeBuilder fahrtrichtungAttributGruppeBuilder = FahrtrichtungAttributGruppeTestDataProvider
			.createWithValues();
		ZustaendigkeitAttribute.ZustaendigkeitAttributeBuilder zustaendigkeitAttributeBuilder = ZustaendigkeitAttributGruppeTestDataProvider
			.createWithValues(
				gebietskoerperschaftGemeinde,
				gebietskoerperschaftGemeinde,
				gebietskoerperschaftKreis);

		Kante kante12 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten1, knoten2)
				.isZweiseitig(false)
				.kantenAttributGruppe(
					KantenAttributGruppe.builder()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
						.kantenAttribute(kantenAttributeBuilder.build())
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppe.builder()
						.isZweiseitig(false)
						.fuehrungsformAttributeLinks(List.of(
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5)).build(),
							fuehrungsformAttributeBuilder.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt
								.of(0.5, 1)).build()))
						.fuehrungsformAttributeRechts(List.of(
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5)).build(),
							fuehrungsformAttributeBuilder.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt
								.of(0.5, 1)).build()))
						.build())
				.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeBuilder.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppe.builder()
						.geschwindigkeitAttribute(List.of(
							GeschwindigkeitAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5)).build(),
							geschwindigkeitAttributeBuilder
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1)).build()))
						.build())
				.zustaendigkeitAttributGruppe(
					ZustaendigkeitAttributGruppe.builder()
						.zustaendigkeitAttribute(List.of(
							ZustaendigkeitAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5)).build(),
							zustaendigkeitAttributeBuilder
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1)).build()))
						.build())
				.build());
		Kante kante23 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten2, knoten3).build());
		Kante kante34 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten3, knoten4)
				.isZweiseitig(false)
				.kantenAttributGruppe(
					KantenAttributGruppe.builder()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
						.kantenAttribute(kantenAttributeBuilder.build())
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppe.builder()
						.isZweiseitig(false)
						.fuehrungsformAttributeLinks(List.of(
							fuehrungsformAttributeBuilder.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt
								.of(0, 0.5)).build(),
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1)).build()))
						.fuehrungsformAttributeRechts(List.of(
							fuehrungsformAttributeBuilder.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt
								.of(0, 0.5)).build(),
							FuehrungsformAttributeTestDataProvider.withGrundnetzDefaultwerte()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1)).build()))
						.build())
				.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeBuilder.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppe.builder()
						.geschwindigkeitAttribute(List.of(
							geschwindigkeitAttributeBuilder
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5)).build(),
							GeschwindigkeitAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1)).build()))
						.build())
				.zustaendigkeitAttributGruppe(
					ZustaendigkeitAttributGruppe.builder()
						.zustaendigkeitAttribute(List.of(
							zustaendigkeitAttributeBuilder
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 0.5)).build(),
							ZustaendigkeitAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1)).build()))
						.build())
				.build());

		entityManager.flush();

		// Act
		attributlueckenSchliessenJob.doRun();

		// Assert
		assertThat(kante23.getKantenAttributGruppe().getNetzklassen()).isEqualTo(kante12.getKantenAttributGruppe()
			.getNetzklassen());
		assertThat(kante23.getKantenAttributGruppe().getIstStandards()).isEqualTo(kante12.getKantenAttributGruppe()
			.getIstStandards());

		RecursiveComparisonConfiguration kantenAttributeComparisonConfiguration = RecursiveComparisonConfiguration
			.builder()
			.withIgnoredFields(
				"kommentar",
				"gemeinde",
				"strassenName",
				"strassenNummer",
				"laengeManuellErfasst")
			.withIgnoreAllOverriddenEquals(true)
			.build();
		KantenAttribute actualKantenAttribute = kante23.getKantenAttributGruppe().getKantenAttribute();
		KantenAttribute expectedKantenAttribute = kante12.getKantenAttributGruppe().getKantenAttribute();
		assertThat(actualKantenAttribute).usingRecursiveComparison(kantenAttributeComparisonConfiguration).isEqualTo(
			expectedKantenAttribute);

		// Führungsform
		RecursiveComparisonConfiguration fuehrungsformAttributeComparisonConfiguration = RecursiveComparisonConfiguration
			.builder()
			.withIgnoredFieldsMatchingRegexes(
				"linearReferenzierterAbschnitt.*")
			.withIgnoreAllOverriddenEquals(true)
			.build();
		FuehrungsformAttribute actualFuehrungsform = kante23.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks().get(0);
		FuehrungsformAttribute sourceFuehrungsform = kante12.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeRechts()
			.stream()
			.filter(a -> a.getLinearReferenzierterAbschnitt().getVon().getAbschnittsmarke() == 0.5)
			.findFirst()
			.get();

		assertThat(actualFuehrungsform).usingRecursiveComparison(fuehrungsformAttributeComparisonConfiguration)
			.isEqualTo(sourceFuehrungsform);

		// Fahrtrichtung
		FahrtrichtungAttributGruppe actualFahrtrichtung = kante23.getFahrtrichtungAttributGruppe();
		FahrtrichtungAttributGruppe sourceFahrtrichtung = kante12.getFahrtrichtungAttributGruppe();

		assertThat(actualFahrtrichtung.isZweiseitig()).isFalse();
		assertThat(actualFahrtrichtung.getFahrtrichtungLinks()).isEqualTo(sourceFahrtrichtung.getFahrtrichtungRechts());
		assertThat(actualFahrtrichtung.getFahrtrichtungRechts()).isEqualTo(sourceFahrtrichtung.getFahrtrichtungLinks());

		// Geschwindigkeit
		GeschwindigkeitAttribute actualGeschwindigkeit = kante23.getGeschwindigkeitAttributGruppe()
			.getImmutableGeschwindigkeitAttribute().get(0);
		GeschwindigkeitAttribute sourceGeschwindigkeit = kante12.getGeschwindigkeitAttributGruppe()
			.getImmutableGeschwindigkeitAttribute()
			.stream()
			.filter(a -> a.getLinearReferenzierterAbschnitt().getVon().getAbschnittsmarke() == 0.5)
			.findFirst()
			.get();

		assertThat(actualGeschwindigkeit.sindAttributeGleich(sourceGeschwindigkeit)).isTrue();

		// Zuständigkeit
		ZustaendigkeitAttribute actualZustaendigkeit = kante23.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().get(0);
		ZustaendigkeitAttribute sourceZustaendigkeit = kante12.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute()
			.stream()
			.filter(a -> a.getLinearReferenzierterAbschnitt().getVon().getAbschnittsmarke() == 0.5)
			.findFirst()
			.get();

		assertThat(actualZustaendigkeit.sindAttributeGleich(sourceZustaendigkeit));
	}

	@Test
	public void testEinfacheLuecke_zweiseitigeKanten_trennstreifenNurAufEinerSeiteMoeglich() {
		//@formatter:off
		/*
		 * Aufbau der Kanten (Pfeil = Stationierungsrichtung):
		 *
		 * 1 ---> 2 ---> 3 ---> 4
		 *        |______|
		 *         Lücke
		 *
		 * Dieser Test testet, dass die richtige Seite der Führungsform für die vier möglichen Trennstreifen genommen
		 * werden, also Kanten- und Trennstreifen-Seiten korrekt behandelt werden.
		*/
		//@formatter:on

		// Arrange
		Knoten knoten1 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(10, 0).build());
		Knoten knoten2 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(20, 0).build());
		Knoten knoten3 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(30, 0).build());
		Knoten knoten4 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(40, 0).build());

		KantenAttribute.KantenAttributeBuilder kantenAttributeBuilder = KantenAttributeTestDataProvider
			.createWithValues(gebietskoerperschaftGemeinde);
		FuehrungsformAttribute.FuehrungsformAttributeBuilder fuehrungsformAttributeBuilderRechts = FuehrungsformAttributeTestDataProvider
			.createWithValues();
		FuehrungsformAttribute.FuehrungsformAttributeBuilder fuehrungsformAttributeBuilderLinks = FuehrungsformAttributeTestDataProvider
			.createWithValues()
			.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE)
			.radverkehrsfuehrung(Radverkehrsfuehrung.UNBEKANNT)
			.trennstreifenBreiteLinks(null)
			.trennstreifenFormLinks(null)
			.trennstreifenTrennungZuLinks(null)
			.trennstreifenBreiteRechts(null)
			.trennstreifenFormRechts(null)
			.trennstreifenTrennungZuRechts(null);
		GeschwindigkeitAttribute.GeschwindigkeitAttributeBuilder geschwindigkeitAttributeBuilder = GeschwindigkeitsAttributeTestDataProvider
			.createWithValues();
		FahrtrichtungAttributGruppe.FahrtrichtungAttributGruppeBuilder fahrtrichtungAttributGruppeBuilder = FahrtrichtungAttributGruppeTestDataProvider
			.createWithValues()
			.isZweiseitig(true)
			.fahrtrichtungRechts(Richtung.BEIDE_RICHTUNGEN)
			.fahrtrichtungLinks(Richtung.GEGEN_RICHTUNG);
		ZustaendigkeitAttribute.ZustaendigkeitAttributeBuilder zustaendigkeitAttributeBuilder = ZustaendigkeitAttributGruppeTestDataProvider
			.createWithValues(
				gebietskoerperschaftGemeinde,
				gebietskoerperschaftGemeinde,
				gebietskoerperschaftKreis);

		Kante kante12 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten1, knoten2)
				.isZweiseitig(true)
				.kantenAttributGruppe(
					KantenAttributGruppe.builder()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
						.kantenAttribute(kantenAttributeBuilder.build())
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppe.builder()
						.isZweiseitig(true)
						.fuehrungsformAttributeLinks(List.of(fuehrungsformAttributeBuilderLinks.build()))
						.fuehrungsformAttributeRechts(List.of(fuehrungsformAttributeBuilderRechts.build()))
						.build())
				.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeBuilder.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppe.builder()
						.geschwindigkeitAttribute(List.of(geschwindigkeitAttributeBuilder.build()))
						.build())
				.zustaendigkeitAttributGruppe(
					ZustaendigkeitAttributGruppe.builder()
						.zustaendigkeitAttribute(List.of(zustaendigkeitAttributeBuilder.build()))
						.build())
				.build());
		Kante kante23 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten2, knoten3).build());
		Kante kante34 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten3, knoten4)
				.isZweiseitig(true)
				.kantenAttributGruppe(
					KantenAttributGruppe.builder()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
						.kantenAttribute(kantenAttributeBuilder.build())
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppe.builder()
						.isZweiseitig(true)
						.fuehrungsformAttributeLinks(List.of(fuehrungsformAttributeBuilderLinks.build()))
						.fuehrungsformAttributeRechts(List.of(fuehrungsformAttributeBuilderRechts.build()))
						.build())
				.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeBuilder.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppe.builder()
						.geschwindigkeitAttribute(List.of(geschwindigkeitAttributeBuilder.build()))
						.build())
				.zustaendigkeitAttributGruppe(
					ZustaendigkeitAttributGruppe.builder()
						.zustaendigkeitAttribute(List.of(zustaendigkeitAttributeBuilder.build()))
						.build())
				.build());

		entityManager.flush();

		// Act
		attributlueckenSchliessenJob.doRun();

		// Assert

		// Allgemeine Attribute
		assertThat(kante23.getKantenAttributGruppe().getNetzklassen()).isEqualTo(kante12.getKantenAttributGruppe()
			.getNetzklassen());
		assertThat(kante23.getKantenAttributGruppe().getIstStandards()).isEqualTo(kante12.getKantenAttributGruppe()
			.getIstStandards());

		RecursiveComparisonConfiguration kantenAttributeComparisonConfiguration = RecursiveComparisonConfiguration
			.builder()
			.withIgnoredFields(
				"kommentar",
				"gemeinde",
				"strassenName",
				"strassenNummer",
				"laengeManuellErfasst")
			.withIgnoreAllOverriddenEquals(true)
			.build();
		KantenAttribute actualKantenAttribute = kante23.getKantenAttributGruppe().getKantenAttribute();
		KantenAttribute expectedKantenAttribute = kante12.getKantenAttributGruppe().getKantenAttribute();
		assertThat(actualKantenAttribute).usingRecursiveComparison(kantenAttributeComparisonConfiguration).isEqualTo(
			expectedKantenAttribute);

		// Führungsform
		RecursiveComparisonConfiguration fuehrungsformAttributeComparisonConfiguration = RecursiveComparisonConfiguration
			.builder()
			.withIgnoreAllOverriddenEquals(true)
			.build();

		assertThat(kante23.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0))
			.usingRecursiveComparison(fuehrungsformAttributeComparisonConfiguration)
			.isEqualTo(kante12.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeLinks().get(0));
		assertThat(kante23.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0))
			.usingRecursiveComparison(fuehrungsformAttributeComparisonConfiguration)
			.isEqualTo(kante12.getFuehrungsformAttributGruppe().getImmutableFuehrungsformAttributeRechts().get(0));

		// Fahrtrichtung
		FahrtrichtungAttributGruppe actualFahrtrichtung = kante23.getFahrtrichtungAttributGruppe();
		FahrtrichtungAttributGruppe sourceFahrtrichtung = kante12.getFahrtrichtungAttributGruppe();

		assertThat(actualFahrtrichtung.isZweiseitig()).isTrue();
		assertThat(actualFahrtrichtung.getFahrtrichtungLinks()).isEqualTo(sourceFahrtrichtung.getFahrtrichtungLinks());
		assertThat(actualFahrtrichtung.getFahrtrichtungRechts()).isEqualTo(sourceFahrtrichtung.getFahrtrichtungRechts()
			.umgedreht());

		// Geschwindigkeit
		GeschwindigkeitAttribute actualGeschwindigkeit = kante23.getGeschwindigkeitAttributGruppe()
			.getImmutableGeschwindigkeitAttribute().get(0);
		GeschwindigkeitAttribute sourceGeschwindigkeit = kante12.getGeschwindigkeitAttributGruppe()
			.getImmutableGeschwindigkeitAttribute().get(0);

		assertThat(actualGeschwindigkeit.sindAttributeGleich(sourceGeschwindigkeit));

		// Zuständigkeit
		ZustaendigkeitAttribute actualZustaendigkeit = kante23.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().get(0);
		ZustaendigkeitAttribute sourceZustaendigkeit = kante12.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().get(0);

		assertThat(actualZustaendigkeit.sindAttributeGleich(sourceZustaendigkeit));
	}

	@Test
	public void testEinfacheLuecke_zweiseitigeKantenMitGegenrichtung() {
		//@formatter:off
		/*
		 * 		Aufbau der Kanten (Pfeil = Stationierungsrichtung):
		 *
		 * 1 ---> 2 ---> 3 <--- 4 ---> 5
		 *        |_____________|
		 *           Lücke
		 */
		//@formatter:on

		// Arrange
		Knoten knoten1 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(10, 0).build());
		Knoten knoten2 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(20, 0).build());
		Knoten knoten3 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(30, 0).build());
		Knoten knoten4 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(40, 0).build());
		Knoten knoten5 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(50, 0).build());

		KantenAttribute.KantenAttributeBuilder kantenAttributeBuilder = KantenAttributeTestDataProvider
			.createWithValues(gebietskoerperschaftGemeinde);
		FuehrungsformAttribute.FuehrungsformAttributeBuilder fuehrungsformAttributeBuilderRechts = FuehrungsformAttributeTestDataProvider
			.createWithValues();
		FuehrungsformAttribute.FuehrungsformAttributeBuilder fuehrungsformAttributeBuilderLinks = FuehrungsformAttributeTestDataProvider
			.createWithValues()
			.belagArt(BelagArt.BETONSTEINPFLASTER_PLATTENBELAG)
			.oberflaechenbeschaffenheit(Oberflaechenbeschaffenheit.ANLASS_ZUR_INTENSIVEN_BEOBACHTUNG_UND_ANALYSE)
			.radverkehrsfuehrung(Radverkehrsfuehrung.SONDERWEG_RADWEG_STRASSENBEGLEITEND)
			.trennstreifenBreiteRechts(Laenge.of(999.99))
			.trennstreifenFormLinks(TrennstreifenForm.TRENNUNG_DURCH_MARKIERUNG_ODER_BAULICHE_TRENNUNG)
			.trennstreifenTrennungZuLinks(TrennungZu.SICHERHEITSTRENNSTREIFEN_ZUM_FUSSVERKEHR);
		GeschwindigkeitAttribute.GeschwindigkeitAttributeBuilder geschwindigkeitAttributeBuilder = GeschwindigkeitsAttributeTestDataProvider
			.createWithValues();
		FahrtrichtungAttributGruppe.FahrtrichtungAttributGruppeBuilder fahrtrichtungAttributGruppeBuilder = FahrtrichtungAttributGruppeTestDataProvider
			.createWithValues()
			.isZweiseitig(true)
			.fahrtrichtungRechts(Richtung.BEIDE_RICHTUNGEN)
			.fahrtrichtungLinks(Richtung.GEGEN_RICHTUNG);
		ZustaendigkeitAttribute.ZustaendigkeitAttributeBuilder zustaendigkeitAttributeBuilder = ZustaendigkeitAttributGruppeTestDataProvider
			.createWithValues(
				gebietskoerperschaftGemeinde,
				gebietskoerperschaftGemeinde,
				gebietskoerperschaftKreis);

		Kante kante12 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten1, knoten2)
				.isZweiseitig(true)
				.kantenAttributGruppe(
					KantenAttributGruppe.builder()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
						.kantenAttribute(kantenAttributeBuilder.build())
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppe.builder()
						.isZweiseitig(true)
						.fuehrungsformAttributeLinks(List.of(fuehrungsformAttributeBuilderLinks.build()))
						.fuehrungsformAttributeRechts(List.of(fuehrungsformAttributeBuilderRechts.build()))
						.build())
				.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeBuilder.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppe.builder()
						.geschwindigkeitAttribute(List.of(geschwindigkeitAttributeBuilder.build()))
						.build())
				.zustaendigkeitAttributGruppe(
					ZustaendigkeitAttributGruppe.builder()
						.zustaendigkeitAttribute(List.of(zustaendigkeitAttributeBuilder.build()))
						.build())
				.build());
		Kante kante23 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten2, knoten3).build());
		Kante kante43 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten4, knoten3).build());
		Kante kante45 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten4, knoten5)
				.isZweiseitig(true)
				.kantenAttributGruppe(
					KantenAttributGruppe.builder()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
						.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
						.kantenAttribute(kantenAttributeBuilder.build())
						.build())
				.fuehrungsformAttributGruppe(
					FuehrungsformAttributGruppe.builder()
						.isZweiseitig(true)
						.fuehrungsformAttributeLinks(List.of(fuehrungsformAttributeBuilderLinks.build()))
						.fuehrungsformAttributeRechts(List.of(fuehrungsformAttributeBuilderRechts.build()))
						.build())
				.fahrtrichtungAttributGruppe(fahrtrichtungAttributGruppeBuilder.build())
				.geschwindigkeitAttributGruppe(
					GeschwindigkeitAttributGruppe.builder()
						.geschwindigkeitAttribute(List.of(geschwindigkeitAttributeBuilder.build()))
						.build())
				.zustaendigkeitAttributGruppe(
					ZustaendigkeitAttributGruppe.builder()
						.zustaendigkeitAttribute(List.of(zustaendigkeitAttributeBuilder.build()))
						.build())
				.build());

		entityManager.flush();

		// Act
		attributlueckenSchliessenJob.doRun();

		// Assert

		// Allgemeine Attribute
		assertThat(kante43.getKantenAttributGruppe().getNetzklassen()).isEqualTo(kante12.getKantenAttributGruppe()
			.getNetzklassen());
		assertThat(kante43.getKantenAttributGruppe().getIstStandards()).isEqualTo(kante12.getKantenAttributGruppe()
			.getIstStandards());

		RecursiveComparisonConfiguration kantenAttributeComparisonConfiguration = RecursiveComparisonConfiguration
			.builder()
			.withIgnoredFields(
				"kommentar",
				"gemeinde",
				"strassenName",
				"strassenNummer",
				"laengeManuellErfasst")
			.withIgnoreAllOverriddenEquals(true)
			.build();
		KantenAttribute actualKantenAttribute = kante43.getKantenAttributGruppe().getKantenAttribute();
		KantenAttribute expectedKantenAttribute = kante12.getKantenAttributGruppe().getKantenAttribute();
		assertThat(actualKantenAttribute).usingRecursiveComparison(kantenAttributeComparisonConfiguration).isEqualTo(
			expectedKantenAttribute);

		// Führungsform
		RecursiveComparisonConfiguration fuehrungsformAttributeComparisonConfiguration = RecursiveComparisonConfiguration
			.builder()
			.withIgnoredFieldsMatchingRegexes(
				"trennstreifen.*")
			.withIgnoreAllOverriddenEquals(true)
			.build();
		FuehrungsformAttribute actualFuehrungsform = kante43.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeLinks().get(0);
		FuehrungsformAttribute sourceFuehrungsform = kante12.getFuehrungsformAttributGruppe()
			.getImmutableFuehrungsformAttributeRechts().get(0);

		assertThat(actualFuehrungsform).usingRecursiveComparison(fuehrungsformAttributeComparisonConfiguration)
			.isEqualTo(sourceFuehrungsform);
		assertThat(actualFuehrungsform.getTrennstreifenBreiteRechts()).isEqualTo(sourceFuehrungsform
			.getTrennstreifenBreiteLinks());
		assertThat(actualFuehrungsform.getTrennstreifenBreiteLinks()).isEqualTo(sourceFuehrungsform
			.getTrennstreifenBreiteRechts());
		assertThat(actualFuehrungsform.getTrennstreifenTrennungZuRechts()).isEqualTo(sourceFuehrungsform
			.getTrennstreifenTrennungZuLinks());
		assertThat(actualFuehrungsform.getTrennstreifenTrennungZuLinks()).isEqualTo(sourceFuehrungsform
			.getTrennstreifenTrennungZuRechts());
		assertThat(actualFuehrungsform.getTrennstreifenFormRechts()).isEqualTo(sourceFuehrungsform
			.getTrennstreifenFormLinks());
		assertThat(actualFuehrungsform.getTrennstreifenFormLinks()).isEqualTo(sourceFuehrungsform
			.getTrennstreifenFormRechts());

		// Fahrtrichtung
		FahrtrichtungAttributGruppe actualFahrtrichtung = kante43.getFahrtrichtungAttributGruppe();
		FahrtrichtungAttributGruppe sourceFahrtrichtung = kante12.getFahrtrichtungAttributGruppe();

		assertThat(actualFahrtrichtung.isZweiseitig()).isTrue();
		assertThat(actualFahrtrichtung.getFahrtrichtungLinks()).isEqualTo(sourceFahrtrichtung.getFahrtrichtungRechts());
		assertThat(actualFahrtrichtung.getFahrtrichtungRechts()).isEqualTo(sourceFahrtrichtung.getFahrtrichtungLinks()
			.umgedreht());

		// Geschwindigkeit
		GeschwindigkeitAttribute actualGeschwindigkeit = kante43.getGeschwindigkeitAttributGruppe()
			.getImmutableGeschwindigkeitAttribute().get(0);
		GeschwindigkeitAttribute sourceGeschwindigkeit = kante12.getGeschwindigkeitAttributGruppe()
			.getImmutableGeschwindigkeitAttribute().get(0);

		assertThat(actualGeschwindigkeit.sindAttributeGleich(sourceGeschwindigkeit));

		// Zuständigkeit
		ZustaendigkeitAttribute actualZustaendigkeit = kante43.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().get(0);
		ZustaendigkeitAttribute sourceZustaendigkeit = kante12.getZustaendigkeitAttributGruppe()
			.getImmutableZustaendigkeitAttribute().get(0);

		assertThat(actualZustaendigkeit.sindAttributeGleich(sourceZustaendigkeit));
	}

	@Test
	public void testMehrfacheStartEndKanten_intersectionAusNetzklassenUndStandardsUebernommen() {
		//@formatter:off
		/*
		 * 4 <--- 5 ---> 6
		 *        ↑
		 *        |
		 * 1 ---> 2 <--- 3
		 */
		//@formatter:on

		// Dieser Test geht hiervon aus:
		Assertive.require(netzkorrekturConfigurationProperties.getMaximaleAnzahlAdjazenterAttribuierterKanten() >= 2);

		// Arrange
		Knoten knoten1 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(10, 0).build());
		Knoten knoten2 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(20, 0).build());
		Knoten knoten3 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(30, 0).build());
		Knoten knoten4 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(10, 10).build());
		Knoten knoten5 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(20, 10).build());
		Knoten knoten6 = netzService.saveKnoten(KnotenTestDataProvider.withPosition(30, 10).build());

		Kante kante12 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten1, knoten2)
				.kantenAttributGruppe(
					KantenAttributGruppe.builder()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT))
						.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ, IstStandard.RADSCHNELLVERBINDUNG))
						.build())
				.build());
		Kante kante32 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten3, knoten2)
				.kantenAttributGruppe(
					KantenAttributGruppe.builder()
						.netzklassen(Set.of(Netzklasse.RADNETZ_FREIZEIT))
						.istStandards(Set.of(IstStandard.RADSCHNELLVERBINDUNG))
						.build())
				.build());
		Kante kante25 = netzService.saveKante(KanteTestDataProvider.fromKnoten(knoten2, knoten5).build());
		Kante kante54 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten5, knoten4)
				.kantenAttributGruppe(
					KantenAttributGruppe.builder()
						.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT))
						.istStandards(Set.of(IstStandard.STARTSTANDARD_RADNETZ))
						.build())
				.build());
		Kante kante56 = netzService.saveKante(
			KanteTestDataProvider.fromKnoten(knoten5, knoten6)
				.kantenAttributGruppe(
					KantenAttributGruppe.builder()
						.kantenAttribute(
							KantenAttributeTestDataProvider.withLeereGrundnetzAttribute()
								.beleuchtung(Beleuchtung.VORHANDEN)
								.build())
						.build())
				.build());

		entityManager.flush();

		// Act
		attributlueckenSchliessenJob.doRun();

		// Assert
		assertThat(kante25.getKantenAttributGruppe().getNetzklassen()).containsExactlyInAnyOrder(
			Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT);
		assertThat(kante25.getKantenAttributGruppe().getIstStandards()).containsExactlyInAnyOrder(
			IstStandard.STARTSTANDARD_RADNETZ);
	}
}