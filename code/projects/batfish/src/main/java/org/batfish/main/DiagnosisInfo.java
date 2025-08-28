package org.batfish.main;

import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.batfish.common.NetworkSnapshot;
import org.batfish.common.topology.Layer2Edge;
import org.batfish.common.topology.Layer2Topology;
import org.batfish.common.topology.ValueEdge;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.BgpSessionProperties.SessionType;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.diagnosis.Diagnoser;
//import org.batfish.diagnosis.localization.Violation;
import org.batfish.diagnosis.util.ConfigTaint;
import org.batfish.diagnosis.util.KeyWord;

public class DiagnosisInfo {
  public static NetworkSnapshot curNetworkSnapshot;
  public static List<NetworkSnapshot> computedSnapshots = new ArrayList<>();
  public static Map<NetworkSnapshot, BgpTopology> bgpTopologyMap = new HashMap<>();
//  public static Map<String, Violation> violationMap = new HashMap<>();
  public static Map<String, Configuration> configurationMap = new HashMap<>();
  public static Diagnoser diagnoser;
  public static Layer2Topology _layer2Topology = null;
  public static List<Layer2Edge> _layer2Edges = new ArrayList<>();
  public static Map<String, Set<String>> _layer2Neighbors = new HashMap<>();
//  记录 peer condition 比较次数
  private int _peerCondCompareCont = 0;
//  记录 redistribute condition 比较次数
  private int _redisCondCompareCont = 0;
//  记录 route filter condition 比较次数
  private int _rpCondCompareCont = 0;
//  记录 prefer route condition 比较次数
  private int _preferCondCompareCont = 0;

  public static BgpTopology addBgpPeer(BgpTopology oldBgpTopology , String thisNode , String thisIp, String peer, String peerIp, Map<String,String> configPathMap){
    MutableValueGraph<BgpPeerConfigId, BgpSessionProperties> graph =
                      ValueGraphBuilder.directed().allowsSelfLoops(false).build();
    List<ValueEdge<BgpPeerConfigId, BgpSessionProperties>> edges = oldBgpTopology.getEdges();

    edges.forEach(
        valueEdge ->
            graph.putEdgeValue(
                valueEdge.getSource(), valueEdge.getTarget(), valueEdge.getValue()));

    //TODO：如果节点没有一个peer怎么办呢？？（当前未考虑这种情况）
    long tailAs = oldBgpTopology.getAsNumber(thisNode, peer);
    long headAs = oldBgpTopology.getAsNumber(peer, thisNode);
    if (tailAs == 0) {
      tailAs = ConfigTaint.getAsNumber(configPathMap.get(thisNode));
    }
    if (headAs == 0) {
      headAs = ConfigTaint.getAsNumber(configPathMap.get(peer));
    }
    //System.out.println("HeadAs:" + HeadAs + "TailAs:" + TailAs);
    BgpPeerConfigId thisConfigId =
        new BgpPeerConfigId(
            thisNode,
            KeyWord.PUBLIC_VPN_NAME,
            Prefix.create(Ip.parse(peerIp),32),false);
    BgpPeerConfigId peerConfigId =
        new BgpPeerConfigId(
            peer,
            KeyWord.PUBLIC_VPN_NAME,
            Prefix.create(Ip.parse(thisIp),32),false);

    BgpSessionProperties.Builder edge_from = BgpSessionProperties.builder()
        .setHeadAs(headAs)
        .setTailAs(tailAs)
        .setHeadIp(Ip.parse(thisIp))
        .setTailIp(Ip.parse(peerIp))
        .setSessionType(headAs == tailAs ? SessionType.IBGP : SessionType.EBGP_MULTIHOP);

    BgpSessionProperties.Builder edge_to = BgpSessionProperties.builder()
        .setHeadAs(tailAs)
        .setTailAs(headAs)
        .setHeadIp(Ip.parse(peerIp))
        .setTailIp(Ip.parse(thisIp))
        .setSessionType(headAs == tailAs ? SessionType.IBGP : SessionType.EBGP_MULTIHOP);
    //两个方向都要添加
    graph.putEdgeValue(thisConfigId, peerConfigId, edge_from.build());
    graph.putEdgeValue(peerConfigId, thisConfigId, edge_to.build());
    return new BgpTopology(graph);
  }

  public static void setDiagnoser(Diagnoser d) {
    diagnoser = d;
  }
  public static Diagnoser getDiagnoser() {
    return diagnoser;
  }

  public void setPeerCondCompareCont(int peerCondCompareCont) {
    _peerCondCompareCont = peerCondCompareCont;
  }
  public int getPeerCondCompareCont() {
    return _peerCondCompareCont;
  }

  public void setRedisCondCompareCont(int redisCondCompareCont) {
    _redisCondCompareCont = redisCondCompareCont;
  }
  public int getRedisCondCompareCont() {
    return _redisCondCompareCont;
  }

  public void setRpCondCompareCont(int rpCondCompareCont) {
    _rpCondCompareCont = rpCondCompareCont;
  }
  public int getRpCondCompareCont() {
    return _rpCondCompareCont;
  }

  public void setPreferCondCompareCont(int preferCondCompareCont){
    _peerCondCompareCont = preferCondCompareCont;
  }
  public int getPreferCondCompareCont() {
    return _preferCondCompareCont;
  }

}
