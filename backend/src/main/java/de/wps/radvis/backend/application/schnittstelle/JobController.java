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

package de.wps.radvis.backend.application.schnittstelle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import de.wps.radvis.backend.common.domain.Job;

@Controller
public class JobController {
	private List<Job> jobs;

	public JobController(@Autowired List<Job> jobs) {
		this.jobs = jobs;
	}

	@RequestMapping(path = "/control", produces = MediaType.TEXT_HTML_VALUE, method = RequestMethod.GET)
	@ResponseBody
	public String getWelcomePage() throws IOException {
		Resource resource = new ClassPathResource("job-control.html");
		BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
		String htmlString = reader.lines().reduce(String::concat).orElse("");

		Stream<String> allLinks = jobs.stream()
			.sorted((j1, j2) -> j1.getName().compareTo(j2.getName()))
			.map(j -> {
				try {
					return String.format("<li><a href=\"/jobs/run/%s\">%s</a></li>",
						new URI(null, null, j.getName(), null).toASCIIString(), j.getName());
				} catch (URISyntaxException e) {
					return "";
				}
			});

		return htmlString.replace("$jobs$", allLinks.reduce(String::concat).orElse(""));
	}
}
