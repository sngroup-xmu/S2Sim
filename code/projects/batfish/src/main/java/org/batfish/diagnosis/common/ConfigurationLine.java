package org.batfish.diagnosis.common;


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationLine {
    private int _lineNumber;
    private String _line;

    public ConfigurationLine(int num, String line) {
        this._lineNumber = num;
        this._line = line;
    }

    /**
     * @return int return the lineNumber
     */
    public int getLineNumber() {
        return _lineNumber;
    }


    /**
     * @return String return the line
     */
    public String getLine() {
        return _line;
    }

    /**
     * @param line the line to set
     */
    public void setLine(String line) {
        this._line = line;
    }

    public void setLineNumber(int lineNumber) {
        this._lineNumber = lineNumber;
    }

    public static Map<Integer, String> transToMap(List<ConfigurationLine> configLines) {
        Map<Integer, String> rawLines = new LinkedHashMap<>();
        configLines.forEach(l->rawLines.put(l.getLineNumber(), l.getLine()));
        return rawLines;
    }

}
