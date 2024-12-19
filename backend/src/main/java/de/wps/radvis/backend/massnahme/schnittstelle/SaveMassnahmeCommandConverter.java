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

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

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

public class SaveMassnahmeCommandConverter extends NetzbezugCommandConverter<MassnahmeNetzBezug> {

	private final VerwaltungseinheitResolver verwaltungseinheitResolver;
	private final BenutzerResolver benutzerResolver;

	public SaveMassnahmeCommandConverter(
		KanteResolver kantenResolver,
		KnotenResolver knotenResolver,
		VerwaltungseinheitResolver verwaltungseinheitResolver,
		BenutzerResolver benutzerResolver) {
		super(kantenResolver, knotenResolver);
		require(verwaltungseinheitResolver, notNullValue());
		require(benutzerResolver, notNullValue());
		this.benutzerResolver = benutzerResolver;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
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

		if (!massnahme.canUpdateKonzeptionsquelle(command.getKonzeptionsquelle())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
				"Konzeptionsquelle " + command.getKonzeptionsquelle() + " ist nicht erlaubt");
		}

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

	@Override
	protected MassnahmeNetzBezug buildNetzbezug(Set<AbschnittsweiserKantenSeitenBezug> abschnitte,
		Set<PunktuellerKantenSeitenBezug> punkte,
		Set<Knoten> knoten) {
		return new MassnahmeNetzBezug(abschnitte, punkte, knoten);
	}
}
