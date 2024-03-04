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

import { Verwaltungseinheit } from 'src/app/shared/models/verwaltungseinheit';
import {
  defaultBundeslandOrganisation,
  defaultOrganisation,
} from 'src/app/shared/models/organisation-test-data-provider.spec';
import { OrganisationPipe } from 'src/app/form-elements/components/organisation.pipe';

describe('OrganisationPipe', () => {
  let pipe: OrganisationPipe;

  beforeEach(() => {
    pipe = new OrganisationPipe();
  });

  it('create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('should work correctly with instance', () => {
    expect(pipe.transform(undefined)).toEqual('');
    expect(pipe.transform(defaultOrganisation)).toEqual('Eine Organisation (Gemeinde)');
    expect(pipe.transform(defaultBundeslandOrganisation)).toEqual('Baden-Württemberg (Bundesland)');
    expect(
      pipe.transform({
        ...defaultBundeslandOrganisation,
        aktiv: false,
      })
    ).toEqual('Baden-Württemberg (Bundesland, inaktiv)');
  });

  it('should work correctly with static method', () => {
    expect(Verwaltungseinheit.getDisplayName(null)).toEqual('');
    expect(
      Verwaltungseinheit.getDisplayName({
        ...defaultOrganisation,
        aktiv: false,
      })
    ).toEqual('Eine Organisation (Gemeinde, inaktiv)');
  });
});
