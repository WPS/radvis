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

package de.wps.radvis.backend.authentication.schnittstelle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;

import de.wps.radvis.backend.authentication.domain.entity.RadVisUserDetails;
import de.wps.radvis.backend.benutzer.domain.BenutzerService;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.valueObject.ServiceBwId;

public class RadVisUserDetailsService implements UserDetailsService {

	private final BenutzerService benutzerService;

	public RadVisUserDetailsService(BenutzerService benutzerService) {
		this.benutzerService = benutzerService;
	}

	@Override
	public UserDetails loadUserByUsername(String serviceBwId) {
		ServiceBwId id = ServiceBwId.of(serviceBwId);
		Optional<Benutzer> potentialBenutzer = benutzerService.findBenutzerByServiceBwIdAndInitialize(id);

		return potentialBenutzer.map(RadVisUserDetailsService::fromUser).orElse(new RadVisUserDetails(id));
	}

	public UserDetails loadUserByUsername(String serviceBwId, Saml2AuthenticatedPrincipal saml2AuthenticatedPrincipal) {
		ServiceBwId id = ServiceBwId.of(serviceBwId);
		Optional<Benutzer> potentialBenutzer = benutzerService.findBenutzerByServiceBwIdAndInitialize(id);

		return potentialBenutzer.map(benutzer -> fromUser(benutzer, saml2AuthenticatedPrincipal
			.getRelyingPartyRegistrationId(), saml2AuthenticatedPrincipal.getSessionIndexes()))
			.orElse(new RadVisUserDetails(id, saml2AuthenticatedPrincipal.getRelyingPartyRegistrationId(),
				saml2AuthenticatedPrincipal.getSessionIndexes()));
	}

	public static UserDetails fromUser(Benutzer benutzer) {
		return new RadVisUserDetails(benutzer, createAuthorities(benutzer));
	}

	public static UserDetails fromUser(Benutzer benutzer, String relyingPartyRegistrationId,
		List<String> sessionIndex) {
		return new RadVisUserDetails(benutzer, createAuthorities(benutzer), relyingPartyRegistrationId, sessionIndex);
	}

	private static List<GrantedAuthority> createAuthorities(Benutzer benutzer) {
		List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
		grantedAuthorities.add(() -> benutzer.getStatus().name());

		benutzer.getRechte().forEach(recht -> {
			GrantedAuthority tmpGrantedAuthority = () -> recht.name();
			grantedAuthorities.add(tmpGrantedAuthority);
		});
		return grantedAuthorities;
	}

}
