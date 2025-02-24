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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, Input, OnChanges, SimpleChanges } from '@angular/core';
import { StyleFunction } from 'ol/style/Style';
import { Observable } from 'rxjs';
import { NetzklassenAuswahlService } from 'src/app/karte/services/netzklassen-auswahl.service';
import { Netzklassefilter } from 'src/app/shared/models/netzklassefilter';
import { Signatur } from 'src/app/shared/models/signatur';
import { SignaturLegende } from 'src/app/shared/models/signatur-legende';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { SignaturStyleProviderService } from 'src/app/viewer/signatur/services/signatur-style-provider.service';

@Component({
  selector: 'rad-radvis-signatur-layer',
  templateUrl: './radvis-signatur-layer.component.html',
  styleUrls: ['./radvis-signatur-layer.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class RadvisSignaturLayerComponent implements OnChanges {
  public static readonly ID_PREFIX = 'RadVis_SIGNATUR_';

  @Input()
  selectedSignatur!: Signatur;

  public styleFunction: StyleFunction | null = null;
  public attributnamen: string[] = [];
  public signaturname: string | null = null;

  public selectedNetzklassen$: Observable<Netzklassefilter[]>;
  public layerPrefix = RadvisSignaturLayerComponent.ID_PREFIX;
  public legende: SignaturLegende | null = null;
  public Netzklassefilter = Netzklassefilter;

  constructor(
    private signaturStyleProvider: SignaturStyleProviderService,
    private changeDetector: ChangeDetectorRef,
    private errorHandlingService: ErrorHandlingService,
    private netzklassenAuswahlService: NetzklassenAuswahlService
  ) {
    this.selectedNetzklassen$ = netzklassenAuswahlService.currentAuswahl$;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.selectedSignatur && this.selectedSignatur) {
      this.signaturStyleProvider.getLegendeForSignatur(this.selectedSignatur).then(legende => {
        this.legende = legende;
        this.changeDetector.markForCheck();
      });
      this.signaturStyleProvider
        .getStyleInformation(this.selectedSignatur)
        .then(styleInformation => {
          this.attributnamen = styleInformation.attributnamen;
          this.styleFunction = styleInformation.styleFunction;
          this.signaturname = this.selectedSignatur.name;
          this.changeDetector.markForCheck();
        })
        .catch(error => this.errorHandlingService.handleError(error, 'Style Function konnte nicht angewendet werden'));
    }
  }
}
