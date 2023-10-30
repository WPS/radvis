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

import { ChangeDetectionStrategy, Component, EventEmitter, Input, OnInit, Output, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { BehaviorSubject, Observable } from 'rxjs';
import { Signatur } from 'src/app/shared/models/signatur';
import { SignaturTyp } from 'src/app/shared/models/signatur-typ';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { SignaturService } from 'src/app/viewer/signatur/services/signatur.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { MatSelect } from '@angular/material/select';

@Component({
  selector: 'rad-radvis-signatur-auswahl',
  templateUrl: './radvis-signatur-auswahl.component.html',
  styleUrls: ['./radvis-signatur-auswahl.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RadvisSignaturAuswahlComponent implements OnInit {
  @Output()
  selectRadVisSignatur = new EventEmitter<Signatur>();

  @Input()
  selectedSignatur: Signatur | null = null;

  @ViewChild('matSelect')
  selectElement: MatSelect | null = null;

  public signaturenSubject$: BehaviorSubject<Signatur[]> = new BehaviorSubject<Signatur[]>([]);
  public formControl: FormControl;
  public compareSignatur = Signatur.compare;

  private massnahmenInfrastrukturSelected = false;

  constructor(
    private signaturService: SignaturService,
    private featureTogglzService: FeatureTogglzService,
    private infrastrukturenSelectionService: InfrastrukturenSelektionService
  ) {
    this.formControl = new FormControl();
    this.formControl.valueChanges.subscribe(value => {
      this.selectRadVisSignatur.emit(value);
    });
    this.infrastrukturenSelectionService.selektierteInfrastrukturen$.subscribe(infrastrukturen => {
      this.massnahmenInfrastrukturSelected = infrastrukturen.includes(MASSNAHMEN);
      this.loadSignaturen();
    });
  }

  ngOnInit(): void {
    this.formControl.patchValue(this.selectedSignatur, { emitEvent: false });
    this.loadSignaturen();
  }

  getDisplayText(signature: Signatur): string {
    if (signature.typ === SignaturTyp.MASSNAHME) {
      return 'Maßnahmen - ' + signature.name;
    }
    return signature.name;
  }

  setFocus(): void {
    this.selectElement?.focus();
  }

  get signaturen$(): Observable<Signatur[]> {
    return this.signaturenSubject$.asObservable();
  }

  private loadSignaturen(): void {
    this.signaturService.getSignaturen().subscribe(signaturen => {
      if (!this.massnahmenInfrastrukturSelected) {
        signaturen = signaturen.filter(s => s.typ !== SignaturTyp.MASSNAHME);
      }
      this.signaturenSubject$.next(signaturen);
    });
  }
}
