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
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { BenutzerStatus } from 'src/app/administration/models/benutzer-status';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';

@Component({
  selector: 'rad-freischaltung',
  templateUrl: './freischaltung.component.html',
  styleUrls: ['./freischaltung.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class FreischaltungComponent {
  status: BenutzerStatus;
  public waiting = false;

  constructor(
    private benutzerDetailsService: BenutzerDetailsService,
    private changeDetectorRef: ChangeDetectorRef,
    private errorHandlingService: ErrorHandlingService
  ) {
    this.status = benutzerDetailsService.aktuellerBenutzerStatus();
  }

  public beantrageReaktivierung(): void {
    this.waiting = true;
    this.benutzerDetailsService.beantrageReaktivierung().subscribe({
      next: benutzerDetails => {
        this.status = benutzerDetails.status;
        this.changeDetectorRef.detectChanges();
      },
      error: err => this.errorHandlingService.handleHttpError(err),
      complete: () => (this.waiting = false),
    });
  }
}
