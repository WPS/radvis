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

package de.wps.radvis.backend.abfrage.netzausschnitt.schnittstelle;

import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.common.schnittstelle.view.AttributeView;
import de.wps.radvis.backend.netz.domain.service.NetzService;
import de.wps.radvis.backend.netz.schnittstelle.NetzToFeatureDetailsConverter;
import lombok.NonNull;

@RestController
@RequestMapping("/api/netz-feature-details")
public class NetzFeatureDetailsController {

	private final NetzToFeatureDetailsConverter netzToFeatureDetailsConverter;
	private final NetzService netzService;

	public NetzFeatureDetailsController(
		@NonNull NetzToFeatureDetailsConverter netzToFeatureDetailsConverter, @NonNull NetzService netzService) {
		super();
		this.netzToFeatureDetailsConverter = netzToFeatureDetailsConverter;
		this.netzService = netzService;
	}

	/**
	 * @deprecated Wird entfernt wenn RadNETZ Matching entfernt wird
	 */
	@GetMapping("kante-feature-details/{id}")
	@Deprecated
	public List<AttributeView> getKantenFeatureDetailsByKantenId(@PathVariable("id") Long id,
		@RequestParam("position") Double[] coordinatesParam) {
		return netzToFeatureDetailsConverter.convertKanteToFeatureDetails(
			netzService.getKante(id),
			new Coordinate(coordinatesParam[0], coordinatesParam[1]));
	}
}
