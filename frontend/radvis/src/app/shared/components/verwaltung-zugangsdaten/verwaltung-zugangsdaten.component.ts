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
import { ChangeDetectionStrategy, ChangeDetectorRef, Component } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { RegenerateCredentialsConfirmComponent } from 'src/app/shared/components/regenerate-credentials-confirm/regenerate-credentials-confirm.component';
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { VerwaltungZugangsdatenService } from 'src/app/shared/services/verwaltung-zugangsdaten.service';
import { MatomoTracker } from 'ngx-matomo-client';

@Component({
  selector: 'rad-verwaltung-zugangsdaten',
  templateUrl: './verwaltung-zugangsdaten.component.html',
  styleUrls: ['./verwaltung-zugangsdaten.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class VerwaltungZugangsdatenComponent {
  public benutzername: string | undefined;
  public passwort: string | undefined;

  public isGeneratingPasswort = false;

  constructor(
    private manualRoutingService: ManualRoutingService,
    private dialog: MatDialog,
    private changeDetector: ChangeDetectorRef,
    private verwaltungZugangsdatenService: VerwaltungZugangsdatenService,
    private notifyUserService: NotifyUserService,
    private matomoTracker: MatomoTracker,
    benutzerDetailsService: BenutzerDetailsService
  ) {
    this.benutzername = benutzerDetailsService.aktuellerBenutzerBasicAuthAnmeldename();
  }

  onGenerateZugangsdaten(): void {
    const dialogRef = this.dialog.open(RegenerateCredentialsConfirmComponent);

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Der Benutzer hat auf "Generieren" geklickt
        this.matomoTracker.trackEvent('Zugangsdaten', 'Generieren', 'Basic-Auth-Token');

        this.isGeneratingPasswort = true;
        this.changeDetector.markForCheck();

        this.verwaltungZugangsdatenService
          .generate()
          .then(benutzerAuthView => {
            this.benutzername = benutzerAuthView.benutzername;
            this.passwort = benutzerAuthView.passwort;
          })
          .finally(() => {
            this.isGeneratingPasswort = false;
            this.changeDetector.markForCheck();
          });
      } else {
        // Der Benutzer hat auf "Abbrechen" geklickt oder den Dialog anderweitig geschlossen
        // -> Es wird kein neues Passwort generiert
      }
    });
  }

  openHandbuchSchnittstelleWmsWfs(): void {
    this.manualRoutingService.openManualWmsWfsSchnittstelle();
  }

  onCopyButtonClicked(): void {
    this.notifyUserService.inform('Passwort wurde in die Zwischenablage kopiert.');
  }
}
