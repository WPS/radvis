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

package de.wps.radvis.backend.furtKreuzung.domain.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ContextConfiguration;

import de.wps.radvis.backend.benutzer.BenutzerConfiguration;
import de.wps.radvis.backend.benutzer.domain.TechnischerBenutzerConfigurationProperties;
import de.wps.radvis.backend.common.CommonConfiguration;
import de.wps.radvis.backend.common.GeoConverterConfiguration;
import de.wps.radvis.backend.common.MailConfiguration;
import de.wps.radvis.backend.common.domain.CommonConfigurationProperties;
import de.wps.radvis.backend.common.domain.FeatureToggleProperties;
import de.wps.radvis.backend.common.domain.JobConfigurationProperties;
import de.wps.radvis.backend.common.domain.MailConfigurationProperties;
import de.wps.radvis.backend.common.domain.PostgisConfigurationProperties;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.common.schnittstelle.DBIntegrationTestIT;
import de.wps.radvis.backend.furtKreuzung.FurtKreuzungConfiguration;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzung;
import de.wps.radvis.backend.furtKreuzung.domain.entity.FurtKreuzungNetzBezug;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenKommentar;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.FurtenKreuzungenTyp;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.GruenAnforderung;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.LichtsignalAnlageEigenschaften;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.Linksabbieger;
import de.wps.radvis.backend.furtKreuzung.domain.valueObject.Rechtsabbieger;
import de.wps.radvis.backend.kommentar.KommentarConfiguration;
import de.wps.radvis.backend.netz.NetzConfiguration;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.organisation.OrganisationConfiguration;
import de.wps.radvis.backend.organisation.domain.GebietskoerperschaftRepository;
import de.wps.radvis.backend.organisation.domain.OrganisationConfigurationProperties;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Tag("group5")
@ContextConfiguration(classes = {
	OrganisationConfiguration.class,
	BenutzerConfiguration.class,
	NetzConfiguration.class,
	GeoConverterConfiguration.class,
	FurtKreuzungConfiguration.class,
	KommentarConfiguration.class,
	CommonConfiguration.class,
	MailConfiguration.class
})
@EnableConfigurationProperties(value = {
	FeatureToggleProperties.class,
	JobConfigurationProperties.class,
	CommonConfigurationProperties.class,
	TechnischerBenutzerConfigurationProperties.class,
	MailConfigurationProperties.class,
	PostgisConfigurationProperties.class,
	OrganisationConfigurationProperties.class
})
class FurtKreuzungRepositoryTestIT extends DBIntegrationTestIT {
	@Autowired
	private FurtKreuzungRepository furtKreuzungRepository;

	@Autowired
	private GebietskoerperschaftRepository gebietskoerperschaftRepository;

	@Autowired
	KantenRepository kantenRepository;

	@PersistenceContext
	private EntityManager entityManager;

	private final RecursiveComparisonConfiguration comparisonConfigurationForFurtKreuzung = RecursiveComparisonConfiguration
		.builder()
		.withIgnoredFields(
			"id",
			"verantwortlicheOrganisation.bereich",
			"verantwortlicheOrganisation.bereichBuffer",
			"netzbezug")
		.build();

	@Test
	void saveFurtKreuzung() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().build();
		final FurtKreuzungNetzBezug netzbezug = new FurtKreuzungNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				kante, LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS)),
			Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG)),
			Collections.emptySet());

		FurtKreuzung neu = new FurtKreuzung(netzbezug, gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Eine verantwortliche Organisation")
				.organisationsArt(OrganisationsArt.KREIS)
				.build()),
			FurtenKreuzungenTyp.KREUZUNG, true, new FurtenKreuzungenKommentar("Dies ist ein Kommentar"),
			KnotenForm.MINIKREISVERKEHR_24_M, Optional.empty(), Optional.empty());

		// arrange
		FurtKreuzung gespeichert = furtKreuzungRepository.save(neu);

		// assert
		assertThat(gespeichert).usingRecursiveComparison().ignoringFields("id").isEqualTo(neu);
	}

	@Test
	void saveFurtKreuzung_mitUndOhneLSA() {
		// arrange
		Kante kante = kantenRepository.save(KanteTestDataProvider.withDefaultValues().build());
		final FurtKreuzungNetzBezug netzbezug = new FurtKreuzungNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				kante, LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS)),
			Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG)),
			Collections.emptySet());

		final FurtKreuzungNetzBezug netzbezug2 = new FurtKreuzungNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				kante, LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS)),
			Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG)),
			Collections.emptySet());

		FurtKreuzung ohneLSA = new FurtKreuzung(netzbezug, gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Eine verantwortliche Organisation")
				.organisationsArt(OrganisationsArt.KREIS)
				.build()),
			FurtenKreuzungenTyp.KREUZUNG, true, new FurtenKreuzungenKommentar("Dies ist ein Kommentar"),
			KnotenForm.MINIKREISVERKEHR_24_M, Optional.empty(), Optional.empty());

		FurtKreuzung mitLSA = new FurtKreuzung(netzbezug2, gebietskoerperschaftRepository.save(
			VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().name("Eine verantwortliche Organisation")
				.organisationsArt(OrganisationsArt.KREIS)
				.build()),
			FurtenKreuzungenTyp.KREUZUNG, true, new FurtenKreuzungenKommentar("Dies ist ein Kommentar"),
			KnotenForm.LSA_KNOTEN_MIT_RADVERKEHRSFUEHRUNG_UEBER_BAULICHE_NEBENANLAGE, Optional.empty(),
			Optional.of(new LichtsignalAnlageEigenschaften(true, false, false, Rechtsabbieger.GRUENPFEIL_ALLE,
				Linksabbieger.EIGENES_SIGNALISIEREN, false, false,
				GruenAnforderung.AUTOMATISCH, null)));

		// arrange
		Long idOhneLSA = furtKreuzungRepository.save(ohneLSA).getId();
		Long idMitLSA = furtKreuzungRepository.save(mitLSA).getId();

		entityManager.flush();
		entityManager.clear();

		FurtKreuzung gespeichertOhneLSA = furtKreuzungRepository.findById(idOhneLSA)
			.orElseThrow();
		FurtKreuzung gespeichertMitLSA = furtKreuzungRepository.findById(idMitLSA)
			.orElseThrow();

		// assert
		assertThat(gespeichertOhneLSA)
			.usingRecursiveComparison(comparisonConfigurationForFurtKreuzung)
			.isEqualTo(ohneLSA);

		assertThat(gespeichertOhneLSA.getNetzbezug()).isEqualTo(ohneLSA.getNetzbezug());

		assertThat(gespeichertOhneLSA.getVerantwortlicheOrganisation().getBereich())
			.isEqualTo(ohneLSA.getVerantwortlicheOrganisation().getBereich());

		assertThat(gespeichertOhneLSA.getLichtsignalAnlageEigenschaften()).isEmpty();

		assertThat(gespeichertMitLSA)
			.usingRecursiveComparison(comparisonConfigurationForFurtKreuzung)
			.isEqualTo(mitLSA);

		assertThat(gespeichertMitLSA.getNetzbezug()).isEqualTo(mitLSA.getNetzbezug());
		assertThat(gespeichertMitLSA.getVerantwortlicheOrganisation().getBereich())
			.isEqualTo(mitLSA.getVerantwortlicheOrganisation().getBereich());
	}
}
