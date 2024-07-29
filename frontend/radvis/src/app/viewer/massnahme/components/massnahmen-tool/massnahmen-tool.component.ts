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
  ChangeDetectorRef,
  Component,
  forwardRef,
  HostListener,
  OnDestroy,
} from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, IsActiveMatchOptions } from '@angular/router';
import { BehaviorSubject, Subscription } from 'rxjs';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { OlMapService } from 'src/app/shared/services/ol-map.service';
import { AddKommentarCommand } from 'src/app/viewer/kommentare/models/add-kommentar-command';
import { Kommentar } from 'src/app/viewer/kommentare/models/kommentar';
import { KommentarService } from 'src/app/viewer/kommentare/services/kommentar.service';
import { DeleteMassnahmeCommand } from 'src/app/viewer/massnahme/models/delete-massnahme-command';
import { MassnahmeToolView } from 'src/app/viewer/massnahme/models/massnahme-tool-view';
import { MASSNAHMEN } from 'src/app/viewer/massnahme/models/massnahme.infrastruktur';
import { MassnahmeFilterService } from 'src/app/viewer/massnahme/services/massnahme-filter.service';
import { MassnahmeNetzbezugDisplayService } from 'src/app/viewer/massnahme/services/massnahme-netzbezug-display.service';
import { MassnahmeUpdatedService } from 'src/app/viewer/massnahme/services/massnahme-updated.service';
import { MassnahmeService } from 'src/app/viewer/massnahme/services/massnahme.service';
import { MassnahmenRoutingService } from 'src/app/viewer/massnahme/services/massnahmen-routing.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import invariant from 'tiny-invariant';
import { DokumentService } from 'src/app/viewer/dokument/services/dokument.service';
import { DokumentListeView } from 'src/app/viewer/dokument/models/dokument-liste-view';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { AddDokumentCommand } from 'src/app/viewer/dokument/models/add-dokument-command';

@Component({
  selector: 'rad-massnahmen-tool',
  templateUrl: './massnahmen-tool.component.html',
  styleUrls: ['./massnahmen-tool.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: MassnahmeUpdatedService,
      useExisting: forwardRef(() => MassnahmenToolComponent),
    },
    {
      provide: KommentarService,
      useExisting: forwardRef(() => MassnahmenToolComponent),
    },
    {
      provide: DokumentService,
      useExisting: forwardRef(() => MassnahmenToolComponent),
    },
    { provide: MassnahmeNetzbezugDisplayService, useExisting: forwardRef(() => MassnahmenToolComponent) },
  ],
})
export class MassnahmenToolComponent
  implements MassnahmeUpdatedService, MassnahmeNetzbezugDisplayService, OnDestroy, KommentarService, DokumentService
{
  eigenschaftenRoute = './' + MassnahmenRoutingService.EIGENSCHAFTEN;
  umsetzungsstandRoute = './' + MassnahmenRoutingService.UMSETZUNGSSTAND;
  massnahmenDateiRoute = './' + MassnahmenRoutingService.DATEIEN;
  massnahmenKommentarRoute = './' + MassnahmenRoutingService.KOMMENTARE;

  routerLinkActiveOptions: IsActiveMatchOptions = {
    fragment: 'exact',
    matrixParams: 'exact',
    paths: 'exact',
    queryParams: 'ignored',
  };

  benachrichtigungAktiv: boolean | null = null;

  massnahmeToolView: MassnahmeToolView | null = null;

  originalGeometrieAnzeigen = false;
  isNetzbezugVisible$ = new BehaviorSubject<boolean>(true);
  massnahmenLayerId = MASSNAHMEN.name;

  private subscriptions: Subscription[] = [];

  private get selectedMassnahmenID(): number {
    const idAusRoute = this.activatedRoute.snapshot.paramMap.get('id');
    invariant(idAusRoute);
    return +idAusRoute;
  }

  constructor(
    private activatedRoute: ActivatedRoute,
    private viewerRoutingService: ViewerRoutingService,
    private massnahmenRoutingService: MassnahmenRoutingService,
    private infrastrukturenSelektionService: InfrastrukturenSelektionService,
    private massnahmeService: MassnahmeService,
    private notifyUserService: NotifyUserService,
    private massnahmeFilterService: MassnahmeFilterService,
    private dialog: MatDialog,
    private errorHandlingService: ErrorHandlingService,
    private fileHandlingService: FileHandlingService,
    private changeDetectorRef: ChangeDetectorRef,
    private olMapService: OlMapService
  ) {
    this.subscriptions.push(
      activatedRoute.data.subscribe(data => {
        this.massnahmeToolView = data.massnahme;
        invariant(this.massnahmeToolView);
        this.massnahmeService.getBenachrichtigungsFunktion(this.massnahmeToolView.id).then(aktiv => {
          this.benachrichtigungAktiv = aktiv;
          changeDetectorRef.detectChanges();
        });
        this.focusMassnahmeIntoView();
      })
    );
    this.infrastrukturenSelektionService.selectInfrastrukturen(MASSNAHMEN);
  }

  @HostListener('keydown.escape')
  public onEscape(): void {
    this.onClose();
  }

  afterDokumentListeInit(): void {
    this.showNetzbezug(true);
    this.changeDetectorRef.detectChanges();
  }

  getDokumentListe(): Promise<DokumentListeView> {
    return this.massnahmeService.getDokumentListe(this.selectedMassnahmenID);
  }

  addDokument(command: AddDokumentCommand, file: File): Promise<void> {
    return this.massnahmeService.uploadFileToMassnahme(this.selectedMassnahmenID, command, file);
  }

  downloadDokument(dokumentId: number): Promise<Blob> {
    return this.massnahmeService.downloadFile(this.selectedMassnahmenID, dokumentId);
  }

  deleteDokument(dokumentId: number): Promise<void> {
    return this.massnahmeService.deleteFile(this.selectedMassnahmenID, dokumentId);
  }

  public addKommentar(command: AddKommentarCommand): Promise<Kommentar[]> {
    return this.massnahmeService.addKommentar(this.selectedMassnahmenID, command);
  }

  public updateMassnahme(): void {
    const idFromRoute = this.massnahmenRoutingService.getIdFromRoute();
    invariant(idFromRoute);
    this.massnahmeService.getMassnahmeToolView(idFromRoute).then(massnahmeToolView => {
      this.massnahmeToolView = massnahmeToolView;
      invariant(this.massnahmeToolView);
      this.focusMassnahmeIntoView();
      this.changeDetectorRef.detectChanges();
    });
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  showNetzbezug(isNetzbezugVisible: boolean): void {
    this.isNetzbezugVisible$.next(isNetzbezugVisible);
  }

  onChangeBenachrichtigung(event: boolean): void {
    const idFromRoute = this.massnahmenRoutingService.getIdFromRoute();
    invariant(idFromRoute);
    this.massnahmeService.stelleBenachrichtigungsFunktionEin(idFromRoute, event).then(aktiv => {
      this.notifyUserService.inform(
        `Benachrichtigungen bei Änderungen an der Maßnahme wurden ${aktiv ? 'aktiviert' : 'deaktiviert'}`
      );
      this.benachrichtigungAktiv = aktiv;
      this.changeDetectorRef.detectChanges();
    });
  }

  onChangeOriginalGeometrie(event: boolean): void {
    this.originalGeometrieAnzeigen = event;
  }

  onDeleteMassnahme(): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        question: `Möchten Sie die Massnahme wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.`,
        labelYes: 'Löschen',
        labelNo: 'Abbrechen',
        inverseButtonColorCoding: true,
        title: 'Achtung',
      } as QuestionYesNo,
    });
    dialogRef.afterClosed().subscribe(yes => {
      if (yes) {
        invariant(this.massnahmeToolView);
        const deleteMassnahmeCommand: DeleteMassnahmeCommand = {
          id: this.massnahmeToolView.id,
          version: this.massnahmeToolView.version,
        };

        this.massnahmeService.delete(deleteMassnahmeCommand).then(() => {
          this.massnahmeFilterService.onMassnahmeDeleted(deleteMassnahmeCommand.id);
          // zusaetzlich refetchData, um insgesamt die Massnahmen aktuell zu halten
          this.massnahmeFilterService.refetchData();
          this.onClose();
        });
      }
    });
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  public focusMassnahmeIntoView(): void {
    const toFocus =
      this.massnahmeToolView?.netzbezug?.kantenBezug[0]?.geometrie.coordinates[0] ||
      this.massnahmeToolView?.netzbezug?.knotenBezug[0]?.geometrie.coordinates ||
      this.massnahmeToolView?.netzbezug?.punktuellerKantenBezug[0]?.geometrie.coordinates;

    if (toFocus) {
      this.olMapService.scrollIntoViewByCoordinate(toFocus);
    }
  }
}
