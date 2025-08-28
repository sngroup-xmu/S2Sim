package org.batfish.diagnosis.common;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationLine {
    private int lineNumber;
    private String line;

    public ConfigurationLine(int num, String line) {
        this.lineNumber = num;
        this.line = line;
    }

    /**
     * @return int return the lineNumber
     */
    public int getLineNumber() {
        return lineNumber;
    }


    /**
     * @return String return the line
     */
    public String getLine() {
        return line;
    }

    /**
     * @param line the line to set
     */
    public void setLine(String line) {
        this.line = line;
    }

    public static Map<Integer, String> transToMap(List<ConfigurationLine> configLines) {
        Map<Integer, String> rawLines = new HashMap<>();
        configLines.forEach(l->rawLines.put(l.getLineNumber(), l.getLine()));
        return rawLines;
    }

}
