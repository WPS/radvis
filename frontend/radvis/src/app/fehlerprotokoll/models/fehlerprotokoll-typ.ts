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

export class FehlerprotokollTyp {
  public static readonly DLM_REIMPORT_JOB_MASSNAHMEN = new FehlerprotokollTyp(
    'DLM_REIMPORT_JOB_MASSNAHMEN',
    'DLM-Reimport (Maßnahmen)'
  );
  public static readonly DLM_REIMPORT_JOB_FAHRRADROUTEN = new FehlerprotokollTyp(
    'DLM_REIMPORT_JOB_FAHRRADROUTEN',
    'DLM-Reimport (Fahrradrouten)'
  );
  public static readonly TOUBIZ_IMPORT_FAHRRADROUTEN = new FehlerprotokollTyp(
    'TOUBIZ_IMPORT_FAHRRADROUTEN',
    'Toubiz-Routen Import'
  );
  public static readonly TFIS_IMPORT_FAHRRADROUTEN = new FehlerprotokollTyp(
    'TFIS_IMPORT_FAHRRADROUTEN',
    'TFIS-Routen Import'
  );
  public static readonly TFIS_IMPORT_LRFW = new FehlerprotokollTyp(
    'TFIS_IMPORT_LRFW',
    'Landesradfernwege (Import aus TFIS)'
  );

  public static readonly OSM_ABBILDUNG_RADNETZ = new FehlerprotokollTyp(
    'OSM_ABBILDUNG_RADNETZ',
    ' OSM-Ausleitung (RadNETZ)'
  );
  public static readonly OSM_ABBILDUNG_KREISNETZ = new FehlerprotokollTyp(
    'OSM_ABBILDUNG_KREISNETZ',
    ' OSM-Ausleitung (Kreisnetz)'
  );
  public static readonly OSM_ABBILDUNG_KOMMUNALNETZ = new FehlerprotokollTyp(
    'OSM_ABBILDUNG_KOMMUNALNETZ',
    ' OSM-Ausleitung (Kommunalnetz)'
  );
  public static readonly OSM_ABBILDUNG_SONSTIGE = new FehlerprotokollTyp(
    'OSM_ABBILDUNG_SONSTIGE',
    ' OSM-Ausleitung (Sonstige)'
  );

  constructor(public readonly name: string, public readonly displayText: string) {}

  public static getAll(): FehlerprotokollTyp[] {
    return Object.values(FehlerprotokollTyp);
  }
}
