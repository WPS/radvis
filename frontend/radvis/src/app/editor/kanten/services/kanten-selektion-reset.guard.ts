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
import { ActivatedRouteSnapshot, CanDeactivateFn, RouterStateSnapshot } from '@angular/router';
import { AbstractAttributGruppeEditor } from 'src/app/editor/kanten/components/abstract-attribut-gruppe-editor';
import { KantenSelektionService } from 'src/app/editor/kanten/services/kanten-selektion.service';
import { NetzBearbeitungModusService } from 'src/app/editor/kanten/services/netz-bearbeitung-modus.service';

export const kantenSelektionResetGuard: CanDeactivateFn<AbstractAttributGruppeEditor> = (
  component: AbstractAttributGruppeEditor,
  currentRoute: ActivatedRouteSnapshot,
  currentState: RouterStateSnapshot,
  nextState: RouterStateSnapshot
) => {
  const kantenSelektionService: KantenSelektionService = inject(KantenSelektionService);
  const netzBearbeitungModusService: NetzBearbeitungModusService = inject(NetzBearbeitungModusService);

  const noReset = nextState && netzBearbeitungModusService.isKantenSubEditorAktiv(nextState.url);
  return kantenSelektionService.cleanUp(!noReset);
};
