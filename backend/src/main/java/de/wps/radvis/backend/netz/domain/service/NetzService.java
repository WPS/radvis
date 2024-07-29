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

package de.wps.radvis.backend.netz.domain.service;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.geotools.geometry.jts.OffsetCurveBuilder;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.history.Revision;
import org.springframework.data.history.RevisionSort;
import org.springframework.data.util.Streamable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import de.wps.radvis.backend.auditing.domain.entity.RevInfo;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.entity.VersionedId;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.KnotenResolver;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteGeometrien;
import de.wps.radvis.backend.netz.domain.entity.KanteGeometryView;
import de.wps.radvis.backend.netz.domain.entity.KanteOsmWayIdsInsert;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenAttribute;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.event.KanteDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.KnotenDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.RadNetzZugehoerigkeitEntferntEvent;
import de.wps.radvis.backend.netz.domain.repository.FahrtrichtungAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.FuehrungsformAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.GeschwindigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.repository.ZustaendigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KantenSeite;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.Zustandsbeschreibung;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Transactional
@Slf4j
public class NetzService implements KanteResolver, KnotenResolver {

	private final KantenRepository kantenRepository;

	private final ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppenRepository;
	private final FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppenRepository;
	private final FahrtrichtungAttributGruppeRepository fahrtrichtungAttributGruppeRepository;
	private final GeschwindigkeitAttributGruppeRepository geschwindigkeitAttributGruppeRepository;
	private final KantenAttributGruppeRepository kantenAttributGruppeRepository;

	private final KnotenRepository knotenRepository;

	private final VerwaltungseinheitResolver verwaltungseinheitResolver;

	private final OffsetCurveBuilder curvebuilderLinks;
	private final OffsetCurveBuilder curvebuilderRechts;

	public NetzService(KantenRepository kantenRepository,
		KnotenRepository knotenRepository,
		ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppenRepository,
		FahrtrichtungAttributGruppeRepository fahrtrichtungAttributGruppeRepository,
		GeschwindigkeitAttributGruppeRepository geschwindigkeitAttributGruppeRepository,
		FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppenRepository,
		KantenAttributGruppeRepository kantenAttributGruppenRepository,
		VerwaltungseinheitResolver verwaltungseinheitResolver) {

		this.kantenRepository = kantenRepository;
		this.knotenRepository = knotenRepository;
		this.zustaendigkeitAttributGruppenRepository = zustaendigkeitAttributGruppenRepository;
		this.fahrtrichtungAttributGruppeRepository = fahrtrichtungAttributGruppeRepository;
		this.geschwindigkeitAttributGruppeRepository = geschwindigkeitAttributGruppeRepository;
		this.fuehrungsformAttributGruppenRepository = fuehrungsformAttributGruppenRepository;
		this.kantenAttributGruppeRepository = kantenAttributGruppenRepository;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;

		curvebuilderLinks = new OffsetCurveBuilder(10, 1);
		curvebuilderRechts = new OffsetCurveBuilder(-10, 1);
	}

	public long getAnzahlAdjazenterKanten(Knoten knoten) {
		return this.kantenRepository.getAnzahlAdjazenterKanten(knoten);
	}

	public boolean existsKante(LineString lineString, KantenAttribute kantenAttribute) {
		List<Kante> kandidaten = kantenRepository.getKantenByLineString(lineString);

		return kandidaten.stream().anyMatch(
			kante -> kante.getKantenAttributGruppe().getKantenAttribute().equals(kantenAttribute));
	}

	public Kante saveKante(Kante kante) {
		return kantenRepository.save(kante);
	}

	public List<Kante> saveKanten(Iterable<Kante> kanten) {
		return Streamable.of(kantenRepository.saveAll(kanten)).toList();
	}

	public void deleteKante(Kante kante) {
		require(kante.getQuelle().equals(QuellSystem.RadVis));
		RadVisDomainEventPublisher.publish(
			new KanteDeletedEvent(kante.getId(), kante.getGeometry(), NetzAenderungAusloeser.RADVIS_KANTE_LOESCHEN,
				LocalDateTime.now()));
		kantenRepository.delete(kante);
	}

	public Knoten saveKnoten(Knoten knoten) {
		return knotenRepository.save(knoten);
	}

	public Stream<Kante> findKanteByQuelle(QuellSystem quelle) {
		return kantenRepository.findKanteByQuelle(quelle);
	}

	public Set<Kante> getKantenInBereichNachQuelleUndIsAbgebildet(Envelope bereich, QuellSystem quelle) {
		return kantenRepository.getKantenInBereichNachQuelleUndIsAbgebildet(bereich, quelle);
	}

	public Stream<Kante> getKantenInBereichNachQuelle(Envelope bereich, QuellSystem quelle) {
		return kantenRepository.getKantenInBereichNachQuelle(bereich, quelle);
	}

	public List<Knoten> getKnotenInBereichNachQuelle(Envelope bereich, QuellSystem quellSystem) {
		return knotenRepository.getKnotenInBereichFuerQuelle(bereich, quellSystem);
	}

	public Stream<Kante> getKantenInBereichNachQuelleEagerFetchKantenAttribute(Envelope bereich, QuellSystem quelle) {
		return kantenRepository.getKantenInBereichNachQuelleEagerFetchKantenAttribute(bereich, quelle);
	}

	public List<Kante> getKantenInBereichNachQuelleList(Envelope bereich, QuellSystem quelle) {
		return kantenRepository.getKantenInBereichNachQuelleList(bereich, quelle);
	}

	public Set<Kante> getKantenInBereichMitNetzklassen(Envelope bereich,
		Set<NetzklasseFilter> netzklassen, boolean showDLM) {
		return kantenRepository.getKantenInBereichNachNetzklasse(bereich, netzklassen, showDLM);
	}

	public void buildIndices() {
		knotenRepository.buildIndex();
		kantenRepository.buildIndex();
	}

	public Kante loadKanteForModification(Long id, Long kantenVersion) {
		Kante kante = getKante(id);

		if (!kantenVersion.equals(kante.getVersion())) {
			throw new OptimisticLockException(
				"Ein/e andere Nutzer/in hat dieses Objekt bearbeitet. "
					+ "Bitte laden Sie das Objekt neu und führen Sie Ihre Änderungen erneut durch.");
		}

		return kante;
	}

	public Knoten loadKnotenForModification(Long id, Long knotenVersion) {
		Knoten knoten = knotenRepository.findById(id).orElseThrow();

		if (!knotenVersion.equals(knoten.getVersion())) {
			throw new OptimisticLockException(
				"Ein/e andere Nutzer/in hat dieses Objekt bearbeitet. "
					+ "Bitte laden Sie das Objekt neu und führen Sie Ihre Änderungen erneut durch.");
		}

		return knoten;
	}

	public ZustaendigkeitAttributGruppe loadZustaendigkeitAttributGruppeForModification(Long id, Long version) {
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe = zustaendigkeitAttributGruppenRepository.findById(id)
			.orElseThrow();

		if (!version.equals(zustaendigkeitAttributGruppe.getVersion())) {
			throw new OptimisticLockException(
				"Ein/e andere Nutzer/in hat dieses Objekt bearbeitet. "
					+ "Bitte laden Sie das Objekt neu und führen Sie Ihre Änderungen erneut durch.");
		}

		return zustaendigkeitAttributGruppe;
	}

	public KantenAttributGruppe loadKantenAttributGruppeForModification(Long id, Long version) {
		KantenAttributGruppe kantenAttributGruppe = kantenAttributGruppeRepository.findById(id)
			.orElseThrow();

		if (!version.equals(kantenAttributGruppe.getVersion())) {
			throw new OptimisticLockException(
				"Ein/e andere Nutzer/in hat dieses Objekt bearbeitet. "
					+ "Bitte laden Sie das Objekt neu und führen Sie Ihre Änderungen erneut durch.");
		}

		return kantenAttributGruppe;
	}

	public FuehrungsformAttributGruppe loadFuehrungsformAttributGruppeForModification(Long id, Long version) {
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe = fuehrungsformAttributGruppenRepository.findById(id)
			.orElseThrow();

		if (!version.equals(fuehrungsformAttributGruppe.getVersion())) {
			throw new OptimisticLockException(
				"Ein/e andere Nutzer/in hat dieses Objekt bearbeitet. "
					+ "Bitte laden Sie das Objekt neu und führen Sie Ihre Änderungen erneut durch.");
		}

		return fuehrungsformAttributGruppe;
	}

	public FahrtrichtungAttributGruppe loadFahrtrichtungAttributGruppeForModification(Long id, Long version) {
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe = fahrtrichtungAttributGruppeRepository.findById(id)
			.orElseThrow();

		if (!version.equals(fahrtrichtungAttributGruppe.getVersion())) {
			throw new OptimisticLockException(
				"Ein/e andere Nutzer/in hat dieses Objekt bearbeitet. "
					+ "Bitte laden Sie das Objekt neu und führen Sie Ihre Änderungen erneut durch.");
		}

		return fahrtrichtungAttributGruppe;
	}

	public GeschwindigkeitAttributGruppe loadGeschwindigkeitAttributGruppeForModification(Long id, Long version) {
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe = geschwindigkeitAttributGruppeRepository
			.findById(id).orElseThrow();

		if (!version.equals(geschwindigkeitAttributGruppe.getVersion())) {
			throw new OptimisticLockException(
				"Ein/e andere Nutzer/in hat dieses Objekt bearbeitet. "
					+ "Bitte laden Sie das Objekt neu und führen Sie Ihre Änderungen erneut durch.");
		}

		return geschwindigkeitAttributGruppe;
	}

	public KnotenOrtslage berechneOrtslage(Knoten knoten) {
		KnotenOrtslage knotenOrtslage = null;

		final List<Kante> adjazenteKanten = kantenRepository.getAdjazenteKanten(knoten);

		for (Kante kante : adjazenteKanten) {
			GeschwindigkeitAttribute geschwindigkeitsAttributeAmKnoten = kante.getGeschwindigkeitAttributeAnPunkt(
				knoten.getKoordinate());
			Optional<KantenOrtslage> kantenOrtslageOptional = geschwindigkeitsAttributeAmKnoten.getOrtslage();
			if (kantenOrtslageOptional.isEmpty()) {
				continue;
			}

			KantenOrtslage kantenOrtslage = kantenOrtslageOptional.get();

			if (knotenOrtslage == null) {
				if (kantenOrtslage.equals(KantenOrtslage.INNERORTS)) {
					knotenOrtslage = KnotenOrtslage.INNERORTS;
				} else {
					knotenOrtslage = KnotenOrtslage.AUSSERORTS;
				}
				continue;
			}

			if (knotenOrtslage.equals(KnotenOrtslage.INNERORTS)
				&& kantenOrtslage.equals(KantenOrtslage.AUSSERORTS)
				|| knotenOrtslage.equals(KnotenOrtslage.AUSSERORTS)
					&& kantenOrtslage.equals(KantenOrtslage.INNERORTS)) {
				return KnotenOrtslage.ORTSEINGANGSBEREICH;
			}
		}

		return knotenOrtslage;
	}

	public Geometry berechneNebenkante(Geometry kantenverlauf, KantenSeite seite) {
		return seite == KantenSeite.LINKS
			? curvebuilderLinks.offset(kantenverlauf)
			: curvebuilderRechts.offset(kantenverlauf);
	}

	public Kante createGrundnetzKante(Long vonKnotenId, Long bisKnotenId, Status status) {
		require(vonKnotenId, notNullValue());
		require(bisKnotenId, notNullValue());
		Knoten vonKnoten = getKnoten(vonKnotenId);
		Knoten bisKnoten = getKnoten(bisKnotenId);

		Kante kante = new Kante(vonKnoten, bisKnoten);
		kante.setGrundnetz(true);
		kante.getKantenAttributGruppe().getKantenAttribute().setStatus(status);
		return kantenRepository.save(kante);
	}

	public Kante createGrundnetzKanteWithNewBisKnoten(Long vonKnotenId, Point bisKnotenCoor, Status status) {
		require(vonKnotenId, notNullValue());
		require(bisKnotenCoor, notNullValue());
		Knoten vonKnoten = getKnoten(vonKnotenId);
		Knoten bisKnoten = Knoten.builder().quelle(QuellSystem.RadVis).point(bisKnotenCoor).build();

		Kante kante = new Kante(vonKnoten, bisKnoten);
		kante.setGrundnetz(true);
		kante.getKantenAttributGruppe().getKantenAttribute().setStatus(status);
		return kantenRepository.save(kante);
	}

	@Override
	public List<Kante> getKanten(Set<Long> ids) {
		return StreamSupport.stream(kantenRepository.findAllById(ids).spliterator(), false)
			.collect(Collectors.toList());
	}

	@Override
	public Kante getKante(Long kanteId) {
		return kantenRepository.findById(kanteId).orElseThrow(EntityNotFoundException::new);
	}

	public Knoten getKnoten(Long knotenId) {
		return knotenRepository.findById(knotenId).orElseThrow(EntityNotFoundException::new);
	}

	public Set<Kante> getKantenInOrganisationsbereich(Verwaltungseinheit organisation) {
		return kantenRepository.getKantenInBereich(organisation.getBereich()
			.orElse(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createMultiPolygon()));
	}

	public List<Kante> getRadVisNetzKantenInBereich(Envelope envelope) {
		return kantenRepository.getKantenInBereichNachQuellen(envelope, Set.of(QuellSystem.DLM, QuellSystem.RadVis))
			.collect(Collectors.toList());
	}

	public List<Knoten> getRadVisNetzKnotenInBereich(Envelope envelope) {
		return knotenRepository.getKnotenInBereichNachQuellen(envelope, Set.of(QuellSystem.DLM, QuellSystem.RadVis));
	}

	public Set<Kante> getKantenInOrganisationsbereichEagerFetchNetzklassen(Verwaltungseinheit organisation) {
		return kantenRepository.getKantenInOrganisationsbereichEagerFetchNetzklassen(organisation);
	}

	public Stream<Kante> getKantenInOrganisationsbereichEagerFetchKnoten(Verwaltungseinheit organisation) {
		return kantenRepository.getKantenInOrganisationsbereichEagerFetchKnoten(organisation);
	}

	@Override
	public List<Knoten> getKnoten(Set<Long> knoten) {
		return StreamSupport.stream(knotenRepository.findAllById(knoten).spliterator(), false)
			.collect(Collectors.toList());
	}

	public int deleteVerwaisteDLMKnoten(NetzAenderungAusloeser ausloeser) {
		knotenRepository.findVerwaisteDLMKnoten()
			.forEach(knoten -> {
				RadVisDomainEventPublisher.publish(new KnotenDeletedEvent(
					knoten.getId(), knoten.getPoint(), ausloeser, LocalDateTime.now()));
			});
		return knotenRepository.deleteVerwaisteDLMKnoten();
	}

	public void deleteAll(Iterable<Kante> toDeleteInPersistenceContext) {
		kantenRepository.deleteAll(toDeleteInPersistenceContext);
	}

	public void insertOsmWayIds(List<KanteOsmWayIdsInsert> inserts) {
		kantenRepository.insertOsmWayIds(inserts);
	}

	public void truncateOsmWayIds() {
		kantenRepository.truncateOsmWayIds();
	}

	public List<KanteGeometryView> getFuerOsmAbbildungRelevanteKanten(Envelope partition) {
		return kantenRepository.getFuerOsmAbbildungRelevanteKanten(partition);
	}

	public Optional<String> getNetzklassenVonKante(Long kanteId) {
		return kantenRepository.getNetzklassenVonKante(kanteId);
	}

	public void aktualisiereFuehrungsformen(List<FuehrungsformAttributGruppe> attributGruppen) {
		attributGruppen.forEach(attributGruppe -> aktualisiereFuehrungsformen(
			attributGruppe.getImmutableFuehrungsformAttributeLinks(),
			attributGruppe.getImmutableFuehrungsformAttributeRechts(),
			attributGruppe.getId(),
			attributGruppe.getVersion()));
	}

	private void aktualisiereFuehrungsformen(List<FuehrungsformAttribute> fuehrungsFormAttributeLinks,
		List<FuehrungsformAttribute> fuehrungsFormAttributeRechts, Long gruppenID, Long gruppenVersion) {

		FuehrungsformAttributGruppe gruppe = loadFuehrungsformAttributGruppeForModification(gruppenID, gruppenVersion);

		if (!FuehrungsformAttributGruppe.isSeitenBezugValid(fuehrungsFormAttributeLinks,
			fuehrungsFormAttributeRechts, gruppe.isZweiseitig())) {
			throw new RuntimeException("Unterschiedliche Attribute bei einseitiger Kante");
		}

		gruppe.replaceFuehrungsformAttribute(
			fuehrungsFormAttributeLinks,
			fuehrungsFormAttributeRechts);
	}

	public void aktualisiereKantenAttribute(List<KantenAttributGruppe> kantenAttributGruppen) {
		Map<KantenAttributGruppe, KantenAttributGruppe> alteGruppeAufNeueGruppe = kantenAttributGruppen.stream()
			.collect(
				Collectors.toMap(neueGruppe -> loadKantenAttributGruppeForModification(neueGruppe.getId(),
					neueGruppe.getVersion()), Function.identity()));

		List<Long> letzteRadNETZKlasseEntfernt = new ArrayList<>();

		// Aufsammeln derjeniger Gruppen, die vorher RadNETZ waren und nach dem Update nicht mehr.
		alteGruppeAufNeueGruppe.forEach((alteGruppe, neueGruppe) -> {
			if (alteGruppe.isRadNETZ() && !neueGruppe.isRadNETZ()) {
				letzteRadNETZKlasseEntfernt.add(alteGruppe.getId());
			}
		});

		// Update der Gruppen
		alteGruppeAufNeueGruppe.forEach(
			(alteGruppe, neueGruppe) -> alteGruppe.update(neueGruppe.getNetzklassen(), neueGruppe.getIstStandards(),
				neueGruppe.getKantenAttribute()));

		if (!letzteRadNETZKlasseEntfernt.isEmpty()) {
			RadVisDomainEventPublisher.publish(new RadNetzZugehoerigkeitEntferntEvent(letzteRadNETZKlasseEntfernt));
		}
	}

	public void aktualisiereKantenZweiseitig(Map<VersionedId, Boolean> isZweiseitigMap) {
		isZweiseitigMap.keySet().forEach(key -> {
			Kante kante = loadKanteForModification(key.getId(), key.getVersion());
			kante.changeSeitenbezug(isZweiseitigMap.get(key));
		});
	}

	@SuppressWarnings("serial")
	public void aktualisiereVerlaeufeUndGeometrien(List<KanteGeometrien> kantenGeometrien) {
		kantenGeometrien.forEach(kanteGeometrien -> {
			Kante kante = loadKanteForModification(kanteGeometrien.getId(), kanteGeometrien.getVersion());

			if (!Kante.isVerlaufValid(kanteGeometrien.getVerlaufLinks(), kanteGeometrien.getVerlaufRechts(),
				kante.isZweiseitig())) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"Unterschiedliche Verläufe bei einseitiger Kante sind nicht erlaubt.");
			}
			kante.updateVerlauf(kanteGeometrien.getVerlaufLinks(), kanteGeometrien.getVerlaufRechts());

			if (kante.isManuelleGeometrieAenderungErlaubt()) {
				if (!kante.isTopologieValid(kanteGeometrien.getGeometry())) {
					throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
						"Start- und Endpunkt einer Kante müssen auf dem Knoten liegen, sonst ist die Routing-Fähigkeit kompromittiert."
							+ " Bitte setzen Sie die Werte zurück und korrigieren Sie Ihre Eingabe.") {
					};
				}
				kante.aendereGeometrieManuell(kanteGeometrien.getGeometry());
			}

		});
	}

	public void aktualisiereGeschwindigkeitAttribute(Map<VersionedId, List<GeschwindigkeitAttribute>> attributeMap) {
		attributeMap.keySet().forEach(versionedId -> {
			GeschwindigkeitAttributGruppe attributGruppe = loadGeschwindigkeitAttributGruppeForModification(
				versionedId.getId(), versionedId.getVersion());
			attributGruppe.replaceGeschwindigkeitAttribute(attributeMap.get(versionedId));
		});
	}

	public void aktualisiereZustaendigkeitsAttribute(
		Map<VersionedId, List<ZustaendigkeitAttribute>> zustaendigkeitMap) {
		zustaendigkeitMap.keySet().forEach(versionedId -> {
			ZustaendigkeitAttributGruppe attributGruppe = loadZustaendigkeitAttributGruppeForModification(
				versionedId.getId(), versionedId.getVersion());
			attributGruppe.replaceZustaendigkeitAttribute(zustaendigkeitMap.get(versionedId));
		});
	}

	public void aktualisiereFahrtrichtung(Map<VersionedId, FahrtrichtungAttributGruppe> fahrtrichtungMap) {
		fahrtrichtungMap.keySet().forEach(versionedId -> {
			FahrtrichtungAttributGruppe attributGruppe = loadFahrtrichtungAttributGruppeForModification(
				versionedId.getId(),
				versionedId.getVersion());

			Richtung fahrtrichtungLinks = fahrtrichtungMap.get(versionedId).getFahrtrichtungLinks();
			Richtung fahrtrichtungRechts = fahrtrichtungMap.get(versionedId).getFahrtrichtungRechts();

			if (!FahrtrichtungAttributGruppe.isValid(fahrtrichtungLinks, fahrtrichtungRechts,
				attributGruppe.isZweiseitig())) {
				throw new RuntimeException("Unterschiedliche Fahrtrichtungen bei einseitiger Kante");
			}

			attributGruppe.setRichtung(fahrtrichtungLinks, fahrtrichtungRechts);
		});
	}

	public void refreshNetzMaterializedViews() {
		kantenRepository.refreshNetzMaterializedViews();
	}

	public void aktualisiereKnoten(long knotenId, long knotenVersion, Long gemeinde, Kommentar kommentar,
		Zustandsbeschreibung zustandsbeschreibung, KnotenForm knotenForm) {

		Knoten knoten = loadKnotenForModification(knotenId, knotenVersion);
		if (knoten.getQuelle().equals(QuellSystem.RadNETZ)) {
			throw new AccessDeniedException("RadNETZ-Knoten dürfen nicht bearbeitet werden.");
		}

		aktualisiereKnoten(knoten, gemeinde, kommentar, zustandsbeschreibung, knotenForm);
	}

	private void aktualisiereKnoten(Knoten knoten, Long gemeinde, Kommentar kommentar,
		Zustandsbeschreibung zustandsbeschreibung,
		KnotenForm knotenForm) {
		Verwaltungseinheit gemeindeLoaded = null;
		if (gemeinde != null) {
			gemeindeLoaded = verwaltungseinheitResolver.resolve(gemeinde);
		}

		KnotenAttribute neueKnotenattribute = new KnotenAttribute(kommentar,
			zustandsbeschreibung,
			knotenForm, gemeindeLoaded);

		knoten.setKnotenAttribute(neueKnotenattribute);
	}

	public int countAndLogVernetzungFehlerhaft() {
		List<Kante> kantenWhereLinestringEndsAreNotOnKnoten = kantenRepository
			.findKantenWhereLinestringEndsAreNotOnKnoten(KnotenIndex.SNAPPING_DISTANCE);

		if (kantenWhereLinestringEndsAreNotOnKnoten.isEmpty()) {
			return 0;
		}

		log.warn("Bei folgenden Kanten endet/beginnt der LineString nicht am Knoten: "
			+ String.join(", ", kantenWhereLinestringEndsAreNotOnKnoten.stream().map(k -> k.getId().toString())
				.collect(Collectors.toList())));
		return kantenWhereLinestringEndsAreNotOnKnoten.size();
	}

	public void loescheGesamtesNetz() {
		kantenRepository.deleteAll();
		knotenRepository.deleteAll();
	}

	public long getAnzahlKanten() {
		return kantenRepository.count();
	}

	public boolean wurdeAngelegtVon(Kante kante, Benutzer benutzer) {
		require(kante, notNullValue());
		require(benutzer, notNullValue());
		Optional<Revision<Long, Kante>> firstRevision = kantenRepository.findRevisions(kante.getId(),
			PageRequest.of(0, 1, RevisionSort.asc())).stream().findFirst();
		if (firstRevision.isEmpty()) {
			log.warn("No revision for Kante {}", kante);
			return false;
		}
		RevInfo revInfo = firstRevision.get().getMetadata().getDelegate();
		return benutzer.equals(revInfo.getBenutzer());
	}
}
