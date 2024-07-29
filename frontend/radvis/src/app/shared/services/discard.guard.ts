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

import { inject } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRouteSnapshot, CanDeactivateFn, RouterStateSnapshot } from '@angular/router';
import { Observable, of } from 'rxjs';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { map } from 'rxjs/operators';

export interface DiscardableComponent {
  canDiscard: () => boolean;
}

export const discardGuard: CanDeactivateFn<DiscardableComponent> = (
  component: DiscardableComponent,
  currentRoute: ActivatedRouteSnapshot,
  currentState: RouterStateSnapshot,
  nextState: RouterStateSnapshot
): Observable<boolean> => {
  const canDiscard = component.canDiscard.bind(component);
  if (canDiscard()) {
    return of(true);
  }

  const dialog: MatDialog = inject(MatDialog);
  const dialogRef = dialog.open(ConfirmationDialogComponent, {
    data: {
      question: 'Wollen Sie den aktuellen Editor wirklich verlassen ohne zu speichern?',
      labelYes: 'Verlassen',
      labelNo: 'Zurück zum Editor',
    } as QuestionYesNo,
  });

  // Ein Klick außerhalb des Dialogbereichs sorgt dafür, dass hier "undefined" zurück kommt. Damit kann der Standard
  // discard-Mechanismus von Angular nicht umgehen und interpretiert das als "yes"-Aktion, was aber nicht gewollt ist.
  // Klickt man außerhalb des Dialogs soll das als "no"-Aktion gewertet werden.
  return dialogRef.afterClosed().pipe(map(yes => Boolean(yes)));
};
