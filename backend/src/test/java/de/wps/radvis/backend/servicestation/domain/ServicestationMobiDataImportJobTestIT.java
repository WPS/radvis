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
package de.wps.radvis.backend.servicestation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.apache.commons.compress.utils.Lists;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.operation.TransformException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.CoordinateReferenceSystemConverterUtility;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescriptionTestDataProvider;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.repository.GeoJsonImportRepository;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.OrganisationRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
import de.wps.radvis.backend.servicestation.domain.entity.ServicestationMobiDataImportStatistik;
import de.wps.radvis.backend.servicestation.domain.entity.provider.ServicestationTestDataProvider;
import de.wps.radvis.backend.servicestation.domain.valueObject.Betreiber;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationName;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationTyp;

@Tag("group5")
@ContextConfiguration(classes = {
	de.wps.radvis.backend.servicestation.domain.ServicestationMobiDataImportJobTestIT.TestConfiguration.class,
	CommonConfiguration.class,
	GeoConverterConfiguration.class,
	OrganisationConfiguration.class,
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
})
class ServicestationMobiDataImportJobTestIT extends DBIntegrationTestIT {

	private Gebietskoerperschaft badSaeckingen;
	private Gebietskoerperschaft rheinNeckarKreis;
	private Organisation unbekannt;
	private Gebietskoerperschaft gemeindeHerrenberg;
	private Gebietskoerperschaft kreisHerrenberg;

	@EnableJpaRepositories(basePackages = { "de.wps.radvis.backend.servicestation" })
	@EntityScan(basePackages = { "de.wps.radvis.backend.servicestation", "de.wps.radvis.backend.netz",
		"de.wps.radvis.backend.common", "de.wps.radvis.backend.dokument", "de.wps.radvis.backend.organisation", })
	public static class TestConfiguration {
	}

	@Mock
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Autowired
	private ServicestationRepository servicestationRepository;

	@Autowired
	private GeoJsonImportRepository geoJsonImportRepository;

	@Autowired
	private VerwaltungseinheitService verwaltungseinheitService;

	@Autowired
	private VerwaltungseinheitRepository verwaltungseinheitRepository;

	@Autowired
	private OrganisationRepository organisationRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	private ServicestationMobiDataImportJob servicestationenImportJob;

	@BeforeEach
	void setUp() throws MalformedURLException {
		MockitoAnnotations.openMocks(this);

		when(jobExecutionDescriptionRepository.save(any())).thenReturn(
			JobExecutionDescriptionTestDataProvider.withDefaultValues().build());

		badSaeckingen = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider
				.defaultGebietskoerperschaft()
				.name("Bad Säckingen")
				.organisationsArt(OrganisationsArt.GEMEINDE)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(
					417779.91, 5266142.56, 425870.54, 5271513.08
				))
				.build());

		gemeindeHerrenberg = gebietskoerperschaftRepository.save(VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Herrenberg")
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(
				484517.34, 5378575.25, 498668.04, 5387368.94
			))
			.build());
		kreisHerrenberg = gebietskoerperschaftRepository.save(VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Herrenberg")
			.organisationsArt(OrganisationsArt.KREIS)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(
				484017.34, 5378075.25, 498668.04, 5387368.94
			))
			.build());
		rheinNeckarKreis = gebietskoerperschaftRepository.save(VerwaltungseinheitTestDataProvider
			.defaultGebietskoerperschaft()
			.name("Rhein-Neckar-Kreis")
			.organisationsArt(OrganisationsArt.KREIS)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(
				460067.71, 5446840.96, 507346.49, 5497063.48
			))
			.build());
		unbekannt = organisationRepository.save(VerwaltungseinheitTestDataProvider
			.defaultOrganisation()
			.name("Unbekannt")
			.organisationsArt(OrganisationsArt.SONSTIGES)
			.build());

		servicestationenImportJob = new ServicestationMobiDataImportJob(jobExecutionDescriptionRepository,
			geoJsonImportRepository, servicestationRepository, verwaltungseinheitService,
			verwaltungseinheitRepository,
			new File("src/test/resources/testServicestationMobiData.json").toURI().toURL().toString());
	}

	@AfterEach
	void cleanUp() {
		AdditionalRevInfoHolder.clear();
	}

	@Test
	void doRun_testJson_allePropertiesKorrektGesetzt() throws FactoryException, TransformException {
		// act
		servicestationenImportJob.doRun();

		// assert
		List<Servicestation> all = Lists.newArrayList(servicestationRepository.findAll().iterator());
		assertThat(all).hasSize(8);
		assertThat(all)
			.usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
				.withIgnoredFields("id", "dokumentListe")
				.withComparatorForFields(
					Comparator.comparing(Point::getCoordinate, GeometryTestdataProvider.LENIENT_COORDINATE_COMPARATOR),
					"geometrie")
				.withComparatorForType(Comparator.comparing(AbstractEntity::getId), Verwaltungseinheit.class)
				.build())
			.containsExactlyInAnyOrder(
				ServicestationTestDataProvider.withDefaultMobiDataValues()
					.version(0L)
					.geometrie(getPointInUtm32(new Coordinate(47.5584657, 7.9666078)))
					.organisation(badSaeckingen)
					.name(ServicestationName.of("Funpark/Feuerwehr"))
					.typ(ServicestationTyp.RADSERVICE_PUNKT_GROSS)
					.build(),
				ServicestationTestDataProvider.withDefaultMobiDataValues()
					.version(0L)
					.geometrie(getPointInUtm32(new Coordinate(47.5529956, 7.9500806)))
					.organisation(badSaeckingen)
					.name(ServicestationName.of("Funpark/Feuerwehr"))
					.typ(ServicestationTyp.RADSERVICE_PUNKT_GROSS)
					.build(),
				ServicestationTestDataProvider.withDefaultMobiDataValues()
					.version(0L)
					.geometrie(getPointInUtm32(new Coordinate(47.5666143, 7.9146569)))
					.organisation(rheinNeckarKreis)
					.name(ServicestationName.of("Wallbach"))
					.typ(ServicestationTyp.RADSERVICE_PUNKT_GROSS)
					.build(),
				ServicestationTestDataProvider.withDefaultMobiDataValues()
					.version(0L)
					.geometrie(getPointInUtm32(new Coordinate(47.5827458, 8.00138315)))
					.organisation(unbekannt)
					.name(ServicestationName.of("Lochmühle"))
					.typ(ServicestationTyp.RADSERVICE_PUNKT_KLEIN)
					.build(),
				ServicestationTestDataProvider.withDefaultMobiDataValues()
					.version(0L)
					.geometrie(getPointInUtm32(new Coordinate(48.59652965, 8.8704902046814)))
					.organisation(gemeindeHerrenberg)
					.betreiber(Betreiber.of("Stadt Herrenberg"))
					.name(ServicestationName.of("Marktplatz"))
					.typ(ServicestationTyp.RADSERVICE_PUNKT_GROSS)
					.build(),
				ServicestationTestDataProvider.withDefaultMobiDataValues()
					.version(0L)
					.geometrie(getPointInUtm32(new Coordinate(48.59104305, 8.8754962487166)))
					.organisation(kreisHerrenberg)
					.betreiber(Betreiber.of("Stadt Herrenberg"))
					.name(ServicestationName.of("Hallenbad"))
					.typ(ServicestationTyp.RADSERVICE_PUNKT_GROSS)
					.build(),
				ServicestationTestDataProvider.withDefaultMobiDataValues()
					.version(0L)
					.geometrie(getPointInUtm32(new Coordinate(48.58096285, 8.90817244152688)))
					.organisation(kreisHerrenberg)
					.betreiber(Betreiber.of("Stadt Herrenberg"))
					.name(ServicestationName.of("Mönchberg"))
					.typ(ServicestationTyp.RADSERVICE_PUNKT_GROSS)
					.build(),
				ServicestationTestDataProvider.withDefaultMobiDataValues()
					.version(0L)
					.geometrie(getPointInUtm32(new Coordinate(48.576814, 8.922645)))
					.organisation(kreisHerrenberg)
					.betreiber(Betreiber.of("Stadt Herrenberg"))
					.name(ServicestationName.of("Kayh"))
					.typ(ServicestationTyp.RADSERVICE_PUNKT_GROSS)
					.build()
			);
	}

	@Test
	void doRun_servicestationBereitsVorhandenLeichteVerschiebung_wirdGeupdatet()
		throws FactoryException, TransformException {
		// arrange

		Point newPoint = getPointInUtm32(new Coordinate(47.5666143, 7.9146569));
		Point oldPoint = (Point) AffineTransformation.translationInstance(1, 1).transform(newPoint);
		Long servicestationVorhandenId = servicestationRepository.save(
			ServicestationTestDataProvider.withDefaultMobiDataValues()
				.geometrie(oldPoint)
				.organisation(unbekannt)
				.name(ServicestationName.of("Wallbach"))
				.typ(ServicestationTyp.RADSERVICE_PUNKT_KLEIN)
				.build()).getId();

		// act
		servicestationenImportJob.doRun();

		// assert
		Optional<Servicestation> servicestationGeupdated = servicestationRepository.findById(servicestationVorhandenId);
		assertThat(servicestationGeupdated).isPresent();
		assertThat(servicestationGeupdated.get())
			.usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
				.withIgnoredFields("dokumentListe")
				.withComparatorForFields(
					Comparator.comparing(Point::getCoordinate, GeometryTestdataProvider.LENIENT_COORDINATE_COMPARATOR),
					"geometrie")
				.withComparatorForType(Comparator.comparing(AbstractEntity::getId), Verwaltungseinheit.class)
				.build())
			.isEqualTo(ServicestationTestDataProvider.withDefaultMobiDataValues()
				.id(servicestationVorhandenId)
				.version(1L)
				.geometrie(newPoint)
				.organisation(rheinNeckarKreis)
				.name(ServicestationName.of("Wallbach"))
				.typ(ServicestationTyp.RADSERVICE_PUNKT_GROSS)
				.build());
	}

	@Test
	void doRun_servicestationMitDemselbenNamenBereitsVorhandenStarkeVerschiebung_wirdGeloeschtUndNeuAngelegt()
		throws FactoryException, TransformException {
		// arrange
		Point newPoint = getPointInUtm32(new Coordinate(47.5666143, 7.9146569));
		Point oldPoint = (Point) AffineTransformation.translationInstance(
			ServicestationMobiDataImportJob.MAX_VERSCHIEBUNG_VORHANDENER_STATION,
			ServicestationMobiDataImportJob.MAX_VERSCHIEBUNG_VORHANDENER_STATION).transform(newPoint);
		Long servicestationZuLoeschenId = servicestationRepository.save(
			ServicestationTestDataProvider.withDefaultMobiDataValues()
				.geometrie(oldPoint)
				.organisation(unbekannt)
				.name(ServicestationName.of("Wallbach"))
				.typ(ServicestationTyp.RADSERVICE_PUNKT_KLEIN)
				.build()).getId();

		// act
		servicestationenImportJob.doRun();

		// assert
		List<Servicestation> all = Lists.newArrayList(servicestationRepository.findAll().iterator());
		assertThat(all).hasSize(8);
		assertThat(all).extracting(AbstractEntity::getId).doesNotContain(servicestationZuLoeschenId);
		assertThat(all)
			.usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
				.withIgnoredFields("dokumentListe", "id")
				.withComparatorForFields(
					Comparator.comparing(Point::getCoordinate, GeometryTestdataProvider.LENIENT_COORDINATE_COMPARATOR),
					"geometrie")
				.withComparatorForType(Comparator.comparing(AbstractEntity::getId), Verwaltungseinheit.class)
				.build())
			.contains(ServicestationTestDataProvider.withDefaultMobiDataValues()
				.id(servicestationZuLoeschenId)
				.version(0L)
				.geometrie(newPoint)
				.organisation(rheinNeckarKreis)
				.name(ServicestationName.of("Wallbach"))
				.typ(ServicestationTyp.RADSERVICE_PUNKT_GROSS)
				.build());
	}

	@Test
	void doRun_zweiServicestationAufDiesselbeBestehendeStationGemappt_ersteAktualisiertZweiteNeuAngelegt()
		throws FactoryException, TransformException {
		// arrange
		servicestationenImportJob.doRun();

		Point newPoint = getPointInUtm32(new Coordinate(47.5666143, 7.9146569));
		Point oldPoint = (Point) AffineTransformation.translationInstance(10, 10).transform(newPoint);
		Long servicestationZuLoeschenId = servicestationRepository.save(
			ServicestationTestDataProvider.withDefaultMobiDataValues()
				.geometrie(oldPoint)
				.organisation(unbekannt)
				.name(ServicestationName.of("Wallbach"))
				.typ(ServicestationTyp.RADSERVICE_PUNKT_KLEIN)
				.build()).getId();

		// act
		servicestationenImportJob.doRun();

		// assert
		List<Servicestation> all = Lists.newArrayList(servicestationRepository.findAll().iterator());
		assertThat(all).hasSize(8);
		assertThat(all).extracting(AbstractEntity::getId).doesNotContain(servicestationZuLoeschenId);
		assertThat(all)
			.usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
				.withIgnoredFields("dokumentListe", "id")
				.withComparatorForFields(
					Comparator.comparing(Point::getCoordinate, GeometryTestdataProvider.LENIENT_COORDINATE_COMPARATOR),
					"geometrie")
				.withComparatorForType(Comparator.comparing(AbstractEntity::getId), Verwaltungseinheit.class)
				.build())
			.contains(ServicestationTestDataProvider.withDefaultMobiDataValues()
				.id(servicestationZuLoeschenId)
				.version(0L)
				.geometrie(newPoint)
				.organisation(rheinNeckarKreis)
				.name(ServicestationName.of("Wallbach"))
				.typ(ServicestationTyp.RADSERVICE_PUNKT_GROSS)
				.build());
	}

	@Test
	void doRun_zweiDurchlaeufeMitIdentischerTestJson_alleStationenImZweitenDurchlaufGeUpdatet() {
		// act
		Optional<JobStatistik> jobStatistikInitial = servicestationenImportJob.doRun();
		Optional<JobStatistik> jobStatistikZweiterDurchlauf = servicestationenImportJob.doRun();

		// assert
		ServicestationMobiDataImportStatistik mobiDataImportStatistikInitial = (ServicestationMobiDataImportStatistik) jobStatistikInitial
			.get();
		ServicestationMobiDataImportStatistik mobiDataImportStatistikZweiterDurchlauf = (ServicestationMobiDataImportStatistik) jobStatistikZweiterDurchlauf
			.get();

		assertThat(mobiDataImportStatistikInitial.anzahlGeloescht).isZero();
		assertThat(mobiDataImportStatistikInitial.anzahlGeupdated).isZero();
		assertThat(mobiDataImportStatistikInitial.anzahlAttributmappingFehlerhaft).isZero();

		assertThat(mobiDataImportStatistikInitial.anzahlNeuErstellt).isEqualTo(
			mobiDataImportStatistikZweiterDurchlauf.anzahlGeupdated);

		assertThat(mobiDataImportStatistikZweiterDurchlauf.anzahlGeloescht).isZero();
		assertThat(mobiDataImportStatistikZweiterDurchlauf.anzahlNeuErstellt).isZero();
		assertThat(mobiDataImportStatistikZweiterDurchlauf.anzahlAttributmappingFehlerhaft).isZero();
	}

	private static Point getPointInUtm32(Coordinate coordinateWGS84) throws FactoryException, TransformException {
		return GeometryTestdataProvider.createPoint(
			CoordinateReferenceSystemConverterUtility.transformCoordinate(coordinateWGS84,
				KoordinatenReferenzSystem.WGS84, KoordinatenReferenzSystem.ETRS89_UTM32_N));
	}
}