package org.lds.sso.appwrap.conditions.evaluator.syntax;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class StringWriterAppender implements Appender {
    private StringWriter sw = new StringWriter();
    private PrintWriter pw = new PrintWriter(sw);
    private String name;
    private Layout layout;
    private ErrorHandler eh;
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

    public void doAppend(LoggingEvent le) {
        pw.print(le.getMessage());
    }

    public ErrorHandler getErrorHandler() {
        return eh;
    }

    public Filter getFilter() {
        return filter;
    }

    public Layout getLayout() {
        return layout;
    }

    public String getName() {
        return this.getClass().getName();
    }

    public boolean requiresLayout() {
        return false;
    }

    public void setErrorHandler(ErrorHandler eh) {
        this.eh = eh;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public void setName(String name) {
        this.name = name;
    }
}
