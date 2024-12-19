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

package de.wps.radvis.backend.massnahme.domain;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.index.strtree.STRtree;

import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.common.domain.valueObject.CsvData;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.fahrradroute.domain.repository.FahrradrouteRepository;
import de.wps.radvis.backend.massnahme.domain.dbView.MassnahmeListenDbView;
import de.wps.radvis.backend.massnahme.domain.entity.Massnahme;
import de.wps.radvis.backend.massnahme.domain.entity.MassnahmeNetzBezug;
import de.wps.radvis.backend.massnahme.domain.entity.Umsetzungsstand;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeUmsetzungsstandViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.MassnahmeViewRepository;
import de.wps.radvis.backend.massnahme.domain.repository.UmsetzungsstandRepository;
import de.wps.radvis.backend.massnahme.domain.valueObject.MassnahmenPaketId;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.service.AbstractEntityWithNetzbezugService;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Transactional
@Slf4j
public class MassnahmeService extends AbstractEntityWithNetzbezugService<Massnahme> {
	private final MassnahmeRepository massnahmeRepository;
	private final MassnahmeViewRepository massnahmeViewRepository;
	private final UmsetzungsstandRepository umsetzungsstandRepository;
	private final MassnahmeUmsetzungsstandViewRepository massnahmeUmsetzungsstandViewRepository;
	private final MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService;
	private final KantenRepository kantenRepository;
	private final BenutzerService benutzerService;
	private final FahrradrouteRepository fahrradrouteRepository;
	private final double distanzMassnahmeZuFahrradrouteInMetern;

	public MassnahmeService(MassnahmeRepository massnahmeRepository,
		MassnahmeViewRepository massnahmeViewRepository,
		MassnahmeUmsetzungsstandViewRepository massnahmeUmsetzungsstandViewRepository,
		UmsetzungsstandRepository umsetzungsstandRepository,
		KantenRepository kantenRepository,
		MassnahmeNetzbezugAenderungProtokollierungsService massnahmeNetzbezugAenderungProtokollierungsService,
		BenutzerService benutzerService,
		FahrradrouteRepository fahrradrouteRepository,
		NetzService netzService,
		double distanzMassnahmeZuFahrradrouteInMetern,
		double erlaubteAbweichungKantenRematch) {
		super(massnahmeRepository, netzService, erlaubteAbweichungKantenRematch);
		this.massnahmeRepository = massnahmeRepository;
		this.massnahmeViewRepository = massnahmeViewRepository;
		this.massnahmeUmsetzungsstandViewRepository = massnahmeUmsetzungsstandViewRepository;
		this.massnahmeNetzbezugAenderungProtokollierungsService = massnahmeNetzbezugAenderungProtokollierungsService;
		this.umsetzungsstandRepository = umsetzungsstandRepository;
		this.kantenRepository = kantenRepository;
		this.benutzerService = benutzerService;
		this.fahrradrouteRepository = fahrradrouteRepository;
		this.distanzMassnahmeZuFahrradrouteInMetern = distanzMassnahmeZuFahrradrouteInMetern;
	}

	@Override
	public Massnahme get(Long massnahmeId) {
		return massnahmeRepository.findByIdAndGeloeschtFalse(massnahmeId).orElseThrow(EntityNotFoundException::new);
	}

	// Es kann pro PaketId bis zu zwei Maßnahmen geben: Eine für Start- und eine für Zielstandard
	public List<Massnahme> getMassnahmeByPaketId(String paketId) {
		return massnahmeRepository.findByMassnahmenPaketId(MassnahmenPaketId.of(paketId));
	}

	public Umsetzungsstand getUmsetzungsstand(Long umsetzungsstandId) {
		return umsetzungsstandRepository.findById(umsetzungsstandId).orElseThrow(EntityNotFoundException::new);
	}

	public Umsetzungsstand loadUmsetzungsstandForModification(Long id, Long version) {
		require(id, notNullValue());
		require(version, notNullValue());

		Umsetzungsstand umsetzungsstand = getUmsetzungsstand(id);

		if (!version.equals(umsetzungsstand.getVersion())) {
			throw new OptimisticLockException(
				"Ein/e andere Nutzer/in hat dieses Objekt bearbeitet. "
					+ "Bitte laden Sie das Objekt neu und führen Sie Ihre Änderungen erneut durch.");
		}

		return umsetzungsstand;
	}

	public Massnahme saveMassnahme(Massnahme massnahme) {
		return massnahmeRepository.save(massnahme);
	}

	public void saveUmsetzungsstand(Umsetzungsstand umsetzungsstand) {
		umsetzungsstandRepository.save(umsetzungsstand);
	}

	public Massnahme getMassnahmeByUmsetzungsstand(Umsetzungsstand umsetzungsstand) {
		return massnahmeRepository.findByUmsetzungsstandAndGeloeschtFalse(umsetzungsstand)
			.orElseThrow(EntityNotFoundException::new);
	}

	public Optional<MassnahmeNetzBezug> getNetzbezugByUmsetzungsstandId(Long umsetzungsstandId) {
		Optional<Umsetzungsstand> umsetzungsstandById = umsetzungsstandRepository.findById(umsetzungsstandId);
		if (umsetzungsstandById.isEmpty()) {
			return Optional.empty();
		}
		return massnahmeRepository.findByUmsetzungsstandAndGeloeschtFalse(umsetzungsstandById.get())
			.map(Massnahme::getNetzbezug);
	}

	public Set<MassnahmenPaketId> findAllMassnahmenPaketIds() {
		return massnahmeRepository.findAllMassnahmenPaketIds();
	}

	@SuppressWarnings("unchecked")
	public List<MassnahmeListenDbView> getAlleMassnahmenListenViews(
		Boolean historischeMassnahmenAnzeigen,
		Optional<Verwaltungseinheit> innerhalbVerwaltungseinheit,
		List<Long> nebenFahrradroutenIds) {
		Optional<MultiPolygon> bereich = innerhalbVerwaltungseinheit.flatMap(Verwaltungseinheit::getBereich);

		List<MassnahmeListenDbView> allInBereich = massnahmeViewRepository.findAllWithFilters(bereich,
			historischeMassnahmenAnzeigen);

		if (!nebenFahrradroutenIds.isEmpty()) {
			List<Geometry> allGeometries = fahrradrouteRepository.getAllGeometries(nebenFahrradroutenIds);
			if (!allGeometries.isEmpty()) {
				// Die einzelnen MultiLineStrings aufteilen und jedes einzelne Segment (also Strecke zwischen zwei
				// Koordinaten) in den Index einfügen, statt ganze (Multi)LineStrings einzufügen. Diese Segmente sind
				// natürlich wesentlich kleiner als die Fahrradroute selbst. Weiter unten nutzen wir die Envelopes von
				// Routen und Maßnahmen um diejenigen Maßnahmen in er Nähe von Routen zu finden. Ist nun eine gesamte
				// Fahrradroute mit ihrem riesigen Envelope (bei LRFW teilweise halb BW) im Index, dann ist so ziemlich
				// jede Maßnahme "in der Nähe", was die Idee von räumlichen Queries irgendwo absurd macht. Denn für jede
				// Maßnahme muss man nochmal exakt die Distanz berechnen um wirklich sicherzugehen, dass sie innerhalb
				// des konfigurierten Radius ist. Diese Distanzberechnung ist aber sehr teuer und wir wollen so wenig
				// davon wie möglich machen.
				// Deswegen fügen wir die kleineren Segmente ein. Die Query ist dann ein Stück weit genauer, heißt ganz
				// viele Maßnahmen werden direkt aussortiert, weil sie weit abseits der angefragten Fahrradrouten sind.
				// Wir brauchen dann also nur noch wenige exakte Distanzberechnungen, was uns viel zeit spart. Es mag
				// kontraintuitiv sein mehr Objekte in den Index einzufügen um schneller zu sein, aber so ist das
				// nunmal.
				STRtree strTree = new STRtree();
				allGeometries.stream()
					.flatMap(geom -> unwrap(geom).stream())
					.forEach(geom -> {
						Coordinate[] coordinates = geom.getCoordinates();

						for (int i = 0; i < coordinates.length - 1; i++) {
							Coordinate[] segmentCoords = new Coordinate[2];
							segmentCoords[0] = coordinates[i];
							segmentCoords[1] = coordinates[i + 1];

							LineString segment = KoordinatenReferenzSystem.ETRS89_UTM32_N
								.getGeometryFactory()
								.createLineString(segmentCoords);

							strTree.insert(segment.getEnvelopeInternal(), segment);
						}
					});

				List<MassnahmeListenDbView> result = allInBereich.stream()
					.filter(m -> {
						if (m.getGeometry() == null) {
							return false;
						}

						// Zunächst holen wir alle Fahrradrouten-Segmente, deren Envelopes mit dem erweiterten Envelope
						// der Maßnahme überlappen. Das ist eine sehr schnelle aber auch sehr ungenaue Operation. Daher
						// müssen alle Segmente nochmal durchgegangen werden, ob die Maßnahme wirklich innerhalb von
						// x Metern an einem der Segmente dran ist. Das ist eine verhältnißmäßig teure Operation,
						// deswegen die Vorfilterung nach Envelope auf den kleinen Segmenten, um möglichst wenig exakte
						// aber teure Berechnungen der Distanz machen zu müssen.
						Envelope bufferedMassnahmeEnvelope = m.getGeometry().getEnvelopeInternal();
						bufferedMassnahmeEnvelope.expandBy(distanzMassnahmeZuFahrradrouteInMetern);
						return strTree.query(bufferedMassnahmeEnvelope)
							.stream()
							.anyMatch(routeSegment -> {
								return ((Geometry) routeSegment).isWithinDistance(m.getGeometry(),
									distanzMassnahmeZuFahrradrouteInMetern);
							});
					})
					.toList();

				return result;
			}
		}

		return allInBereich;
	}

	private List<LineString> unwrap(Geometry multiLineString) {
		ArrayList<LineString> result = new ArrayList<>();

		for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
			Geometry childGeometry = multiLineString.getGeometryN(i);
			if (childGeometry instanceof MultiLineString) {
				result.addAll(unwrap((MultiLineString) childGeometry));
			} else if (childGeometry instanceof LineString) {
				result.add((LineString) childGeometry);
			} else {
				throw new RuntimeException("Unsupported geometry type " + childGeometry.getGeometryType()
					+ " during unwrap of MultiLineString.");
			}
		}

		return result;
	}

	public void haengeDateiAn(Long id, Dokument dokument) {
		Massnahme massnahme = get(id);
		haengeDateiAn(massnahme, dokument);
	}

	public void haengeDateiAn(Massnahme massnahme, Dokument dokument) {
		massnahme.addDokument(dokument);
		massnahmeRepository.save(massnahme);
	}

	public Dokument getDokument(Long massnahmeId, Long dokumentId) {
		return get(massnahmeId)
			.getDokumentListe()
			.getDokumente()
			.stream().filter(d -> dokumentId.equals(d.getId()))
			.findFirst()
			.orElseThrow(EntityNotFoundException::new);
	}

	public void deleteDokument(Long massnahmeId, Long dokumentId) {
		Massnahme massnahme = get(massnahmeId);
		massnahme.deleteDokument(dokumentId);
		massnahmeRepository.save(massnahme);
	}

	public CsvData getUmsetzungsstandAuswertung(List<Long> massnahmenIds) {
		List<String> headers = List.of(getUmsetzungsstandAuswertungKopfzeile());
		List<Map<String, String>> data = getUmsetzungsstandAuswertungRows(massnahmenIds).stream().map(row -> {
			Map<String, String> rowData = new HashMap<>();
			for (int i = 0; i < row.length; i++) {
				rowData.put(headers.get(i), row[i]);
			}
			return rowData;
		}).toList();

		return CsvData.of(data, headers);
	}

	private String[] getUmsetzungsstandAuswertungKopfzeile() {
		return new String[] {
			"Maßnahmennummer",
			"Bezeichnung",
			"Stadt-/Landkreis",
			"Gemeinde",
			"Baulastträger laut Maßnahmenblatt",
			"Umsetzungsstatus",
			"Länge der Kantensegmente in Meter",
			"Zugehörigkeit RadNETZ Alltag",
			"Zugehörigkeit RadNETZ Freizeit",
			"Zugehörigkeit RadNETZ Zielnetz",
			"1. Ist die Umsetzung erfolgt",
			"2. Umsetzung gemäß RadNETZ-Maßnahmenblatt",
			"3. Grund für Abweichung zum RadNETZ-Maßnahmenblatt",
			"4. Prüfung auf Einhaltung der RadNETZ-Qualitätsstandards",
			"5. Beschreibung der abweichenden RadNETZ-Maßnahme",
			"6. Kosten der RadNETZ-Maßnahme",
			"7. Anmerkung zu RadNETZ-Maßnahmen",
			"Vor-, Nachname und E-Mail-Adresse des Nutzers, der die Umfrage zuletzt bestätigt hat",
			"Zeitpunkt der letzten Bestätigung der Umfrage",
			"Umsetzungsstand-Status"
		};
	}

	private List<String[]> getUmsetzungsstandAuswertungRows(List<Long> ids) {
		return massnahmeUmsetzungsstandViewRepository.findAllById(ids).map(massnahme -> {
			return new String[] {
				massnahme.getMassnahmeKonzeptId(),
				massnahme.getBezeichnung(),
				massnahme.getKreis(),
				massnahme.getGemeinde(),
				massnahme.getBaulastOrganisationsArt(),
				massnahme.getUmsetzungsstatus().toString(),
				massnahme.getLaenge().toString(),
				massnahme.getNetzklassen().contains(Netzklasse.RADNETZ_ALLTAG) ? "Ja" : "Nein",
				massnahme.getNetzklassen().contains(Netzklasse.RADNETZ_FREIZEIT) ? "Ja" : "Nein",
				massnahme.getNetzklassen().contains(Netzklasse.RADNETZ_ZIELNETZ) ? "Ja" : "Nein",
				massnahme.getIstUmgesetzt(),
				massnahme.getUmsetzungGemaessMassnahmenblatt(),
				massnahme.getGrundFuerAbweichung(),
				massnahme.getPruefungQualitaetsstandardsErfolgt(),
				massnahme.getBeschreibungAbweichenderMassnahme(),
				massnahme.getKostenDerMassnahme(),
				massnahme.getAnmerkung(),
				massnahme.getBenutzerKontaktdaten(),
				massnahme.getLetzteAenderung(),
				massnahme.getUmsetzungsstandStatus().toString()
			};
		}).collect(Collectors.toList());
	}

	public List<Massnahme> findByMassnahmePaketId(MassnahmenPaketId massnahmePaketId) {
		List<Massnahme> massnahmen = massnahmeRepository.findByMassnahmenPaketId(massnahmePaketId);
		if (massnahmen.size() > 1) {
			log.warn("Es wurden mehr als eine ({}) Massnahmen für die Paket-ID {} gefunden!",
				massnahmen.size(),
				massnahmePaketId);
		}
		return massnahmen;

	}

	public List<Massnahme> findByKanteIdInNetzBezug(Long kanteId) {
		return massnahmeRepository.findByKantenInNetzBezug(List.of(kanteId));
	}

	public List<Massnahme> findByKantenIdsInNetzBezug(Collection<Long> kantenIds) {
		return massnahmeRepository.findByKantenInNetzBezug(kantenIds);
	}

	public List<Massnahme> findByKnotenIdInNetzBezug(long knotenId) {
		return massnahmeRepository.findByKnotenInNetzBezug(List.of(knotenId));
	}

	public boolean hatRadNETZNetzBezug(Massnahme massnahme) {
		return massnahme.getNetzbezug()
			.getImmutableKantenAbschnittBezug().stream()
			.anyMatch(
				abschnittsweiserKantenSeitenBezug -> abschnittsweiserKantenSeitenBezug.getKante().isRadNETZ())
			||
			massnahme.getNetzbezug()
				.getImmutableKantenPunktBezug().stream()
				.anyMatch(punktuellerKantenSeitenBezug -> punktuellerKantenSeitenBezug.getKante().isRadNETZ())
			||
			massnahme.getNetzbezug()
				.getImmutableKnotenBezug().stream()
				.anyMatch(
					knoten -> kantenRepository.getAdjazenteKanten(knoten).stream().anyMatch(Kante::isRadNETZ));
	}

	public void massnahmenArchivieren(List<Long> ids) {
		Iterable<Massnahme> massnahmen = massnahmeRepository.findAllById(ids);
		massnahmen.forEach(m -> {
			if (!m.isArchiviert()) {
				m.archivieren();
			}
		});
		massnahmeRepository.saveAll(massnahmen);
	}

	public Massnahme archivierungAufheben(Long id) {
		Massnahme massnahme = get(id);
		massnahme.archivierungAufheben();
		return saveMassnahme(massnahme);
	}

	@Override
	protected void protokolliereNetzBezugAenderungFuerGeloeschteKnoten(List<Massnahme> entitiesWithKnotenInNetzbezug,
		Knoten knoten, LocalDateTime datum, NetzAenderungAusloeser ausloeser) {
		log.debug("Erstelle Protokoll-Einträge für Löschung von Knoten {} in Maßnahmen {}", knoten.getId(),
			entitiesWithKnotenInNetzbezug.stream().map(e -> e.getId()).toList());
		massnahmeNetzbezugAenderungProtokollierungsService.protokolliereNetzBezugAenderungFuerGeloeschteKnoten(
			entitiesWithKnotenInNetzbezug, knoten.getId(), datum, ausloeser, knoten.getPoint(),
			benutzerService.getTechnischerBenutzer());
	}

	@Override
	protected Collection<? extends Massnahme> findByKnotenInNetzbezug(List<Long> knotenIds) {
		return massnahmeRepository.findByKnotenInNetzBezug(knotenIds);
	}

	@Override
	protected void protokolliereNetzBezugAenderungFuerGeloeschteKanten(List<Massnahme> entitiesWithKanteInNetzbezug,
		Long kanteId, Geometry geometry, LocalDateTime datum, NetzAenderungAusloeser ausloeser) {
		log.debug("Erstelle Protokoll-Einträge für Löschung von Kante {} in Maßnahmen {}", kanteId,
			entitiesWithKanteInNetzbezug.stream().map(e -> e.getId()).toList());

		massnahmeNetzbezugAenderungProtokollierungsService.protokolliereNetzBezugAenderungFuerGeloeschteKante(
			entitiesWithKanteInNetzbezug, kanteId, datum, ausloeser, geometry,
			benutzerService.getTechnischerBenutzer());
	}
}
