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

import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { SignaturLegende } from 'src/app/shared/models/signatur-legende';
import { SignaturTyp } from 'src/app/shared/models/signatur-typ';
import { WMSLegende } from 'src/app/shared/models/wms-legende';

@Component({
  selector: 'rad-legende',
  templateUrl: './legende.component.html',
  styleUrls: ['./legende.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LegendeComponent implements OnChanges {
  @Input()
  usePrintLayout = false;
  @Input()
  legenden: (WMSLegende | SignaturLegende)[] = [];

  legendeOpen = false;
  signaturLegenden: SignaturLegende[] = [];
  wmsLegenden: WMSLegende[] = [];

  ngOnChanges(): void {
    this.signaturLegenden = [];
    this.wmsLegenden = [];
    this.legenden?.forEach(l => {
      if (this.isSignaturLegende(l)) {
        this.signaturLegenden.push(l);
      } else {
        this.wmsLegenden.push(l);
      }
    });
  }

  getDisplayText(signaturLegende: SignaturLegende): string {
    if (signaturLegende.typ === SignaturTyp.MASSNAHME) {
      return 'Maßnahmen - ' + signaturLegende.name;
    }
    return signaturLegende.name;
  }

  parseDash(entry: { color: string; dash?: string }): string {
    if (!entry.dash) {
      return 'none';
    }
    const dashValues = entry.dash
      .split(' ')
      .map(n => +n)
      .sort((a, b) => a - b)
      .reverse();
    const first = dashValues[0] * 2;
    const second = (dashValues[0] + dashValues[1]) * 2;
    return `repeating-linear-gradient(to right, ${entry.color} 0 ${first}%, #ffffff ${first}% ${second}%)`;
  }

  private isSignaturLegende(legende: WMSLegende | SignaturLegende): legende is SignaturLegende {
    return (legende as SignaturLegende).entries !== undefined;
  }
}
