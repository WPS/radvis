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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef, OnDestroy } from '@angular/core';
import { DokumentService } from 'src/app/viewer/dokument/services/dokument.service';
import { ActivatedRoute, IsActiveMatchOptions } from '@angular/router';
import { Subscription } from 'rxjs';
import invariant from 'tiny-invariant';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { MatDialog } from '@angular/material/dialog';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { DokumentListeView } from 'src/app/viewer/dokument/models/dokument-liste-view';
import { AddDokumentCommand } from 'src/app/viewer/dokument/models/add-dokument-command';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { AbstellanlageRoutingService } from 'src/app/viewer/abstellanlage/services/abstellanlage-routing.service';
import { Abstellanlage } from 'src/app/viewer/abstellanlage/models/abstellanlage';
import { AbstellanlageService } from 'src/app/viewer/abstellanlage/services/abstellanlage.service';
import { ABSTELLANLAGEN } from 'src/app/viewer/abstellanlage/models/abstellanlage.infrastruktur';
import { DeleteAbstellanlageCommand } from 'src/app/viewer/abstellanlage/models/delete-abstellanlage-command';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { AbstellanlageFilterService } from 'src/app/viewer/abstellanlage/services/abstellanlage-filter.service';
import { AbstellanlageUpdatedService } from 'src/app/viewer/abstellanlage/services/abstellanlage-updated.service';

@Component({
  templateUrl: './abstellanlage-tool.component.html',
  styleUrls: ['./abstellanlage-tool.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: DokumentService,
      useExisting: forwardRef(() => AbstellanlageToolComponent),
    },
    {
      provide: AbstellanlageUpdatedService,
      useExisting: forwardRef(() => AbstellanlageToolComponent),
    },
  ],
})
export class AbstellanlageToolComponent implements OnDestroy, DokumentService, AbstellanlageUpdatedService {
  eigenschaftenRoute = './' + AbstellanlageRoutingService.EIGENSCHAFTEN;
  dateiRoute = './' + AbstellanlageRoutingService.DATEIEN;

  routerLinkActiveOptions: IsActiveMatchOptions = {
    fragment: 'exact',
    matrixParams: 'exact',
    paths: 'exact',
    queryParams: 'ignored',
  };

  abstellanlage: Abstellanlage | null = null;

  private subscriptions: Subscription[] = [];

  private get selectedAbstellanlageID(): number {
    const idAusRoute = this.activatedRoute.snapshot.paramMap.get('id');
    invariant(idAusRoute);
    return +idAusRoute;
  }

  constructor(
    private activatedRoute: ActivatedRoute,
    private viewerRoutingService: ViewerRoutingService,
    private infrastrukturenSelektionService: InfrastrukturenSelektionService,
    private abstellanlageService: AbstellanlageService,
    private abstellanlageFilterService: AbstellanlageFilterService,
    private errorHandlingService: ErrorHandlingService,
    private dialog: MatDialog,
    private changeDetectorRef: ChangeDetectorRef,
    private olMapService: OlMapService
  ) {
    this.subscriptions.push(
      activatedRoute.data.subscribe(data => {
        this.setAbstellanlageAndFocus(data.abstellanlage);
      })
    );
    this.infrastrukturenSelektionService.selectInfrastrukturen(ABSTELLANLAGEN);
  }

  afterDokumentListeInit(): void {
    this.changeDetectorRef.detectChanges();
  }

  getDokumentListe(): Promise<DokumentListeView> {
    return this.abstellanlageService.getDokumentListe(this.selectedAbstellanlageID);
  }

  addDokument(command: AddDokumentCommand, file: File): Promise<void> {
    return this.abstellanlageService.uploadFile(this.selectedAbstellanlageID, command, file);
  }

  downloadDokument(dokumentId: number): Promise<Blob> {
    return this.abstellanlageService.downloadFile(this.selectedAbstellanlageID, dokumentId);
  }

  deleteDokument(dokumentId: number): Promise<void> {
    return this.abstellanlageService.deleteFile(this.selectedAbstellanlageID, dokumentId);
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  onDeleteAbstellanlage(): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        question: `Möchten Sie die Abstellanlage wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.`,
        labelYes: 'Löschen',
        labelNo: 'Abbrechen',
        inverseButtonColorCoding: true,
        title: 'Achtung',
      } as QuestionYesNo,
    });
    dialogRef.afterClosed().subscribe(yes => {
      if (yes) {
        invariant(this.abstellanlage);
        const id = this.abstellanlage.id;
        const deleteAbstellanlageCommand: DeleteAbstellanlageCommand = {
          version: this.abstellanlage.version,
        };
        this.abstellanlageService
          .delete(id, deleteAbstellanlageCommand)
          .then(() => {
            this.abstellanlageFilterService.onAbstellanlageDeleted(id);
            // zusaetzlich refetchData, um insgesamt die Massnahmen aktuell zu halten
            this.abstellanlageFilterService.refetchData();
            this.onClose();
          })
          .catch(error => this.errorHandlingService.handleHttpError(error));
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  public focusAbstellanlageIntoView(): void {
    const toFocus = this.abstellanlage?.geometrie.coordinates;

    if (toFocus) {
      this.olMapService.scrollIntoViewByCoordinate(toFocus);
    }
  }

  public updateAbstellanlage(): void {
    invariant(this.abstellanlage?.id);
    this.abstellanlageService
      .get(this.abstellanlage.id)
      .then(abstellanlage => this.setAbstellanlageAndFocus(abstellanlage));
  }

  private setAbstellanlageAndFocus(abstellanlage: Abstellanlage): void {
    invariant(abstellanlage);
    this.abstellanlage = abstellanlage;
    this.focusAbstellanlageIntoView();
  }
}
