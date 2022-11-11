// Add this to Code.gs

//TODO:  give more details about the spreadsheet

function uploadEfficiencyToElasticsearch() {
  // var ss = SpreadsheetApp.getActive()
  // var sheet = ss.getSheetByName("TEST")

  var esMeta = ManagementService_.getEsMeta()
  // sheet.getRange(1,1).setValue(esMeta.username)
  // sheet.getRange(2,1).setValue(esMeta.password)

  var headers = {
      "Authorization": "Basic " + Utilities.base64Encode(esMeta.username + ":" + esMeta.password),
      "Content-Type": "application/json"
  }

  // Delete existing entries

  var bulkDeleteUrl = esMeta.url + "/kenpom_all/_delete_by_query"
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
  // sheet.getRange(3,1).setValue(bulkDeleteResponse.getContentText())

  // Now bulk upload new entries:

  var toUpload = LookupService_.getJsonLookup("KenPomRange")
  var toUploadJson = []
  for (var team in toUpload) {
    var teamStats = toUpload[team]
    if (teamStats) {
      teamStats["team_season.team"] = team
      toUploadJson.push(JSON.stringify({ "create" : { "_index" : "kenpom_all" } }))
      toUploadJson.push(JSON.stringify(teamStats))
    }
  }
  // sheet.getRange(4,1).setValue(toUploadJson.length)
  // sheet.getRange(5,1).setValue(JSON.stringify(toUploadJson).substring(0, 256))

  var bulkUploadUrl = esMeta.url + "/_bulk?pipeline=men_efficiency_id_pipeline"
  var bulkUploadOptions = {
    "method": "post",
    "headers": headers,
    "payload": toUploadJson.join("\n") + "\n"
  }
  var bulkUploadResponse = UrlFetchApp.fetch(bulkUploadUrl, bulkUploadOptions)
  // sheet.getRange(6,1).setValue(bulkUploadResponse.getContentText().substring(0, 1024))
}


