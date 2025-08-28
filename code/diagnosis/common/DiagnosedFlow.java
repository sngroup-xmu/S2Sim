//package org.batfish.diagnosis.common;
//
//import java.io.File;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import org.batfish.common.topology.Layer2Edge;
//import org.batfish.datamodel.Prefix;
//
///**
// * Description of a flow (traffic), including these characters:
// * --------------------必选项 necessary【用户输入】-------------------------
// *  1) srcNode, 起点（数据包发送方）
// *  2) vpn, 起点vpn的名称
// *  3) dstNode, 终点（数据包要送达的节点）
// *  4) networkType, 组网类型
// *  5) caseType, 错误编号
// *  ------------非必选项 necessary【分析得到/已知特性的组网结构】---------------
// *  4) ifMpls
// *  5) mplsProtocol
// *  6) mplsArea
// *  ...
// *  EVPN, SRv6, ....
// */
//public class DiagnosedFlow {
//
//    String srcNode;
//    String dstNode;
//    String reqDstPrefixString; // specified in requirement
//    String cfgDstPrefixString; // searching from the configuration
//    NetworkType networkType;
//    String configRootPath;
//    Set<Layer2Edge> failedEdges;
//    // the forwarding path
//    List<String> existingFlowPath;
//    Map<String, String> configPathMap;
//
//    public String getSrcNode() {
//        return srcNode;
//    }
//
//    public String getDstNode() {
//        return dstNode;
//    }
//
//    public String getReqDstPrefixString() {
//        return reqDstPrefixString;
//    }
//    public Prefix getReqDstPrefix() {
//        return Prefix.parse(reqDstPrefixString);
//    }
//
//    public String getCfgDstPrefixString() {
//        return cfgDstPrefixString;
//    }
//
//    public NetworkType getNetworkType() {
//        return networkType;
//    }
//
//    public String getConfigRootPath() {
//        return configRootPath;
//    }
//
//    public Set<Layer2Edge> getFailedEdges() {
//        return failedEdges;
//    }
//
//    public List<String> getExistingFlowPath() {
//        return existingFlowPath;
//    }
//
//    public Map<String, String> getConfigPathMap() {
//        return configPathMap;
//    }
//
//    public String getConfigPath(String node) {
//        return configPathMap.get(node);
//    }
//
//    private boolean isValidArgument(String str) {
//        return str!=null && !str.equals("");
//    }
//
//    public boolean isValid() {
//        return isValidArgument(srcNode) && isValidArgument(dstNode) && isValidArgument(cfgDstPrefixString)
//                && isValidArgument(configRootPath);
//    }
//
//    public static final class Builder {
//        String srcNode;
//        String dstNode;
//        String reqDstPrefixString; // specified in requirement
//        String cfgDstPrefixString; // searching from the configuration
//        NetworkType networkType;
//        String configRootPath;
//        Set<Layer2Edge> failedEdges;
//        // the forwarding path
//        List<String> existingFlowPath;
//        Map<String, String> configPathMap;
//
//        public Builder() {
//        }
//
//        public Builder withSrcNode(String srcNode) {
//            this.srcNode = srcNode;
//            return this;
//        }
//
//
//        public Builder withReqDstPrefix(String dstPrefix) {
//            this.reqDstPrefixString = dstPrefix;
//            return this;
//        }
//
//        public Builder withDstDev(String dstNode) {
//            this.dstNode = dstNode;
//            return this;
//        }
//
//        public Builder withNetworkType(NetworkType networkType) {
//            this.networkType = networkType;
//            return this;
//        }
//
//        public Builder withCfgRootPath(String cfgRootPath) {
//            this.configRootPath = cfgRootPath;
//            return this;
//        }
//
//
//        public Builder withExistingFlowPath(List<String> path) {
//            this.existingFlowPath = path;
//            return this;
//        }
//
//
//        public DiagnosedFlow build() {
//            DiagnosedFlow flow = new DiagnosedFlow();
//            flow.dstNode = this.dstNode;
//            flow.configRootPath = this.configRootPath;
//            flow.srcNode = this.srcNode;
//            flow.networkType = this.networkType;
//            flow.reqDstPrefixString = this.reqDstPrefixString;
//            flow.cfgDstPrefixString = this.cfgDstPrefixString;
//            flow.existingFlowPath = this.existingFlowPath;
//            // ----------创建configurationFileMap-------------------
//            flow.configPathMap = new HashMap<>();
//            File rootFile = new File(configRootPath);
//            File[] files = rootFile.listFiles();
//            for (File file: files) {
//                String fileName = file.getName();
//                if (fileName.contains("cfg")) {
//                    String[] words = fileName.split("\\.");
//                    flow.configPathMap.put(words[0], file.getAbsolutePath());
//                }
//            }
//            // ------------------------------------------------
//            return flow;
//        }
//    }
//}
