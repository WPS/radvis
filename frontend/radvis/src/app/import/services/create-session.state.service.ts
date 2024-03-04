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

import { Injectable } from '@angular/core';
import { AttributeImportFormat } from 'src/app/import/attribute/models/attribute-import-format';
import { ImportTyp } from 'src/app/import/models/import-typ';
import { AttributeParameter } from '../attribute/models/attribute-parameter';
import { DateiUploadInfo } from '../models/datei-upload-info';
import { NetzklassenParameter } from '../netzklassen/models/netzklassen-parameter';

@Injectable()
export class CreateSessionStateService {
  public static DATEI_PARAMETER_REIHENFOLGE_ERROR = 'Erst Datei hochladen, dann die Parameter eingeben.';
  public static IMPORT_TYP_MISMATCH_ERROR = 'Parameter und Import-Typ passen nicht zusammen';
  public static IMPORT_FORMAT_MISMATCH_ERROR = 'Dateiformat und Import-Typ passen nicht zusammen';

  private _dateiUploadInfo: DateiUploadInfo | null = null;

  public get dateiUploadInfo(): DateiUploadInfo | null {
    return this._dateiUploadInfo;
  }

  private _parameterInfo: NetzklassenParameter | AttributeParameter | null = null;
  public get parameterInfo(): NetzklassenParameter | AttributeParameter | null {
    return this._parameterInfo;
  }

  private _attributeImportFormat: AttributeImportFormat | null = null;

  public updateDateiUploadInfo(info: DateiUploadInfo): void {
    this._dateiUploadInfo = info;
  }

  public get attributeImportFormat(): AttributeImportFormat | null {
    return this._attributeImportFormat;
  }

  public updateAttributeImportFormat(format: AttributeImportFormat): void {
    if (this.dateiUploadInfo == null) {
      throw new Error(CreateSessionStateService.DATEI_PARAMETER_REIHENFOLGE_ERROR);
    }
    if (this.dateiUploadInfo.importTyp !== ImportTyp.ATTRIBUTE_UEBERNEHMEN) {
      throw new Error(CreateSessionStateService.IMPORT_FORMAT_MISMATCH_ERROR);
    }
    this._attributeImportFormat = format;
  }

  public updateParameterInfo(parameter: NetzklassenParameter | AttributeParameter): void {
    if (this.dateiUploadInfo == null) {
      throw new Error(CreateSessionStateService.DATEI_PARAMETER_REIHENFOLGE_ERROR);
    }
    if (parameter instanceof NetzklassenParameter && this.dateiUploadInfo.importTyp !== ImportTyp.NETZKLASSE_ZUWEISEN) {
      throw new Error(CreateSessionStateService.IMPORT_TYP_MISMATCH_ERROR);
    }
    if (parameter instanceof AttributeParameter && this.dateiUploadInfo.importTyp !== ImportTyp.ATTRIBUTE_UEBERNEHMEN) {
      throw new Error(CreateSessionStateService.IMPORT_TYP_MISMATCH_ERROR);
    }
    this._parameterInfo = parameter;
  }

  public reset(): void {
    this._dateiUploadInfo = null;
    this._parameterInfo = null;
    this._attributeImportFormat = null;
  }
}
