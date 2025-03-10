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

package de.wps.radvis.backend.karte.schnittstelle;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.common.domain.annotation.WithFehlercode;
import de.wps.radvis.backend.common.domain.valueObject.Fehlercode;
import de.wps.radvis.backend.karte.domain.HintergrundKarteConfigurationProperties;
import de.wps.radvis.backend.karte.domain.HintergrundKartenService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/hintergrundkarte")
@WithFehlercode(Fehlercode.HINTERGRUNDKARTEN)
public class HintergrundKartenController {

	private final HintergrundKartenService hintergrundKartenService;
	private final Map<String, HintergrundKarteConfigurationProperties> hintergrundKarten;
	private final String defaultHintergrundKarte;

	public HintergrundKartenController(
		@NonNull HintergrundKartenService hintergrundKartenService,
		@NonNull Map<String, HintergrundKarteConfigurationProperties> hintergrundKarten,
		@NonNull String defaultHintergrundKarte) {
		this.defaultHintergrundKarte = defaultHintergrundKarte;
		this.hintergrundKarten = hintergrundKarten;
		this.hintergrundKartenService = hintergrundKartenService;
	}

	@GetMapping(value = "/xyz/{key}/{x}/{y}/{z}", produces = MediaType.IMAGE_JPEG_VALUE)
	public Mono<byte[]> getTile(@PathVariable("key") String key, @PathVariable("x") int x,
		@PathVariable("y") int y, @PathVariable("z") int z, final HttpServletResponse response) {
		response.addHeader("Cache-Control", "max-age=31536000, must-revalidate, no-transform");
		Map<String, Integer> params = new HashMap<>();
		params.put("z", z);
		params.put("x", x);
		params.put("y", y);

		return hintergrundKartenService.getTileXYZ(key, params).orElseThrow();
	}

	@GetMapping(value = "/wms/{key}", produces = MediaType.IMAGE_JPEG_VALUE)
	public Mono<byte[]> getWMSTile(@PathVariable("key") String key, @RequestParam("BBOX") String bbox,
		@RequestParam("WIDTH") String width,
		@RequestParam("HEIGHT") String height, @RequestParam("CRS") String crs, final HttpServletResponse response) {
		response.addHeader("Cache-Control", "max-age=31536000, must-revalidate, no-transform");
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("bbox", bbox);
		params.add("width", width);
		params.add("height", height);
		params.add("crs", crs);

		return hintergrundKartenService.getTileWMS(key, params).orElseThrow();
	}

	@GetMapping("layers")
	public List<HintergrundKarteView> getLayers() {
		return hintergrundKarten.entrySet().stream().map((entry) -> {
			return new HintergrundKarteView(entry.getKey(), entry.getValue(),
				defaultHintergrundKarte.equals(entry.getKey()));
		}).collect(Collectors.toList());
	}
}
