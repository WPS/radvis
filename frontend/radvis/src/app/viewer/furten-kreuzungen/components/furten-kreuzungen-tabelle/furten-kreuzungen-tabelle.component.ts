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

import { ChangeDetectionStrategy, Component, forwardRef } from '@angular/core';
import { Observable } from 'rxjs';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { FurtKreuzungListenView } from 'src/app/viewer/furten-kreuzungen/models/furt-kreuzung-listen-view';
import { FurtenKreuzungenFilterService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen-filter.service';
import { FurtenKreuzungenRoutingService } from 'src/app/viewer/furten-kreuzungen/services/furten-kreuzungen-routing.service';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';

@Component({
  selector: 'rad-furten-kreuzungen-tabelle',
  templateUrl: './furten-kreuzungen-tabelle.component.html',
  styleUrls: ['./furten-kreuzungen-tabelle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: AbstractInfrastrukturenFilterService, useExisting: forwardRef(() => FurtenKreuzungenFilterService) },
  ],
})
export class FurtenKreuzungenTabelleComponent {
  selectedFurtKreuzungId$: Observable<number | null>;

  displayedColumns: string[] = ['id', 'verantwortlich', 'typ', 'radnetzKonform', 'kommentar', 'knotenForm'];

  data$: Observable<FurtKreuzungListenView[]>;
  public isBenutzerBerechtigtFurtenKreuzungenZuErstellen: boolean;

  isSmallViewport = false;

  constructor(
    public furtenKreuzungenFilterService: FurtenKreuzungenFilterService,
    private furtenKreuzungenRoutingService: FurtenKreuzungenRoutingService,
    private benutzerDetailsService: BenutzerDetailsService
  ) {
    this.isBenutzerBerechtigtFurtenKreuzungenZuErstellen = benutzerDetailsService.canCreateFurtenKreuzungen();
    this.data$ = this.furtenKreuzungenFilterService.filteredList$;
    this.selectedFurtKreuzungId$ = furtenKreuzungenRoutingService.selectedInfrastrukturId$;
  }

  onChangeBreakpointState(isSmall: boolean): void {
    this.isSmallViewport = isSmall;
  }

  public onSelectRecord(id: number): void {
    this.furtenKreuzungenRoutingService.toInfrastrukturEditor(id);
  }

  onFilterReset(): void {
    this.furtenKreuzungenFilterService.reset();
  }

  onCreate(): void {
    this.furtenKreuzungenRoutingService.toCreator();
  }
}
