$.namespace('azkaban');


$(function(){
/*
     $("#searchUser").click(function(){
        var serach_text = $("#userName_serach").val();
        var _url = "/userManager?search="+serach_text;
        $("#search-form").attr("action",_url);
        $("#search-form").submit();
     });
*/
     userManagerView = new azkaban.UserManagerView({
         el: $('#add_userManager')
       });

    $("#addUser").bind("click",function(){
        userManagerView.display("", true);
        $("#user-box").val('');
        $("#pass-box").val('');
        $("#group-box").val('');
        $("#roles-box").val('');
        $("#lxdh-box").val('');
        $("#email-box").val('');
        $("#user-box").attr('readonly',false);
        $("#group-box option").eq(0).attr('selected',false);
        $("#roles-box option").eq(0).attr('selected',false);
    });


});


var userManagerView;
azkaban.UserManagerView= Backbone.View.extend({
  events: {
    "click #change-btn": "handleAddOrEditUser"
  },

  initialize: function(settings) {
    $('#change-user-error-msg').hide();
  },

  display: function(userid, newPerm) {
    this.newPerm = newPerm;

    if (newPerm) {
        $('#change-title').text("Add New User");
    }
    else {
        $('#change-title').text("edit User");
    }

    userManagerView.render();
    $('#add_userManager').modal().on('hide.bs.modal', function(e) {
      $('#change-user-error-msg').hide();
    });
  },

  render: function() {
  },

  handleAddOrEditUser : function(evt) {
    var flag = $("#user-box").attr('readonly');
    var requestURL = contextURL + "/userManager";
    var name = $('#user-box').val().trim();
    var pass = $('#pass-box').val().trim();
   // var group = $('#group-box').val().trim();
   var groupStr="";
   $("#group-box option:selected").each(function(){
           groupStr+=$(this).text()+",";
    });
    var roleStr="";
  //  var roles = $('#roles-box').val().trim();
    $("#roles-box option:selected").each(function(){
        roleStr+=$(this).text()+",";
    });
    var group = groupStr.substring(0,groupStr.length-1);
    var roles = roleStr.substring(0,roleStr.length-1);
    var lxdh = $('#lxdh-box').val().trim();
    var email = $('#email-box').val().trim();

    var type = "";
    var ajax = "";
    if(flag!=undefined){
        type="editUser";
        ajax="editUser";
    }else{
        type="addUser";
        ajax="addUser";
    }
    var requestData = {
      "type": type,
      "ajax": ajax,
      "name": name,
      "pass": pass,
      "group": group,
      "roles": roles,
      "lxdh": lxdh,
      "email": email
    };

    var successHandler = function(data) {
      console.log("Output");
      if (data.error) {
        $("#change-user-error-msg").text(data.error);
        $("#change-user-error-msg").slideDown();
        return;
      }

      var replaceURL = requestURL;
      window.location.replace(replaceURL);
    };
    $.get(requestURL, requestData, successHandler, "json");
    }

});

//edit user
function editUser($this){
    $("#user-box").val('');
    $("#pass-box").val('');
  //  $("#group-box").val('');
  //  $("#roles-box").val('');
    $("#lxdh-box").val('');
    $("#email-box").val('');
    $("#group-box option").eq(0).attr('selected',false);
    $("#roles-box option").eq(0).attr('selected',false);
    var id = $this.id;
    var userName = $("#"+id).parent().siblings("#username").html();
    var pass     = $("#"+id).parent().siblings("#pass").html();
    var group    = $("#"+id).parent().siblings("#group").html();
    var roles    = $("#"+id).parent().siblings("#roles").html();
    var lxdh     = $("#"+id).parent().siblings("#lxdh").html();
    var email    = $("#"+id).parent().siblings("#email").html();
    var group_array = group.split(",");
    var roles_array = roles.split(",");

        $("#group-box option").each(function(){
             var optionVal = $(this).attr("id");
             if(optionVal!=null&&optionVal!=''){
                 for(var i = 0;i<group_array.length;i++){
                     if(optionVal==group_array[i]){
                        $(this).attr("selected",true);
                        break;
                      }
                   }
              }
        });



    $("#roles-box option").each(function(){
         var optionVal = $(this).attr("id");;
         if(optionVal!=null&&optionVal!=''){
             for(var i = 0;i<roles_array.length;i++){
                 if(roles_array[i]==optionVal){
                    $(this).attr("selected",true);
                    break;
                  }
                }
         }
    });


    userManagerView.display("", false);
    $("#user-box").val(userName);
    $("#pass-box").val(pass);
  //  $("#group-box").val(group);
 //   $("#roles-box").val(roles);
    $("#lxdh-box").val(lxdh);
    $("#email-box").val(email);
    $("#user-box").attr('readonly',true);
}

function deleteUser($this){
    var id = $this.id;
    if(confirm("是否确认删除")){
        var requestURL = contextURL + "/userManager";
        var requestData = {
              "type": "deleteUser",
              "name": id
            };
        var successHandler = function(data) {
              console.log("Output");
              if (data.error) {
                alert(error);
                return;
              }

              var replaceURL = requestURL;
              window.location.replace(replaceURL);
            };
            $.get(requestURL, requestData, successHandler, "json");
    }else{
        return;
    }
}