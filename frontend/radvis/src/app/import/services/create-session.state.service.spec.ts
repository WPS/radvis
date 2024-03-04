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

import { AttributeParameter } from 'src/app/import/attribute/models/attribute-parameter';
import { DateiUploadInfo } from 'src/app/import/models/datei-upload-info';
import { ImportTyp } from 'src/app/import/models/import-typ';
import { NetzklassenParameter } from 'src/app/import/netzklassen/models/netzklassen-parameter';
import { CreateSessionStateService } from 'src/app/import/services/create-session.state.service';
import { Netzklasse } from 'src/app/shared/models/netzklasse';

describe('CreateSessionStateService', () => {
  let service: CreateSessionStateService;

  beforeEach(() => {
    service = new CreateSessionStateService();
  });

  it('should throw if parameter without datei upload', () => {
    expect(() => service.updateParameterInfo(NetzklassenParameter.of(Netzklasse.KOMMUNALNETZ_ALLTAG))).toThrowError(
      CreateSessionStateService.DATEI_PARAMETER_REIHENFOLGE_ERROR
    );
  });

  it('should throw if parameter not matching import typ - attribute', () => {
    service.updateDateiUploadInfo(DateiUploadInfo.of(ImportTyp.ATTRIBUTE_UEBERNEHMEN, new File([], 'testFile'), 1));
    expect(() => service.updateParameterInfo(NetzklassenParameter.of(Netzklasse.KOMMUNALNETZ_ALLTAG))).toThrowError(
      CreateSessionStateService.IMPORT_TYP_MISMATCH_ERROR
    );
  });

  it('should throw if parameter not matching import typ - netzklasse', () => {
    service.updateDateiUploadInfo(DateiUploadInfo.of(ImportTyp.NETZKLASSE_ZUWEISEN, new File([], 'testFile'), 1));
    expect(() => service.updateParameterInfo(AttributeParameter.of(['testAttribut']))).toThrowError(
      CreateSessionStateService.IMPORT_TYP_MISMATCH_ERROR
    );
  });
});
