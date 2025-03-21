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

package de.wps.radvis.backend.quellimport.ttsib.domain.entity;

import de.wps.radvis.backend.quellimport.ttsib.domain.valueObject.TtSibEinordnung;
import de.wps.radvis.backend.quellimport.ttsib.domain.valueObject.TtSibQuerschnittArt;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@NoArgsConstructor
@ToString
public class TtSibStreifen extends TtSibAbstractEntity {
	@Getter
	private Integer breiteVon;

	@Getter
	private Integer breiteBis;

	@Getter
	@Enumerated(EnumType.STRING)
	private TtSibQuerschnittArt art;

	@Getter
	@Enumerated(EnumType.STRING)
	private TtSibEinordnung einordnung;

	@Getter
	private Integer nr;

	public TtSibStreifen(Integer breiteVon, Integer breiteBis, TtSibEinordnung ttSibEinordnung, Integer nr,
		TtSibQuerschnittArt ttSibQuerschnittArt) {
		this.breiteVon = breiteVon;
		this.breiteBis = breiteBis;
		this.einordnung = ttSibEinordnung;
		this.nr = nr;
		this.art = ttSibQuerschnittArt;
	}

	public boolean isRadwegStreifen() {
		return this.art.isRadweg();
	}
}
