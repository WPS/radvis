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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { instance, mock, when } from 'ts-mockito';
import { MockBuilder } from 'ng-mocks';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { AnpassungswunschTabelleComponent } from 'src/app/viewer/anpassungswunsch/components/anpassungswunsch-tabelle/anpassungswunsch-tabelle.component';
import { AnpassungswunschFilterService } from 'src/app/viewer/anpassungswunsch/services/anpassungswunsch-filter.service';
import { AnpassungswunschListenView } from 'src/app/viewer/anpassungswunsch/models/anpassungswunsch-listen-view';
import { AnpassungenRoutingService } from 'src/app/viewer/anpassungswunsch/services/anpassungen-routing.service';

describe(AnpassungswunschTabelleComponent.name, () => {
  let anpassungswunschTabelleComponent: AnpassungswunschTabelleComponent;
  let fixture: ComponentFixture<AnpassungswunschTabelleComponent>;

  let anpassungswunschFilterService: AnpassungswunschFilterService;
  let anpassungswunschRoutingService: AnpassungenRoutingService;

  const filteredAnpassungswunschSubject = new BehaviorSubject<AnpassungswunschListenView[]>([]);
  const selectedInfrastrukturIdSubject = new BehaviorSubject<number | null>(null);

  beforeEach(() => {
    anpassungswunschFilterService = mock(AnpassungswunschFilterService);
    anpassungswunschRoutingService = mock(AnpassungenRoutingService);

    when(anpassungswunschFilterService.filteredList$).thenReturn(filteredAnpassungswunschSubject.asObservable());
    when(anpassungswunschRoutingService.selectedInfrastrukturId$).thenReturn(
      selectedInfrastrukturIdSubject.asObservable()
    );

    return MockBuilder(AnpassungswunschTabelleComponent, ViewerModule)
      .provide({ provide: AnpassungswunschFilterService, useValue: instance(anpassungswunschFilterService) })
      .provide({ provide: AnpassungenRoutingService, useValue: instance(anpassungswunschRoutingService) });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(AnpassungswunschTabelleComponent);
    anpassungswunschTabelleComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  beforeEach(() => {
    filteredAnpassungswunschSubject.next([]);
  });

  it('should create', () => {
    expect(anpassungswunschTabelleComponent).toBeTruthy();
  });
});
