//package org.batfish.diagnosis.reference;
//
//import org.batfish.datamodel.DataPlane;
//import org.batfish.datamodel.Topology;
//import org.batfish.datamodel.bgp.BgpTopology;
//import org.batfish.diagnosis.common.DiagnosedFlow;
//import org.batfish.diagnosis.util.KeyWord;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.HashSet;
//import java.util.HashMap;
//
///*
// * maintain the all overlay info (BGP and Static, maybe OSPF in the future) of the network
// * Suppose that the BgpTopology is connected before using the generator,
// * We will diagnose the BgpPeer connection first
// */
//
///**
// * Maintain the all reference control plane symbols, now we support 3 types of network:
// *  1) BGP network
// *     - using eBGP to connect each other
// *     - disseminating Static/Connected routes
// *  2) BGP + OSPF network
// *     - using iBGP/eBGP to connect each other
// *     - iBGP peer Ips (Loopback0 interface) rely on OSPF
// *     - disseminating Static/Connected routes
// *  3) OSPF network
// *     - basic OSPF configuration: router-id [], network [] area [], ip cost []
// */
//public class BgpGenerator {
//
//    public DiagnosedFlow getFlow() {
//        return flow;
//    }
//
//    private DiagnosedFlow flow;
//
//    private BgpForwardingTree _bgpForwardingTree;
//    private StaticForwardingTree staticForwardingTree;
//
//    // 对于错误bgpTree的generator，这里的bgp topo就是错误的那个（但对new generator，初始化的时候还是用的错误的bgp topo）
//    private BgpTopology _bgpTopology;
//
//    private Topology _topology;
//
//    public BgpGenerator(DiagnosedFlow flow,
//                        BgpTopology bgpTopology,
//                        Topology layer3Topology) {
//        this.flow = flow;
//        _bgpTopology = bgpTopology;
//        _topology = layer3Topology;
//    }
//
//
//    public BgpTopology getBgpTopology() {
//        return _bgpTopology;
//    }
//
//    public BgpForwardingTree getBgpTree() {
//        return _bgpForwardingTree;
//    }
//
//    public StaticForwardingTree getStaticTree() { return staticForwardingTree; }
//
//    private <T> boolean ifTwoPathOverlap (List<T> p1, List<T> p2) {
//        for (T node : p1) {
//            if (p2.contains(node)) {
//                return true;
//            }
//        }
//        return false;
//    }
//
//    public void initializeBgpForwardingTree(DataPlane dataPlane) {
//        _bgpForwardingTree = new BgpForwardingTree(flow, _bgpTopology);
//        if (dataPlane!=null) {
//            _bgpForwardingTree.initialize(dataPlane);
//        }
//    }
//
//    private void printStringList(List<String> list, String title, String seperator) {
//        System.out.println(KeyWord.PRINT_LINE_HALF + title + KeyWord.PRINT_LINE_HALF);
//        for (String string : list) {
//            System.out.print(string + seperator);
//        }
//        System.out.println();
//    }
//
//
//    private List<String> copyPath(List<String> path, int fromIndex, int toIndex) {
//        List<String> newPath = new ArrayList<>();
//        assert toIndex>fromIndex && path.size()>=toIndex && fromIndex>=0;
//        for (int i=fromIndex; i<toIndex; i+=1) {
//            newPath.add(path.get(i));
//        }
//        return newPath;
//    }
//
//    // 输入是spec中要求可达的节点，如果某个节点X在AS i中，则需要将AS i中其他iBGP节点也加入需要可达的nodes集合
//    // 如果使用隧道，则不需要AS内 full mesh
//    // 返回的节点集合就是要在MST加入的节点
//    // TODO:
//    private Set<String> processReachNodes(Set<String> nodes) {
//        nodes.remove(flow.getDstNode());
//        return nodes;
//    }
//
//
//
//    /*
//     * 这一步只生成BGP Tree里的路由传播和优选的树（实际转发时，域内路径可能会有出入）
//     * 这里的出入源自于域内传播的BGP路由下一跳不一定永远是接收eBGP路由的入节点
//     * 【域内(AS内)可能会通过不止一个IGP域相连，所以反射时下一跳可能会多次改变】
//     * 1）跨IGP域的节点可能改下一跳再传 :o
//     * 2）跨IGP域的节点终止目标路由（明细路由）传播，发送默认路由至上游节点 :(
//     * ---------------------------------------------------------------------------
//     * 现有的错误的tree必须是连通的
//     * TODO: 现有的【根据prov信息生成的】tree不连通？会有这种情况出现么？不会吧？有的话是什么情况呢
//     * TODO: 这里生成的BGP树是针对可达性要求的（最小生成树算法【如果有reference参考，可以考虑改进选下一个“最小边”的指标】）
//     * 如果有waypoint/bypass要求，需要先改动原有的错误的树（还没实现），但这里要分情况考虑：
//     * 1）如果是要经过/绕开AS内部(intra)某些节点：a) 非边界节点：需要IGP支持，b）边界节点：BGP策略
//     * 2）如果要经过/绕开某些AS(inter)：则此时路由传播和优选的路径就和转发的路径一致（AS-level）
//     * ---------------------------------------------------------------------------
//     * PS: 现在暂时都不考虑ACL这种数据面的策略
//     */
//
//    public BgpForwardingTree genBgpTree(Set<String> reqReachNodes, BgpForwardingTree refTree) {
//        // error oldTree 所在的generator调用，oldBGPTree已经在generator里
//        // 目标src节点是当前generator里的unreachable nodes
//        // 在现有error oldBGPTree上继续生成minTree【针对路由传播的tree: BestRouteFrom】
//
//        // 假设bgpTopo是连通的，即不需要bgpTopology的参考来构造转发树
//
//        Set<String> reachableNodes = new HashSet<>(_bgpForwardingTree.getReachableNodes());
//
//        reqReachNodes.removeAll(reachableNodes);
//        Set<String> unreachableNodes = _bgpForwardingTree.getUnreachableNodes();
//        unreachableNodes.removeAll(reachableNodes);
//        // 考虑fail节点
//
//        Map<String, Integer> distanceMap = new HashMap<>();
//        Map<String, String> primNearestNodeMap = new HashMap<>(_bgpForwardingTree.getBestRouteFromMap());
//        // disMap initialization
//        _bgpTopology.getGraph().nodes().forEach(node->distanceMap.put(node.getHostname(), Integer.MAX_VALUE));
//        // TODO: 如果没有serialize到BGPTree时，没有节点和bgp ip的映射，这里会出现bestRouteFrom和nextHopForwarding不一致问题：nextHop有devName，但是bestRouteFrom没有devName
//        reachableNodes.forEach(node->distanceMap.put(node, _bgpForwardingTree.getBestRouteFromPath(node).size()-1));
//        // dstNode init
//        distanceMap.put(flow.getDstNode(), 0);
//
//        // 用ref的连接信息参考作为Prim的加入节点选择（MST不止一个时）
//        String curNode = _bgpForwardingTree.chooseFirstNodeHasUnreachablePeer(_bgpTopology); // 选一个已reach的开始
//
//        // 把已有的reach节点的peer遍历更新一遍disMap【重要，不然会出现有点节点拓展失败的情况】
//        for (String reachedNode: reachableNodes) {
//            for (String peer: _bgpTopology.getPeers(reachedNode)) {
//                if (distanceMap.get(reachedNode) + 1 < distanceMap.get(peer)) {
//                    distanceMap.put(peer, distanceMap.get(reachedNode) + 1);
//                    primNearestNodeMap.put(peer, reachedNode);
//                } else if (distanceMap.get(reachedNode) + 1 == distanceMap.get(peer)) {
//                    if (refTree!=null) {
//                        // 参考正确流的信息
//                        if (refTree.getBestRouteFromNode(peer).equals(reachedNode)) {
//                            primNearestNodeMap.put(peer, reachedNode);
//                        }
//                    }
//                }
//            }
//        }
//        // 开始在当前MST上加节点
//        while (reqReachNodes.size()>0) {
//            for (String peer: _bgpTopology.getPeers(curNode)) {
////                if (reachableNodes.contains(peer)) {
////                    // 更新disMap时只考虑spec要求的节点集合
////                    continue;
////                }
//                System.out.println("*****************");
//                System.out.println(curNode);
//                System.out.println(peer);
//                if (distanceMap.get(curNode) + 1 < distanceMap.get(peer)) {
//                    distanceMap.put(peer, distanceMap.get(curNode) + 1);
//                    primNearestNodeMap.put(peer, curNode);
//                } else if (distanceMap.get(curNode) + 1 == distanceMap.get(peer)) {
//                    if (refTree != null) {
//                        // 参考正确流的信息
//                        if (refTree.getBestRouteFromNode(peer).equals(curNode)) {
//                            primNearestNodeMap.put(peer, curNode);
//                        }
//                    }
//                }
//            }
//            // 选下一个进入树的节点
//            // TODO: 根据refTree选？
//            String nextNode = unreachableNodes.iterator().next();
//            for (String unreachNode : unreachableNodes) {
//                if (distanceMap.get(nextNode) > distanceMap.get(unreachNode)) {
//                    nextNode = unreachNode;
//                }
//            }
//
//            _bgpForwardingTree.addBestRouteFromEdge(nextNode, primNearestNodeMap.get(nextNode));
//            curNode = nextNode;
//            unreachableNodes.remove(curNode);
//            reqReachNodes.remove(curNode);
//            reachableNodes.add(curNode);
//            printStringList(_bgpForwardingTree.getBestRouteFromPath(curNode), "NEW-PATH", ", ");
//        }
//
//        return _bgpForwardingTree;
//    }
//
//    private String getNextNodeAddToMST(Set<String> nodes, Map<String, Integer> distanceMap) {
//        if (nodes.size()<1) {
//            return null;
//        } else if (nodes.size()==1) {
//            return nodes.iterator().next();
//        } else {
//            String node = nodes.iterator().next();
//            int dis = distanceMap.get(node);
//            for (String n: nodes) {
//                if (distanceMap.get(n) < dis) {
//                    dis = distanceMap.get(n);
//                    node = n;
//                }
//            }
//            return node;
//        }
//    }
//
//}
