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

import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.dokument.schnittstelle.AddDokumentCommand;
import de.wps.radvis.backend.massnahme.domain.MassnahmeService;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.valueObject.Konzeptionsquelle;
import de.wps.radvis.backend.massnahme.domain.valueObject.Umsetzungsstatus;
import de.wps.radvis.backend.netz.domain.bezug.AbschnittsweiserKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.bezug.PunktuellerKantenSeitenBezug;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.netz.schnittstelle.command.KnotenNetzbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.NetzbezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.PunktuellerKantenSeitenBezugCommand;
import de.wps.radvis.backend.netz.schnittstelle.command.SeitenabschnittsKantenBezugCommand;
import lombok.NonNull;

public class MassnahmeGuard {

	private final NetzService netzService;
	private final ZustaendigkeitsService zustaendigkeitsService;
	private final BenutzerResolver benutzerResolver;
	private final MassnahmeService massnahmeService;

	public MassnahmeGuard(@NonNull ZustaendigkeitsService zustaendigkeitsService,
		@NonNull NetzService netzService, @NonNull BenutzerResolver benutzerResolver,
		@NonNull MassnahmeService massnahmeService) {
		this.netzService = netzService;
		this.zustaendigkeitsService = zustaendigkeitsService;
		this.benutzerResolver = benutzerResolver;
		this.massnahmeService = massnahmeService;
	}

	public void saveMassnahme(Authentication authentication, SaveMassnahmeCommand command) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		Massnahme alteMassnahme = massnahmeService.get(command.getId());
		boolean hasUmsetzungsstatusChanged = !alteMassnahme.getUmsetzungsstatus().equals(command.getUmsetzungsstatus());
		if (hasUmsetzungsstatusChanged) {
			assertDarfUmsetzungsstatusAendern(aktiverBenutzer, alteMassnahme.getUmsetzungsstatus(),
				command.getUmsetzungsstatus(), command.getKonzeptionsquelle());
		}

		assertDarfMassnahmenBearbeiten(aktiverBenutzer, command.getNetzbezug());
	}

	private void assertDarfUmsetzungsstatusAendern(Benutzer aktiverBenutzer, Umsetzungsstatus alterUmsetzungsstatus,
		Umsetzungsstatus neuerUmsetzungsstatus, Konzeptionsquelle konzeptionsquelle) {
		assertDarfUmsetzungsstatusSetzen(aktiverBenutzer, neuerUmsetzungsstatus, konzeptionsquelle);

		if (aktiverBenutzer.hatRecht(Recht.MASSNAHMEN_STORNIEREN)) {
			return;
		}

		if (!Konzeptionsquelle.isRadNetzKonzeptionsquelle(konzeptionsquelle)) {
			return;
		}

		if (alterUmsetzungsstatus.equals(neuerUmsetzungsstatus)) {
			return;
		}

		if (alterUmsetzungsstatus.equals(Umsetzungsstatus.STORNIERT_NICHT_ERFORDERLICH)
			|| alterUmsetzungsstatus.equals(Umsetzungsstatus.STORNIERT_ENGSTELLE)) {
			throw new AccessDeniedException(
				"Sie sind nicht berechtigt, den Status von stornierten Maßnahmen zu ändern.");
		}

	}

	private void assertDarfUmsetzungsstatusSetzen(Benutzer aktiverBenutzer, Umsetzungsstatus neuerUmsetzungsstatus,
		Konzeptionsquelle konzeptionsquelle) {
		if (aktiverBenutzer.hatRecht(Recht.MASSNAHMEN_STORNIEREN)) {
			return;
		}

		if (!Konzeptionsquelle.isRadNetzKonzeptionsquelle(konzeptionsquelle)) {
			return;
		}

		if (neuerUmsetzungsstatus.equals(Umsetzungsstatus.STORNIERT_NICHT_ERFORDERLICH)
			|| neuerUmsetzungsstatus.equals(Umsetzungsstatus.STORNIERT_ENGSTELLE)) {
			throw new AccessDeniedException("Sie sind nicht berechtigt, Maßnahmen zu stornieren.");
		}
	}

	public boolean darfMassnahmeBearbeiten(Benutzer benutzer, Massnahme massnahme) {
		if (benutzer.hatRecht(Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN)) {
			return true;
		}

		if (benutzer.hatRecht(Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN)) {
			boolean hasKanteOhneZustaendigkeit = massnahme.getNetzbezug().getImmutableKantenAbschnittBezug().stream()
				.anyMatch(abschnittsweiserKantenSeitenBezug -> !zustaendigkeitsService
					.istImZustaendigkeitsbereich(abschnittsweiserKantenSeitenBezug.getKante(), benutzer));

			if (hasKanteOhneZustaendigkeit) {
				return false;
			}

			boolean hasKnotenOhneZustaendigkeit = massnahme.getNetzbezug().getImmutableKnotenBezug().stream()
				.anyMatch(knotenBezug -> !zustaendigkeitsService
					.istImZustaendigkeitsbereich(knotenBezug, benutzer));

			if (hasKnotenOhneZustaendigkeit) {
				return false;
			}

			boolean hasPunktuellenKantenbezugOhneZustaendigkeit = massnahme.getNetzbezug()
				.getImmutableKantenPunktBezug().stream().anyMatch(
					punktuellerKantenSeitenBezug -> !zustaendigkeitsService.istImZustaendigkeitsbereich(
						punktuellerKantenSeitenBezug.getKante(), benutzer));

			if (hasPunktuellenKantenbezugOhneZustaendigkeit) {
				return false;
			}

			return true;
		}

		return false;
	}

	public boolean canMassnahmeLoeschen(Benutzer benutzer, Massnahme massnahme) {
		return darfMassnahmeBearbeiten(benutzer, massnahme) && !massnahme.isRadNETZMassnahme();
	}

	public void deleteMassnahme(Authentication authentication, Massnahme massnahme) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		assertDarfMassnahmenBearbeiten(aktiverBenutzer, massnahme.getNetzbezug());
		if (massnahme.isRadNETZMassnahme()) {
			throw new AccessDeniedException("RadNETZ-Massnahmen können nicht gelöscht werden.");
		}
	}

	public void createMassnahme(Authentication authentication, CreateMassnahmeCommand command) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		assertDarfUmsetzungsstatusSetzen(aktiverBenutzer, command.getUmsetzungsstatus(),
			command.getKonzeptionsquelle());
		assertDarfMassnahmenBearbeiten(aktiverBenutzer, command.getNetzbezug());
	}

	public void saveUmsetzungsstand(Authentication authentication, SaveUmsetzungsstandCommand command) {
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);

		Optional<MassnahmeNetzBezug> netzbezugByUmsetzungsstandId = this.massnahmeService
			.getNetzbezugByUmsetzungsstandId(
				command.getId());

		if (netzbezugByUmsetzungsstandId.isEmpty()) {
			throw new AccessDeniedException("Der Netzbezug der Massnahme konnte nicht ermittelt werden.");
		}

		MassnahmeNetzBezug netzbezug = netzbezugByUmsetzungsstandId.get();

		assertDarfMassnahmenBearbeiten(aktiverBenutzer, netzbezug);
	}

	public void starteUmsetzungsstandsabfrage(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!(benutzer.hatRecht(Recht.UMSETZUNGSSTANDSABFRAGEN_STARTEN))) {
			throw new AccessDeniedException("Sie sind nicht berechtigt Umsetzungsstandsabfragen zu starten.");
		}
	}

	public void getUmsetzungsstandAuswertung(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!(benutzer.hatRecht(Recht.UMSETZUNGSSTANDSABFRAGEN_AUSWERTEN))) {
			throw new AccessDeniedException("Sie sind nicht berechtigt Umsetzungsstandsabfragen auszuwerten.");
		}
	}

	private void assertDarfMassnahmenBearbeiten(Benutzer aktiverBenutzer, MassnahmeNetzBezug netzbezug) {
		if (aktiverBenutzer.hatRecht(Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN)) {
			return;
		}

		if (!aktiverBenutzer.hatRecht(Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN)) {
			throw new AccessDeniedException("Sie sind nicht berechtigt Massnahmen zu bearbeiten.");
		}

		assertZustaendigkeitsbereich(aktiverBenutzer, netzbezug);
	}

	private void assertDarfMassnahmenBearbeiten(Benutzer aktiverBenutzer, NetzbezugCommand netzbezugCommand) {
		if (aktiverBenutzer.hatRecht(Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN)) {
			return;
		}

		if (!aktiverBenutzer.hatRecht(Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN)) {
			throw new AccessDeniedException("Sie sind nicht berechtigt Massnahmen zu bearbeiten.");
		}

		Stream<Kante> kantenAbschnitte = netzbezugCommand.getKantenBezug().stream()
			.map(SeitenabschnittsKantenBezugCommand::getKanteId)
			.map(netzService::getKante);
		Stream<Knoten> knoten = netzbezugCommand.getKnotenBezug().stream()
			.map(KnotenNetzbezugCommand::getKnotenId)
			.map(netzService::getKnoten);
		Stream<Kante> kantenPunkte = netzbezugCommand.getPunktuellerKantenBezug().stream()
			.map(PunktuellerKantenSeitenBezugCommand::getKanteId)
			.map(netzService::getKante);

		zustaendigkeitsService.assertAllGeometriesInZustaendigkeitsbereich(aktiverBenutzer, kantenAbschnitte, knoten,
			kantenPunkte);
	}

	private void assertZustaendigkeitsbereich(Benutzer aktiverBenutzer, MassnahmeNetzBezug netzbezug) {
		Stream<Kante> kanteStream = netzbezug.getImmutableKantenAbschnittBezug().stream()
			.map(AbschnittsweiserKantenSeitenBezug::getKante);

		Stream<Knoten> knotenStream = netzbezug.getImmutableKnotenBezug().stream();

		Stream<Kante> punktuelleNetzbezuegeStream = netzbezug.getImmutableKantenPunktBezug()
			.stream()
			.map(PunktuellerKantenSeitenBezug::getKante);

		zustaendigkeitsService.assertAllGeometriesInZustaendigkeitsbereich(aktiverBenutzer, kanteStream, knotenStream,
			punktuelleNetzbezuegeStream);
	}

	public void deleteDatei(Authentication authentication, Long massnahmeId) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN) &&
			!benutzer.hatRecht(Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN)) {
			throw new AccessDeniedException("Fehlende Berechtigung zum Entfernen von Dateien an Massnahmen.");
		}

		if (benutzer.hatRecht(Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN)) {
			return;
		}

		MassnahmeNetzBezug netzbezug = massnahmeService.get(massnahmeId).getNetzbezug();

		assertZustaendigkeitsbereich(benutzer, netzbezug);
	}

	public void uploadDatei(Long massnahmeId, AddDokumentCommand command, MultipartFile file,
		Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN) &&
			!benutzer.hatRecht(Recht.MASSNAHME_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN_VEROEFFENTLICHEN)) {
			throw new AccessDeniedException("Fehlende Berechtigung zum Hochladen von Dateien an Massnahmen.");
		}

		if (benutzer.hatRecht(Recht.ALLE_MASSNAHMEN_ERFASSEN_BEARBEITEN)) {
			return;
		}

		MassnahmeNetzBezug netzbezug = massnahmeService.get(massnahmeId).getNetzbezug();

		assertZustaendigkeitsbereich(benutzer, netzbezug);
	}

	public void getUmsetzungsstandsabfrageVorschau(Authentication authentication) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!(benutzer.hatRecht(Recht.UMSETZUNGSSTANDSABFRAGEN_STARTEN))) {
			throw new AccessDeniedException("Sie sind nicht berechtigt Umsetzungsstandsabfragen zu starten.");
		}

	}

	public void archivieren(Authentication authentication, MassnahmenArchivierenCommand command) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(Recht.MASSNAHMEN_ARCHIVIEREN)) {
			throw new AccessDeniedException("Sie sind nicht berechtigt Maßnahmen zu archivieren.");
		}
	}

	public void unarchivieren(Authentication authentication, Long id) {
		Benutzer benutzer = benutzerResolver.fromAuthentication(authentication);
		if (!benutzer.hatRecht(Recht.MASSNAHMEN_ARCHIVIEREN)) {
			throw new AccessDeniedException("Sie sind nicht berechtigt, die Archivierung für Maßnahmen aufzuheben.");
		}
	}
}
