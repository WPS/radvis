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

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { register } from 'ol/proj/proj4';
import proj4 from 'proj4';
import { FehlerprotokollModule } from 'src/app/fehlerprotokoll/fehlerprotokoll.module';
import { HintergrundAuswahlComponent } from 'src/app/karte/components/hintergrund-auswahl/hintergrund-auswahl.component';
import { HintergrundLayerComponent } from 'src/app/karte/components/hintergrund-layer/hintergrund-layer.component';
import { KarteButtonComponent } from 'src/app/karte/components/karte-button/karte-button.component';
import { KarteMenuItemComponent } from 'src/app/karte/components/karte-menu-item/karte-menu-item.component';
import { LegendeComponent } from 'src/app/karte/components/legende/legende.component';
import { NetzklassenAuswahlComponent } from 'src/app/karte/components/netzklassen-auswahl/netzklassen-auswahl.component';
import { OlMapComponent } from 'src/app/karte/components/ol-map/ol-map.component';
import { OrtsSucheComponent } from 'src/app/karte/components/orts-suche/orts-suche.component';
import { SharedModule } from 'src/app/shared/shared.module';

// Definiere die Projektion von EPSG:25832 und registriere sie bei OpenLayers, damit bestimmte Transformationen funktioniert
proj4.defs('EPSG:25832', '+proj=utm +zone=32 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs');
register(proj4);

@NgModule({
  declarations: [
    OrtsSucheComponent,
    OlMapComponent,
    NetzklassenAuswahlComponent,
    KarteMenuItemComponent,
    HintergrundAuswahlComponent,
    HintergrundLayerComponent,
    KarteButtonComponent,
    LegendeComponent,
  ],
  imports: [CommonModule, SharedModule, FehlerprotokollModule],
  exports: [OlMapComponent, KarteButtonComponent, KarteMenuItemComponent],
})
export class KarteModule {}
