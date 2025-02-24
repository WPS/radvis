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
import { ServicestationRoutingService } from 'src/app/viewer/servicestation/services/servicestation-routing.service';
import { SERVICESTATIONEN } from 'src/app/viewer/servicestation/models/servicestation.infrastruktur';
import { Servicestation } from 'src/app/viewer/servicestation/models/servicestation';
import { ServicestationService } from 'src/app/viewer/servicestation/services/servicestation.service';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { DeleteServicestationCommand } from 'src/app/viewer/servicestation/models/delete-servicestation-command';
import { ServicestationFilterService } from 'src/app/viewer/servicestation/services/servicestation-filter.service';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { ServicestationUpdatedService } from 'src/app/viewer/servicestation/services/servicestation-updated.service';

@Component({
  selector: 'rad-servicestation-tool',
  templateUrl: './servicestation-tool.component.html',
  styleUrls: ['./servicestation-tool.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: DokumentService,
      useExisting: forwardRef(() => ServicestationToolComponent),
    },
    {
      provide: ServicestationUpdatedService,
      useExisting: forwardRef(() => ServicestationToolComponent),
    },
  ],
  standalone: false,
})
export class ServicestationToolComponent implements OnDestroy, DokumentService, ServicestationUpdatedService {
  eigenschaftenRoute = './' + ServicestationRoutingService.EIGENSCHAFTEN;
  servicestationDateiRoute = './' + ServicestationRoutingService.DATEIEN;

  routerLinkActiveOptions: IsActiveMatchOptions = {
    fragment: 'exact',
    matrixParams: 'exact',
    paths: 'exact',
    queryParams: 'ignored',
  };

  servicestation: Servicestation | null = null;

  private subscriptions: Subscription[] = [];

  private get selectedServicestationID(): number {
    const idAusRoute = this.activatedRoute.snapshot.paramMap.get('id');
    invariant(idAusRoute);
    return +idAusRoute;
  }

  constructor(
    private activatedRoute: ActivatedRoute,
    private viewerRoutingService: ViewerRoutingService,
    private infrastrukturenSelektionService: InfrastrukturenSelektionService,
    private servicestationService: ServicestationService,
    private servicestationFilterService: ServicestationFilterService,
    private dialog: MatDialog,
    private changeDetectorRef: ChangeDetectorRef,
    private olMapService: OlMapService,
    private errorHandlingService: ErrorHandlingService
  ) {
    this.subscriptions.push(
      activatedRoute.data.subscribe(data => {
        this.servicestation = data.servicestation;
        invariant(this.servicestation);
        this.focusServicestationIntoView();
      })
    );
    this.infrastrukturenSelektionService.selectInfrastrukturen(SERVICESTATIONEN);
  }

  afterDokumentListeInit(): void {
    this.changeDetectorRef.detectChanges();
  }

  getDokumentListe(): Promise<DokumentListeView> {
    return this.servicestationService.getDokumentListe(this.selectedServicestationID);
  }

  addDokument(command: AddDokumentCommand, file: File): Promise<void> {
    return this.servicestationService.uploadFile(this.selectedServicestationID, command, file);
  }

  downloadDokument(dokumentId: number): Promise<Blob> {
    return this.servicestationService.downloadFile(this.selectedServicestationID, dokumentId);
  }

  deleteDokument(dokumentId: number): Promise<void> {
    return this.servicestationService.deleteFile(this.selectedServicestationID, dokumentId);
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  onDeleteServicestation(): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        question: `Möchten Sie die Servicestation wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.`,
        labelYes: 'Löschen',
        labelNo: 'Abbrechen',
        inverseButtonColorCoding: true,
        title: 'Achtung',
      } as QuestionYesNo,
    });
    dialogRef.afterClosed().subscribe(yes => {
      if (yes) {
        invariant(this.servicestation);
        const id = this.servicestation.id;
        const deleteServicestationCommand: DeleteServicestationCommand = {
          version: this.servicestation.version,
        };
        this.servicestationService
          .delete(id, deleteServicestationCommand)
          .then(() => {
            this.servicestationFilterService.onServicestationDeleted(id);
            // zusaetzlich refetchData, um insgesamt die Massnahmen aktuell zu halten
            this.servicestationFilterService.refetchData();
            this.onClose();
          })
          .catch(error => this.errorHandlingService.handleHttpError(error));
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  public focusServicestationIntoView(): void {
    const toFocus = this.servicestation?.geometrie.coordinates;

    if (toFocus) {
      this.olMapService.scrollIntoViewByCoordinate(toFocus);
    }
  }

  updateServicestation(): void {
    invariant(this.servicestation?.id);
    this.servicestationService
      .get(this.servicestation.id)
      .then(servicestation => this.setServicestationAndFocus(servicestation));
  }

  private setServicestationAndFocus(servicestation: Servicestation): void {
    invariant(servicestation);
    this.servicestation = servicestation;
    this.focusServicestationIntoView();
  }
}
