/*******************************************************************************
 * Copyright 2021 Cognizant Technology Solutions
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
import { Component, OnInit, ViewChild } from '@angular/core';
import { Router, NavigationExtras } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatPaginator } from '@angular/material/paginator';
import { MatTableDataSource } from '@angular/material/table';
import { MessageDialogService } from '../../application-dialog/message-dialog-service';
import { DashboardDetailsDialog } from '@insights/app/modules/dashboard-pdf-download/dashboard-details-dialog/dashboard-details-dialog';
import { ReportManagementService } from '../../reportmanagement/reportmanagement.service';
import { DataSharedService } from '@insights/common/data-shared-service';
import { MileStoneService } from '../mile-stone.service';
import { MileStoneDialog } from '../mile-stone-dialog/milestone-dialog';
import { MatRadioChange } from '@angular/material/radio';

@Component({
  selector: 'app-mile-stone-list',
  templateUrl: './mile-stone-list.component.html',
  styleUrls: ['./mile-stone-list.component.css', './../../home.module.css']
})
export class MileStoneListComponent implements OnInit {
  displayedColumns = [];
  @ViewChild(MatPaginator, { static: true }) paginator: MatPaginator;
  mileStoneDatasource = new MatTableDataSource<any>();
  enableEdit: boolean = false;
  onRadioBtnSelect: boolean = false;
  dashConfigList: any;
  data: any[];
  kpiId: number;
  kpiName: string;
  toolname: string;
  groupName: string;
  category: string;
  refreshRadio: boolean = false;
  milestone: any;
  showConfirmMessage: string;
  type: string;
  enableRefresh: boolean = false;
  MAX_ROWS_PER_TABLE = 10;
  orgArr = [];
  orgName: any;
  isDatainProgress: boolean = false;
  disablebutton = [];
  timeZone: string = '';
  count: number;
  disableRestart = true; 
  selectedIndex = -1;
  currentPageValue : number;


  constructor(public messageDialog: MessageDialogService,
    public dataShare: DataSharedService, public router: Router, public dialog: MatDialog,
    public reportmanagementService: ReportManagementService, private mileStoneService: MileStoneService
  ) {
  }

  ngOnInit() {
    this.getAllConfig();
    this.displayedColumns = ['radio', 'mileStoneName', 'startDate', 'endDate', 'status', 'details'];
    this.currentPageValue = this.paginator.pageIndex * this.MAX_ROWS_PER_TABLE;
  }

  ngAfterViewInit() {
    this.mileStoneDatasource.paginator = this.paginator;
  }

  add() {
    this.mileStoneService.iconClkSubject.next('CLICK');
    this.router.navigate(['InSights/Home/milestone'], { skipLocationChange: true });
  }
  edit() {
    this.mileStoneService.iconClkSubject.next('CLICK');
    let milestoneJson = this.milestone;
    let selectedOutcomeList = [];
    let existingOutcomeList = [];
    let serviceName= '';
    for (const element of milestoneJson.listOfOutcomes) {
     // let id = element.id;
     // let outcome = element.outcome;
     // selectedOutcomeList.push({id:id,outcome:outcome});
     selectedOutcomeList.push(element.outcomeId);
     existingOutcomeList.push(element.id);
     serviceName = element.serviceName;
    }

    let navigationExtras: NavigationExtras = {
      skipLocationChange: true,
      queryParams: {
        "id": milestoneJson.id,
        "mileStoneName": milestoneJson.mileStoneName,
        "startDate": milestoneJson.startDate,
        "endDate": milestoneJson.endDate,
        "outcomeList": selectedOutcomeList,
        "existingOutcomeList": existingOutcomeList
      }
    };
    this.router.navigate(['InSights/Home/editMileStone'], navigationExtras);
  }
  refresh() {
    this.selectedIndex = -1;
    this.getAllConfig();
    this.refreshRadio = false;
    this.onRadioBtnSelect = false;

  }
  enableButtons(event: MatRadioChange, index) {
    this.selectedIndex = index + this.currentPageValue ;
    this.onRadioBtnSelect = true;
    this.disablebutton[index] = false;
    if(event.value.status == "ERROR") {
      this.disableRestart = false;
    } else {
      this.disableRestart = true;
    }
  }

  changeCurrentPageValue() {
    this.selectedIndex = -1;
    this.currentPageValue = this.paginator.pageIndex  * this.MAX_ROWS_PER_TABLE;
   }

  async getAllConfig() {
    this.isDatainProgress = true;
    var self = this;
    self.refreshRadio = false;
    let mileStoneList = await this.mileStoneService.fetchMileStoneConfig();
    if (mileStoneList.status === "success") {
      this.count = 0;
      this.mileStoneDatasource.data = mileStoneList.data;
      for (var element of this.mileStoneDatasource.data) {
        if (this.count < this.mileStoneDatasource.data.length) {
          this.mileStoneDatasource.data[this.count].startDate = this.dataShare.convertDateToSpecificDateFormat(
            new Date(this.mileStoneDatasource.data[this.count].startDate * 1000), "yyyy-MM-dd");
            this.mileStoneDatasource.data[this.count].endDate = this.dataShare.convertDateToSpecificDateFormat(
              new Date(this.mileStoneDatasource.data[this.count].endDate * 1000), "yyyy-MM-dd");
        }
        this.count += 1;
      }
    }
    this.isDatainProgress = false;
    this.mileStoneDatasource.paginator = this.paginator;
  }

  applyFilter(filterValue: string) {
    this.mileStoneDatasource.filter = filterValue.trim();
  }

  showAllDetails(data) {
    console.log(data)
    for(var item in data["listOfOutcomes"]) {
      data["listOfOutcomes"][item]["lastUpdatedDate"] = this.dataShare.convertDateToSpecificDateFormat(new Date(data["listOfOutcomes"][item]["lastUpdatedDate"]), "yyyy-MM-dd HH:mm:ss");
    }
    let showDetailsDialog = this.dialog.open(MileStoneDialog, {
      panelClass: 'showjson-dialog-container',
      width: '80%',
      // height: '75%',
      disableClose: true,
      data: { outcomeList: data, showCardDetail: true }
    });
  }

  delete() {
    var self = this;
    let data = self.milestone;
    var title = "Delete Milestone";
    var dialogmessage =
      "Do you want to delete a MileStone <b>" +
      "</b>? <br><br> <b> Please note: </b> The action of deleting a Milestone " +
      "<b>" +
      "</b> CANNOT be UNDONE. Do you want to continue? ";
    const dialogRef = self.messageDialog.showConfirmationMessage(
      title,
      dialogmessage,
      this.milestone.id,
      "ALERT",
      "40%"
    );
    dialogRef.afterClosed().subscribe((result) => {
      if (result == "yes") {
        self.mileStoneService
          .deleteMileStone(JSON.stringify(self.milestone.id))
          .then(function (data) {
            console.log(data)
            if (data.status === "success") {
              self.messageDialog.showApplicationsMessage("<b>" + "Deleted Successfully" + "</b>", "SUCCESS");
              self.refresh();
            } else if (data.status == 'failure') {
              console.error(data)
              self.messageDialog.showApplicationsMessage(data.message, "ERROR");
            } else {
              self.messageDialog.showApplicationsMessage("Failed to delete Please check logs for details.", "ERROR");
            }
          })
          .catch(function (data) {
            self.showConfirmMessage = "service_error";
          });
      }
    });
  }

  restartMileStone(){
    var self = this;
    let data = self.milestone;
    this.disableRestart = true;
    var title = "Restart Milestone";
    var dialogmessage =
      "Do you want to restart a MileStone <b>" +
      "</b>? <br><br> <b> Please note: </b> It will restart only Errored Outcomes " +
      "<b>" +
      "</b> CANNOT be UNDONE. Do you want to continue? ";
    const dialogRef = self.messageDialog.showConfirmationMessage(
      title,
      dialogmessage,
      this.milestone.id,
      "ALERT",
      "40%"
    );
    dialogRef.afterClosed().subscribe((result) => {
      if (result == "yes") {
        var requestJson ={}
        requestJson['id'] = self.milestone.id;
        self.mileStoneService
          .restartMileStone(JSON.stringify(requestJson))
          .then(function (data) {
            if (data.status === "success") {
              self.messageDialog.showApplicationsMessage("<b>" + "Restarted Successfully" + "</b>", "SUCCESS");
              self.refresh();
            } else if (data.status == 'failure') {
              console.error(data)
              self.messageDialog.showApplicationsMessage(data.message, "ERROR");
            } else {
              self.messageDialog.showApplicationsMessage("Failed to restart Please check logs for details.", "ERROR");
            }
          })
          .catch(function (data) {
            self.showConfirmMessage = "service_error";
          });
      }
    });
  }

}



