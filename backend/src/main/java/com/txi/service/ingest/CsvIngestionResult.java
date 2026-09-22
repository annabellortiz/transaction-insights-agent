package com.txi.service.ingest;

import java.util.ArrayList;
import java.util.List;

public class CsvIngestionResult {
    private int totalRows;
    private int insertedRows;
    private List<String> errors = new ArrayList<>();

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getInsertedRows() { return insertedRows; }
    public void setInsertedRows(int insertedRows) { this.insertedRows = insertedRows; }
    public List<String> getErrors() { return errors; }
    public void addError(String e) { this.errors.add(e); }
}
