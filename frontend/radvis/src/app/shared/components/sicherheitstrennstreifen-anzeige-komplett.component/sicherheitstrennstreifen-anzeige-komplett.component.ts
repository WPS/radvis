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
import { TrennstreifenSeite } from 'src/app/shared/models/trennstreifen-seite';
import { Richtung } from 'src/app/editor/kanten/models/richtung';

@Component({
  selector: 'rad-sicherheitstrennstreifen-anzeige-komplett',
  templateUrl: './sicherheitstrennstreifen-anzeige-komplett.component.html',
  styleUrls: ['./sicherheitstrennstreifen-anzeige-komplett.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SicherheitstrennstreifenAnzeigeKomplettComponent {
  @Output()
  selectedSeiteChange = new EventEmitter<TrennstreifenSeite>();

  @Input()
  trennstreifenEinseitig?: boolean;
  @Input()
  trennstreifenRichtungRechts?: Richtung;
  @Input()
  trennstreifenRichtungLinks?: Richtung;
  @Input()
  trennstreifenSeiteSelected: TrennstreifenSeite | undefined;
  @Input()
  trennstreifenBearbeiteteSeiten: Set<TrennstreifenSeite> = new Set<TrennstreifenSeite>();

  public TrennstreifenSeite = TrennstreifenSeite;

  constructor() {}

  onTrennstreifenSeiteSelectionChanged(seite: TrennstreifenSeite): void {
    this.selectedSeiteChange.emit(seite);
  }
}
