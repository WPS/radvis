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

import { DebugElement } from '@angular/core';
import { By } from '@angular/platform-browser';
import { AutomatischerImportSchritt } from 'src/app/import/models/automatischer-import-schritt';

export class ImportNetzklasseAutomatischeAbbildungTestAdapter {
  private readonly erledigtIcon = 'check';
  private readonly offenIcon = 'more_horiz';
  private readonly fehlerIcon = 'close';

  private readonly schritte = [
    AutomatischerImportSchritt.IMPORT_DER_GEOMETRIEN,
    AutomatischerImportSchritt.ABBILDUNG_AUF_RADVIS_NETZ,
    AutomatischerImportSchritt.AUTOMATISCHE_ABBILDUNG_ABGESCHLOSSEN,
  ];

  constructor(private componentElement: DebugElement) {}

  isWeiterButtonDisabled(): boolean {
    return this.componentElement.query(By.css('.buttons')).children[2].nativeElement.disabled;
  }

  getErledigteSchritte(): AutomatischerImportSchritt[] {
    return this.getSchritteWithIcon(this.erledigtIcon);
  }

  getOffeneSchritte(): AutomatischerImportSchritt[] {
    return this.getSchritteWithIcon(this.offenIcon);
  }

  getFehlerhafteSchritte(): AutomatischerImportSchritt[] {
    return this.getSchritteWithIcon(this.fehlerIcon);
  }

  sindAlleSchritteOffen(): boolean {
    return (
      this.getErledigteSchritte().length === 0 &&
      this.getFehlerhafteSchritte().length === 0 &&
      this.getOffeneSchritte().length === 3
    );
  }

  sindAlleSchritteErledigt(): boolean {
    return (
      this.getErledigteSchritte().length === 3 &&
      this.getFehlerhafteSchritte().length === 0 &&
      this.getOffeneSchritte().length === 0
    );
  }

  private getStatusSummaryIcons(): string[] {
    return [
      this.componentElement.children[0].children[0].children[0].nativeElement.innerText,
      this.componentElement.children[0].children[1].children[0].nativeElement.innerText,
      this.componentElement.children[0].children[2].children[0].nativeElement.innerText,
    ];
  }

  private getSchritteWithIcon(matIconInnerText: string): AutomatischerImportSchritt[] {
    return this.getStatusSummaryIcons()
      .map((icon, index) => {
        if (icon === matIconInnerText) {
          return index;
        } else {
          return -1;
        }
      })
      .filter(index => index >= 0)
      .map(i => this.schritte[i]);
  }
}
