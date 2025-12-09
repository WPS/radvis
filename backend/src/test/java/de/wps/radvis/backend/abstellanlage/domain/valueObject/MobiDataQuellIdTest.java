package de.wps.radvis.backend.abstellanlage.domain.valueObject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.valid4j.errors.RequireViolation;

class MobiDataQuellIdTest {

	@Test
	void sollteKeineNegativenWerteAkzeptieren() {
		assertThatExceptionOfType(RequireViolation.class).isThrownBy(
			() -> MobiDataQuellId.of(-1));

	}

	@Test
	void sollteEqualsUndHashCodeImplementieren() {
		assertThat(MobiDataQuellId.of(1)).isEqualTo(MobiDataQuellId.of(1));
		assertThat(MobiDataQuellId.of(1)).isNotEqualTo(MobiDataQuellId.of(2));

		assertThat(MobiDataQuellId.of(1).hashCode()).isEqualTo(MobiDataQuellId.of(1).hashCode());
	}
}