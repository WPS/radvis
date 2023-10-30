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

/* eslint-disable @typescript-eslint/dot-notation */
import { ChangeDetectorRef } from '@angular/core';
import { fakeAsync, tick } from '@angular/core/testing';
import { ActivatedRoute, Data } from '@angular/router';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { BehaviorSubject } from 'rxjs';
import { KommentarListeComponent } from 'src/app/viewer/kommentare/components/kommentar-liste/kommentar-liste.component';
import { KommentareModule } from 'src/app/viewer/kommentare/kommentare.module';
import { AddKommentarCommand } from 'src/app/viewer/kommentare/models/add-kommentar-command';
import { Kommentar } from 'src/app/viewer/kommentare/models/kommentar';
import { KommentarListeResolverData } from 'src/app/viewer/kommentare/models/kommentar-liste-resolver-data';
import { KommentarService } from 'src/app/viewer/kommentare/services/kommentar.service';
import { anything, deepEqual, instance, mock, verify, when } from 'ts-mockito';

class TestKommentarService implements KommentarService {
  // eslint-disable-next-line no-unused-vars
  public addKommentar(command: AddKommentarCommand): Promise<Kommentar[]> {
    throw new Error('Method not implemented.');
  }
}

describe(KommentarListeComponent.name, () => {
  let component: KommentarListeComponent;
  let fixture: MockedComponentFixture<KommentarListeComponent>;

  let kommentarService: KommentarService;
  let changeDetectorRef: ChangeDetectorRef;

  let activatedRoute: ActivatedRoute;
  let dataSubject: BehaviorSubject<Data>;

  const kommentarListe: Kommentar[] = [
    {
      kommentarText: 'Hello Stonehenge!',
      benutzer: 'Doctor Who',
      datum: '2022-06-23T16:50:21',
      fromLoggedInUser: true,
    } as Kommentar,
    {
      kommentarText: 'Bow ties are cool!',
      benutzer: 'Doctor Who',
      datum: '2022-01-01T01:50:21',
      fromLoggedInUser: true,
    } as Kommentar,
  ];

  beforeEach(() => {
    activatedRoute = mock(ActivatedRoute);
    dataSubject = new BehaviorSubject({
      kommentare: { massnahmeId: 33, liste: [] } as KommentarListeResolverData,
    } as Data);

    when(activatedRoute.data).thenReturn(dataSubject.asObservable());

    changeDetectorRef = mock(ChangeDetectorRef);
    kommentarService = mock(TestKommentarService);

    return MockBuilder(KommentarListeComponent, KommentareModule)
      .provide({
        provide: ActivatedRoute,
        useValue: instance(activatedRoute),
      })
      .provide({
        provide: KommentarService,
        useValue: instance(kommentarService),
      })
      .provide({
        provide: ChangeDetectorRef,
        useValue: instance(changeDetectorRef),
      });
  });

  beforeEach(() => {
    fixture = MockRender(KommentarListeComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('route subscription', () => {
    it('should update and sort kommenarListe from route', () => {
      const markForCheckSpy = spyOn(component['changeDetectorRef'], 'markForCheck');
      dataSubject.next({ kommentare: { liste: kommentarListe } });
      expect(component.kommentarListe?.[0]).toEqual(kommentarListe[1]);
      expect(component.kommentarListe?.[1]).toEqual(kommentarListe[0]);
      expect(markForCheckSpy).toHaveBeenCalled();
    });
  });

  describe(KommentarListeComponent.prototype.sendKommentar.name, () => {
    beforeEach(() => {
      when(kommentarService.addKommentar(anything())).thenResolve(kommentarListe);
    });

    it('should build correct command and update kommentarListe', fakeAsync(() => {
      component.sendKommentar('Guess who?');
      verify(
        kommentarService.addKommentar(
          deepEqual({
            kommentarText: 'Guess who?',
          } as AddKommentarCommand)
        )
      ).once();

      tick();

      expect(component.kommentarListe?.[0]).toEqual(kommentarListe[1]);
      expect(component.kommentarListe?.[1]).toEqual(kommentarListe[0]);
    }));
  });
});
