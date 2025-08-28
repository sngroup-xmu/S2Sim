package org.batfish.diagnosis.reference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.diagnosis.common.DiagnosedFlow;
import org.batfish.diagnosis.conditions.BgpCondition;
import org.batfish.diagnosis.conditions.SelectionRoute;
import org.batfish.diagnosis.util.InputData;
import org.batfish.diagnosis.util.KeyWord;

/**
 * The forwarding tree is for specific single prefix (one dst, multi src)
 */

public class BgpForwardingTree {

    BgpTopology _bgpTopology;

    DiagnosedFlow _flow;

    // The next-hop map is for each router to forward traffic
    // 只是针对【路由级的】每个节点【对应协议】计算出的下一跳的map
    // (It is allowed that each node reach its next-hop router using: 1) IGP, 2)
    // directed connection, 3) Static)
    private Map<String, String> _nextHopForwardingMap;
    // The best route source map is for each router to receive and select its best
    // route
    // 【路由级】路由传播和优选的from节点
    private Map<String, String> _bestRouteFromMap;
    // 一开始没有bgp路由的节点

    // k-failure场景下，一个节点可以有多个优选的neighbor/路由,所以用这个来存
    private Map<String, Set<String>> multiBestRouteFromMap;
    private Map<String, List<List<String>>> bestRoutesFromPathMap;

    private Set<String> _reachableNodes;
    // 一开始接收的可达路由的集合, 最优的, 最长前缀匹配的
    private Map<String, Bgpv4Route> _bestRouteMap;

    public void addBestRoutesFromPath(String node, List<List<String>> paths) {
        if (bestRoutesFromPathMap == null) {
            bestRoutesFromPathMap = new HashMap<>();
        }
        bestRoutesFromPathMap.put(node, paths);
    }

    public Bgpv4Route getBestBgpRoute(String node) {
        return _bestRouteMap.get(node);
    }

    public Set<String> getUnreachableNodes() {
        Set<String> allNodes = _bgpTopology.getNodesName();
        allNodes.removeAll(_reachableNodes);
        return allNodes;
    }

    public enum PathType {
        BEST_ROUTE_FROM,
        BEST_FORWARDING
    }

    public String chooseFirstNodeHasUnreachablePeer(BgpTopology bgpTopology) {
        Set<String> reachNodes = getReachableNodes();
        for (String node : reachNodes) {
            Set<String> peers = bgpTopology.getPeers(node);
            if (InputData.getIntersectantSet(peers, _reachableNodes).size() < peers.size()) {
                return node;
            }
        }
        assert false;
        return null;
    }

    public BgpForwardingTree(DiagnosedFlow flow, BgpTopology bgpTopology) {
        _flow = flow;
        _nextHopForwardingMap = new HashMap<>();
        _bestRouteFromMap = new HashMap<>();
        multiBestRouteFromMap = new HashMap<>();
        _bestRouteMap = new HashMap<>();

        _bgpTopology = bgpTopology;

        // 初始时unreachNodes是除了dst外所有节点
        _reachableNodes = new HashSet<>();
        _reachableNodes.add(flow.getDstNode());
    }

    public void initialize(DataPlane dataPlane) {
        // TODO: 根据 data plane 生成当前 nextHopForwardingMap, bestRouteFromMap, ...
    }

    public Map<String, String> getBestRouteFromMap() {
        return _bestRouteFromMap;
    }

    public void addNextHopForwardingEdge(String head, String tail) {
        _nextHopForwardingMap.put(head, tail);
    }

    public void addBestRouteFromEdge(String head, String tail) {
        Logger logger = Logger.getLogger(KeyWord.LOGGER_NAME);
        if (_flow.getFailureNumber() == 0 && _bestRouteFromMap.containsKey(head)) {
            if (!_bestRouteFromMap.get(head).equals(tail)) {
                logger.warning("INCONSISTENT FORWARDING BEHAVIOUR!!");
            }
        }
        _bestRouteFromMap.put(head, tail);
        if (!multiBestRouteFromMap.containsKey(head)) {
            multiBestRouteFromMap.put(head, new HashSet<>());
        }
        multiBestRouteFromMap.get(head).add(tail);
    }

    public Set<String> getReachableNodes() {
        // TODO: 选哪个map作为参考？
        return _reachableNodes;
    }

    // RECONSTRUCTION: genNewTree
    public boolean addBestRouteFromPath(List<String> path) {
        for (int i = 0; i < path.size() - 1; i += 1) {
            if (!_bestRouteFromMap.containsKey(path.get(i))) {
                _bestRouteFromMap.put(path.get(i), path.get(i + 1));
            }
            else {
                if (!_bestRouteFromMap.get(path.get(i)).equals(path.get(i + 1))) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<String> getBestRouteFromPath(String startDevName) {
        // start&end node are both in the returned path
        List<String> path = new ArrayList<>();
        path.add(startDevName);
        while (true) {
            String thisNode = path.get(path.size() - 1);
            if (thisNode.equals(_flow.getDstNode()) || !_bestRouteFromMap.containsKey(thisNode)) {
                break;
            }
            String nextNode = _bestRouteFromMap.get(thisNode);

            if (!thisNode.equals(_flow.getDstNode()) && path.contains(nextNode)) {
                System.out.println(path);
                System.out.println("LOOP!!!!!!!!!!!");
                return null;
            }
            path.add(nextNode);
        }
        // must be a valid path
        if (path.get(path.size() - 1).equals(_flow.getDstNode())) {
            return path;
        }
        else {
            return null;
        }
    }

    public List<String> getForwardingPath(String startDevName, String endDevName) {
        // start&end node are both in the returned path
        List<String> path = new ArrayList<>();
        path.add(startDevName);
        while (true) {
            String thisNode = path.get(path.size() - 1);

            if (thisNode.equals(endDevName) || !_nextHopForwardingMap.containsKey(thisNode)) {
                break;
            }

            String nextNode = _nextHopForwardingMap.get(thisNode);
            if (!thisNode.equals(endDevName) && path.contains(nextNode)) {
                System.out.println(path);
                System.out.println("LOOP!!!!!!!!!!!");
                return null;
            }
            path.add(nextNode);
        }
        // valid path
        if (path.get(path.size() - 1).equals(endDevName)) {
            return path;
        }
        else {
            return null;
        }
    }

    public Map<String, Set<String>> getAllInNeighbors(Map<String, String> forwardingMap,
        Set<String> considerNodes) {
        // 入参是BGPTree的bestRouteFromMap
        Map<String, Set<String>> maps = new HashMap<>();
        for (String node : forwardingMap.keySet()) {
            // the value node is the out neighbor of the key node
            if (node.equals(forwardingMap.get(node))) {
                // dst节点的下一跳是自己，所以不把自己加入prop/in 集合中
                continue;
            }
            if (!maps.containsKey(forwardingMap.get(node))) {
                Set<String> nodes = new HashSet<>();
                if (considerNodes.contains(node)
                    && considerNodes.contains(forwardingMap.get(node))) {
                    nodes.add(node);
                }

                maps.put(forwardingMap.get(node), nodes);
            }
            else {
                if (considerNodes.contains(node)
                    && considerNodes.contains(forwardingMap.get(node))) {
                    maps.get(forwardingMap.get(node)).add(node);
                }

            }
        }
        return maps;
    }

    public Map<String, Set<String>> getAllOutNeighbors(Map<String, String> forwardingMap,
        Set<String> considerNodes) {
        Map<String, Set<String>> maps = new HashMap<>();
        for (String node : forwardingMap.keySet()) {
            // the value node is the out neighbor of the key node
            if (node.equals(forwardingMap.get(node))) {
                // dst节点的下一跳是自己，所以不把自己加入acpt/out 集合中
                continue;
            }
            if (!maps.containsKey(node)) {
                Set<String> nodes = new HashSet<>();
                if (considerNodes.contains(node)
                    && considerNodes.contains(forwardingMap.get(node))) {
                    nodes.add(forwardingMap.get(node));
                }

                maps.put(node, nodes);
            }
            else {
                if (considerNodes.contains(node)
                    && considerNodes.contains(forwardingMap.get(node))) {
                    maps.get(node).add(forwardingMap.get(node));
                }

            }
        }
        return maps;
    }

    public List<Long> getAsPath(List<String> nodePath, BgpTopology bgpTopology) {
        List<Long> asPath = new ArrayList<>();
        for (int i = 1; i < nodePath.size(); i++) {
            long asNum;

            asNum = _bgpTopology.getAsNumber(nodePath.get(i), nodePath.get(i - 1));

            if (!asPath.contains(asNum)) {
                asPath.add(asNum);
            }

        }
        return asPath;
    }

    // TODO:暂未考虑有反射的情况
    public Map<String, Set<String>> getRRClients(Map<String, Set<String>> inNodes,
        Map<String, Set<String>> outNodes, BgpTopology bgpTopology) {
        // TODO 考虑和原有改动最小
        Map<String, Set<String>> clientsMap = new HashMap<>();
        // for (String node : inNodes.keySet()) {
        // Set<String> clients = new HashSet<>();
        // // in-nodes是要传出去路由的节点
        // Set<String> nodesIn = inNodes.get(node);
        // // out-nodes是转发流量的下一跳，也是要优选的节点
        // Set<String> nodesOut = outNodes.get(node);
        // if (nodesOut==null) {
        // // dstDev没有out nodes
        // continue;
        // }
        // // 把已经配了的删除了
        // List<String> nodesOutNotClient =
        // bgpTopology.getNodesInAs(bgpTopology.getAsNumber(node), nodesIn).stream()
        // .filter(nodeIn->!bgpTopology.ifConfiguredRRClient(node,
        // nodeIn)).collect(Collectors.toList());
        // List<String> nodesInNotClient =
        // bgpTopology.getNodesInAs(bgpTopology.getAsNumber(node), nodesIn).stream()
        // .filter(nodeIn->!bgpTopology.ifConfiguredRRClient(node,
        // nodeIn)).collect(Collectors.toList());
        //
        // if (nodesInNotClient.size()==0 || nodesOutNotClient.size()==0) {
        // continue;
        // } else if (nodesInNotClient.size() <= nodesOutNotClient.size()) {
        // nodesInNotClient.forEach(n->clients.add(n));
        // } else {
        // nodesOutNotClient.forEach(n->clients.add(n));
        // }
        //
        // if (clients.size() > 0) {
        // clientsMap.put(node, clients);
        // }
        //
        // }
        return clientsMap;
    }

    public List<String> getNextHopList(List<String> path, BgpTopology bgpTopology) {
        List<String> ipList = new ArrayList<>();

        for (int i = 1; i < path.size(); i++) {
            BgpSessionProperties session;
            BgpPeerConfigId curBgpPeerConfigId;
            String curNode = path.get(i);

            session = _bgpTopology.getBgpSessionProp(path.get(i), path.get(i - 1));
            curBgpPeerConfigId = _bgpTopology.getBgpPeerConfigId(path.get(i - 1), path.get(i));

            ipList.add(curBgpPeerConfigId.getRemotePeerPrefix().toString());

            if (session.isEbgp()) {
                // eBGP加一个就退出
                break;
            }
        }

        if (ipList.size() > 0) {
            return ipList;
        }
        else {
            return null;
        }
    }

    /**
     * @description: 无故障情况下生成BGP condition，主要用到_bestFromMap
     * @return {*}
     */
    public Map<String, BgpCondition> genBgpConditionsWithoutFailure(Set<String> reqSrcNodes,
        BgpTopology bgpTopology) {
        // Set<String> nodesSetCondition = new HashSet<>(_bestRouteFromMap.keySet()); // 2025/04/09-rulan
        Set<String> nodesSetCondition = new HashSet<>(); 

        for (String node : reqSrcNodes) {
            List<String> path = getBestRouteFromPath(node);
            nodesSetCondition.addAll(path);
        }

        // TODO: 1. assign the nextHop attribute for each route based on the traffic
        // forwarding tree
        Map<String, BgpCondition> conds = new HashMap<>();
        // prop是in-nodes
        Map<String, Set<String>> propNeighborMap = getAllInNeighbors(_bestRouteFromMap,
            nodesSetCondition);
        // accept是out-nodes
        Map<String, Set<String>> acptNeighborMap = getAllOutNeighbors(_bestRouteFromMap,
            nodesSetCondition);

        Map<String, Set<String>> clientsMap = getRRClients(propNeighborMap, acptNeighborMap,
            bgpTopology);

        // TODO: 这里的VPN到底应该指什么
        String vpnName = KeyWord.PUBLIC_VPN_NAME;
        for (String node : nodesSetCondition) {
            // dst节点单独设置
            if (node.equals(_flow.getDstNode())) {
                continue;
            }
            List<String> bestRouteFromPath = getBestRouteFromPath(node);
            // 把单条的selection route构造成集合
            SelectionRoute selectionRoute = new SelectionRoute.Builder(
                _flow.getCfgDstPrefixString()).vpnName(vpnName)
                .nextHop(getNextHopList(bestRouteFromPath, bgpTopology))
                .asPath(getAsPath(bestRouteFromPath, bgpTopology)).build();
            Set<SelectionRoute> selectionRoutes = new HashSet<>();
            selectionRoutes.add(selectionRoute);

            conds.put(node, new BgpCondition.Builder(_flow.getCfgDstPrefixString())
                // vpn名称只在有vpnName的节点上设置，其余设置public
                // prop-neighbor和acpt-neighbor还有peer的list可以去除不在nodesSetCondition里的节点
                .vpnName(vpnName).propNeighbors(propNeighborMap.get(node))
                .acptNeighbors(acptNeighborMap.get(node))
                .ibgpPeers(
                    InputData.getIntersectantSet(
                        bgpTopology.getBgpPeers(node,
                            InputData.getUnionSet(propNeighborMap.get(node),
                                acptNeighborMap.get(node)),
                            false),
                        nodesSetCondition))
                .ebgpPeers(
                    InputData.getIntersectantSet(
                        bgpTopology.getBgpPeers(node,
                            InputData.getUnionSet(propNeighborMap.get(node),
                                acptNeighborMap.get(node)),
                            true),
                        nodesSetCondition))
                .rrClient(clientsMap.get(node)).selectionRoutes(selectionRoutes)
                .redistribution(false).build());
        }
        // 添加终点（原发）节点的redistribute约束
        String node = _flow.getDstNode();
        conds.put(node, new BgpCondition.Builder(_flow.getCfgDstPrefixString()).vpnName(vpnName)
            .propNeighbors(propNeighborMap.get(node)).acptNeighbors(acptNeighborMap.get(node))
            .ibgpPeers(InputData.getIntersectantSet(
                bgpTopology.getBgpPeers(node, propNeighborMap.get(node), false),
                nodesSetCondition))
            .ebgpPeers(InputData.getIntersectantSet(
                bgpTopology.getBgpPeers(node, propNeighborMap.get(node), true),
                nodesSetCondition))
            .rrClient(clientsMap.get(node)).redistribution(true).build());
        return conds;
    }

    public Map<String, BgpCondition> genBgpConditions(Set<String> reqSrcNodes,
        BgpTopology bgpTopology) {
        if (_flow.getFailureNumber() == 0) {
            return genBgpConditionsWithoutFailure(reqSrcNodes, bgpTopology);
        }
        else {
            return genBgpConditionsWithFailure(reqSrcNodes, bgpTopology);
        }
    }

    /**
     * @description: 故障情况下生成BGP condition，通过遍历每个起点的路径来添加条件
     * @return {*}
     */
    public Map<String, BgpCondition> genBgpConditionsWithFailure(Set<String> reqSrcNodes,
        BgpTopology bgpTopology) {

        Set<String> allNodes = new HashSet<>();

        Map<String, BgpCondition> conds = new HashMap<>();
        // prop是in-nodes
        Map<String, Set<String>> propNeighborMap = new HashMap<>();
        // accept是out-nodes
        Map<String, Set<String>> acptNeighborMap = new HashMap<>();

        Map<String, Set<SelectionRoute>> selectionRouteMap = new HashMap<>();

        String vpnName = KeyWord.PUBLIC_VPN_NAME;

        for (String source : bestRoutesFromPathMap.keySet()) {
            List<List<String>> paths = bestRoutesFromPathMap.get(source);
            for (List<String> path : paths) {
                allNodes.addAll(path);
                // 遍历每一条path构造【path上每一个节点】的上下游节点以及优选的路由
                for (int i = 0; i < path.size() - 1; i++) {
                    String curNode = path.get(i);
                    String bestRouteFromNode = path.get(i + 1);
                    // accept的节点是最优路由的发送节点
                    if (!acptNeighborMap.containsKey(curNode)) {
                        acptNeighborMap.put(curNode, new HashSet<>());
                    }
                    acptNeighborMap.get(curNode).add(bestRouteFromNode);
                    // prop节点是发送最优路由的节点
                    if (!propNeighborMap.containsKey(bestRouteFromNode)) {
                        propNeighborMap.put(bestRouteFromNode, new HashSet<>());
                    }
                    propNeighborMap.get(bestRouteFromNode).add(curNode);
                    // selectionRoute通过当前节点之后的子路径构造
                    List<String> subPath = path.subList(i, path.size());
                    SelectionRoute selectionRoute = new SelectionRoute.Builder(
                        _flow.getCfgDstPrefixString()).vpnName(vpnName)
                        .nextHop(getNextHopList(subPath, bgpTopology))
                        .asPath(getAsPath(subPath, bgpTopology)).build();
                    if (!selectionRouteMap.containsKey(curNode)) {
                        selectionRouteMap.put(curNode, new HashSet<>());
                    }
                    selectionRouteMap.get(curNode).add(selectionRoute);
                }
            }
        }
        // 获取反射condition
        Map<String, Set<String>> clientsMap = getRRClients(propNeighborMap, acptNeighborMap,
            bgpTopology);

        for (String node : allNodes) {
            if (_flow.getDstNode().equals(node)) {
                conds.put(node, new BgpCondition.Builder(_flow.getCfgDstPrefixString())
                    .vpnName(vpnName).propNeighbors(propNeighborMap.get(node))
                    .acptNeighbors(acptNeighborMap.get(node))
                    .ibgpPeers(bgpTopology.getBgpPeers(node, propNeighborMap.get(node), false))
                    .ebgpPeers(bgpTopology.getBgpPeers(node, propNeighborMap.get(node), true))
                    .rrClient(clientsMap.get(node)).redistribution(true).build());
            }
            else {
                conds.put(node, new BgpCondition.Builder(_flow.getCfgDstPrefixString())
                    // vpn名称只在有vpnName的节点上设置，其余设置public
                    .vpnName(vpnName).propNeighbors(propNeighborMap.get(node))
                    .acptNeighbors(acptNeighborMap.get(node))
                    .ibgpPeers(bgpTopology.getBgpPeers(node,
                        InputData.getUnionSet(propNeighborMap.get(node),
                            acptNeighborMap.get(node)),
                        false))
                    .ebgpPeers(bgpTopology.getBgpPeers(node,
                        InputData.getUnionSet(propNeighborMap.get(node),
                            acptNeighborMap.get(node)),
                        true))
                    .rrClient(clientsMap.get(node)).selectionRoutes(selectionRouteMap.get(node))
                    .redistribution(false).build());
            }

        }

        return conds;
    }
    // public void serializeBgpCondition(String filePath, Map<String, BgpCondition>
    // conditions) {
    // Gson gson = new GsonBuilder().serializeNulls().create();
    // String jsonString = gson.toJson(conditions);
    // // System.out.println(jsonString);
    // try{
    // File file = new File(filePath);
    //
    // if(!file.getParentFile().exists()){
    // //若父目录不存在则创建父目录
    // file.getParentFile().mkdirs();
    // }
    //
    // if(file.exists()){
    // //若文件存在，则删除旧文件
    // file.delete();
    // }
    //
    // file.createNewFile();
    //
    // //将格式化后的字符串写入文件
    // // FileWriter writer = new FileWriter(filePath);
    // // writer.write(jsonString);
    // Writer writer = new OutputStreamWriter(new FileOutputStream(file),
    // StandardCharsets.UTF_8);
    // writer.write(jsonString);
    // writer.flush();
    // writer.close();
    // } catch (IOException e){
    // e.printStackTrace();
    // }
    // }

    public String getBestNextHop(String curNode) {
        if (_nextHopForwardingMap.containsKey(curNode)) {
            return _nextHopForwardingMap.get(curNode);
        }
        return null;
    }

    public String getBestRouteFromNode(String node) {
        if (_bestRouteFromMap.containsKey(node)) {
            return _bestRouteFromMap.get(node);
        }
        return null;
    }

    public <T> boolean ifSetContainsSameElement(Set<T> inputSet, T eleT) {
        for (T t : inputSet) {
            if (t.equals(eleT)) {
                return true;
            }
        }
        return false;
    }

    /*
     * 计算满足转发可达所需的IGP互通的点对集合： 1）IBGP Peer（只考虑原来在unreachableNodes里的，不在的说明已经可达了）
     * 2）所有节点BGP路由下一跳
     */
    /**
     * @param bgpTopology
     * @return
     */
    // public Map<String, Set<Node>> computeReachIgpNodes(BgpTopology bgpTopology) {
    // //
    // Map<String, Set<Node>> reachNodes = new HashMap<>();
    // Map<String, Set<String>> inNeighborMap = getAllInNeighbors(_bestRouteFromMap,
    // _bestRouteFromMap.keySet());
    // // Ibgp peer 互达
    // for (String node : inNeighborMap.keySet()) {
    // // 每个node都要和它的所有in节点建立peer关系（双向可达）
    // if (!reachNodes.containsKey(node)) {
    // reachNodes.put(node, new HashSet<Node>());
    // }
    // inNeighborMap.get(node).forEach(peer->{
    //
    // // node到peer可达
    // String peerIp;
    // if (bgpTopology.getNodeIp(peer)!=null) {
    // peerIp = bgpTopology.getNodeIp(peer);
    // } else {
    // peerIp = _bestRouteMap.get(peer).getPeerIpString();
    // }
    // Node reachPeerNode = new Node(peer, peerIp);
    // if (!ifSetContainsSameElement(reachNodes.get(node), reachPeerNode)) {
    // reachNodes.get(node).add(reachPeerNode);
    // }
    //
    // // peer到node也可达
    // if (!reachNodes.containsKey(peer)) {
    // reachNodes.put(peer, new HashSet<Node>());
    // }
    // String thisIp;
    // if (bgpTopology.getNodeIp(node)!=null) {
    // thisIp = bgpTopology.getNodeIp(node);
    // } else {
    // thisIp = _bestRouteMap.get(node).getPeerIpString();
    // }
    // Node reachThisNode = new Node(node, thisIp);
    // if (!ifSetContainsSameElement(reachNodes.get(peer), reachThisNode)) {
    // reachNodes.get(peer).add(reachThisNode);
    // }
    // });
    // }
    // // best-route下一跳可达
    // if (_bestRouteMap!=null) {
    // _bestRouteMap.forEach((node, route)->{
    // if (!reachNodes.containsKey(node)) {
    // reachNodes.put(node, new HashSet<Node>());
    // }
    // // TODO 避开EBGP下一跳
    // if
    // (bgpTopology.getAsNumber(node)==bgpTopology.getAsNumber(route.getNextHopDev()))
    // {
    // Node nextHopNode = new Node(route.getNextHopDev(),
    // route.getNextHopIpString());
    // if (!ifSetContainsSameElement(reachNodes.get(node), nextHopNode)) {
    // reachNodes.get(node).add(nextHopNode);
    // }
    // }
    // });
    // }
    //
    // return reachNodes;
    // }

}
