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

import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { WeitereKartenebenenService } from 'src/app/viewer/weitere-kartenebenen/services/weitere-kartenebenen.service';
import { WeitereKartenebenenDialogComponent } from 'src/app/viewer/weitere-kartenebenen/components/weitere-kartenebenen-dialog/weitere-kartenebenen-dialog.component';

@Component({
  selector: 'rad-weitere-kartenebenen-infrastruktur-menu',
  templateUrl: './weitere-kartenebenen-infrastruktur-menu.component.html',
  styleUrls: ['./weitere-kartenebenen-infrastruktur-menu.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class WeitereKartenebenenInfrastrukturMenuComponent {
  @Input()
  collapsed = false;

  public hasLayers$: Observable<boolean>;

  constructor(
    private dialog: MatDialog,
    weitereKartenebenenService: WeitereKartenebenenService
  ) {
    this.hasLayers$ = weitereKartenebenenService.weitereKartenebenen$.pipe(map(l => l.length > 0));
  }

  onOpenEditDialog(): void {
    this.dialog.open(WeitereKartenebenenDialogComponent, {
      width: '800px',
      disableClose: true,
    });
  }
}
