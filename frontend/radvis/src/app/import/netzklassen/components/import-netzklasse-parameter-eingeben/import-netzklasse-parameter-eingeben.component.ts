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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { UntypedFormControl } from '@angular/forms';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { NetzklassenParameter } from 'src/app/import/netzklassen/models/netzklassen-parameter';
import { StartNetzklassenImportSessionCommand } from 'src/app/import/netzklassen/models/start-netzklassen-import-session-command';
import { NetzklassenImportService } from 'src/app/import/netzklassen/services/netzklassen-import.service';
import { NetzklassenRoutingService } from 'src/app/import/netzklassen/services/netzklassen-routing.service';
import { CreateSessionStateService } from 'src/app/import/services/create-session.state.service';
import { Netzklasse } from 'src/app/shared/models/netzklasse';

@Component({
  selector: 'rad-netzklasse-parameter-eingeben',
  templateUrl: './import-netzklasse-parameter-eingeben.component.html',
  styleUrls: ['./import-netzklasse-parameter-eingeben.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportNetzklasseParameterEingebenComponent {
  private static readonly STEP = 2;
  netzklasseOptions = Netzklasse.options.filter(
    enumOption =>
      ![Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ZIELNETZ].includes(
        (Netzklasse as any)[enumOption.name]
      )
  );
  formControl = new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty);
  sessionExists = false;
  uploading = false;

  constructor(
    private netzklassenImportService: NetzklassenImportService,
    private netzklassenRoutingService: NetzklassenRoutingService,
    private createSessionStateService: CreateSessionStateService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.netzklassenImportService.getImportSession().subscribe(session => {
      this.sessionExists = !!session;
      if (session) {
        this.formControl.disable();
        this.formControl.setValue(session.netzklasse);
      } else {
        if (
          this.createSessionStateService.parameterInfo !== null &&
          this.createSessionStateService.parameterInfo instanceof NetzklassenParameter
        ) {
          this.formControl.setValue((this.createSessionStateService.parameterInfo as NetzklassenParameter).netzklasse);
        }
        this.formControl.valueChanges.subscribe((value: Netzklasse) => this.onNetzklassenChange(value));
      }
    });
  }

  onNetzklassenChange(neueNetzklasse: Netzklasse): void {
    this.createSessionStateService.updateParameterInfo(NetzklassenParameter.of(neueNetzklasse));
  }

  onAbort(): void {
    this.netzklassenImportService.deleteImportSession().subscribe(() => {
      this.netzklassenRoutingService.navigateToFirst();
    });
    this.createSessionStateService.reset();
  }

  onStart(): void {
    this.formControl.disable();
    if (this.sessionExists) {
      this.navigateToNextStep();
      return;
    }

    if (
      this.createSessionStateService.dateiUploadInfo !== null &&
      this.createSessionStateService.parameterInfo !== null
    ) {
      this.uploading = true;
      this.netzklassenImportService
        .createSessionAndStartNetzklassenImport(
          {
            organisation: this.createSessionStateService.dateiUploadInfo?.organisation,
            netzklasse: (this.createSessionStateService.parameterInfo as NetzklassenParameter).netzklasse,
          } as StartNetzklassenImportSessionCommand,
          this.createSessionStateService.dateiUploadInfo.file
        )
        .then(() => {
          this.createSessionStateService.reset();
          this.navigateToNextStep();
        })
        .finally(() => {
          this.uploading = false;
          this.changeDetectorRef.markForCheck();
        });
    } else {
      this.formControl.enable();
      throw new Error(
        'Es sind noch nicht alle Informationen zum Erstellen einer Session vorhanden. Bitte überprüfen Sie die Eingaben von Schritt 1 und Schritt 2.'
      );
    }
  }

  onNext(): void {
    this.navigateToNextStep();
  }

  private navigateToNextStep(): void {
    this.netzklassenRoutingService.navigateToNext(ImportNetzklasseParameterEingebenComponent.STEP);
  }

  onPrevious(): void {
    this.netzklassenRoutingService.navigateToPrevious(ImportNetzklasseParameterEingebenComponent.STEP);
  }
}
