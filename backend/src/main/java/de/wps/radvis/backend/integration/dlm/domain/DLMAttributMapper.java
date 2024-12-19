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

import java.util.HashSet;

import de.wps.radvis.backend.netz.domain.entity.FahrtrichtungAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.FuehrungsformAttribute;
import de.wps.radvis.backend.netz.domain.entity.GeschwindigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.KantenAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute;
import de.wps.radvis.backend.netz.domain.entity.KantenAttribute.KantenAttributeBuilder;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import de.wps.radvis.backend.quellimport.common.domain.entity.ImportedFeature;

public class DLMAttributMapper {

	public KantenAttributGruppe mapKantenAttributGruppe(ImportedFeature feature) {
		KantenAttributeBuilder builder = KantenAttribute.builder();

		if (feature.hasAttribut("eigenname")) {
			builder.strassenName(StrassenName.of(feature.getAttribut("eigenname").toString()));
		}
		if (feature.hasAttribut("bezeichnung")) {
			builder.strassenNummer(StrassenNummer.of(feature.getAttribut("bezeichnung").toString()));
		}

		return new KantenAttributGruppe(builder.build(), new HashSet<>(), new HashSet<>());
	}

	public GeschwindigkeitAttribute mapGeschindigkeitAttribute(ImportedFeature feature) {
		return GeschwindigkeitAttribute.builder().build();
	}

	public FuehrungsformAttribute mapFuehrungsformAttribute(ImportedFeature feature) {
		return FuehrungsformAttribute.builder().build();
	}

	public FahrtrichtungAttributGruppe mapFahrtrichtungAttributGruppe(ImportedFeature importedFeature) {
		return FahrtrichtungAttributGruppe.builder().build();
	}

	public ZustaendigkeitAttribute mapZustaendigkeitAttribute(ImportedFeature feature) {
		return ZustaendigkeitAttribute.builder().build();
	}
}
