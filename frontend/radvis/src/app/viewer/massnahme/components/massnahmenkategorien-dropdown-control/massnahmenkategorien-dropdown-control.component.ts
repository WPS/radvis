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

import { ChangeDetectionStrategy, ChangeDetectorRef, Component, forwardRef, OnDestroy, OnInit } from '@angular/core';
import { UntypedFormControl, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatAutocompleteSelectedEvent, MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { Subject, Subscription } from 'rxjs';
import { delay, first } from 'rxjs/operators';
import { AbstractFormControl } from 'src/app/form-elements/components/abstract-form-control';
import {
  MassnahmenkategorieGruppe,
  MassnahmenkategorieOptionGroup,
} from 'src/app/viewer/massnahme/models/massnahmenkategorie-option-group';
import { MASSNAHMENKATEGORIEN, Massnahmenkategorien } from 'src/app/viewer/massnahme/models/massnahmenkategorien';

@Component({
  selector: 'rad-massnahmenkategorien-dropdown-control',
  templateUrl: './massnahmenkategorien-dropdown-control.component.html',
  styleUrls: ['./massnahmenkategorien-dropdown-control.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => MassnahmenkategorienDropdownControlComponent),
      multi: true,
    },
  ],
})
export class MassnahmenkategorienDropdownControlComponent
  extends AbstractFormControl<string[]>
  implements OnInit, OnDestroy {
  private static CLICKED_KATEGORIE_CLASS = 'clicked-kategorie';

  placeholder = 'Kategorie wählen';

  groupedOptions = MASSNAHMENKATEGORIEN;

  selectedOptions: string[] = [];

  formControl: UntypedFormControl;
  filteredGroupedOptions: MassnahmenkategorieOptionGroup[] = [];

  public displayFn = Massnahmenkategorien.getDisplayTextForMassnahmenKategorie;
  public displayOberkategorie = Massnahmenkategorien.getDisplayTextForOberkategorieVonKategorie;

  public disabled = false;

  private panelOpenSubject = new Subject<void>();
  private subscriptions: Subscription[] = [];

  constructor(private changeDetector: ChangeDetectorRef) {
    super();
    this.formControl = new UntypedFormControl(null);
    this.formControl.valueChanges.subscribe(searchTerm => {
      this.updateFilteredOptions(searchTerm);
      this.changeDetector.markForCheck();
    });
  }

  ngOnInit(): void {
    this.filteredGroupedOptions = this.groupedOptions;
  }

  ngOnDestroy(): void {
    this.subscriptions.forEach(s => s.unsubscribe());
  }

  public writeValue(value: string[]): void {
    this.formControl.reset(value, { emitEvent: false });
    this.selectedOptions = value;
    this.changeDetector.markForCheck();
  }

  onOptionSelected(event: MatAutocompleteSelectedEvent, ele: HTMLInputElement): void {
    const value = event.option.value;
    if (!this.selectedOptions.includes(value)) {
      this.selectedOptions.push(value);
      this.onChange(this.selectedOptions);
      ele.value = '';
    }
  }

  onRemoved(kategorie: string): void {
    this.selectedOptions = this.selectedOptions.filter(kat => kat !== kategorie);
    this.onChange(this.selectedOptions);
    this.updateFilteredOptions();
  }

  // Verlässt man das Formularfeld ohne einen Wert auszuwählen, so wird die zuletzt ausgewählte Option wieder gesetzt
  onBlur(ele: HTMLInputElement): void {
    // this.writeValue(this.selectedOptions);
    ele.value = '';
  }

  // Das Standardverhalten von Autocomplete bei Klick wird hier überschrieben, weil sonst bei Auswahl eines Wertes und
  // erneutem Klick auf das Formularfeld das Options-Panel nicht mehr angezeigt wird
  onClick(event: Event, trigger: MatAutocompleteTrigger): void {
    if (this.disabled) {
      return;
    }
    this.formControl.reset('');
    event.stopPropagation();
    trigger.openPanel();
  }

  public setDisabledState(isDisabled: boolean): void {
    if (isDisabled) {
      this.formControl.disable({ emitEvent: false });
    } else {
      this.formControl.enable({ emitEvent: false });
    }
    this.disabled = isDisabled;
    this.changeDetector.markForCheck();
  }

  onPanelClosed(): void {
    this.removeKategorieHighlightClass();
  }

  onChipClick(kategorie: string, trigger: MatAutocompleteTrigger): void {
    if (this.disabled) {
      return;
    }
    this.updateFilteredOptions('');
    this.changeDetector.detectChanges();

    if (!trigger.panelOpen) {
      // first(), da uns späteres öffnen nicht interessiert
      // delay(10), da der DOM sonst noch nicht fertig war. Ist bisschen unschön, da eine magic number zu raten
      this.subscriptions.push(
        this.panelOpenSubject.pipe(first(), delay(50)).subscribe(() => {
          const panelElement: HTMLElement = trigger.autocomplete.panel?.nativeElement;
          this.scrollOptionIntoView(panelElement, kategorie);
        })
      );
      trigger.openPanel();
    } else {
      const panelElement: HTMLElement = trigger.autocomplete.panel?.nativeElement;
      this.scrollOptionIntoView(panelElement, kategorie);
    }
  }

  onPanelOpened(): void {
    this.panelOpenSubject.next();
  }

  public getElementIdByKategorie(kategorie: string): string {
    return 'kategorie-option-' + kategorie;
  }

  private removeKategorieHighlightClass(): void {
    document
      .querySelectorAll(`.${MassnahmenkategorienDropdownControlComponent.CLICKED_KATEGORIE_CLASS}`)
      .forEach(el => {
        el.classList.remove(MassnahmenkategorienDropdownControlComponent.CLICKED_KATEGORIE_CLASS);
      });
  }

  private scrollOptionIntoView = (panelElement: HTMLElement, kategorie: string): void => {
    if (panelElement) {
      this.removeKategorieHighlightClass();
      const matOption = panelElement.querySelector(`#${this.getElementIdByKategorie(kategorie)}`);
      if (matOption) {
        matOption.classList.add(MassnahmenkategorienDropdownControlComponent.CLICKED_KATEGORIE_CLASS);
        matOption.scrollIntoView();
      }
    }
  };

  private updateFilteredOptions(term?: string): void {
    this.filteredGroupedOptions = term
      ? (this.groupedOptions
          .map(gruppe => this.filterOberGruppe(gruppe, term))
          .filter(value => value !== null && value !== undefined) as MassnahmenkategorieOptionGroup[])
      : this.groupedOptions;
  }

  private filterOberGruppe(list: MassnahmenkategorieOptionGroup, term: string): MassnahmenkategorieOptionGroup | null {
    if (list.displayText.toLowerCase().includes(term.toLowerCase())) {
      return list;
    }

    const options = list.options
      .map(gruppe => this.filterGruppe(gruppe, term))
      .filter(value => value !== null && value !== undefined) as MassnahmenkategorieGruppe[];

    return options.length > 0 ? { ...list, options } : null;
  }

  private filterGruppe(list: MassnahmenkategorieGruppe, term: string): MassnahmenkategorieGruppe | null {
    if (list.displayText.toLowerCase().includes(term.toLowerCase())) {
      return list;
    }
    const options = list.options.filter(opt => opt.displayText.toLowerCase().includes(term.toLowerCase()));
    return options.length > 0 ? { ...list, options } : null;
  }
}
