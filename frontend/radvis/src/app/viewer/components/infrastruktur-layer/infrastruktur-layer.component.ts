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

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Observable } from 'rxjs';
import { ABSTELLANLAGEN } from 'src/app/viewer/abstellanlage/models/abstellanlage.infrastruktur';
import { ANPASSUNGSWUNSCH } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch.infrastruktur';
import { BARRIEREN } from 'src/app/viewer/barriere/models/barriere.infrastruktur';
import { FAHRRADROUTE } from 'src/app/viewer/fahrradroute/models/fahrradroute.infrastruktur';
import { FURTEN_KREUZUNGEN } from 'src/app/viewer/furten-kreuzungen/models/furten-kreuzungen.infrastruktur';
import { LEIHSTATIONEN } from 'src/app/viewer/leihstation/models/leihstation.infrastruktur';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { SERVICESTATIONEN } from 'src/app/viewer/servicestation/models/servicestation.infrastruktur';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { WEGWEISENDE_BESCHILDERUNG } from 'src/app/viewer/wegweisende-beschilderung/models/wegweisende-beschilderung.infrastruktur';
import { FAHRRADZAEHLSTELLE } from 'src/app/viewer/fahrradzaehlstelle/models/fahrradzaehlstelle.infrastruktur';

@Component({
  selector: 'rad-infrastruktur-layer',
  templateUrl: './infrastruktur-layer.component.html',
  styleUrls: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InfrastrukturLayerComponent {
  selektierteInfrastrukturen$: Observable<Infrastruktur[]>;
  readonly FAHRRADROUTE = FAHRRADROUTE;
  readonly MASSNAHMEN = MASSNAHMEN;
  readonly ANPASSUNGSWUNSCH = ANPASSUNGSWUNSCH;
  readonly BARRIEREN = BARRIEREN;
  readonly FURTEN_KREUZUNGEN = FURTEN_KREUZUNGEN;
  readonly WEGWEISENDE_BESCHILDERUNG = WEGWEISENDE_BESCHILDERUNG;
  readonly ABSTELLANLAGEN = ABSTELLANLAGEN;
  readonly SERVICESTATIONEN = SERVICESTATIONEN;
  readonly LEIHSTATIONEN = LEIHSTATIONEN;
  readonly FAHRRADZAEHLSTELLE = FAHRRADZAEHLSTELLE;

  constructor(private infrastrukturenSelektionService: InfrastrukturenSelektionService) {
    this.selektierteInfrastrukturen$ = this.infrastrukturenSelektionService.selektierteInfrastrukturen$;
  }
}
