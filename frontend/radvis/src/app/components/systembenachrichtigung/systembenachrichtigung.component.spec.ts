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
import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ngMocks } from 'ng-mocks';
import { SystembenachrichtigungService } from 'src/app/services/systembenachrichtigung.service';
import { SharedModule } from 'src/app/shared/shared.module';
import { instance, mock, when } from 'ts-mockito';
import { SystembenachrichtigungComponent } from './systembenachrichtigung.component';

describe('SystembenachrichtigungComponent', () => {
  let systembenachrichtigungService: SystembenachrichtigungService;
  beforeEach(() => {
    systembenachrichtigungService = mock(SystembenachrichtigungService);
    TestBed.configureTestingModule({
      declarations: [SystembenachrichtigungComponent],
      imports: [SharedModule],
      providers: [
        {
          provide: SystembenachrichtigungService,
          useValue: instance(systembenachrichtigungService),
        },
      ],
    });
  });

  describe('initial visible', () => {
    it('should be true if nachricht vorhanden', fakeAsync(() => {
      when(systembenachrichtigungService.fetch()).thenResolve(null);
      const fixture = TestBed.createComponent(SystembenachrichtigungComponent);
      const component = fixture.componentInstance;
      fixture.autoDetectChanges(true);

      expect(component.systemnachrichtVisible).toBeFalse();

      tick();

      expect(component.systemnachrichtVisible).toBeFalse();
    }));

    it('should be false if keine nachricht vorhanden', fakeAsync(() => {
      when(systembenachrichtigungService.fetch()).thenResolve({
        vom: new Date('2024-08-19'),
        text: 'test',
      });
      const fixture = TestBed.createComponent(SystembenachrichtigungComponent);
      const component = fixture.componentInstance;
      fixture.autoDetectChanges(true);

      expect(component.systemnachrichtVisible).toBeFalse();

      tick();

      expect(component.systemnachrichtVisible).toBeTrue();
    }));
  });

  describe('title', () => {
    it('should show "wird geladen"', fakeAsync(() => {
      when(systembenachrichtigungService.fetch()).thenResolve(null);
      const fixture = TestBed.createComponent(SystembenachrichtigungComponent);
      const component = fixture.componentInstance;
      component.systemnachrichtVisible = true;
      fixture.autoDetectChanges(true);

      expect(component.loading).toBeTrue();
      expect(ngMocks.formatText(ngMocks.find('.header'))).toBe(component['WIRD_GELADEN']);

      tick();

      expect(component.loading).toBeFalse();
      expect(ngMocks.formatText(ngMocks.find('.header'))).not.toBe(component['WIRD_GELADEN']);
    }));

    it('should show "keine Systemnachricht"', fakeAsync(() => {
      when(systembenachrichtigungService.fetch()).thenResolve(null);
      const fixture = TestBed.createComponent(SystembenachrichtigungComponent);
      const component = fixture.componentInstance;
      component.systemnachrichtVisible = true;
      fixture.autoDetectChanges(true);
      tick();

      expect(component.loading).toBeFalse();
      expect(ngMocks.formatText(ngMocks.find('.header'))).toBe(component['KEINE_NACHRICHT']);
    }));

    it('should show systemnachricht datum', fakeAsync(() => {
      when(systembenachrichtigungService.fetch()).thenResolve({
        vom: new Date('2024-08-19'),
        text: 'test',
      });
      const fixture = TestBed.createComponent(SystembenachrichtigungComponent);
      const component = fixture.componentInstance;
      component.systemnachrichtVisible = true;
      fixture.autoDetectChanges(true);
      tick();

      expect(component.loading).toBeFalse();
      expect(ngMocks.formatText(ngMocks.find('.header'))).toBe('Systemnachricht vom 19.08.2024');
    }));
  });
});
