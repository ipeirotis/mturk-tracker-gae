package com.tracker.bigquery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.model.ErrorProto;
import com.google.api.services.bigquery.model.QueryRequest;
import com.google.api.services.bigquery.model.QueryResponse;
import com.google.api.services.bigquery.model.Table;
import com.google.api.services.bigquery.model.TableCell;
import com.google.api.services.bigquery.model.TableDataInsertAllRequest;
import com.google.api.services.bigquery.model.TableDataInsertAllRequest.Rows;
import com.google.api.services.bigquery.model.TableDataInsertAllResponse;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableRow;
import com.google.api.services.bigquery.model.TableSchema;

public class BigQueryService {

    @SuppressWarnings("unused")
    private static final Logger logger = Logger.getLogger(BigQueryService.class.getName());
    
    private final Bigquery bigquery;
    private final String projectId;
    private final String datasetId;

    public BigQueryService(Bigquery bigquery, String projectId, String datasetId) {
        this.bigquery = bigquery;
        this.projectId = projectId;
        this.datasetId = datasetId;
    }

    public QueryResult executeQuery(String query) {
        try {
            QueryRequest queryRequest = new QueryRequest().setQuery(query);
            QueryResponse response = bigquery.jobs().query(projectId, queryRequest).execute();
            return getQueryResult(response);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to get query result", e.getMessage()), e);
        }
    }
    public void createTable(String tableId, Map<String, String> fields) {
        try {
            Table table = buildTable(tableId, fields);
            bigquery.tables().insert(projectId, datasetId, table).execute();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error creating table %s: %s", tableId, e.getMessage()), e);
        }
    }


    public Table getTable(String tableId) {
        try {
            return bigquery.tables().get(projectId, datasetId, tableId).execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                return null;
            }
            throw new RuntimeException(String.format("Error getting table %s: %s", tableId, e.getMessage()), e);
        }catch (IOException e) {
            throw new RuntimeException(String.format("Error getting table %s: %s", tableId, e.getMessage()), e);
        }
    }
    
    public void insert(String tableId, List<Map<String, Object>> data) {
        try {
            List<Rows> rowsList = new ArrayList<Rows>();
            for(Map<String, Object> dataItem : data){
                TableDataInsertAllRequest.Rows rows = new TableDataInsertAllRequest.Rows();
                rows.setJson(buildTableRow(dataItem));
                rowsList.add(rows);
            }

            TableDataInsertAllRequest request = new TableDataInsertAllRequest().setRows(rowsList);
            TableDataInsertAllResponse response = bigquery.tabledata().insertAll(projectId, datasetId, tableId, request).execute();

            if (response != null) {
                List<TableDataInsertAllResponse.InsertErrors> insertErrors = response.getInsertErrors();
                if (insertErrors != null && !insertErrors.isEmpty()) {

                    for (TableDataInsertAllResponse.InsertErrors insertError : insertErrors) {

                        List<ErrorProto> errorProtos = insertError.getErrors();
                        if (errorProtos != null && !errorProtos.isEmpty()) {

                            ErrorProto errorProto = errorProtos.get(0);

                            String errMsg = String.format("Unable to insert data for tableId: %s", tableId);
                            if (StringUtils.isNotBlank(errorProto.getReason())) {
                                errMsg += String.format(". Reason: %s", errorProto.getReason());
                            }
                            if (StringUtils.isNotBlank(errorProto.getMessage())) {
                                errMsg += String.format(". Message: %s", errorProto.getMessage());
                            }
                            throw new RuntimeException(errMsg);
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(String.format("Error inserting data into Bigquery table %s", tableId), e);
        }
    }

    private Table buildTable(String tableId, Map<String, String> fields) {
        TableSchema tableSchema = buildTableSchema(fields);

        Table table = new Table();
        table.setId(tableId);
        table.setSchema(tableSchema);
        table.setKind("bigquery#table");
        TableReference tableReference = new TableReference();
        tableReference.setDatasetId(datasetId);
        tableReference.setProjectId(projectId);
        tableReference.setTableId(tableId);
        table.setTableReference(tableReference);
        return table;
    }

    private TableSchema buildTableSchema(Map<String, String> fields) {
        List<TableFieldSchema> tableFieldSchemas = new ArrayList<>();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            TableFieldSchema tableFieldSchema = new TableFieldSchema();
            tableFieldSchema.setName(entry.getKey());
            tableFieldSchema.setType(entry.getValue());
            tableFieldSchemas.add(tableFieldSchema);
        }

        TableSchema tableSchema = new TableSchema();
        tableSchema.setFields(tableFieldSchemas);
        return tableSchema;
    }

    private TableRow buildTableRow(Map<String, Object> data) {
        TableRow row = new TableRow();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            row.set(entry.getKey(), entry.getValue());
        }
        return row;
    }

    private List<List<Object>> getResultData(List<TableRow> rows) {
        List<List<Object>> results = new ArrayList<List<Object>>();
        if (rows != null) {
            for (TableRow tableRow : rows) {
                List<Object> row = new ArrayList<Object>();
                List<TableCell> fields = tableRow.getF();
                for (TableCell cell : fields) {
                    row.add(cell.getV());
                }
                results.add(row);
            }
        }
        return results;
    }

    private QueryResult getQueryResult(QueryResponse response) throws IOException {
        List<String> schema = new ArrayList<String>();
        for (TableFieldSchema tableFieldSchema : response.getSchema().getFields()) {
            schema.add(tableFieldSchema.getName());
        }

        List<List<Object>> data = getResultData(response.getRows());

        return new QueryResult(data, schema, response.getTotalRows(), response.getPageToken());
    }

} 
