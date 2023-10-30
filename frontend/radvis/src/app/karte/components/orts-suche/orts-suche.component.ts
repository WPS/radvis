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

import { ChangeDetectionStrategy, Component, ElementRef, EventEmitter, Output, ViewChild } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { Coordinate } from 'ol/coordinate';
import { Extent } from 'ol/extent';
import { Observable } from 'rxjs';
import { debounceTime, filter, switchMap } from 'rxjs/operators';
import { OrtsSucheService } from 'src/app/karte/services/orts-suche.service';
import { OrtsSucheResult } from 'src/app/shared/models/orts-suche-result';

@Component({
  selector: 'rad-orts-suche',
  templateUrl: './orts-suche.component.html',
  styleUrls: ['./orts-suche.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrtsSucheComponent {
  @Output()
  public ortAusgewaehlt = new EventEmitter<{ coordinate: Coordinate; extent: Extent }>();

  @ViewChild('input') inputElement: ElementRef | null = null;

  public ortsSucheControl = new FormControl();

  suggestions$: Observable<OrtsSucheResult[]>;

  constructor(private ortsSucheService: OrtsSucheService) {
    this.suggestions$ = this.ortsSucheControl.valueChanges.pipe(
      debounceTime(50),
      // das gewählte OrtsSucheResult Objekt wird bei der Auswahl eingefügt und soll ignoriert werden
      filter(suchBegriff => !suchBegriff.centerCoordinate),
      switchMap((suchBegriff: string) => this.ortsSucheService.sucheOrt(suchBegriff))
    );
  }

  onOrtAusgewaehlt(event: MatAutocompleteSelectedEvent): void {
    this.ortAusgewaehlt.emit({
      coordinate: event.option.value.centerCoordinate,
      extent: event.option.value.extent as Extent,
    });
  }

  extractName(suggestion: OrtsSucheResult): string {
    return suggestion ? suggestion.name : '';
  }

  onRefreshClick(): void {
    if (this.ortsSucheControl.value) {
      this.ortAusgewaehlt.emit(this.ortsSucheControl.value);
    }
  }

  onClearClick(): void {
    this.ortsSucheControl.patchValue('');
  }

  setFocus(): void {
    this.inputElement?.nativeElement.focus();
  }
}
