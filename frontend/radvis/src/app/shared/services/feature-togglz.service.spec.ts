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

import { HttpClient } from '@angular/common/http';
import { of } from 'rxjs';
import { FeatureTogglzService } from 'src/app/shared/services/feature-togglz.service';
import { anything, instance, mock, when } from 'ts-mockito';

describe(FeatureTogglzService.name, () => {
  let featureTogglzService: FeatureTogglzService;

  let httpClient: HttpClient;

  beforeEach(async () => {
    httpClient = mock(HttpClient);

    when(httpClient.get(anything())).thenReturn(of([{ toggle: 'MASSNAHMEN', enabled: true }]));

    featureTogglzService = new FeatureTogglzService(instance(httpClient));

    await featureTogglzService.fetchTogglz();
  });

  it('should create service', () => {
    expect(featureTogglzService).toBeTruthy();
  });
});
