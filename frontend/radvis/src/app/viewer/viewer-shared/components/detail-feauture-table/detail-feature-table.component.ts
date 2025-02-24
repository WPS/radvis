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

import { ChangeDetectionStrategy, Component, Input, OnChanges } from '@angular/core';
import { MatTableDataSource } from '@angular/material/table';
import { NetzDetailFeatureTableLink } from 'src/app/viewer/viewer-shared/models/netzdetail-feature-table-link';

interface TableRow {
  key: string;
  value: string | NetzDetailFeatureTableLink;
}

interface TableGroup {
  group: string;
}

@Component({
  selector: 'rad-detail-feature-table',
  templateUrl: './detail-feature-table.component.html',
  styleUrls: ['./detail-feature-table.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class DetailFeatureTableComponent implements OnChanges {
  @Input()
  public attribute!: Map<string, { [key: string]: string | NetzDetailFeatureTableLink }>;

  @Input()
  public leereAttributeVisible = false;
  public attributeForTable = new MatTableDataSource<TableRow | TableGroup>();

  public displayedColumns = ['key', 'value'];
  public nurLeereAttributeHinweisVisible = false;

  public isGroupItem = (index: number, item: TableRow | TableGroup): boolean => {
    return this.isGroup(item);
  };

  ngOnChanges(): void {
    this.attributeForTable.data = this.getGefilterteAttribute(this.leereAttributeVisible);
    this.nurLeereAttributeHinweisVisible = this.getGefilterteAttribute(false).length === 0;
  }

  private getGefilterteAttribute(considerLeereAttribute: boolean): (TableRow | TableGroup)[] {
    const result: (TableRow | TableGroup)[] = [];
    this.attribute.forEach((attributes, group) => {
      if (group !== '') {
        result.push({ group });
      }
      Object.entries(attributes).forEach(([k, v]) => {
        if (this.isValueNonEmpty(v) || considerLeereAttribute) {
          result.push({ key: k, value: v });
        }
      });
      if (result.length > 0 && 'group' in result[result.length - 1]) {
        result.pop();
      }
    });
    return result;
  }

  private isGroup(item: TableRow | TableGroup): item is TableGroup {
    return (item as any).group;
  }

  private isValueNonEmpty(value: string | NetzDetailFeatureTableLink): boolean {
    return value !== '' && value !== null && value !== 'Unbekannt';
  }
}
