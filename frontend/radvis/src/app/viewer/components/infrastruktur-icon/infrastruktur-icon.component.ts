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
import { ChangeDetectionStrategy, Component, Input, OnInit } from '@angular/core';
import { MatIconRegistry } from '@angular/material/icon';
import { DomSanitizer } from '@angular/platform-browser';
import { Infrastruktur } from 'src/app/viewer/viewer-shared/models/infrastruktur';

@Component({
  selector: 'rad-infrastruktur-icon',
  templateUrl: './infrastruktur-icon.component.html',
  styleUrl: './infrastruktur-icon.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class InfrastrukturIconComponent implements OnInit {
  @Input()
  infrastruktur!: Infrastruktur;
  @Input()
  size = 24;

  readonly iconPrefix = 'infrastrukturen-icon-';

  constructor(
    private iconRegistry: MatIconRegistry,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    this.iconRegistry.addSvgIcon(
      this.iconPrefix + this.infrastruktur.name,
      this.sanitizer.bypassSecurityTrustResourceUrl('./assets/' + this.infrastruktur.iconFileName)
    );
  }
}
