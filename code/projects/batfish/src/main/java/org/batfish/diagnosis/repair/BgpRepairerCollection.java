package org.batfish.diagnosis.repair;

import com.google.common.io.Files;

import java.io.*;
import java.util.*;

import org.batfish.diagnosis.localization.Localizer;
import org.batfish.diagnosis.localization.RouteForbiddenLocalizer;

public class BgpRepairerCollection {
   private Map<String, Set<Repairer>> repairMap = new HashMap<>();
   // 输出的时候去重
   Map<String, Map<Integer, Set<String>>> delLineMap = new HashMap<>();
   Map<String, Map<Integer, Set<String>>> addLineMap = new HashMap<>();

   public void addRepairer(String node, Repairer repairer) {
       if (!repairMap.containsKey(node)) {
           repairMap.put(node, new LinkedHashSet<Repairer>());
       }
       repairMap.get(node).add(repairer);
   }

   public void merge(BgpRepairerCollection collection) {

   }

   public void mergeRepairerSet(String node, Set<Repairer> repairerSet) {
       if (!repairMap.containsKey(node)) {
           repairMap.put(node, repairerSet);
       }
       repairMap.get(node).addAll(repairerSet);
   }

   private void addRepairLines(Map<String, Map<Integer, Set<String>>> targetMap, String node, Integer lineNum,
           Set<String> lines) {
       if (!targetMap.containsKey(node)) {
           targetMap.put(node, new HashMap<>());
       }
       Map<Integer, Set<String>> linesOfNode = targetMap.get(node);
       if (!linesOfNode.containsKey(lineNum)) {
           linesOfNode.put(lineNum, new LinkedHashSet<>());
       }
       linesOfNode.get(lineNum).addAll(lines);
   }

   private void addRepairLine(Map<String, Map<Integer, Set<String>>> targetMap, String node, Integer lineNum,
           String line) {
       if (!targetMap.containsKey(node)) {
           targetMap.put(node, new HashMap<>());
       }
       Map<Integer, Set<String>> linesOfNode = targetMap.get(node);
       if (!linesOfNode.containsKey(lineNum)) {
           linesOfNode.put(lineNum, new LinkedHashSet<>());
       }
       linesOfNode.get(lineNum).add(line);
   }

   /*
    * 输出每个case的各设备的改动行
    */
   public void printConfigChange(String newFilePath) {
       String addNotification = " 在此行后加入下列配置";
       String delNotification = " 删除此行配置";
       Set<String> repairedNodes = new HashSet<>();
       repairedNodes.addAll(addLineMap.keySet());
       repairedNodes.addAll(delLineMap.keySet());

       // 新写入的配置用streamWriter
       File newFile = new File(newFilePath);
       FileOutputStream newFileOutputStream = null;
       OutputStreamWriter newOutputStreamWriter = null;

       // 创建新修复文件，如果原来存在就del重建
       if (newFile.exists()) {
           newFile.delete();
       }
       newFile.getParentFile().mkdirs();
       try {
           newFile.createNewFile();
           newFileOutputStream = new FileOutputStream(newFile);
           newOutputStreamWriter = new OutputStreamWriter(newFileOutputStream, "utf-8");
           // 逐个加入修复节点需要新增和删除的行
           for (String node: repairedNodes) {
               newOutputStreamWriter.write("----------------------------" + node + "----------------------------");
               newOutputStreamWriter.write("\r\n");
               if (addLineMap.containsKey(node)) {
                   // 写入新增行
                   Map<Integer, Set<String>> addedLinesForThisNode = addLineMap.get(node);
                   for (int lineNum: addedLinesForThisNode.keySet()) {
                       String startString = "(" + lineNum + " +)" + addNotification;
                       newOutputStreamWriter.write(startString);
                       newOutputStreamWriter.write("\r\n");
                       for (String line: addedLinesForThisNode.get(lineNum)) {
                           newOutputStreamWriter.write(line);
                           newOutputStreamWriter.write("\r\n");
                       }
                   }
               }
               if (delLineMap.containsKey(node)) {
                   // 写入新增行
                   Map<Integer, Set<String>> delLinesForThisNode = delLineMap.get(node);
                   for (int lineNum: delLinesForThisNode.keySet()) {
                       String startString = "(" + lineNum + " -)" + delNotification;
                       newOutputStreamWriter.write(startString);
                       newOutputStreamWriter.write("\r\n");
                       for (String line: delLinesForThisNode.get(lineNum)) {
                           newOutputStreamWriter.write(line);
                           newOutputStreamWriter.write("\r\n");
                       }
                   }
               }
               newOutputStreamWriter.write("------------------------------------------------------------");
               newOutputStreamWriter.write("\r\n");
               newOutputStreamWriter.write("\r\n");
           }
           newOutputStreamWriter.close();
           newFileOutputStream.close();
       } catch (IOException e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
       }



   }

   private void printLineMap(boolean flag) {
       // flag 为true表示输出新增行
       Map<String, Map<Integer, Set<String>>> targetMap;
       if (flag) {
           targetMap = addLineMap;
       } else {
           targetMap = delLineMap;
       }
       for (String node : targetMap.keySet()) {
           System.out.println("*************************" + node + "***********************");
           Map<Integer, Set<String>> lineMap = targetMap.get(node);
           lineMap.forEach((lineNum, lines) -> {
               String word = String.valueOf(lineNum);
               if (flag) {
                   word = word.concat(" +");
               } else {
                   word = word.concat(" -");
               }
               System.out.println(word);
               lines.forEach(l -> System.out.println("  " + l));
           });
       }
   }

   /**
    * 最后处理各个节点的 repairer 的 del和 add行，因为有的错误涉及两个节点（如peer），需要把它们放到各自的 repairedLine里
    */

   public void finish() {

       for (String node : repairMap.keySet()) {
           for (Repairer repairer : repairMap.get(node)) {
               if (repairer == null) {
                   // 还没有implement的repairer
                   continue;
               }
               if (repairer instanceof PeerRepairer) {
                   if (((PeerRepairer) repairer).getPeerAddedLines() != null) {
                       ((PeerRepairer) repairer).getPeerAddedLines().forEach((lineNum, lines) -> {
                           addRepairLines(addLineMap, ((PeerRepairer) repairer).peerDevName, lineNum, lines);
                       });
                   }
                   if (((PeerRepairer) repairer).getPeerDeletedLines() != null) {
                       ((PeerRepairer) repairer).getPeerDeletedLines().forEach((lineNum, line) -> {
                           addRepairLine(delLineMap, ((PeerRepairer) repairer).peerDevName, lineNum, line);
                       });
                   }
               }
               repairer.getAddedLines().forEach(line -> {
                   addRepairLine(addLineMap, node, line.getLineNumber(), line.getLine());
               });
               repairer.getDeletedLines().forEach(line -> {
                   addRepairLine(delLineMap, node, line.getLineNumber(), line.getLine());
               });
           }
       }
       printLineMap(true);
       printLineMap(false);

       System.out.println(" ");
   }

   /**
    * 生成修复后配置时，两种情况：
    *  情况1：未修改节点的配置直接从源目录下复制过来，若已存在就不再复制（不然测试时可能反复复制）
    *  情况2：配置需更改的节点：在对应行进行增删操作
    *
    * @param oldRootPath 原来的路径
    * @param newRootPath 新的路径
    */
   public void genRepairedConfiguration(String oldRootPath, String newRootPath) {
       File oldConfigDir = new File(oldRootPath);
       assert oldConfigDir.isDirectory();

       // 情况1：把不更改的配置先复制到新路径下
       File[] oldFiles = oldConfigDir.listFiles();
       assert oldFiles != null;
       for (File oldFile : oldFiles) {
           String oldFileName = oldFile.getName();
           if (oldFileName.contains(".cfg")) {
               // 只考虑cfg结尾的是配置文件
               String[] names = oldFileName.split("\\.");
               String node = names[0];
               if (addLineMap.containsKey(node) || delLineMap.keySet().contains(node)) {
                   // 跳过待修复的配置文件
                   continue;
               }
               File newFile = new File(newRootPath + "/" + oldFile.getName());
               if (!newFile.exists()) {
                   try {
                       newFile.getParentFile().mkdirs();
                       newFile.createNewFile();
                       Files.copy(oldFile, newFile);
                   } catch (IOException e) {
                       // TODO Auto-generated catch block
                       e.printStackTrace();
                   }
               }
           }
       }

       // 情况2：重写需要修改的配置：增删Map的key值的并集
       Set<String> nodesNeedRepair = new HashSet<>(addLineMap.keySet());
       nodesNeedRepair.addAll(delLineMap.keySet());

       nodesNeedRepair.forEach(node -> writeToNewFileWithModification(node, oldRootPath + "/" + node + ".cfg",
               newRootPath + "/" + node + ".cfg"));

   }

   /*
    * 获取字符串（配置行）前面的空格数量，修复的行前面加上等量的空格数
    */
   private String getLineSpace(String line) {
       if (line==null) {
           return "";
       }
       char[] words = line.toCharArray();
       int spaceNum = 0;
       String space = "";
       for (char word : words) {
           if (word == ' ') {
               spaceNum += 1;
               space += " ";
           } else {
               break;
           }
       }
       return space;
   }

   /**
    * 已知当前节点需要增和删的配置，以及原始配置。
    *
    * @param node        设备节点
    * @param oldFilePath 旧文件路径
    * @param newFilePath 新文件路径
    */

   public void writeToNewFileWithModification(String node, String oldFilePath, String newFilePath) {

       // 旧配置用reader
       BufferedReader reader;
       // 新写入的配置用streamWriter
       File newFile = new File(newFilePath);
       FileOutputStream newFileOutputStream = null;
       OutputStreamWriter newOutputStreamWriter = null;
       // 需要删除&增加的行
       Map<Integer, Set<String>> delMap = delLineMap.get(node);
       Map<Integer, Set<String>> addMap = addLineMap.get(node);
       try {
           // 创建新修复文件，如果原来存在就del重建
           if (newFile.exists()) {
               newFile.delete();
           }
           newFile.getParentFile().mkdirs();
           newFile.createNewFile();
           newFileOutputStream = new FileOutputStream(newFile);
           newOutputStreamWriter = new OutputStreamWriter(newFileOutputStream, "utf-8");

           // 旧配置边读边处理add/del的行，逐行读取
           reader = new BufferedReader(new FileReader(oldFilePath));
           String line = reader.readLine().trim();
           int lineNum = 1;
           while (line != null) {
               if (delMap == null || !delMap.containsKey(lineNum)) {
                   // 不删除的行直接复制，删除的行直接跳过
                   newOutputStreamWriter.write(line);
                   newOutputStreamWriter.write("\r\n");
               }
               // 新增行前的空格应该和当前原配置里的下一行一致【因为是加在对应行号之后】
               line = reader.readLine();
               if (addMap != null && addMap.containsKey(lineNum)) {
                   for (String addLine : addMap.get(lineNum)) {
                       try {
                           newOutputStreamWriter.write(addLine);
                           newOutputStreamWriter.write("\r\n");
                       } catch (IOException e) {
                           // TODO Auto-generated catch block
                           e.printStackTrace();
                       }
                   }
               }
               // line = reader.readLine();
               lineNum += 1;
           }
           reader.close();
           newOutputStreamWriter.close();
           newFileOutputStream.close();
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

   /*
     * 如果之前有repairer使用过同样的policy，则把相应的routePolicy返回，新的routeForbiddenRepairer引用它
     */
    private RouteForbiddenRepairer getRouteForbiddenRepairerWithSamePolicyName(String name, Set<Repairer> repairSet) {
        for (Repairer repairer : repairSet) {
            if (repairer instanceof RouteForbiddenRepairer) {
                if (((RouteForbiddenRepairer) repairer).getPolicyName().equals(name)) {
                    return (RouteForbiddenRepairer) repairer;
                }
            }
        }
        return null;
    }

   public void transLocalizerToRepaier(Map<String, Set<Localizer>> localizerMap) {

        for (String node : localizerMap.keySet()) {
            if (!repairMap.containsKey(node)) {
                repairMap.put(node, new HashSet<>());
            }
            Set<Repairer> repairSet = repairMap.get(node);
            Set<Localizer> localizerSet = localizerMap.get(node);
            for (Localizer localizer : localizerSet) {
                // routeForbidden的修复，对于同一个policy的多个localizer/repairer需引用同一个routePolicy实例【不需要是对同一个neighbor的】
                // if (localizer instanceof RouteForbiddenLocalizer) {
                //     RouteForbiddenRepairer otherRepairerWithSamePolicyName = getRouteForbiddenRepairerWithSamePolicyName(
                //             ((RouteForbiddenLocalizer) localizer).getPolicyName(), repairSet);
                //     if (otherRepairerWithSamePolicyName != null) {
                //         RouteForbiddenRepairer repairer = new RouteForbiddenRepairer(
                //                 (RouteForbiddenLocalizer) localizer, otherRepairerWithSamePolicyName.getPolicyName());
                //         repairer.genRepair();
                //         repairSet.add(repairer);
                //         continue;
                //         // if
                //         // (!otherRepairerWithSamePolicyName.getBgpRoute().getIpPrefixString().equals(((RouteForbiddenLocalizer)
                //         // localizer).getBgpRoute().getIpPrefixString())) {
                //         // // policy相同但需要修复的前缀不同，引用现有的防止route-policy node重复
                //         // RouteForbiddenRepairer repairer = new RouteForbiddenRepairer(
                //         // (RouteForbiddenLocalizer) localizer,
                //         // otherRepairerWithSamePolicyName.getRoutePolicy());
                //         // repairer.genRepair();
                //         // repairSet.add(repairer);
                //         // continue;
                //         // }
                //     }
                // }
                Repairer repairer = localizer.genRepairer();
                if(repairer != null) {
                    repairer.genRepair();
                }
                repairSet.add(repairer);

            }
        }
    }

    

        /**
     * 最后处理各个节点的 repairer 的 del和 add行，因为有的错误涉及两个节点（如peer），需要把它们放到各自的 repairedLine里
     */

     public void finish(boolean ifPrint) {

        for (String node : repairMap.keySet()) {
            for (Repairer repairer : repairMap.get(node)) {
                if (repairer == null) {
                    // 还没有implement的repairer
                    continue;
                }
                if (repairer instanceof PeerRepairer) {
                    if (((PeerRepairer) repairer).getPeerAddedLines() != null) {
                        ((PeerRepairer) repairer).getPeerAddedLines().forEach((lineNum, lines) -> {
                            addRepairLines(addLineMap, ((PeerRepairer) repairer).peerDevName, lineNum, lines);
                        });
                    }
                    if (((PeerRepairer) repairer).getPeerDeletedLines() != null) {
                        ((PeerRepairer) repairer).getPeerDeletedLines().forEach((lineNum, line) -> {
                            addRepairLine(delLineMap, ((PeerRepairer) repairer).peerDevName, lineNum, line);
                        });
                    }
                }
                
                repairer.getAddedLines().forEach(line-> {
                    addRepairLine(addLineMap, node, line.getLineNumber(), line.getLine());
                });
                repairer.getDeletedLines().forEach(line-> {
                    addRepairLine(addLineMap, node, line.getLineNumber(), line.getLine());
                });
            }
        }
        if (ifPrint) {
            printLineMap(true);
            printLineMap(false); 
        }
        

        System.out.println(" ");
    }

}
