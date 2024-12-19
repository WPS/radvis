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
import { fakeAsync, flush, tick } from '@angular/core/testing';
import { UntypedFormBuilder } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { MockBuilder, MockedComponentFixture, MockRender } from 'ng-mocks';
import { BehaviorSubject } from 'rxjs';
import { NotifyUserService } from 'src/app/shared/services/notify-user.service';
import { RoutingProfileVerwaltenDialogComponent } from 'src/app/viewer/fahrradroute/components/routing-profile-verwalten-dialog/routing-profile-verwalten-dialog.component';
import { CustomRoutingProfile } from 'src/app/viewer/fahrradroute/models/custom-routing-profile';
import { RoutingProfileService } from 'src/app/viewer/fahrradroute/services/routing-profile.service';
import { ViewerModule } from 'src/app/viewer/viewer.module';
import { anything, capture, instance, mock, when } from 'ts-mockito';

describe(RoutingProfileVerwaltenDialogComponent.name, () => {
  let component: RoutingProfileVerwaltenDialogComponent;
  let fixture: MockedComponentFixture<RoutingProfileVerwaltenDialogComponent>;

  let profiles$$: BehaviorSubject<CustomRoutingProfile[]>;
  let routingProfileService: RoutingProfileService;

  beforeEach(() => {
    routingProfileService = mock(RoutingProfileService);
    profiles$$ = new BehaviorSubject<CustomRoutingProfile[]>([]);
    when(routingProfileService.profiles$).thenReturn(profiles$$.asObservable());
    when(routingProfileService.save(anything())).thenReturn(Promise.resolve());

    return MockBuilder(RoutingProfileVerwaltenDialogComponent, ViewerModule)
      .provide({ provide: RoutingProfileService, useValue: instance(routingProfileService) })
      .provide({ provide: UntypedFormBuilder, useValue: new UntypedFormBuilder() })
      .provide({ provide: ChangeDetectorRef, useValue: instance(mock(ChangeDetectorRef)) })
      .provide({ provide: MatDialogRef, useValue: instance(mock(MatDialogRef)) })
      .provide({ provide: NotifyUserService, useValue: instance(mock(NotifyUserService)) });
  });

  beforeEach(() => {
    fixture = MockRender(RoutingProfileVerwaltenDialogComponent);
    component = fixture.point.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('form', () => {
    describe(RoutingProfileVerwaltenDialogComponent.prototype.onAddProfile.name, () => {
      it('should add empty Formgroup and be invalid & dirty', () => {
        expect(component.profileFormArray).toHaveSize(0);
        expect(component.profileFormArray.valid).toBeTrue();
        expect(component.profileFormArray.dirty).toBeFalse();

        component.onAddProfile();
        expect(component.profileFormArray).toHaveSize(1);
        expect(component.profileFormArray.valid).toBeFalse();
        expect(component.profileFormArray.dirty).toBeTrue();
      });
    });

    describe(RoutingProfileVerwaltenDialogComponent.prototype.onDeleteProfile.name, () => {
      it('should delete at correct index and still be valid', () => {
        profiles$$.next([
          {
            name: 'profil1',
            profilJson: '{}',
          },
          {
            name: 'profil2',
            profilJson: '{ "speed": []}',
          },
        ] as CustomRoutingProfile[]);

        expect(component.profileFormArray).toHaveSize(2);
        expect(component.profileFormArray.valid).toBeTrue();
        component.onDeleteProfile(0);
        expect(component.profileFormArray).toHaveSize(1);
        expect(component.profileFormArray.valid).toBeTrue();
      });
    });

    describe('Validation', () => {
      it('should accept correct input', () => {
        profiles$$.next([
          {
            name: 'profil1',
            profilJson: '{}',
          },
          {
            name: 'profil2',
            profilJson: '{ "speed": [ { "if": "belagart == \\"ASPHALT\\"", "multiply_by": "0.3"  } ]}',
          },
        ] as CustomRoutingProfile[]);

        expect(component.profileFormArray).toHaveSize(2);
        expect(component.profileFormArray.valid).toBeTrue();
      });

      it('should not accept wrong belgart', () => {
        profiles$$.next([
          {
            name: 'profil1',
            profilJson: '{}',
          },
          {
            name: 'profil2',
            profilJson: '{ "speed": [ { "if": "belagart == \\"GRUEN_WIE_GRASS\\"", "multiply_by": "0.3"  } ]}',
          },
        ] as CustomRoutingProfile[]);

        expect(component.profileFormArray).toHaveSize(2);
        expect(component.profileFormArray.invalid).toBeTrue();
      });

      it('should not accept unknown encoded value', () => {
        profiles$$.next([
          {
            name: 'profil1',
            profilJson: '{}',
          },
          {
            name: 'profil2',
            profilJson: '{ "speed": [ { "if": "licht == \\"UNBEKANNT\\"", "multiply_by": "0.3"  } ]}',
          },
        ] as CustomRoutingProfile[]);

        expect(component.profileFormArray).toHaveSize(2);
        expect(component.profileFormArray.invalid).toBeTrue();
      });
    });

    describe(RoutingProfileVerwaltenDialogComponent.prototype['resetForm'].name, () => {
      const profiles = [
        {
          id: 1,
          name: 'Test1',
          profilJson: '{}',
        },
        {
          id: 2,
          name: 'Test2',
          profilJson: '{}',
        },
      ];

      beforeEach(() => {
        component.profileFormArray.markAsDirty();
        profiles$$.next(profiles);
      });

      it('should mark as pristine', () => {
        expect(component.profileFormArray.dirty).toBeFalse();
      });

      it('should set controls', () => {
        expect(component.profileFormArray.value).toEqual([
          {
            id: 1,
            name: 'Test1',
            profilJson: '{}',
          },
          {
            id: 2,
            name: 'Test2',
            profilJson: '{}',
          },
        ]);
      });
    });

    it('should read correct save command and prettyPrint Json', fakeAsync(() => {
      const profiles = [
        {
          id: 1,
          name: 'Test1',
          profilJson: '{}',
        },
        {
          id: 2,
          name: 'Test2',
          profilJson: '{}',
        },
      ];
      profiles$$.next(profiles);
      tick();

      component.profileFormArray.at(0).patchValue({
        profilJson: '{ "speed" : [] }',
      });
      component.profileFormArray.at(1).patchValue({
        profilJson: '{ "priority":[] }',
      });
      tick();

      component.onSave();
      tick();

      expect(capture(routingProfileService.save).last()[0]).toEqual([
        {
          id: 1,
          name: 'Test1',
          profilJson: `{
  "speed": []
}`,
        },
        {
          id: 2,
          name: 'Test2',
          profilJson: `{
  "priority": []
}`,
        },
      ]);

      // Wegen des setTimeout() im ngAfterViewInit brauchen wir hier das flush,
      // siehe z.B.: https://www.damirscorner.com/blog/posts/20210917-TestingTimersWithFakeAsync.html
      flush();
    }));
  });
});
