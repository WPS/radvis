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

package de.wps.radvis.backend.integration.dlm.domain;

import java.util.Optional;

import org.locationtech.jts.geom.Point;

import de.wps.radvis.backend.common.domain.valueObject.QuellSystem;
import de.wps.radvis.backend.integration.dlm.domain.entity.KnotenTupel;
import de.wps.radvis.backend.integration.netzbildung.domain.exception.StartUndEndpunktGleichException;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenIndex;

public class FindKnotenFromIndexService {

	public KnotenTupel findOrCreateKnotenTupel(Point vonPoint, Point nachPoint, KnotenIndex knotenIndex)
		throws StartUndEndpunktGleichException {
		if (vonPoint.equals(nachPoint)) {
			throw new StartUndEndpunktGleichException(QuellSystem.DLM.toString(), vonPoint);
		}

		Optional<Knoten> knotenOptionalVon = knotenIndex.finde(vonPoint);
		Knoten vonKnoten = knotenOptionalVon.orElseGet(() -> new Knoten(vonPoint, QuellSystem.DLM));

		Optional<Knoten> knotenOptionalNach = knotenIndex.finde(nachPoint);
		Knoten nachKnoten = knotenOptionalNach.orElseGet(() -> new Knoten(nachPoint, QuellSystem.DLM));

		// Überprüfen, ob die Geometrien der beiden Knoten identisch sind,
		// um ggf. den weiter entfernten Endpunkt des LineStrings nicht aus dem Index zu holen
		if (vonKnoten.getKoordinate().equals(nachKnoten.getKoordinate())) {
			Point pointAusIndex = vonKnoten.getPoint();
			if (vonPoint.distance(pointAusIndex) <= nachPoint.distance(pointAusIndex)) {
				nachKnoten = new Knoten(nachPoint, QuellSystem.DLM);
				if (knotenOptionalNach.isPresent()) {
					knotenIndex.fuegeEin(nachKnoten);
				}
			} else {
				vonKnoten = new Knoten(vonPoint, QuellSystem.DLM);
				if (knotenOptionalVon.isPresent()) {
					knotenIndex.fuegeEin(vonKnoten);
				}
			}
		}

		if (knotenOptionalVon.isEmpty()) {
			knotenIndex.fuegeEin(vonKnoten);
		}
		if (knotenOptionalNach.isEmpty()) {
			knotenIndex.fuegeEin(nachKnoten);
		}

		return new KnotenTupel(vonKnoten, nachKnoten);
	}

	KnotenTupel findOrCreateKnotenTupelMitKnotenGefixt(Point pointFuerUpdatedKnoten, Knoten alterKnoten,
		KnotenIndex knotenIndex,
		boolean istNeuerKnotenVonKnoten)
		throws StartUndEndpunktGleichException {
		if (pointFuerUpdatedKnoten.equals(alterKnoten.getPoint())) {
			throw new StartUndEndpunktGleichException(QuellSystem.DLM.toString(), pointFuerUpdatedKnoten);
		}

		Optional<Knoten> neuerKnotenOptional = knotenIndex.finde(pointFuerUpdatedKnoten);
		Knoten neuerKnoten;

		if (neuerKnotenOptional.isPresent()) {
			neuerKnoten = neuerKnotenOptional.get();
			// Falls der neue Knoten auf dem alten liegt, erschaffe einen neuen Knoten
			if (neuerKnoten.getKoordinate().equals(alterKnoten.getKoordinate())) {
				neuerKnoten = new Knoten(pointFuerUpdatedKnoten, QuellSystem.DLM);
				knotenIndex.fuegeEin(neuerKnoten);
			}
		} else {
			// Falls Knoten nicht im Index, erschaffe neuen
			neuerKnoten = new Knoten(pointFuerUpdatedKnoten, QuellSystem.DLM);
			knotenIndex.fuegeEin(neuerKnoten);
		}

		if (istNeuerKnotenVonKnoten) {
			return new KnotenTupel(neuerKnoten, alterKnoten);
		} else {
			return new KnotenTupel(alterKnoten, neuerKnoten);
		}
	}
}
