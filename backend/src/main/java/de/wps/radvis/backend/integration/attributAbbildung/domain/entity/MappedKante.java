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

package de.wps.radvis.backend.integration.attributAbbildung.domain.entity;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Embeddable
public class MappedKante {
	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "von", column = @Column(name = "von_grundnetz")),
		@AttributeOverride(name = "bis", column = @Column(name = "bis_grundnetz"))
	})
	private LinearReferenzierterAbschnitt linearReferenzierterAbschnittAufGrundnetzKante;
	@Embedded
	@AttributeOverrides({
		@AttributeOverride(name = "von", column = @Column(name = "von_quellnetz")),
		@AttributeOverride(name = "bis", column = @Column(name = "bis_quellnetz"))
	})
	private LinearReferenzierterAbschnitt linearReferenzierterAbschnittAufQuellnetzKante;
	private boolean richtungenVertauscht;
	private Long kanteId;

	public MappedKante(LineareReferenzProjektionsergebnis lineareReferenzFuerKantenSegment,
		LineareReferenzProjektionsergebnis lineareReferenzFuerLinearReferenzierteAttribute, Long kanteId) {
		this.linearReferenzierterAbschnittAufGrundnetzKante = lineareReferenzFuerKantenSegment.getErgebnisProjektion();
		this.linearReferenzierterAbschnittAufQuellnetzKante = lineareReferenzFuerLinearReferenzierteAttribute
			.getErgebnisProjektion();
		this.richtungenVertauscht = lineareReferenzFuerKantenSegment
			.isWurdenVonUndBisBeiProjektionVertauscht() != lineareReferenzFuerLinearReferenzierteAttribute
				.isWurdenVonUndBisBeiProjektionVertauscht();
		this.kanteId = kanteId;
	}
}
