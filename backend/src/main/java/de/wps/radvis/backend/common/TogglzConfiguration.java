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

package de.wps.radvis.backend.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.togglz.core.manager.EnumBasedFeatureProvider;
import org.togglz.core.spi.FeatureProvider;
import org.togglz.core.user.UserProvider;
import org.togglz.spring.security.SpringSecurityUserProvider;

import de.wps.radvis.backend.benutzer.domain.valueObject.Recht;
import de.wps.radvis.backend.common.domain.FeatureTogglz;

@Configuration
public class TogglzConfiguration {
	@Bean
	@SuppressWarnings({ "unchecked", "varargs" })
	public FeatureProvider featureProvider() {
		return new EnumBasedFeatureProvider(FeatureTogglz.class);
	}

	@Bean
	public UserProvider getUserProvider() {
		// Der Parameter legt fest, welches Recht ein Benutzer haben muss um die Togglz im laufenden Betrieb zu ändern.
		// Der Administrator ist der einzige der das Recht "Jobs ausführen" hat, daher ist das eine logische Wahl.
		return new SpringSecurityUserProvider(Recht.JOBS_AUSFUEHREN.name());
	}
}
