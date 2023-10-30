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

package de.wps.radvis.backend.integration.netzbildung.domain;

import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.StartUndEndpunktGleichException;
import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Kante;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.domain.valueObject.DlmId;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractNetzbildungService {
	protected NetzService netzService;
	private KnotenIndex knotenIndex;

	protected EntityManager entityManager;

	public AbstractNetzbildungService(NetzService netzService, EntityManager entityManager) {
		this.entityManager = entityManager;
		this.netzService = netzService;
	}

	abstract protected QuellSystem getQuelle();

	protected void addKante(LineString lineString, KantenAttributGruppe kantenAttributGruppe,
		FahrtrichtungAttributGruppe fahrtrichtungAttributgruppe,
		ZustaendigkeitAttributGruppe zustaendigkeitAttributgruppe,
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe,
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe,
		DlmId dlmId, String technischeFeatureID) throws StartUndEndpunktGleichException {
		addKante(lineString, kantenAttributGruppe,
			fahrtrichtungAttributgruppe,
			zustaendigkeitAttributgruppe,
			geschwindigkeitAttributGruppe,
			fuehrungsformAttributGruppe,
			dlmId, technischeFeatureID, false);
	}

	protected void addKante(LineString lineString, KantenAttributGruppe kantenAttributGruppe,
		FahrtrichtungAttributGruppe fahrtrichtungAttributgruppe,
		ZustaendigkeitAttributGruppe zustaendigkeitAttributgruppe,
		GeschwindigkeitAttributGruppe geschwindigkeitAttributGruppe,
		FuehrungsformAttributGruppe fuehrungsformAttributGruppe,
		DlmId dlmId, String technischeFeatureID, boolean isGrundnetz) throws StartUndEndpunktGleichException {
		Point vonPoint = lineString.getStartPoint();
		Point nachPoint = lineString.getEndPoint();

		if (vonPoint.equals(nachPoint)) {
			throw new StartUndEndpunktGleichException(getQuelle().toString(), vonPoint);
		}

		Knoten vonKnoten = createOrMergeKnoten(vonPoint);
		Knoten nachKnoten = createOrMergeKnoten(nachPoint);

		// Überprüfen, ob die Geometrien der beiden Knoten identisch sind,
		// um ggf. den weiter entfernten Endpunkt des LineStrings nicht aus dem Index zu holen
		if (vonKnoten.getKoordinate().equals(nachKnoten.getKoordinate())) {
			Point pointAusIndex = vonKnoten.getPoint();
			if (vonPoint.distance(pointAusIndex) <= nachPoint.distance(pointAusIndex)) {
				nachKnoten = new Knoten(nachPoint, getQuelle());
			} else {
				vonKnoten = new Knoten(vonPoint, getQuelle());
			}
		}

		Kante kante = new Kante(dlmId,
			technischeFeatureID,
			vonKnoten,
			nachKnoten,
			lineString,
			false,
			getQuelle(),
			kantenAttributGruppe,
			fahrtrichtungAttributgruppe,
			zustaendigkeitAttributgruppe,
			geschwindigkeitAttributGruppe,
			fuehrungsformAttributGruppe);
		kante.setGrundnetz(isGrundnetz);
		netzService.saveKante(kante);
		log.debug("Kante zwischen Punkten {}, {} hinzugefügt", vonKnoten.getPoint(),
			nachKnoten.getPoint());
		knotenIndex.fuegeEin(vonKnoten);
		knotenIndex.fuegeEin(nachKnoten);
	}

	private Knoten createOrMergeKnoten(Point point) {
		Optional<Knoten> knotenOptional = findeKnoten(point);
		Knoten knoten;
		if (knotenOptional.isPresent()) {
			knoten = entityManager.merge(knotenOptional.get());
		} else {
			knoten = new Knoten(point, getQuelle());
		}
		return knoten;
	}

	protected void initKnotenIndex() {
		this.knotenIndex = new KnotenIndex();
	}

	protected Optional<Knoten> findeKnoten(Point point) {
		return knotenIndex.finde(point);
	}
}
