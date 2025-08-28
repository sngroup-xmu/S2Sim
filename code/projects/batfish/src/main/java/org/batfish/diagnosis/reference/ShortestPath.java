package org.batfish.diagnosis.reference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

// import java.util.*;

public class ShortestPath {
    private int V; // 无向图中的顶点个数
    private List<LinkedList<Integer>> adj; // 邻接表表示无向图

    public ShortestPath(int v) {
        V = v;
        adj= new ArrayList<>();
        for (int i = 0; i < v; ++i)
            adj.add(i, new LinkedList<>());
    }

    // 添加边
    void addEdge(int v, int w) {
        adj.get(v).add(w);
        adj.get(w).add(v);
    }

    void delEdge(int v, int w) {
        adj.get(v).remove(new Integer(w));
        adj.get(w).remove(new Integer(v));
    }

    // 输出最短路径
    List<Integer> printShortestPath(int src, int dest) {
        int[] dist = new int[V];
        int[] prev = new int[V];

        if (bfs(src, dest, dist, prev)) {
            ArrayList<Integer> path = new ArrayList<Integer>();
            int crawl = dest;
            path.add(crawl);

            while (prev[crawl] != -1) {
                path.add(prev[crawl]);
                crawl = prev[crawl];
            }

            System.out.println("最短路径是: ");
            for (int i = path.size() - 1; i >= 0; i--) {
                System.out.print(path.get(i) + " ");
            }
            return path;
        } else {
            System.out.println("源点无法到达终点");
            return null;
        }
    }

    // 使用BFS计算最短路径
    boolean bfs(int src, int dest, int[] dist, int[] prev) {
        LinkedList<Integer> queue = new LinkedList<Integer>();
        boolean[] visited = new boolean[V];

        for (int i = 0; i < V; i++) {
            visited[i] = false;
            dist[i] = Integer.MAX_VALUE;
            prev[i] = -1;
        }

        visited[src] = true;
        dist[src] = 0;
        queue.add(src);

        while (!queue.isEmpty()) {
            int u = queue.poll();

            for (int v : adj.get(u)) {
                if (!visited[v]) {
                    visited[v] = true;
                    dist[v] = dist[u] + 1;
                    prev[v] = u;
                    queue.add(v);

                    // 如果到达目标节点，则返回true

                    if (v == dest)
                        return true;
                }
            }
        }

        return false;
    }

    public static List<List<String>> findKEdgeDisjointPaths(String source, String destination, Map<String, Set<String>> graph, int k) {
        // 获取节点名称和数字的映射
        Map<String, Integer> nodesMap = new HashMap<>();
        Map<Integer, String> nodesMapReverse = new HashMap<>();
        int nodeIndex = 0;
        for (String n: graph.keySet()) {
            for (String v: graph.get(n)) {
                if (!nodesMap.containsKey(n)) {
                    nodesMap.put(n, nodeIndex++);
                    nodesMapReverse.put(nodeIndex-1,n);
                }
                if (!nodesMap.containsKey(v)) {
                    nodesMap.put(v, nodeIndex++);
                    nodesMapReverse.put(nodeIndex-1,v);
                }
            }
            
        }
        // 构造图
        ShortestPath g = new ShortestPath(nodesMap.size());
        graph.forEach((n,peers)->{
            peers.forEach(v->g.addEdge(nodesMap.get(n), nodesMap.get(v)));
        });
        // 起点终点的int值
        int sourceNum = nodesMap.get(source);
        int destinationNumber = nodesMap.get(destination);
        // 迭代k次找路
        List<List<String>> paths = new ArrayList<>();
        List<Integer> path = new ArrayList<>();
        while (k-->0 && path!=null) {
            path = g.printShortestPath(sourceNum, destinationNumber);
            if (path!=null) {
                List<String> stringPath = new ArrayList<>();
                path.forEach(n->stringPath.add(nodesMapReverse.get(n)));
                paths.add(stringPath);
                // 把边删掉
                for (int i=0; i<path.size()-1; i++) {
                    g.delEdge(path.get(i), path.get(i+1));
                }
            }
            
        }

        return paths;
    }
}

