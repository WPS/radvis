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
import { Subscription } from 'rxjs';
import { EnumOption } from 'src/app/form-elements/models/enum-option';
import { Bauwerksmangel } from 'src/app/shared/models/bauwerksmangel';
import { BauwerksmangelArt } from 'src/app/shared/models/bauwerksmangel-art';
import { KNOTENFORMEN } from 'src/app/shared/models/knotenformen';
import { QuerungshilfeDetails } from 'src/app/shared/models/querungshilfe-details';
import { SharedKnotenFormGroup } from 'src/app/shared/models/shared-knoten-form-group';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-shared-knoten-form-group-component',
  templateUrl: './shared-knoten-form-group.component.html',
  styleUrl: './shared-knoten-form-group.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class SharedKnotenFormGroupComponent implements OnInit, OnDestroy {
  @Input()
  sharedKnotenFormGroup!: SharedKnotenFormGroup;

  subscriptions: Subscription[] = [];

  querungshilfeDetailsOptions: EnumOption[] = [];
  bauwerksmangelOptions = Bauwerksmangel.options;
  bauwerksmangelArtOptions: EnumOption[] = [];
  public knotenFormOptions = KNOTENFORMEN;

  constructor(private changeDetector: ChangeDetectorRef) {}

  ngOnInit(): void {
    invariant(this.sharedKnotenFormGroup);

    this.subscriptions.push(
      this.sharedKnotenFormGroup.statusChanges.subscribe(() => this.changeDetector.markForCheck()),
      this.sharedKnotenFormGroup.valueChanges.subscribe(() => this.changeDetector.markForCheck()),
      this.sharedKnotenFormGroup.controls.knotenForm.valueChanges.subscribe(value => {
        this.updateEnumOptionsForKnotenform(value);
        this.changeDetector.markForCheck();
      })
    );

    this.updateEnumOptionsForKnotenform(this.sharedKnotenFormGroup.value.knotenForm ?? null);
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  private updateEnumOptionsForKnotenform(knotenform: string | null): void {
    if (knotenform) {
      this.querungshilfeDetailsOptions = QuerungshilfeDetails.getOptionsForKnotenform(knotenform);
      this.bauwerksmangelArtOptions = BauwerksmangelArt.getOptionsForKnotenform(knotenform);
    } else {
      this.querungshilfeDetailsOptions = [];
      this.bauwerksmangelArtOptions = [];
    }
  }
}
