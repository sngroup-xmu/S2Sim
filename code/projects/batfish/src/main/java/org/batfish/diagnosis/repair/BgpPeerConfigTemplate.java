package org.batfish.diagnosis.repair;

import org.apache.commons.lang3.StringUtils;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.diagnosis.util.KeyWord;

import java.util.ArrayList;
import java.util.List;

public class BgpPeerConfigTemplate {
   // boolean变量默认为false
   String node;
   boolean peerEnabled;
   long peerAsNumber;
   String peerIp;
   boolean peerConnectInterface;
   String connectInterfaceName;
   int eBgpMaxHop;

   public enum PeerConfigLocationView{
       BGP_PROCESS,
       PEER_ADDRESS_FAMILY
   }
   /**
    * Builder for BgpPeerConfiguration
    */
   public static class Builder {
       private BgpPeerConfigTemplate buildObj = new BgpPeerConfigTemplate();

       public Builder(String node, String ip) {
           buildObj.node = node;
           buildObj.peerIp = ip;
       }
       /**
        * @param peerEnabled to be set
        * @return BgpPeerConfiguration.Builder
        */
       public Builder peerEnabled(boolean peerEnabled) {
           buildObj.peerEnabled = peerEnabled;
           return this;
       }

       /**
        * @param peerAsNumber to be set
        * @return BgpPeerConfiguration.Builder
        */
       public Builder peerAsNumber(long peerAsNumber) {
           buildObj.peerAsNumber = peerAsNumber;
           return this;
       }


       /**
        * @param peerConnectInterface to be set
        * @return BgpPeerConfiguration.Builder
        */
       public Builder peerConnectInterface(boolean peerConnectInterface) {
           buildObj.peerConnectInterface = peerConnectInterface;
           return this;
       }

       /**
        * @param eBgpMaxHop to be set
        * @return BgpPeerConfiguration.Builder
        */
       public Builder eBgpMaxHop(int eBgpMaxHop) {
           buildObj.eBgpMaxHop = eBgpMaxHop;
           return this;
       }

       public Builder connectInterfaceName(String name) {
           buildObj.peerConnectInterface = true;
           buildObj.connectInterfaceName = name;
           return this;
       }

       /**
        * @return BgpPeerConfiguration
        */
       public BgpPeerConfigTemplate build() {
           return buildObj;
       }
   }

   public long getPeerAsNum() {
       return peerAsNumber;
   }

   /*
    * 从原始的配置行里生成一个bgp peer config，解析相应关键字的赋值
    * 输入的原始配置需要保证【完整性】：即连带的group行也要输入
    * 输出的lines一定包含ip，否则就是【全部漏配】，完全漏配不会调用这个func
    */
   public static BgpPeerConfigTemplate genBgpPeerConfiguration(String node, String peerIp, List<String> configLines, BgpSessionProperties.SessionType sessionType, BgpPeerConnectType peerConnectType) {
       boolean peerEnabled = false;
       long peerAsNum = (Long) null;
       boolean peerConnectInterface = false;
       int ebgpMaxHop = (Integer) null;
       String connectInfName = null;

       for (String line: configLines) {
           if (line.contains("enable")) {
               peerEnabled = true;
           }
           if (line.contains("as-number")) {
               String[] words = line.split(" ");
               peerAsNum = Long.valueOf(words[words.length-1]);
           }
           if (line.contains("connect-interface")) {
               String[] words = line.split(" ");
               peerConnectInterface = true;
               connectInfName = words[words.length-1];
           }
           if (line.contains("ebgp-max-hop")) {
               String[] words = line.split(" ");
               ebgpMaxHop = Integer.valueOf(words[words.length-1]);
           }
       }


       switch (peerConnectType) {
           case NONDIRECT_CONNECT: {
               if (sessionType.toString().toLowerCase().contains("ebgp")) {
                   return new Builder(node, peerIp)
                                                   .peerAsNumber(peerAsNum)
                                                   .peerEnabled(peerEnabled)
                                                   .peerConnectInterface(peerConnectInterface)
                                                   .connectInterfaceName(connectInfName)
                                                   .eBgpMaxHop(ebgpMaxHop).build();
               } else {
                   return new Builder(node, peerIp)
                                                   .peerAsNumber(peerAsNum)
                                                   .peerEnabled(peerEnabled)
                                                   .peerConnectInterface(peerConnectInterface)
                                                   .connectInterfaceName(connectInfName).build();
               }
           }
           case DIRECT_CONNECT: {
               return new Builder(node, peerIp)
                                                   .peerAsNumber(peerAsNum)
                                                   .peerEnabled(peerEnabled).build();
           }
           default: return null;
       }
   }

   private boolean islegalAsNumber(long asNum) {
       if (StringUtils.isNotEmpty(String.valueOf(asNum))) {
           if (asNum > 0) {
               return true;
           }
       }
       return false;
   }
   /*
    * 把当前的peer实例输出成
    */
   public List<String> genConfigurationLines() {
       List<String> lines = new ArrayList<>();
       if (islegalAsNumber(peerAsNumber)) {
           lines.add("peer " + peerIp + " as-number " + peerAsNumber);
       }
       if (peerEnabled) {
           lines.add("peer " + peerIp + " enable");
       }
       if (peerConnectInterface) {
           lines.add("peer " + peerIp + " connect-interface " + connectInterfaceName);
       }
       if (eBgpMaxHop > 1) {
           lines.add("peer " + peerIp + " ebgp-max-hop " + eBgpMaxHop);
       }
       return lines;
   }

   /*
    * 输出某条peer配置所处的视图
    */
   public static PeerConfigLocationView getPeerConfigLocationView(String line) {
       if (line.contains(KeyWord.AS_NUMBER) || line.contains(KeyWord.CONNECTED_INTERFACE)) {
           return PeerConfigLocationView.BGP_PROCESS;
       }
       return PeerConfigLocationView.PEER_ADDRESS_FAMILY;
   }

   /*
    * 【当前实例是理想的peerConfiguration】
    * 输出 当前peerConfig 和 实际配置的差距【写入del和add lines】
    */


}
