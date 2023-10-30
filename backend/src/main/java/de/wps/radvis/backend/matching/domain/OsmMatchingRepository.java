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

package de.wps.radvis.backend.matching.domain;

import org.locationtech.jts.geom.LineString;

import de.wps.radvis.backend.matching.domain.exception.KeinMatchGefundenException;
import de.wps.radvis.backend.matching.domain.valueObject.LinearReferenziertesOsmMatchResult;

public interface OsmMatchingRepository {
	LineString matchGeometry(LineString geometrie, String profile) throws KeinMatchGefundenException;

	/**
	 * @param geometrie
	 * 	Bei Kreis-Geometrien und Geometrien mit Kehrtwenden können die ermittelten lin. Referenzen u.U. ungenau oder falsch sein.
	 * @param profile
	 * 	"bike"|"car"|"foot"
	 * @return das MatchResult enthält
	 * <ul>
	 *     <li>die gemachte Geometrie inkl.(!!!) etwaiger Kehrtwenden (Matching-Artefakte)</li>
	 *     <li>die lin. Referenzen der gemachten Geometrie auf OsmWays exkl. (!!!) derjenigen, die auf Kehrtwenden in der gemachten Geometrie zurückzuführen.</li>
	 * </ul>
	 * @throws KeinMatchGefundenException
	 * 	Falls kein oder kein hinreichend gutes Match gefunden wurde
	 */
	LinearReferenziertesOsmMatchResult matchGeometryLinearReferenziert(LineString geometrie, String profile)
		throws KeinMatchGefundenException;
}
