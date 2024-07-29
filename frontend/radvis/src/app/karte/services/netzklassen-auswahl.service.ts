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

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { MapQueryParamsService } from 'src/app/karte/services/map-query-params.service';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';

@Injectable({
  providedIn: 'root',
})
export class NetzklassenAuswahlService {
  currentAuswahl$: Observable<Netzklassefilter[]>;
  constructor(private mapQueryParamService: MapQueryParamsService) {
    this.currentAuswahl$ = this.mapQueryParamService.mapQueryParams$.pipe(
      map(params => params.netzklassen ?? [Netzklassefilter.RADNETZ])
    );
  }

  get currentAuswahl(): Netzklassefilter[] {
    return this.mapQueryParamService.mapQueryParamsSnapshot.netzklassen ?? [Netzklassefilter.RADNETZ];
  }

  public selectNetzklasse(selectedNetzklasse: Netzklassefilter): void {
    const currentNetzklassen = this.mapQueryParamService.mapQueryParamsSnapshot.netzklassen;
    this.mapQueryParamService.update({ netzklassen: [...currentNetzklassen, selectedNetzklasse] });
  }

  public deselectNetzklasse(deselectedNetzklasse: Netzklassefilter): void {
    const currentNetzklassen = this.mapQueryParamService.mapQueryParamsSnapshot.netzklassen;
    const newNetzklassen = currentNetzklassen.filter(nk => nk !== deselectedNetzklasse);
    this.mapQueryParamService.update({ netzklassen: newNetzklassen });
  }
}
