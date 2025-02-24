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
import { map } from 'rxjs/operators';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { BarriereListenView } from 'src/app/viewer/barriere/models/barriere-listen-view';
import { BarriereFilterService } from 'src/app/viewer/barriere/services/barriere-filter.service';
import { BarriereRoutingService } from 'src/app/viewer/barriere/services/barriere-routing.service';
import { SpaltenDefinition } from 'src/app/viewer/viewer-shared/models/spalten-definition';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';

@Component({
  selector: 'rad-barrieren-tabelle',
  templateUrl: './barrieren-tabelle.component.html',
  styleUrls: ['./barrieren-tabelle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [{ provide: AbstractInfrastrukturenFilterService, useExisting: forwardRef(() => BarriereFilterService) }],
  standalone: false,
})
export class BarrierenTabelleComponent {
  selectedBarriereId$: Observable<number | null>;

  spaltenDefinition: SpaltenDefinition[] = [
    { name: 'verantwortlich', displayName: 'Verantwortliche Organisation' },
    { name: 'barriereform', displayName: 'Barriereform', width: 'large' },
    { name: 'durchfahrtsbreite', displayName: 'Barrieredurchfahrbreite' },
    { name: 'sicherung', displayName: 'Absperranlagensicherung' },
    { name: 'markierung', displayName: 'Absperranlagenmarkierung' },
  ];

  data$: Observable<BarriereListenView[]>;
  public isBenutzerBerechtigtBarrierenZuErstellen: boolean;

  isSmallViewport = false;
  filteredSpalten$: Observable<string[]>;

  constructor(
    public barriereFilterService: BarriereFilterService,
    private barriereRoutingService: BarriereRoutingService,
    benutzerDetailsService: BenutzerDetailsService
  ) {
    this.isBenutzerBerechtigtBarrierenZuErstellen = benutzerDetailsService.canCreateFurtenKreuzungen();
    this.data$ = this.barriereFilterService.filteredList$;
    this.selectedBarriereId$ = barriereRoutingService.selectedInfrastrukturId$;
    this.filteredSpalten$ = this.barriereFilterService.filter$.pipe(
      map(filteredFields => filteredFields.map(f => f.field))
    );
  }

  public onSelectRecord(id: number): void {
    this.barriereRoutingService.toInfrastrukturEditor(id);
  }

  onChangeBreakpointState(isSmall: boolean): void {
    this.isSmallViewport = isSmall;
  }

  onFilterReset(): void {
    this.barriereFilterService.reset();
  }

  onCreate(): void {
    this.barriereRoutingService.toCreator();
  }
}
