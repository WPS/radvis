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

package de.wps.radvis.backend.karte.domain.entity;

import java.net.MalformedURLException;
import java.net.URL;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode
public class HintergrundKarte {

	@Getter
	private String domain;
	@Getter
	private String path;
	@Getter
	private String query;

	public HintergrundKarte(String url) {
		try {
			URL urlObj = new URL(url);
			this.domain = urlObj.getProtocol() + "://" + urlObj.getHost();
			this.path = urlObj.getPath();
			this.query = urlObj.getQuery();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}
}
