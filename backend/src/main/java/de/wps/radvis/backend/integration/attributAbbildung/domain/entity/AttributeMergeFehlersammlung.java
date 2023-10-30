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

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Geometry;

import de.wps.radvis.backend.integration.attributAbbildung.domain.exception.MehrdeutigeLinearReferenzierteAttributeException;
import lombok.Getter;

public class AttributeMergeFehlersammlung {

	private final Long grundnetzKanteID;
	@Getter
	private final Geometry grundnetzKanteGeometry;
	private final List<Long> projizierteKanteIds;

	@Getter
	private List<MehrdeutigeLinearReferenzierteAttributeException> exceptions;

	public AttributeMergeFehlersammlung(Long grundnetzKanteID, Geometry grundnetzKanteGeometry,
		List<Long> projizierteKanteIds) {
		this.grundnetzKanteID = grundnetzKanteID;
		this.grundnetzKanteGeometry = grundnetzKanteGeometry;
		this.projizierteKanteIds = projizierteKanteIds;

		this.exceptions = new ArrayList<>();
	}

	public void addException(MehrdeutigeLinearReferenzierteAttributeException e) {
		e.setGrundnetzKantenID(this.grundnetzKanteID);
		e.setProjizierteKanteIds(this.projizierteKanteIds);
		e.setGrundnetzKanteGeometry(this.grundnetzKanteGeometry);
		exceptions.add(e);
	}
}
