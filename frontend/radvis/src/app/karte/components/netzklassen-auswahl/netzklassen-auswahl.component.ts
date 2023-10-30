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

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { Observable } from 'rxjs';
import { NetzklassenAuswahlService } from 'src/app/karte/services/netzklassen-auswahl.service';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';

@Component({
  selector: 'rad-netzklassen-auswahl',
  templateUrl: './netzklassen-auswahl.component.html',
  styleUrls: ['./netzklassen-auswahl.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NetzklassenAuswahlComponent {
  @Input()
  public zoom: number = Number.MAX_VALUE;

  public selectedNetzklassen$: Observable<Netzklassefilter[]>;
  public allNetzklassen: Netzklassefilter[] = Netzklassefilter.getAll();

  constructor(private netzklassenAuswahlService: NetzklassenAuswahlService) {
    this.selectedNetzklassen$ = this.netzklassenAuswahlService.currentAuswahl$;
  }

  onNetzklassenAuswahlChange(netzklasse: Netzklassefilter, checked: boolean): void {
    if (checked) {
      this.netzklassenAuswahlService.selectNetzklasse(netzklasse);
    } else {
      this.netzklassenAuswahlService.deselectNetzklasse(netzklasse);
    }
  }
}
