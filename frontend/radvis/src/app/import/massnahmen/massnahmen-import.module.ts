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

import { NgModule } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { ImportSharedModule } from 'src/app/import/import-shared/import-shared.module';
import { ImportMassnahmenAttributeAuswaehlenComponent } from 'src/app/import/massnahmen/components/import-massnahmen-attribute-auswaehlen/import-massnahmen-attribute-auswaehlen.component';
import { ImportMassnahmenAttributfehlerUeberpruefenComponent } from 'src/app/import/massnahmen/components/import-massnahmen-attributfehler-ueberpruefen/import-massnahmen-attributfehler-ueberpruefen.component';
import { ImportMassnahmenDateiHochladenComponent } from 'src/app/import/massnahmen/components/import-massnahmen-datei-hochladen/import-massnahmen-datei-hochladen.component';
import { ImportMassnahmenFehlerprotokollHerunterladenComponent } from 'src/app/import/massnahmen/components/import-massnahmen-fehlerprotokoll-herunterladen/import-massnahmen-fehlerprotokoll-herunterladen.component';
import { ImportMassnahmenImportUeberpruefenLayerComponent } from 'src/app/import/massnahmen/components/import-massnahmen-import-ueberpruefen-layer/import-massnahmen-import-ueberpruefen-layer.component';
import { ImportMassnahmenImportUeberpruefenComponent } from 'src/app/import/massnahmen/components/import-massnahmen-import-ueberpruefen/import-massnahmen-import-ueberpruefen.component';
import { ImportMassnahmenToolComponent } from 'src/app/import/massnahmen/components/import-massnahmen-tool/import-massnahmen-tool.component';
import { MassnahmenImportZuordnungStatus } from 'src/app/import/massnahmen/models/massnahmen-import-zuordnung-status';
import { KarteModule } from 'src/app/karte/karte.module';
import { SharedModule } from 'src/app/shared/shared.module';
import { ViewerSharedModule } from 'src/app/viewer/viewer-shared/viewer-shared.module';

@NgModule({
  declarations: [
    ImportMassnahmenToolComponent,
    ImportMassnahmenDateiHochladenComponent,
    ImportMassnahmenAttributeAuswaehlenComponent,
    ImportMassnahmenAttributfehlerUeberpruefenComponent,
    ImportMassnahmenImportUeberpruefenComponent,
    ImportMassnahmenImportUeberpruefenLayerComponent,
    ImportMassnahmenFehlerprotokollHerunterladenComponent,
  ],
  imports: [SharedModule, ImportSharedModule, KarteModule, ViewerSharedModule],
  exports: [ImportMassnahmenImportUeberpruefenLayerComponent],
})
export class MassnahmenImportModule {
  constructor(iconRegistry: MatIconRegistry, sanitizer: DomSanitizer) {
    [
      MassnahmenImportZuordnungStatus.NEU,
      MassnahmenImportZuordnungStatus.ZUGEORDNET,
      MassnahmenImportZuordnungStatus.GELOESCHT,
      MassnahmenImportZuordnungStatus.FEHLERHAFT,
    ].forEach(status => {
      iconRegistry.addSvgIcon(
        `massnahmeZuordnung${status.charAt(0) + status.slice(1).toLowerCase()}`,
        sanitizer.bypassSecurityTrustResourceUrl(`./assets/icon-massnahme-zuordnung-${status.toLowerCase()}.svg`)
      );
    });
  }
}
