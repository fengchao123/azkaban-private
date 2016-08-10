/**
 * Created by fengxj on 8/9/16.
 */
$(function () {
    $("#project-save-btn").bind("click",function (e) {
        var flowdata = flow.exportData();
        var projectName = $('#project_name').val();
        jobEditView.saveProject(projectName,flowdata);
    });
});