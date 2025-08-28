//package org.batfish.diagnosis.localization;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import org.batfish.diagnosis.common.ConfigurationLine;
//import org.batfish.diagnosis.repair.Repairer;
//
//public abstract class Localizer {
//    // 把错误行放到这里存储，到时候调用localizer计算错误行就可以返回localizer自身 或者 其他变量，用于后续的自动修复
//    protected List<ConfigurationLine> errorLines = new ArrayList<>();
//
//    // key是行号，String是那一行的配置命令
//    abstract List<ConfigurationLine> genErrorConfigLines();
//
//    /**
//     * @return Map<Integer, String> return the errorLines
//     */
//    public List<ConfigurationLine> getErrorLines() {
//        return errorLines;
//    }
//
//    public void addErrorLine(Integer lineNum, String line) {
//        errorLines.add(new ConfigurationLine(lineNum, line));
//    }
//
//    public void addErrorLines(Map<Integer, String> lines) {
//        lines.forEach((lineNum, line) -> {
//            errorLines.add(new ConfigurationLine(lineNum, line));
//        });
//    }
//
//    public void addErrorLines(List<ConfigurationLine> lines) {
//        errorLines.addAll(lines);
//    }
//
//    public void addErrorLine(ConfigurationLine line) {
//        errorLines.add(line);
//    }
//
//    /*
//     * 在loclize之后再修复，已经知道了localize的具体行
//     */
//    public abstract Repairer genRepairer();
//
//}
