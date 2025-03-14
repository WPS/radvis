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

package de.wps.radvis.backend.netz.domain.event;

import java.time.LocalDateTime;
import java.util.List;

import de.wps.radvis.backend.common.domain.RadVisDomainEvent;
import de.wps.radvis.backend.netz.domain.entity.Knoten;
import de.wps.radvis.backend.netz.domain.entity.KnotenDeleteStatistik;
import de.wps.radvis.backend.netz.domain.valueObject.NetzAenderungAusloeser;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class KnotenDeletedEvent implements RadVisDomainEvent {
	private final NetzAenderungAusloeser ausloeser;

	private final LocalDateTime datum;

	private final KnotenDeleteStatistik statistik;

	private List<Knoten> knoten;

	public KnotenDeletedEvent(List<Knoten> knoten, NetzAenderungAusloeser ausloeser,
		LocalDateTime datum, KnotenDeleteStatistik statistik) {
		super();
		this.knoten = knoten;
		this.ausloeser = ausloeser;
		this.datum = datum;
		this.statistik = statistik;
	}
}
