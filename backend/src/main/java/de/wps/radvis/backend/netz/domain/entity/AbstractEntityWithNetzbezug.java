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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.ToString;

@MappedSuperclass
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = true)
public abstract class AbstractEntityWithNetzbezug extends VersionierteEntity {
	public AbstractEntityWithNetzbezug(Long id, Long version) {
		super(id, version);
	}

	public abstract AbstractNetzBezug getNetzbezug();

	public abstract void ersetzeKnotenInNetzbezug(Map<Long, Knoten> ersatzKnoten);

	public abstract void removeKnotenFromNetzbezug(Collection<Long> knotenIds);

	public abstract void ersetzeKanteInNetzbezug(Kante zuErsetzendeKante, Set<Kante> ersetztDurch,
		double erlaubteAbweichungKantenRematch);

	public abstract void removeKanteFromNetzbezug(Collection<Long> kantenIds);
}
