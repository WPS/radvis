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

package de.wps.radvis.backend.abstellanlage.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.abstellanlage.domain.entity.Abstellanlage;
import de.wps.radvis.backend.abstellanlage.domain.entity.AbstellanlageBRImportStatistik;
import de.wps.radvis.backend.abstellanlage.domain.entity.provider.AbstellanlageTestDataProvider;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBeschreibung;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenBetreiber;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenOrt;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenQuellSystem;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenStatus;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AbstellanlagenWeitereInformation;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.AnzahlStellplaetze;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.ExterneAbstellanlagenId;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Stellplatzart;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberdacht;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberwacht;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.domain.repository.CsvRepository;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;

@Tag("group5")
@ContextConfiguration(classes = { AbstellanlageBRImportJobTestIT.TestConfiguration.class, CommonConfiguration.class,
	GeoConverterConfiguration.class })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class })
class AbstellanlageBRImportJobTestIT extends DBIntegrationTestIT {

	private Abstellanlage anlageMitAttributenAusCsv;

	@EnableJpaRepositories(basePackages = { "de.wps.radvis.backend.abstellanlage" })
	@EntityScan(basePackages = { "de.wps.radvis.backend.abstellanlage", "de.wps.radvis.backend.netz",
		"de.wps.radvis.backend.common", "de.wps.radvis.backend.dokument" })
	public static class TestConfiguration {
	}

	@Mock
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	@Mock
	private JobConfigurationProperties jobConfigurationProperties;

	@Autowired
	private AbstellanlageRepository abstellanlageRepository;

	@Autowired
	private CsvRepository csvRepository;

	@Autowired
	private CoordinateReferenceSystemConverter converter;

	private AbstellanlageBRImportJob abstellanlageBRImportJob;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		abstellanlageBRImportJob = new AbstellanlageBRImportJob(jobExecutionDescriptionRepository,
			jobConfigurationProperties, csvRepository, converter, abstellanlageRepository);

		anlageMitAttributenAusCsv = new Abstellanlage(
			GeometryTestdataProvider.createPoint(new Coordinate(583112.10, 5423680.94)),
			AbstellanlagenBetreiber.of("Landratsamt Ostalbkreis"),
			ExterneAbstellanlagenId.of("INFRA-de:08136:2042-FAHRRADANLAGE-1"),
			AbstellanlagenQuellSystem.MOBIDATABW,
			null,
			AnzahlStellplaetze.of(16),
			null,
			null,
			Ueberwacht.UNBEKANNT,
			AbstellanlagenOrt.BIKE_AND_RIDE,
			null,
			Stellplatzart.ANLEHNBUEGEL,
			Ueberdacht.of(false),
			null,
			null,
			null,
			AbstellanlagenBeschreibung.of("Direkt neben Steig 1"),
			AbstellanlagenWeitereInformation.of("nicht an dieser Stelle testen"),
			AbstellanlagenStatus.AKTIV,
			new DokumentListe());

		File fileToImport = new File("src/test/resources/BFRK_Fahrradanlage_1Eintrag.csv");
		when(jobConfigurationProperties.getAbstellanlageBRImportUrlList()).thenReturn(List.of(
			fileToImport.toURI().toString()));
	}

	@Test
	void doRun_ausCsvDatei_neuAnlegen_richtigeAttribute() {
		// act
		Optional<JobStatistik> jobStatistik = abstellanlageBRImportJob.doRun();

		// assert
		List<Abstellanlage> abstellanlagen = StreamSupport.stream(abstellanlageRepository.findAll().spliterator(),
			false)
			.collect(Collectors.toList());
		assertThat(abstellanlagen).hasSize(1);
		assertThat(abstellanlagen.get(0))
			.usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringOverriddenEqualsForTypes(Abstellanlage.class)
			.ignoringFields("id", "version", "geometrie", "dokumentListe", "weitereInformation")
			.isEqualTo(anlageMitAttributenAusCsv);

		assertThat(abstellanlagen.get(0).getDokumentListe().getDokumente()).isEmpty();
		assertThat(abstellanlagen.get(0).getGeometrie().getCoordinate()
			.distance(anlageMitAttributenAusCsv.getGeometrie().getCoordinate()))
				.isLessThan(0.1);

		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik = (AbstellanlageBRImportStatistik) jobStatistik
			.get();
		assertThat(abstellanlageBRImportStatistik.anzahlNeuErstellt).isEqualTo(1);
	}

	@Test
	void doRun_ausCsvDatei_update_updatedVorhandeneAbstellanlage() {
		// arrange
		Abstellanlage mobiDataMitExtIdInCsv = abstellanlageRepository.save(
			AbstellanlageTestDataProvider.withDefaultValues()
				.quellSystem(AbstellanlagenQuellSystem.MOBIDATABW)
				.externeId(ExterneAbstellanlagenId.of("INFRA-de:08136:2042-FAHRRADANLAGE-1"))
				.build());

		// act
		Optional<JobStatistik> jobStatistik = abstellanlageBRImportJob.doRun();

		// assert
		List<Abstellanlage> abstellanlagen = StreamSupport.stream(abstellanlageRepository.findAll().spliterator(),
			false).collect(Collectors.toList());
		assertThat(abstellanlagen).hasSize(1);
		assertThat(abstellanlagen.get(0)).usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringOverriddenEqualsForTypes(Abstellanlage.class)
			.ignoringFields("id", "version", "geometrie", "dokumentListe", "weitereInformation")
			.isEqualTo(anlageMitAttributenAusCsv);
		assertThat(abstellanlagen.get(0).getGeometrie().getCoordinate()
			.distance(anlageMitAttributenAusCsv.getGeometrie().getCoordinate()))
				.isLessThan(0.1);
		assertThat(abstellanlagen.get(0).getId()).isEqualTo(mobiDataMitExtIdInCsv.getId());

		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik = (AbstellanlageBRImportStatistik) jobStatistik
			.get();
		assertThat(abstellanlageBRImportStatistik.anzahlGeupdated).isEqualTo(1);
	}

	@Test
	void doRun_ausCsvDatei_nichtImportierteMobiDataBWLoeschen() {
		// arrange
		Abstellanlage mobiDataMitExtIdNichtInCsv = abstellanlageRepository.save(
			AbstellanlageTestDataProvider.withDefaultValues()
				.quellSystem(AbstellanlagenQuellSystem.MOBIDATABW)
				.externeId(ExterneAbstellanlagenId.of("123ExterneId"))
				.build());
		Abstellanlage radvisMitExtIdNichtInCsv = abstellanlageRepository.save(
			AbstellanlageTestDataProvider.withDefaultValues()
				.quellSystem(AbstellanlagenQuellSystem.RADVIS)
				.externeId(ExterneAbstellanlagenId.of("123ExterneId"))
				.build());

		// act
		Optional<JobStatistik> jobStatistik = abstellanlageBRImportJob.doRun();

		// assert
		List<Abstellanlage> abstellanlagen = StreamSupport.stream(abstellanlageRepository.findAll().spliterator(),
			false).collect(Collectors.toList());
		assertThat(abstellanlagen).hasSize(2);
		assertThat(abstellanlagen.stream().map(AbstractEntity::getId)).doesNotContain(
			mobiDataMitExtIdNichtInCsv.getId());
		assertThat(abstellanlagen.stream().map(AbstractEntity::getId)).contains(radvisMitExtIdNichtInCsv.getId());

		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik = (AbstellanlageBRImportStatistik) jobStatistik
			.get();
		assertThat(abstellanlageBRImportStatistik.anzahlGeloescht).isEqualTo(1);
	}

	@Test
	void generateWeitereInformationen() {
		// act
		abstellanlageBRImportJob.doRun();

		// assert
		String htmlText = "<ul>\n"
			+ "<li><a href=\"https://mobidata-bw.de/linkZuAnlageFoto\" target=\"_blank\">Anlage</a></li>\n"
			+ "<li><a href=\"https://mobidata-bw.de/linkZuWegZurAnlageFoto\" target=\"_blank\">Weg zur Anlage</a></li>\n"
			+ "<li><a href=\"https://mobidata-bw.de/linkZuHindernisZufahrtFoto\" target=\"_blank\">Hinderniszufahrt</a></li>\n"
			+ "<li><a href=\"https://mobidata-bw.de/linkZuBesonderheitenFoto\" target=\"_blank\">Besonderheiten</a></li>\n"
			+ "</ul>";

		List<Abstellanlage> abstellanlagen = StreamSupport.stream(abstellanlageRepository.findAll().spliterator(),
			false)
			.collect(Collectors.toList());
		assertThat(abstellanlagen.get(0).getWeitereInformation()).isPresent();
		assertThat(abstellanlagen.get(0).getWeitereInformation().get()).isEqualTo(
			AbstellanlagenWeitereInformation.of(htmlText));
	}

	@Test
	void doRun_ausCsvDatei_DatenquelleRadVIS_wirdIgnoriert() {
		// arrange
		File fileToImport = new File("src/test/resources/BFRK_Fahrradanlage_1EintragMobiData_1EintragRadVIS.csv");
		when(jobConfigurationProperties.getAbstellanlageBRImportUrlList()).thenReturn(List.of(
			fileToImport.toURI().toString()));

		// act
		Optional<JobStatistik> jobStatistik = abstellanlageBRImportJob.doRun();

		// assert
		List<Abstellanlage> abstellanlagen = StreamSupport.stream(abstellanlageRepository.findAll().spliterator(),
			false)
			.collect(Collectors.toList());
		assertThat(abstellanlagen).hasSize(1);
		assertThat(abstellanlagen.get(0))
			.usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringOverriddenEqualsForTypes(Abstellanlage.class)
			.ignoringFields("id", "version", "geometrie", "dokumentListe", "weitereInformation")
			.isEqualTo(anlageMitAttributenAusCsv);

		assertThat(abstellanlagen.get(0).getDokumentListe().getDokumente()).isEmpty();
		assertThat(abstellanlagen.get(0).getGeometrie().getCoordinate()
			.distance(anlageMitAttributenAusCsv.getGeometrie().getCoordinate()))
				.isLessThan(0.1);

		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik = (AbstellanlageBRImportStatistik) jobStatistik
			.get();
		assertThat(abstellanlageBRImportStatistik.anzahlNeuErstellt).isEqualTo(1);
		assertThat(abstellanlageBRImportStatistik.anzahlRadVISAbstellanlagenUebersprungen).isEqualTo(1);
	}

	@Test
	void koordinatenTransformFehlgeschlagen_nurEinEintragNichtGelesen() {
		File fileToImport = new File("src/test/resources/BFRK_Fahrradanlage_2Eintrag_falscheKoordinaten.csv");
		when(jobConfigurationProperties.getAbstellanlageBRImportUrlList()).thenReturn(List.of(
			fileToImport.toURI().toString()));

		// act
		abstellanlageBRImportJob.doRun();

		// act
		Optional<JobStatistik> jobStatistik = abstellanlageBRImportJob.doRun();

		// assert
		List<Abstellanlage> abstellanlagen = StreamSupport.stream(abstellanlageRepository.findAll().spliterator(),
			false).collect(Collectors.toList());
		assertThat(abstellanlagen).hasSize(1);

		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik = (AbstellanlageBRImportStatistik) jobStatistik
			.get();
		assertThat(abstellanlageBRImportStatistik.anzahlAbstellanlagenAttributmappingFehlerhaft).isEqualTo(1);
	}

	@Test
	void doRun_ausCsvDatei_neuAnlegen_richtigeAttribute_falseQuotationMarksInCsv() {
		/*
		 * Wenn in der QuellCsvDatei steht: einige defekte Fahrräder dort ""vergessen"
		 * Wird das eingelesen zu: einige defekte Fahrräder dort "vergessen
		 * An dieser Stelle ist die CSV-Datei Fehlerhaft und am Ende fehlt das zweite
		 * Anführungszeichen, wir müssen aber damit zurecht kommen koennen.
		 */

		// arrange
		File fileToImport = new File("src/test/resources/BFRK_Fahrradanlage_1Eintrag_falseQuotation.csv");
		when(jobConfigurationProperties.getAbstellanlageBRImportUrlList()).thenReturn(List.of(
			fileToImport.toURI().toString()));

		// act
		Optional<JobStatistik> jobStatistik = abstellanlageBRImportJob.doRun();

		// assert
		Abstellanlage expectedAbstellanlage = new Abstellanlage(
			GeometryTestdataProvider.createPoint(new Coordinate(583112.10, 5423680.94)),
			AbstellanlagenBetreiber.of("Landratsamt Ostalbkreis"),
			ExterneAbstellanlagenId.of("INFRA-de:08136:2042-FAHRRADANLAGE-1"),
			AbstellanlagenQuellSystem.MOBIDATABW,
			null,
			AnzahlStellplaetze.of(16),
			null,
			null,
			Ueberwacht.UNBEKANNT,
			AbstellanlagenOrt.BIKE_AND_RIDE,
			null,
			Stellplatzart.ANLEHNBUEGEL,
			Ueberdacht.of(false),
			null,
			null,
			null,
			AbstellanlagenBeschreibung.of("einige defekte Fahrräder dort \"vergessen"),
			AbstellanlagenWeitereInformation.of("nicht an dieser Stelle testen"),
			AbstellanlagenStatus.AKTIV,
			new DokumentListe());

		List<Abstellanlage> abstellanlagen = StreamSupport.stream(abstellanlageRepository.findAll().spliterator(),
			false)
			.collect(Collectors.toList());
		assertThat(abstellanlagen).hasSize(1);
		assertThat(abstellanlagen.get(0))
			.usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringOverriddenEqualsForTypes(Abstellanlage.class)
			.ignoringFields("id", "version", "geometrie", "dokumentListe", "weitereInformation")
			.isEqualTo(expectedAbstellanlage);

		assertThat(abstellanlagen.get(0).getDokumentListe().getDokumente()).isEmpty();
		assertThat(abstellanlagen.get(0).getGeometrie().getCoordinate()
			.distance(expectedAbstellanlage.getGeometrie().getCoordinate()))
				.isLessThan(0.1);

		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik = (AbstellanlageBRImportStatistik) jobStatistik
			.get();
		assertThat(abstellanlageBRImportStatistik.anzahlNeuErstellt).isEqualTo(1);
	}
}