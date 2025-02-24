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

import { fakeAsync, TestBed, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatChipRemove, MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MockBuilder, MockedComponentFixture, MockRender, ngMocks } from 'ng-mocks';
import { MassnahmenkategorienDropdownControlComponent } from 'src/app/viewer/massnahme/components/massnahmenkategorien-dropdown-control/massnahmenkategorien-dropdown-control.component';
import { MassnahmeModule } from 'src/app/viewer/massnahme/massnahme.module';
import { MassnahmenkategorieOptionGroup } from 'src/app/viewer/massnahme/models/massnahmenkategorie-option-group';
import { Massnahmenkategorien } from 'src/app/viewer/massnahme/models/massnahmenkategorien';

describe(MassnahmenkategorienDropdownControlComponent.name, () => {
  let component: MassnahmenkategorienDropdownControlComponent;
  let fixture: MockedComponentFixture<MassnahmenkategorienDropdownControlComponent>;

  ngMocks.faster();

  beforeAll(() => {
    return MockBuilder(MassnahmenkategorienDropdownControlComponent, MassnahmeModule);
  });

  beforeEach(() => {
    fixture = MockRender(MassnahmenkategorienDropdownControlComponent, {
      massnahmeKategorienOptions: Massnahmenkategorien.ALL,
    } as MassnahmenkategorienDropdownControlComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should set massnahmeKategorienOptions Input as filteredOptions', () => {
    expect(component.massnahmeKategorienOptions).toEqual(Massnahmenkategorien.ALL);
    fixture.componentInstance.massnahmeKategorienOptions = allOptions;
    fixture.detectChanges();
    expect(component.massnahmeKategorienOptions).toEqual(allOptions);
  });

  describe('updateFilteredOptions', () => {
    it('should update if massnahmeKategorienOptions Input changed', () => {
      component.massnahmeKategorienOptions = [
        {
          name: 'GRUPPE',
          displayText: 'Gruppe',
          options: [
            {
              name: 'SUB_GRUPPE',
              displayText: 'SubGruppe',
              gewichtung: 1,
              options: [
                {
                  name: 'OPTION_A1',
                  displayText: 'A1',
                },
                {
                  name: 'OPTION_A2',
                  displayText: 'A2',
                },
              ],
            },
          ],
        },
      ];
      component.formControl.patchValue('A2');
      expect(component.filteredGroupedOptions).toEqual([
        {
          name: 'GRUPPE',
          displayText: 'Gruppe',
          options: [
            {
              name: 'SUB_GRUPPE',
              displayText: 'SubGruppe',
              gewichtung: 1,
              options: [
                {
                  name: 'OPTION_A2',
                  displayText: 'A2',
                },
              ],
            },
          ],
        },
      ]);

      fixture.componentInstance.massnahmeKategorienOptions = [
        {
          name: 'GRUPPE',
          displayText: 'Gruppe',
          options: [
            {
              name: 'SUB_GRUPPE',
              displayText: 'SubGruppe',
              gewichtung: 1,
              options: [
                {
                  name: 'OPTION_A1',
                  displayText: 'A1',
                },
              ],
            },
          ],
        },
      ];
      fixture.detectChanges();

      expect(component.filteredGroupedOptions).toEqual([]);
      expect(component.formControl.value).toEqual('A2');
    });

    it('should filter correctly', fakeAsync(() => {
      component.massnahmeKategorienOptions = allOptions;

      component.formControl.patchValue('Text');

      tick();
      fixture.detectChanges();

      expect(component.filteredGroupedOptions).toEqual([
        {
          name: 'GRUPPE_A',
          displayText: 'Gruppe A',
          options: [
            {
              name: 'SUB_GRUPPE_A1',
              displayText: 'SubGruppe A1',
              gewichtung: 1,
              options: [
                {
                  name: 'OPTION_A1_I',
                  displayText: 'TestText A1',
                },
              ],
            },
            {
              name: 'SUB_GRUPPE_A2',
              displayText: 'SubGruppe A2',
              gewichtung: 1,
              options: [
                {
                  name: 'OPTION_A2_I',
                  displayText: 'TestText A2',
                },
              ],
            },
          ],
        },
        {
          name: 'GRUPPE_B',
          displayText: 'Gruppe B',
          options: [
            {
              name: 'SUB_GRUPPE_B1',
              displayText: 'SubGruppe B1',
              gewichtung: 1,
              options: [
                {
                  name: 'OPTION_3',
                  displayText: 'Text',
                },
              ],
            },
          ],
        },
      ]);
    }));

    it('should filter on secondlevel option-groups correctly', fakeAsync(() => {
      component.massnahmeKategorienOptions = allOptions;

      component.formControl.patchValue('SubGruppe B');

      tick();
      fixture.detectChanges();

      expect(component.filteredGroupedOptions).toEqual([
        {
          name: 'GRUPPE_B',
          displayText: 'Gruppe B',
          options: [
            {
              name: 'SUB_GRUPPE_B1',
              displayText: 'SubGruppe B1',
              gewichtung: 1,
              options: [
                {
                  name: 'OPTION_3',
                  displayText: 'Text',
                },
              ],
            },
          ],
        },
      ]);
    }));

    it('should filter on firstlevel option-groups correctly', fakeAsync(() => {
      component.massnahmeKategorienOptions = allOptions;

      component.formControl.patchValue('Gruppe A');

      tick();
      fixture.detectChanges();

      expect(component.filteredGroupedOptions).toEqual([
        {
          name: 'GRUPPE_A',
          displayText: 'Gruppe A',
          options: [
            {
              name: 'SUB_GRUPPE_A1',
              displayText: 'SubGruppe A1',
              gewichtung: 1,
              options: [
                {
                  name: 'OPTION_A1_I',
                  displayText: 'TestText A1',
                },
                {
                  name: 'OPTION_A1_II',
                  displayText: 'Test A1',
                },
              ],
            },
            {
              name: 'SUB_GRUPPE_A2',
              displayText: 'SubGruppe A2',
              gewichtung: 1,
              options: [
                {
                  name: 'OPTION_A2_I',
                  displayText: 'TestText A2',
                },
                {
                  name: 'OPTION_A2_II',
                  displayText: 'Test A2',
                },
              ],
            },
          ],
        },
      ]);
    }));

    it('should filter complete group if no option of this group matches', fakeAsync(() => {
      component.massnahmeKategorienOptions = allOptions;

      component.formControl.patchValue('Test');

      tick();
      fixture.detectChanges();

      expect(component.filteredGroupedOptions).toEqual([
        {
          name: 'GRUPPE_A',
          displayText: 'Gruppe A',
          options: [
            {
              name: 'SUB_GRUPPE_A1',
              displayText: 'SubGruppe A1',
              gewichtung: 1,
              options: [
                {
                  name: 'OPTION_A1_I',
                  displayText: 'TestText A1',
                },
                {
                  name: 'OPTION_A1_II',
                  displayText: 'Test A1',
                },
              ],
            },
            {
              name: 'SUB_GRUPPE_A2',
              displayText: 'SubGruppe A2',
              gewichtung: 1,
              options: [
                {
                  name: 'OPTION_A2_I',
                  displayText: 'TestText A2',
                },
                {
                  name: 'OPTION_A2_II',
                  displayText: 'Test A2',
                },
              ],
            },
          ],
        },
      ]);
    }));

    it('should offer all options when no input was made', fakeAsync(() => {
      component.massnahmeKategorienOptions = allOptions;

      component.formControl.patchValue(null);

      tick();
      fixture.detectChanges();

      expect(component.filteredGroupedOptions).toEqual(allOptions);
    }));
  });

  describe('onOptionSelected', () => {
    it('it should not add duplicates', fakeAsync(() => {
      const spyOnChange = spyOn(component, 'onChange');

      component.massnahmeKategorienOptions = allOptions;

      expect(component.selectedOptions).toEqual([]);
      expect(spyOnChange).not.toHaveBeenCalled();

      component.onOptionSelected(
        { option: { value: 'OPTION_A2_I' } } as MatAutocompleteSelectedEvent,
        { value: '' } as HTMLInputElement
      );

      expect(component.selectedOptions).toEqual(['OPTION_A2_I']);
      expect(spyOnChange).toHaveBeenCalledOnceWith(['OPTION_A2_I']);

      component.onOptionSelected(
        { option: { value: 'OPTION_A2_I' } } as MatAutocompleteSelectedEvent,
        { value: '' } as HTMLInputElement
      );

      expect(component.selectedOptions).toEqual(['OPTION_A2_I']);
      expect(spyOnChange).toHaveBeenCalledOnceWith(['OPTION_A2_I']);
    }));
  });
});

describe(MassnahmenkategorienDropdownControlComponent.name + ' - embedded', () => {
  let component: MassnahmenkategorienDropdownControlComponent;
  let fixture: MockedComponentFixture<MassnahmenkategorienDropdownControlComponent>;

  beforeEach(() => {
    return TestBed.configureTestingModule({
      declarations: [MassnahmenkategorienDropdownControlComponent],
      imports: [
        MatChipsModule,
        MatAutocompleteModule,
        ReactiveFormsModule,
        MatTooltipModule,
        MatFormFieldModule,
        MatIconModule,
        NoopAnimationsModule,
      ],
    });
  });

  beforeEach(() => {
    fixture = MockRender(MassnahmenkategorienDropdownControlComponent, {
      massnahmeKategorienOptions: Massnahmenkategorien.ALL,
    } as MassnahmenkategorienDropdownControlComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
    fixture.autoDetectChanges(true);
  });

  it('should display value not within allowed options', async () => {
    fixture.componentInstance.massnahmeKategorienOptions = Massnahmenkategorien.RADNETZ_2024_KATEGORIEN_ONLY;
    component.writeValue(['NEUBAU_WEG_NACH_RADNETZ_QUALITAETSSTANDARD']);
    fixture.detectChanges();
    await fixture.whenRenderingDone();

    expect(
      (
        fixture.debugElement.query(By.css('.mat-mdc-chip .mat-mdc-chip-action-label'))?.nativeElement as HTMLElement
      )?.textContent?.trim()
    ).toEqual('Neubau eines Weges nach RadNET ...');
  });

  describe('disabled', () => {
    it('should not open panel if disabled', async () => {
      const clickedKategorie = Massnahmenkategorien.ALL[1].options[0].options[0].name;
      component.writeValue([clickedKategorie]);
      component.setDisabledState(false);
      fixture.detectChanges();
      const panelOpenSpy = spyOn(component, 'onPanelOpened');
      await fixture.whenRenderingDone();

      (fixture.debugElement.query(By.css('.mat-mdc-chip'))?.nativeElement as HTMLElement).dispatchEvent(
        new Event('click')
      );
      // bis das panel offen ist, ist leider eine magic number. wenn der test fehlschlägt, erstmal probieren, hochzusetzen
      await new Promise(resolve => setTimeout(resolve, 1000));
      await fixture.whenRenderingDone();
      expect(panelOpenSpy).toHaveBeenCalled();
      panelOpenSpy.calls.reset();

      component.setDisabledState(true);
      fixture.detectChanges();
      await fixture.whenRenderingDone();

      (fixture.debugElement.query(By.css('.mat-mdc-chip'))?.nativeElement as HTMLElement).dispatchEvent(
        new Event('click')
      );
      // bis das panel offen ist, ist leider eine magic number. wenn der test fehlschlägt, erstmal probieren, hochzusetzen
      await new Promise(resolve => setTimeout(resolve, 1000));
      await fixture.whenRenderingDone();
      expect(panelOpenSpy).not.toHaveBeenCalled();
    });

    it('should not show remove for kategories', async () => {
      const clickedKategorie = Massnahmenkategorien.ALL[1].options[0].options[0].name;
      component.writeValue([clickedKategorie]);

      component.setDisabledState(false);
      fixture.detectChanges();
      await fixture.whenRenderingDone();
      expect(fixture.debugElement.query(By.directive(MatChipRemove))).toBeTruthy();

      component.setDisabledState(true);
      fixture.detectChanges();
      await fixture.whenRenderingDone();
      expect(fixture.debugElement.query(By.directive(MatChipRemove))).toBeFalsy();
    });

    it('should hide input', async () => {
      const clickedKategorie = Massnahmenkategorien.ALL[1].options[0].options[0].name;
      component.writeValue([clickedKategorie]);

      component.setDisabledState(false);
      fixture.detectChanges();
      await fixture.whenRenderingDone();
      expect(fixture.debugElement.query(By.css('input')).attributes.hidden).toBeUndefined();

      component.setDisabledState(true);
      fixture.detectChanges();
      await fixture.whenRenderingDone();
      expect(fixture.debugElement.query(By.css('input')).attributes.hidden).not.toBeUndefined();
    });
  });

  describe('onChipClick', () => {
    it('should open panel, scroll to Option and highlight', async () => {
      const clickedKategorie = Massnahmenkategorien.ALL[1].options[0].options[0].name;
      component.writeValue([clickedKategorie]);
      fixture.detectChanges();

      await fixture.whenRenderingDone();

      (fixture.debugElement.query(By.css('.mat-mdc-chip'))?.nativeElement as HTMLElement).dispatchEvent(
        new Event('click')
      );
      // bis das panel offen ist, ist leider eine magic number. wenn der test fehlschlägt, erstmal probieren, hochzusetzen
      await new Promise(resolve => setTimeout(resolve, 1000));
      await fixture.whenRenderingDone();

      const highlightedKategorie = fixture.debugElement.query(
        By.css('.' + MassnahmenkategorienDropdownControlComponent['CLICKED_KATEGORIE_CLASS'])
      );
      expect(highlightedKategorie).toBeTruthy();
      expect(highlightedKategorie.nativeElement.id).toBe(component.getElementIdByKategorie(clickedKategorie));
    });

    it('should scroll to Option and highlight with opened panel', async () => {
      const clickedKategorie = Massnahmenkategorien.ALL[1].options[0].options[0].name;
      component.writeValue([clickedKategorie]);
      fixture.detectChanges();
      const panelOpenSpy = spyOn(component, 'onPanelOpened');

      await fixture.whenRenderingDone();

      (fixture.debugElement.query(By.css('input'))?.nativeElement as HTMLElement).dispatchEvent(new Event('click'));

      await fixture.whenRenderingDone();
      expect(panelOpenSpy).toHaveBeenCalled();

      (fixture.debugElement.query(By.css('.mat-mdc-chip'))?.nativeElement as HTMLElement).dispatchEvent(
        new Event('click')
      );
      // bis das panel offen ist, ist leider eine magic number. wenn der test fehlschlägt, erstmal probieren, hochzusetzen
      await new Promise(resolve => setTimeout(resolve, 1000));
      await fixture.whenRenderingDone();

      const highlightedKategorie = fixture.debugElement.query(
        By.css('.' + MassnahmenkategorienDropdownControlComponent['CLICKED_KATEGORIE_CLASS'])
      );
      expect(highlightedKategorie).toBeTruthy();
      expect(highlightedKategorie.nativeElement.id).toBe(component.getElementIdByKategorie(clickedKategorie));
    });

    it('should scroll to second option und highlight', async () => {
      const clickedKategorie1 = Massnahmenkategorien.ALL[1].options[0].options[0].name;
      const clickedKategorie2 = Massnahmenkategorien.ALL[0].options[0].options[0].name;
      component.writeValue([clickedKategorie1, clickedKategorie2]);
      fixture.detectChanges();

      await fixture.whenRenderingDone();

      (fixture.debugElement.queryAll(By.css('.mat-mdc-chip'))[0].nativeElement as HTMLElement).dispatchEvent(
        new Event('click')
      );
      // bis das panel offen ist, ist leider eine magic number. wenn der test fehlschlägt, erstmal probieren, hochzusetzen
      await new Promise(resolve => setTimeout(resolve, 1000));
      await fixture.whenRenderingDone();

      const highlightedKategorie1 = fixture.debugElement.query(
        By.css('.' + MassnahmenkategorienDropdownControlComponent['CLICKED_KATEGORIE_CLASS'])
      );
      expect(highlightedKategorie1).toBeTruthy();
      expect(highlightedKategorie1.nativeElement.id).toBe(component.getElementIdByKategorie(clickedKategorie1));

      (fixture.debugElement.queryAll(By.css('.mat-mdc-chip'))[1].nativeElement as HTMLElement).dispatchEvent(
        new Event('click')
      );
      // bis das panel offen ist, ist leider eine magic number. wenn der test fehlschlägt, erstmal probieren, hochzusetzen
      await new Promise(resolve => setTimeout(resolve, 1000));
      await fixture.whenRenderingDone();

      const highlightedKategorie2 = fixture.debugElement.query(
        By.css('.' + MassnahmenkategorienDropdownControlComponent['CLICKED_KATEGORIE_CLASS'])
      );
      expect(highlightedKategorie2).toBeTruthy();
      expect(highlightedKategorie2.nativeElement.id).toBe(component.getElementIdByKategorie(clickedKategorie2));
    });

    it('should scroll to Option after option filtered out', async () => {
      const clickedKategorie = Massnahmenkategorien.ALL[1].options[0].options[0].name;
      component.writeValue([clickedKategorie]);
      component.filteredGroupedOptions = [component.massnahmeKategorienOptions[0]];
      fixture.detectChanges();
      await fixture.whenRenderingDone();

      (fixture.debugElement.query(By.css('.mat-mdc-chip'))?.nativeElement as HTMLElement).dispatchEvent(
        new Event('click')
      );
      // bis das panel offen ist, ist leider eine magic number. wenn der test fehlschlägt, erstmal probieren, hochzusetzen
      await new Promise(resolve => setTimeout(resolve, 1000));
      await fixture.whenRenderingDone();

      const highlightedKategorie = fixture.debugElement.query(
        By.css('.' + MassnahmenkategorienDropdownControlComponent['CLICKED_KATEGORIE_CLASS'])
      );
      expect(highlightedKategorie).toBeTruthy();
      expect(highlightedKategorie.nativeElement.id).toBe(component.getElementIdByKategorie(clickedKategorie));
      expect(component.filteredGroupedOptions).toEqual(component.massnahmeKategorienOptions);
    });

    it('should be unhighlighted after close', async () => {
      const clickedKategorie = Massnahmenkategorien.ALL[1].options[0].options[0].name;
      component.writeValue([clickedKategorie]);
      fixture.detectChanges();

      await fixture.whenRenderingDone();

      (fixture.debugElement.query(By.css('.mat-mdc-chip'))?.nativeElement as HTMLElement).dispatchEvent(
        new Event('click')
      );
      // bis das panel offen ist, ist leider eine magic number. wenn der test fehlschlägt, erstmal probieren, hochzusetzen
      await new Promise(resolve => setTimeout(resolve, 1000));
      await fixture.whenRenderingDone();

      component.onPanelClosed();

      const highlightedKategorie = fixture.debugElement.query(
        By.css('.' + MassnahmenkategorienDropdownControlComponent['CLICKED_KATEGORIE_CLASS'])
      );
      expect(highlightedKategorie).toBeFalsy();
    });
  });
});

const allOptions: MassnahmenkategorieOptionGroup[] = [
  {
    name: 'GRUPPE_A',
    displayText: 'Gruppe A',
    options: [
      {
        name: 'SUB_GRUPPE_A1',
        displayText: 'SubGruppe A1',
        gewichtung: 1,
        options: [
          {
            name: 'OPTION_A1_I',
            displayText: 'TestText A1',
          },
          {
            name: 'OPTION_A1_II',
            displayText: 'Test A1',
          },
        ],
      },
      {
        name: 'SUB_GRUPPE_A2',
        displayText: 'SubGruppe A2',
        gewichtung: 1,
        options: [
          {
            name: 'OPTION_A2_I',
            displayText: 'TestText A2',
          },
          {
            name: 'OPTION_A2_II',
            displayText: 'Test A2',
          },
        ],
      },
    ],
  },
  {
    name: 'GRUPPE_B',
    displayText: 'Gruppe B',
    options: [
      {
        name: 'SUB_GRUPPE_B1',
        displayText: 'SubGruppe B1',
        gewichtung: 1,
        options: [
          {
            name: 'OPTION_3',
            displayText: 'Text',
          },
        ],
      },
    ],
  },
];
