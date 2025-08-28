package org.batfish.diagnosis.reference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import org.batfish.common.topology.Layer2Topology;
import org.batfish.datamodel.DataPlane;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.diagnosis.common.DiagnosedFlow;
import org.batfish.diagnosis.util.KeyWord;

/*
* maintain the all overlay info (BGP and Static, maybe OSPF in the future) of the network
* Suppose that the BgpTopology is connected before using the generator,
* We will diagnose the BgpPeer connection first
*/

/**
* Maintain the all reference control plane symbols, now we support 3 types of network:
*  1) BGP network
*     - using eBGP to connect each other
*     - disseminating Static/Connected routes
*  2) BGP + OSPF network
*     - using iBGP/eBGP to connect each other
*     - iBGP peer Ips (Loopback0 interface) rely on OSPF
*     - disseminating Static/Connected routes
*  3) OSPF network
*     - basic OSPF configuration: router-id [], network [] area [], ip cost []
*/
public class BgpGenerator {

   public DiagnosedFlow getFlow() {
       return _flow;
   }

   private DiagnosedFlow _flow;

   private BgpForwardingTree _bgpForwardingTree;
   private StaticForwardingTree _staticForwardingTree;

   // 对于错误bgpTree的generator，这里的bgp topo就是错误的那个（但对new generator，初始化的时候还是用的错误的bgp topo）
   private BgpTopology _bgpTopology;
   private Layer2Topology layer2Topology;


   public BgpGenerator(DiagnosedFlow flow,
                       BgpTopology bgpTopology) {
       this._flow = flow;
       _bgpTopology = bgpTopology;
   }


   public BgpTopology getBgpTopology() {
       return _bgpTopology;
   }

   public BgpForwardingTree getBgpTree() {
       return _bgpForwardingTree;
   }

   public StaticForwardingTree getStaticTree() { return _staticForwardingTree; }

   private <T> boolean ifTwoPathOverlap (List<T> p1, List<T> p2) {
       for (T node : p1) {
           if (p2.contains(node)) {
               return true;
           }
       }
       return false;
   }

   public void initializeBgpForwardingTree(DataPlane dataPlane) {
       _bgpForwardingTree = new BgpForwardingTree(_flow, _bgpTopology);
       if (dataPlane!=null) {
           _bgpForwardingTree.initialize(dataPlane);  //此处为空操作
       }
   }

   private void printStringList(List<String> list, String title, String seperator) {
       System.out.println(KeyWord.PRINT_LINE_HALF + title + KeyWord.PRINT_LINE_HALF);
       for (String string : list) {
           System.out.print(string + seperator);
       }
       System.out.println();
   }


   private List<String> copyPath(List<String> path, int fromIndex, int toIndex) {
       List<String> newPath = new ArrayList<>();
       assert toIndex>fromIndex && path.size()>=toIndex && fromIndex>=0;
       for (int i=fromIndex; i<toIndex; i+=1) {
           newPath.add(path.get(i));
       }
       return newPath;
   }

   // 输入是spec中要求可达的节点，如果某个节点X在AS i中，则需要将AS i中其他iBGP节点也加入需要可达的nodes集合
   // 如果使用隧道，则不需要AS内 full mesh
   // 返回的节点集合就是要在MST加入的节点
   // TODO:
   private Set<String> processReachNodes(Set<String> nodes) {
       nodes.remove(_flow.getDstNode());
       return nodes;
   }

    /*
     * 这一步只生成BGP Tree里的路由传播和优选的树（实际转发时，域内路径可能会有出入）
     * 这里的出入源自于域内传播的BGP路由下一跳不一定永远是接收eBGP路由的入节点
     * 【域内(AS内)可能会通过不止一个IGP域相连，所以反射时下一跳可能会多次改变】 1）跨IGP域的节点可能改下一跳再传 :o
     * 2）跨IGP域的节点终止目标路由（明细路由）传播，发送默认路由至上游节点 :(
     * ---------------------------------------------------------------------------
     * 现有的错误的tree必须是连通的 TODO: 现有的【根据prov信息生成的】tree不连通？会有这种情况出现么？不会吧？有的话是什么情况呢 TODO:
     * 这里生成的BGP树是针对可达性要求的（最小生成树算法【如果有reference参考，可以考虑改进选下一个“最小边”的指标】）
     * 如果有waypoint/bypass要求，需要先改动原有的错误的树（还没实现），但这里要分情况考虑：
     * 1）如果是要经过/绕开AS内部(intra)某些节点：a) 非边界节点：需要IGP支持，b）边界节点：BGP策略
     * 2）如果要经过/绕开某些AS(inter)：则此时路由传播和优选的路径就和转发的路径一致（AS-level）
     * ---------------------------------------------------------------------------
     * PS: 现在暂时都不考虑ACL这种数据面的策略
     */

   public BgpForwardingTree genBgpTreeWithoutFailure(@Nullable BgpForwardingTree refTree) {
       // error oldTree 所在的generator调用，oldBGPTree已经在generator里
       // 目标src节点是当前generator里的unreachable nodes
       // 在现有error oldBGPTree上继续生成minTree【针对路由传播的tree: BestRouteFrom】

       // 假设bgpTopo是连通的，即不需要bgpTopology的参考来构造转发树

        Set<String> reqReachNodes = new HashSet<>();
        reqReachNodes.addAll(_flow.getSrcNode());

        Set<String> reachableNodes = new HashSet<>(_bgpForwardingTree.getReachableNodes());
        reqReachNodes.removeAll(reachableNodes);
        Set<String> unreachableNodes = _bgpForwardingTree.getUnreachableNodes();
        unreachableNodes.removeAll(reachableNodes);
        // 考虑fail节点

        Map<String, Integer> distanceMap = new HashMap<>();
        Map<String, String> primNearestNodeMap = new HashMap<>(
                _bgpForwardingTree.getBestRouteFromMap());
        // disMap initialization
        _bgpTopology.getGraph().nodes()
                .forEach(node -> distanceMap.put(node.getHostname(), Integer.MAX_VALUE));
        // TODO: 如果没有serialize到BGPTree时，没有节点和bgp
        // ip的映射，这里会出现bestRouteFrom和nextHopForwarding不一致问题：nextHop有devName，但是bestRouteFrom没有devName
        reachableNodes.forEach(node -> distanceMap.put(node,
                _bgpForwardingTree.getBestRouteFromPath(node).size() - 1));
        // dstNode init
        distanceMap.put(_flow.getDstNode(), 0);

        // 用ref的连接信息参考作为Prim的加入节点选择（MST不止一个时）
        String curNode = _bgpForwardingTree.chooseFirstNodeHasUnreachablePeer(_bgpTopology); // 选一个已reach的开始

     // 把已有的reach节点的peer遍历更新一遍disMap【重要，不然会出现有点节点拓展失败的情况】
     // reachableNodes是变动的
     for (String reachedNode: reachableNodes) {
         for (String peer: _bgpTopology.getPeers(reachedNode)) {
             if (distanceMap.get(reachedNode) + 1 < distanceMap.get(peer)) {
                 distanceMap.put(peer, distanceMap.get(reachedNode) + 1);
                 primNearestNodeMap.put(peer, reachedNode);
             } else if (distanceMap.get(reachedNode) + 1 == distanceMap.get(peer)) {
                 if (refTree!=null) {
                     // 参考正确流的信息
                     if (refTree.getBestRouteFromNode(peer).equals(reachedNode)) {
                         primNearestNodeMap.put(peer, reachedNode);
                     }
                 }
             }
         }
     }
     // 开始在当前MST上加节点
     while (reqReachNodes.size()>0) {
       for (String peer : _bgpTopology.getPeers(curNode)) {
         //                if (reachableNodes.contains(peer)) {
         //                    // 更新disMap时只考虑spec要求的节点集合
         //                    continue;
         //                }
//         System.out.println("*****************");
//         System.out.println(curNode);
//         System.out.println(peer);
         if (distanceMap.get(curNode) + 1 < distanceMap.get(peer)) {
           distanceMap.put(peer, distanceMap.get(curNode) + 1);
           primNearestNodeMap.put(peer, curNode);
         } else if (distanceMap.get(curNode) + 1 == distanceMap.get(peer)) {
           if (refTree != null) {
             // 参考正确流的信息
             if (refTree.getBestRouteFromNode(peer).equals(curNode)) {
               primNearestNodeMap.put(peer, curNode);
             }
           }
         }
       }
       // 选下一个进入树的节点
       // TODO: 根据refTree选？
       String nextNode = unreachableNodes.iterator().next();
       for (String unreachNode : unreachableNodes) {
         if(_flow.getExistingFlowPath() != null && _flow.getExistingFlowPath().contains(nextNode)) {
           //实现按照waypoint进行选路
           //Step1:如果req有waypoint要求，获取当前节点到dst的路径
           List<String> bestRouteFromNode = new ArrayList<>();
           if (primNearestNodeMap.get(nextNode) != null) {
             bestRouteFromNode = _bgpForwardingTree.getBestRouteFromPath(primNearestNodeMap.get(
                 nextNode));
           }
           //Step2：当前找到一条dst到src的路径，但是这条路径中不包含waypoint节点
           List<String> subList = _flow.getExistingFlowPath().subList(_flow.getExistingFlowPath().indexOf(nextNode)+1, _flow.getExistingFlowPath().size());
           if (!bestRouteFromNode.containsAll(subList)) {
             //把src节点的distance改成MAX，在后续的节点中重新找path
             distanceMap.put(nextNode, Integer.MAX_VALUE);
           }
         }
         if (distanceMap.get(nextNode) > distanceMap.get(unreachNode)) {
           nextNode = unreachNode;
         }
         //如果有waypoint属性，那么优先选择waypoint节点开始拓展节点
         else if(distanceMap.get(nextNode) == distanceMap.get(unreachNode)
             && _flow.getExistingFlowPath() != null
             && _flow.getExistingFlowPath().contains(unreachNode)){
           nextNode = unreachNode;
         }
       }

       curNode = nextNode;
       unreachableNodes.remove(curNode);
       reqReachNodes.remove(curNode);
       reachableNodes.add(curNode);
       _bgpForwardingTree.addBestRouteFromEdge(curNode, primNearestNodeMap.get(curNode));
       printStringList(_bgpForwardingTree.getBestRouteFromPath(curNode), "NEW-PATH", ", ");
     }

     return _bgpForwardingTree;
   }

   private String getNextNodeAddToMST(Set<String> nodes, Map<String, Integer> distanceMap) {
       if (nodes.size()<1) {
           return null;
       } else if (nodes.size()==1) {
           return nodes.iterator().next();
       } else {
           String node = nodes.iterator().next();
           int dis = distanceMap.get(node);
           for (String n: nodes) {
               if (distanceMap.get(n) < dis) {
                   dis = distanceMap.get(n);
                   node = n;
               }
           }
           return node;
       }
   }

  public BgpForwardingTree genBgpTree(BgpForwardingTree refTree) {
    if (_flow.getFailureNumber()>0) {
      return genBgpTreeWithFailure();
    } else {
      return genBgpTreeWithoutFailure(refTree);
    }
  }


  public BgpForwardingTree genBgpTreeWithFailure() {
    int k = _flow.getFailureNumber();
    String destination = _flow.getDstNode();
    for (String source: _flow.getSrcNode()) {
      // find K path的函数输出的路径，起始点是反着的，所以这里也反着输入
      List<List<String>> paths = ShortestPath.findKEdgeDisjointPaths(destination, source, _bgpTopology.getBgpPeerConnectionMap(), k+1);
      _bgpForwardingTree.addBestRoutesFromPath(source, paths);
      for (List<String> path: paths) {
        for (int i=0; i<path.size()-1; i++) {
          _bgpForwardingTree.addBestRouteFromEdge(path.get(i), path.get(i+1));
        }
      }
    }
    return _bgpForwardingTree;
  }


  public Layer2Topology getLayer2Topology() {
    // TODO Auto-generated method stub
    return this.layer2Topology;
  }


}
