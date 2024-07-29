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
import { ChangeDetectionStrategy, ChangeDetectorRef, Component, OnDestroy } from '@angular/core';
import { FormControl, UntypedFormArray, UntypedFormGroup } from '@angular/forms';
import { interval, Subscription } from 'rxjs';
import { exhaustMap, startWith, take, takeWhile } from 'rxjs/operators';
import { MassnahmenImportAttribute } from 'src/app/import/massnahmen/models/massnahmen-import-attribute';
import { MassnahmenImportAttributeAuswaehlenCommand } from 'src/app/import/massnahmen/models/massnahmen-import-attribute-uebernehmen-command';
import { MassnahmenImportSessionView } from 'src/app/import/massnahmen/models/massnahmen-import-session-view';
import { MassnahmenImportService } from 'src/app/import/massnahmen/services/massnahmen-import.service';
import { MassnahmenImportRoutingService } from 'src/app/import/massnahmen/services/massnahmen-routing.service';
import { Severity } from 'src/app/import/models/import-session-view';
import { ErrorHandlingService } from 'src/app/shared/services/error-handling.service';
import { ManualRoutingService } from 'src/app/shared/services/manual-routing.service';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-import-massnahmen-attribute-auswaehlen',
  templateUrl: './import-massnahmen-attribute-auswaehlen.component.html',
  styleUrl: './import-massnahmen-attribute-auswaehlen.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportMassnahmenAttributeAuswaehlenComponent implements OnDestroy {
  private static readonly STEP = 2;
  session: MassnahmenImportSessionView | null = null;
  loading = false;

  massnahmenImportAlleAttribute = MassnahmenImportAttribute.alleAttribute;
  massnahmenImportPflichtAttributeOptions = MassnahmenImportAttribute.pflichtAttributeOptions;
  massnahmenImportOptionaleAttributeOptions = MassnahmenImportAttribute.optionaleAttributeOptions;

  formGroup: UntypedFormGroup;
  formControlMap: Map<MassnahmenImportAttribute, FormControl>;
  pollingSubscription: Subscription | undefined;
  executing: boolean = false;

  constructor(
    private massnahmenImportService: MassnahmenImportService,
    private massnahmenImportRoutingService: MassnahmenImportRoutingService,
    private manualRoutingService: ManualRoutingService,
    private errorHandlingService: ErrorHandlingService,
    private notifyUserService: NotifyUserService,
    private changeDetectorRef: ChangeDetectorRef
  ) {
    this.loading = true;

    this.formControlMap = new Map(
      this.massnahmenImportAlleAttribute.map(attribut => [attribut, new FormControl<boolean>(false)])
    );

    this.formGroup = new UntypedFormGroup({
      pflichtAttribute: new UntypedFormArray(
        MassnahmenImportAttribute.pflichtAttribute.map(attribut => this.formControlMap.get(attribut)!)
      ),
      optionaleAttribute: new UntypedFormArray(
        MassnahmenImportAttribute.optionaleAttribute.map(attribut => this.formControlMap.get(attribut)!)
      ),
    });

    this.massnahmenImportService.getImportSession().subscribe(session => {
      this.session = session;
      if (session) {
        if (this.schrittAbgeschlossen) {
          this.formGroup.disable();
          session.attribute.forEach(attribut => this.formControlMap.get(attribut)?.setValue(true));
        } else if (this.isAttributeValidierenRunning) {
          this.startPolling();
        }
        this.loading = false;
        this.changeDetectorRef.detectChanges();
      }
    });
  }

  private startPolling(): void {
    this.pollingSubscription = interval(MassnahmenImportService.POLLING_INTERVALL_IN_MILLISECONDS)
      .pipe(
        startWith(0),
        take(MassnahmenImportService.MAX_POLLING_CALLS),
        takeWhile(() => this.isAttributeValidierenRunning),
        exhaustMap(() => this.massnahmenImportService.getImportSession())
      )
      .subscribe({
        next: session => {
          this.session = session as MassnahmenImportSessionView;
          if (this.schrittAbgeschlossen) {
            this.navigateToNextStep();
          }
          if (this.schrittAbgeschlossenOderFehler) {
            this.executing = false;
          }
          this.changeDetectorRef.markForCheck();
        },
        error: () => {
          this.notifyUserService.warn('Fehler bei der Statusabfrage. Wurde der Import abgebrochen?');
          this.executing = false;
          this.changeDetectorRef.markForCheck();
        },
      });
  }

  get schrittAbgeschlossen(): boolean {
    return (this.session && this.session.schritt > ImportMassnahmenAttributeAuswaehlenComponent.STEP) ?? false;
  }

  onAlleMassnahmenAbwaehlen(): void {
    this.formControlMap.forEach(control => control.setValue(false));
  }

  onAlleMassnahmenAuswaehlen(): void {
    this.formControlMap.forEach(control => control.setValue(true));
  }

  onAbort(): void {
    this.massnahmenImportService.deleteImportSession().subscribe(() => {
      this.massnahmenImportRoutingService.navigateToFirst();
    });
  }

  onPrevious(): void {
    this.massnahmenImportRoutingService.navigateToPrevious(ImportMassnahmenAttributeAuswaehlenComponent.STEP);
  }

  onNext(): void {
    this.navigateToNextStep();
  }

  onStart(): void {
    invariant(!this.schrittAbgeschlossen);

    this.formGroup.disable();

    const attribute: string[] = [];
    this.formControlMap.forEach((control, attribut) => {
      if (control.value) {
        attribute.push(attribut);
      }
    });

    this.executing = true;

    this.massnahmenImportService
      .attributeAuswaehlen({
        attribute: attribute,
      } as MassnahmenImportAttributeAuswaehlenCommand)
      .subscribe({
        next: () => {
          if (this.session) {
            this.session.executing = true;
          }
          this.startPolling();
        },
        error: err => {
          this.errorHandlingService.handleHttpError(err);
          this.formGroup.enable();
          this.changeDetectorRef.markForCheck();
          this.executing = false;
        },
      });
  }

  get anyAttributSelected(): boolean {
    return (
      this.formGroup.value.pflichtAttribute.some((val: boolean) => val) ||
      this.formGroup.value.optionaleAttribute.some((val: boolean) => val)
    );
  }

  private navigateToNextStep(): void {
    this.massnahmenImportRoutingService.navigateToNext(ImportMassnahmenAttributeAuswaehlenComponent.STEP);
  }

  get isAttributeValidierenRunning(): boolean {
    return this.session?.schritt === ImportMassnahmenAttributeAuswaehlenComponent.STEP && this.session?.executing;
  }

  get schrittAbgeschlossenOderFehler(): boolean {
    return this.schrittAbgeschlossen || this.hasFehler;
  }

  get fehler(): string[] {
    return this.session?.log.filter(l => l.severity === Severity.ERROR).map(l => l.fehlerBeschreibung) || [];
  }

  get hasFehler(): boolean {
    return this.fehler.length > 0;
  }

  openHandbuch(): void {
    this.manualRoutingService.openManualPflichtattribute();
  }

  ngOnDestroy(): void {
    this.pollingSubscription?.unsubscribe();
  }
}
