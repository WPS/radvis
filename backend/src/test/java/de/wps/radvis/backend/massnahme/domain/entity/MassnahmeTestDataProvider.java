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

package de.wps.radvis.backend.massnahme.domain.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.entity.BenutzerTestDataProvider;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.dokument.domain.entity.DokumentListe;
import de.wps.radvis.backend.kommentar.domain.entity.KommentarListe;
import de.wps.radvis.backend.massnahme.domain.valueObject.Bezeichnung;
import de.wps.radvis.backend.massnahme.domain.valueObject.Durchfuehrungszeitraum;
import de.wps.radvis.backend.massnahme.domain.valueObject.Handlungsverantwortlicher;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Kostenannahme;
import de.wps.radvis.backend.massnahme.domain.valueObject.LGVFGID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MaViSID;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmeKonzeptID;
import de.wps.radvis.backend.massnahme.domain.valueObject.Massnahmenkategorie;
import de.wps.radvis.backend.massnahme.domain.valueObject.Prioritaet;
import de.wps.radvis.backend.massnahme.domain.valueObject.Realisierungshilfe;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.massnahme.domain.valueObject.VerbaID;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.provider.KanteTestDataProvider;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.SollStandard;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import de.wps.radvis.backend.organisation.domain.provider.VerwaltungseinheitTestDataProvider;

public class MassnahmeTestDataProvider {

	public static Massnahme.MassnahmeBuilder withPflichtfelderAbPlanung() {
		return withDefaultValues().durchfuehrungszeitraum(Durchfuehrungszeitraum.of(2024))
			.baulastZustaendiger(VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft().id(10L).build())
			.handlungsverantwortlicher(Handlungsverantwortlicher.BAULASTTRAEGER);
	}

	public static Massnahme.MassnahmeBuilder withDefaultValues() {
		Verwaltungseinheit organisation = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Coole Organisation")
			.organisationsArt(OrganisationsArt.BUNDESLAND)
			.build();

		Verwaltungseinheit zustaendiger = VerwaltungseinheitTestDataProvider.defaultGebietskoerperschaft()
			.name("Mega coole zuständige Organisation")
			.organisationsArt(OrganisationsArt.REGIERUNGSBEZIRK)
			.build();

		Benutzer benutzer = BenutzerTestDataProvider.admin(organisation).build();
		Kante kante = KanteTestDataProvider.withDefaultValues().build();
		final MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(new AbschnittsweiserKantenSeitenBezug(
				kante, LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS)),
			Set.of(new PunktuellerKantenSeitenBezug(kante, LineareReferenz.of(0.25), Seitenbezug.BEIDSEITIG)),
			Collections.emptySet()
		);

		return Massnahme.builder()
			.bezeichnung(Bezeichnung.of("Bezeichnung"))
			.massnahmenkategorien(Set.of(Massnahmenkategorie.FURTEN_ERNEUERN, Massnahmenkategorie.ANPASSUNG_AN_BAUWERK))
			.netzbezug(netzbezug)
			.zustaendiger(zustaendiger)
			.durchfuehrungszeitraum(Durchfuehrungszeitraum.of(2022))
			.umsetzungsstatus(Umsetzungsstatus.IDEE)
			.dokumentListe(new DokumentListe(new ArrayList<>()))
			.veroeffentlicht(true)
			.planungErforderlich(true)
			.maViSID(MaViSID.of("maViSID"))
			.verbaID(VerbaID.of("verbaID"))
			.lgvfgid(LGVFGID.of("lgvfgid"))
			.massnahmeKonzeptId(MassnahmeKonzeptID.of("ABC123"))
			.prioritaet(Prioritaet.of(1))
			.kostenannahme(Kostenannahme.of(1234L))
			.netzklassen(Set.of(Netzklasse.KOMMUNALNETZ_ALLTAG, Netzklasse.KREISNETZ_FREIZEIT))
			.benutzerLetzteAenderung(benutzer)
			.letzteAenderung(LocalDateTime.of(2050, 10, 1, 0, 0))
			.sollStandard(SollStandard.BASISSTANDARD)
			.handlungsverantwortlicher(Handlungsverantwortlicher.BAULASTTRAEGER)
			.konzeptionsquelle(Konzeptionsquelle.SONSTIGE)
			.kommentarListe(new KommentarListe())
			.sonstigeKonzeptionsquelle("WAMBO")
			.zuBenachrichtigendeBenutzer(new HashSet<>())
			.realisierungshilfe(Realisierungshilfe.NR_2_2_1);
	}

	public static Massnahme.MassnahmeBuilder withKanten(Kante... kanten) {
		Set<AbschnittsweiserKantenSeitenBezug> kantenAbschnitte = Arrays.stream(kanten)
			.map(k -> new AbschnittsweiserKantenSeitenBezug(
				k, LinearReferenzierterAbschnitt.of(0, 1), Seitenbezug.LINKS))
			.collect(Collectors.toSet());

		final MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			kantenAbschnitte,
			Set.of(),
			Collections.emptySet()
		);
		return withDefaultValues().netzbezug(netzbezug);
	}

	public static Massnahme.MassnahmeBuilder withKnoten(Knoten... knoten) {
		Set<Knoten> knotenSet = Arrays.stream(knoten).collect(Collectors.toSet());

		final MassnahmeNetzBezug netzbezug = new MassnahmeNetzBezug(
			Set.of(),
			Set.of(),
			knotenSet
		);
		return withDefaultValues().netzbezug(netzbezug);
	}

	public static Massnahme.MassnahmeBuilder withDefaultValuesAndOrganisation(Verwaltungseinheit organisation) {
		return withDefaultValues()
			.baulastZustaendiger(organisation)
			.unterhaltsZustaendiger(organisation)
			.zustaendiger(organisation);
	}

	public static Massnahme.MassnahmeBuilder withDefaultValuesBenutzerAndOrganisation(
		Benutzer benutzer,
		Verwaltungseinheit organisation
	) {
		return withDefaultValuesAndOrganisation(organisation).benutzerLetzteAenderung(benutzer);
	}
}
