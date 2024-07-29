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

package de.wps.radvis.backend.netz.schnittstelle;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.LineareReferenz;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.KnotenResolver;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.AbstractNetzBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.schnittstelle.command.KnotenNetzbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.NetzbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.PunktuellerKantenSeitenBezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SeitenabschnittsKantenBezugCommand;

public abstract class NetzbezugCommandConverter<T extends AbstractNetzBezug> {

	private final KanteResolver kantenResolver;
	private final KnotenResolver knotenResolver;

	public NetzbezugCommandConverter(KanteResolver kantenResolver, KnotenResolver knotenResolver) {
		require(kantenResolver, notNullValue());
		require(knotenResolver, notNullValue());
		this.kantenResolver = kantenResolver;
		this.knotenResolver = knotenResolver;
	}

	protected T createNetzbezug(NetzbezugCommand netzbezugCommand) {
		final Set<AbschnittsweiserKantenSeitenBezug> kantenSeitenAbschnitte = netzbezugCommand.getKantenBezug()
			.stream().map(this::createKantenSeitenAbschnitt)
			.collect(Collectors.toSet());

		final Set<PunktuellerKantenSeitenBezug> kantenSeitenPunkte = netzbezugCommand.getPunktuellerKantenBezug()
			.stream().map(this::createPunktuellerKantenSeitenBezug)
			.collect(Collectors.toSet());

		final Set<Knoten> knoten = new HashSet<>(knotenResolver.getKnoten(
			netzbezugCommand.getKnotenBezug().stream()
				.map(KnotenNetzbezugCommand::getKnotenId)
				.collect(Collectors.toSet())));
		return buildNetzbezug(kantenSeitenAbschnitte, kantenSeitenPunkte, knoten);
	}

	abstract protected T buildNetzbezug(Set<AbschnittsweiserKantenSeitenBezug> abschnitte,
		Set<PunktuellerKantenSeitenBezug> punkte,
		Set<Knoten> knoten);

	private AbschnittsweiserKantenSeitenBezug createKantenSeitenAbschnitt(
		SeitenabschnittsKantenBezugCommand seitenabschnittsKantenBezugCommand) {

		Kante kante = kantenResolver.getKante(seitenabschnittsKantenBezugCommand.getKanteId());
		LinearReferenzierterAbschnitt abschnitt = seitenabschnittsKantenBezugCommand.getLinearReferenzierterAbschnitt();
		Seitenbezug seitenbezug = seitenabschnittsKantenBezugCommand.getKantenSeite();
		return new AbschnittsweiserKantenSeitenBezug(kante, abschnitt, seitenbezug);
	}

	private PunktuellerKantenSeitenBezug createPunktuellerKantenSeitenBezug(
		PunktuellerKantenSeitenBezugCommand punktuellerKantenSeitenBezugCommand) {

		Kante kante = kantenResolver.getKante(punktuellerKantenSeitenBezugCommand.getKanteId());
		LineareReferenz lineareReferenz = LineareReferenz.of(punktuellerKantenSeitenBezugCommand.getLineareReferenz());
		Seitenbezug seitenbezug = Seitenbezug.BEIDSEITIG;
		return new PunktuellerKantenSeitenBezug(kante, lineareReferenz, seitenbezug);
	}
}
