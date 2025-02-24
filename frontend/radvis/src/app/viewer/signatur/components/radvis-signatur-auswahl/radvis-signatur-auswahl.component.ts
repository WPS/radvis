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

import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
} from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatSelect } from '@angular/material/select';
import { Subscription } from 'rxjs';
import { NetzklassenAuswahlService } from 'src/app/karte/services/netzklassen-auswahl.service';
import { Signatur } from 'src/app/shared/models/signatur';
import { SignaturTyp } from 'src/app/shared/models/signatur-typ';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { SignaturService } from 'src/app/viewer/signatur/services/signatur.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';

@Component({
  selector: 'rad-radvis-signatur-auswahl',
  templateUrl: './radvis-signatur-auswahl.component.html',
  styleUrls: ['./radvis-signatur-auswahl.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class RadvisSignaturAuswahlComponent implements OnInit, OnDestroy {
  @Output()
  selectRadVisSignatur = new EventEmitter<Signatur | null>();

  @Input()
  selectedSignatur: Signatur | null = null;

  @ViewChild('matSelect')
  selectElement: MatSelect | null = null;

  public signaturen: Signatur[] = [];
  public formControl: FormControl<Signatur | null>;
  public compareSignatur = Signatur.compare;

  private subscriptions: Subscription[] = [];

  protected readonly signaturIncompatibleHinweis = Signatur.SIGNATUR_NETZFILTER_INCOMPATIBLE_MESSAGE;

  constructor(
    private signaturService: SignaturService,
    private infrastrukturenSelectionService: InfrastrukturenSelektionService,
    private changeDetector: ChangeDetectorRef,
    private netzklassenAuswahlService: NetzklassenAuswahlService
  ) {
    this.formControl = new FormControl<Signatur | null>(null);
    this.formControl.valueChanges.subscribe(value => {
      this.selectRadVisSignatur.emit(value);
    });
    this.infrastrukturenSelectionService.selektierteInfrastrukturen$.subscribe(() => {
      this.initSignaturen();
      this.changeDetector.markForCheck();
    });
    this.initSignaturen();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  private initSignaturen(): void {
    this.signaturen = this.signaturService.getSignaturen(
      this.infrastrukturenSelectionService.isSelected(MASSNAHMEN)
        ? [SignaturTyp.MASSNAHME, SignaturTyp.NETZ]
        : [SignaturTyp.NETZ]
    );
  }

  ngOnInit(): void {
    this.formControl.patchValue(this.selectedSignatur, { emitEvent: false });
  }

  getDisplayText(signature: Signatur): string {
    if (signature.typ === SignaturTyp.MASSNAHME) {
      return 'Maßnahmen - ' + signature.name;
    }
    return signature.name;
  }

  isSelectionCompatibleWithNetzklassenfilter(): boolean {
    if (this.formControl.value) {
      return Signatur.isCompatibleWithNetzklassenfilter(
        this.formControl.value,
        this.netzklassenAuswahlService.currentAuswahl
      );
    }

    return true;
  }

  setFocus(): void {
    this.selectElement?.focus();
  }
}
