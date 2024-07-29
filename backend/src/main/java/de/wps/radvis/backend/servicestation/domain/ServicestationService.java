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

package de.wps.radvis.backend.servicestation.domain;

import java.util.Set;

import org.locationtech.jts.geom.Geometry;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.BenutzerResolver;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.common.domain.service.AbstractVersionierteEntityService;
import de.wps.radvis.backend.dokument.domain.entity.Dokument;
import de.wps.radvis.backend.netz.domain.service.ZustaendigkeitsService;
import de.wps.radvis.backend.servicestation.domain.entity.Servicestation;
import de.wps.radvis.backend.servicestation.domain.valueObject.ServicestationenQuellSystem;
import jakarta.persistence.EntityNotFoundException;

public class ServicestationService extends AbstractVersionierteEntityService<Servicestation> {

	private final BenutzerResolver benutzerResolver;
	private final ZustaendigkeitsService zustaendigkeitsService;

	public ServicestationService(ServicestationRepository repository,
		BenutzerResolver benutzerResolver,
		ZustaendigkeitsService zustaendigkeitsService) {
		super(repository);
		this.benutzerResolver = benutzerResolver;
		this.zustaendigkeitsService = zustaendigkeitsService;
	}

	public Dokument getDokument(Long servicestationId, Long dokumentId) {
		return get(servicestationId)
			.getDokumentListe()
			.getDokumente()
			.stream().filter(d -> dokumentId.equals(d.getId()))
			.findFirst()
			.orElseThrow(EntityNotFoundException::new);
	}

	public void addDokument(Long servicestationId, Dokument dokument) {
		Servicestation servicestation = get(servicestationId);
		servicestation.addDokument(dokument);
	}

	public void deleteDokument(Long servicestationId, Long dokumentId) {
		Servicestation servicestation = get(servicestationId);
		servicestation.deleteDokument(dokumentId);
	}

	public boolean darfBenutzerBearbeiten(Authentication authentication, Servicestation servicestation) {
		Geometry geometry = servicestation.getGeometrie();
		Benutzer aktiverBenutzer = benutzerResolver.fromAuthentication(authentication);
		Set<Recht> benutzerRechte = aktiverBenutzer.getRechte();

		return servicestation.getQuellSystem().equals(ServicestationenQuellSystem.RADVIS) &&
			benutzerRechte.contains(Recht.SERVICEANGEBOTE_IM_ZUSTAENDIGKEITSBEREICH_ERFASSEN_BEARBEITEN) &&
			zustaendigkeitsService.istImZustaendigkeitsbereich(geometry, aktiverBenutzer);
	}
}
