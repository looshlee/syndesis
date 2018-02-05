import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

import { ChartsModule } from 'ng2-charts/ng2-charts';
import { ModalModule } from 'ngx-bootstrap';
import { TooltipModule } from 'ngx-bootstrap/tooltip';
import { BsDropdownModule } from 'ngx-bootstrap/dropdown';

import { SyndesisCommonModule } from '../common/common.module';
import { IntegrationListModule } from '@syndesis/ui/integration/list';

import { DashboardComponent } from './dashboard.component';
import { EmptyStateComponent } from './emptystate.component';

import { DashboardConnectionsComponent } from './connections.component';
import { DashboardIntegrationsComponent } from './integrations.component';

const routes: Routes = [
  { path: '', component: DashboardComponent, pathMatch: 'full' }
];

@NgModule({
  imports: [
    CommonModule,
    RouterModule.forChild(routes),
    SyndesisCommonModule,
    ChartsModule,
    ModalModule,
    TooltipModule,
    IntegrationListModule,
    BsDropdownModule.forRoot()
  ],
  declarations: [
    DashboardComponent,
    DashboardConnectionsComponent,
    DashboardIntegrationsComponent,
    EmptyStateComponent
  ]
})
export class DashboardModule {}
