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

import { ChangeDetectionStrategy, Component, EventEmitter, Output } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { Coordinate } from 'ol/coordinate';
import { Extent } from 'ol/extent';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';

@Component({
  selector: 'rad-koordinaten-suche',
  templateUrl: './koordinaten-suche.component.html',
  styleUrls: ['./koordinaten-suche.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class KoordinatenSucheComponent {
  @Output()
  public koordinateSuche = new EventEmitter<{ coordinate: Coordinate; extent?: Extent }>();

  public coordinateInputFormGroup = new UntypedFormGroup({
    ost: new UntypedFormControl(''),
    nord: new UntypedFormControl(''),
  });

  constructor(private notifyUserService: NotifyUserService) {}

  public onSubmit(): void {
    const eastingString = this.coordinateInputFormGroup.value.ost as string;
    const northingString = this.coordinateInputFormGroup.value.nord as string;

    const easting = Number.parseFloat(eastingString.replace(',', '.'));
    const northing = Number.parseFloat(northingString.replace(',', '.'));

    if (
      !Number.isNaN(easting) &&
      !Number.isNaN(northing) &&
      this.coordinateInputFormGroup.value.ost !== '' &&
      this.coordinateInputFormGroup.value.nord !== ''
    ) {
      this.koordinateSuche.emit({ coordinate: [easting, northing] as Coordinate });
    } else {
      this.notifyUserService.warn(
        'Eingegebene Koordinaten konnten nicht verarbeitet werden. Bsp.: Ost 513000.0 Nord 5402000.0'
      );
    }
  }

  public onClearClick(): void {
    this.coordinateInputFormGroup.reset();
  }
}
