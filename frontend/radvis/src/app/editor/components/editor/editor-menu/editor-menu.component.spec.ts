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

import { EditorMenuComponent } from 'src/app/editor/components/editor/editor-menu/editor-menu.component';
import { instance, mock, when } from 'ts-mockito';
import { NavigationEnd, Router } from '@angular/router';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { EditorModule } from 'src/app/editor/editor.module';
import { EditorRoutingService } from 'src/app/editor/editor-shared/services/editor-routing.service';
import { Subject } from 'rxjs';

describe(EditorMenuComponent.name, () => {
  let component: EditorMenuComponent;
  let fixture: MockedComponentFixture<EditorMenuComponent>;

  let routingService: EditorRoutingService;
  let router: Router;
  let routerEvents$: Subject<NavigationEnd>;

  beforeEach(() => {
    routingService = mock(EditorRoutingService);
    when(routingService.getImportRoute()).thenReturn('/bla/bli');
    router = mock(Router);
    routerEvents$ = new Subject();
    when(router.events).thenReturn(routerEvents$.asObservable());
    when(router.url).thenReturn('some/where/else');
    return MockBuilder(EditorMenuComponent, EditorModule)
      .provide({ provide: EditorRoutingService, useValue: instance(routingService) })
      .provide({
        provide: Router,
        useValue: instance(router),
      });
  });

  beforeEach(() => {
    fixture = MockRender(EditorMenuComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  describe('importRoute', () => {
    it('should not route to import when already in child route', done => {
      expect(component.importRoute).not.toBeNull();
      routerEvents$.subscribe(() => done());
      when(router.url).thenReturn('/bla/bli/blup');
      routerEvents$.next(new NavigationEnd(1, 'egal', ''));
      expect(component.importRoute).toBeNull();
    });
  });
});
