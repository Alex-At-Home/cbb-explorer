// Add this to Code.gs

//TODO:  give more details about the spreadsheet

//("Team Stats" table is defined by menEfficiencyStatsMergeConfig)

function doGet(e) {
  uploadEfficiencyToElasticsearch();
  return HtmlService.createHtmlOutputFromFile("WebApp"); //(this is just the default HTML created by Google script engine)
}

function uploadEfficiencyToElasticsearch() {
  var ss = SpreadsheetApp.getActive();
  var sheet = ss.getSheetByName("TEST");

  var esMeta = ManagementService_.getEsMeta();
  // sheet.getRange(1,1).setValue(esMeta.username)
  // sheet.getRange(2,1).setValue(esMeta.password)

  var headers = {
    Authorization:
      "Basic " +
      Utilities.base64Encode(esMeta.username + ":" + esMeta.password),
    "Content-Type": "application/json",
  };

  // Refresh the 3P shooting LUT from the query "Team Stats"

  var tableName = "Team Stats";
  var tableConfig = ManagementService_.listSavedObjects()[tableName];
  var queryMeta = ElasticsearchService_.getElasticsearchMetadata(
    tableName,
    tableConfig
  );

  var aggregationQuery =
    ElasticsearchService_.buildAggregationQuery(tableConfig);
  delete aggregationQuery._source;

  var refreshTableUrl =
    esMeta.url + "/" + tableConfig.aggregation_table.index_pattern + "/_search";
  var refreshTableOptions = {
    method: "post",
    headers: headers,
    payload: JSON.stringify(aggregationQuery),
  };
  var refreshTableResponse = UrlFetchApp.fetch(
    refreshTableUrl,
    refreshTableOptions
  );

  // sheet.getRange(1,2).setValue(JSON.stringify(queryMeta).substring(0, 2048))
  // sheet.getRange(2,2).setValue(tableConfig.aggregation_table.index_pattern)
  // sheet.getRange(3,2).setValue(JSON.stringify(aggregationQuery).substring(0, 2048))
  // sheet.getRange(4,2).setValue(refreshTableResponse.getContentText().substring(0, 2048))

  ElasticsearchService_.handleAggregationResponse(
    tableName,
    tableConfig,
    queryMeta,
    { response: JSON.parse(refreshTableResponse.getContentText()) },
    aggregationQuery
  );

  // Build KenPom entries

  var toUpload = LookupService_.getJsonLookup("KenPomRange");
  var toUploadJson = [];
  for (var team in toUpload) {
    var teamStats = toUpload[team];
    if (teamStats) {
      teamStats["team_season.team"] = team;
      toUploadJson.push(JSON.stringify({ create: { _index: "kenpom_all" } }));
      toUploadJson.push(JSON.stringify(teamStats));
    }
  }
  // sheet.getRange(4,1).setValue(toUploadJson.length)
  // sheet.getRange(5,1).setValue(JSON.stringify(toUploadJson).substring(0, 256))

  if (toUploadJson.length > 100) {
    //(arbitrary criteria for "lookup succeeded", normally failure is 0 or 1)

    // Then Delete existing entries

    var bulkDeleteUrl = esMeta.url + "/kenpom_all/_delete_by_query";
    var bulkDeleteOptions = {
      method: "post",
      headers: headers,
      payload: JSON.stringify({
        query: {
          term: {
            "team_season.year": "2023",
          },
        },
      }),
    };
    var bulkDeleteResponse = UrlFetchApp.fetch(
      bulkDeleteUrl,
      bulkDeleteOptions
    );
    // sheet.getRange(3,1).setValue(bulkDeleteResponse.getContentText())

    // Now bulk upload new entries from data range "KenPomRange":

    var bulkUploadUrl =
      esMeta.url + "/_bulk?pipeline=men_efficiency_id_pipeline";
    var bulkUploadOptions = {
      method: "post",
      headers: headers,
      payload: toUploadJson.join("\n") + "\n",
    };
    var bulkUploadResponse = UrlFetchApp.fetch(
      bulkUploadUrl,
      bulkUploadOptions
    );
    // sheet.getRange(6,1).setValue(bulkUploadResponse.getContentText().substring(0, 1024))
  }
}
