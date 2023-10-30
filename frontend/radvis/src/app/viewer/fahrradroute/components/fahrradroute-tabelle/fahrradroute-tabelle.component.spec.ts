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
import { BenutzerDetailsService } from 'src/app/shared/services/benutzer-details.service';
import { FahrradrouteListenView } from 'src/app/viewer/fahrradroute/models/fahrradroute-listen-view';
import { FahrradrouteFilterService } from 'src/app/viewer/fahrradroute/services/fahrradroute-filter.service';
import { FahrradrouteRoutingService } from 'src/app/viewer/fahrradroute/services/fahrradroute-routing.service';
import { instance, mock, when } from 'ts-mockito';
import { FahrradrouteTabelleComponent } from './fahrradroute-tabelle.component';
import { MockBuilder } from 'ng-mocks';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { FahrradrouteProfilService } from 'src/app/viewer/fahrradroute/services/fahrradroute-profil.service';

describe(FahrradrouteTabelleComponent.name, () => {
  let fahrradrouteTabelleComponent: FahrradrouteTabelleComponent;
  let fixture: ComponentFixture<FahrradrouteTabelleComponent>;

  let fahrradrouteFilterService: FahrradrouteFilterService;
  let fahrradrouteRoutingService: FahrradrouteRoutingService;
  let fahrradrouteProfilService: FahrradrouteProfilService;

  const filteredFahrradroutenSubject = new BehaviorSubject<FahrradrouteListenView[]>([]);
  const selectedInfrastrukturIdSubject = new BehaviorSubject<number | null>(null);
  const showCurrentProfileSubject = new BehaviorSubject<boolean>(false);

  beforeEach(() => {
    fahrradrouteFilterService = mock(FahrradrouteFilterService);
    fahrradrouteRoutingService = mock(FahrradrouteRoutingService);
    fahrradrouteProfilService = mock(FahrradrouteProfilService);

    when(fahrradrouteFilterService.filteredList$).thenReturn(filteredFahrradroutenSubject.asObservable());
    when(fahrradrouteRoutingService.selectedInfrastrukturId$).thenReturn(selectedInfrastrukturIdSubject.asObservable());
    when(fahrradrouteProfilService.showCurrentRouteProfile$).thenReturn(showCurrentProfileSubject.asObservable());

    return MockBuilder(FahrradrouteTabelleComponent, ViewerModule)
      .provide({ provide: FahrradrouteFilterService, useValue: instance(fahrradrouteFilterService) })
      .provide({ provide: FahrradrouteRoutingService, useValue: instance(fahrradrouteRoutingService) })
      .provide({ provide: BenutzerDetailsService, useValue: instance(mock(BenutzerDetailsService)) })
      .provide({ provide: FahrradrouteProfilService, useValue: instance(fahrradrouteProfilService) });
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(FahrradrouteTabelleComponent);
    fahrradrouteTabelleComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  beforeEach(() => {
    filteredFahrradroutenSubject.next([]);
  });

  it('should create', () => {
    expect(fahrradrouteTabelleComponent).toBeTruthy();
  });
});
