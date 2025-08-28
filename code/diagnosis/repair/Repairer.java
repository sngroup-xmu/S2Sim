//package org.batfish.diagnosis.repair;
//
//
//import org.batfish.diagnosis.common.ConfigurationLine;
//
//import java.util.*;
//
///**
// * 修理者
// * * 输出需要del和add的行
// * * del和add同一行表示此行需修改参数
// * * add行的行号表示在当前错误配置上【此行后】加上对应配置
// * 【这里add的行需要提前处理，考虑每种修复的配置的【缩进】和【结束符#】添加】
// *
// * @author yrl
// * @date 2023/08/17
// */
//
//public abstract class Repairer {
//    /*
//
//     */
//    private List<ConfigurationLine> deletedLines = new ArrayList<>();
//    private List<ConfigurationLine> addedLines = new ArrayList<>();
//
//    public void addDeletedLine(Integer lineNum, String line) {
//        deletedLines.add(new ConfigurationLine(lineNum, line));
//    }
//
//    public void addDeletedLine(ConfigurationLine configurationLine) {
//        deletedLines.add(configurationLine);
//    }
//
//
//    /**
//     * 修改某行，自动生成 del行 和 add行
//     *
//     * @param line           行
//     * @param changedWordMap 需要变动的词语映射
//     */
//
//    public void changeLine(ConfigurationLine line, Map<String, String> changedWordMap) {
//        String[] lineWords = line.getLine().split(" ");
//        // 替换changedWordMap
//        for (int i = 0; i < lineWords.length; i++) {
//            String oldWord = lineWords[i];
//            if (changedWordMap.containsKey(oldWord)) {
//                lineWords[i] = changedWordMap.get(oldWord);
//            }
//        }
//        // 删除原配置行
//        addDeletedLine(line.getLineNumber(), line.getLine());
//        // 生成新增配置行
//        StringBuilder newLine = new StringBuilder();
//        for (String word: lineWords) {
//            newLine.append(word).append(" ");
//        }
//        // 增加新的配置行
//        addAddedLine(line.getLineNumber(), newLine.toString());
//    }
//    public void addDeletedLines(List<ConfigurationLine> lines) {
//        deletedLines.addAll(lines);
//    }
//
//    public void addAddedLine(Integer lineNum, String line) {
//        addedLines.add(new ConfigurationLine(lineNum, line));
//    }
//
//    public List<ConfigurationLine> getDeletedLines() {
//        return deletedLines;
//    }
//
//    // Map<Integer, String> getModifiedLines()
//    public List<ConfigurationLine> getAddedLines() {
//        return addedLines;
//    }
//
//    public abstract void genRepair();
//}
