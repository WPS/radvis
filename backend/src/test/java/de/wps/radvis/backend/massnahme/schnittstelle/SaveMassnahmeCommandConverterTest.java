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

package de.wps.radvis.backend.massnahme.schnittstelle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.provider.MassnahmeTestDataProvider;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.LGVFGID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MaViSID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Prioritaet;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.VerbaID;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.KnotenResolver;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.netz.schnittstelle.command.KnotenNetzbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.PunktuellerKantenSeitenBezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SeitenabschnittsKantenBezugCommand;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;

class SaveMassnahmeCommandConverterTest {

	static MockedStatic<RadVisDomainEventPublisher> domainPublisherMock;

	@Mock
	private KanteResolver kanteResolver;
	@Mock
	private KnotenResolver knotenResolver;
	@Mock
	private VerwaltungseinheitResolver verwaltungseinheitResolver;
	@Mock
	private BenutzerResolver benutzerresolver;

	private SaveMassnahmeCommandConverter commandConverter;

	private Verwaltungseinheit testOrganisation;
	private Verwaltungseinheit testOrganisation2;
	private Benutzer testBenutzer;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		testOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(500L)
			.name("Coole Organisation").organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();
		testOrganisation2 = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(2L)
			.name("Andere Organisation").organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();
		testBenutzer = BenutzerTestDataProvider.admin(testOrganisation).build();

		when(verwaltungseinheitResolver.resolve(testOrganisation.getId())).thenReturn(testOrganisation);
		when(verwaltungseinheitResolver.resolve(testOrganisation2.getId())).thenReturn(testOrganisation2);

		when(benutzerresolver.fromAuthentication(any())).thenReturn(testBenutzer);
		commandConverter = new SaveMassnahmeCommandConverter(kanteResolver, verwaltungseinheitResolver,
			benutzerresolver, knotenResolver);
		domainPublisherMock = mockStatic(RadVisDomainEventPublisher.class);
	}

	@AfterEach
	void cleanUp() {
		domainPublisherMock.close();
	}

	@Test
	void convert_Massnahme() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().id(200L).build();
		AbschnittsweiserKantenSeitenBezug abschnittsweiserKantenSeitenBezug = new AbschnittsweiserKantenSeitenBezug(
			kante,
			LinearReferenzierterAbschnitt.of(0, 1),
			Seitenbezug.LINKS);
		when(kanteResolver.getKante(200L)).thenReturn(kante);

		Massnahme massnahme = MassnahmeTestDataProvider
			.withDefaultValues()
			.netzbezug(
				new MassnahmeNetzBezug(Set.of(abschnittsweiserKantenSeitenBezug), Set.of(), Collections.emptySet()))
			.baulastZustaendiger(testOrganisation)
			.unterhaltsZustaendiger(testOrganisation2)
			.id(42L)
			.build();

		final var knotenIds = Set.of(32L);
		final var knoten = mock(Knoten.class);
		when(knotenResolver.getKnoten(knotenIds)).thenReturn(List.of(knoten));
		NetzbezugCommand netzbezug = new NetzbezugCommand(
			List.of(new SeitenabschnittsKantenBezugCommand(200L, LinearReferenzierterAbschnitt.of(0, 1),
				Seitenbezug.BEIDSEITIG)),
			List.of(new PunktuellerKantenSeitenBezugCommand(200L, 0.67D)),
			List.of(new KnotenNetzbezugCommand(32L)));

		SaveMassnahmeCommand command = SaveMassnahmeCommand.builder()
			.bezeichnung(Bezeichnung.of("Bezeichnung Neu")) // <- geändert
			.massnahmenkategorien(Set.of(Massnahmenkategorie.FURTEN_ERNEUERN))
			.durchfuehrungszeitraum(Durchfuehrungszeitraum.of(2022))
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.veroeffentlicht(true)
			.planungErforderlich(true)
			.netzbezug(netzbezug) // <- geändert
			.maViSID(MaViSID.of("maViSID"))
			.verbaID(VerbaID.of("verbaID"))
			.lgvfgid(LGVFGID.of("lgvfgid"))
			.massnahmeKonzeptID(MassnahmeKonzeptID.of("ACB123"))
			.prioritaet(Prioritaet.of(1))
			.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG))
			.baulastZustaendigerId(500L)
			.unterhaltsZustaendigerId(2L) // <- geändert
			.zustaendigerId(1L)
			.sollStandard(SollStandard.BASISSTANDARD)
			.handlungsverantwortlicher(Handlungsverantwortlicher.BAULASTTRAEGER)
			.konzeptionsquelle(Konzeptionsquelle.SONSTIGE)
			.sonstigeKonzeptionsquelle("WAMBO")
			.build();

		// act
		Authentication authentication = mock(Authentication.class);
		commandConverter.apply(authentication, command, massnahme);

		// assert
		assertThat(massnahme.getBezeichnung()).isEqualTo(Bezeichnung.of("Bezeichnung Neu"));
		assertThat(massnahme.getMassnahmenkategorien()).isEqualTo(Set.of(Massnahmenkategorie.FURTEN_ERNEUERN));
		assertThat(massnahme.getUmsetzungsstatus()).isEqualTo(Umsetzungsstatus.IDEE);
		assertThat(massnahme.getVeroeffentlicht()).isTrue();
		assertThat(massnahme.getPlanungErforderlich()).isTrue();
		assertThat(massnahme.getBaulastZustaendiger()).contains(testOrganisation);
		assertThat(massnahme.getunterhaltsZustaendiger()).contains(testOrganisation2);
		assertThat(massnahme.getDurchfuehrungszeitraum()).contains(Durchfuehrungszeitraum.of(2022));
		assertThat(massnahme.getMassnahmeKonzeptID()).contains(command.getMassnahmeKonzeptID());
		assertThat(massnahme.getSollStandard()).isEqualTo(command.getSollStandard());
		assertThat(massnahme.getHandlungsverantwortlicher()).contains(command.getHandlungsverantwortlicher());
		assertThat(massnahme.getKonzeptionsquelle()).isEqualTo(command.getKonzeptionsquelle());
		assertThat(massnahme.getSonstigeKonzeptionsquelle()).contains(command.getSonstigeKonzeptionsquelle());

		final Set<AbschnittsweiserKantenSeitenBezug> kantenSeitenAbschnitteResult = massnahme.getNetzbezug()
			.getImmutableKantenAbschnittBezug();
		assertThat(kantenSeitenAbschnitteResult).hasSize(1);
		assertThat(kantenSeitenAbschnitteResult.stream().findFirst().get().getSeitenbezug()).isEqualTo(
			Seitenbezug.BEIDSEITIG);
		assertThat(kantenSeitenAbschnitteResult.stream().findFirst().get().getKante()).isEqualTo(kante);
		assertThat(kantenSeitenAbschnitteResult.stream().findFirst().get().getLinearReferenzierterAbschnitt()
			.getVonValue()).isZero();
		assertThat(kantenSeitenAbschnitteResult.stream().findFirst().get().getLinearReferenzierterAbschnitt()
			.getBisValue()).isEqualTo(1);

		final Set<PunktuellerKantenSeitenBezug> punktuellerKantenSeitenBezugsResult = massnahme.getNetzbezug()
			.getImmutableKantenPunktBezug();
		assertThat(punktuellerKantenSeitenBezugsResult).hasSize(1);
		assertThat(punktuellerKantenSeitenBezugsResult.stream().findFirst().get().getSeitenbezug()).isEqualTo(
			Seitenbezug.BEIDSEITIG);
		assertThat(punktuellerKantenSeitenBezugsResult.stream().findFirst().get().getKante()).isEqualTo(kante);
		assertThat(punktuellerKantenSeitenBezugsResult.stream().findFirst().get().getLineareReferenz()
			.getAbschnittsmarke()).isEqualTo(0.67D);

		final Set<Knoten> knotenResult = massnahme.getNetzbezug().getImmutableKnotenBezug();
		assertThat(knotenResult).containsExactly(knoten);
	}
}
