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

package de.wps.radvis.backend.authentication.domain.entity;

import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;
import lombok.Getter;

public class RadVisUserDetails extends User {

	private static final long serialVersionUID = 3940764147762591350L;

	@Getter
	private ServiceBwId serviceBwId;

	@Getter
	private boolean isRegistered;

	@Getter
	private Benutzer benutzer;

	public RadVisUserDetails(Benutzer benutzer, List<GrantedAuthority> grantedAuthorities) {
		super(benutzer.getServiceBwId().toString(), "{noop}", grantedAuthorities);
		this.benutzer = benutzer;
		this.isRegistered = true;
	}

	public RadVisUserDetails(ServiceBwId serviceBwId) {
		super(serviceBwId.toString(), "{noop}", Collections.emptyList());
		this.serviceBwId = serviceBwId;
		this.isRegistered = false;
	}

}
