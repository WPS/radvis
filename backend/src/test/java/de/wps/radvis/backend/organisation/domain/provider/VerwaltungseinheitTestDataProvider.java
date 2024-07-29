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

package de.wps.radvis.backend.organisation.domain.provider;

import java.util.Set;

import de.wps.radvis.backend.common.GeometryTestdataProvider;
import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft.GebietskoerperschaftBuilder;
import de.wps.radvis.backend.organisation.domain.entity.Organisation;

public class VerwaltungseinheitTestDataProvider {
	public static GebietskoerperschaftBuilder defaultGebietskoerperschaft() {
		return Gebietskoerperschaft.builder()
			.name("DefaultOrganisation")
			.organisationsArt(OrganisationsArt.KREIS)
			.bereich(GeometryTestdataProvider.createQuadratischerBereich(0, 0, 100, 100))
			.aktiv(true);
	}

	public static Organisation.OrganisationBuilder defaultOrganisation() {
		return Organisation.builder()
			.name("DefaultOrganisation").zustaendigFuerBereichOf(Set.of())
			.organisationsArt(OrganisationsArt.SONSTIGES)
			.aktiv(true);
	}
}
