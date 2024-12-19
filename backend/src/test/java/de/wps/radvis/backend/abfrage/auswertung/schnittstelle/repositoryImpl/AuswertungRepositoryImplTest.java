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

package de.wps.radvis.backend.abfrage.auswertung.schnittstelle.repositoryImpl;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.abfrage.auswertung.AuswertungConfiguration;
import de.wps.radvis.backend.abfrage.auswertung.domain.entity.AuswertungsFilter;
import de.wps.radvis.backend.abfrage.auswertung.domain.repository.AuswertungRepository;
import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.NetzConfigurationProperties;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.provider.FuehrungsformAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.ZustaendigkeitAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.BelagArt;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group1")
@ContextConfiguration(classes = {
	AuswertungConfiguration.class,
	NetzConfiguration.class,
	OrganisationConfiguration.class,
	GeoConverterConfiguration.class,
	CommonConfiguration.class,
	BenutzerConfiguration.class
})
@EnableConfigurationProperties(value = {
	CommonConfigurationProperties.class,
	FeatureToggleProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class,
	NetzConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
class AuswertungRepositoryImplTestIT extends DBIntegrationTestIT {
	private static final GeometryFactory GEO_FACTORY = KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory();
	public static final Offset<BigInteger> TEST_OFFSET = Offset.offset(BigInteger.TWO);

	@Autowired
	private AuswertungRepository auswertungRepository;

	@Autowired
	private KantenRepository kantenRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@PersistenceContext
	EntityManager entityManager;

	@Test
	void testGetKmAnzahl_addiertLaengenKorrekt() {
		// Arrange
		Kante kante1 = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.quelle(QuellSystem.DLM)
			.build();
		Kante kante2 = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40))
			.quelle(QuellSystem.DLM)
			.build();
		Kante kante3 = getKanteBuilder(new Coordinate(31, 31), new Coordinate(40, 40))
			.quelle(QuellSystem.DLM)
			.build();
		kantenRepository.save(kante1);
		kantenRepository.save(kante2);
		kantenRepository.save(kante3);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder().build());

		// Assert
		assertThat(result).isNotZero();
		BigInteger expected = convertToBigIntInCm(kante1.getGeometry().getLength() + kante2.getGeometry().getLength()
			+ kante3.getGeometry().getLength());
		assertThat(result).isCloseTo(expected, TEST_OFFSET);

	}

	@Test
	void testGetKmAnzahl_beziehtNurKantenMitStatusUNTER_VERKEHRein() {
		// Arrange
		Kante kanteUnterVerkehr = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue().build())
			.quelle(QuellSystem.DLM)
			.build();
		Kante kanteImBau = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.kantenAttribute(KantenAttribute.builder()
					.status(Status.IN_BAU)
					.build())
				.build())
			.quelle(QuellSystem.DLM)
			.build();
		Kante kanteNichtMitDemRadBefahrbar = getKanteBuilder(new Coordinate(31, 31), new Coordinate(40, 40))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.kantenAttribute(KantenAttribute.builder()
					.status(Status.NICHT_MIT_RAD_BEFAHRBAR)
					.build())
				.build())
			.quelle(QuellSystem.DLM)
			.build();
		Kante kanteFiktiv = getKanteBuilder(new Coordinate(31, 31), new Coordinate(40, 40))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.kantenAttribute(KantenAttribute.builder()
					.status(Status.FIKTIV)
					.build())
				.build())
			.quelle(QuellSystem.DLM)
			.build();
		Kante kanteKonzeption = getKanteBuilder(new Coordinate(31, 31), new Coordinate(40, 40))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.kantenAttribute(KantenAttribute.builder()
					.status(Status.KONZEPTION)
					.build())
				.build())
			.quelle(QuellSystem.DLM)
			.build();
		Kante kanteNichtFuerRadvehrkehrFreigegeben = getKanteBuilder(new Coordinate(31, 31), new Coordinate(40, 40))
			.kantenAttributGruppe(KantenAttributGruppeTestDataProvider.defaultValue()
				.kantenAttribute(KantenAttribute.builder()
					.status(Status.NICHT_FUER_RADVERKEHR_FREIGEGEBEN)
					.build())
				.build())
			.quelle(QuellSystem.DLM)
			.build();

		kantenRepository.save(kanteUnterVerkehr);
		kantenRepository.save(kanteImBau);
		kantenRepository.save(kanteNichtMitDemRadBefahrbar);
		kantenRepository.save(kanteFiktiv);
		kantenRepository.save(kanteKonzeption);
		kantenRepository.save(kanteNichtFuerRadvehrkehrFreigegeben);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder().build());

		// Assert
		assertThat(result).isNotZero();
		BigInteger expected = convertToBigIntInCm(kanteUnterVerkehr.getGeometry().getLength());
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void testGetKmAnzahl_zaehltZweisetigeKantenDoppelt() {
		// Arrange
		Kante kante1 = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.isZweiseitig(true)
			.quelle(QuellSystem.DLM)
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppe.builder().isZweiseitig(true).build())
			.fahrtrichtungAttributGruppe(FahrtrichtungAttributGruppe.builder().isZweiseitig(true).build())
			.build();
		Kante kante2 = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40))
			.quelle(QuellSystem.DLM)
			.build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder().build());

		// Assert
		assertThat(result).isNotZero();
		BigInteger expected = convertToBigIntInCm(
			kante1.getGeometry().getLength() * 2 + kante2.getGeometry().getLength());
		assertThat(result).isCloseTo(expected, TEST_OFFSET);

	}

	@Test
	void testGetKmAnzahl_filterTrifftAufKeineKanteZu_liefert0() {
		// Arrange
		Kante kante1 = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.quelle(QuellSystem.DLM)
			.build();
		Kante kante2 = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40))
			.quelle(QuellSystem.DLM)
			.build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder().netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG)).build());

		// Assert
		assertThat(result).isZero();
	}

	@Test
	void testGetKmAnzahl_alleFilterGesetzt() {
		// Arrange
		Gebietskoerperschaft baulastTraeger = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.name("baulastTraeger")
			.uebergeordneteOrganisation(null)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(21, 1, 40, 40))
			.build();

		Gebietskoerperschaft gemeinde = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.name("gemeinde")
			.uebergeordneteOrganisation(null)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(1, 1, 20, 40))
			.build();

		Verwaltungseinheit gemeindeSaved = gebietskoerperschaftRepository.save(gemeinde);
		Verwaltungseinheit baulastSaved = gebietskoerperschaftRepository.save(baulastTraeger);

		Kante kanteDLMOhneRadNETZKlasseInnerhalbGemeinde = getKanteBuilder(new Coordinate(1, 10),
			new Coordinate(2, 20))
				.quelle(QuellSystem.DLM)
				.build();

		List<FuehrungsformAttribute> fuehrungsformAttribute = List.of(
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.2))
				.build(),
			FuehrungsformAttribute.builder()
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.5))
				.build(),
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
				.build());
		// Baulasttraeger: 0 - 0.4 & 0.8 - 1
		// Radverkehrsfuehrung.BEGEGNUNBSZONE: 0 - 0.2 & 0.5 - 1
		// => nur 0-0.2 & 0.8-1 ist beides korrekt, daher 0.4
		Kante kanteRadNETZMitBasisstandardInnerhalbGemeindeMitBaulasttraegerPlusRadverkehrsfuehrungAnteil0_4 = getKanteBuilder(
			new Coordinate(1, 10), new Coordinate(2, 20),
			Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT), Set.of(IstStandard.BASISSTANDARD))
				.zustaendigkeitAttributGruppe(
					ZustaendigkeitAttributGruppeTestDataProvider
						.withLeereGrundnetzAttribute()
						.zustaendigkeitAttribute(
							List.of(
								ZustaendigkeitAttribute.builder()
									.baulastTraeger(baulastTraeger)
									.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
									.build(),
								ZustaendigkeitAttribute.builder()
									.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 0.8))
									.build(),
								ZustaendigkeitAttribute.builder()
									.baulastTraeger(baulastTraeger)
									.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.8, 1))
									.build()))
						.build())
				.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
					.fuehrungsformAttributeLinks(fuehrungsformAttribute)
					.fuehrungsformAttributeRechts(fuehrungsformAttribute)
					.build())
				.quelle(QuellSystem.DLM)
				.build();
		Kante kanteRadNETZMitBasisstandardInnerhalbGemeindeOhneBaulasttraeger = getKanteBuilder(new Coordinate(1, 10),
			new Coordinate(2, 50), Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT),
			Set.of(IstStandard.BASISSTANDARD))
				.quelle(QuellSystem.DLM)
				.build();
		Kante kanteRadNETZMitBasisstandardAusserhalbGemeinde = getKanteBuilder(new Coordinate(31, 10),
			new Coordinate(40, 20), Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT),
			Set.of(IstStandard.BASISSTANDARD))
				.quelle(QuellSystem.RadVis).dlmId(null)
				.build();
		Kante kanteRadvisOhneRadNETZKLasseInnerhalbGmeinde = getKanteBuilder(new Coordinate(1, 10),
			new Coordinate(2, 40))
				.quelle(QuellSystem.RadVis).dlmId(null)
				.build();
		Kante kanteRadvisMitRadNETZKLasseUndStandardInnerhalbGemeinde = getKanteBuilder(new Coordinate(1, 10),
			new Coordinate(2, 40), Set.of(Netzklasse.RADNETZ_ALLTAG), Set.of(IstStandard.BASISSTANDARD))
				.quelle(QuellSystem.RadVis).dlmId(null)
				.build();

		Kante kanteRadNETZAusserhalbGemeinde = getKanteBuilder(new Coordinate(31, 31), new Coordinate(40, 40))
			.quelle(QuellSystem.RadVis).dlmId(null)
			.build();
		kantenRepository.save(kanteDLMOhneRadNETZKlasseInnerhalbGemeinde);
		kantenRepository.save(
			kanteRadNETZMitBasisstandardInnerhalbGemeindeMitBaulasttraegerPlusRadverkehrsfuehrungAnteil0_4);
		kantenRepository.save(kanteRadNETZMitBasisstandardInnerhalbGemeindeOhneBaulasttraeger);
		kantenRepository.save(kanteRadNETZMitBasisstandardAusserhalbGemeinde);
		kantenRepository.save(kanteRadvisOhneRadNETZKLasseInnerhalbGmeinde);
		kantenRepository.save(kanteRadvisMitRadNETZKLasseUndStandardInnerhalbGemeinde);
		kantenRepository.save(kanteRadNETZAusserhalbGemeinde);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder()
				.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG))
				.istStandards(Set.of(IstStandard.BASISSTANDARD))
				.gemeindeKreisBezirkId(gemeindeSaved.getId())
				.baulast(baulastSaved)
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.build());

		// Assert
		assertThat(result).isNotZero();
		BigInteger expected = convertToBigIntInCm(
			kanteRadNETZMitBasisstandardInnerhalbGemeindeMitBaulasttraegerPlusRadverkehrsfuehrungAnteil0_4.getGeometry()
				.getLength() * 0.4);
		assertThat(result).isCloseTo(expected, TEST_OFFSET);

	}

	@Test
	void testGetKmAnzahl_betrachtetNurGrundnetzkanten() {
		// Arrange

		Kante kanteDLMOhneRadNETZKlasse = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.quelle(QuellSystem.DLM)
			.build();
		Kante kanteRadNETZMitRadNETZKlasse = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20),
			Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT), Collections.emptySet())
				.quelle(QuellSystem.RadNETZ).dlmId(null)
				.build();
		Kante kanteRadvisOhneRadNETZKLasse = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40))
			.quelle(QuellSystem.RadVis).dlmId(null)
			.build();
		Kante kanteLGLMitRadNETZKLasse = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40),
			Set.of(Netzklasse.RADNETZ_ZIELNETZ), Collections.emptySet())
				.quelle(QuellSystem.LGL).dlmId(null)
				.build();

		Kante kanteRadVis = getKanteBuilder(new Coordinate(31, 31), new Coordinate(40, 40))
			.quelle(QuellSystem.RadVis).dlmId(null)
			.build();
		kantenRepository.save(kanteDLMOhneRadNETZKlasse);
		kantenRepository.save(kanteRadNETZMitRadNETZKlasse);
		kantenRepository.save(kanteRadvisOhneRadNETZKLasse);
		kantenRepository.save(kanteLGLMitRadNETZKLasse);
		kantenRepository.save(kanteRadVis);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder().build());

		// Assert
		assertThat(result).isNotZero();
		BigInteger expected = convertToBigIntInCm(
			kanteDLMOhneRadNETZKlasse.getGeometry().getLength() + kanteRadvisOhneRadNETZKLasse.getGeometry()
				.getLength()
				+ kanteRadVis.getGeometry().getLength());
		assertThat(result).isCloseTo(expected, TEST_OFFSET);

	}

	@Test
	void testGetKmAnzahl_gemeindeGesetzt_betrachtetKorrekteGemeinden() {
		// Arrange
		Gebietskoerperschaft gemeinde1 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.name("gemeinde1")
			.uebergeordneteOrganisation(null)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(1, 1, 20, 40))
			.build();

		Gebietskoerperschaft gemeinde2 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.organisationsArt(OrganisationsArt.GEMEINDE)
			.name("gemeinde2")
			.uebergeordneteOrganisation(null)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(21, 1, 40, 40))
			.build();

		Long gemeinde1Id = gebietskoerperschaftRepository.save(gemeinde1).getId();
		Long gemeinde2Id = gebietskoerperschaftRepository.save(gemeinde2).getId();

		Kante kante1 = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.quelle(QuellSystem.DLM)
			.build();
		Kante kante2 = getKanteBuilder(new Coordinate(1, 10), new Coordinate(40, 40))
			.quelle(QuellSystem.DLM)
			.build();
		Kante kante3 = getKanteBuilder(new Coordinate(31, 31), new Coordinate(40, 40))
			.quelle(QuellSystem.DLM)
			.build();

		kantenRepository.save(kante1);
		kantenRepository.save(kante2);
		kantenRepository.save(kante3);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger resultGemeinde1 = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder().gemeindeKreisBezirkId(gemeinde1Id).build());

		BigInteger resultGemeinde2 = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder().gemeindeKreisBezirkId(gemeinde2Id).build());

		// Assert
		assertThat(resultGemeinde1).isNotZero();
		BigInteger expected1 = convertToBigIntInCm(kante1.getGeometry().getLength() + kante2.getGeometry().getLength());
		assertThat(resultGemeinde1).isCloseTo(expected1, TEST_OFFSET);

		assertThat(resultGemeinde2).isNotZero();
		BigInteger expected2 = convertToBigIntInCm(kante2.getGeometry().getLength() + kante3.getGeometry().getLength());
		assertThat(resultGemeinde2).isCloseTo(expected2, TEST_OFFSET);
	}

	@Test
	void testGetKmAnzahl_netzklasseGesetzt_betrachtetKorrekteNetzklassen() {
		// Arrange
		Kante kanteQuelleRadNETZFreizeitUndAlltag = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20),
			Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT), Collections.emptySet())
				.quelle(QuellSystem.RadVis).dlmId(null)
				.build();

		Kante kanteQuelleRadNETZZielnetz = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20),
			Set.of(Netzklasse.RADNETZ_ZIELNETZ), Collections.emptySet())
				.quelle(QuellSystem.RadVis).dlmId(null)
				.build();

		Kante kanteKreisNetzFreizeit = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40),
			Set.of(Netzklasse.KREISNETZ_FREIZEIT), Collections.emptySet())
				.quelle(QuellSystem.DLM)
				.build();

		Kante kanteKreisNetzAlltag = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40),
			Set.of(Netzklasse.KREISNETZ_ALLTAG), Collections.emptySet())
				.quelle(QuellSystem.DLM)
				.build();

		kantenRepository.save(kanteQuelleRadNETZFreizeitUndAlltag);
		kantenRepository.save(kanteQuelleRadNETZZielnetz);
		kantenRepository.save(kanteKreisNetzFreizeit);
		kantenRepository.save(kanteKreisNetzAlltag);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder()
				.netzklassen(
					Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT, Netzklasse.KREISNETZ_FREIZEIT))
				.build());

		// Assert
		assertThat(result).isNotZero();
		BigInteger expected = convertToBigIntInCm(
			kanteQuelleRadNETZFreizeitUndAlltag.getGeometry().getLength() + kanteKreisNetzAlltag.getGeometry()
				.getLength());
		assertThat(result).isCloseTo(expected, TEST_OFFSET);

	}

	@Test
	void testGetKmAnzahl_nichtKlassifiziertGesetzt_betrachtetKorrekteNetzklassen() {
		// Arrange
		Kante kanteRadNETZ = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20),
			Set.of(Netzklasse.RADNETZ_ALLTAG), Collections.emptySet())
				.quelle(QuellSystem.DLM)
				.build();

		Kante kanteNichtKlassifiziert = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40))
			.quelle(QuellSystem.DLM)
			.build();

		kantenRepository.save(kanteRadNETZ);
		kantenRepository.save(kanteNichtKlassifiziert);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act

		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder().beachteNichtKlassifizierteKanten(true).build());

		// Assert
		assertThat(result).isNotZero();
		assertThat(result).isCloseTo(
			convertToBigIntInCm(kanteNichtKlassifiziert.getGeometry().getLength()), TEST_OFFSET);

	}

	@Test
	void testGetKmAnzahl_istStandardGesetzt_betrachtetKorrekteStandards() {
		// Arrange
		Kante kanteBasisstandardUndRadNETZStartstandard = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20),
			Collections.emptySet(), Set.of(IstStandard.BASISSTANDARD, IstStandard.STARTSTANDARD_RADNETZ))
				.quelle(QuellSystem.DLM)
				.build();

		Kante kanteBasisStandard = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20), Collections.emptySet(),
			Set.of(IstStandard.BASISSTANDARD))
				.quelle(QuellSystem.DLM)
				.build();

		Kante kanteRadvorrangrouten = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40),
			Collections.emptySet(), Set.of(IstStandard.RADVORRANGROUTEN))
				.quelle(QuellSystem.DLM)
				.build();

		Kante kanteKeinStandard = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40))
			.quelle(QuellSystem.DLM)
			.build();

		kantenRepository.save(kanteBasisstandardUndRadNETZStartstandard);
		kantenRepository.save(kanteBasisStandard);
		kantenRepository.save(kanteRadvorrangrouten);
		kantenRepository.save(kanteKeinStandard);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder()
				.istStandards(
					Set.of(IstStandard.BASISSTANDARD, IstStandard.STARTSTANDARD_RADNETZ))
				.build());

		// Assert
		assertThat(result).isNotZero();
		BigInteger expected = convertToBigIntInCm(
			kanteBasisstandardUndRadNETZStartstandard.getGeometry().getLength()
				+ kanteBasisStandard.getGeometry().getLength());
		assertThat(result).isCloseTo(expected, TEST_OFFSET);

	}

	@Test
	void testGetKmAnzahl_keinStandardGesetzt_betrachtetKorrekteStandards() {
		// Arrange
		Kante kanteBasisstandard = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20), Collections.emptySet(),
			Set.of(IstStandard.BASISSTANDARD))
				.quelle(QuellSystem.DLM)
				.build();

		Kante kanteOhneStandards = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40))
			.quelle(QuellSystem.DLM)
			.build();

		kantenRepository.save(kanteBasisstandard);
		kantenRepository.save(kanteOhneStandards);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder().beachteKantenOhneStandards(true).build());

		// Assert
		assertThat(result).isNotZero();
		assertThat(result).isEqualTo(
			convertToBigIntInCm(kanteOhneStandards.getGeometry().getLength()));
	}

	@Test
	void testGetKmAnzahl_baulastTraegerIdGesetzt_betrachtetKorrektenAnteil() {
		// Arrange
		Gebietskoerperschaft baulastTraeger = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.organisationsArt(OrganisationsArt.GEMEINDE)
				.name("baulastTraeger")
				.uebergeordneteOrganisation(null)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(1, 1, 20, 40))
				.build());

		Kante kanteBaulasttraegerMitAnteil0_6 = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider
					.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(
						List.of(
							ZustaendigkeitAttribute.builder()
								.baulastTraeger(baulastTraeger)
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
								.build(),
							ZustaendigkeitAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 0.8))
								.build(),
							ZustaendigkeitAttribute.builder()
								.baulastTraeger(baulastTraeger)
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.8, 1))
								.build()))
					.build())
			.quelle(QuellSystem.DLM)
			.build();

		Kante kanteOhneBaulastTraeger = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40))
			.quelle(QuellSystem.DLM)
			.build();

		kantenRepository.save(kanteBaulasttraegerMitAnteil0_6);
		kantenRepository.save(kanteOhneBaulastTraeger);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder().baulast(baulastTraeger).build());

		// Assert
		assertThat(result).isNotZero();
		BigInteger expected = convertToBigIntInCm(kanteBaulasttraegerMitAnteil0_6.getGeometry().getLength() * 0.6);
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void testGetKmAnzahl_unterhaltIdGesetzt_betrachtetKorrektenAnteil() {
		// Arrange
		Gebietskoerperschaft unterhaltZustaendiger = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.organisationsArt(OrganisationsArt.GEMEINDE)
				.name("unterhaltZustaendiger")
				.uebergeordneteOrganisation(null)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(1, 1, 20, 40))
				.build());

		Kante kanteUnterhaltMitAnteil0_6 = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider
					.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(
						List.of(
							ZustaendigkeitAttribute.builder()
								.unterhaltsZustaendiger(unterhaltZustaendiger)
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
								.build(),
							ZustaendigkeitAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 0.8))
								.build(),
							ZustaendigkeitAttribute.builder()
								.unterhaltsZustaendiger(unterhaltZustaendiger)
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.8, 1))
								.build()))
					.build())
			.quelle(QuellSystem.DLM)
			.build();

		Kante kanteOhneUnterhalt = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40))
			.quelle(QuellSystem.DLM)
			.build();

		kantenRepository.save(kanteUnterhaltMitAnteil0_6);
		kantenRepository.save(kanteOhneUnterhalt);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder().unterhalt(unterhaltZustaendiger).build());

		// Assert
		assertThat(result).isNotZero();
		BigInteger expected = convertToBigIntInCm(kanteUnterhaltMitAnteil0_6.getGeometry().getLength() * 0.6);
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void testGetKmAnzahl_erhaltIdGesetzt_betrachtetKorrektenAnteil() {
		// Arrange
		Gebietskoerperschaft erhaltZustaendiger = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.organisationsArt(OrganisationsArt.GEMEINDE)
				.name("erhaltZustaendiger")
				.uebergeordneteOrganisation(null)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(1, 1, 20, 40))
				.build());

		Kante kanteErhaltMitAnteil0_6 = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider
					.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(
						List.of(
							ZustaendigkeitAttribute.builder()
								.erhaltsZustaendiger(erhaltZustaendiger)
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.4))
								.build(),
							ZustaendigkeitAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.4, 0.8))
								.build(),
							ZustaendigkeitAttribute.builder()
								.erhaltsZustaendiger(erhaltZustaendiger)
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.8, 1))
								.build()))
					.build())
			.quelle(QuellSystem.DLM)
			.build();

		Kante kanteOhneErhalt = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40))
			.quelle(QuellSystem.DLM)
			.build();

		kantenRepository.save(kanteErhaltMitAnteil0_6);
		kantenRepository.save(kanteOhneErhalt);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder().erhalt(erhaltZustaendiger).build());

		// Assert
		assertThat(result).isNotZero();
		BigInteger expected = convertToBigIntInCm(kanteErhaltMitAnteil0_6.getGeometry().getLength() * 0.6);
		assertThat(result).isEqualTo(expected);
	}

	@Test
	void testGetKmAnzahl_erhaltIdUnterhaltIdUndBaulastTraegerGesetzt_betrachtetKorrektenAnteil() {
		// Arrange
		Gebietskoerperschaft erhaltZustaendiger = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.organisationsArt(OrganisationsArt.GEMEINDE)
				.name("erhaltZustaendiger")
				.uebergeordneteOrganisation(null)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(1, 1, 20, 40))
				.build());

		Gebietskoerperschaft unterhaltZustaendiger = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.organisationsArt(OrganisationsArt.GEMEINDE)
				.name("unterhaltZustaendiger")
				.uebergeordneteOrganisation(null)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(1, 1, 20, 40))
				.build());

		Gebietskoerperschaft baulastTraeger = gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
				.organisationsArt(OrganisationsArt.GEMEINDE)
				.name("baulastTraeger")
				.uebergeordneteOrganisation(null)
				.bereich(GeometryTestdataProvider.createQuadratischerBereich(1, 1, 20, 40))
				.build());

		Kante kanteErhaltUnterhaltBaulastMitAnteil0_4 = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.zustaendigkeitAttributGruppe(
				ZustaendigkeitAttributGruppeTestDataProvider
					.withLeereGrundnetzAttribute()
					.zustaendigkeitAttribute(
						List.of(
							ZustaendigkeitAttribute.builder()
								.erhaltsZustaendiger(erhaltZustaendiger)
								.unterhaltsZustaendiger(unterhaltZustaendiger)
								.baulastTraeger(baulastTraeger)
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.3))
								.build(),
							ZustaendigkeitAttribute.builder()
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.3, 0.5))
								.build(),
							ZustaendigkeitAttribute.builder()
								.baulastTraeger(baulastTraeger)
								.unterhaltsZustaendiger(unterhaltZustaendiger)
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 0.7))
								.build(),
							ZustaendigkeitAttribute.builder()
								.baulastTraeger(baulastTraeger)
								.unterhaltsZustaendiger(unterhaltZustaendiger)
								.erhaltsZustaendiger(erhaltZustaendiger)
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.7, 0.8))
								.build(),
							ZustaendigkeitAttribute.builder()
								.erhaltsZustaendiger(erhaltZustaendiger)
								.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.8, 1))
								.build()))
					.build())
			.quelle(QuellSystem.DLM)
			.build();

		Kante kanteOhneErhaltUnterhaltBaulast = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 40))
			.quelle(QuellSystem.DLM)
			.build();

		kantenRepository.save(kanteErhaltUnterhaltBaulastMitAnteil0_4);
		kantenRepository.save(kanteOhneErhaltUnterhaltBaulast);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();

		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(
				AuswertungsFilter.builder()
					.baulast(baulastTraeger)
					.erhalt(erhaltZustaendiger)
					.unterhalt(unterhaltZustaendiger)
					.build());

		// Assert
		assertThat(result).isNotZero();
		BigInteger expected = convertToBigIntInCm(
			kanteErhaltUnterhaltBaulastMitAnteil0_4.getGeometry().getLength() * 0.4);
		assertThat(result).isCloseTo(expected, TEST_OFFSET);
	}

	@Test
	void testGetKmAnzahl_Fuehrungsform() {
		Kante kanteOhneFuehrungsformAttribute = getKanteBuilder(new Coordinate(1, 10),
			new Coordinate(2, 20))
				.quelle(QuellSystem.DLM)
				.build();

		List<FuehrungsformAttribute> fuehrungsformAsphaltUndBegegnungszone0_2 = List.of(
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.belagArt(BelagArt.ASPHALT)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0., 0.2))
				.build(),
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.belagArt(BelagArt.BETON)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.2, 0.5))
				.build(),
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.SONSTIGE_STRASSE_WEG)
				.belagArt(BelagArt.ASPHALT)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0.5, 1))
				.build());

		Kante kanteAsphaltUndBegegnungszone0_2 = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.fuehrungsformAttributeLinks(fuehrungsformAsphaltUndBegegnungszone0_2)
				.fuehrungsformAttributeRechts(fuehrungsformAsphaltUndBegegnungszone0_2)
				.build())
			.quelle(QuellSystem.DLM)
			.build();

		List<FuehrungsformAttribute> fuehrungsformAsphaltUndBegegenungszone = List.of(
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.belagArt(BelagArt.ASPHALT)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
				.build());

		Kante kanteAsphaltUndBegegnungszone = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.fuehrungsformAttributeLinks(fuehrungsformAsphaltUndBegegenungszone)
				.fuehrungsformAttributeRechts(fuehrungsformAsphaltUndBegegenungszone)
				.build())
			.quelle(QuellSystem.DLM)
			.build();

		List<FuehrungsformAttribute> fuehrungsformAsphaltUndSonstigerWeg = List.of(
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.SONSTIGER_BETRIEBSWEG)
				.belagArt(BelagArt.ASPHALT)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
				.build());

		Kante kanteAsphaltUndSonstigerWeg = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.fuehrungsformAttributeLinks(fuehrungsformAsphaltUndSonstigerWeg)
				.fuehrungsformAttributeRechts(fuehrungsformAsphaltUndSonstigerWeg)
				.build())
			.quelle(QuellSystem.DLM)
			.build();

		List<FuehrungsformAttribute> fuehrungsformGebundenUndBegegnungszone = List.of(
			FuehrungsformAttribute.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.belagArt(BelagArt.WASSERGEBUNDENE_DECKE)
				.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
				.build());

		Kante kanteGebundenUndBegegnungszone = getKanteBuilder(new Coordinate(1, 10), new Coordinate(2, 20))
			.fuehrungsformAttributGruppe(FuehrungsformAttributGruppeTestDataProvider.withGrundnetzDefaultwerte()
				.fuehrungsformAttributeLinks(fuehrungsformAsphaltUndSonstigerWeg)
				.fuehrungsformAttributeRechts(fuehrungsformAsphaltUndSonstigerWeg)
				.build())
			.quelle(QuellSystem.DLM)
			.build();

		kantenRepository.save(kanteOhneFuehrungsformAttribute);
		kantenRepository.save(
			kanteAsphaltUndBegegnungszone0_2);
		kantenRepository.save(kanteAsphaltUndBegegnungszone);
		kantenRepository.save(kanteAsphaltUndSonstigerWeg);
		kantenRepository.save(kanteGebundenUndBegegnungszone);

		entityManager.flush();
		entityManager.clear();

		kantenRepository.refreshNetzMaterializedViews();
		// Act
		BigInteger result = auswertungRepository
			.getCmAnzahl(AuswertungsFilter.builder()
				.radverkehrsfuehrung(Radverkehrsfuehrung.BEGEGNUNBSZONE)
				.belagArt(BelagArt.ASPHALT)
				.build());

		// Assert
		assertThat(result).isNotZero();
		BigInteger expected = convertToBigIntInCm(kanteAsphaltUndBegegnungszone0_2.getGeometry().getLength() * 0.2
			+ kanteAsphaltUndBegegnungszone.getGeometry().getLength());
		assertThat(result).isCloseTo(expected, TEST_OFFSET);

	}

	private Kante.KanteBuilder getKanteBuilder(Coordinate vonKoordinate, Coordinate nachKoordinate) {
		QuellSystem quellSystem = QuellSystem.DLM;
		Knoten vonKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(vonKoordinate, quellSystem).build();
		Knoten nachKnoten = KnotenTestDataProvider.withCoordinateAndQuelle(nachKoordinate, quellSystem).build();

		LineString lineStringInnerhalb = GEO_FACTORY
			.createLineString(new Coordinate[] { vonKnoten.getKoordinate(), nachKnoten.getKoordinate() });

		return KanteTestDataProvider.withDefaultValues().vonKnoten(vonKnoten).nachKnoten(nachKnoten)
			.geometry(lineStringInnerhalb).quelle(quellSystem).aufDlmAbgebildeteGeometry(null);
	}

	private Kante.KanteBuilder getKanteBuilder(Coordinate vonKoordinate, Coordinate nachKoordinate,
		Set<Netzklasse> netzklassen, Set<IstStandard> istStandards) {

		return getKanteBuilder(vonKoordinate, nachKoordinate)
			.kantenAttributGruppe(
				KantenAttributGruppeTestDataProvider
					.defaultValue()
					.netzklassen(netzklassen)
					.istStandards(istStandards)
					.build());
	}

	private static BigInteger convertToBigIntInCm(double val) {
		return BigInteger.valueOf((int) Math.floor(val * 100));
	}
}
