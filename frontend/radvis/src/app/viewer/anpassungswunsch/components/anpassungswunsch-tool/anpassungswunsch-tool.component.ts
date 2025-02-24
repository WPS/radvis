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

import { ChangeDetectionStrategy, Component, forwardRef, HostListener } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute, IsActiveMatchOptions } from '@angular/router';
import {
  ConfirmationDialogComponent,
  QuestionYesNo,
} from 'src/app/shared/components/confirmation-dialog/confirmation-dialog.component';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { ANPASSUNGSWUNSCH } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch.infrastruktur';
import { AnpassungenRoutingService } from 'src/app/viewer/anpassungswunsch/services/anpassungen-routing.service';
import { AnpassungswunschFilterService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch-filter.service';
import { AnpassungswunschService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch.service';
import { AddKommentarCommand } from 'src/app/viewer/kommentare/models/add-kommentar-command';
import { Kommentar } from 'src/app/viewer/kommentare/models/kommentar';
import { KommentarService } from 'src/app/viewer/kommentare/services/kommentar.service';
import { InfrastrukturenSelektionService } from 'src/app/viewer/viewer-shared/services/infrastrukturen-selektion.service';
import { ViewerRoutingService } from 'src/app/viewer/viewer-shared/services/viewer-routing.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-anpassungswunsch-tool',
  templateUrl: './anpassungswunsch-tool.component.html',
  styleUrls: ['./anpassungswunsch-tool.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: KommentarService,
      useExisting: forwardRef(() => AnpassungswunschToolComponent),
    },
  ],
  standalone: false,
})
export class AnpassungswunschToolComponent implements KommentarService {
  eigenschaftenRoute = './' + AnpassungenRoutingService.EIGENSCHAFTEN;
  kommentarRoute = './' + AnpassungenRoutingService.KOMMENTARE;

  routerLinkActiveOptions: IsActiveMatchOptions = {
    fragment: 'exact',
    matrixParams: 'exact',
    paths: 'exact',
    queryParams: 'ignored',
  };

  private get anpassungswunschId(): number {
    const idAusRoute = this.activatedRoute.snapshot.paramMap.get('id');
    invariant(idAusRoute);
    return +idAusRoute;
  }

  constructor(
    private viewerRoutingService: ViewerRoutingService,
    private anpassungswunschService: AnpassungswunschService,
    private anpassungswunschFilterService: AnpassungswunschFilterService,
    private activatedRoute: ActivatedRoute,
    private infrastrukturenSelektionService: InfrastrukturenSelektionService,
    private dialog: MatDialog,
    private featureTogglzService: FeatureTogglzService
  ) {
    this.infrastrukturenSelektionService.selectInfrastrukturen(ANPASSUNGSWUNSCH);
  }

  @HostListener('keydown.escape')
  public onEscape(): void {
    this.onClose();
  }

  public addKommentar(command: AddKommentarCommand): Promise<Kommentar[]> {
    return this.anpassungswunschService.addKommentar(this.anpassungswunschId, command);
  }

  onClose(): void {
    this.viewerRoutingService.toViewer();
  }

  onDelete(): void {
    const dialogRef = this.dialog.open(ConfirmationDialogComponent, {
      data: {
        question: `Möchten Sie den Anpassungswunsch wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.`,
        labelYes: 'Löschen',
        labelNo: 'Abbrechen',
        inverseButtonColorCoding: true,
        title: 'Achtung',
      } as QuestionYesNo,
    });
    dialogRef.afterClosed().subscribe(yes => {
      if (yes) {
        this.anpassungswunschService.delete(this.anpassungswunschId).then(() => {
          this.anpassungswunschFilterService.refetchData();
          this.onClose();
        });
      }
    });
  }

  get isLoeschenVonAnpassungswuenschenToggleOn(): boolean {
    return this.featureTogglzService.isToggledOn(FeatureTogglzService.TOGGLZ_LOESCHEN_VON_ANPASSUNGSWUENSCHEN);
  }
}
