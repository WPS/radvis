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

package de.wps.radvis.backend.netzfehler.domain.valueObject;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

/**
 * Brauchen wir zur Referenzierung, weil die IDs der Konsistenzregel-Verletzungen sich bei jedem Jobdurchlauf verändern
 * und die Kombination aus identity und typ ist jedoch stabil.
 */
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor(staticName = "of")
@ToString
public class KonsistenzregelVerletzungReferenz {
	@Getter
	@NonNull
	@Column(name = "konsistenzregel_identity")
	private String identity;

	@Getter
	@NonNull
	@Column(name = "konsistenzregel_typ")
	private String typ;
}
