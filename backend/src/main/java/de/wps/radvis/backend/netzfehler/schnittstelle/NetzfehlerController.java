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

package de.wps.radvis.backend.netzfehler.schnittstelle;

import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.netzfehler.domain.NetzfehlerService;
import de.wps.radvis.backend.netzfehler.schnittstelle.view.NetzfehlerView;
import lombok.NonNull;

@RestController
@RequestMapping("/api/netzfehler")
@Validated
public class NetzfehlerController {

	private final NetzfehlerGuard netzfehlerGuard;

	private final NetzfehlerService netzfehlerService;

	public NetzfehlerController(
		@NonNull NetzfehlerGuard netzfehlerGuard,
		@NonNull NetzfehlerService netzfehlerService) {
		this.netzfehlerGuard = netzfehlerGuard;
		this.netzfehlerService = netzfehlerService;
	}

	@GetMapping("{id}")
	public NetzfehlerView getNetzfehler(Authentication authentication, @PathVariable("id") Long id) {
		netzfehlerGuard.getNetzfehler(authentication, id);
		return new NetzfehlerView(netzfehlerService.get(id));
	}

	@PutMapping("{id}/erledigt")
	public void setNetzfehlerErledigt(Authentication authentication, @PathVariable("id") Long id) {
		netzfehlerGuard.setNetzfehlerErledigt(authentication, id);
		netzfehlerService.alsErledigtMarkieren(id);
	}
}
