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

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.MockBeans;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.transaction.TestTransaction;

import de.wps.radvis.backend.auditing.domain.AdditionalRevInfoHolder;
import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import de.wps.radvis.backend.common.domain.MailService;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.dokument.DokumentConfiguration;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.konsistenz.regeln.domain.valueObject.KonsistenzregelVerletzungsDetails;
import de.wps.radvis.backend.massnahme.MassnahmeConfiguration;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppeTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.entity.provider.KnotenTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitRepository;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

@Tag("group7")
@AutoConfigureTestEntityManager
@EnableJpaRepositories(basePackageClasses = { MassnahmeConfiguration.class })
@EntityScan(basePackageClasses = {
	MassnahmeConfiguration.class,
	KommentarConfiguration.class,
	DokumentConfiguration.class
})
@EnableConfigurationProperties(value = {
	OrganisationConfigurationProperties.class
})
@MockBeans({
	@MockBean(MailService.class),
})
public class RadNetzMassnahmeNetzklasseKonsistenzregelTestIT extends AbstractKonsistenzregelTestIT {

	@Autowired
	KantenRepository kantenRepository;

	@Autowired
	KnotenRepository knotenRepository;

	@Autowired
	MassnahmeRepository massnahmeRepository;

	@Autowired
	BenutzerRepository benutzerRepository;

	@Autowired
	VerwaltungseinheitRepository verwaltungseinheitRepository;
	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	RadNetzMassnahmenNetzklasseKonsistenzregel konsistenzregel;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		konsistenzregel = new RadNetzMassnahmenNetzklasseKonsistenzregel(jdbcTemplate);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void pruefen() {
		// arrange
		TestTransaction.end();
		TestTransaction.start();
		AdditionalRevInfoHolder.setAuditingContext(AuditingContext.DLM_REIMPORT_JOB);

		Verwaltungseinheit gebietskoerperschaft = gebietskoerperschaftRepository
			.save(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().build());
		Benutzer benutzer = benutzerRepository
			.save(BenutzerTestDataProvider.defaultBenutzer().organisation(gebietskoerperschaft).build());

		Knoten knotenRadNetz = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 0), QuellSystem.DLM)
				.build());
		Knoten knotenGemischt = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(0, 100), QuellSystem.DLM)
				.build());
		Kante radnetzKante = kantenRepository
			.save(KanteTestDataProvider.fromKnoten(knotenRadNetz, knotenGemischt).kantenAttributGruppe(
				KantenAttributGruppeTestDataProvider.defaultValue()
					.netzklassen(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.KOMMUNALNETZ_ALLTAG))
					.build())
				.quelle(QuellSystem.DLM)
				.dlmId(DlmId.of("DlmId1")).build());

		Knoten knotenOhneNetzklasse = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 0), QuellSystem.DLM)
				.build());
		Kante kanteOhneNetzklasse = kantenRepository
			.save(KanteTestDataProvider
				.fromKnoten(knotenGemischt, knotenOhneNetzklasse).quelle(QuellSystem.DLM).kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(Set.of())
						.build())
				.dlmId(DlmId.of("DlmId2")).build());

		Knoten knotenKreisnetz = knotenRepository
			.save(KnotenTestDataProvider.withCoordinateAndQuelle(new Coordinate(100, 100), QuellSystem.DLM)
				.build());
		Kante kanteKreisnetz = kantenRepository
			.save(KanteTestDataProvider
				.fromKnoten(knotenKreisnetz, knotenOhneNetzklasse).quelle(QuellSystem.DLM).kantenAttributGruppe(
					KantenAttributGruppeTestDataProvider.defaultValue().netzklassen(Set.of(Netzklasse.KREISNETZ_ALLTAG))
						.build())
				.dlmId(DlmId.of("DlmId3")).build());

		// fehler
		Massnahme streckenMassnahmeKreisnetz = massnahmeRepository
			.save(MassnahmeTestDataProvider.withKanten(kanteOhneNetzklasse).benutzerLetzteAenderung(benutzer)
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME_2024).zustaendiger(gebietskoerperschaft)
				.build());

		// ok
		massnahmeRepository
			.save(MassnahmeTestDataProvider.withKanten(kanteOhneNetzklasse).umsetzungsstatus(Umsetzungsstatus.STORNIERT)
				.benutzerLetzteAenderung(benutzer).zustaendiger(gebietskoerperschaft)
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME_2024).build());

		// ok
		massnahmeRepository
			.save(MassnahmeTestDataProvider.withKanten(radnetzKante).benutzerLetzteAenderung(benutzer)
				.zustaendiger(gebietskoerperschaft)
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).build());

		// ok
		massnahmeRepository
			.save(MassnahmeTestDataProvider.withKanten(kanteOhneNetzklasse).benutzerLetzteAenderung(benutzer)
				.zustaendiger(gebietskoerperschaft)
				.konzeptionsquelle(Konzeptionsquelle.SONSTIGE).build());

		// fehler
		Massnahme streckenMassnahmeGemischt = massnahmeRepository
			.save(MassnahmeTestDataProvider.withKanten(radnetzKante, kanteOhneNetzklasse)
				.benutzerLetzteAenderung(benutzer).zustaendiger(gebietskoerperschaft)
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).build());

		// fehler
		Massnahme streckenMassnahmeDoppelterFehler = massnahmeRepository
			.save(MassnahmeTestDataProvider.withKanten(kanteKreisnetz, kanteOhneNetzklasse)
				.benutzerLetzteAenderung(benutzer).zustaendiger(gebietskoerperschaft)
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).build());

		// ok
		massnahmeRepository
			.save(MassnahmeTestDataProvider.withKnoten(knotenGemischt).benutzerLetzteAenderung(benutzer)
				.zustaendiger(gebietskoerperschaft)
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).build());

		// ok
		massnahmeRepository
			.save(MassnahmeTestDataProvider.withKnoten(knotenRadNetz).benutzerLetzteAenderung(benutzer)
				.zustaendiger(gebietskoerperschaft)
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).build());

		// fehler
		Massnahme knotenMassnahmeKreisnetz = massnahmeRepository
			.save(MassnahmeTestDataProvider.withKnoten(knotenOhneNetzklasse).benutzerLetzteAenderung(benutzer)
				.zustaendiger(gebietskoerperschaft)
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).build());

		// fehler
		massnahmeRepository
			.save(MassnahmeTestDataProvider.withKnoten(knotenOhneNetzklasse).benutzerLetzteAenderung(benutzer)
				.zustaendiger(gebietskoerperschaft)
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).umsetzungsstatus(Umsetzungsstatus.STORNIERT)
				.build());

		// ok
		massnahmeRepository
			.save(MassnahmeTestDataProvider.withKnoten(knotenOhneNetzklasse).benutzerLetzteAenderung(benutzer)
				.zustaendiger(gebietskoerperschaft)
				.konzeptionsquelle(Konzeptionsquelle.SONSTIGE).build());

		// fehler
		Massnahme multipleKnotenMassnahme = massnahmeRepository
			.save(MassnahmeTestDataProvider.withKnoten(knotenOhneNetzklasse, knotenRadNetz)
				.benutzerLetzteAenderung(benutzer).zustaendiger(gebietskoerperschaft)
				.konzeptionsquelle(Konzeptionsquelle.RADNETZ_MASSNAHME).build());

		TestTransaction.flagForCommit();
		TestTransaction.end();

		// wir sichern uns gegen false positive tests ab

		List<Map<String, Object>> allMassnahmen = jdbcTemplate
			.queryForList("SELECT id, konzeptionsquelle FROM massnahme");
		assertThat(allMassnahmen).hasSize(12);

		// act
		List<KonsistenzregelVerletzungsDetails> result = konsistenzregel.pruefen();

		assertThat(result.size()).isEqualTo(5);
		assertThat(result).extracting(KonsistenzregelVerletzungsDetails::getIdentity)
			.containsExactlyInAnyOrder(streckenMassnahmeKreisnetz.getId().toString(),
				streckenMassnahmeGemischt.getId().toString(), streckenMassnahmeDoppelterFehler.getId().toString(),
				knotenMassnahmeKreisnetz.getId().toString(), multipleKnotenMassnahme.getId().toString());

		KonsistenzregelVerletzungsDetails konsistenzregelVerletzungsDetailsStreckenMassnahmeKreisnetz = result.stream()
			.filter(k -> k.getIdentity().equals(streckenMassnahmeKreisnetz.getId().toString())).findAny().get();
		KonsistenzregelVerletzungsDetails konsistenzregelVerletzungsDetailsStreckenMassnahmeGemischt = result.stream()
			.filter(k -> k.getIdentity().equals(streckenMassnahmeGemischt.getId().toString())).findAny().get();
		KonsistenzregelVerletzungsDetails konsistenzregelVerletzungsDetailsStreckenMassnahmeDoppelterFehler = result
			.stream().filter(k -> k.getIdentity().equals(streckenMassnahmeDoppelterFehler.getId().toString())).findAny()
			.get();
		KonsistenzregelVerletzungsDetails konsistenzregelVerletzungsDetailsKnotenMassnahmeKreisnetz = result.stream()
			.filter(k -> k.getIdentity().equals(knotenMassnahmeKreisnetz.getId().toString())).findAny().get();
		KonsistenzregelVerletzungsDetails konsistenzregelVerletzungsDetailsMultipleKnotenMassnahme = result.stream()
			.filter(k -> k.getIdentity().equals(multipleKnotenMassnahme.getId().toString())).findAny().get();

		assertThat(konsistenzregelVerletzungsDetailsStreckenMassnahmeKreisnetz.getPosition())
			.isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createPoint(LineStrings.getMidPoint(kanteOhneNetzklasse.getGeometry())));
		assertThat(konsistenzregelVerletzungsDetailsStreckenMassnahmeGemischt.getPosition())
			.isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createPoint(
				LineStrings.getMidPoint(kanteOhneNetzklasse.getGeometry())));
		assertThat(konsistenzregelVerletzungsDetailsStreckenMassnahmeDoppelterFehler.getPosition()).satisfiesAnyOf(
			pos -> assertThat(pos).isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createPoint(LineStrings.getMidPoint(kanteOhneNetzklasse.getGeometry()))),
			pos -> assertThat(pos).isEqualTo(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
				.createPoint(LineStrings.getMidPoint(kanteKreisnetz.getGeometry()))));
		assertThat(konsistenzregelVerletzungsDetailsKnotenMassnahmeKreisnetz.getPosition())
			.isEqualTo(knotenOhneNetzklasse.getPoint());
		assertThat(konsistenzregelVerletzungsDetailsMultipleKnotenMassnahme.getPosition())
			.isEqualTo(knotenOhneNetzklasse.getPoint());

		assertThat(konsistenzregelVerletzungsDetailsStreckenMassnahmeKreisnetz.getBeschreibung())
			.isEqualTo(RadNetzMassnahmenNetzklasseKonsistenzregel.createBeschreibungFuerStreckenmassnahme(
				streckenMassnahmeKreisnetz.getId(), kanteOhneNetzklasse.getId()));
		assertThat(konsistenzregelVerletzungsDetailsStreckenMassnahmeGemischt.getBeschreibung())
			.isEqualTo(RadNetzMassnahmenNetzklasseKonsistenzregel.createBeschreibungFuerStreckenmassnahme(
				streckenMassnahmeGemischt.getId(), kanteOhneNetzklasse.getId()));
		assertThat(konsistenzregelVerletzungsDetailsStreckenMassnahmeDoppelterFehler.getBeschreibung()).satisfiesAnyOf(
			pos -> assertThat(pos)
				.isEqualTo(RadNetzMassnahmenNetzklasseKonsistenzregel.createBeschreibungFuerStreckenmassnahme(
					streckenMassnahmeDoppelterFehler.getId(), kanteOhneNetzklasse.getId())),
			pos -> assertThat(pos)
				.isEqualTo(RadNetzMassnahmenNetzklasseKonsistenzregel.createBeschreibungFuerStreckenmassnahme(
					streckenMassnahmeDoppelterFehler.getId(), kanteKreisnetz.getId())));
		assertThat(konsistenzregelVerletzungsDetailsKnotenMassnahmeKreisnetz.getBeschreibung())
			.isEqualTo(RadNetzMassnahmenNetzklasseKonsistenzregel
				.createBeschreibungFuerKnotenmassnahme(knotenMassnahmeKreisnetz.getId(), knotenOhneNetzklasse.getId()));
		assertThat(konsistenzregelVerletzungsDetailsMultipleKnotenMassnahme.getBeschreibung())
			.isEqualTo(RadNetzMassnahmenNetzklasseKonsistenzregel
				.createBeschreibungFuerKnotenmassnahme(multipleKnotenMassnahme.getId(), knotenOhneNetzklasse.getId()));
	}
}
