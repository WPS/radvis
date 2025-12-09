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

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections4.IterableUtils;
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
import de.wps.radvis.backend.abstellanlage.domain.valueObject.MobiDataQuellId;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Stellplatzart;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberdacht;
import de.wps.radvis.backend.abstellanlage.domain.valueObject.Ueberwacht;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.AbstractEntity;
import de.wps.radvis.backend.common.domain.entity.JobStatistik;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;

@Tag("group5")
@ContextConfiguration(classes = { AbstellanlageBRImportJobTestIT.TestConfiguration.class, CommonConfiguration.class,
	GeoConverterConfiguration.class })
@EnableConfigurationProperties(value = { CommonConfigurationProperties.class })
class AbstellanlageBRImportJobTestIT extends DBIntegrationTestIT {

	@Mock
	JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	private Abstellanlage anlageMitAttributenAusJson;

	@Autowired
	private AbstellanlageRepository abstellanlageRepository;
	@Autowired
	private CoordinateReferenceSystemConverter converter;

	AbstellanlageBRImportJob setupJob(File abtellanlagenJson) {
		return new AbstellanlageBRImportJob(
			jobExecutionDescriptionRepository,
			converter,
			abstellanlageRepository,
			new File("src/test/resources/parkapi-sources.json").toURI().toString(),
			abtellanlagenJson.toURI().toString()
		);
	}

	AbstellanlageBRImportJob setupJob() {
		return setupJob(new File("src/test/resources/parkapi-parking-sites-purpose-bike-1Eintrag.json"));
	}

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		anlageMitAttributenAusJson = Abstellanlage.builder()
			.geometrie(GeometryTestdataProvider.createPoint(new Coordinate(583112.10, 5423680.94)))
			.betreiber(AbstellanlagenBetreiber.of("Landratsamt Ostalbkreis"))
			.externeId(ExterneAbstellanlagenId.of("INFRA-de:08136:2042-FAHRRADANLAGE-1"))
			.quellSystem(AbstellanlagenQuellSystem.MOBIDATABW)
			.mobiDataQuellId(MobiDataQuellId.of(38))
			.zustaendig(null)
			.anzahlStellplaetze(AnzahlStellplaetze.of(16))
			.anzahlSchliessfaecher(null)
			.anzahlLademoeglichkeiten(null)
			.ueberwacht(Ueberwacht.UNBEKANNT)
			.abstellanlagenOrt(AbstellanlagenOrt.BIKE_AND_RIDE)
			.groessenklasse(null)
			.stellplatzart(Stellplatzart.ANLEHNBUEGEL)
			.ueberdacht(Ueberdacht.of(false))
			.beschreibung(AbstellanlagenBeschreibung.of("Direkt neben Steig 1"))
			.weitereInformation(AbstellanlagenWeitereInformation.of("nicht an dieser Stelle testen"))
			.status(AbstellanlagenStatus.AKTIV)
			.dokumentListe(new DokumentListe())
			.build();
	}

	@Test
	void doRun_ausJsonDatei_neuAnlegen_richtigeAttribute() {
		// act
		AbstellanlageBRImportJob abstellanlageBRImportJob = setupJob();
		Optional<JobStatistik> jobStatistik = abstellanlageBRImportJob.doRun();

		// assert
		List<Abstellanlage> abstellanlagen = IterableUtils.toList(abstellanlageRepository.findAll());

		assertThat(abstellanlagen).hasSize(1);
		assertThat(abstellanlagen.get(0))
			.usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringOverriddenEqualsForTypes(Abstellanlage.class)
			.ignoringFields("id", "version", "geometrie", "dokumentListe", "weitereInformation")
			.isEqualTo(anlageMitAttributenAusJson);

		assertThat(abstellanlagen.get(0).getDokumentListe().getDokumente()).isEmpty();
		assertThat(abstellanlagen.get(0).getGeometrie().getCoordinate()
			.distance(anlageMitAttributenAusJson.getGeometrie().getCoordinate()))
				.isLessThan(0.1);

		assertThat(jobStatistik).isPresent();
		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik = (AbstellanlageBRImportStatistik) jobStatistik
			.get();
		assertThat(abstellanlageBRImportStatistik.anzahlNeuErstellt).isEqualTo(1);
	}

	@Test
	void doRun_ausJsonDatei_update_updatedVorhandeneAbstellanlage() {
		// arrange
		AbstellanlageBRImportJob abstellanlageBRImportJob = setupJob();
		Abstellanlage mobiDataMitExtIdUndSourceIdInCsv = abstellanlageRepository.save(
			AbstellanlageTestDataProvider.withDefaultValues()
				.quellSystem(AbstellanlagenQuellSystem.MOBIDATABW)
				.externeId(ExterneAbstellanlagenId.of("INFRA-de:08136:2042-FAHRRADANLAGE-1"))
				.mobiDataQuellId(MobiDataQuellId.of(38))
				.build());

		// act
		Optional<JobStatistik> jobStatistik = abstellanlageBRImportJob.doRun();

		// assert
		List<Abstellanlage> abstellanlagen = IterableUtils.toList(abstellanlageRepository.findAll());

		assertThat(abstellanlagen).hasSize(1);
		assertThat(abstellanlagen.get(0)).usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringOverriddenEqualsForTypes(Abstellanlage.class)
			.ignoringFields("id", "version", "geometrie", "dokumentListe", "weitereInformation")
			.isEqualTo(anlageMitAttributenAusJson);
		assertThat(abstellanlagen.get(0).getGeometrie().getCoordinate()
			.distance(anlageMitAttributenAusJson.getGeometrie().getCoordinate()))
				.isLessThan(0.1);
		assertThat(abstellanlagen.get(0).getId()).isEqualTo(mobiDataMitExtIdUndSourceIdInCsv.getId());

		assertThat(jobStatistik).isPresent();
		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik = (AbstellanlageBRImportStatistik) jobStatistik
			.get();
		assertThat(abstellanlageBRImportStatistik.anzahlGeupdated).isEqualTo(1);
	}

	@Test
	void doRun_ausJsonDatei_nichtImportierteMobiDataBWLoeschen() {
		// arrange
		AbstellanlageBRImportJob abstellanlageBRImportJob = setupJob();
		Abstellanlage mobiDataMitExtIdNichtInCsv = abstellanlageRepository.save(
			AbstellanlageTestDataProvider.withDefaultValues()
				.quellSystem(AbstellanlagenQuellSystem.MOBIDATABW)
				.externeId(ExterneAbstellanlagenId.of("123ExterneId"))
				.mobiDataQuellId(MobiDataQuellId.of(123))
				.build());
		Abstellanlage radvisMitExtIdNichtInCsv = abstellanlageRepository.save(
			AbstellanlageTestDataProvider.withDefaultValues()
				.quellSystem(AbstellanlagenQuellSystem.RADVIS)
				.externeId(ExterneAbstellanlagenId.of("456ExterneId"))
				.build());

		// act
		Optional<JobStatistik> jobStatistik = abstellanlageBRImportJob.doRun();

		// assert
		List<Abstellanlage> abstellanlagen = IterableUtils.toList(abstellanlageRepository.findAll());

		assertThat(abstellanlagen).hasSize(2);
		assertThat(abstellanlagen.stream().map(AbstractEntity::getId))
			.doesNotContain(mobiDataMitExtIdNichtInCsv.getId())
			.contains(radvisMitExtIdNichtInCsv.getId());

		assertThat(jobStatistik).isPresent();
		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik = (AbstellanlageBRImportStatistik) jobStatistik
			.get();
		assertThat(abstellanlageBRImportStatistik.anzahlGeloescht).isEqualTo(1);
	}

	@Test
	void generateWeitereInformationen() {
		// act
		AbstellanlageBRImportJob abstellanlageBRImportJob = setupJob();
		abstellanlageBRImportJob.doRun();

		// assert
		String htmlText = """
			<ul>
			<li><a href="https://mobidata-bw.de/infosZuAnlage" target="_blank">Weitere Informationen zur Anlage</a></li>
			<li><a href="https://mobidata-bw.de/linkZuAnlageFoto" target="_blank">Foto der Anlage</a></li>
			</ul>""";

		List<Abstellanlage> abstellanlagen = IterableUtils.toList(abstellanlageRepository.findAll());

		assertThat(abstellanlagen.get(0).getWeitereInformation()).isPresent()
			.contains(AbstellanlagenWeitereInformation.of(htmlText));
	}

	@Test
	void doRun_ausJsonDatei_DatenquelleRadVIS_wirdIgnoriert() {
		// arrange
		File fileToImport = new File(
			"src/test/resources/parkapi-parking-sites-purpose-bike-1EintragMobiData_1EintragRadvis.json");

		AbstellanlageBRImportJob abstellanlageBRImportJob = setupJob(fileToImport);

		// act
		Optional<JobStatistik> jobStatistik = abstellanlageBRImportJob.doRun();

		// assert
		List<Abstellanlage> abstellanlagen = IterableUtils.toList(abstellanlageRepository.findAll());

		assertThat(abstellanlagen).hasSize(1);
		assertThat(abstellanlagen.get(0))
			.usingRecursiveComparison()
			.usingOverriddenEquals()
			.ignoringOverriddenEqualsForTypes(Abstellanlage.class)
			.ignoringFields("id", "version", "geometrie", "dokumentListe", "weitereInformation")
			.isEqualTo(anlageMitAttributenAusJson);

		assertThat(abstellanlagen.get(0).getDokumentListe().getDokumente()).isEmpty();
		assertThat(abstellanlagen.get(0).getGeometrie().getCoordinate()
			.distance(anlageMitAttributenAusJson.getGeometrie().getCoordinate()))
				.isLessThan(0.1);

		assertThat(jobStatistik).isPresent();
		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik = (AbstellanlageBRImportStatistik) jobStatistik
			.get();
		assertThat(abstellanlageBRImportStatistik.anzahlNeuErstellt).isEqualTo(1);
		assertThat(abstellanlageBRImportStatistik.anzahlRadVISAbstellanlagenUebersprungen).isEqualTo(1);
	}

	@Test
	void koordinatenTransformFehlgeschlagen_nurEinEintragNichtGelesen() {
		File fileToImport = new File(
			"src/test/resources/parkapi-parking-sites-purpose-bike-2Eintraege_1falscheKoordinate.json");

		AbstellanlageBRImportJob abstellanlageBRImportJob = setupJob(fileToImport);

		// act
		Optional<JobStatistik> jobStatistik = abstellanlageBRImportJob.doRun();

		// assert
		List<Abstellanlage> abstellanlagen = IterableUtils.toList(abstellanlageRepository.findAll());

		assertThat(abstellanlagen).hasSize(1);

		assertThat(jobStatistik).isPresent();
		AbstellanlageBRImportStatistik abstellanlageBRImportStatistik = (AbstellanlageBRImportStatistik) jobStatistik
			.get();
		assertThat(abstellanlageBRImportStatistik.anzahlAbstellanlagenAttributmappingFehlerhaft).isEqualTo(1);
	}

	@Test
	void doRun_betreiberNichtHinterlegt_UnbekanntAlsBetreiber() {
		File fileToImport = new File(
			"src/test/resources/parkapi-parking-sites-purpose-bike-1Eintrag_keinOperator.json");
		AbstellanlageBRImportJob abstellanlageBRImportJob = setupJob(fileToImport);

		// act
		abstellanlageBRImportJob.doRun();

		// assert
		List<Abstellanlage> abstellanlagen = IterableUtils.toList(abstellanlageRepository.findAll());

		assertThat(abstellanlagen).hasSize(1);
		assertThat(abstellanlagen.get(0))
			.usingRecursiveComparison()
			.ignoringFields("id", "version", "geometrie", "dokumentListe", "weitereInformation", "betreiber")
			.isEqualTo(anlageMitAttributenAusJson);

		assertThat(abstellanlagen.get(0).getBetreiber()).isEqualTo(
			AbstellanlagenBetreiber.of("Unbekannt"));
	}

	@Test
	void doRun_selbeExterneId_wirdNachMobiDataQuellIdDisambiguiert() {
		File fileToImport = new File(
			"src/test/resources/parkapi-parking-sites-purpose-bike-2Eintraege_selbeExterneId_andereDatenquelle.json");
		AbstellanlageBRImportJob abstellanlageBRImportJob = setupJob(fileToImport);

		// act
		abstellanlageBRImportJob.doRun();

		// assert
		List<Abstellanlage> abstellanlagen = IterableUtils.toList(abstellanlageRepository.findAll());

		assertThat(abstellanlagen).hasSize(2);
		assertThat(abstellanlagen).extracting(Abstellanlage::getMobiDataQuellId).extracting(Optional::get)
			.containsExactlyInAnyOrder(MobiDataQuellId.of(18L), MobiDataQuellId.of(38L));

		assertThat(abstellanlagen).extracting(Abstellanlage::getExterneId).extracting(Optional::get)
			.containsOnly(ExterneAbstellanlagenId.of("123"));
	}

	@Test
	void doRun_jsonInvalide_earlyExit() {
		/*
		 * Wenn das Json kaputt ist, versuchen wir das nicht zu parsen...
		 */

		// arrange
		File fileToImport = new File(
			"src/test/resources/parkapi-parking-sites-purpose-bike-1Eintrag_jsonNichtValide.json");
		AbstellanlageBRImportJob abstellanlageBRImportJob = setupJob(fileToImport);

		// act
		Optional<JobStatistik> jobStatistik = abstellanlageBRImportJob.doRun();

		// assert
		List<Abstellanlage> abstellanlagen = IterableUtils.toList(abstellanlageRepository.findAll());

		assertThat(abstellanlagen).isEmpty();
		AbstellanlageBRImportStatistik expectedStatistik = new AbstellanlageBRImportStatistik();
		expectedStatistik.urlOderGeojsonFehlerhaft = true;
		assertThat(jobStatistik).isPresent();
		assertThat((AbstellanlageBRImportStatistik) jobStatistik.get())
			.usingRecursiveComparison()
			.isEqualTo(expectedStatistik);
	}

	@EnableJpaRepositories(basePackages = { "de.wps.radvis.backend.abstellanlage" })
	@EntityScan(basePackages = { "de.wps.radvis.backend.abstellanlage", "de.wps.radvis.backend.netz",
		"de.wps.radvis.backend.common", "de.wps.radvis.backend.dokument" })
	public static class TestConfiguration {
	}
}