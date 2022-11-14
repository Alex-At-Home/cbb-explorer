// Add this to Code.gs

//TODO:  give more details about the spreadsheet


function doPost(request) {
   var contents = request.postData.contents
   uploadEfficiencyToElasticsearch(contents)
   return HtmlService.createHtmlOutputFromFile("WebApp") //(this is just the default HTML created by Google script engine)
 }
 
 function uploadEfficiencyToElasticsearch(contents) {
 
   // Phase 1: update the master spreadsheet
 
   if (contents) {
     var lines = contents.split("\n")
 
     var ss = SpreadsheetApp.getActive()
     var sheet = ss.getSheetByName("Massey")
 
     for (var line = 0; line < lines.length; ++line) {
       var fields = lines[line].split(",")
       for (var field = 0; field < fields.length; ++field) {
           sheet.getRange(1 + line,2 + field).setValue(fields[field])
       }
     }
   }
 
   // Phase 2: build export object from named range "MasseyExport"
 
   var toUpload = LookupService_.getJsonLookup("MasseyExport")
   var toUploadJson = []
   for (var team in toUpload) {
     var teamStats = toUpload[team]
     if (teamStats) {
       teamStats["team_season.team"] = team
       teamStats["team_season.year"] = "2023"
       toUploadJson.push(JSON.stringify({ "create" : { "_index" : "massey_all" } }))
       toUploadJson.push(JSON.stringify(teamStats))
     }
   }
 
   // Phase 3: upload to the database
 
   var esMeta = ManagementService_.getEsMeta()
   var headers = {
       "Authorization": "Basic " + Utilities.base64Encode(esMeta.username + ":" + esMeta.password),
       "Content-Type": "application/json"
   }
 
   var bulkDeleteUrl = esMeta.url + "/massey_all/_delete_by_query"
   var bulkDeleteOptions = {
     "method": "post",
     "headers": headers,
     "payload": JSON.stringify({
       "query" : {
           "term" : {
             "team_season.year": "2023"
           }
       }
     })
   }
   var bulkDeleteResponse = UrlFetchApp.fetch(bulkDeleteUrl, bulkDeleteOptions)
 
   var bulkUploadUrl = esMeta.url + "/_bulk?pipeline=men_efficiency_id_pipeline"
   var bulkUploadOptions = {
     "method": "post",
     "headers": headers,
     "payload": toUploadJson.join("\n") + "\n"
   }
   var bulkUploadResponse = UrlFetchApp.fetch(bulkUploadUrl, bulkUploadOptions)
 
 }
 