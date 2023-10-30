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

package de.wps.radvis.backend.auditing.schnittstelle;

import java.io.Serializable;
import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

import de.wps.radvis.backend.auditing.domain.AuditingContext;
import de.wps.radvis.backend.benutzer.domain.entity.Benutzer;
import de.wps.radvis.backend.common.domain.entity.JobExecutionDescription;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@RevisionEntity(AdditionalRevInfoApplier.class)
@ToString
public class RevInfo implements Serializable {
	private static final long serialVersionUID = 8530213963961662300L;

	@Id
	// TODO why?!
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequence-generator")
	@GenericGenerator(name = "sequence-generator", strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator", parameters = {
		@Parameter(name = "sequence_name", value = "hibernate_sequence"),
		@Parameter(name = "optimizer", value = "pooled"),
		@Parameter(name = "initial_value", value = "1"),
		@Parameter(name = "increment_size", value = "100")
	})
	@RevisionNumber
	@Getter
	@Setter
	private Long id;

	@Getter
	@Setter
	@RevisionTimestamp
	private long timestamp;

	@Getter
	@Setter
	@Enumerated(EnumType.STRING)
	private AuditingContext auditingContext;

	@OneToOne
	@Getter
	@Setter
	private JobExecutionDescription jobExecutionDescription;

	// TODO müsste das nicht ManyToOne sein?!?
	@OneToOne
	@Getter
	@Setter
	private Benutzer benutzer;

	public RevInfo() {
	}

	@Transient
	public Date getRevisionDate() {
		return new Date(this.timestamp);
	}

	/**
	 * kopiert von {@link org.hibernate.envers.DefaultRevisionEntity}
	 */
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		} else if (!(o instanceof RevInfo)) {
			return false;
		} else {
			RevInfo that = (RevInfo) o;
			return this.id.equals(that.id) && this.timestamp == that.timestamp;
		}
	}

	/**
	 * kopiert von {@link org.hibernate.envers.DefaultRevisionEntity}
	 */
	public int hashCode() {
		int result = (int) this.id.longValue();
		result = 31 * result + (int) (this.timestamp ^ this.timestamp >>> 32);
		return result;
	}
}
