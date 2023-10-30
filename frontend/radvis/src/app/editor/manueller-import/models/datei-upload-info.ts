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

import { ImportTyp } from 'src/app/editor/manueller-import/models/import-typ';
import invariant from 'tiny-invariant';

export class DateiUploadInfo {
  public get organisation(): number {
    return this._organisation;
  }

  public get file(): File {
    return this._file;
  }

  public get importTyp(): ImportTyp {
    return this._importTyp;
  }

  private constructor(private _importTyp: ImportTyp, private _file: File, private _organisation: number) {}

  public static of(importTyp: ImportTyp, file: File, organisation: number): DateiUploadInfo {
    invariant(importTyp);
    invariant(file);
    invariant(organisation);
    return new DateiUploadInfo(importTyp, file, organisation);
  }
}
