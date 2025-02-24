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
import { ExportFormat } from 'src/app/viewer/viewer-shared/models/export-format';
import { TabellenSpaltenAuswahlService } from 'src/app/viewer/viewer-shared/services/tabellen-spalten-auswahl.service';

export interface ExportEvent {
  format: ExportFormat;
  felder: string[];
}

@Component({
  selector: 'rad-export-button',
  templateUrl: './export-button.component.html',
  styleUrls: ['./export-button.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class ExportButtonComponent {
  @Input()
  exportFormate: ExportFormat[] = [];

  @Input()
  exporting = false;

  @Input()
  isMenuItem = false;

  @Output()
  export = new EventEmitter<ExportEvent>();

  ExportFormat = ExportFormat;

  constructor(private tabellenSpaltenAuswahlService: TabellenSpaltenAuswahlService) {}

  onClick(format: ExportFormat): void {
    this.export.next({ format, felder: this.tabellenSpaltenAuswahlService.getCurrentAuswahl() });
  }
}
