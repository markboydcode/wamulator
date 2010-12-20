package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class StringWriterHandler extends Handler {
    private StringWriter sw = new StringWriter();
    private PrintWriter pw = new PrintWriter(sw);
    private String name;
    private Filter filter;
    
    public void clearBuffer() {
        sw = new StringWriter();
        pw = new PrintWriter(sw);
    }
    public String getLogOutput() {
        pw.flush();
        return sw.toString();
    }
    public void addFilter(Filter filter) {
        this.filter = filter;
    }

    public void clearFilters() {
        this.filter = null;
    }

    public void close() {
    }

    public void publish(LogRecord le) {
        pw.print(le.getMessage());
    }

    public void flush() {
    	pw.flush();
    }
    
    public Filter getFilter() {
        return filter;
    }

    public String getName() {
        return this.getClass().getName();
    }

    public boolean requiresLayout() {
        return false;
    }

    public void setName(String name) {
        this.name = name;
    }
}
