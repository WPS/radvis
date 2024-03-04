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

package de.wps.radvis.backend.basicAuthentication.domain;

import java.io.Serial;
import java.util.ArrayList;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import de.wps.radvis.backend.benutzer.domain.BenutzerHolder;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import lombok.Getter;

@Getter
public class BenutzerBasicAuthenticationToken extends UsernamePasswordAuthenticationToken implements BenutzerHolder {

	@Serial
	private static final long serialVersionUID = 721188193922596154L;

	public BenutzerBasicAuthenticationToken(Benutzer benutzer, Object credentials) {
		super(benutzer, credentials, new ArrayList<>());
	}

	@Override
	public Benutzer getBenutzer() {
		return (Benutzer) getPrincipal();
	}

	@Override
	public String getName() {
		return getBenutzer().getBasicAuthAnmeldeName();
	}
}
