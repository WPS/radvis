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

package de.wps.radvis.backend.systemnachricht.schnittstelle;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.systemnachricht.domain.SystemnachrichtService;

@RestController
@RequestMapping("/api/systemnachricht")
public class SystemnachrichtController {
	private final SystemnachrichtService systemnachrichtService;
	private final SystemnachrichtGuard systemnachrichtGuard;

	public SystemnachrichtController(SystemnachrichtService systemnachrichtService,
		SystemnachrichtGuard systemnachrichtGuard) {
		this.systemnachrichtService = systemnachrichtService;
		this.systemnachrichtGuard = systemnachrichtGuard;
	}

	@DeleteMapping
	public void delete(Authentication authentication) {
		systemnachrichtGuard.delete(authentication);
		systemnachrichtService.delete();
	}

	@PostMapping
	public void create(Authentication authentication, @RequestBody CreateSystemnachrichtCommand command) {
		systemnachrichtGuard.create(authentication, command);
		this.systemnachrichtService.create(command.getText());
	}

	@GetMapping
	public Optional<SystemnachrichtView> get() {
		return systemnachrichtService.find().map(SystemnachrichtView::new);
	}

}
