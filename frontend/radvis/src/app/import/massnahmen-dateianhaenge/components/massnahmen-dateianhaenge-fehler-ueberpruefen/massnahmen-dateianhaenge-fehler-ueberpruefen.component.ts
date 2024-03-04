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

import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { ActivatedRoute } from '@angular/router';
import { MassnahmenDateianhaengeImportSessionView } from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-import-session-view';
import {
  MassnahmenDateianhaengeMappingHinweis,
  MassnahmenDateianhaengeZuordnung,
  MassnahmenDateianhaengeZuordnungStatus,
} from 'src/app/import/massnahmen-dateianhaenge/models/massnahmen-dateianhaenge-zuordnung';
import { MassnahmenDateianhaengeRoutingService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge-routing.service';
import { MassnahmenDateianhaengeService } from 'src/app/import/massnahmen-dateianhaenge/services/massnahmen-dateianhaenge.service';

export interface MassnahmenDateianhaengeFehlerUeberpruefenRow {
  ordnername: string;
  rowspan: number;
  hide: boolean;
  datei: string;
  hinweise: MassnahmenDateianhaengeMappingHinweis[];
}

@Component({
  selector: 'rad-massnahmen-dateianhaenge-fehler-ueberpruefen',
  templateUrl: './massnahmen-dateianhaenge-fehler-ueberpruefen.component.html',
  styleUrl: './massnahmen-dateianhaenge-fehler-ueberpruefen.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MassnahmenDateianhaengeFehlerUeberpruefenComponent {
  private static readonly STEP = 2;

  dataSource: MatTableDataSource<MassnahmenDateianhaengeFehlerUeberpruefenRow>;
  displayedColumns = ['ordner', 'datei', 'fehler'];

  canContinue = false;

  constructor(
    private service: MassnahmenDateianhaengeService,
    private routingService: MassnahmenDateianhaengeRoutingService,
    route: ActivatedRoute
  ) {
    const session: MassnahmenDateianhaengeImportSessionView = route.snapshot.data.session;
    this.canContinue = session.zuordnungen.some(z => z.status === MassnahmenDateianhaengeZuordnungStatus.GEMAPPT);
    this.dataSource = new MatTableDataSource<MassnahmenDateianhaengeFehlerUeberpruefenRow>(
      this.convertZuordnungen(session.zuordnungen)
    );
  }

  onAbort(): void {
    this.service.deleteImportSession().subscribe(() => this.routingService.navigateToFirst());
  }

  onPrevious(): void {
    this.routingService.navigateToPrevious(MassnahmenDateianhaengeFehlerUeberpruefenComponent.STEP);
  }

  onNext(): void {
    this.service
      .continueAfterFehlerUeberpruefen()
      .subscribe(() => this.routingService.navigateToNext(MassnahmenDateianhaengeFehlerUeberpruefenComponent.STEP));
  }

  private convertZuordnungen(
    zuordnungen: MassnahmenDateianhaengeZuordnung[]
  ): MassnahmenDateianhaengeFehlerUeberpruefenRow[] {
    return zuordnungen.reduce((current: MassnahmenDateianhaengeFehlerUeberpruefenRow[], next) => {
      const base: MassnahmenDateianhaengeFehlerUeberpruefenRow = {
        ordnername: next.ordnername,
        datei: '-',
        hinweise: next.hinweise,
        rowspan: next.dateien.length || 1,
        hide: false,
      };
      if (next.dateien.length === 0) {
        current.push(base);
      }
      for (let i = 0; i < next.dateien.length; i++) {
        const datei = next.dateien[i];
        current.push({
          ...base,
          datei: datei,
          hide: i > 0,
        });
      }
      return current;
    }, []);
  }
}
