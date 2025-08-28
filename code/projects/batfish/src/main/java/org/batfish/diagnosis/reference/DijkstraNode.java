// /*
//  * @Author: Rulan Yang yrll@github.com
//  * @Date: 2024-01-21 23:06:59
//  * @LastEditors: Rulan Yang yrll@github.com
//  * @LastEditTime: 2024-01-22 15:15:16
//  * @FilePath: /batfish-repair-/projects/batfish/src/main/java/org/batfish/diagnosis/reference/DijkstraNode.java
//  * @Description: 
//  * 
//  * Copyright (c) 2024 by ${git_name_email}, All Rights Reserved. 
//  */
// package org.batfish.diagnosis.reference;

// import java.io.BufferedReader;
// import java.io.FileReader;
// import java.io.IOException;
// import java.util.*;

// public class DijkstraNode {

//     String id;
//     int cost;
//     private HashMap<String, Integer> adjacencyList=new HashMap<>();
//     HashMap<String,LinkedList<String>> selectedNodes=new HashMap<>();
//     int minCost=Integer.MAX_VALUE;
//     int minNode;

//     public HashMap<String, Integer> getAdjacencyList() {
//         return adjacencyList;
//     }

//     public void removeAdjacency(String node) {
//         adjacencyList.remove(node);
//     }

//     public DijkstraNode(String _id)
//     {
//         id=_id;
//     }

//     public static HashMap<String, DijkstraNode> parseNodesFromFile(String filePath) {
//         HashMap<String, DijkstraNode> nodes = new HashMap<>();
//         BufferedReader reader = null;
//         try {
//             reader = new BufferedReader(new FileReader(filePath));
//             String test_split[];
//             String text = null;
//             int count = Integer.parseInt(reader.readLine());


//             while ((text = reader.readLine()) != null) {
//                 if (text.equals("")) {
//                     continue;
//                 }
//                 test_split = text.split(" ");
//                 String id = test_split[1];
//                 String pred = test_split[0];
//                 int cost = Integer.parseInt(test_split[2]);


//                 if (!nodes.containsKey(id)) {
//                     nodes.put(id, new DijkstraNode(id));
//                     nodes.get(id).adjacencyList.put(pred, cost);
//                 } else {
//                     nodes.get(id).adjacencyList.put(pred, cost);
//                 }

//                 if (!nodes.containsKey(pred)) {
//                     nodes.put(pred, new DijkstraNode(pred));
//                     nodes.get(pred).adjacencyList.put(id, cost);
//                 } else {
//                     nodes.get(pred).adjacencyList.put(id, cost);
//                 }
//             }
//         } catch (IOException e) {
//             throw new RuntimeException(e);
//         }
//         return nodes;
//     }

//     public static HashMap<String, DijkstraNode> parseNodes(Map<String, String> nodeConnectionMap) {
//         HashMap<String, DijkstraNode> nodes = new HashMap<>();

//         for (String node: nodeConnectionMap.keySet()) {
//             String peer = nodeConnectionMap.get(node);
            
//             String id = peer;
//             String pred = node;
//             int cost = 1;


//             if (!nodes.containsKey(id)) {
//                 nodes.put(id, new DijkstraNode(id));
//                 nodes.get(id).adjacencyList.put(pred, cost);
//             } else {
//                 nodes.get(id).adjacencyList.put(pred, cost);
//             }

//             if (!nodes.containsKey(pred)) {
//                 nodes.put(pred, new DijkstraNode(pred));
//                 nodes.get(pred).adjacencyList.put(id, cost);
//             } else {
//                 nodes.get(pred).adjacencyList.put(id, cost);
//             }
//         }

//         return nodes;
//     }


//     public void init() {
//         selectedNodes = new HashMap<>();
//     }

//     /**
//      * @description: 找到node1所在的连通分支【需要考虑fail的设备】
//      * @return {*}
//      */
//     public static Set<String> getConnectedComponent(String node1, HashMap<String, DijkstraNode> nodes) {
//         Set<String> connectedNodes = new HashSet<>();
//         // 以node1为起点开始BFS遍历它的neighbors
//         Queue<String> queue = new LinkedList<String>();
//         queue.add(node1);
//         while(!queue.isEmpty()) {
//             String curNode = queue.poll();
//             connectedNodes.add(curNode);
//             for (String peer : nodes.get(curNode).getAdjacencyList().keySet()) {
//                 if (!connectedNodes.contains(peer)) {
//                     queue.add(peer);
//                 }
//             }
//         }
//         return connectedNodes;
//     }

//     public static void removeEdges(LinkedList<String> path, HashMap<String, DijkstraNode> nodes) {
//         for (int i=0; i<path.size()-1; i++) {
//             String curNode = path.get(i);
//             String nextNode = path.get(i+1);
//             nodes.get(curNode).removeAdjacency(nextNode);
//             nodes.get(nextNode).removeAdjacency(curNode);
//         }
//     }

//     public static void process(String source, HashMap<String, DijkstraNode> nodes) {
//         // 把和source节点不在一个连通分支的节点去除
//         Set<String> sourceNodeConnectedComponent = getConnectedComponent(source, nodes);
//         if (sourceNodeConnectedComponent.size()<nodes.size()) {
//             Set<String> allNodes = new HashSet<>(nodes.keySet());
//             for (String n: allNodes) {
//                 if (!sourceNodeConnectedComponent.contains(n)) {
//                     nodes.remove(n);
//                 }
//             }
//         }
//     }
// }

