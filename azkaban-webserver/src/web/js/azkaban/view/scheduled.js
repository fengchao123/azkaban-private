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

var slaView;
var tableSorterView;
$(function() {
  slaView = new azkaban.ChangeSlaView({el:$('#sla-options')});
  tableSorterView = new azkaban.TableSorter({el:$('#scheduledFlowsTbl')});
  //var requestURL = contextURL + "/manager";

  // Set up the Flow options view. Create a new one every time :p
  //$('#addSlaBtn').click( function() {
  //  slaView.show();
  //});
});



var advFilterView;
azkaban.AdvFilterView = Backbone.View.extend({
  events: {
    "click #filter-btn": "handleAdvFilter"
  },

  initialize: function(settings) {
    $('#datetimebegin').datetimepicker();
    $('#datetimeend').datetimepicker();
    $('#datetimebegin').on('change.dp', function(e) {
      $('#datetimeend').data('DateTimePicker').setStartDate(e.date);
    });
    $('#datetimeend').on('change.dp', function(e) {
      $('#datetimebegin').data('DateTimePicker').setEndDate(e.date);
    });
    $('#adv-filter-error-msg').hide();
  },

  handleAdvFilter: function(evt) {
    console.log("handleAdv");
    var projcontain = $('#projcontain').val();
    var flowcontain = $('#flowcontain').val();
    var usercontain = $('#usercontain').val();
    var status = $('#status option:selected').text();
    var period_units = $('#period_units').val();
    var has_shortMessage = $('#has_shortMessage').val();
    var begin  = $('#datetimebegin').val();
    var end    = $('#datetimeend').val();

    console.log("filtering schedule");

   // var historyURL = contextURL + "/history"
    var redirectURL = contextURL + "/schedule"

    var requestURL = redirectURL + "?advfilter=true" + "&projcontain=" + projcontain + "&flowcontain=" + flowcontain + "&usercontain=" + usercontain + "&period_units=" + period_units +"&status="+status+"&has_shortMessage="+has_shortMessage+ "&begin=" + begin + "&end=" + end ;
    window.location = requestURL;
  },

  render: function() {
  }
});

$(function() {
  filterView = new azkaban.AdvFilterView({el: $('#adv-filter')});
  $('#adv-filter-btn').click( function() {
    $('#adv-filter').modal();
  });
});

function queryallschedule(){
    var redirectURL = contextURL + "/schedule";
    window.location = redirectURL;
}
