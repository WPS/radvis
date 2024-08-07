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

package de.wps.radvis.backend.manuellerimport.attributeimport.schnittstelle.view;

import java.util.List;

import de.wps.radvis.backend.manuellerimport.attributeimport.domain.entity.AttributeImportSession;
import de.wps.radvis.backend.manuellerimport.common.domain.valueobject.AttributeImportFormat;
import de.wps.radvis.backend.manuellerimport.common.schnittstelle.view.AbstractOrganisationImportSessionView;
import lombok.Getter;

@Getter
public class AttributeImportSessionView extends AbstractOrganisationImportSessionView {
	private final List<String> attribute;

	private final Long anzahlKantenMitUneindeutigerAttributzuordnung;

	private final AttributeImportFormat attributeImportFormat;

	public AttributeImportSessionView(AttributeImportSession attributeImportSession) {
		super(attributeImportSession, attributeImportSession.getOrganisation());
		this.attribute = attributeImportSession.getAttribute();
		this.anzahlKantenMitUneindeutigerAttributzuordnung = attributeImportSession
			.getAnzahlKantenMitUneindeutigerAttributzuordnung();
		this.attributeImportFormat = attributeImportSession.getAttributeImportFormat();
	}
}
