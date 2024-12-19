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

package de.wps.radvis.backend.netz.domain.entity;

import static org.valid4j.Assertive.require;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.Audited;

import de.wps.radvis.backend.common.domain.RadVisDomainEventPublisher;
import de.wps.radvis.backend.common.domain.entity.VersionierteEntity;
import de.wps.radvis.backend.netz.domain.event.RadNetzZugehoerigkeitChangedEvent;
import de.wps.radvis.backend.netz.domain.valueObject.IstStandard;
import de.wps.radvis.backend.netz.domain.valueObject.Netzklasse;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenName;
import de.wps.radvis.backend.netz.domain.valueObject.StrassenNummer;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@Entity
@Audited
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(callSuper = true)
@Getter
public class KantenAttributGruppe extends VersionierteEntity {

	@Embedded
	@NonNull
	KantenAttribute kantenAttribute;

	@Getter
	@ElementCollection(fetch = FetchType.EAGER)
	@Column(name = "netzklasse")
	@Enumerated(EnumType.STRING)
	@NonNull
	private Set<Netzklasse> netzklassen;

	@Getter
	@ElementCollection
	@Column(name = "standard")
	@Enumerated(EnumType.STRING)
	@NonNull
	private Set<IstStandard> istStandards;

	public KantenAttributGruppe(KantenAttribute kantenAttribute, Set<Netzklasse> netzklassen,
		Set<IstStandard> istStandards) {
		this.kantenAttribute = kantenAttribute;
		this.netzklassen = netzklassen;
		this.istStandards = istStandards;
	}

	@Builder(builderMethodName = "privateBuilder")
	private KantenAttributGruppe(Long id, Long version, KantenAttribute kantenAttribute, Set<Netzklasse> netzklassen,
		Set<IstStandard> istStandards) {
		super(id, version);
		this.kantenAttribute = kantenAttribute;
		this.netzklassen = netzklassen;
		this.istStandards = istStandards;
	}

	public static KantenAttributGruppeBuilder builder() {
		return privateBuilder().kantenAttribute(KantenAttribute.builder().build())
			.netzklassen(new HashSet<>()).istStandards(new HashSet<>());
	}

	public void update(@NonNull Set<Netzklasse> netzklassen, @NonNull Set<IstStandard> istStandards,
		@NonNull KantenAttribute kantenAttribute) {
		require(istStandardsAllowedForNetzklassen(netzklassen, istStandards),
			"Falls ein RadNETZ-IstStandard gesetzt ist, muss die Kante zu einer RadNETZ-Netzklasse gehören.");

		final boolean zuvorRadnetzZugehoerig = entsprechenNetzklassenRadNETZ(this.netzklassen);

		this.kantenAttribute = kantenAttribute;
		this.netzklassen = netzklassen;
		this.istStandards = istStandards;

		final boolean danachRadnetzZugehoerig = entsprechenNetzklassenRadNETZ(this.netzklassen);

		if (this.id != null && zuvorRadnetzZugehoerig != danachRadnetzZugehoerig) {
			RadVisDomainEventPublisher.publish(new RadNetzZugehoerigkeitChangedEvent(this.id, danachRadnetzZugehoerig));
		}
	}

	/**
	 * Diese Methode existiert nur aus performance gründen
	 */
	void updateNetzklassen(@NonNull Set<Netzklasse> netzklassen) {
		final boolean zuvorRadnetzZugehoerig = entsprechenNetzklassenRadNETZ(this.netzklassen);
		final boolean danachRadnetzZugehoerig = entsprechenNetzklassenRadNETZ(netzklassen);
		final boolean wurdeRadnetzEntfernt = zuvorRadnetzZugehoerig && !danachRadnetzZugehoerig;

		if (wurdeRadnetzEntfernt) {
			require(istStandardsAllowedForNetzklassen(netzklassen, istStandards),
				"Ist-Standards sind für gewählte Netzklassen nicht erlaubt");
		}

		this.netzklassen = netzklassen;

		if (zuvorRadnetzZugehoerig != danachRadnetzZugehoerig) {
			RadVisDomainEventPublisher.publish(new RadNetzZugehoerigkeitChangedEvent(this.id, danachRadnetzZugehoerig));
		}
	}

	public static boolean istStandardsAllowedForNetzklassen(Set<Netzklasse> netzklassen,
		Set<IstStandard> istStandards) {
		if (istStandards.contains(IstStandard.STARTSTANDARD_RADNETZ)
			|| istStandards.contains(IstStandard.ZIELSTANDARD_RADNETZ)) {
			return entsprechenNetzklassenRadNETZ(netzklassen);
		}
		return true;
	}

	private static boolean entsprechenNetzklassenRadNETZ(Set<Netzklasse> netzklassen) {
		return Netzklasse.RADNETZ_NETZKLASSEN.stream().anyMatch(netzklassen::contains);
	}

	public void reset() {
		this.kantenAttribute = KantenAttribute.builder().build();
		this.netzklassen = new HashSet<>();
		this.istStandards = new HashSet<>();
	}

	public boolean isRadNETZ() {
		return entsprechenNetzklassenRadNETZ(netzklassen);
	}

	public boolean sindAttributeGleich(KantenAttributGruppe other) {
		return getNetzklassen().equals(other.getNetzklassen()) &&
			getIstStandards().equals(other.getIstStandards()) &&
			getKantenAttribute().equals(other.getKantenAttribute());
	}

	void updateStrassenInfo(StrassenName neuerStrassenName, StrassenNummer neueStrassenNummer) {
		kantenAttribute = kantenAttribute.toBuilder().strassenName(neuerStrassenName).strassenNummer(neueStrassenNummer)
			.build();
	}
}
