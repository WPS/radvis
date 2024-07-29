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

package de.wps.radvis.backend.netz.domain.entity;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.valid4j.Assertive.ensure;
import static org.valid4j.Assertive.require;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.linearref.LengthIndexedLine;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.common.domain.valueObject.KoordinatenReferenzSystem;
import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.netz.domain.event.KanteGeometrieChangedEvent;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.KantenSeite;
import de.wps.radvis.backend.netz.domain.valueObject.Laenge;
import de.wps.radvis.backend.netz.domain.valueObject.LinearReferenzierteOsmWayId;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.Richtung;
import de.wps.radvis.backend.netz.domain.valueObject.SeitenbezogeneProfilEigenschaften;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Audited
@Entity
@ToString(callSuper = true)
@Slf4j
public class Kante extends VersionierteEntity {

	private static final double MINIMALE_LAENGE_LINEAR_REFERENZIERTER_SEGMENTE = 1.;

	@Getter
	@JsonIgnore
	private DlmId dlmId;

	@Getter
	@JsonIgnore
	private String ursprungsfeatureTechnischeID;

	@NotAudited
	@Getter
	@Setter
	@JsonIgnore
	@ElementCollection
	private Set<LinearReferenzierteOsmWayId> osmWayIds;

	// wenn eine Kante gelöscht wird, soll der Knoten nicht gelöscht werden
	@Getter
	@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	@JsonIgnore
	private Knoten vonKnoten;

	// wenn eine Kante gelöscht wird, soll der Knoten nicht gelöscht werden
	@Getter
	@ManyToOne(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	@JsonIgnore
	private Knoten nachKnoten;

	@Getter
	@Enumerated(EnumType.STRING)
	private QuellSystem quelle;
	@Getter
	private LineString geometry;

	@Getter
	private int kantenLaengeInCm;

	private LineString verlaufLinks;

	private LineString verlaufRechts;

	@Getter
	private boolean isZweiseitig;

	@Getter
	@Setter
	private boolean isGrundnetz;

	@Setter
	private LineString aufDlmAbgebildeteGeometry;

	@Getter
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "kanten_attributgruppe_id")
	@JsonIgnore
	private KantenAttributGruppe kantenAttributGruppe;

	@Getter
	@JsonIgnore
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "geschwindigkeit_attributgruppe_id")
	private GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe;

	@Getter
	@JsonIgnore
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "fahrtrichtung_attributgruppe_id")
	@NonNull
	private FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe;

	@Getter
	@JsonIgnore
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "zustaendigkeit_attributgruppe_id")
	private ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe;

	@Getter
	@JsonIgnore
	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "fuehrungsform_attribut_gruppe_id")
	@NonNull
	private FuehrungsformAttributGruppe fuehrungsformAttributGruppe;

	public Kante(String ursprungsfeatureTechnischeID, Knoten vonKnoten, Knoten nachKnoten, LineString geometry,
		boolean isZweiseitig,
		QuellSystem quelle,
		KantenAttributGruppe kantenAttributGruppe, FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe,
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe,
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe,
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe) {
		this(null, ursprungsfeatureTechnischeID, vonKnoten, nachKnoten, geometry, isZweiseitig, quelle,
			kantenAttributGruppe,
			fahrtrichtungAttributGruppe,
			zustaendigkeitAttributGruppe,
			geschwindigkeitAttributGruppe,
			fuehrungsformAttributGruppe);
		// Wenn wir das in den allgemeinen Konstruktor ziehen, müssen wir 200+ Tests fixen
		require(isTopologieValid(geometry));
	}

	public Kante(DlmId dlmId, String ursprungsfeatureTechnischeID, Knoten vonKnoten, Knoten nachKnoten,
		LineString geometry, boolean isZweiseitig, QuellSystem quelle, KantenAttributGruppe kantenAttributGruppe,
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe,
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe,
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe,
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe) {
		this(null, ursprungsfeatureTechnischeID, vonKnoten, nachKnoten, quelle, geometry, null, isZweiseitig,
			kantenAttributGruppe, fahrtrichtungAttributGruppe, zustaendigkeitAttributGruppe,
			geschwindigkeitAttributGruppe, fuehrungsformAttributGruppe, null, null, null, dlmId, false);
		// Wenn wir das in den allgemeinen Konstruktor ziehen, müssen wir 200+ Tests fixen
		require(isTopologieValid(geometry));
	}

	public static boolean isVerlaufValid(LineString verlaufLinks, LineString verlaufRechts, boolean isZweiseitig) {
		if (!isZweiseitig) {
			return Objects.equals(verlaufLinks, verlaufRechts);
		}

		return true;
	}

	public static boolean isZweiseitigkeitKonsistent(boolean isZweiseitig,
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe,
		FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe) {
		return isZweiseitig == fuehrungsformAttributGruppe.isZweiseitig()
			&& isZweiseitig == fahrtrichtungAttributGruppe.isZweiseitig();
	}

	public static List<Kante> distinktiereUndSortiereNachMinYDerGeometrie(Set<Kante> inputList) {
		Map<LineString, Kante> distinkteKanten = new HashMap<>();
		for (Kante kante : inputList) {
			distinkteKanten.putIfAbsent(kante.getGeometry(), kante);
		}

		return distinkteKanten.values()
			.stream()
			.sorted(Comparator.comparing(kante -> kante.getZugehoerigeDlmGeometrie().getEnvelopeInternal().getMinY()))
			.collect(Collectors.toList());
	}

	private boolean isKoordinatenSystemValid(Geometry geometry) {
		require(geometry, notNullValue());
		return geometry.getSRID() == KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid();
	}

	@Builder
	private Kante(Long id, String ursprungsfeatureTechnischeID, Knoten vonKnoten, Knoten nachKnoten, QuellSystem quelle,
		LineString geometry, LineString aufDlmAbgebildeteGeometry, boolean isZweiseitig,
		KantenAttributGruppe kantenAttributGruppe, FahrtrichtungAttributGruppe fahrtrichtungAttributGruppe,
		ZustaendigkeitAttributGruppe zustaendigkeitAttributGruppe,
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe,
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe, LineString verlaufLinks, LineString verlaufRechts,
		Long version, DlmId dlmId, boolean isGrundnetz) {
		super(id, version);

		require(isKoordinatenSystemValid(geometry),
			"Geometrie muss in UTM32 (SRID: %s) kodiert sein, gesetzte SRID ist %s.",
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(), geometry.getSRID());
		require(ursprungsfeatureTechnischeID != null || (quelle != QuellSystem.DLM && quelle != QuellSystem.RadNETZ),
			"Wenn es sich um eine importierte Kante handelt, dann sollte die featureID gsetzt sein");
		require(vonKnoten, notNullValue());
		require(nachKnoten, notNullValue());
		require(quelle, notNullValue());
		require(!quelle.equals(QuellSystem.DLM) || dlmId != null,
			"Wenn es sich um eine DLM-Kante handelt, muss die dlmId gesetzt sein");
		require(dlmId == null || quelle.equals(QuellSystem.DLM),
			"Wenn die dlmId gesetzt ist, muss es sich um eine DLM-Kante handeln");
		require(kantenAttributGruppe, notNullValue());
		require(fahrtrichtungAttributGruppe, notNullValue());
		require(zustaendigkeitAttributGruppe, notNullValue());
		require(geschwindigkeitAttributGruppe, notNullValue());
		require(fuehrungsformAttributGruppe, notNullValue());
		require(isVerlaufValid(verlaufLinks, verlaufRechts, isZweiseitig));
		require(!vonKnoten.getKoordinate().equals(nachKnoten.getKoordinate()),
			"Koordinaten von VonKnoten (%s) & Nachknoten (%s) dürfen nicht identisch sein", vonKnoten, nachKnoten);
		require(isVerlaufValid(verlaufLinks, verlaufRechts, isZweiseitig));
		require(isZweiseitigkeitKonsistent(isZweiseitig, fuehrungsformAttributGruppe, fahrtrichtungAttributGruppe),
			"Zweiseitigkeit der Kante muss Konsistent mit der FührungsformAttributGruppe und der FahrtrichtungAttributGruppe sein.");

		this.ursprungsfeatureTechnischeID = ursprungsfeatureTechnischeID;
		this.vonKnoten = vonKnoten;
		this.nachKnoten = nachKnoten;
		this.geometry = geometry;
		this.quelle = quelle;
		this.kantenLaengeInCm = geometry != null ? (int) Math.round(geometry.getLength() * 100) : 0;
		this.aufDlmAbgebildeteGeometry = aufDlmAbgebildeteGeometry;
		this.isZweiseitig = isZweiseitig;
		this.kantenAttributGruppe = kantenAttributGruppe;
		this.fahrtrichtungAttributGruppe = fahrtrichtungAttributGruppe;
		this.zustaendigkeitAttributGruppe = zustaendigkeitAttributGruppe;
		this.geschwindigkeitAttributGruppe = geschwindigkeitAttributGruppe;
		this.fuehrungsformAttributGruppe = fuehrungsformAttributGruppe;
		this.verlaufLinks = verlaufLinks;
		this.verlaufRechts = verlaufRechts;
		this.dlmId = dlmId;
		this.isGrundnetz = isGrundnetz;
	}

	public Kante(Knoten vonKnoten, Knoten bisKnoten) {
		this(null, vonKnoten, bisKnoten,
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory().createLineString(
				new Coordinate[] { vonKnoten.getKoordinate(), bisKnoten.getKoordinate() }),
			false,
			QuellSystem.RadVis,
			KantenAttributGruppe.builder().build(),
			FahrtrichtungAttributGruppe.builder().build(),
			ZustaendigkeitAttributGruppe.builder().build(),
			GeschwindigkeitAttributGruppe.builder().build(),
			FuehrungsformAttributGruppe.builder().build());
	}

	public ZustaendigkeitAttribute getZustaendigkeitAttributeAnPunkt(Coordinate punkt) {
		return getLinearReferenzierteAttributeAnPunkt(
			zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute(),
			punkt);
	}

	public GeschwindigkeitAttribute getGeschwindigkeitAttributeAnPunkt(Coordinate punkt) {
		return getLinearReferenzierteAttributeAnPunkt(
			geschwindigkeitAttributGruppe.getImmutableGeschwindigkeitAttribute(),
			punkt);
	}

	private <T extends LinearReferenzierteAttribute> T getLinearReferenzierteAttributeAnPunkt(List<T> list,
		Coordinate punkt) {

		LengthIndexedLine lengthIndexedLine = new LengthIndexedLine(this.geometry);
		double distanceFromStart = lengthIndexedLine.project(punkt);
		double percentageOfPoint = distanceFromStart / this.geometry.getLength();

		return list.stream().filter(
			attr -> attr.getLinearReferenzierterAbschnitt().getVonValue() <= percentageOfPoint
				&& attr.getLinearReferenzierterAbschnitt().getBisValue() >= percentageOfPoint)
			.min(Comparator.comparing(attribut -> attribut.getLinearReferenzierterAbschnitt().getVonValue()))
			.get();
	}

	public FuehrungsformAttribute getFuehrungsformAttributeAnPunkt(Coordinate punkt) {
		return getLinearReferenzierteAttributeAnPunkt(
			fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks(),
			punkt);
	}

	public FuehrungsformAttribute getFuehrungsformAttributeAnPunkt(Coordinate punkt, KantenSeite kantenSeite) {
		List<FuehrungsformAttribute> immutableFuehrungsformAttribute = kantenSeite == KantenSeite.LINKS
			? fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks()
			: fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts();
		return getLinearReferenzierteAttributeAnPunkt(
			immutableFuehrungsformAttribute,
			punkt);
	}

	public Laenge getLaengeBerechnet() {
		return Laenge.of(geometry.getLength());
	}

	public boolean isAbgebildet() {
		return getZugehoerigeDlmGeometrie() != null;
	}

	public LineString getZugehoerigeDlmGeometrie() {
		if (quelle == QuellSystem.DLM) {
			return geometry;
		}
		return aufDlmAbgebildeteGeometry;
	}

	public boolean isManuelleGeometrieAenderungErlaubt() {
		return this.quelle == QuellSystem.RadVis;
	}

	public void updateVerlauf(LineString links, LineString rechts) {
		require(isVerlaufValid(links, rechts, isZweiseitig));
		aendereVerlaufLinks(links);
		aendereVerlaufRechts(rechts);
	}

	private void aendereVerlaufLinks(LineString newLinks) {
		if (newLinks == null) {
			this.verlaufLinks = null;
			return;
		}

		if (this.verlaufLinks != null && this.verlaufLinks.equals(newLinks)) {
			return;
		}

		require(isKoordinatenSystemValid(geometry),
			"Geometrie muss in UTM32 (SRID: %s) kodiert sein, gesetzte SRID ist %s.",
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(), geometry.getSRID());
		this.verlaufLinks = newLinks;
	}

	private void aendereVerlaufRechts(LineString newRechts) {
		if (newRechts == null) {
			this.verlaufRechts = null;
			return;
		}

		if (this.verlaufRechts != null && this.verlaufRechts.equals(newRechts)) {
			return;
		}

		require(isKoordinatenSystemValid(geometry),
			"Geometrie muss in UTM32 (SRID: %s) kodiert sein, gesetzte SRID ist %s.",
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(), geometry.getSRID());
		this.verlaufRechts = newRechts;
	}

	public Optional<LineString> getVerlaufLinks() {
		return Optional.ofNullable(verlaufLinks);
	}

	public Optional<LineString> getVerlaufRechts() {
		return Optional.ofNullable(verlaufRechts);
	}

	public Optional<Set<Netzklasse>> getHoechsteNetzklassen() {
		Set<Netzklasse> netzklassen = kantenAttributGruppe.getNetzklassen();
		Optional<Netzklasse> hoechsteNetzklasse = netzklassen.stream()
			.max(Comparator.comparing(Netzklasse::getPrioritaet));
		if (hoechsteNetzklasse.isPresent() && hoechsteNetzklasse.get() == Netzklasse.RADNETZ_ALLTAG
			&& netzklassen.contains(Netzklasse.RADNETZ_FREIZEIT)) {
			return Optional.of(Set.of(Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT));
		} else {
			return hoechsteNetzklasse.map(Set::of);
		}
	}

	public boolean hasNetzklasse(Netzklasse netzklasse) {
		return kantenAttributGruppe.getNetzklassen().contains(netzklasse);
	}

	public void changeSeitenbezug(boolean isZweiseitig) {
		this.isZweiseitig = isZweiseitig;
		// Wenn eine Kante nicht zweiseitig ist, sollen die Werte auf der linken und der rechten Seite gleich sein.
		if (!isZweiseitig) {
			this.verlaufRechts = verlaufLinks;
		}

		fuehrungsformAttributGruppe.changeSeitenbezug(isZweiseitig);
		fahrtrichtungAttributGruppe.changeSeitenbezug(isZweiseitig);
	}

	public void ueberschreibeKantenAttribute(KantenAttribute kantenAttribute) {
		this.kantenAttributGruppe.update(kantenAttributGruppe.getNetzklassen(), kantenAttributGruppe.getIstStandards(),
			kantenAttribute);
	}

	public void ueberschreibeNetzklassen(Set<Netzklasse> netzklassen) {
		this.kantenAttributGruppe.updateNetzklassen(netzklassen);
	}

	public void ueberschreibeIstStandards(Set<IstStandard> istStandards) {
		this.kantenAttributGruppe.update(kantenAttributGruppe.getNetzklassen(), istStandards,
			kantenAttributGruppe.getKantenAttribute());
	}

	public void ueberschreibeGeschwindgkeitsAttribute(GeschwindigkeitAttribute geschwindigkeitAttribute) {
		this.getGeschwindigkeitAttributGruppe().replaceGeschwindigkeitAttribute(List.of(geschwindigkeitAttribute));
	}

	public void ueberschreibeFahrtrichtungAttribute(boolean istZweiseitig, Richtung links, Richtung rechts) {
		this.getFahrtrichtungAttributGruppe().changeSeitenbezug(istZweiseitig);
		this.getFahrtrichtungAttributGruppe().setRichtung(links, rechts);
	}

	public void ueberschreibeZustaendigketisAttribute(ZustaendigkeitAttribute zustaendigkeitAttribute) {
		this.getZustaendigkeitAttributGruppe().replaceZustaendigkeitAttribute(List.of(zustaendigkeitAttribute));
	}

	public void ueberschreibeFuehrungsformAttribute(boolean isZweiseitig,
		List<FuehrungsformAttribute> fuehrungsformAttributeLinks,
		List<FuehrungsformAttribute> fuehrungsformAttributeRechts) {

		this.getFuehrungsformAttributGruppe().changeSeitenbezug(isZweiseitig);
		this.getFuehrungsformAttributGruppe().replaceFuehrungsformAttribute(fuehrungsformAttributeLinks,
			fuehrungsformAttributeRechts);
	}

	public void updateTopologie(Knoten neuVon, Knoten neuNach) {
		require(quelle == QuellSystem.DLM || quelle == QuellSystem.RadVis);
		require(!neuVon.equals(neuNach));

		this.vonKnoten = neuVon;
		this.nachKnoten = neuNach;

		if (quelle.equals(QuellSystem.RadVis)) {
			korrigiereStartAndEndpointOfLinestring();
		}

		ensure(isTopologieValid(geometry));
	}

	public void korrigiereStartAndEndpointOfLinestring() {
		require(isManuelleGeometrieAenderungErlaubt());
		boolean startPointCorrect = geometry.getStartPoint()
			.equals(vonKnoten.getPoint());
		boolean endPointCorrect = geometry.getEndPoint()
			.equals(nachKnoten.getPoint());
		if (startPointCorrect && endPointCorrect) {
			return;
		}
		List<Coordinate> existingCoordinates = new ArrayList<>(Arrays.asList(geometry.getCoordinates()));
		if (!startPointCorrect) {
			log.info("Startpunkt der Geometrie wurde korrigiert.");
			existingCoordinates.add(0, vonKnoten.getKoordinate());
		}
		if (!endPointCorrect) {
			log.info("Endpunkt der Geometrie wurde korrigiert.");
			existingCoordinates.add(nachKnoten.getKoordinate());
		}
		aendereGeometrieManuell(KoordinatenReferenzSystem.ETRS89_UTM32_N.getGeometryFactory()
			.createLineString(existingCoordinates.toArray(new Coordinate[existingCoordinates.size()])));

		ensure(isTopologieValid(geometry));
	}

	public void updateDLMGeometryUndTopology(LineString neueGeometry, Knoten neuVon, Knoten neuNach) {
		require(quelle == QuellSystem.DLM);

		if (!neuVon.equals(vonKnoten)) {
			this.vonKnoten = neuVon;
		}
		if (!neuNach.equals(nachKnoten)) {
			this.nachKnoten = neuNach;
		}

		updateDLMGeometry(neueGeometry);

		ensure(isTopologieValid(geometry));
	}

	public void updateDLMGeometry(LineString neueGeometry) {
		require(quelle == QuellSystem.DLM);
		require(isKoordinatenSystemValid(geometry),
			"Geometrie muss in UTM32 (SRID: %s) kodiert sein, gesetzte SRID ist %s.",
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(), geometry.getSRID());
		require(isTopologieValid(neueGeometry));

		this.geometry = neueGeometry;
		this.kantenLaengeInCm = (int) Math.round(geometry.getLength() * 100);
		this.aufDlmAbgebildeteGeometry = neueGeometry;
	}

	public void aendereGeometrieManuell(LineString newGeometry) {
		if (this.geometry.equalsExact(newGeometry)) {
			return;
		}
		require(this.isManuelleGeometrieAenderungErlaubt(), "Änderung der Geometrie dieser Kante nicht erlaubt");
		require(isKoordinatenSystemValid(geometry),
			"Geometrie muss in UTM32 (SRID: %s) kodiert sein, gesetzte SRID ist %s.",
			KoordinatenReferenzSystem.ETRS89_UTM32_N.getSrid(), geometry.getSRID());
		require(isTopologieValid(newGeometry));

		this.aufDlmAbgebildeteGeometry = null;
		this.geometry = newGeometry;
		this.kantenLaengeInCm = (int) Math.round(newGeometry.getLength() * 100);
		RadVisDomainEventPublisher.publish(new KanteGeometrieChangedEvent(this.id));
	}

	public boolean isTopologieValid(LineString neueGeometry) {
		return !vonKnoten.getKoordinate().equals(nachKnoten.getKoordinate()) &&
			neueGeometry.getStartPoint().getCoordinate()
				.distance(vonKnoten.getPoint().getCoordinate()) <= KnotenIndex.SNAPPING_DISTANCE
			&& neueGeometry.getEndPoint().getCoordinate()
				.distance(nachKnoten.getPoint().getCoordinate()) <= KnotenIndex.SNAPPING_DISTANCE;
	}

	/**
	 * Beide Geometrien müssen simple sein
	 */
	public double getUeberschneidunsanteilWith(LineString lineString) {
		Optional<LineString> ueberschneidung = LineStrings.calculateUeberschneidungslinestring(geometry,
			lineString);

		if (ueberschneidung.isEmpty()) {
			return 0;
		}

		return ueberschneidung.get().getLength() / this.geometry.getLength();
	}

	@Deprecated
	public void resetKantenAttributeAusserDLM() {

		Optional<StrassenName> strassenname = this.kantenAttributGruppe.getKantenAttribute().getStrassenName();
		Optional<StrassenNummer> strassennummer = this.kantenAttributGruppe.getKantenAttribute().getStrassenNummer();
		this.kantenAttributGruppe.reset();
		strassenname.ifPresent(name -> kantenAttributGruppe.getKantenAttribute().setStrassenName(name));
		strassennummer.ifPresent(nummer -> kantenAttributGruppe.getKantenAttribute().setStrassenNummer(nummer));

		this.geschwindigkeitAttributGruppe.reset();
		this.fahrtrichtungAttributGruppe.reset();
		this.zustaendigkeitAttributGruppe.reset();
		this.fuehrungsformAttributGruppe.reset();
	}

	public void defragmentiereLinearReferenzierteAttribute() {

		// Defragmentiere Zustaendigkeit
		List<ZustaendigkeitAttribute> zustaendigkeit = new ArrayList<>(
			zustaendigkeitAttributGruppe.getImmutableZustaendigkeitAttribute());

		List<ZustaendigkeitAttribute> zustaendigkeitDefragmentiert = LinearReferenzierteAttribute
			.defragmentiereLinearReferenzierteAttribute(zustaendigkeit, geometry.getLength(),
				MINIMALE_LAENGE_LINEAR_REFERENZIERTER_SEGMENTE);

		zustaendigkeitAttributGruppe.replaceZustaendigkeitAttribute(zustaendigkeitDefragmentiert);

		// Defragmentiere Geschwindigkeit
		List<GeschwindigkeitAttribute> geschwindigkeit = new ArrayList<>(
			geschwindigkeitAttributGruppe.getImmutableGeschwindigkeitAttribute());

		List<GeschwindigkeitAttribute> geschwindigkeitDefragmentiert = LinearReferenzierteAttribute
			.defragmentiereLinearReferenzierteAttribute(geschwindigkeit, geometry.getLength(),
				MINIMALE_LAENGE_LINEAR_REFERENZIERTER_SEGMENTE);

		geschwindigkeitAttributGruppe.replaceGeschwindigkeitAttribute(geschwindigkeitDefragmentiert);

		// Defragmentiere Fuehrungsform
		List<FuehrungsformAttribute> fuehrungsformLinks = new ArrayList<>(
			fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeLinks());

		List<FuehrungsformAttribute> defragmentierteFuehrungsformLinks = LinearReferenzierteAttribute
			.defragmentiereLinearReferenzierteAttribute(fuehrungsformLinks, geometry.getLength(),
				MINIMALE_LAENGE_LINEAR_REFERENZIERTER_SEGMENTE);

		if (fuehrungsformAttributGruppe.isZweiseitig()) {
			List<FuehrungsformAttribute> fuehrungsformRechts = new ArrayList<>(
				fuehrungsformAttributGruppe.getImmutableFuehrungsformAttributeRechts());

			List<FuehrungsformAttribute> defragmentierteFuehrungsformRechts = LinearReferenzierteAttribute
				.defragmentiereLinearReferenzierteAttribute(fuehrungsformRechts, geometry.getLength(),
					MINIMALE_LAENGE_LINEAR_REFERENZIERTER_SEGMENTE);

			fuehrungsformAttributGruppe.replaceFuehrungsformAttribute(defragmentierteFuehrungsformLinks,
				defragmentierteFuehrungsformRechts);
		} else {
			fuehrungsformAttributGruppe.replaceFuehrungsformAttribute(defragmentierteFuehrungsformLinks);
		}
	}

	public boolean isRadNETZ() {
		return hasNetzklasse(Netzklasse.RADNETZ_ALLTAG) || hasNetzklasse(Netzklasse.RADNETZ_FREIZEIT) || hasNetzklasse(
			Netzklasse.RADNETZ_ZIELNETZ);
	}

	public SeitenbezogeneProfilEigenschaften getProfilEigenschaften() {
		return SeitenbezogeneProfilEigenschaften.of(
			fuehrungsformAttributGruppe.getBelagArtWertMitGroesstemAnteilLinks(),
			fuehrungsformAttributGruppe.getBelagArtWertMitGroesstemAnteilRechts(),
			fuehrungsformAttributGruppe.getRadverkehrsfuehrungWertMitGroesstemAnteilLinks(),
			fuehrungsformAttributGruppe.getRadverkehrsfuehrungWertMitGroesstemAnteilRechts()
		);
	}
}
