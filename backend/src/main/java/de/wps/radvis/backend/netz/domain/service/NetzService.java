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
import static org.valid4j.Assertive.ensure;
import static org.valid4j.Assertive.require;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
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
import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.common.domain.valueObject.Seitenbezug;
import de.wps.radvis.backend.netz.domain.KanteResolver;
import de.wps.radvis.backend.netz.domain.KnotenResolver;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KanteDeleteStatistik;
import de.wps.radvis.backend.netz.domain.entity.KanteGeometrien;
import de.wps.radvis.backend.netz.domain.entity.KanteGeometryView;
import de.wps.radvis.backend.netz.domain.entity.KanteOsmWayIdsInsert;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenAttribute;
import de.wps.radvis.backend.netz.domain.entity.KnotenDeleteStatistik;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.entity.LineStrings;
import de.wps.radvis.backend.netz.domain.entity.NahegelegeneneKantenDbView;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.event.KantenDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.KnotenDeletedEvent;
import de.wps.radvis.backend.netz.domain.event.RadNetzZugehoerigkeitEntferntEvent;
import de.wps.radvis.backend.netz.domain.repository.FahrtrichtungAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.FuehrungsformAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.GeschwindigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.repository.KantenRepository;
import de.wps.radvis.backend.netz.domain.repository.KnotenRepository;
import de.wps.radvis.backend.netz.domain.repository.ZustaendigkeitAttributGruppeRepository;
import de.wps.radvis.backend.netz.domain.valueObject.Bauwerksmangel;
import de.wps.radvis.backend.netz.domain.valueObject.BauwerksmangelArt;
import de.wps.radvis.backend.netz.domain.valueObject.KantenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.KantenSeite;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenForm;
import de.wps.radvis.backend.netz.domain.valueObject.KnotenOrtslage;
import de.wps.radvis.backend.netz.domain.valueObject.Kommentar;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import de.wps.radvis.backend.netz.domain.valueObject.NetzklasseFilter;
import de.wps.radvis.backend.netz.domain.valueObject.QuerungshilfeDetails;
import de.wps.radvis.backend.netz.domain.valueObject.Radverkehrsfuehrung;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.Status;
import de.wps.radvis.backend.netz.domain.valueObject.Zustandsbeschreibung;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitResolver;
import de.wps.radvis.backend.organisation.domain.entity.Verwaltungseinheit;
import jakarta.persistence.EntityManager;
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

	private final EntityManager entityManager;

	private final double erlaubteAbweichungKnotenRematch;
	private final Laenge nahegelegeneKantenDistanzInM;
	private final int kantenParallelitaetSegmente;
	private final double kantenParallelitaetToleranz;
	private final double nahegelegeneKantenMinAbgebildeteRelativeGesamtlaenge;

	public NetzService(KantenRepository kantenRepository,
		KnotenRepository knotenRepository,
		ZustaendigkeitAttributGruppeRepository zustaendigkeitAttributGruppenRepository,
		FahrtrichtungAttributGruppeRepository fahrtrichtungAttributGruppeRepository,
		GeschwindigkeitAttributGruppeRepository geschwindigkeitAttributGruppeRepository,
		FuehrungsformAttributGruppeRepository fuehrungsformAttributGruppenRepository,
		KantenAttributGruppeRepository kantenAttributGruppenRepository,
		VerwaltungseinheitResolver verwaltungseinheitResolver, EntityManager entityManager,
		double erlaubteAbweichungKnotenRematch, Laenge nahegelegeneKantenDistanzInM,
		int kantenParallelitaetSegmente, double kantenParallelitaetToleranz,
		double nahegelegeneKantenMinAbgebildeteRelativeGesamtlaenge) {

		this.kantenRepository = kantenRepository;
		this.knotenRepository = knotenRepository;
		this.zustaendigkeitAttributGruppenRepository = zustaendigkeitAttributGruppenRepository;
		this.fahrtrichtungAttributGruppeRepository = fahrtrichtungAttributGruppeRepository;
		this.geschwindigkeitAttributGruppeRepository = geschwindigkeitAttributGruppeRepository;
		this.fuehrungsformAttributGruppenRepository = fuehrungsformAttributGruppenRepository;
		this.kantenAttributGruppeRepository = kantenAttributGruppenRepository;
		this.verwaltungseinheitResolver = verwaltungseinheitResolver;
		this.entityManager = entityManager;
		this.erlaubteAbweichungKnotenRematch = erlaubteAbweichungKnotenRematch;
		this.nahegelegeneKantenDistanzInM = nahegelegeneKantenDistanzInM;
		this.kantenParallelitaetSegmente = kantenParallelitaetSegmente;
		this.kantenParallelitaetToleranz = kantenParallelitaetToleranz;
		this.nahegelegeneKantenMinAbgebildeteRelativeGesamtlaenge = nahegelegeneKantenMinAbgebildeteRelativeGesamtlaenge;

		curvebuilderLinks = new OffsetCurveBuilder(10, 1);
		curvebuilderRechts = new OffsetCurveBuilder(-10, 1);
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

	public void deleteKante(Kante kante, KanteDeleteStatistik statistik) {
		require(kante.getQuelle().equals(QuellSystem.RadVis));
		RadVisDomainEventPublisher.publish(
			new KantenDeletedEvent(List.of(kante.getId()), List.of(kante.getGeometry()),
				NetzAenderungAusloeser.RADVIS_KANTE_LOESCHEN, LocalDateTime.now(), statistik));
		kantenRepository.delete(kante);
	}

	public Knoten saveKnoten(Knoten knoten) {
		return knotenRepository.save(knoten);
	}

	public Stream<Kante> findKanteByQuelle(QuellSystem quelle) {
		return kantenRepository.findKanteByQuelle(quelle);
	}

	public Stream<Kante> findKanteByStatusNotAndQuelleIn(Status statusToIgnore, List<QuellSystem> quellen) {
		return kantenRepository.findKanteByStatusNotAndQuelleIn(statusToIgnore, quellen);
	}

	public Set<Kante> getKantenInBereichNachQuelleUndIsAbgebildet(Envelope bereich, QuellSystem quelle) {
		return kantenRepository.getKantenInBereichNachQuelleUndIsAbgebildet(bereich, quelle);
	}

	public Stream<Kante> getKantenInBereichNachQuelle(Envelope bereich, QuellSystem quelle) {
		return kantenRepository.getKantenInBereichNachQuellen(bereich, Set.of(quelle));
	}

	public List<Knoten> getKnotenInBereichNachQuelle(Envelope bereich, QuellSystem quellSystem) {
		return knotenRepository.getKnotenInBereichFuerQuelle(bereich, quellSystem);
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

	@SuppressWarnings("deprecation")
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

	@SuppressWarnings("deprecation")
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

	/**
	 * Gibt true zurück, wenn die Kante wahrscheinlich eine Straße bildet. Wir wissen das nicht genau, schauen daher
	 * auf Dinge wie Radverkehrsführung und Straßennummer.
	 */
	private boolean isWahrscheinlichStrasse(Kante kante) {
		// Wege mit Radverkehrsführung sind zumindest nicht irgendwelche Waldwege, die üblicherweise null oder Unbekannt
		// als Radverkehrsführung haben, sondern Landstraße oder andere wichtigere Straßen.
		FuehrungsformAttributGruppe attributGruppe = kante.getFuehrungsformAttributGruppe();

		ArrayList<FuehrungsformAttribute> attribute = new ArrayList<>();
		attribute.addAll(attributGruppe.getImmutableFuehrungsformAttributeLinks());
		attribute.addAll(attributGruppe.getImmutableFuehrungsformAttributeRechts());

		boolean hatRadverkehrsfuehrung = attribute.stream().anyMatch(attribut -> {
			return attribut.getRadverkehrsfuehrung() != null
				&& attribut.getRadverkehrsfuehrung() != Radverkehrsfuehrung.UNBEKANNT;
		});

		// Wege mit einer Straßennummer sind sehr wahrscheinlich tatsächliche Straßen.
		boolean hatStrassennummer = kante.getKantenAttributGruppe().getKantenAttribute().getStrassenNummer()
			.isPresent();

		return hatRadverkehrsfuehrung || hatStrassennummer;
	}

	/**
	 * Ermittelt geometrisch nahegelegene Kanten, die Straßen darstellen und parallel zur angegebenen Basiskante
	 * verlaufen. Pro Kanten-View sind die Geometrie der Basiskante und die Geometrie der dazugehörigen parallelen Kante
	 * aufeinander zugeschnitten, es werden also nur die Teile zurückgegeben, die aufeinander matchen und tatsächlich
	 * parallel zueinander verlaufen.
	 *
	 * Nahegelegene Kanten, die gemäß der Parameter zu klein sind, keine passende Radverkehrsführung oder nicht
	 * parallel genug sind, werden nicht betrachtet. Wird insgesamt zu wenig der Basiskante überdeckt, ist das Ergebnis
	 * leer.
	 *
	 * @param kante Basiskante in deren Nähe gesucht werden soll.
	 * @param seite Seite (RECHTS oder LINKS) auf der gesucht werden soll. Der Wert BEIDSEITIG wird nicht unterstützt.
	 * @param nahegelegeneKantenDistanzInM Entfernung von der Kante in Metern in der gesucht werden soll.
	 * @param kantenParallelitaetSegmente Genauigkeit (Anzahl an Unterteilungen der Kanten) für die Prüfung
	 *     auf Parallelität. Höhere Werte führen zu höherer Genauigkeit aber ggf. längerer Laufzeit.
	 * @param kantenParallelitaetToleranz Wer in Grad für die Toleranz der Genauigkeit. Kleinere Werte
	 *     liefern striktere Ergebnisse. Zu kleine Werte führen ggf. zu false-negatives (also als "nicht parallel"
	 *     erkannte Kanten, die aber sehrwohl parallel verlaufen).
	 */
	private boolean hatNahegelegeneParalleleStrassen(Kante kante,
		LinearReferenzierterAbschnitt abschnitt, Seitenbezug seite, Laenge nahegelegeneKantenDistanzInM,
		int kantenParallelitaetSegmente, double kantenParallelitaetToleranz) {
		require(seite == Seitenbezug.LINKS || seite == Seitenbezug.RECHTS);

		LineString basiskanteAbschnittGeometry = abschnitt.toSegment(kante.getGeometry());
		List<LinearReferenzierterAbschnitt> strassenAbschnitte = kantenRepository.getNahegelegeneKantenAufSeite(kante,
			abschnitt, seite, nahegelegeneKantenDistanzInM)
			.stream()
			.filter(nahegelegeneneKantenDbView -> {
				return isWahrscheinlichStrasse(nahegelegeneneKantenDbView.getNahegelegeneKante());
			})
			.filter(nahegelegeneneKantenDbView -> {
				boolean sindParallel = LineStrings.sindParallel(
					nahegelegeneneKantenDbView.getBasisKanteSegment(),
					nahegelegeneneKantenDbView.getNahegelegeneKanteSegment(),
					kantenParallelitaetSegmente,
					kantenParallelitaetToleranz
				);
				return sindParallel;
			})
			.map(k -> LinearReferenzierterAbschnitt.of(basiskanteAbschnittGeometry, k.getBasisKanteSegment()))
			.toList();

		if (strassenAbschnitte.isEmpty() || LinearReferenzierterAbschnitt.getSummierteRelativeLaenge(strassenAbschnitte)
			< nahegelegeneKantenMinAbgebildeteRelativeGesamtlaenge) {
			return false;
		}

		return true;
	}

	/**
	 * Ermittelt wie viel der übergebenen Kante durch die kantenViews abgedeckt wird. Hierbei wird angenommen, dass
	 * jedes Basiskante-Segment der views einen Teil der übergebenen Kante entspricht. Gibt es in den kantenViews
	 * mehrere Kanten, die den gleichen linear referenzierten Abschnitt überdecken, werden diese NICHT doppelt gezählt.
	 * Beispiel: Bei drei Kanten-Views, die alle drei den Abschnitt [0.3, 0.5] der übergebenen Kante abdecken, ist das
	 * Ergebnis 0.2, da nur 20% der Kante abgedeckt sind.
	 *
	 * @return Ein Wert >=0 und <=1, der angibt, um wie viel die angegebene Kante durch die kantenViews abgedeckt wird.
	 *     Ein Wert von 1.0 sagt aus, dass 100% der kante von den views abgedeckt wird. Ein Wert von 0.5 entsprechend 50% usw.
	 */
	private static double getAnteilAbgedeckterBasiskante(LineString basiskanteGeometrie,
		List<NahegelegeneneKantenDbView> kantenViews) {
		List<LinearReferenzierterAbschnitt> abschnitte = new ArrayList<>(
			kantenViews.stream().map(k -> LinearReferenzierterAbschnitt.of(basiskanteGeometrie, k
				.getBasisKanteSegment())).toList());

		for (int i = 0; i < abschnitte.size() - 1; i++) {
			LinearReferenzierterAbschnitt abschnittA = abschnitte.get(i);

			for (int j = i + 1; j < abschnitte.size(); j++) {
				LinearReferenzierterAbschnitt abschnittB = abschnitte.get(j);

				if (abschnittA.intersects(abschnittB)) {
					LinearReferenzierterAbschnitt union = abschnittA.union(abschnittB).get();
					abschnitte.remove(abschnittA);
					abschnitte.remove(abschnittB);
					abschnitte.add(union);

					// Das aktuell i-the Element nochmal betrachten, da dieses mit einem anderen gemerged wird. Dafür
					// das i-- an dieser Stelle, um das i++ der äußeren Schleiße zu kompensieren.
					i--;
					break;
				}
			}
		}

		double addierteRelativeLaenge = abschnitte.stream().mapToDouble(a -> a.relativeLaenge()).sum();

		ensure(addierteRelativeLaenge >= 0, "Relative Länge muss >=0 und <=1 sein, aber war " + addierteRelativeLaenge);
		ensure(addierteRelativeLaenge <= 1, "Relative Länge muss >=0 und <=1 sein, aber war " + addierteRelativeLaenge);
		return addierteRelativeLaenge;
	}

	/**
	 * Ermittelt die Seite (sofern vorhanden) auf der sich andere Kanten befinden, die (sehr wahrscheinlich) parallele
	 * Straßen darstellen. Die Seite bezieht sich auf die Stationierungsrichtung der Kante. Es wird einerseits nur der
	 * angegebene Abschnitt untersucht, also nicht die gesamte Kante, und es gibt Metriken nach denen entschieden wird
	 * welche umliegenden Kanten betrachtet werden und wann das Resultat stark genug ist, sodass hier ein konkreter
	 * Seitenbezug zurückgegeben wird. Wird also z.B. RECHTS zurückgegeben, kann es trotzdem auf der linken Seite Kanten
	 * geben, die aber als nicht betrachtungswürdig eingestuft wurden (z.B. weil zu kurz).
	 *
	 * @param kante Die Kante zu der geschaut werden soll auf welcher Seite sich parallele Straßen befinden.
	 * @param abschnitt Der Abschnitt der Kante zu dem geschaut werden soll.
	 * @return RECHTS oder LINKS, wenn auf der jeweiligen Seite parallel Straßen existieren, BEIDSEITIG wenn auf beiden
	 *     Seiten parallele Straßen existieren oder ein leerer Optional, wenn keine parallelen Straßen existieren.
	 */
	public Optional<Seitenbezug> getSeiteMitParallelenStrassenKanten(Kante kante,
		LinearReferenzierterAbschnitt abschnitt) {
		boolean hatStrassenRechts = hatNahegelegeneParalleleStrassen(kante,
			abschnitt, Seitenbezug.RECHTS, nahegelegeneKantenDistanzInM, kantenParallelitaetSegmente,
			kantenParallelitaetToleranz);
		boolean hatStrassenLinks = hatNahegelegeneParalleleStrassen(kante,
			abschnitt, Seitenbezug.LINKS, nahegelegeneKantenDistanzInM, kantenParallelitaetSegmente,
			kantenParallelitaetToleranz);

		if (hatStrassenRechts && hatStrassenLinks) {
			return Optional.of(Seitenbezug.BEIDSEITIG);
		} else if (hatStrassenLinks) {
			return Optional.of(Seitenbezug.LINKS);
		} else if (hatStrassenRechts) {
			return Optional.of(Seitenbezug.RECHTS);
		}

		return Optional.empty();
	}

	@Override
	public List<Knoten> getKnoten(Set<Long> knoten) {
		return StreamSupport.stream(knotenRepository.findAllById(knoten).spliterator(), false)
			.collect(Collectors.toList());
	}

	public int deleteVerwaisteDLMKnoten(NetzAenderungAusloeser ausloeser, KnotenDeleteStatistik statistik) {
		log.info("Entfernt verwaiste DLM-Knoten (Auslöser: {})", ausloeser);

		List<Knoten> verwaisteKnoten = knotenRepository.findVerwaisteDLMKnoten();
		if (verwaisteKnoten.isEmpty()) {
			return 0;
		}

		RadVisDomainEventPublisher.publish(
			new KnotenDeletedEvent(verwaisteKnoten, ausloeser, LocalDateTime.now(), statistik));
		entityManager.flush();

		knotenRepository.deleteAllById(verwaisteKnoten.stream().map(k -> k.getId()).toList());

		log.info("{} Verwaiste DLM-Knoten entfernt", verwaisteKnoten.size());
		return verwaisteKnoten.size();
	}

	public void deleteAll(Collection<Kante> kantenToDelete, NetzAenderungAusloeser ausloeser,
		KanteDeleteStatistik statistik) {
		log.debug("Lösche {} Kanten", kantenToDelete.size());

		if (kantenToDelete.isEmpty()) {
			return;
		}

		RadVisDomainEventPublisher.publish(
			new KantenDeletedEvent(
				kantenToDelete.stream().map(Kante::getId).toList(),
				kantenToDelete.stream().map(Kante::getGeometry).toList(),
				ausloeser,
				LocalDateTime.now(), statistik));

		entityManager.flush();

		kantenRepository.deleteAll(kantenToDelete);
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
				if (!kante.isGeometryUpdateValid(kanteGeometrien.getGeometry())) {
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

			attributGruppe.update(fahrtrichtungLinks, fahrtrichtungRechts);
		});
	}

	public void refreshNetzMaterializedViews() {
		log.info("Refreshing RadVisNetz-Materialized-Views");
		kantenRepository.refreshNetzMaterializedViews();
		log.info("Finished refreshing RadVisNetz-Materialized-Views");
	}

	public void aktualisiereKnoten(long knotenId, long knotenVersion, Long gemeinde, Kommentar kommentar,
		Zustandsbeschreibung zustandsbeschreibung, KnotenForm knotenForm, QuerungshilfeDetails querungshilfeDetails,
		Bauwerksmangel bauwerksmangel, Set<BauwerksmangelArt> bauwerksmangelArt) {

		Knoten knoten = loadKnotenForModification(knotenId, knotenVersion);
		if (knoten.getQuelle().equals(QuellSystem.RadNETZ)) {
			throw new AccessDeniedException("RadNETZ-Knoten dürfen nicht bearbeitet werden.");
		}

		aktualisiereKnoten(knoten, gemeinde, kommentar, zustandsbeschreibung, knotenForm, querungshilfeDetails,
			bauwerksmangel, bauwerksmangelArt);
	}

	private void aktualisiereKnoten(Knoten knoten, Long gemeinde, Kommentar kommentar,
		Zustandsbeschreibung zustandsbeschreibung, KnotenForm knotenForm, QuerungshilfeDetails querungshilfeDetails,
		Bauwerksmangel bauwerksmangel, Set<BauwerksmangelArt> bauwerksmangelArt) {
		Verwaltungseinheit gemeindeLoaded = null;
		if (gemeinde != null) {
			gemeindeLoaded = verwaltungseinheitResolver.resolve(gemeinde);
		}

		KnotenAttribute neueKnotenattribute = new KnotenAttribute(kommentar,
			zustandsbeschreibung, knotenForm, gemeindeLoaded, querungshilfeDetails, bauwerksmangel, bauwerksmangelArt);

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

	public List<Knoten> findKnotenInBereich(Envelope bereich) {
		return knotenRepository.getKnotenInBereichNachQuellen(bereich,
			Set.of(QuellSystem.DLM, QuellSystem.RadVis));
	}

	public Optional<Knoten> findErsatzKnoten(Long fuerKnotenId, List<Long> excludeIds) {
		return knotenRepository.findErsatzKnotenCandidates(fuerKnotenId, erlaubteAbweichungKnotenRematch).stream()
			.filter(k -> !excludeIds.contains(k.getId()))
			.findFirst();
	}
}
