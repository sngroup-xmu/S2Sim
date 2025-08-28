// /*
//  * @Author: Rulan Yang yrll@github.com
//  * @Date: 2024-01-21 23:06:50
//  * @LastEditors: Rulan Yang yrll@github.com
//  * @LastEditTime: 2024-01-22 22:31:31
//  * @FilePath: /batfish-repair-/projects/batfish/src/main/java/org/batfish/diagnosis/reference/Dijkstra.java
//  * @Description: 
//  * 
//  * Copyright (c) 2024 by ${git_name_email}, All Rights Reserved. 
//  */
// package org.batfish.diagnosis.reference;

// import java.util.ArrayList;
// import java.util.HashMap;
// import java.util.LinkedList;
// import java.util.List;
// import java.util.Map;

// /**
//  * Created by malmardi on 11/26/2016.
//  */
// public class Dijkstra {

//     HashMap<String, LinkedList<String>> distances = new HashMap<>();

//     public void algorithm(String source, HashMap<String, DijkstraNode> nodes) {
//         //Initialization
//         distances = new HashMap<>();
//         for (String key : nodes.keySet()) {
//             if (!key.equals(source)) {
//                 LinkedList<String> temp = new LinkedList<>();
//                 temp.add(source);
//                 temp.add(String.valueOf(Integer.MAX_VALUE));
//                 distances.put(key, temp);
//             }
//         }

//         for (String key : nodes.get(source).getAdjacencyList().keySet()) {
//             if (distances.containsKey(key)) {

//                 LinkedList<String> temp = new LinkedList<>();
//                 temp.add(source);
//                 temp.add(nodes.get(source).getAdjacencyList().get(key).toString());
//                 distances.replace(key, temp);
//             }
//         }

//         LinkedList<String> N = new LinkedList<>();
//         N.add(source);

//         String oldNode = source;
//         while (N.size() < nodes.size()) {

//             Integer minCost = Integer.MAX_VALUE;
//             String minNode = "";
//             for (String key : distances.keySet()) {
//                 if (Integer.parseInt(distances.get(key).get(1)) < minCost) {
//                     minCost = Integer.parseInt(distances.get(key).get(1));
//                     minNode = key;
//                 }
//             }

//             LinkedList<String> temp = new LinkedList<>();
//             temp.add(distances.get(minNode).get(0).toString());
//             temp.add(Integer.toString(minCost));


//             nodes.get(source).selectedNodes.put(minNode, temp);


//             Integer oldCost = minCost;
//             distances.remove(minNode);

//             for (String key : nodes.get(minNode).getAdjacencyList().keySet()) {
//                 if (distances.containsKey(key))
//                     if (Integer.parseInt(distances.get(key).get(1)) >= (nodes.get(minNode).getAdjacencyList().get(key) + oldCost)) {
//                         LinkedList<String> temp2 = new LinkedList<>();
//                         temp2.add(minNode);
//                         temp2.add(Integer.toString(nodes.get(minNode).getAdjacencyList().get(key) + oldCost));
//                         distances.replace(key, temp2);

//                     }
//             }
//             oldNode = minNode;
//             N.add(minNode);
//         }
//     }

//     public void init() {
//         distances = new HashMap<>();
//     }

//     // 输入必须是连通图
//     public static LinkedList<String> findPath(String source, String destination, HashMap<String, DijkstraNode> nodes) {
//         Dijkstra dijkstra = new Dijkstra();
//         dijkstra.algorithm(source, nodes);
//         LinkedList<String> path = new LinkedList<>();
//         path.add(destination);
//         if (!nodes.get(source).selectedNodes.containsKey(destination)) {
//             return null;
//         }
//         String temp = nodes.get(source).selectedNodes.get(destination).get(0).toString();
//         path.add(temp);
//         while (!temp.equals(source)) {
//             temp = nodes.get(source).selectedNodes.get(temp).get(0).toString();
//             path.add(temp);
//         }
//         return path;
//     }

//     public static List<LinkedList<String>> findKEdgeDisjointPaths(String source, String destination, Map<String, String> graph, int k) {
//         List<LinkedList<String>> paths = new ArrayList<>();
//         HashMap<String, DijkstraNode> nodes = DijkstraNode.parseNodes(graph);
//         LinkedList<String> path = Dijkstra.findPath(source, destination, nodes);
//         paths.add(path);
//         printPath(path);
//         while (k-->0) {
//             nodes.forEach((nodeName, node)->node.init());
//             DijkstraNode.removeEdges(path, nodes);
//             DijkstraNode.process(source, nodes);
//             path = Dijkstra.findPath(source, destination, nodes);
//             paths.add(path);
//             printPath(path);
//         }
//         return paths;
//     }

//     public static void printPath(LinkedList<String> path) {
//         if (path==null) {
//             System.out.println("There is no path");
//             return;
//         }
//         for (int i = path.size() - 1; i > 0; i--) {
//             System.out.print(path.get(i) + "->");
//         }
//         System.out.println(path.get(0));
//     }
// }

