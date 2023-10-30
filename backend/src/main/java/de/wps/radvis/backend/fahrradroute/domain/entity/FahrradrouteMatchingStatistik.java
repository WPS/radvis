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

package de.wps.radvis.backend.fahrradroute.domain.entity;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

public class FahrradrouteMatchingStatistik {
	public int anzahlMatchingErfolgreich = 0;
	public int anzahlMatchingFehlgeschlagen = 0;
	public int anzahlRoutingErfolgreich = 0;
	public int anzahlRoutingFehlgeschlagen = 0;

	public int anzahlRoutenTeilweiseAusserhalbBW = 0;
	public int anzahlRoutenNachBwZuschnittErfolgreich = 0;
	public int anzahlNachZuschnittVieleTeilstrecken = 0;

	public int anzahlRoutenMitKehrtwenden = 0;
	public int anzahlKehrtwendenGesamt = 0;
	public int anzahlRoutenMitAbweichungen = 0;

	@Override
	public String toString() {
		return ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE);
	}
}
