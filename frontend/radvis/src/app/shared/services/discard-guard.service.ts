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
import { Injectable } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable, of } from 'rxjs';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { DiscardableComponent } from 'src/app/shared/services/discard.guard';

@Injectable({
  providedIn: 'root',
})
export class DiscardGuardService {
  constructor(private dialog: MatDialog) {}

  public canDeactivate(component: DiscardableComponent): Observable<boolean> {
    const canDiscard = component.canDiscard.bind(component);
    if (canDiscard()) {
      return of(true);
    }

    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        question: 'Wollen Sie den aktuellen Editor wirklich verlassen ohne zu speichern?',
        labelYes: 'Verlassen',
        labelNo: 'Zurück zum Editor',
      } as QuestionYesNo,
    });

    return dialogRef.afterClosed();
  }
}
