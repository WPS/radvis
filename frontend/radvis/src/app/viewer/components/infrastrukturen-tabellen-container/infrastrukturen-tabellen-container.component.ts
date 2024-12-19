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
import {
  ChangeDetectionStrategy,
  Component,
  ComponentRef,
  Inject,
  OnDestroy,
  Optional,
  ViewContainerRef,
} from '@angular/core';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { Subscription } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { InfrastrukturenTabellenComponent } from 'src/app/viewer/components/infrastruktur-tabellen/infrastrukturen-tabellen.component';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';

interface InfrastrukturenTabellenDialogData {
  asDialog: boolean;
  componentRef: ComponentRef<InfrastrukturenTabellenComponent>;
}

@Component({
  selector: 'rad-infrastrukturen-tabellen-container',
  templateUrl: './infrastrukturen-tabellen-container.component.html',
  styleUrl: './infrastrukturen-tabellen-container.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InfrastrukturenTabellenContainerComponent implements OnDestroy {
  infrastrukturenTabellenRef: ComponentRef<InfrastrukturenTabellenComponent>;
  subscriptions: Subscription[] = [];

  constructor(
    private viewContainer: ViewContainerRef,
    matDialog: MatDialog,
    infrastrukturenSelectionService: InfrastrukturenSelektionService,
    @Inject(MAT_DIALOG_DATA) @Optional() private matDialogData?: InfrastrukturenTabellenDialogData,
    @Optional() private matDialogRef?: MatDialogRef<InfrastrukturenTabellenContainerComponent>
  ) {
    if (matDialogData) {
      this.infrastrukturenTabellenRef = matDialogData.componentRef;
      viewContainer.insert(this.infrastrukturenTabellenRef.hostView);
    } else {
      this.infrastrukturenTabellenRef = viewContainer.createComponent(InfrastrukturenTabellenComponent);
    }
    this.subscriptions.push(
      this.infrastrukturenTabellenRef.instance.showFullscreen.subscribe(() => {
        this.infrastrukturenTabellenRef.setInput('asDialog', true);
        matDialog
          .open<InfrastrukturenTabellenContainerComponent, InfrastrukturenTabellenDialogData>(
            InfrastrukturenTabellenContainerComponent,
            {
              data: { asDialog: true, componentRef: this.infrastrukturenTabellenRef },
              width: '90vw',
              height: '90vh',
              disableClose: true,
            }
          )
          .afterClosed()
          .subscribe(() => {
            viewContainer.insert(this.infrastrukturenTabellenRef.hostView);
            this.infrastrukturenTabellenRef.setInput('asDialog', false);
          });
      }),
      infrastrukturenSelectionService.selektierteInfrastrukturen$
        .pipe(
          map(i => i.length > 0),
          distinctUntilChanged()
        )
        .subscribe(visible => {
          if (visible) {
            this.viewContainer.insert(this.infrastrukturenTabellenRef.hostView);
          } else {
            this.viewContainer.detach(0);
          }
        })
    );
  }
  get asDialog(): boolean {
    return this.matDialogData?.asDialog ?? false;
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
    if (!this.matDialogData) {
      this.infrastrukturenTabellenRef.destroy();
    }
  }

  onCloseDialog(): void {
    this.viewContainer.detach(0);
    this.matDialogRef?.close();
  }
}
