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

package de.wps.radvis.backend.organisation.schnittstelle;

import java.io.File;

import de.wps.radvis.backend.common.domain.valueObject.OrganisationsArt;
import de.wps.radvis.backend.common.schnittstelle.CoordinateReferenceSystemConverter;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitImportRepository;
import de.wps.radvis.backend.organisation.schnittstelle.repositoryImpl.VerwaltungseinheitBkgFormatImportRepositoryImpl;
import de.wps.radvis.backend.organisation.schnittstelle.repositoryImpl.VerwaltungseinheitCustomFormatImportRepositoryImpl;
import lombok.NonNull;

public class VerwaltungseinheitImportRepositoryFactory {
	private final CoordinateReferenceSystemConverter coordinateConverter;
	private final OrganisationsArt obersteGebietskoerperschaftArt;
	private final String obersteGebietskoerperschaftName;

	public VerwaltungseinheitImportRepositoryFactory(@NonNull CoordinateReferenceSystemConverter coordinateConverter,
		@NonNull OrganisationsArt obersteGebietskoerperschaftArt, @NonNull String obersteGebietskoerperschaftName) {
		this.coordinateConverter = coordinateConverter;
		this.obersteGebietskoerperschaftArt = obersteGebietskoerperschaftArt;
		this.obersteGebietskoerperschaftName = obersteGebietskoerperschaftName;
	}

	public VerwaltungseinheitImportRepository getImportRepository(File gebietskoerperschaftShpVerzeichnis) {
		if (VerwaltungseinheitBkgFormatImportRepositoryImpl.checkShapeFiles(gebietskoerperschaftShpVerzeichnis)) {
			return new VerwaltungseinheitBkgFormatImportRepositoryImpl(coordinateConverter,
				obersteGebietskoerperschaftArt, obersteGebietskoerperschaftName,
				gebietskoerperschaftShpVerzeichnis);
		}

		if (VerwaltungseinheitCustomFormatImportRepositoryImpl.checkShapeFiles(gebietskoerperschaftShpVerzeichnis)) {
			return new VerwaltungseinheitCustomFormatImportRepositoryImpl(coordinateConverter,
				obersteGebietskoerperschaftName, obersteGebietskoerperschaftArt,
				gebietskoerperschaftShpVerzeichnis);
		}

		throw new UnsupportedOperationException("Kein VerwaltungseinheitImportRepository für shapes in "
			+ gebietskoerperschaftShpVerzeichnis.toString() + " gefunden.");
	}
}
