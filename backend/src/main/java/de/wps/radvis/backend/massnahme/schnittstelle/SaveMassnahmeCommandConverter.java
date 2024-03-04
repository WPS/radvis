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

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.KnotenResolver;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.schnittstelle.command.KnotenNetzbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.PunktuellerKantenSeitenBezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SeitenabschnittsKantenBezugCommand;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public class SaveMassnahmeCommandConverter {

	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final KanteResolver kantenResolver;
	private final BenutzerResolver benutzerResolver;
	private final KnotenResolver knotenResolver;

	public SaveMassnahmeCommandConverter(
		KanteResolver kantenResolver,
		VerwaltungseinheitResolver verwaltungseinheitResolver,
		BenutzerResolver benutzerResolver,
		KnotenResolver knotenResolver) {
		require(kantenResolver, notNullValue());
		require(knotenResolver, notNullValue());
		require(verwaltungseinheitResolver, notNullValue());
		require(benutzerResolver, notNullValue());
		this.kantenResolver = kantenResolver;
		this.benutzerResolver = benutzerResolver;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
		this.knotenResolver = knotenResolver;
	}

	public void apply(Authentication authentication, SaveMassnahmeCommand command, Massnahme massnahme) {
		Verwaltungseinheit baulastZustaendiger = null;
		if (command.getBaulastZustaendigerId() != null) {
			baulastZustaendiger = verwaltungseinheitResolver.resolve(command.getBaulastZustaendigerId());
		}
		Verwaltungseinheit unterhaltsZustaendiger = null;
		if (command.getUnterhaltsZustaendigerId() != null) {
			unterhaltsZustaendiger = verwaltungseinheitResolver.resolve(command.getUnterhaltsZustaendigerId());
		}
		Verwaltungseinheit zustaendiger = null;
		if (command.getZustaendigerId() != null) {
			zustaendiger = verwaltungseinheitResolver.resolve(command.getZustaendigerId());
		}
		NetzbezugCommand netzbezugCommand = command.getNetzbezug();
		MassnahmeNetzBezug netzbezug = createNetzbezug(netzbezugCommand);

		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		massnahme.update(
			command.getBezeichnung(),
			command.getMassnahmenkategorien(),
			netzbezug,
			command.getDurchfuehrungszeitraum(),
			command.getUmsetzungsstatus(),
			command.getVeroeffentlicht(),
			command.getPlanungErforderlich(),
			command.getMaViSID(),
			command.getVerbaID(),
			command.getLgvfgid(),
			command.getPrioritaet(),
			command.getKostenannahme(),
			command.getNetzklassen(),
			aktiverBenutzer,
			LocalDateTime.now(),
			baulastZustaendiger,
			unterhaltsZustaendiger,
			zustaendiger,
			command.getMassnahmeKonzeptID(),
			command.getSollStandard(),
			command.getHandlungsverantwortlicher(),
			command.getKonzeptionsquelle(),
			command.getSonstigeKonzeptionsquelle(),
			command.getRealisierungshilfe());
	}

	private MassnahmeNetzBezug createNetzbezug(NetzbezugCommand netzbezugCommand) {
		final Set<AbschnittsweiserKantenSeitenBezug> kantenSeitenAbschnitte = netzbezugCommand.getKantenBezug()
			.stream().map(this::createKantenSeitenAbschnitt)
			.collect(Collectors.toSet());
		final Set<PunktuellerKantenSeitenBezug> kantenSeitenPunkte = netzbezugCommand.getPunktuellerKantenBezug()
			.stream().map(this::createPunktuellerKantenSeitenBezug)
			.collect(Collectors.toSet());
		final Set<Knoten> knoten = new HashSet<>(knotenResolver.getKnoten(
			netzbezugCommand.getKnotenBezug().stream().map(KnotenNetzbezugCommand::getKnotenId).collect(
				Collectors.toSet())));
		return new MassnahmeNetzBezug(kantenSeitenAbschnitte, kantenSeitenPunkte, knoten);
	}

	private AbschnittsweiserKantenSeitenBezug createKantenSeitenAbschnitt(
		SeitenabschnittsKantenBezugCommand seitenabschnittsKantenBezugCommand) {

		Kante kante = kantenResolver.getKante(seitenabschnittsKantenBezugCommand.getKanteId());
		LinearReferenzierterAbschnitt linearReferenzierterAbschnitt = seitenabschnittsKantenBezugCommand
			.getLinearReferenzierterAbschnitt();
		Seitenbezug seitenbezug = seitenabschnittsKantenBezugCommand.getSeitenbezug();
		return new AbschnittsweiserKantenSeitenBezug(kante, linearReferenzierterAbschnitt, seitenbezug);
	}

	private PunktuellerKantenSeitenBezug createPunktuellerKantenSeitenBezug(
		PunktuellerKantenSeitenBezugCommand punktuellerKantenSeitenBezugCommand) {

		Kante kante = kantenResolver.getKante(punktuellerKantenSeitenBezugCommand.getKanteId());
		LineareReferenz lineareReferenz = LineareReferenz.of(punktuellerKantenSeitenBezugCommand.getLineareReferenz());
		Seitenbezug seitenbezug = Seitenbezug.BEIDSEITIG;
		return new PunktuellerKantenSeitenBezug(kante, lineareReferenz, seitenbezug);
	}

}
