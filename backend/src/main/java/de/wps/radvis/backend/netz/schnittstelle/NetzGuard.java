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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.schnittstelle.GeoJsonConverter;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.schnittstelle.command.ChangeSeitenbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.CreateKanteCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveFuehrungsformAttributGruppeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveGeschwindigkeitAttributGruppeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteAttributeCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteFahrtrichtungCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKanteVerlaufCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveKnotenCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SaveZustaendigkeitAttributGruppeCommand;

public class NetzGuard {

	private final NetzService netzService;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final BenutzerResolver benutzerResolver;

	public NetzGuard(NetzService netzService,
		ZustaendigkeitsService zustaendigkeitsService, BenutzerResolver benutzerResolver) {
		this.netzService = netzService;
		this.zustaendigkeitsService = zustaendigkeitsService;
		this.benutzerResolver = benutzerResolver;
	}

	public void saveKnoten(
		Authentication authentication,
		SaveKnotenCommand command) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		authorizeEditKnoten(command.getId(), aktiverBenutzer);
	}

	public void saveKanteAllgemein(
		Authentication authentication,
		List<SaveKanteAttributeCommand> commands) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		Set<Long> kanteIds = commands.stream().map(SaveKanteAttributeCommand::getKanteId)
			.collect(Collectors.toSet());
		authorizeEditKanten(kanteIds, aktiverBenutzer);
		for (SaveKanteAttributeCommand command : commands) {
			authorizeRadNetzVerlegen(command.getKanteId(), command.getNetzklassen(), aktiverBenutzer);
			authorizeKreisnetzVerlegen(command.getKanteId(), command.getNetzklassen(), aktiverBenutzer);
		}
	}

	public void changeSeitenbezug(
		Authentication authentication,
		List<ChangeSeitenbezugCommand> commands) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		Set<Long> kanteIds = commands.stream()
			.map(ChangeSeitenbezugCommand::getId)
			.collect(Collectors.toSet());

		authorizeEditKanten(kanteIds, aktiverBenutzer);
	}

	public void saveGeschwindigkeitAttributGruppen(
		Authentication authentication,
		List<SaveGeschwindigkeitAttributGruppeCommand> commands) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		Set<Long> kanteIds = commands.stream()
			.map(SaveGeschwindigkeitAttributGruppeCommand::getKanteId)
			.collect(Collectors.toSet());

		authorizeEditKanten(kanteIds, aktiverBenutzer);
	}

	public void saveKanteVerlauf(
		Authentication authentication,
		List<SaveKanteVerlaufCommand> commands) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		Set<Long> kanteIds = commands.stream()
			.map(SaveKanteVerlaufCommand::getId)
			.collect(Collectors.toSet());

		authorizeEditKanten(kanteIds, aktiverBenutzer);
	}

	public void saveFuehrungsformAttributGruppen(
		Authentication authentication,
		List<SaveFuehrungsformAttributGruppeCommand> commands) {

		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		Set<Long> kanteIds = commands.stream()
			.map(SaveFuehrungsformAttributGruppeCommand::getKanteId)
			.collect(Collectors.toSet());

		authorizeEditKanten(kanteIds, aktiverBenutzer);
	}

	public void saveZustaendigkeitAttributGruppen(
		Authentication authentication,
		List<SaveZustaendigkeitAttributGruppeCommand> commands) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		Set<Long> kanteIds = commands.stream()
			.map(SaveZustaendigkeitAttributGruppeCommand::getKanteId)
			.collect(Collectors.toSet());

		authorizeEditKanten(kanteIds, aktiverBenutzer);
	}

	public void saveFahrtrichtungAttributGruppe(
		Authentication authentication,
		List<SaveKanteFahrtrichtungCommand> commands) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		Set<Long> kanteIds = commands.stream()
			.map(SaveKanteFahrtrichtungCommand::getKanteId)
			.collect(Collectors.toSet());

		authorizeEditKanten(kanteIds, aktiverBenutzer);
	}

	public void createKante(CreateKanteCommand command, Authentication authentication) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		if (!aktiverBenutzer.hatRecht(Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT)
			&& !aktiverBenutzer.hatRecht(Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Kanten zu erstellen.");
		}

		// Wenn beide Knoten der zu erstellenden Kante im Zustaendigkeitsbereich liegen, dann liegt auch
		// die zu erstellende Kante im Zustaendigkeitsbereich des aktiven Benutzers
		Knoten vonKnoten = netzService.getKnoten(command.getVonKnotenId());
		Knoten bisKnoten;
		if (Objects.nonNull(command.getBisKnotenId())) {
			bisKnoten = netzService.getKnoten(command.getBisKnotenId());
		} else {
			bisKnoten = Knoten.builder().quelle(QuellSystem.RadVis)
				.point(GeoJsonConverter.create3DJtsPointFromGeoJson(command.getBisKnotenCoor(),
					KoordinatenReferenzSystem.ETRS89_UTM32_N)).build();
		}

		if (!aktiverBenutzer.hatRecht(Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN)) {
			if (!zustaendigkeitsService.istImZustaendigkeitsbereich(vonKnoten, aktiverBenutzer) ||
				!zustaendigkeitsService.istImZustaendigkeitsbereich(bisKnoten, aktiverBenutzer)) {
				throw new AccessDeniedException("Die erstellte Kante liegt nicht in Ihrem Zuständigkeitsbereich.");
			}
		}

		if ((!vonKnoten.getQuelle().equals(QuellSystem.DLM) && !vonKnoten.getQuelle().equals(QuellSystem.RadVis))
			|| (!bisKnoten.getQuelle().equals(QuellSystem.DLM)
				&& !bisKnoten.getQuelle().equals(QuellSystem.RadVis))) {
			throw new AccessDeniedException("Es können nur Kanten zwischen DLM- oder RadVis-Knoten erstellt werden.");
		}
	}

	private void authorizeEditKanten(Set<Long> kanteIds, Benutzer benutzer) {

		if (!benutzer.hatRecht(Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT)
			&& !benutzer.hatRecht(Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Kanten zu bearbeiten.");
		}

		if (!benutzer.hatRecht(Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN)) {
			netzService.getKanten(kanteIds).forEach(kante -> {
				if (!zustaendigkeitsService.istImZustaendigkeitsbereich(kante, benutzer)) {
					throw new AccessDeniedException("Die Kante liegt nicht in Ihrem Zuständigkeitsbereich.");
				}
			});
		}

	}

	public void authorizeEditKnoten(Long knotenId, Benutzer benutzer) {
		if (!benutzer.hatRecht(Recht.BEARBEITUNG_VON_RADWEGSTRECKEN_DES_EIGENEN_GEOGRAPHISCHEN_ZUSTAENDIGKEIT)
			&& !benutzer.hatRecht(Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung Kanten zu bearbeiten.");
		}

		if (!benutzer.hatRecht(Recht.BEARBEITUNG_VON_ALLEN_RADWEGSTRECKEN)) {
			if (!zustaendigkeitsService.istImZustaendigkeitsbereich(netzService.getKnoten(knotenId), benutzer)) {
				throw new AccessDeniedException("Der Knoten liegt nicht in Ihrem Zuständigkeitsbereich.");
			}
		}
	}

	private void authorizeRadNetzVerlegen(long kanteId, Set<Netzklasse> neueNetzklassen, Benutzer benutzer) {
		if (benutzer.hatRecht(Recht.RADNETZ_ROUTENVERLEGUNGEN)) {
			return;
		}

		Set<Netzklasse> alteNetzklassen = netzService.getKante(kanteId).getKantenAttributGruppe().getNetzklassen();
		if (netzklasseHinzugefuegtOderEntfernt(alteNetzklassen, neueNetzklassen, Netzklasse.RADNETZ_NETZKLASSEN)
		) {
			throw new AccessDeniedException("Sie sind nicht berechtigt, die Netzklasse RadNETZ zu verändern.");
		}
	}

	private void authorizeKreisnetzVerlegen(long kanteId, Set<Netzklasse> neueNetzklassen, Benutzer benutzer) {
		if (benutzer.hatRecht(Recht.KREISNETZ_ROUTENVERLEGUNGEN)) {
			return;
		}

		Set<Netzklasse> alteNetzklassen = netzService.getKante(kanteId).getKantenAttributGruppe().getNetzklassen();
		if (netzklasseHinzugefuegtOderEntfernt(alteNetzklassen, neueNetzklassen, Netzklasse.KREISNETZ_NETZKLASSEN)
		) {
			throw new AccessDeniedException("Sie sind nicht berechtigt, die Netzklasse Kreisnetz zu verändern.");
		}
	}

	private boolean netzklasseHinzugefuegtOderEntfernt(Set<Netzklasse> alteNetzklassen,
		Set<Netzklasse> neueNetzklassen, Set<Netzklasse> zuPruefendeNetzklassen) {

		return zuPruefendeNetzklassen.stream()
			.anyMatch(netzklasse -> (!alteNetzklassen.contains(netzklasse) && neueNetzklassen.contains(netzklasse)) || // Hinzugefügt
				(alteNetzklassen.contains(netzklasse) && !neueNetzklassen.contains(netzklasse)) // Entfernt
			);
	}

	public void deleteRadVISKanteById(Long id, Authentication authentication) {
		Kante kante = netzService.getKante(id);
		if (!kante.getQuelle().equals(QuellSystem.RadVis)) {
			throw new AccessDeniedException("Nur RadVIS-Kanten können gelöscht werden.");
		}

		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!kannLoeschen(kante, benutzer)) {
			throw new AccessDeniedException("Sie haben nicht die Berechtigung diese Kante zu löschen.");
		}
	}

	public boolean isLoeschenErlaubt(Kante kante, Benutzer benutzer) {
		return kante.getQuelle().equals(QuellSystem.RadVis) && kannLoeschen(kante, benutzer);
	}

	private boolean kannLoeschen(Kante kante, Benutzer benutzer) {
		return benutzer.getRollen().contains(Rolle.RADVIS_ADMINISTRATOR) || netzService.wurdeAngelegtVon(kante,
			benutzer);
	}
}
