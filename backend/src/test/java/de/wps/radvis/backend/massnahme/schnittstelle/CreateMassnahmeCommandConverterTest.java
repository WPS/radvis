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
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.KnotenResolver;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.schnittstelle.command.KnotenNetzbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.PunktuellerKantenSeitenBezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SeitenabschnittsKantenBezugCommand;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;
import de.wps.radvis.backend.organisation.domain.valueObject.OrganisationsArt;

class CreateMassnahmeCommandConverterTest {

	@Mock
	private KanteResolver kanteResolver;
	@Mock
	private KnotenResolver knotenResolver;
	@Mock
	private VerwaltungseinheitResolver verwaltungseinheitResolver;
	@Mock
	private BenutzerResolver benutzerresolver;

	private CreateMassnahmeCommandConverter commandConverter;

	private Verwaltungseinheit testOrganisation;
	private Benutzer testBenutzer;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);

		testOrganisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(1L)
			.name("Coole Organisation")
			.organisationsArt(
				OrganisationsArt.BUNDESLAND)
			.build();
		testBenutzer = BenutzerTestDataProvider.admin(testOrganisation).build();

		when(verwaltungseinheitResolver.resolve(testOrganisation.getId())).thenReturn(testOrganisation);

		when(benutzerresolver.fromAuthentication(any())).thenReturn(testBenutzer);
		commandConverter = new CreateMassnahmeCommandConverter(kanteResolver, knotenResolver,
			verwaltungseinheitResolver,
			benutzerresolver);
	}

	@Test
	void convert() {
		// arrange
		Kante kante = KanteTestDataProvider.withDefaultValues().id(200L).build();
		when(kanteResolver.getKante(200L)).thenReturn(kante);

		final Set<Long> knotenIds = Set.of(32L);
		final Knoten knoten = mock(Knoten.class);
		when(knotenResolver.getKnoten(knotenIds)).thenReturn(List.of(knoten));
		NetzbezugCommand netzbezug = new NetzbezugCommand(
			List.of(new SeitenabschnittsKantenBezugCommand(200L, LinearReferenzierterAbschnitt.of(0.2, 0.4),
				Seitenbezug.LINKS)),
			List.of(new PunktuellerKantenSeitenBezugCommand(200L, 0.67D)),
			List.of(new KnotenNetzbezugCommand(32L)));

		CreateMassnahmeCommand command = CreateMassnahmeCommand.builder()
			.bezeichnung(Bezeichnung.of("Massnahme"))
			.massnahmenkategorien(Set.of(Massnahmenkategorie.BORDABSENKUNGEN_HERSTELLEN_AUSSERORTS))
			.netzbezug(netzbezug)
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.veroeffentlicht(true)
			.planungErforderlich(false)
			.durchfuehrungszeitraum(Durchfuehrungszeitraum.of(2021))
			.baulastZustaendigerId(testOrganisation.getId())
			.zustaendigerId(testOrganisation.getId())
			.konzeptionsquelle(Konzeptionsquelle.SONSTIGE)
			.sonstigeKonzeptionsquelle("WAMBO")
			.build();

		// act
		Authentication authentication = mock(Authentication.class);
		Massnahme massnahme = commandConverter.convert(authentication, command);

		// assert
		assertThat(massnahme.getBezeichnung()).isEqualTo(Bezeichnung.of("Massnahme"));
		assertThat(massnahme.getMassnahmenkategorien()).containsExactly(
			Massnahmenkategorie.BORDABSENKUNGEN_HERSTELLEN_AUSSERORTS);
		assertThat(massnahme.getUmsetzungsstatus()).isEqualTo(Umsetzungsstatus.IDEE);
		assertThat(massnahme.getVeroeffentlicht()).isTrue();
		assertThat(massnahme.getPlanungErforderlich()).isFalse();
		assertThat(massnahme.getBaulastZustaendiger()).contains(testOrganisation);
		assertThat(massnahme.getDurchfuehrungszeitraum()).contains(Durchfuehrungszeitraum.of(2021));
		assertThat(massnahme.getKonzeptionsquelle()).isEqualTo(command.getKonzeptionsquelle());
		assertThat(massnahme.getSonstigeKonzeptionsquelle()).contains(command.getSonstigeKonzeptionsquelle());

		final Set<AbschnittsweiserKantenSeitenBezug> seitenabschnittsKantenSeitenAbschnitteResult = massnahme
			.getNetzbezug()
			.getImmutableKantenAbschnittBezug();
		assertThat(seitenabschnittsKantenSeitenAbschnitteResult).hasSize(1);
		assertThat(seitenabschnittsKantenSeitenAbschnitteResult.stream().findFirst().get().getSeitenbezug()).isEqualTo(
			Seitenbezug.LINKS);
		assertThat(seitenabschnittsKantenSeitenAbschnitteResult.stream().findFirst().get().getKante()).isEqualTo(kante);
		assertThat(
			seitenabschnittsKantenSeitenAbschnitteResult.stream().findFirst().get().getLinearReferenzierterAbschnitt()
				.getVonValue()).isEqualTo(0.2);
		assertThat(
			seitenabschnittsKantenSeitenAbschnitteResult.stream().findFirst().get().getLinearReferenzierterAbschnitt()
				.getBisValue()).isEqualTo(0.4);

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
