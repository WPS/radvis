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

import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { Richtung } from 'src/app/editor/kanten/models/richtung';
import { KantenSeite } from 'src/app/shared/models/kantenSeite';
import { TrennstreifenSeite } from 'src/app/shared/models/trennstreifen-seite';

@Component({
  selector: 'rad-sicherheitstrennstreifen-anzeige',
  templateUrl: './sicherheitstrennstreifen-anzeige.component.html',
  styleUrls: ['./sicherheitstrennstreifen-anzeige.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SicherheitstrennstreifenAnzeigeComponent {
  @Input()
  hasStationierungsrichtungspfeil: boolean | undefined;

  @Input()
  richtung: Richtung | undefined;

  @Input()
  seiteRechts: TrennstreifenSeite | undefined;

  @Input()
  seiteLinks: TrennstreifenSeite | undefined;

  @Input()
  rechtsDirty = false;

  @Input()
  linksDirty = false;

  @Input()
  selectedSeite: TrennstreifenSeite | undefined;
  @Output()
  selectedSeiteChange = new EventEmitter<TrennstreifenSeite>();

  public Seitenbezug = KantenSeite;
  public Richtung = Richtung;

  constructor() {}

  onTrennstreifenClicked(seite: TrennstreifenSeite | undefined): void {
    if (this.selectedSeite !== seite) {
      this.selectedSeiteChange.emit(seite);
    }
  }
}
