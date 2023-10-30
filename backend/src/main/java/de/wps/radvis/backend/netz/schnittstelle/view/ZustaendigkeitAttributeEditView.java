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

package de.wps.radvis.backend.netz.schnittstelle.view;

import de.wps.radvis.backend.common.domain.valueObject.LinearReferenzierterAbschnitt;
import de.wps.radvis.backend.netz.domain.entity.ZustaendigkeitAttribute;
import de.wps.radvis.backend.netz.domain.valueObject.VereinbarungsKennung;
import de.wps.radvis.backend.organisation.schnittstelle.VerwaltungseinheitView;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ZustaendigkeitAttributeEditView {

	private VerwaltungseinheitView baulastTraeger;

	private VerwaltungseinheitView unterhaltsZustaendiger;

	private VerwaltungseinheitView erhaltsZustaendiger;

	private VereinbarungsKennung vereinbarungsKennung;

	protected LinearReferenzierterAbschnitt linearReferenzierterAbschnitt;

	public ZustaendigkeitAttributeEditView(ZustaendigkeitAttribute zustaendigkeitAttribute) {

		this.vereinbarungsKennung = zustaendigkeitAttribute.getVereinbarungsKennung().orElse(null);

		zustaendigkeitAttribute.getBaulastTraeger().ifPresent(b -> baulastTraeger = new VerwaltungseinheitView(b));
		zustaendigkeitAttribute.getUnterhaltsZustaendiger()
			.ifPresent(organisation -> unterhaltsZustaendiger = new VerwaltungseinheitView(organisation));
		zustaendigkeitAttribute.getErhaltsZustaendiger()
			.ifPresent(organisation -> erhaltsZustaendiger = new VerwaltungseinheitView(organisation));

		this.linearReferenzierterAbschnitt = zustaendigkeitAttribute.getLinearReferenzierterAbschnitt();
	}
}
