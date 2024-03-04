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

import { ComponentFixture } from '@angular/core/testing';
import { MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatOption } from '@angular/material/core';
import { By } from '@angular/platform-browser';
import { MockBuilder, MockRender } from 'ng-mocks';
import { of } from 'rxjs';
import { OrtsSucheComponent } from 'src/app/karte/components/orts-suche/orts-suche.component';
import { OrtsSucheService } from 'src/app/karte/services/orts-suche.service';
import { OrtsSucheResult } from 'src/app/shared/models/orts-suche-result';
import { SharedModule } from 'src/app/shared/shared.module';

describe(OrtsSucheComponent.name, () => {
  let component: OrtsSucheComponent;
  let fixture: ComponentFixture<OrtsSucheComponent>;

  beforeEach(() => {
    return MockBuilder(OrtsSucheComponent, SharedModule).mock(OrtsSucheService, {
      sucheOrt: (suchBegriff: string) => {
        if (suchBegriff === '') {
          return of([]);
        } else {
          return of([
            {
              name: 'string',
              centerCoordinate: [12, 12],
              extent: [0, 0, 0, 0],
            },
          ]);
        }
      },
    });
  });

  beforeEach(() => {
    fixture = MockRender(OrtsSucheComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('handle input', () => {
    it('suggestions should be empty if suchBegriff is empty', (done: DoneFn) => {
      component.ortsSucheControl.setValue('initialer Wert');
      component.suggestions$.subscribe(suggestions => {
        expect(suggestions).toHaveSize(0);
        done();
      });
      component.ortsSucheControl.setValue('');
    });

    it('suggestions should not be empty if suchBegriff is not empty', (done: DoneFn) => {
      component.suggestions$.subscribe(suggestions => {
        expect(suggestions).toHaveSize(1);
        done();
      });
      component.ortsSucheControl.setValue('fake');
    });

    describe('handle selection', () => {
      it('clicking a suggestion should emit Event', () => {
        const spy = spyOn(component.ortAusgewaehlt, 'emit');

        fixture.debugElement.query(By.css('mat-autocomplete')).triggerEventHandler(
          'optionSelected',
          new MatAutocompleteSelectedEvent(
            null as any,
            {
              value: {
                name: 'Stuttgart',
                centerCoordinate: [12, 12],
              } as OrtsSucheResult,
            } as MatOption
          )
        );

        expect(spy).toHaveBeenCalled();
        expect(spy.calls.mostRecent().args[0]?.coordinate).toEqual([12, 12]);
      });
    });
  });
});
