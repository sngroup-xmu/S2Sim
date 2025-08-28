package org.batfish.diagnosis;

import java.io.File;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.batfish.common.topology.Layer2Edge;
import org.batfish.common.topology.Layer2Node;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.Interface;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.dataplane.ibdp.VirtualRouter;
import org.batfish.diagnosis.common.ConfigurationLine;
import org.batfish.diagnosis.common.DiagnosedFlow;
import org.batfish.diagnosis.conditions.BgpCondition;
import org.batfish.diagnosis.localization.InconsistentRouteLocalizer;
import org.batfish.diagnosis.localization.LocalizationUtil;
import org.batfish.diagnosis.localization.Localizer;
import org.batfish.diagnosis.localization.Violation;
import org.batfish.diagnosis.reference.BgpForwardingTree;
import org.batfish.diagnosis.reference.BgpGenerator;
import org.batfish.diagnosis.repair.BgpRepairerCollection;
import org.batfish.diagnosis.util.ConfigExtractor;
import org.batfish.diagnosis.util.ConfigTaint;
import org.batfish.diagnosis.util.InputData;
import org.batfish.main.DiagnosisInfo;

/**
* 针对一条flow的诊断,保存不同协议的诊断信息（当前主要是BGP的）
* */
public class Diagnoser {
  private BgpGenerator _bgpGenerator;
  private final DiagnosedFlow _flow;

  private DataPlane _dataPlane;
  private BgpTopology _bgpTopology;
  private Map<String, Configuration> _configurationMap;

  // 错误条件【外部输入】
  private Map<String, Violation> _violations;
  private Layer2Topology _layer2Topology;
  private Map<String, Set<String>> _layer2Neighbors;
  private Map<String, Map<String, String>> _nodeInfIpMap; // <node,<interface, ip>>
  private List<Layer2Edge> _layer2Edges;
//   private Map<String, Set<Layer2Edge>> _layer2Edges;
  Map<String, Set<Localizer>> localizersMap;

  public DiagnosedFlow getFlow() {
      return _flow;
  }

  public Map<String, Map<String, String>> getNodeInfIpMap() {
      return _nodeInfIpMap;
  }

  // 2025-04-13-rulan: 获取和toNode连接的thisNode的Interface的ip
  public String getInfIpToANode(String thisNode, String toNode) {
    Layer2Edge layer2Edge = getLayer2EdgeForNodes(thisNode, toNode);
    if (layer2Edge != null) {
        Layer2Node node1 = layer2Edge.getNode1();
        Layer2Node node2 = layer2Edge.getNode2();
        if (node1.getHostname().equals(thisNode)) {
            return _nodeInfIpMap.get(node1.getHostname()).get(node1.getInterfaceName());
        } else {
            return _nodeInfIpMap.get(node2.getHostname()).get(node2.getInterfaceName());
        }
    }
    return "0.0.0.0";
  }


  public Configuration getConfiguration(String node) {
      return _configurationMap.get(node);
  }

  public Layer2Topology reComputeLayer2Topology(Map<String, Configuration> configurationMap) {
        // 2025-04-12-rulan: 重新计算二层拓扑,顺便把二层邻居关系也计算出来
        _layer2Neighbors = new HashMap<>();
        _layer2Edges = new ArrayList<>();
        _nodeInfIpMap = new HashMap<>();
        Layer2Topology.Builder layer2TopologyBuilder = Layer2Topology.builder();
        Map<Prefix, Layer2Node> prefixToNode = new HashMap<>();
        for (String node: configurationMap.keySet()) {
            Configuration config = configurationMap.get(node);
            _nodeInfIpMap.put(node, new HashMap<>());
            config.getActiveInterfaces().forEach((infName, inf)->{
                Prefix infPrefix =
                    inf.getConcreteAddress() != null ? inf.getConcreteAddress().getPrefix() : null;
                if(inf.getConcreteAddress() != null) {
                  _nodeInfIpMap.get(node).put(infName, inf.getConcreteAddress().getIp().toString());
                }
                if (infPrefix != null) {
                    if (!prefixToNode.containsKey(infPrefix)) {
                        Layer2Node layer2Node = new Layer2Node(node, infName, null);
                        prefixToNode.put(infPrefix, layer2Node);
                    } else {
                        // 说明有多个接口在同一个prefix上
                        Layer2Node layer2Node = new Layer2Node(node, infName, null);
                        Layer2Node peerLayer2Node = prefixToNode.get(infPrefix);
                        Layer2Edge layer2Edge = new Layer2Edge(layer2Node, peerLayer2Node);
                        layer2TopologyBuilder.addEdge(layer2Edge);
                        if (!_layer2Neighbors.containsKey(node)) {
                            _layer2Neighbors.put(node, new HashSet<>());
                        }
                        if (!_layer2Neighbors.containsKey(peerLayer2Node.getHostname())) {
                            _layer2Neighbors.put(peerLayer2Node.getHostname(), new HashSet<>());
                        }
                        _layer2Neighbors.get(node).add(peerLayer2Node.getHostname());
                        _layer2Neighbors.get(peerLayer2Node.getHostname()).add(node);
                        _layer2Edges.add(layer2Edge);
                    }

                }
                
            });
        }
        return layer2TopologyBuilder.build();
  }

  public Layer2Edge getLayer2EdgeForNodes(String node1, String node2) {
      for (Layer2Edge edge: _layer2Edges) {
          if (edge.getNode1().getHostname().equals(node1) && edge.getNode2().getHostname().equals(node2) || 
          (edge.getNode1().getHostname().equals(node2) && edge.getNode2().getHostname().equals(node1))) {
              return edge;
          }
      }
      return null;
  }




  /**
   * 输入是一系列具有【相同dst节点&dst IP】的
   */
  public Diagnoser(DiagnosedFlow flow, DataPlane dataPlane,
                   BgpTopology bgpTopology, Map<String, Configuration> configurationMap, Layer2Topology layer2Topology) {
      // input数据初始化
      this._flow = flow;
      // 输入1：配置根目录
      this._dataPlane = dataPlane;
      this._configurationMap = configurationMap;
      this._bgpTopology = bgpTopology;
      if (layer2Topology == null) {
        layer2Topology = reComputeLayer2Topology(configurationMap);
        DiagnosisInfo._layer2Topology = layer2Topology;
        DiagnosisInfo._layer2Edges = _layer2Edges;
        DiagnosisInfo._layer2Neighbors = _layer2Neighbors;
      }
      this._layer2Topology = layer2Topology;
      _layer2Edges = DiagnosisInfo._layer2Edges;
      _layer2Neighbors = DiagnosisInfo._layer2Neighbors;

      // 2025-04-12-rulan: 检查是否所有的src节点都在bgp topology里


      // 检查BGP Topology是否连通,不连通加边保证BGP Topology连通（自动生成的配置中若BGP拓扑不连通则二层拓扑也不连通，因此先不考虑查找二层拓扑加边）
      // step 1: 得到不连通的src的节点(当前一条流一条流的诊断，因此源节点只有一个，后续多流一起做再更新getUnconnectedNodes)
      Set<String> unconnectedSrcNodes = getUnconnectedNodes(flow.getDstNode(), flow.getSrcNode());

      if (unconnectedSrcNodes.size()>0) {
        // 2025-04-12-rulan: 不连通就把layer2上所有可以连接的边都加上
        Map<String, Set<String>> bgpPeerMap = this._bgpTopology.getBgpPeerConnectionMap();
        Set<String> addedPeer = new HashSet<>();
        for (String node: bgpPeerMap.keySet()) {
            if (_layer2Neighbors.get(node).size() > bgpPeerMap.get(node).size()) {
                for (String possiblePeer: _layer2Neighbors.get(node)) {
                    if (!bgpPeerMap.get(node).contains(possiblePeer)) {
                        // 说明这个peer可以加上
                        Layer2Edge layer2Edge = getLayer2EdgeForNodes(node, possiblePeer);
                        Layer2Node node1 = layer2Edge.getNode1();
                        Layer2Node node2 = layer2Edge.getNode2();
                        if (layer2Edge != null) {
                            // 说明这个peer可以加上
                            this._bgpTopology = DiagnosisInfo.addBgpPeer(this._bgpTopology, node1.getHostname(), _nodeInfIpMap.get(node1.getHostname()).get(node1.getInterfaceName()),
                                                            node2.getHostname(), _nodeInfIpMap.get(node2.getHostname()).get(node2.getInterfaceName()),
                                                            _flow.getConfigPathMap());
                            addedPeer.add(possiblePeer);
                            System.out.println("ADD Edge: ( " + possiblePeer + " - " + node + " )");
                        }

                    }
                }
            }
        }
        // // 后续添加peer边过程中，可能某条边使得多个src同时连通，所以用remainNodes备份记录仍未达的节点
        // Set<String> remainUnconnectedSrcNodes = new HashSet<>(unconnectedSrcNodes);
        // // step 2: 把dst节点所在的连通分支找到
        // Set<String> dstComponentSet = getConnectedComponent(flow.getDstNode());
        // // step 2 分别找到src节点所在的连通分支们，每一个src所在的连通分支都要和dst分支连通
        // for (String srcNode: unconnectedSrcNodes) {
        //   // 如果某节点不在remain集合中，说明之前某次已经跟随其他节点的连通分支连通dst节点了
        //   if (!remainUnconnectedSrcNodes.contains(srcNode)) {
        //     continue;
        //   }
        //   // 每次使得一个src所在的连通分支和dst的分支连接成功
        //   Set<String> srcComponentSet = getConnectedComponent(srcNode);
        //   srcComponentSet.forEach(n->{
        //     if (remainUnconnectedSrcNodes.contains(n)) {
        //       remainUnconnectedSrcNodes.remove(n);
        //     }
        //   });
        //   // 选取一个数量更少的连通分支进行遍历，查找其中节点可以增加的peer
        //   Set<String> extendedNodeSet = srcComponentSet;
        //   if (srcComponentSet.size() > dstComponentSet.size()) {
        //     extendedNodeSet = dstComponentSet;
        //   }
        //   // 用flag记录是否存在一条边可以连通两个分支
        //   boolean connectAtOneHop = false;
        //   // 可能一条边还不能将两个分支连通起来，用一个集合保存备选的连通边
        //   Map<String, Set<String>> alternativePeers = new HashMap<>();
        //   // 遍历查找每个配置里配的peer比bgp topology里peer多的节点
        //   for (String node: extendedNodeSet) {
        //     if (connectAtOneHop) {
        //       // 说明已经找到一条边连通两个分支
        //       System.out.println("BGP Topology is connected!");
        //       break;
        //     }
        //     //configPeerIps记录的是<peerName,peerIp>
        //     Map<String,String> configPeers = ConfigExtractor.parseBgpPeerIpFromConfiguration(flow.getConfigPath(node));
        //     //existingPeers记录的是peerName
        //     Set<String> existingPeers = this._bgpTopology.getPeers(node);
        //     //TODO：如果peer两端都没配呢？就不会存在满足if条件的节点，这种情况需要考虑吗（查找Layer2拓扑好像可以解决，暂时不考虑这种情况）
        //     if (existingPeers.size() < configPeers.size()) {
        //       //删除配置中正确建立的peer，只留下没建好的
        //       for (String peerName: existingPeers) {
        //         configPeers.remove(peerName);
        //       }
        //       //遍历未建好的peer，加边
        //       for(String unBuildPeer : configPeers.keySet())
        //       {
        //         // TODO:确保配置中的peerIp是合法的接口ip
        //         // 看这个peer是否能把两个分支连通起来
        //         if (srcComponentSet.contains(node) && dstComponentSet.contains(unBuildPeer) || srcComponentSet.contains(unBuildPeer) && dstComponentSet.contains(node)) {
        //           // 该peer可以把两个分支连起来，将从该peer到当前节点的边添加到BGP topology上
        //           this._bgpTopology = DiagnosisInfo.addBgpPeer(this._bgpTopology, unBuildPeer, configPeers.get(unBuildPeer), node, ConfigExtractor.getPeerIpForPeer(flow.getConfigPath(node),unBuildPeer));
        //           connectAtOneHop = true;
        //           System.out.println("ADD Edge: ( " + unBuildPeer + " - " + node + " )");
        //           break;
        //         }
        //         if (!alternativePeers.containsKey(node)) {
        //           alternativePeers.put(node, new HashSet<>());
        //         }
        //         alternativePeers.get(node).add(unBuildPeer);
        //       }
        //     }
        //   }
        //   if (!connectAtOneHop) {
        //     System.out.println("Need more than one extra peers to make the bgp topology connected");
        //   }
        // }
      }
      //ToDO：多流场景下要保存更新后的BGP拓扑以保证加边的数量最少

      //用更新后的（如有）BGP拓扑生成BgpGenerator
      this._bgpGenerator = new BgpGenerator(flow, this._bgpTopology);
      this._bgpGenerator.initializeBgpForwardingTree(dataPlane);
  }

   public Set<String> getConnectedComponent(String node1) {
     Set<String> connectedNodes = new HashSet<>();
     // 以node1为起点开始BFS遍历它的neighbors
     Queue<String> queue = new LinkedList<String>();
     queue.add(node1);
     while(!queue.isEmpty()) {
       String curNode = queue.poll();
       connectedNodes.add(curNode);
       for (String peer : this._bgpTopology.getPeers(curNode)) {
         if (!connectedNodes.contains(peer)) {
           queue.add(peer);
         }
       }
     }
     return connectedNodes;
   }

   private Set<String> getUnconnectedNodes(String node1, Set<String> node2) {
    Set<String> unconnectedNodes = new HashSet<>();
    // generates the connection component using BFS
    if (!this._bgpTopology.getNodesName().contains(node1)) {
        return unconnectedNodes;
    }
    Set<String> connectedComponentForNode1 = getConnectedComponent(node1);
    // 最终visited标志位为true的表示node1所在连通分支
    for (String node: node2) {
        if (!connectedComponentForNode1.contains(node)) {
            unconnectedNodes.add(node);
        }
    }
    return unconnectedNodes;
}

  /*
   * 再serialize BGP之前，判断目的前缀是否在目的设备上源发了
   *      如果这里返回false，表示后面BGP的provenance文件失效（尽管文件里有prov信息，但是那是其他设备上源发的）
   * */

    public static String getTargetPrefixFromDirectOrStatic(String node, String curPrefixString, VirtualRouter virtualRouter) {
        Prefix curPrefix = null;
        if (Ip.tryParse(curPrefixString).isPresent()) {
            curPrefix = Ip.parse(curPrefixString).toPrefix();
        } else if (Prefix.tryParse(curPrefixString).isPresent()){
            curPrefix = Prefix.parse(curPrefixString);
        } else {
            assert false: "INPUT PREFIX_STRING INVALID: " + curPrefixString;
        }
        // STEP 1: 先在接口上遍历一遍查找
        Interface inf = LocalizationUtil.findTargetInfNameFromConfig(curPrefix, virtualRouter.getConfiguration());
        if (inf!=null) {
            return inf.getPrimaryNetwork().toString();
        }


        // STEP 2: 找配置里的static route
        for (StaticRoute r: virtualRouter.getConfiguration().getDefaultVrf().getStaticRoutes()) {
            if (r.getNetwork().containsPrefix(curPrefix)) {
                return r.getNetwork().toString();
            }
        }

        return curPrefix.toString();
    }


    public BgpGenerator getBgpGenerator() {
        return _bgpGenerator;
    }

    public Map<String, BgpCondition> diagnose(boolean ifSave) {
        /*
           STEP1: BGP 诊断
            1) BGP 连通，诊断 BGP Peer
            2) BGP condition生成
         */

        // TODO: BGP Peer 生成

        Set<String> reachNodes = new HashSet<>();
        reachNodes.addAll(_bgpGenerator.getBgpTree().getReachableNodes());
        // use the correct traffic as a reference to generate the policy-compliant "Forwarding Tree"
        BgpForwardingTree reqTree;
        // 模块1：BGP路由可达诊断
        reqTree = _bgpGenerator.genBgpTree(null);

        Set<String> reqReachNodes = new HashSet<>();
        reqReachNodes.addAll(_flow.getSrcNode());
        Map<String, BgpCondition> condMap = reqTree.genBgpConditions(reqReachNodes, _bgpGenerator.getBgpTopology());
        if (ifSave) {
            BgpCondition.serialize(condMap, _flow.getConditionPath());
        }
        return condMap;

    }

    public void printErrorLines(Map<String, Map<Integer, String>> lines) {
        for (String node: lines.keySet()) {
            System.out.println("---------------------------" + node + "---------------------------");
            List<Integer> lineNumList = new ArrayList<>(lines.get(node).keySet());
            Collections.sort(lineNumList);
            lineNumList.forEach(num->{
                System.out.println("[" + num + "]" + " " +lines.get(node).get(num));
            });

            System.out.println();
        }
    }


    public Map<String, Map<Integer, String>> mergeLinesMap(Map<String, Map<Integer, String>> map1, Map<String, Map<Integer, String>> map2) {
        for (String node: map2.keySet()) {
            if (map1.containsKey(node)) {
                map1.get(node).putAll(map2.get(node));
            } else {
                map1.put(node, map2.get(node));
            }
        }
        return map1;
    }

    //TODO: implementation
    private Set<String> getNodesInSameAs(String node, BgpTopology bgpTopology) {
        return new HashSet<String>();
    }

    /**
     * 最精确的检测方法是static的下一跳和BGP的转发路径不成环，但是由于无法提前得知BGP的实际转发路径，
     * 所以用了一个更强的条件：static下一跳和newBgpTree的下一跳一致
     * 输入:
     *  1) reqNodes是flow的requirement指定的src节点
     */
    public Map<String, List<ConfigurationLine>> localizeInconsistentStaticAndDirect(
            BgpGenerator newGenerator) {
        Map<String, List<ConfigurationLine>> lineMap = new HashMap<>();
        // 保持这个set是为了检查非隧道的节点时，避免一个AS内的节点反复查
        Set<String> nodesAlreadyChecked = new HashSet<>();
        for (String node: _flow.getSrcNode()) {
            Set<String> reqNodes = getNodesInSameAs(node, _bgpGenerator.getBgpTopology());

            for (String checkNode : reqNodes) {

                if (nodesAlreadyChecked.contains(checkNode) || checkNode.equals(_flow.getDstNode())) {
                    continue;
                }
                nodesAlreadyChecked.add(checkNode);

                // bgp的next-hop应该是一个远端节点？邻接的下一跳节点还是需要迭代查IGP的路径？
                String bgpNextHop = newGenerator.getBgpTree().getBestNextHop(checkNode);
                // @TODO:获取节点的BGPRoute，后面用于设置是否需要考虑找到会影响转发的默认路由
                Bgpv4Route bgpv4Route = newGenerator.getBgpTree().getBestBgpRoute(checkNode);
                if (newGenerator.getStaticTree().getNextHop(checkNode) != null) {
                    // 确定static tree上已经有静态路由的
                    String staticNextHop = newGenerator.getStaticTree().getNextHop(checkNode);
                    if (staticNextHop == null) {
                        // 表示这条静态路由的下一跳无效，不影响错误
                        continue;
                    }
                    // TODO 确定bestBgp来自EBGP/IBGP/LOCAL，比较其和Static的优先级
                    if (bgpNextHop != null && !staticNextHop.equals(bgpNextHop)) {
                        long staticPref = newGenerator.getStaticTree().getBestRoute(checkNode)
                                .getMetric();
                        long bgpRoutePref = 100;
                        if (staticPref <= bgpRoutePref) {
                            StaticRoute route = newGenerator.getStaticTree().getBestRoute(checkNode);
                            ConfigurationLine configurationLine = ConfigTaint.staticRouteLinesFinder(
                                    checkNode, newGenerator.getStaticTree().getBestRoute(checkNode),
                                    getConfigPath(checkNode));
                            if (!lineMap.containsKey(checkNode)) {
                                lineMap.put(checkNode, new ArrayList<>());
                            }
                            lineMap.get(checkNode).add(configurationLine);
                            addLocalizerToViolation(checkNode, new InconsistentRouteLocalizer(checkNode,
                                    route, configurationLine));
                        }
                    }
                }
                else {
                    // 检查是否有direct路由
                    // 检查有没有dstPrefix包含的网段受到静态路由影响，这里输入的prefix是spec里指定的那个
                    int a = 1;
                }

            }
        }
        return lineMap;
    }

    public String getConfigPath(String node) {
        return _flow.getConfigPath(node);
    }

    public static String fromJsonToString(String filePath) {
        File file = new File(filePath);
        String jsonStr = "";
        if(file.exists()){
            try {
                jsonStr = FileUtils.readFileToString(file,"UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return jsonStr;
    }

    /**
     * Localization入口
     * @param violationMap 第二次仿真的 path condition
     /* @param newGenerator 第二次仿真的 信息
     * @return
     */
    //public void localize(Map<String, Violation> violationMap, BgpGenerator newGenerator) {

    public void localize(Map<String, Violation> violationMap, boolean ifSave) {
        _violations = violationMap;

        Map<String, Map<Integer, String>> errlinesMap = new HashMap<>();

        // STEP 1: 根据violations定位BGP协议相关的配置错误行
        errlinesMap = localizeErrorsFromViolations();
        if(ifSave){
          String filePath = _flow.getNetworkRootPath() + "/result.txt";
          try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            for (String node : errlinesMap.keySet()) {
              writer.write("\nError Configuration: " + node + ".cfg\n");
              for (Map.Entry<Integer, String> entry : errlinesMap.get(node).entrySet()) {
                writer.write("ErrorLineNumber: " + entry.getKey() + "\n");
                writer.write("Command: " + entry.getValue() + "\n");
              }
            }
            System.out.println("错误信息已写入文件 result.txt");
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        else{
          for(String node : errlinesMap.keySet()){
            System.out.println("\nError Configuration:" + node + ".cfg");
            errlinesMap.get(node).forEach((num, cfg)->{
              System.out.println("ErrorLineNumber:" + num + "\n" + "Command:" + cfg);
            });
          }
        }

        // STEP 2: 根据BGP路由收敛的结果诊断静态路由的错误（还需完善，这个对静态路由的检查是互相依赖的，所以如果全部查到，应该要等BGP、IGP都收敛后才行）
        //
        //localizeInconsistentStaticAndDirect(newGenerator);

        // STEP 3: 根据BGP路由收敛的结果诊断BGP VPN上

        // STEP 4: 检查隧道使能

    }

    /**
     *
     * @return {@link Map}<{@link String}, {@link Map}<{@link Integer}, {@link String}>>
     * @FIXME 所有localizer都有记录，不需要在这里返回linesMap
     * @note violations记录二次仿真的所有违规信息,格式: {node, violation}
     */

    public Map<String, Map<Integer, String>> localizeErrorsFromViolations() {

        Map<String, Map<Integer, String>> errMap = new LinkedHashMap<>();

        if (_violations != null && _violations.size() > 0) {
            _violations.forEach((node, vio) -> {
              vio.getErrorLinesForSingleNode(node, _flow.getConfigPath(node), getConfiguration(node), _flow.getReqDstPrefix(), _bgpGenerator);
            });
            // 将每个violation的localizer的具体错误行放到返回的map里【peer要单独处理，可能peer上也有错误行】
            _violations.forEach((node, vio) -> {
                vio.getLocalizers().forEach(localizer -> {
                    if (!errMap.containsKey(node)) {
                        errMap.put(node, new HashMap<>());
                    }
                    errMap.get(node).putAll(ConfigurationLine.transToMap(localizer.getErrorLines()));
                });
            });
            return errMap.entrySet().stream().filter(m -> m.getValue() != null && m.getValue().size() > 0).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            return errMap;
        }
    }

    private <T> boolean ifListValid(List<T> list) {
        return list!=null && list.size()>0;
    }


    public void repairAll(boolean ifWriteBack, boolean ifPrint, String repairedConfigRootPath) {
        // REPAIR //
        if(_violations != null && _violations.size() > 0){
  //      long startTime = System.currentTimeMillis();
          BgpRepairerCollection bgpRepairerCollection = new BgpRepairerCollection();
          // 把所有的repair都提取出来
          Map<String, Set<Localizer>> curLocalizers = new HashMap<>();
          for (String node : _violations.keySet()) {
              // 这里的localizer是针对每个节点的
              Set<Localizer> localizers = _violations.get(node).getLocalizers();
              curLocalizers.put(node, localizers);
          }


          bgpRepairerCollection.transLocalizerToRepaier(curLocalizers);
          bgpRepairerCollection.finish(ifPrint);


          // 写入对应文件夹中，生成新配置文件
          // if (ifWriteBack) {
          //     bgpRepairerCollection.saveConfigChange(InputData.concatFilePath(repairedConfigRootPath, InputData.concatFilePath("repair-results", "bgp-result.txt")));
          //     bgpRepairerCollection.genRepairedConfiguration(oldConfigRootPath, repairedConfigRootPath);
          // }
        }

    }




    public void genIgpConstraints(BgpGenerator newGenerator, boolean ifSave) {}

    private void addLocalizerToViolation(String node, Localizer localizer) {
        if (localizer==null) {
            return;
        }
        if (!_violations.containsKey(node)) {
            _violations.put(node, new Violation());
        }
        _violations.get(node).addResults(localizer);
    }


    /**
     * 【针对IPRAN】 IPMETRO不用mpls, CLOUDNET不用MPLS LDP
     * find if all vpn binding interface have configured mpls ldp, also global mpls ldp
     * STEP 1: 全局使能
     * FIXME mpls lsr-id错配/漏配未实现
     * STEP 2: 接口使能【检查与其他设备接口相连的那些，layer2Topo上的edge】
     * FIXME 什么样的接口才需要检查隧道使能？（IGP路径上的？）
     */

}
