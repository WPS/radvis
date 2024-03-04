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
import { Routes } from '@angular/router';
import { ImportAttributeAbbildungBearbeitenComponent } from 'src/app/import/attribute/components/import-attribute-abbildung-bearbeiten/import-attribute-abbildung-bearbeiten.component';
import { ImportAttributeAbschliessenComponent } from 'src/app/import/attribute/components/import-attribute-abschliessen/import-attribute-abschliessen.component';
import { ImportAttributeAutomatischeAbbildungComponent } from 'src/app/import/attribute/components/import-attribute-automatische-abbildung/import-attribute-automatische-abbildung.component';
import { ImportAttributeDateiHochladenComponent } from 'src/app/import/attribute/components/import-attribute-datei-hochladen/import-attribute-datei-hochladen.component';
import { ImportAttributeParameterEingebenComponent } from 'src/app/import/attribute/components/import-attribute-parameter-eingeben/import-attribute-parameter-eingeben.component';
import { AttributeRoutingService } from 'src/app/import/attribute/services/attribute-routing.service';
import { attributeSessionExistsGuard } from 'src/app/import/attribute/services/attribute-session-exists.guard';

export class AttributeImportRoutes {
  public static CHILDROUTES: Routes = [
    {
      path: '',
      pathMatch: 'full',
      redirectTo: AttributeRoutingService.DATEI_UPLOAD_ROUTE,
    },
    {
      path: AttributeRoutingService.DATEI_UPLOAD_ROUTE,
      component: ImportAttributeDateiHochladenComponent,
    },
    {
      path: AttributeRoutingService.PARAMETER_EINGEBEN_ROUTE,
      component: ImportAttributeParameterEingebenComponent,
    },
    {
      path: AttributeRoutingService.AUTOMATISCHE_ABBILDUNG_ROUTE,
      component: ImportAttributeAutomatischeAbbildungComponent,
      canActivate: [attributeSessionExistsGuard],
    },
    {
      path: AttributeRoutingService.KORREKTUR_ROUTE,
      component: ImportAttributeAbbildungBearbeitenComponent,
      canActivate: [attributeSessionExistsGuard],
    },
    {
      path: AttributeRoutingService.ABSCHLUSS_ROUTE,
      component: ImportAttributeAbschliessenComponent,
      canActivate: [attributeSessionExistsGuard],
    },
  ];
}
