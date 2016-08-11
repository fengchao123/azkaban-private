/**
 * Created by fengxj on 8/9/16.
 */
var saved=false;
$(function () {
    $("#project-save-btn").bind("click", function (e) {
        var flowdata = flow.exportData();
        var projectName = $('#project_name').val();
        jobEditView.saveProject(projectName, flowdata);
    });
    $("#project-files-btn").bind("click", function (e) {
        listDepFiles();
        $("#job-files-panel").modal("show");
    });
    $("#add_file-btn").bind("click", function (e) {
        $("#file").click();
        //uploadFile();
    });
    $("#file").bind("change", function (e) {
        uploadFile();
        var obj = document.getElementById('file') ;
        obj.outerHTML=obj.outerHTML;
    });

});

window.onbeforeunload = function(event) {
    if(!saved){
        (event || window.event).returnValue = "确定退出吗";
    }

}
window.setInterval(heartbeat,5000)
function heartbeat() {
    var projectURL = contextURL + "manager";
    var projectName = $('#project_name').val();
    HeartBeatInfo={
        project: projectName,
        ajax: "heartbeat"
    }
    $.get(projectURL, HeartBeatInfo, null, "json");
}
function listDepFiles() {
    var tbl = document.getElementById("filelist").tBodies[0];
    var rows = tbl.rows;
    var len = rows.length;
    for (var i = 0; i < len - 1; i++) {
        tbl.deleteRow(0);
    }

    var projectURL = contextURL + "manager";
    var projectName = $('#project_name').val();
    fetchJobInfo = {
        project: projectName,
        ajax: "listDepFile"
    }
    var fetchJobSuccessHandler = function (data) {
        if (data.errMsg) {
            alter(data.errMsg);
            return;
        }
        for (var fileIndex in data.fileNames) {
            var row = handleAddRow2();
            var td = $(row).find('span');
            $(td[0]).text(data.fileNames[fileIndex]);
        }
    }
    $.get(projectURL, fetchJobInfo, fetchJobSuccessHandler, "json");
}

function handleAddRow2() {
    var tr = document.createElement("tr");
    var tdValue = document.createElement("td");

    var remove = document.createElement("div");
    $(remove).addClass("pull-right").addClass('remove_file-btn');
    $(remove).attr("onclick", "handleRemoveColumn(this)");
    var removeBtn = document.createElement("button");
    $(removeBtn).attr('type', 'button');
    $(removeBtn).addClass('btn').addClass('btn-xs').addClass('btn-danger');
    $(removeBtn).text('删除');
    $(remove).append(removeBtn);

    var valueData = document.createElement("span");
    $(valueData).addClass("spanValue");


    $(tdValue).append(valueData);
    $(tdValue).append(remove);
    $(tdValue).addClass("editable_file");
    $(tdValue).addClass("value");
    valueData.myparent = tdValue;

    $(tr).addClass("editRow");
    $(tr).append(tdValue);

    $(tr).insertBefore("#addRow_file");
    return tr;
}
function handleRemoveColumn(target) {
    $(target).parents(".editRow").remove();
}

function uploadFile() {
    var formData = new FormData($('#upload-file-form')[0]);

    $.ajax({
        url: contextURL + "manager",  //server script to process data
        type: 'POST',
        xhr: function () {  // custom xhr
            myXhr = $.ajaxSettings.xhr();
            if (myXhr.upload) { // check if upload property exists
                myXhr.upload.addEventListener('progress', progressHandlingFunction, false); // for handling the progress of the upload
            }
            return myXhr;
        },
        //Ajax事件
        beforeSend: beforeSendHandler,
        success: completeHandler,
        error: errorHandler,
        // Form数据
        data: formData,
        //Options to tell JQuery not to process data or worry about content-type
        cache: false,
        contentType: false,
        processData: false

    });
}

function beforeSendHandler(e) {

}
function completeHandler(e) {
    listDepFiles();
}
function errorHandler(e) {
    alert("error");
}
function progressHandlingFunction(e) {
    if (e.lengthComputable) {
        $('progress').attr({value: e.loaded, max: e.total});
    }
}