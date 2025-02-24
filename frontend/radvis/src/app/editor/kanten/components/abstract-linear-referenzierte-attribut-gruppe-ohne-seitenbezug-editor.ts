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

import { AbstractLinearReferenzierteAttributGruppeEditor } from 'src/app/editor/kanten/components/abstract-linear-referenzierte-attribut-gruppe-editor';
import { KantenSelektion } from 'src/app/editor/kanten/models/kanten-selektion';
import { LinearReferenzierteAttribute } from 'src/app/editor/kanten/models/linear-referenzierte-attribute';
import { VersionierteEntitaet } from 'src/app/shared/models/versionierte-entitaet';

export abstract class AbstractLinearReferenzierteAttributGruppeOhneSeitenbezugEditor<
  A extends LinearReferenzierteAttribute,
  AG extends VersionierteEntitaet,
> extends AbstractLinearReferenzierteAttributGruppeEditor<A, AG> {
  public onInsertAtIndex(kantenIndex: number, segmentIndex: number): void {
    const arrayToChange = this.getAttributeFromAttributGruppe(this.currentAttributgruppen[kantenIndex]);
    const deepCopy = JSON.parse(JSON.stringify(arrayToChange[segmentIndex - 1])) as A;
    arrayToChange.splice(segmentIndex, 0, deepCopy);
    this.kantenSelektionService.adjustSelectionForSegmentInsertion(
      this.currentSelektion![kantenIndex].kante.id,
      segmentIndex
    );
  }

  public onDeleteAtIndex(kantenIndex: number, segmentIndex: number): void {
    const arrayToChange = this.getAttributeFromAttributGruppe(this.currentAttributgruppen[kantenIndex]);
    arrayToChange.splice(segmentIndex, 1);
    this.kantenSelektionService.adjustSelectionForSegmentDeletion(
      this.currentSelektion![kantenIndex].kante.id,
      segmentIndex
    );
  }

  protected abstract getAttributeFromAttributGruppe(attributGruppe: AG): A[];
}
