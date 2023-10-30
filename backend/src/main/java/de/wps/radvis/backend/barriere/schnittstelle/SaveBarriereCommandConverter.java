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

package de.wps.radvis.backend.barriere.schnittstelle;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import de.wps.radvis.backend.barriere.domain.entity.Barriere;
import de.wps.radvis.backend.barriere.domain.entity.BarriereNetzBezug;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.KnotenResolver;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.schnittstelle.command.KnotenNetzbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveNetzBezugCommandConverter;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;

public class SaveBarriereCommandConverter extends SaveNetzBezugCommandConverter {

	private final KnotenResolver knotenResolver;
	private final VerwaltungseinheitResolver verwaltungseinheitResolver;

	public SaveBarriereCommandConverter(KanteResolver kantenResolver, KnotenResolver knotenResolver,
		VerwaltungseinheitResolver verwaltungseinheitResolver) {
		super(kantenResolver);

		this.knotenResolver = knotenResolver;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
	}

	public Barriere convert(SaveBarriereCommand command) {
		return new Barriere(
			createNetzbezug(command.getNetzbezug()),
			verwaltungseinheitResolver.resolve(command.getVerantwortlicheOrganisation()),
			command.getBarrierenForm(),
			command.getVerbleibendeDurchfahrtsbreite(),
			command.getSicherung(),
			command.getMarkierung(),
			command.getBegruendung()
		);
	}

	public void apply(Barriere barriere, SaveBarriereCommand command) {
		barriere.update(
			createNetzbezug(command.getNetzbezug()),
			verwaltungseinheitResolver.resolve(command.getVerantwortlicheOrganisation()),
			command.getBarrierenForm(),
			command.getVerbleibendeDurchfahrtsbreite(),
			command.getSicherung(),
			command.getMarkierung(),
			command.getBegruendung()
		);
	}

	private BarriereNetzBezug createNetzbezug(BarriereNetzBezugCommand netzbezugCommand) {
		final Set<AbschnittsweiserKantenSeitenBezug> kantenSeitenAbschnitte = netzbezugCommand.getKantenBezug()
			.stream().map(this::createKantenSeitenAbschnitt)
			.collect(Collectors.toSet());
		final Set<PunktuellerKantenSeitenBezug> kantenSeitenPunkte = netzbezugCommand.getPunktuellerKantenBezug()
			.stream().map(this::createPunktuellerKantenSeitenBezug)
			.collect(Collectors.toSet());
		final HashSet<Knoten> knoten = new HashSet<>(knotenResolver.getKnoten(
			netzbezugCommand.getKnotenBezug().stream().map(KnotenNetzbezugCommand::getKnotenId).collect(
				Collectors.toSet())));
		return new BarriereNetzBezug(kantenSeitenAbschnitte, kantenSeitenPunkte, knoten);
	}
}
