package com.tracker.bigquery;

import java.math.BigInteger;
import java.util.List;


public class QueryResult {

	private List<List<Object>> data;
	private List<String> schema;
	private int count;
	private BigInteger totalRows;
	private String pageToken;

	public QueryResult(List<List<Object>> data, List<String> schema, BigInteger totalRows, String pageToken) {
		this.data = data;
		this.schema = schema;
		this.count = data.size();
		this.totalRows = totalRows;
		this.pageToken = pageToken;
	}

	public List<List<Object>> getData() {
		return data;
	}

	public Integer getCount() {
		return count;
	}

	public BigInteger getTotalRows() {
		return totalRows;
	}

	public String getPageToken() {
		return pageToken;
	}

	public List<String> getSchema() {
		return schema;
	}
}
