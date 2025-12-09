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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
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
import de.wps.radvis.backend.common.domain.entity.AbstractJob;

@Controller
public class JobController {
	private List<Job> jobs;

	public JobController(@Autowired List<Job> jobs) {
		this.jobs = jobs;
	}

	@RequestMapping(path = "/control", produces = MediaType.TEXT_HTML_VALUE, method = RequestMethod.GET)
	@ResponseBody
	public String getControlPage() throws IOException {
		Resource resource = new ClassPathResource("job-control.html");
		BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()));
		String htmlString = reader.lines().reduce(String::concat).orElse("");

		/*
		Gruppe vom Regex enthält folgende Werte:
		
		Name   -> Prefix
		abcDef -> abc
		abc    -> abc
		AbcDef -> Abc
		Abc    -> Abc
		ABCdef -> ABC
		ABC    -> ABC
		 */
		Pattern jobPrefixPattern = Pattern.compile("^([a-z]+|[A-Z][a-z]+|[A-Z]+).*");

		List<String> jobPrefixe = jobs.stream()
			.map(job -> {
				String name = job.getName();
				Matcher m = jobPrefixPattern.matcher(name);

				if (!m.matches()) {
					return "";
				}

				String originalPrefix = m.group(1);
				String prefix = originalPrefix.toLowerCase();

				// Sonderbehandlung einiger schwieriger und nicht eindeutiger prefixe
				if (prefix.startsWith("massnahme")) {
					return "massnahme";
				} else if (prefix.startsWith("landesradfernweg")) {
					return "landesradfernweg";
				}

				return prefix;
			})
			.distinct()
			.sorted()
			.collect(Collectors.toList());

		Stream<String> groupedJobLists = jobPrefixe
			.stream()
			.filter(prefix -> {
				// Entfernt längere Prefixe mit gleichwertigen kürzeren. Beispielsweise wird "dlmr" (von "DLMReimport...")
				// entfernt, da es bereits einen anderen kürzeren "dlm" Prefix gibt (von "DlmPbfErstellung...").
				return !jobPrefixe.stream()
					.anyMatch(otherPrefix -> !prefix.equals(otherPrefix) && prefix.startsWith(otherPrefix));
			})
			.map(prefix -> {
				String jobListItems = jobs.stream()
					.filter(job -> job.getName().toLowerCase().startsWith(prefix))
					.map(j -> {
						try {
							boolean isRepeatable = true;
							if (j instanceof AbstractJob abstractJob) {
								isRepeatable = abstractJob.isRepeatable();
							}
							String descriptionHtml = j.getDescription() == null ? "<table><tr><td>Keine Details vorhanden</td></tr></table>" : "<br>"+j.getDescription().toHtml(isRepeatable);
							return String.format("<li><a href=\"/jobs/run/%s\">%s</a>%s</li>",
								new URI(null, null, j.getName(), null).toASCIIString(), j.getName(), descriptionHtml);
						} catch (URISyntaxException e) {
							return "";
						}
					})
					.collect(Collectors.joining("\n"));
				return "<ul>" + jobListItems + "\n</ul>\n";
			});

		return htmlString.replace("$jobs$", groupedJobLists.reduce(String::concat).orElse(""));
	}
}
