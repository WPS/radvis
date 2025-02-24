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

package de.wps.radvis.backend.netz.domain.entity.provider;

import java.util.List;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttributGruppe;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute.ZustaendigkeitAttributeBuilder;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.organisation.domain.entity.Gebietskoerperschaft;

public class ZustaendigkeitAttributGruppeTestDataProvider {
	public static ZustaendigkeitAttributGruppe.ZustaendigkeitAttributGruppeBuilder withLeereGrundnetzAttribute() {
		return ZustaendigkeitAttributGruppe.builder();
	}

	public static ZustaendigkeitAttribute.ZustaendigkeitAttributeBuilder withLineareReferenz(double von, double bis) {
		return ZustaendigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(von, bis));
	}

	public static ZustaendigkeitAttribute.ZustaendigkeitAttributeBuilder createWithValues(
		Gebietskoerperschaft defaultOrganisation) {
		return createWithValues(defaultOrganisation, defaultOrganisation, defaultOrganisation);
	}

	public static ZustaendigkeitAttribute.ZustaendigkeitAttributeBuilder createWithValues(
		Gebietskoerperschaft baulastTraeger, Gebietskoerperschaft erhaltsZustaendiger,
		Gebietskoerperschaft unterhaltsZustaendiger) {
		return ZustaendigkeitAttribute.builder()
			.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1))
			.vereinbarungsKennung(VereinbarungsKennung.of("Vereinbaru-Kennu"))
			.baulastTraeger(baulastTraeger)
			.erhaltsZustaendiger(erhaltsZustaendiger)
			.unterhaltsZustaendiger(unterhaltsZustaendiger);
	}

	public static ZustaendigkeitAttributGruppe.ZustaendigkeitAttributGruppeBuilder withAttribute(
		ZustaendigkeitAttributeBuilder attribute) {
		return ZustaendigkeitAttributGruppe.builder()
			.zustaendigkeitAttribute(
				List.of(attribute.linearReferenzierterAbschnitt(LinearReferenzierterAbschnitt.of(0, 1)).build()));
	}
}
