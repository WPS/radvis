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

package de.wps.radvis.backend.barriere.domain;

import de.wps.radvis.backend.barriere.domain.entity.Barriere;
import de.wps.radvis.backend.barriere.domain.repository.BarriereRepository;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.common.domain.service.AbstractVersionierteEntityService;
import de.wps.radvis.backend.organisation.domain.VerwaltungseinheitService;

public class BarriereService extends AbstractVersionierteEntityService<Barriere> {
	private final VerwaltungseinheitService verwaltungseinheitService;

	public BarriereService(BarriereRepository repository, VerwaltungseinheitService verwaltungseinheitService) {
		super(repository);
		this.verwaltungseinheitService = verwaltungseinheitService;
	}

	public boolean darfNutzerBearbeiten(Benutzer benutzer, Barriere barriere) {
		if (benutzer.hatRecht(Recht.FURTEN_KREUZUNGEN_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN)) {
			return verwaltungseinheitService.istUebergeordnet(benutzer.getOrganisation(),
				barriere.getVerantwortlich());
		}
		return false;
	}
}
