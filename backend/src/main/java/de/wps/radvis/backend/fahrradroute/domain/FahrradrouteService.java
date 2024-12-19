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

package de.wps.radvis.backend.fahrradroute.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hibernate.spatial.jts.EnvelopeAdapter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.context.event.EventListener;
import org.springframework.data.util.Lazy;

import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.BatchedCollectionIterator;
import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.FehlerprotokollEintrag;
import de.wps.radvis.backend.common.domain.entity.Importprotokoll;
import de.wps.radvis.backend.common.domain.service.AbstractVersionierteEntityService;
import de.wps.radvis.backend.common.domain.service.FehlerprotokollService;
import de.wps.radvis.backend.common.domain.service.ImportprotokollService;
import de.wps.radvis.backend.common.domain.valueObject.FehlerprotokollTyp;
import de.wps.radvis.backend.common.domain.valueObject.ImportprotokollTyp;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.fahrradroute.domain.dbView.FahrradrouteListenDbOhneGeomView;
import de.wps.radvis.backend.fahrradroute.domain.dbView.FahrradrouteListenDbView;
import de.wps.radvis.backend.fahrradroute.domain.entity.Fahrradroute;
import de.wps.radvis.backend.fahrradroute.domain.entity.FahrradrouteNetzBezugAenderung;
import de.wps.radvis.backend.fahrradroute.domain.entity.ProfilInformationenUpdateStatistik;
import de.wps.radvis.backend.fahrradroute.domain.entity.TfisImportProblem;
import de.wps.radvis.backend.fahrradroute.domain.entity.UpdateAbgeleiteteRoutenInfoStatistik;
import de.wps.radvis.backend.fahrradroute.domain.event.FahrradrouteCreatedEvent;
import de.wps.radvis.backend.fahrradroute.domain.event.FahrradrouteUpdatedEvent;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteNetzBezugAenderungRepository;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteViewRepository;
import de.wps.radvis.backend.fahrradroute.domain.valueObject.FahrradrouteTyp;
import de.wps.radvis.backend.matching.domain.event.CustomRoutingProfilesDeletedEvent;
import de.wps.radvis.backend.matching.domain.exception.KeineRouteGefundenException;
import de.wps.radvis.backend.matching.domain.repository.GraphhopperRoutingRepository;
import de.wps.radvis.backend.matching.domain.valueObject.ProfilRoutingResult;
import de.wps.radvis.backend.matching.domain.valueObject.RoutingResult;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.event.KanteErsetztEvent;
import de.wps.radvis.backend.netz.domain.event.KantenDeletedEvent;
import de.wps.radvis.backend.netz.domain.service.SackgassenService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.NetzBezugAenderungsArt;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FahrradrouteService extends AbstractVersionierteEntityService<Fahrradroute>
	implements FehlerprotokollService, ImportprotokollService {
	protected final FahrradrouteRepository fahrradrouteRepository;
	protected final FahrradrouteViewRepository fahrradrouteViewRepository;
	private final Lazy<GraphhopperRoutingRepository> graphhopperRoutingRepositorySupplier;
	private final FahrradrouteNetzBezugAenderungRepository netzBezugAenderungRepository;

	private final JobExecutionDescriptionRepository jobExecutionDescriptionRepository;
	private final BenutzerService benutzerService;
	private SackgassenService sackgassenService;
	private final double erlaubteAbweichungKantenRematch;

	public FahrradrouteService(FahrradrouteRepository fahrradrouteRepository,
		FahrradrouteViewRepository fahrradrouteViewRepository,
		Lazy<GraphhopperRoutingRepository> graphhopperRoutingRepositorySupplier,
		FahrradrouteNetzBezugAenderungRepository netzBezugAenderungRepository,
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository, BenutzerService benutzerService,
		SackgassenService sackgassenService, double erlaubteAbweichungKantenRematch) {
		super(fahrradrouteRepository);
		this.fahrradrouteRepository = fahrradrouteRepository;
		this.fahrradrouteViewRepository = fahrradrouteViewRepository;
		this.graphhopperRoutingRepositorySupplier = graphhopperRoutingRepositorySupplier;
		this.netzBezugAenderungRepository = netzBezugAenderungRepository;
		this.jobExecutionDescriptionRepository = jobExecutionDescriptionRepository;
		this.benutzerService = benutzerService;
		this.sackgassenService = sackgassenService;
		this.erlaubteAbweichungKantenRematch = erlaubteAbweichungKantenRematch;
	}

	public List<FahrradrouteListenDbView> getAlleFahrradrouteListenViews() {
		return fahrradrouteViewRepository.findAll();
	}

	public List<FahrradrouteListenDbOhneGeomView> getAlleFahrradrouteListenOhneGeomViews() {
		return fahrradrouteViewRepository.findAllWithoutFetchingGeom();
	}

	@Override
	public Fahrradroute get(Long id) {
		return fahrradrouteRepository.findByIdAndGeloeschtFalse(id).orElseThrow(() -> new EntityNotFoundException(
			String.format("Eine Fahrradroute mit der ID '%d' existiert nicht.", id)));
	}

	public Fahrradroute saveFahrradroute(Fahrradroute fahrradroute) {
		return fahrradrouteRepository.save(fahrradroute);
	}

	private boolean isNetzBezugAenderungProtokollNoetig(Fahrradroute fahrradroute) {
		return fahrradroute.getFahrradrouteTyp() == FahrradrouteTyp.RADVIS_ROUTE;
	}

	@EventListener
	public void onKanteErsetzt(KanteErsetztEvent event) {
		List<Fahrradroute> fahrradroutenOnKante = fahrradrouteRepository
			.findByKanteIdInNetzBezug(List.of(event.getZuErsetzendeKante().getId()));

		fahrradroutenOnKante.forEach(f -> {
			log.debug("Kante {} in Fahrradroute {} wird ersetzt...", event.getZuErsetzendeKante().getId(), f.getId());
			f.ersetzeKanteInNetzbezug(event.getZuErsetzendeKante(), event.getErsetztDurch(),
				erlaubteAbweichungKantenRematch);

			if (!f.getAbschnittsweiserKantenBezug().stream()
				.anyMatch(abschn -> abschn.getKante().equals(event.getZuErsetzendeKante()))) {
				event.getStatistik().anzahlAngepassterNetzbezuege++;
				log.debug("Kante {} in Fahrradoute {} wurde erfolgreich ersetzt.", event.getZuErsetzendeKante().getId(),
					f.getId());
			}
		});

		fahrradrouteRepository.saveAll(fahrradroutenOnKante);
	}

	@EventListener
	public void onKantenGeloescht(KantenDeletedEvent event) {
		log.debug("Protokolliere und ändere Netzbezug für Fahrradrouten");

		Benutzer technischerBenutzer = benutzerService.getTechnischerBenutzer();
		List<Long> kantenIds = event.getKantenIds();
		NetzAenderungAusloeser ausloeser = event.getAusloeser();

		ArrayList<Fahrradroute> fahrradrouten = new ArrayList<>();

		// Batching, da Hibernate/Postgres nur eine gewisse Anzahl an Parametern in "someId IN (...)"-Queries zulässt.
		BatchedCollectionIterator.iterate(
			event.getKantenIds(),
			1000,
			(kantenIdBatch, startIndex, endIndex) -> {
				log.debug("Lade Fahrradrouten für Kanten-Batch {} bis {}", startIndex, endIndex);
				fahrradrouten.addAll(fahrradrouteRepository.findByKanteIdInNetzBezug(kantenIdBatch));
			});

		log.debug("Protokolliere und ändere Netzbezug für {} Fahrradrouten mit IDs {}", fahrradrouten.size(),
			fahrradrouten.stream().map(f -> f.getId() + "").collect(Collectors.joining(", ")));

		for (Fahrradroute fahrradroute : fahrradrouten) {
			for (int i = 0; i < event.getKantenIds().size(); i++) {
				Long kanteId = event.getKantenIds().get(i);
				Geometry geometry = event.getGeometries().get(i);

				if (!ausloeser.equals(NetzAenderungAusloeser.RADVIS_KANTE_LOESCHEN) &&
					isNetzBezugAenderungProtokollNoetig(fahrradroute) &&
					fahrradroute.containsKanteInHauptrouteOrVariante(kanteId)) {
					boolean aenderungIstInHauptroute = fahrradroute.containsKante(kanteId);
					FahrradrouteNetzBezugAenderung fahrradrouteNetzBezugAenderung = new FahrradrouteNetzBezugAenderung(
						NetzBezugAenderungsArt.KANTE_GELOESCHT, kanteId, fahrradroute, technischerBenutzer,
						event.getDatum(), ausloeser,
						(LineString) geometry, aenderungIstInHauptroute);
					netzBezugAenderungRepository.save(fahrradrouteNetzBezugAenderung);
				}
			}

			fahrradroute.removeKantenFromNetzbezug(kantenIds);

			event.getStatistik().anzahlAngepassterNetzbezuege++;
		}

		log.debug("Speichere Fahrradrouten");
		fahrradrouteRepository.saveAll(fahrradrouten);

		log.debug("Netzbezugänderung beendet");
	}

	@EventListener
	public void onFahrradrouteCreated(FahrradrouteCreatedEvent event) {
		addAbgeleiteteRoutenInformation(event.getFahrradroute());
	}

	@EventListener
	public void onFahrradrouteUpdated(FahrradrouteUpdatedEvent event) {
		addAbgeleiteteRoutenInformation(event.getFahrradroute());
		fahrradrouteRepository.save(event.getFahrradroute());
	}

	@EventListener
	public void onCustomRoutingProfilesDeleted(CustomRoutingProfilesDeletedEvent event) {
		fahrradrouteRepository.setCustomRoutingProfileIdToDefaultWhereCustomRoutingProfileIdIn(
			event.getCustomProfilIds());
	}

	public void updateAbgeleiteteRoutenInformationVonRadvisUndTfis(
		UpdateAbgeleiteteRoutenInfoStatistik updateAbgeleiteteRoutenInfoStatistik) {
		// Toubiz-Routen muessen hier nicht geupdated werden, da sie in einem anderen Job naechtlich neu
		// importiert werden und dabei auch die AbgeleitetenRoutenInfos neu erzeugt werden
		Stream<Fahrradroute> alleNichtToubizFahrradrouten = fahrradrouteRepository.findAllByFahrradrouteTypNot(
			FahrradrouteTyp.TOUBIZ_ROUTE);

		fahrradrouteRepository.saveAll(alleNichtToubizFahrradrouten.peek(fahrradroute -> {
			log.info("{} {} wird upgedated...", fahrradroute.getName(), fahrradroute.getId());
			updateAbgeleiteteRoutenInfoStatistik.anzahlGeladeneRouten++;
			if (addAbgeleiteteRoutenInformation(fahrradroute)) {
				updateAbgeleiteteRoutenInfoStatistik.anzahlRoutenErfolgreichAktualisiert++;
				log.info("... erfolgreich");
			}
		}).collect(Collectors.toList()));
	}

	private boolean addAbgeleiteteRoutenInformation(Fahrradroute fahrradroute) {
		fahrradroute.getVarianten().forEach(fahrradrouteVariante -> {
			if (fahrradrouteVariante.getGeometrie().isEmpty()) {
				return; // Verhaelt sich im forEach-Loop wie sonst "continue"
			}
			try {
				Long customProfileId = fahrradroute.getCustomProfileId()
					.orElse(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID);
				RoutingResult routingResult = graphhopperRoutingRepositorySupplier.get()
					.route(Arrays.asList(fahrradrouteVariante.getGeometrie().get().getCoordinates()),
						customProfileId, false);
				fahrradrouteVariante.updateAbgeleiteteRoutenInformationen(routingResult.getAnstieg(),
					routingResult.getAbstieg());
			} catch (KeineRouteGefundenException e) {
				log.error("Konnte keine abgeleiteten Routeninformationen für FahrradrouteVariante {} ermitteln: {}",
					fahrradrouteVariante.getId() != null ? fahrradrouteVariante.getId()
						: fahrradroute.getName()
							+ fahrradrouteVariante.getKategorie().toString(),
					e.getMessage(), e);
				fahrradrouteVariante.updateAbgeleiteteRoutenInformationen(null, null);
			}
		});

		if (fahrradroute.getNetzbezugLineString().isEmpty()) {
			log.info(
				"Konnte keine abgeleiteten Routeninformationen für Fahrradroute {} ermitteln: Kein NetzbezugsLineString vorhanden!",
				fahrradroute.getId() != null ? fahrradroute.getId() : fahrradroute.getName());
			fahrradroute.updateAbgeleiteteRoutenInformationen(null, null);
			return false;
		}
		try {
			Long customProfileId = fahrradroute.getCustomProfileId()
				.orElse(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID);
			RoutingResult routingResult = graphhopperRoutingRepositorySupplier.get()
				.route(Arrays.asList(fahrradroute.getNetzbezugLineString().get().getCoordinates()), customProfileId,
					false);
			fahrradroute.updateAbgeleiteteRoutenInformationen(routingResult.getAnstieg(),
				routingResult.getAbstieg());
			return true;

		} catch (KeineRouteGefundenException e) {
			log.error("Konnte keine abgeleiteten Routeninformationen für Fahrradroute {} ermitteln: {}",
				fahrradroute.getId() != null ? fahrradroute.getId() : fahrradroute.getName(),
				e.getMessage(), e);
			fahrradroute.updateAbgeleiteteRoutenInformationen(null, null);
			return false;
		}
	}

	public void updateProfilEigenschaftenVonRadvisUndTfisRouten(
		ProfilInformationenUpdateStatistik profilInformationenUpdateStatistik) {
		// Toubiz-Routen muessen hier nicht geupdated werden, da sie in einem anderen Job naechtlich neu
		// importiert werden und dabei auch die ProfilInformationen neu erzeugt werden
		Stream<Fahrradroute> alleNichtToubizFahrradrouten = fahrradrouteRepository.findAllByFahrradrouteTypNot(
			FahrradrouteTyp.TOUBIZ_ROUTE);

		fahrradrouteRepository.saveAll(alleNichtToubizFahrradrouten.peek(fahrradroute -> {
			log.info("{} {} wird upgedated...", fahrradroute.getName(), fahrradroute.getId());
			profilInformationenUpdateStatistik.anzahlGeladeneRouten++;
			updateProfilEigenschaftenOf(fahrradroute, profilInformationenUpdateStatistik);
		}).collect(Collectors.toList()));
	}

	private void updateProfilEigenschaftenOf(Fahrradroute fahrradroute,
		ProfilInformationenUpdateStatistik profilInformationenUpdateStatistik) {
		fahrradroute.getNetzbezugLineString().ifPresentOrElse(
			netzbezugLineString -> {
				try {
					Long customProfileId = fahrradroute.getCustomProfileId()
						.orElse(GraphhopperRoutingRepository.DEFAULT_PROFILE_ID);
					ProfilRoutingResult profilRoutingResult = graphhopperRoutingRepositorySupplier.get()
						.routeMitProfileigenschaften(Arrays.asList(netzbezugLineString.getCoordinates()),
							customProfileId, false);

					fahrradroute.updateLinearReferenzierteProfilEigenschaften(
						profilRoutingResult.getLinearReferenzierteProfilEigenschaften());
					profilInformationenUpdateStatistik.anzahlProfilInfosAktualisiert++;
				} catch (KeineRouteGefundenException e) {
					log.error("Konnte keine neuen ProfilEigenschaften für Fahrradroute {} ermitteln: {}",
						fahrradroute.getId() != null ? fahrradroute.getId() : fahrradroute.getName(),
						e.getMessage(), e);
					profilInformationenUpdateStatistik.anzahlRoutingFehlgeschlagen++;
				}
			},
			() -> {
				log.info(
					"Konnte keine neuen ProfilEigenschaften für die Fahrradroute \"{}\" mit der id {} ermitteln: Kein NetzbezugsLineString vorhanden!",
					fahrradroute.getName(), fahrradroute.getId());
				profilInformationenUpdateStatistik.anzahlFehlenderNetzbezugLineString++;
			});
	}

	public Optional<ProfilRoutingResult> createStrecke(LineString stuetzpunkte, long customProfileId,
		boolean fahrtrichtungBeruecksichtigen) {
		try {
			return Optional
				.of(graphhopperRoutingRepositorySupplier.get()
					.routeMitProfileigenschaften(
						Arrays.asList(stuetzpunkte.getCoordinates()),
						customProfileId,
						fahrtrichtungBeruecksichtigen));
		} catch (KeineRouteGefundenException e) {
			return Optional.empty();
		}
	}

	@Override
	public List<? extends FehlerprotokollEintrag> getAktuelleFehlerprotokolle(FehlerprotokollTyp typ) {
		switch (typ) {
		case DLM_REIMPORT_JOB_FAHRRADROUTEN:
			return netzBezugAenderungRepository
				.findFahrradrouteNetzBezugAenderungByDatumAfter(LocalDateTime.now().minusDays(1));
		case TOUBIZ_IMPORT_FAHRRADROUTEN:
			return fahrradrouteRepository.findAllToubizImportProbleme();
		case TFIS_IMPORT_FAHRRADROUTEN:
			return fahrradrouteRepository.findAllTfisImportProbleme();
		case TFIS_IMPORT_LRFW:
			List<TfisImportProblem> allLrfwImportProbleme = fahrradrouteRepository.findAllLrfwImportProbleme();
			addSackgassen(allLrfwImportProbleme);
			return allLrfwImportProbleme;
		default:
			log.warn("Der FehlerprotokollService für {} ist nicht implementiert.", typ);
			return List.of();
		}
	}

	private void addSackgassen(List<TfisImportProblem> lrfwImportProbleme) {
		for (TfisImportProblem tfisImportProblem : lrfwImportProbleme) {
			if (tfisImportProblem.isHasNetzbezug()) {
				Set<Kante> gematchteKanten = fahrradrouteRepository
					.getKantenWithKnotenByFahrradroute(tfisImportProblem.getId());
				List<Point> sackgassenknoten = sackgassenService.bestimmeSackgassenknoten(gematchteKanten)
					.stream().map(k -> k.getPoint()).collect(Collectors.toList());
				tfisImportProblem.addSackgassen(sackgassenknoten);
			}
		}
	}

	@Override
	public List<? extends FehlerprotokollEintrag> getAktuelleFehlerprotokolleInBereich(FehlerprotokollTyp typ,
		Envelope bereich) {
		switch (typ) {
		case DLM_REIMPORT_JOB_FAHRRADROUTEN:
			return netzBezugAenderungRepository
				.findFahrradrouteNetzBezugAenderungByDatumAfterInBereich(
					LocalDateTime.now().minusDays(1),
					EnvelopeAdapter.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()));
		case TOUBIZ_IMPORT_FAHRRADROUTEN:
			return fahrradrouteRepository.findAllToubizImportProblemeInBereich(
				EnvelopeAdapter.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()));
		case TFIS_IMPORT_FAHRRADROUTEN:
			return fahrradrouteRepository.findAllTfisImportProblemeInBereich(
				EnvelopeAdapter.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()));
		case TFIS_IMPORT_LRFW:
			List<TfisImportProblem> allLrfwImportProblemeInBereich = fahrradrouteRepository
				.findAllLrfwImportProblemeInBereich(
					EnvelopeAdapter.toPolygon(bereich, KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid()));
			addSackgassen(allLrfwImportProblemeInBereich);
			return allLrfwImportProblemeInBereich;
		default:
			log.warn("Der FehlerprotokollService für {} ist nicht implementiert.", typ);
			return List.of();
		}
	}

	@Override
	public List<Importprotokoll> getAllImportprotokolleAfter(LocalDateTime after) {
		List<String> names = List.of(FahrradroutenTfisImportJob.JOB_NAME, FahrradroutenTfisUpdateJob.JOB_NAME,
			FahrradroutenToubizImportJob.JOB_NAME);

		return jobExecutionDescriptionRepository.findAllByNameInAfterOrderByExecutionStartDesc(after, names).stream()
			.map(
				jobExecutionDescription -> {
					String importQuelle = jobExecutionDescription.getName().toLowerCase().contains("tfis")
						? "TFIS"
						: "Toubiz";

					return new Importprotokoll(
						jobExecutionDescription,
						ImportprotokollTyp.FAHRRADROUTE,
						importQuelle);
				})
			.collect(Collectors.toList());
	}
}
