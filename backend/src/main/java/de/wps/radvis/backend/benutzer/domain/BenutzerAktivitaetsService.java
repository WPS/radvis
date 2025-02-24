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

package de.wps.radvis.backend.benutzer.domain;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;

import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.benutzer.domain.repository.BenutzerRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

@Transactional
@Slf4j
public class BenutzerAktivitaetsService {

	private final BenutzerRepository benutzerRepository;

	private final static Map<Long, Lock> benutzerIdToLockMap = new ConcurrentHashMap<>();

	public BenutzerAktivitaetsService(BenutzerRepository benutzerRepository) {
		this.benutzerRepository = benutzerRepository;
	}

	@EventListener
	public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
		Authentication authentication = event.getAuthentication();
		if (authentication instanceof BenutzerHolder) {
			Benutzer benutzer = ((BenutzerHolder) authentication).getBenutzer();
			if (benutzer == null || benutzer.getId() == null) {
				return;
			}

			benutzerIdToLockMap.putIfAbsent(benutzer.getId(), new ReentrantLock());

			Lock lock = benutzerIdToLockMap.get(benutzer.getId());
			if (lock.tryLock()) {
				// Nutzer nochmal aus DB holen. Es kann nämlich bei schnell eingehenden Anfragen sein, dass zwischen
				// initialem Holen vom Nutzer und dieser Stelle hier schon ein anderer Request den Nutzer veändert hst.
				// In so einem Fall würde eine Optimistic-Locking-Exception fliegen.
				Optional<Benutzer> benutzerOpt = benutzerRepository
					.findById(((BenutzerHolder) authentication).getBenutzer().getId());
				if (benutzerOpt.isEmpty()) {
					return;
				}

				benutzer = benutzerOpt.get();
				log.info("Letzt aktivität: {}", benutzer.getLetzteAktivitaet());

				if (benutzer.getLetzteAktivitaet().equals(LocalDate.now())) {
					return;
				}

				try {
					log.info("Aktualisiere letzte Aktivität von Benutzer mit Id {}", benutzer.getId());
					benutzer.aktualisiereLetzteAktivitaet();
					benutzerRepository.save(benutzer);
				} finally {
					lock.unlock();
				}
			}
		}
	}
}
