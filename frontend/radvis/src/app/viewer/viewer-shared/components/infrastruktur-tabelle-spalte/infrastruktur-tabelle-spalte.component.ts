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
import { ChangeDetectionStrategy, Component, Input, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { MatColumnDef } from '@angular/material/table';
import { SpaltenDefinition } from 'src/app/viewer/viewer-shared/models/spalten-definition';
import invariant from 'tiny-invariant';

@Component({
  selector: 'rad-infrastruktur-tabelle-spalte',
  templateUrl: './infrastruktur-tabelle-spalte.component.html',
  styleUrl: './infrastruktur-tabelle-spalte.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false,
})
export class InfrastrukturTabelleSpalteComponent implements OnInit {
  @Input()
  spaltenDefinition!: SpaltenDefinition;
  @Input()
  getElementValueFn!: (item: any, key: string) => string | string[];
  @Input()
  /**
   * (Optional) alternatives custom template für tabellenspalte.
   * Bsp.:
   * <ng-template let-element="element" let-name="name" #cellTemplate>
   *  @if(name==='test') {
   *    <a [routeLink]="testLink">getElementValueFn(element)</a>
   *  } @else {
   *    getElementValueFn(element)
   *  }
   * </ng-template>
   */
  cellTemplate: TemplateRef<any> | undefined;

  @ViewChild(MatColumnDef) matColumnDef: MatColumnDef | undefined;

  ngOnInit(): void {
    invariant(this.spaltenDefinition);
    if (!Boolean(this.cellTemplate)) {
      invariant(this.getElementValueFn);
    }
  }

  get expandable(): boolean {
    if (this.spaltenDefinition?.expandable) {
      return true;
    }

    return false;
  }

  get width(): string {
    return this.spaltenDefinition?.width ?? 'auto';
  }
}
