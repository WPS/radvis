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

package de.wps.radvis.backend.wegweisendeBeschilderung.schnittstelle;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.wps.radvis.backend.common.domain.JobExecutionDescriptionRepository;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import de.wps.radvis.backend.wegweisendeBeschilderung.domain.repository.WegweisendeBeschilderungRepository;
import de.wps.radvis.backend.wegweisendeBeschilderung.schnittstelle.view.WegweisendeBeschilderungListenView;
import de.wps.radvis.backend.wegweisendeBeschilderung.schnittstelle.view.WegweiserImportprotokollView;
import jakarta.persistence.EntityNotFoundException;

@RestController
@RequestMapping("/api/wegweisendebeschilderung")
public class WegweisendeBeschilderungController {

	private final WegweisendeBeschilderungRepository wegweisendeBeschilderungRepository;
	private final JobExecutionDescriptionRepository jobExecutionDescriptionRepository;

	public WegweisendeBeschilderungController(
		WegweisendeBeschilderungRepository wegweisendeBeschilderungRepository,
		JobExecutionDescriptionRepository jobExecutionDescriptionRepository) {
		this.wegweisendeBeschilderungRepository = wegweisendeBeschilderungRepository;
		this.jobExecutionDescriptionRepository = jobExecutionDescriptionRepository;
	}

	@GetMapping("{id}")
	public WegweisendeBeschilderungListenView getWegweisendeBeschilderung(@PathVariable("id") Long id) {
		return wegweisendeBeschilderungRepository.findById(id)
			.map(WegweisendeBeschilderungListenView::new)
			.orElseThrow(EntityNotFoundException::new);
	}

	@GetMapping("/list")
	public List<WegweisendeBeschilderungListenView> getAlleWegweisendeBeschilderung() {
		return wegweisendeBeschilderungRepository.findAll().stream()
			.map(WegweisendeBeschilderungListenView::new)
			.collect(Collectors.toList());
	}

	@GetMapping("importprotokoll/{id}")
	public WegweiserImportprotokollView getImportprotokoll(@PathVariable Long id) {
		JobExecutionDescription jobExecutionDescription = jobExecutionDescriptionRepository.findById(id)
			.orElseThrow(EntityNotFoundException::new);
		return new WegweiserImportprotokollView(jobExecutionDescription);

	}
}
