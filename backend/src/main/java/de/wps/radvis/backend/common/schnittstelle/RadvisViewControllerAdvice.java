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

package de.wps.radvis.backend.common.schnittstelle;

import static org.valid4j.Assertive.require;

import java.util.Arrays;

import jakarta.servlet.http.HttpServletRequest;

import org.locationtech.jts.geom.Envelope;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = { RadvisViewController.class })
public class RadvisViewControllerAdvice {

	@ModelAttribute("sichtbereich")
	public Envelope sichtbereich(HttpServletRequest request) {
		String clientViewport = request.getParameter("view");
		if (clientViewport != null) {
			String[] strCoords = clientViewport.split(",");
			require(strCoords.length == 4, "Der view-Parameter muss 4 kommaseparierte Ordinaten enthalten");
			double[] coords = Arrays.stream(strCoords).mapToDouble(Double::parseDouble).toArray();
			return new Envelope(coords[0], coords[2], coords[1], coords[3]);
		}
		return new Envelope();
	}

}
