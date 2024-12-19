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

package de.wps.radvis.backend.administration.schnittstelle.command;

import java.time.LocalDate;
import java.util.Set;

import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.annotation.JsonInclude;

import de.wps.radvis.backend.benutzer.domain.valueObject.Mailadresse;
import de.wps.radvis.backend.benutzer.domain.valueObject.Name;
import de.wps.radvis.backend.benutzer.domain.valueObject.Rolle;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@ToString
@Validated
public class SaveBenutzerCommand {
	@NotNull
	private long id;
	@NotNull
	private long version;
	private LocalDate ablaufdatum;
	@NotNull
	private Name vorname;
	@NotNull
	private Name nachname;
	@NotNull
	private Mailadresse email;
	@NotNull
	private Long organisation;
	@NotNull
	@NotEmpty
	private Set<Rolle> rollen;
}
