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
import { FormControl } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ImportSessionView } from 'src/app/editor/manueller-import/models/import-session-view';
import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import { NetzklassenImportSessionView } from 'src/app/editor/manueller-import/models/netzklassen-import-session-view';
import { NetzklassenParameter } from 'src/app/editor/manueller-import/models/netzklassen-parameter';
import { StartNetzklassenImportSessionCommand } from 'src/app/editor/manueller-import/models/start-netzklassen-import-session-command';
import { CreateSessionStateService } from 'src/app/editor/manueller-import/services/create-session.state.service';
import { ManuellerImportRoutingService } from 'src/app/editor/manueller-import/services/manueller-import-routing.service';
import { ManuellerImportService } from 'src/app/editor/manueller-import/services/manueller-import.service';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import { Netzklasse } from 'src/app/shared/models/netzklasse';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';

@Component({
  selector: 'rad-parameter-eingeben',
  templateUrl: './import-netzklasse-parameter-eingeben.component.html',
  styleUrls: ['./import-netzklasse-parameter-eingeben.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportNetzklasseParameterEingebenComponent {
  previousLink: string;
  nextLink: string;
  netzklasseOptions = Netzklasse.options.filter(
    enumOption =>
      ![Netzklasse.RADNETZ_ALLTAG, Netzklasse.RADNETZ_FREIZEIT, Netzklasse.RADNETZ_ZIELNETZ].includes(
        (Netzklasse as any)[enumOption.name]
      )
  );
  formControl = new FormControl(null, RadvisValidators.isNotNullOrEmpty);
  sessionExists = false;
  uploading = false;

  constructor(
    private manuellerImportRoutingService: ManuellerImportRoutingService,
    private route: ActivatedRoute,
    private router: Router,
    private manuellerImportService: ManuellerImportService,
    private fileHandlingService: FileHandlingService,
    private createSessionStateService: CreateSessionStateService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.previousLink =
      '../' +
      manuellerImportRoutingService.getRouteForStep(route.snapshot.data.step - 1, ImportTyp.NETZKLASSE_ZUWEISEN);
    this.nextLink =
      '../' +
      this.manuellerImportRoutingService.getRouteForStep(
        this.route.snapshot.data.step + 1,
        ImportTyp.NETZKLASSE_ZUWEISEN
      );

    this.manuellerImportService.existsImportSession(ImportTyp.NETZKLASSE_ZUWEISEN).then(exists => {
      this.sessionExists = exists;
      if (exists) {
        this.formControl.disable();
        this.manuellerImportService
          .getImportSession()
          .toPromise()
          .then((session: ImportSessionView) => {
            this.formControl.setValue((session as NetzklassenImportSessionView).netzklasse);
          });
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
    this.manuellerImportService.deleteImportSession().then(() => {
      this.router.navigate(['../' + this.manuellerImportRoutingService.getStartStepRoute()], {
        relativeTo: this.route,
        queryParamsHandling: 'merge',
      });
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
      this.manuellerImportService
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

  private navigateToNextStep(): void {
    this.router.navigate([this.nextLink], {
      relativeTo: this.route,
      queryParamsHandling: 'merge',
    });
  }
}
