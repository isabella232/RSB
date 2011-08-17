/*
 *   R Service Bus
 *   
 *   Copyright (c) Copyright of OpenAnalytics BVBA, 2010-2011
 *
 *   ===========================================================================
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   @author rsb.development@openanalytics.eu
 */

var remoteDataRoot;
var urlParams = {};
(function () {
    var e,
        a = /\+/g,  // Regex for replacing addition symbol with a space
        r = /([^&;=]+)=?([^&;]*)/g,
        d = function (s) { return decodeURIComponent(s.replace(a, " ")); },
        q = window.location.search.substring(1);

    while (e = r.exec(q))
       urlParams[d(e[1])] = d(e[2]);
})();

function xmlTimeStampToDate(xmlDate)
{
    var dt = new Date();
    var dtS = xmlDate.slice(xmlDate.indexOf('T')+1, xmlDate.indexOf('.'))
    var TimeArray = dtS.split(":");
    dt.setUTCHours(TimeArray[0],TimeArray[1],TimeArray[2]);
    dtS = xmlDate.slice(0, xmlDate.indexOf('T'))
    TimeArray = dtS.split("-");
    dt.setUTCFullYear(TimeArray[0],TimeArray[1]-1,TimeArray[2]);
    return dt;
}

function loadApplicationResults(applicationName, highlightJobId) {
  $.ajax({ url: "api/rest/results/" + applicationName,
           success: function(responseXML) {
             
    var tableBody = $("#resultsTableBody");
    tableBody.empty();
    
    $(responseXML).find('result').each(function(){
     var jobId = $(this).attr("jobId");
     var resultUri = $(this).attr("selfUri");
     var dataUri = $(this).attr("dataUri");
     var timestamp = $(this).attr("resultTime");
     
     tableBody.append("<tr id='res-" + jobId + "' class='" + (jobId === highlightJobId ? "highlighted" : "") + "'><td>"
                      + $(this).attr("applicationName")
                      + "</td><td>"
                      + xmlTimeStampToDate(timestamp)
                      + "</td><td>"
                      + jobId
                      + "</td><td>"
                      + "<img src='images/"
                      + (($(this).attr("success")==='true')?"success.gif":"failure.png")
                      + "' title='Status' border='0' />&nbsp;"
                      + $(this).attr("type")
                      + "</td><td>"
                      + "<a href='" + dataUri + "' target='_blank' id='getres-"+jobId+"'><img src='images/download.gif' title='Download' border='0' /></a>"
                      + "&nbsp;&nbsp;"
                      + "<a href='#' id='delres-"+jobId+"'><img src='images/delete.png' title='Delete' border='0' /></a>"
                      +"</td></tr>");
    
     $("#delres-"+jobId).click(function() {
       var shouldDelete = confirm("Are you sure you want to delete the result of job: " + jobId + "?");
       if (shouldDelete) {
         $.ajax({
           type       : 'DELETE',
           url        : resultUri,
           jobId      : jobId,
                     
           success: function(data, textStatus, xhr) {
             $("#res-"+jobId).hide();
           },
         
           error: function(jqXHR, textStatus, errorThrown) {
             // DELETE returns No Content, which may yield a parse issue -> if this happens, reload all the results
             loadApplicationResults(applicationName);
           }
         });
       }
     });
    });
    
    $('#resultsPanel').show(250);
  }});
}

var remoteDataNode = function(remoteDirectory) {
  var name;
  var attr = {};
  var children = [];
  
  if (remoteDirectory) {
    name = remoteDirectory.name;
    attr = {path: remoteDirectory.path, uri: remoteDirectory.uri};
  }
  
  var o = $({});
  
  o.extend({
    openNode: function(cb) {
      log.debug("Open->"+this.getName());
      
      if (this.getAttr()) {
        $.ajax({
          type       : 'GET',
          url        : this.getAttr().uri,
          dataType   : 'json',
                    
          success: function(data) {
            // add child directories
            var childDirectories = data.directory.directory;
            var newChildren = [];
            for(var key in childDirectories) {
              var childDirectory = childDirectories[key];
              var newChild = remoteDataNode(childDirectory); 
              newChildren.push(newChild);
              o.addChild(newChild);
            }
            // TODO add child files
            remoteDataRoot.trigger("addChildren.jstree",[o,newChildren]);
          }
        });
        
        remoteDataRoot.trigger("addChildren.jstree",[this,children]);
        if (cb && typeof(cb) === "function") {
          cb();
        }
      }
    },
    closeNode: function() {
      // NOOP
    },
    hasChildren : function() {
      // FIXME add a isEmpty property on remote dir REST resource
      return this.getAttr().path == '/' || children.length > 0;
    },
    addChild : function(child) {
      children.push(child);
      return this;
    },
    removeChild : function(i) {
      var child;
      if (i>=children.length) {
        child = children.pop();
      } else {
        child = children[i];
        delete children[i];
      }
      remoteDataRoot.trigger("removeChildren.jstree",[this,child]);               
      return this;
    },
    getAttr : function() {
      return attr;
    },
    getName : function() {
      return name;
    },
    getProps : function() {
      return attr;
    }
  });
  
  return o;
};

function initializeRemoteDataTree(data) {
  $('#remoteDataTree').jstree({
    themes : {
      theme : "classic",
      url : "./css/jstree-classic/style.css",
      dots : true,
      icons : true
    },
    model_data: {
      data: function(){return(remoteDataRoot);}, 
      progressive_render: true, 
      progressive_unload: true,
      type_attr: "mytype",
      id_prefix: "myid"
    },
    ui : {
      select_limit : 1
    },
    plugins : [ 'themes', 'ui', 'model_data' ]
  });
  
  remoteDataRoot = remoteDataNode({name:'Remote Data',uri:'api/rest/data',path:'/'});
  remoteDataRoot = remoteDataNode().addChild(remoteDataRoot);
  log.info('Remote Data Browser Initialized');
}

function showRemoteFileSelector(selectionType, targetInput) {
  if ((selectionType != 'file') && (selectionType != 'directory')) {
    alert("Unsupported section type: " + selectionType);
    return false;
  }
  
  $('button:has(span:contains(Select))').attr('disabled', 'disabled');
  
  $('#remoteDataSelectorType').text(selectionType);
  
  // TODO support selectionType=file|directory, targetInput
  $('#remoteDataSelector').dialog('open');
  return false;
}

$(document).ready(function() {
  // Dialogs
  $('#remoteDataSelector').dialog({
    autoOpen: false,
    width: 600,
    buttons: {
      "Select": function() { 
        $(this).dialog("close"); 
      }, 
      "Cancel": function() { 
        $(this).dialog("close"); 
      } 
    }
  });
  
  // Panels
  $('#requiredParamsPanel').panel({
      collapsible:false,
      collapsed:true
  });
  $('#optionalParamsPanel').panel({
      collapsed:true
  });
  
  $('#runningJobsPanel').panel({
      collapsed:false
  });
  $('#runningJobsPanel').hide();
  
  $('#resultsPanel').panel({
      collapsed:false
  });
  $('#resultsPanel').hide();
  
  // Application Results Loading
  $('#applicationResultsButton').click(function() {
      var applicationName = $('#applicationName').val();
      
      if (applicationName.length == 0) {
        alert("You must provide an application name for retrieving its results!");
      } else {
        loadApplicationResults(applicationName);
      }
    });
 
  // Upload Form and Ajax Job Progress Monitor
  $("#jobUploadForm").validate();
  
  $('#jobUploadForm').ajaxForm({
      beforeSubmit: function(a,f,o) {
        o.dataType = 'xml';
      },
      
      success: function(responseXML, textStatus, xhr) {
          // reset the file selector only
          $('#jobFileSelector').attr({ value: '' });
          $('#jobFileSelector').MultiFile('reset');
          
          var response = $('jobToken', responseXML)
          var jobId = response.attr('jobId');

          if (!jobId) {
            // something went wrong: display the server response as-is
            alert($(responseXML).text());
            return;
          }
          
          var appName = response.attr('applicationName');
          var resultUri = response.attr('resultUri');
          
          $('#runningJobsPanel').show(250);
           
          $('#runningJobsTableBody').append("<tr id='run-" + jobId + "'><td>"
              + appName
              + "</td><td>"
              + Date.today().setTimeToNow()
              + "</td><td>"
              + jobId
              +"</td></tr>");

          $.ajax({
              type       : 'GET',
              url        : resultUri,
              appName    : appName,
              jobId      : jobId,
                        
              success: function(data, textStatus, xhr) {
                $('#run-' + jobId).hide();
                loadApplicationResults(appName, jobId);
              },

              error: function(xhr, textStatus, errorThrown) {
                // try again after 5 secs
                var reAjax = this;
                $.doTimeout(5000, function() { $.ajax(reAjax) });
              }
          });
      },
  });
  
  // Load URL parameters into specific field values
  $('#applicationName').val(urlParams['appName']);
  
  // Setup page appearance
  document.title = RSB_FORM_TITLE;
  $('#formTitle').html(RSB_FORM_TITLE);

  // Remote data browser
  $.ajax({
    type       : 'GET',
    url        : 'api/rest/data',
    dataType   : 'json',
              
    success: function(data, textStatus, xhr) {
      initializeRemoteDataTree(data);
    }
  });
  
  log.info('RSB UI Ready');
});     
