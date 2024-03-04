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

package de.wps.radvis.backend.integration.attributAbbildung.domain;

import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.MehrdeutigeProjektionException;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.ProjektionsLaengeZuKurzException;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.ProjektionsLaengenVerhaeltnisException;
import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.ProjizierteKanteIstIsoliertException;
import de.wps.radvis.backend.netzfehler.domain.NetzfehlerRepository;
import de.wps.radvis.backend.netzfehler.domain.ProtokollService;
import de.wps.radvis.backend.netzfehler.domain.entity.Netzfehler;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerBeschreibung;
import de.wps.radvis.backend.netzfehler.domain.valueObject.NetzfehlerTyp;
import jakarta.transaction.Transactional;
import lombok.NonNull;

@Transactional
public class AttributeProjektionsProtokollService extends ProtokollService {

	public AttributeProjektionsProtokollService(
		@NonNull NetzfehlerRepository netzfehlerRepository) {
		super(netzfehlerRepository);
	}

	public void handle(ProjektionsLaengenVerhaeltnisException e, String jobZuordnung) {
		Netzfehler netzfehler = new Netzfehler(NetzfehlerTyp.ATTRIBUT_PROJEKTION,
			NetzfehlerBeschreibung.of(e.getMessage()), jobZuordnung,
			e.getDlmUeberschneidung());
		protokolliere(netzfehler);
	}

	public void handle(ProjektionsLaengeZuKurzException e, String jobZuordnung) {
		Netzfehler netzfehler = new Netzfehler(NetzfehlerTyp.ATTRIBUT_PROJEKTION,
			NetzfehlerBeschreibung.of(e.getMessage()), jobZuordnung,
			e.getDlmUeberschneidung());
		protokolliere(netzfehler);
	}

	public void handle(MehrdeutigeProjektionException e, String jobZuordnung) {
		Netzfehler netzfehler = new Netzfehler(NetzfehlerTyp.ATTRIBUT_PROJEKTION,
			NetzfehlerBeschreibung.of(e.getMessage()), jobZuordnung,
			e.getGrundnetzKanteGeometry());
		protokolliere(netzfehler);
	}

	public void handle(ProjizierteKanteIstIsoliertException e, String jobZuordnung) {
		Netzfehler netzfehler = new Netzfehler(NetzfehlerTyp.ATTRIBUT_PROJEKTION,
			NetzfehlerBeschreibung.of(e.getMessage()), jobZuordnung,
			e.getGeometry());
		protokolliere(netzfehler);
	}

}
