/*
 * Copyright 2012 LinkedIn Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

$.namespace('azkaban');

var jobEditView;
azkaban.JobEditView = Backbone.View.extend({
    events: {
        "click": "closeEditingTarget",
        "click #set-btn": "handleSet",
        "click #set-btn2": "handleSet2",
        "click #cancel-btn": "handleCancel",
        "click #close-btn": "handleCancel",
        "click #add-btn": "handleAddRow",
        "click table .editable": "handleEditColumn",
        "click table .remove-btn": "handleRemoveColumn"
    },

    initialize: function (setting) {
        this.projectURL = contextURL + "manager"
        this.generalParams = {}
        this.overrideParams = {}
    },

    handleCancel: function (evt) {
        $('#job-edit-pane').hide();
        var tbl = document.getElementById("generalProps").tBodies[0];
        var rows = tbl.rows;
        var len = rows.length;
        for (var i = 0; i < len - 1; i++) {
            tbl.deleteRow(0);
        }
    },

    show: function (projectName, flowName, jobName) {
        this.projectName = projectName;
        this.flowName = flowName;
        this.jobName = jobName;

        var projectURL = this.projectURL

        $('#job-edit-pane').modal();

        var handleAddRow = this.handleAddRow;

        /*var overrideParams;
         var generalParams;
         this.overrideParams = overrideParams;
         this.generalParams = generalParams;*/
        var fetchJobInfo = {
            "project": this.projectName,
            "ajax": "fetchJobInfo",
            "flowName": this.flowName,
            "jobName": this.jobName
        };
        var mythis = this;
        var fetchJobSuccessHandler = function (data) {
            if (data.error) {
                alert(data.error);
                return;
            }
            document.getElementById('jobName').innerHTML = data.jobName;
            document.getElementById('jobType').innerHTML = data.jobType;
            var generalParams = data.generalParams;
            var overrideParams = data.overrideParams;

            /*for (var key in generalParams) {
             var row = handleAddRow();
             var td = $(row).find('span');
             $(td[1]).text(key);
             $(td[2]).text(generalParams[key]);
             }*/

            mythis.overrideParams = overrideParams;
            mythis.generalParams = generalParams;

            for (var okey in overrideParams) {
                if (okey != 'type' && okey != 'dependencies') {
                    var row = handleAddRow();
                    var td = $(row).find('span');
                    $(td[0]).text(okey);
                    $(td[1]).text(overrideParams[okey]);
                }
            }
        };

        $.get(projectURL, fetchJobInfo, fetchJobSuccessHandler, "json");
    },

    show2: function (projectName, jobName, nodeInfo, myCodeMirror,gooflow,nodeid) {
        //清除配置参数
        var tbl = document.getElementById("generalProps").tBodies[0];
        var rows = tbl.rows;
        var len = rows.length;
        for (var i = 0; i < len - 1; i++) {
            tbl.deleteRow(0);
        }

        this.projectName = projectName;
        this.jobName = jobName;
        this.myCodeMirror = myCodeMirror;
        this.editpanel = $('#job-edit-pane');
        this.scriptEditor=$("#script_editor");
        this.gooflow =gooflow;
        this.nodeid = nodeid;
        document.getElementById('jobName').innerHTML = this.jobName;
        document.getElementById('jobType').innerHTML = nodeInfo.type;
        $(this.editpanel).modal('show');
        var projectURL = this.projectURL

        $('.CodeMirror-linenumbers').css("width","29px");

        var handleAddRow = this.handleAddRow;

        /*var overrideParams;
         var generalParams;
         this.overrideParams = overrideParams;
         this.generalParams = generalParams;*/
        var fetchJobInfo = {
            "project": this.projectName,
            "ajax": "fetchJobInfo2",
            "jobName": this.jobName
        };
        var mythis = this;
        var fetchJobSuccessHandler = function (data) {
            if (data.error) {
                alert(data.error);
                return;
            }
            if (data.newjob){
                mythis.newJob(projectName, nodeInfo, mythis.myCodeMirror);
                $(mythis.editpanel).modal('show');
                return;
            }
            var jobName = data.jobName;
            if(data.overrideParams.showName){
                jobName = data.overrideParams.showName;
            }
            document.getElementById('jobName').innerHTML = jobName;
            document.getElementById('jobType').innerHTML = data.jobType;
            var generalParams = data.generalParams;
            var overrideParams = data.overrideParams;

            /*for (var key in generalParams) {
             var row = handleAddRow();
             var td = $(row).find('span');
             $(td[1]).text(key);
             $(td[2]).text(generalParams[key]);
             }*/

            mythis.overrideParams = overrideParams;
            mythis.generalParams = generalParams;
            mythis.fillJobEditPane(overrideParams,mythis.myCodeMirror);
            $(mythis.editpanel).modal('show');
            var scriptType = overrideParams.type;
            var fileName;
            if (scriptType == "command") {
                var reg = /[^\/]*\.sh/g;
                fileName = overrideParams["command"].match(reg);
            }
            else if (scriptType == "hive") {
                var reg = /[^\/]*\.sql/g;
                fileName = overrideParams["hive.script"].match(reg);
            }
            else if (scriptType == "druidIndex") {

            }

            if(!fileName){
                return;
            }

            var downloadScriptInfo = {
                "project": mythis.projectName,
                "ajax": "downloadScript",
                "fileName": fileName[0]
            };
            $.get(projectURL, downloadScriptInfo, downloadScriptHandler, "text");
        };


        $.get(projectURL, fetchJobInfo, fetchJobSuccessHandler, "json");

        var downloadScriptHandler = function (data) {
            myCodeMirror.setValue(data);
            $(mythis.scriptEditor).css("display","block");
        };

    },

    show3: function () {
        var handleAddRow = this.handleAddRow2;
        //$("#add-btn2").bind("click",handleAddRow);
        $("#job-files-panel").modal("show");
    },

    fillJobEditPane: function (overrideParams,myCodeMirror) {
        var arr = [ "type", "dependencies", "top", "left", "height" , "width" ,"name" ];
        $.inArray(okey, arr);  //返回 3,
        for (var okey in overrideParams) {
            if ($.inArray(okey, arr) == -1) {
                var row = this.handleAddRow();
                var td = $(row).find('span');
                $(td[0]).text(okey);
                $(td[1]).text(overrideParams[okey]);
            }
        }
        var scriptType = overrideParams.type;
        var mode
        switch (scriptType) {
            case "command":
            {
                mode = "shell"
            }
            case "hive":
            {
                mode = "sql"
            }
        }
        if(mode){
            myCodeMirror.setOption("mode", mode);
            $(this.scriptEditor).css("display","block");
        }else{
            $(this.scriptEditor).css("display","none");
        }
        myCodeMirror.setValue("");

    },
    
    newJob: function (projectName, nodeInfo, myCodeMirror) {
        this.overrideParams={};
        this.generalParams={};

        var scripttype = nodeInfo.type;
        this.overrideParams['type'] = scripttype;
        this.overrideParams['showName']=nodeInfo.name;
        var nodename = nodeInfo.name;
        var mode;
        switch (scripttype){
            case "command":{
                this.overrideParams['command']="sh ./scripts/" + nodename + ".sh";
                mode = "shell";
                break;
            }

            case "hive":{
                this.overrideParams['hive.script']="scripts/" + nodename + ".sql";
                this.overrideParams['user.to.proxy']="azkaban";
                mode = "sql";
                break;
            }
            case "druidIndex":{
                var date = new Date();
                var time1 = date.format("yyyy-MM-ddThh:mm");
                date.setDate(date.getDate() - 1)
                var time2 = date.format("yyyy-MM-ddThh:mm");
                this.overrideParams['datasourceId']="";
                this.overrideParams['datasourceName']="";
                this.overrideParams['segmentGranularity']="DAY";
                this.overrideParams['intervals']=time1 + "/" + time2;
                this.overrideParams['paths']="";
                this.overrideParams['numShards']=1;
                this.overrideParams['timestampColumn']="ts";
                this.overrideParams['timestampFormat']="millis";
                break;
            }
        }

        this.fillJobEditPane(this.overrideParams,myCodeMirror);

        //document.getElementById('jobName').innerHTML = "";
    },

    handleSet: function (evt) {
        this.closeEditingTarget(evt);
        var jobOverride = {};
        var editRows = $(".editRow");
        for (var i = 0; i < editRows.length; ++i) {
            var row = editRows[i];
            var td = $(row).find('span');
            var key = $(td[0]).text();
            var val = $(td[1]).text();

            if (key && key.length > 0) {
                jobOverride[key] = val;
            }
        }

        var overrideParams = this.overrideParams
        var generalParams = this.generalParams

        jobOverride['type'] = overrideParams['type']
        if ('dependencies' in overrideParams) {
            jobOverride['dependencies'] = overrideParams['dependencies']
        }

        var project = this.projectName
        var flowName = this.flowName
        var jobName = this.jobName

        var jobOverrideData = {
            project: project,
            flowName: flowName,
            jobName: jobName,
            ajax: "setJobOverrideProperty",
            jobOverride: jobOverride
        };

        var projectURL = this.projectURL
        var redirectURL = projectURL + '?project=' + project + '&flow=' + flowName + '&job=' + jobName;
        var jobOverrideSuccessHandler = function (data) {
            if (data.error) {
                alert(data.error);
            }
            else {
                window.location = redirectURL;
            }
        };

        $.get(projectURL, jobOverrideData, jobOverrideSuccessHandler, "json");
    },

    handleSet2: function (evt) {
        this.closeEditingTarget(evt);
        var jobOverride = {};
        var editRows = $(".editRow");
        for (var i = 0; i < editRows.length; ++i) {
            var row = editRows[i];
            var td = $(row).find('span');
            var key = $(td[0]).text();
            var val = $(td[1]).text();

            if (key && key.length > 0) {
                jobOverride[key] = val;
            }
        }

        var overrideParams = this.overrideParams
        var generalParams = this.generalParams

        jobOverride['type'] = overrideParams['type']
        if ('dependencies' in overrideParams) {
            jobOverride['dependencies'] = overrideParams['dependencies']
        }
        var script_content =this.myCodeMirror.getValue();
        var project = this.projectName
        var flowName = this.flowName
        var jobName = this.jobName

        var jobOverrideData = {
            project: project,
            flowName: flowName,
            jobName: jobName,
            ajax: "setJobOverrideProperty2",
            jobOverride: jobOverride,
            scriptContent:script_content
        };

        var projectURL = this.projectURL
        var redirectURL = projectURL + '?project=' + project + '&edit=1';

        var mythis=this;
        var jobOverrideSuccessHandler = function (data) {
            if (data.error) {
                alert(data.error);
            }
            else {
                //window.location = redirectURL;
                $(mythis.editpanel).modal('hide');
                var showName = jobName;
                if(jobOverride["showName"]){
                    showName=jobOverride["showName"]
                }
                mythis.gooflow.setName(mythis.nodeid,showName,"node");
            }
        };

        $.post(projectURL, jobOverrideData, jobOverrideSuccessHandler, "json");
    },

    saveProject: function (projectName,flowdata) {
        var projectURL = this.projectURL;
        this.projectName = projectName;
        var data = {
            project: projectName,
            ajax: "saveProject",
            data: JSON.stringify(flowdata)
        };
        var saveProjectSuccessHandler = function (data) {
            if (data.error) {
                alert(data.error);
            }
            else {
                saved = true;
                window.location = projectURL + "?project=" +projectName ;
            }
        };

        $.post(projectURL, data, saveProjectSuccessHandler, "json");
    },

    handleAddRow: function (evt) {
        var tr = document.createElement("tr");
        var tdName = document.createElement("td");
        $(tdName).addClass('property-key');
        var tdValue = document.createElement("td");

        var remove = document.createElement("div");
        $(remove).addClass("pull-right").addClass('remove-btn');
        var removeBtn = document.createElement("button");
        $(removeBtn).attr('type', 'button');
        $(removeBtn).addClass('btn').addClass('btn-xs').addClass('btn-danger');
        $(removeBtn).text('删除');
        $(remove).append(removeBtn);

        var nameData = document.createElement("span");
        $(nameData).addClass("spanValue");
        var valueData = document.createElement("span");
        $(valueData).addClass("spanValue");

        $(tdName).append(nameData);
        $(tdName).addClass("editable");
        nameData.myparent = tdName;

        $(tdValue).append(valueData);
        $(tdValue).append(remove);
        $(tdValue).addClass("editable");
        $(tdValue).addClass("value");
        valueData.myparent = tdValue;

        $(tr).addClass("editRow");
        $(tr).append(tdName);
        $(tr).append(tdValue);

        $(tr).insertBefore("#addRow");
        return tr;
    },

    handleAddRow2: function (evt) {
        var tr = document.createElement("tr");
        var tdValue = document.createElement("td");

        var remove = document.createElement("div");
        $(remove).addClass("pull-right").addClass('remove-btn');
        var removeBtn = document.createElement("button");
        $(removeBtn).attr('type', 'button');
        $(removeBtn).addClass('btn').addClass('btn-xs').addClass('btn-danger');
        $(removeBtn).text('删除');
        $(remove).append(removeBtn);

        var valueData = document.createElement("span");
        $(valueData).addClass("spanValue");


        $(tdValue).append(valueData);
        $(tdValue).append(remove);
        $(tdValue).addClass("editable");
        $(tdValue).addClass("value");
        valueData.myparent = tdValue;

        $(tr).addClass("editRow");
        $(tr).append(tdValue);

        $(tr).insertBefore("#addRow_file");
        return tr;
    },
    handleEditColumn: function (evt) {
        var curTarget = evt.currentTarget;
        if (this.editingTarget != curTarget) {
            this.closeEditingTarget(evt);

            var text = $(curTarget).children(".spanValue").text();
            $(curTarget).empty();

            var input = document.createElement("input");
            $(input).attr("type", "text");
            $(input).addClass("form-control").addClass("input-sm");
            $(input).val(text);

            $(curTarget).addClass("editing");
            $(curTarget).append(input);
            $(input).focus();
            var obj = this;
            $(input).keypress(function (evt) {
                if (evt.which == 13) {
                    obj.closeEditingTarget(evt);
                }
            });
            this.editingTarget = curTarget;
        }

        evt.preventDefault();
        evt.stopPropagation();
    },

    handleRemoveColumn: function (evt) {
        var curTarget = evt.currentTarget;
        // Should be the table
        var row = curTarget.parentElement.parentElement;
        $(row).remove();
    },

    closeEditingTarget: function (evt) {
        if (this.editingTarget == null ||
            this.editingTarget == evt.target ||
            this.editingTarget == evt.target.myparent) {
            return;
        }
        var input = $(this.editingTarget).children("input")[0];
        var text = $(input).val();
        $(input).remove();

        var valueData = document.createElement("span");
        $(valueData).addClass("spanValue");
        $(valueData).text(text);

        if ($(this.editingTarget).hasClass("value")) {
            var remove = document.createElement("div");
            $(remove).addClass("pull-right").addClass('remove-btn');
            var removeBtn = document.createElement("button");
            $(removeBtn).attr('type', 'button');
            $(removeBtn).addClass('btn').addClass('btn-xs').addClass('btn-danger');
            $(removeBtn).text('Delete');
            $(remove).append(removeBtn);
            $(this.editingTarget).append(remove);
        }

        $(this.editingTarget).removeClass("editing");
        $(this.editingTarget).append(valueData);
        valueData.myparent = this.editingTarget;
        this.editingTarget = null;
    }
});

$(function () {
    jobEditView = new azkaban.JobEditView({
        el: $('#job-edit-pane')
    });
});
