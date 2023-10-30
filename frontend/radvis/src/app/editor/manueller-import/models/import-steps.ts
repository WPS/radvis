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

import { ImportAttributeAbbildungBearbeitenComponent } from 'src/app/editor/manueller-import/components/import-attribute-abbildung-bearbeiten/import-attribute-abbildung-bearbeiten.component';
import { ImportAttributeAbschliessenComponent } from 'src/app/editor/manueller-import/components/import-attribute-abschliessen/import-attribute-abschliessen.component';
import { ImportAttributeParameterEingebenComponent } from 'src/app/editor/manueller-import/components/import-attribute-parameter-eingeben/import-attribute-parameter-eingeben.component';
import { ImportAutomatischeAbbildungComponent } from 'src/app/editor/manueller-import/components/import-automatische-abbildung/import-automatische-abbildung.component';
import { ImportDateiHochladenComponent } from 'src/app/editor/manueller-import/components/import-datei-hochladen/import-datei-hochladen.component';
import { ImportNetzklasseAbbildungBearbeitenComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-abbildung-bearbeiten/import-netzklasse-abbildung-bearbeiten.component';
import { ImportNetzklasseAbschliessenComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-abschliessen/import-netzklasse-abschliessen.component';
import { ImportNetzklasseParameterEingebenComponent } from 'src/app/editor/manueller-import/components/import-netzklasse-parameter-eingeben/import-netzklasse-parameter-eingeben.component';
import { ImportStep } from 'src/app/editor/manueller-import/models/import-step';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';
import { SessionExistsGuard } from 'src/app/editor/manueller-import/services/session-exists-guard.service';
import { NetzklassenSessionExistsGuard } from 'src/app/editor/manueller-import/services/netzklassen-session-exists-guard.service';
import { AttributeSessionExistsGuard } from 'src/app/editor/manueller-import/services/attribute-session-exists-guard.service';

export const IMPORT_STEPS: ImportStep[] = [
  {
    bezeichnung: 'Datei hochladen',
    route: {
      link: ManuellerImportRoutingService.IMPORT_DATEI_UPLOAD_ROUTE,
      component: ImportDateiHochladenComponent,
    },
  },
  {
    bezeichnung: 'Parameter Eingeben',
    route: {
      link: ManuellerImportRoutingService.IMPORT_NETZKLASSE_PARAMETER_EINGEBEN_ROUTE,
      component: ImportNetzklasseParameterEingebenComponent,
    },
    abweichendeAttributImportRoute: {
      link: ManuellerImportRoutingService.IMPORT_ATTRIBUTE_PARAMETER_EINGEBEN_ROUTE,
      component: ImportAttributeParameterEingebenComponent,
    },
  },
  {
    bezeichnung: 'Automatische Abbildung',
    route: {
      link: ManuellerImportRoutingService.IMPORT_AUTOMATISCHE_ABBILDUNG_ROUTE,
      component: ImportAutomatischeAbbildungComponent,
      guard: SessionExistsGuard,
    },
  },
  {
    bezeichnung: 'Abbildung bearbeiten',
    route: {
      link: ManuellerImportRoutingService.IMPORT_NETZKLASSE_KORREKTUR_ROUTE,
      component: ImportNetzklasseAbbildungBearbeitenComponent,
      guard: NetzklassenSessionExistsGuard,
    },
    abweichendeAttributImportRoute: {
      link: ManuellerImportRoutingService.IMPORT_ATTRIBUTE_KORREKTUR_ROUTE,
      component: ImportAttributeAbbildungBearbeitenComponent,
      guard: AttributeSessionExistsGuard,
    },
  },
  {
    bezeichnung: 'Import abschließen',
    route: {
      link: ManuellerImportRoutingService.IMPORT_NETZKLASSE_ABSCHLUSS_ROUTE,
      component: ImportNetzklasseAbschliessenComponent,
      guard: NetzklassenSessionExistsGuard,
    },
    abweichendeAttributImportRoute: {
      link: ManuellerImportRoutingService.IMPORT_ATTRIBUTE_ABSCHLUSS_ROUTE,
      component: ImportAttributeAbschliessenComponent,
      guard: AttributeSessionExistsGuard,
    },
  },
];
