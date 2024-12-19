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
import java.util.Set;

import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.KnotenResolver;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.schnittstelle.NetzbezugCommandConverter;
import de.wps.radvis.backend.netz.schnittstelle.command.NetzbezugCommand;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;

public class CreateMassnahmeCommandConverter extends NetzbezugCommandConverter<MassnahmeNetzBezug> {

	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final BenutzerResolver benutzerResolver;

	public CreateMassnahmeCommandConverter(
		KanteResolver kantenResolver,
		KnotenResolver knotenResolver,
		VerwaltungseinheitResolver verwaltungseinheitResolver,
		BenutzerResolver benutzerResolver) {
		super(kantenResolver, knotenResolver);
		require(verwaltungseinheitResolver, notNullValue());
		require(benutzerResolver, notNullValue());
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
		this.benutzerResolver = benutzerResolver;
	}

	public Massnahme convert(Authentication authentication, CreateMassnahmeCommand command) {
		require(authentication, notNullValue());

		NetzbezugCommand netzbezugCommand = command.getNetzbezug();
		MassnahmeNetzBezug netzbezug = createNetzbezug(netzbezugCommand);

		Verwaltungseinheit unterhaltsZustaendigerLoaded = null;
		if (command.getBaulastZustaendigerId() != null) {
			unterhaltsZustaendigerLoaded = verwaltungseinheitResolver.resolve(command.getBaulastZustaendigerId());
		}

		Verwaltungseinheit zustaendiger = verwaltungseinheitResolver.resolve(command.getZustaendigerId());

		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		return new Massnahme(
			command.getBezeichnung(),
			command.getMassnahmenkategorien(),
			netzbezug,
			command.getUmsetzungsstatus(),
			command.getVeroeffentlicht(),
			command.getPlanungErforderlich(),
			command.getDurchfuehrungszeitraum(),
			unterhaltsZustaendigerLoaded,
			zustaendiger,
			LocalDateTime.now(),
			aktiverBenutzer,
			command.getSollStandard(),
			command.getHandlungsverantwortlicher(),
			command.getKonzeptionsquelle(),
			command.getSonstigeKonzeptionsquelle());
	}

	@Override
	protected MassnahmeNetzBezug buildNetzbezug(Set<AbschnittsweiserKantenSeitenBezug> abschnitte,
		Set<PunktuellerKantenSeitenBezug> punkte, Set<Knoten> knoten) {
		return new MassnahmeNetzBezug(abschnitte, punkte, knoten);
	}
}
