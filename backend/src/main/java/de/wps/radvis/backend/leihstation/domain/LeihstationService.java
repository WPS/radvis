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

package de.wps.radvis.backend.leihstation.domain;

import java.util.Set;

import org.locationtech.jts.geom.Geometry;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.common.domain.service.AbstractVersionierteEntityService;
import de.wps.radvis.backend.leihstation.domain.entity.Leihstation;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;

public class LeihstationService extends AbstractVersionierteEntityService<Leihstation> {

	private final BenutzerResolver benutzerResolver;
	private final ZustaendigkeitsService zustaendigkeitsService;

	public LeihstationService(LeihstationRepository repository,
		BenutzerResolver benutzerResolver,
		ZustaendigkeitsService zustaendigkeitsService) {
		super(repository);
		this.benutzerResolver = benutzerResolver;
		this.zustaendigkeitsService = zustaendigkeitsService;
	}

	public boolean darfBenutzerBearbeiten(Authentication authentication, Leihstation leihstation) {
		Geometry geometry = leihstation.getGeometrie();
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		Set<Recht> benutzerRechte = aktiverBenutzer.getRechte();

		return benutzerRechte.contains(Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN) &&
			zustaendigkeitsService.istImZustaendigkeitsbereich(geometry, aktiverBenutzer);
	}
}
