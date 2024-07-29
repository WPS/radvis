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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnDestroy, OnInit } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { NetzklassenAuswahlService } from 'src/app/karte/services/netzklassen-auswahl.service';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { Signatur } from 'src/app/shared/models/signatur';

@Component({
  selector: 'rad-netzklassen-auswahl',
  templateUrl: './netzklassen-auswahl.component.html',
  styleUrls: ['./netzklassen-auswahl.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NetzklassenAuswahlComponent implements OnInit, OnDestroy {
  @Input()
  public zoom: number = Number.MAX_VALUE;
  @Input()
  public selectedSignatur: Signatur | null = null;

  public selectedNetzklassen$: Observable<Netzklassefilter[]>;
  public allNetzklassen: Netzklassefilter[] = Netzklassefilter.getAll();

  protected showSignaturIncompatibleHinweis = false;
  protected readonly signaturIncompatibleHinweis = Signatur.SIGNATUR_NETZFILTER_INCOMPATIBLE_MESSAGE;

  private subscriptions: Subscription[] = [];

  constructor(
    private netzklassenAuswahlService: NetzklassenAuswahlService,
    changeDetector: ChangeDetectorRef
  ) {
    this.selectedNetzklassen$ = this.netzklassenAuswahlService.currentAuswahl$;
    this.subscriptions.push(
      this.selectedNetzklassen$.subscribe(netzklassenfilter => {
        if (this.selectedSignatur) {
          this.showSignaturIncompatibleHinweis = !Signatur.isCompatibleWithNetzklassenfilter(
            this.selectedSignatur,
            netzklassenfilter
          );
          changeDetector.markForCheck();
        }
      })
    );
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  ngOnInit(): void {
    if (this.selectedSignatur) {
      this.showSignaturIncompatibleHinweis = !Signatur.isCompatibleWithNetzklassenfilter(
        this.selectedSignatur,
        this.netzklassenAuswahlService.currentAuswahl
      );
    }
  }

  onNetzklassenAuswahlChange(netzklasse: Netzklassefilter, checked: boolean): void {
    if (checked) {
      this.netzklassenAuswahlService.selectNetzklasse(netzklasse);
    } else {
      this.netzklassenAuswahlService.deselectNetzklasse(netzklasse);
    }
  }
}
