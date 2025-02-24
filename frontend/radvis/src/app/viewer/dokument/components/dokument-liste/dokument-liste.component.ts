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

import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, UntypedFormGroup } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';
import { Subscription } from 'rxjs';
import { RadvisValidators } from 'src/app/form-elements/models/radvis-validators';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { FileHandlingService } from 'src/app/shared/services/file-handling.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { DokumentListeView } from 'src/app/viewer/dokument/models/dokument-liste-view';
import { DokumentView } from 'src/app/viewer/dokument/models/dokument-view';
import { DokumentService } from 'src/app/viewer/dokument/services/dokument.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-dokument-liste',
  templateUrl: './dokument-liste.component.html',
  styleUrls: ['./dokument-liste.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DokumentListeComponent implements OnInit, OnDestroy {
  public static readonly DOKUMENTLISTE_DATA_KEY = 'dokumente';
  public dokumentListeView!: DokumentListeView;
  public formGroup: UntypedFormGroup;
  public uploading = false;
  private subscriptions: Subscription[] = [];

  constructor(
    private dokumentService: DokumentService,
    private fileHandlingService: FileHandlingService,
    private notifyUserService: NotifyUserService,
    private errorHandlingService: ErrorHandlingService,
    private changeDetectorRef: ChangeDetectorRef,
    private dialog: MatDialog,
    activatedRoute: ActivatedRoute
  ) {
    this.formGroup = new UntypedFormGroup({
      file: new UntypedFormControl(null, RadvisValidators.isNotNullOrEmpty),
    });

    this.subscriptions.push(
      activatedRoute.data.subscribe(data => {
        this.dokumentListeView = data.dokumente as DokumentListeView;
        invariant(this.dokumentListeView);
        this.resetForm();
      })
    );
  }

  onUpload(): void {
    if (!this.formGroup.valid) {
      this.notifyUserService.warn('Bitte wählen Sie eine Datei mit maximal 100MB.');
      return;
    }

    this.uploading = true;

    const command = {
      filename: this.formGroup.value.file.name,
    };

    this.dokumentService
      .addDokument(command, this.formGroup.value.file)
      .then(() => this.notifyUserService.inform(`Datei ${command.filename} wurde erfolgreich hochgeladen`))
      .then(() => this.aktualisiereDokumentenListe())
      .finally(() => {
        this.uploading = false;
        this.changeDetectorRef.detectChanges();
      });
  }

  onDownload(dokument: DokumentView): void {
    this.dokumentService
      .downloadDokument(dokument.dokumentId)
      .then(blob => {
        try {
          this.fileHandlingService.downloadInBrowser(blob, dokument.dateiname);
        } catch (err) {
          this.notifyUserService.warn('Die heruntergeladene Datei konnte nicht geöffnet werden.');
        }
      })
      .catch(err => this.errorHandlingService.handleError(err, 'Die Datei konnte nicht heruntergeladen werden.'));
  }

  onDelete(dokument: DokumentView): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        question: `Möchten Sie die Datei ${dokument.dateiname} wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.`,
        labelYes: 'Löschen',
        labelNo: 'Abbrechen',
      } as QuestionYesNo,
    });
    dialogRef.afterClosed().subscribe(yes => {
      if (yes) {
        this.dokumentService
          .deleteDokument(dokument.dokumentId)
          .then(() => {
            this.notifyUserService.inform(`Datei ${dokument.dateiname} wurde erfolgreich gelöscht.`);
            return this.aktualisiereDokumentenListe();
          })
          .finally(() => {
            this.uploading = false;
            this.changeDetectorRef.detectChanges();
          });
      }
    });
  }

  ngOnInit(): void {
    this.dokumentService.afterDokumentListeInit();
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  canDiscard(): boolean {
    return !this.formGroup.dirty;
  }

  resetForm(): void {
    this.formGroup.reset();
  }

  tooltipForDokument(dokument: DokumentView): string {
    return `Hochgeladen von ${dokument.benutzerVorname} ${dokument.benutzerNachname} am ${new DatePipe(
      'en-US'
    ).transform(new Date(dokument.datum), 'dd.MM.yyyy HH:mm')!}.`;
  }

  private aktualisiereDokumentenListe(): Promise<void> {
    this.resetForm();
    return this.dokumentService.getDokumentListe().then(dokumentListeView => {
      this.dokumentListeView = dokumentListeView;
    });
  }
}
