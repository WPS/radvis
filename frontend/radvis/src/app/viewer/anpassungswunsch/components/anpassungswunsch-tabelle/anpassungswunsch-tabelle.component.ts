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
import { FormControl } from '@angular/forms';
import { Observable } from 'rxjs';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { AnpassungswunschListenView } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-listen-view';
import { AnpassungenRoutingService } from 'src/app/viewer/anpassungswunsch/services/anpassungen-routing.service';
import { AnpassungswunschFilterService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch-filter.service';
import { AbstractInfrastrukturenFilterService } from 'src/app/viewer/viewer-shared/services/abstract-infrastrukturen-filter.service';

@Component({
  selector: 'rad-anpassungswunsch-tabelle',
  templateUrl: './anpassungswunsch-tabelle.component.html',
  styleUrls: ['./anpassungswunsch-tabelle.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    { provide: AbstractInfrastrukturenFilterService, useExisting: forwardRef(() => AnpassungswunschFilterService) },
  ],
})
export class AnpassungswunschTabelleComponent {
  selectedAnpassungswunschID$: Observable<number | null>;

  displayedColumns: string[] = ['beschreibung', 'status', 'kategorie', 'verantwortlicheOrganisation'];

  data$: Observable<AnpassungswunschListenView[]>;

  public anpassungswunschCreatorRoute: string;
  fertigeAnpassungswuenscheAusblendenControl: FormControl;

  public isBenutzerBerechtigtAnpassungswunschZuErstellen: boolean;

  isSmallViewport = false;

  constructor(
    public anpassungswunschFilterService: AnpassungswunschFilterService,
    private anpassungenRoutingService: AnpassungenRoutingService,
    private benutzerDetailsService: BenutzerDetailsService
  ) {
    this.data$ = this.anpassungswunschFilterService.filteredList$;

    this.selectedAnpassungswunschID$ = this.anpassungenRoutingService.selectedInfrastrukturId$;

    this.isBenutzerBerechtigtAnpassungswunschZuErstellen = benutzerDetailsService.canCreateAnpassungswunsch();

    this.anpassungswunschCreatorRoute = anpassungenRoutingService.getCreatorRoute();

    this.fertigeAnpassungswuenscheAusblendenControl = new FormControl(
      anpassungswunschFilterService.abgeschlosseneSindAusgeblendet
    );

    this.fertigeAnpassungswuenscheAusblendenControl.valueChanges.subscribe(active => {
      if (active) {
        anpassungswunschFilterService.abgeschlosseneAusblenden();
      } else {
        anpassungswunschFilterService.abgeschlosseneEinblenden();
      }
    });
  }

  onChangeBreakpointState(isSmall: boolean): void {
    this.isSmallViewport = isSmall;
  }

  public onSelectRecord(id: number): void {
    this.anpassungenRoutingService.toInfrastrukturEditor(id);
  }

  onFilterReset(): void {
    this.anpassungswunschFilterService.reset();
  }
}
