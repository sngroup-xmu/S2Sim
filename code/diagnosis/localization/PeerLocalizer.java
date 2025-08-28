//package org.batfish.diagnosis.localization;
//
//
//import org.batfish.datamodel.BgpPeerConfig;
//import org.batfish.diagnosis.common.ConfigurationLine;
//import org.batfish.diagnosis.repair.Repairer;
//import org.batfish.diagnosis.util.ConfigTaint;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.batfish.diagnosis.repair.BgpPeerConnectType.NONDIRECT_CONNECT;
//import static org.batfish.diagnosis.util.ConfigTaint.transPrefixOrIpToIpString;
//
///*
// * Localize "violateIbgpPeer"/"violateEbgpPeer" errors
// * 注意：【暂时不能处理，某节点上一个bgpPeer都没有配置对过，因为这样bgpTopo解析的时候根本没有遇到过这样的一个node】
// */
//public class PeerLocalizer extends Localizer {
//    private String localNode;
//    private String remoteNode;
//    private String localIp;
//    private String remoteIp;
//    private BgpPeerConfig localPeer;
//    private BgpPeer remotePeer;
//
//    public String getLocalCfgFilePath() {
//        return localCfgFilePath;
//    }
//
//    public String getRemoteCfgFilePath() {
//        return remoteCfgFilePath;
//    }
//
//    private String localCfgFilePath;
//    private String remoteCfgFilePath;
//    private Generator generator;
//    private BgpTopology refBgpTopology;
//    private Violation violation;
//    // 表示创建的时候是通过其他节点的violation创建的，所以这里定位出的error lines要写入全局变量
//    // private boolean initializeFromOtherNode;
//    private Map<Integer, String> peerErrorLines;
//
//    public Violation getViolation() {
//        return violation;
//    }
//
//    /*
//     * 获取local node与当前 peer/其他 peer配置用到的peer连接种类：如果localPeer因为没配好而为null则查看local node其他peer的类型
//     */
//    public PeerConnectType getDefaultPeerConnectType() {
//        if (localPeer==null) {
//            if (refBgpTopology!=null) {
//                return refBgpTopology.getArbitraryValidPeer(localNode).getPeerConnectType();
//            } else {
//                return generator.getBgpTopology().getArbitraryValidPeer(localNode).getPeerConnectType();
//            }
//        } else {
//            return localPeer.getPeerConnectType();
//        }
//    }
//
//    public Generator getGenerator() {
//        return generator;
//    }
//
//    public Map<Integer, String> getPeerErrorLines() {
//        return peerErrorLines;
//    }
//
//    public enum PeerErrorType {
//        PEER_IP_REACH_LOCAL,
//        PEER_IP_REACH_REMOTE,
//
//        ACL_FILTER_TCP_PORT,
//
//        PEER_AS_NUMBER_INCONSISTENT_LOCAL,
//        PEER_AS_NUMBER_INCONSISTENT_REMOTE,
//
//        PEER_IP_INCONSISTENT_LOCAL,
//        PEER_IP_INCONSISTENT_REMOTE,
//
//        EBGP_MAX_HOP_LOCAL,
//        EBGP_MAX_HOP_REMOTE,
//
//        PEER_IGNORE_LOCAL,
//        PEER_IGNORE_REMOTE,
//
//        PEER_CONNECT_INTERFACE_LOCAL,
//        PEER_CONNECT_INTERFACE_REMOTE,
//
//        PEER_NOT_CONFIGURED_LOCAL,
//        PEER_NOT_CONFIGURED_REMOTE,
//
//        UNKOWN_LOCAL,
//        UNKOWN_REMOTE;
//    }
//
//    public String getPeerLoopbackIp() {
//        if (generator.getBgpTopology().getNodeIp(remoteNode) != null) {
//            return generator.getBgpTopology().getNodeIp(remoteNode);
//        } else if (refBgpTopology != null) {
//            if (refBgpTopology.getNodeIp(remoteNode) != null) {
//                return refBgpTopology.getNodeIp(remoteNode);
//            }
//        }
//        return generator.getLayer2Topology().getNodeInterfaceFromName(remoteNode, "LoopBack0").getInfIpv4HostIpString();
//    }
//
//    public PeerRepairer genPeerRepairer() {
//        // String peerIp;
//        // String localIp;
//        BgpPeerType peerType;
//        // if (localPeer==null || remotePeer==null) {
//        // // 如果原始的BgpPeerInfo文件没有提供有用信息，那么都默认双方要用各自的loopback0口连
//        // // @TODO: 考虑单边错漏的直连peer（如果双边漏配，则还是用loopback0）
//        // peerIp = getPeerLoopbackIp();
//        // } else {
//        // // 走到这一步说明是配了peer，但是eBGP跳数不对【针对华为，暂时不确定还有其他什么例子】
//        // peerIp = localPeer.getPeerIpString();
//        // }
//        // // 先获取一个ref的BgpPeer/BgpPeerConfiguration
//        // if (!generator.getLayer2Topology().getIpLocatedInterface(remoteNode,
//        // peerIp).getInfName().equals("LoopBack0")) {
//        // // @TODO: 处理直连peer的情况
//        // assert false;
//        // }
//        // localIp = generator.getBgpTopology().getNodeIp(localNode);
//
//        if (generator.getBgpTopology().getAsNumber(localNode).equals(generator.getBgpTopology().getAsNumber(remoteNode))) {
//            peerType = BgpPeerType.IBGP;
//        } else {
//            peerType = BgpPeerType.EBGP;
//        }
//        return new PeerRepairer(localNode, localIp, remoteNode, remoteIp, peerType,
//                NONDIRECT_CONNECT, this);
//    }
//
//    // 一定要在BgpPeer初始化后再调用，目的是确定peerIp和localIp
//    // @TODO: 没有lp0，应该找直连ip
//    public void assignPeerIp() {
//        if (localPeer != null) {
//            localIp = localPeer.getLocalIpString();
//            remoteIp = localPeer.getPeerIpString();
//        } else {
//            Layer2Topology layer2Topology = generator.getLayer2Topology();
//            if (generator.getFlow().isIpv6Peer()) {
//                localIp = layer2Topology.getNodeInterfaceFromName(localNode, KeyWord.LOOPBACK0).getInfIpv6IpString();
//                remoteIp = layer2Topology.getNodeInterfaceFromName(remoteNode, KeyWord.LOOPBACK0).getInfIpv6IpString();
//            } else {
//                localIp = layer2Topology.getNodeInterfaceFromName(localNode, KeyWord.LOOPBACK0).getInfIpv4HostIpString();
//                remoteIp = layer2Topology.getNodeInterfaceFromName(remoteNode, KeyWord.LOOPBACK0).getInfIpv4HostIpString();
//            }
//        }
//    }
//
//    public PeerLocalizer(String node1, String node2, Generator generator, Violation violation,
//            BgpTopology refBgpTopology) {
//        this.localNode = node1;
//        this.remoteNode = node2;
//        this.localCfgFilePath = generator.getFlow().getConfigPath(node1);
//        this.remoteCfgFilePath = generator.getFlow().getConfigPath(node2);
//        this.generator = generator;
//        // 这边应该把refBgpTopoplogy上的peer信息写下来
//        this.localPeer = generator.getBgpTopology().getBgpPeer(node1, node2);
//        this.remotePeer = generator.getBgpTopology().getBgpPeer(node2, node1);
//        this.violation = violation;
//        this.refBgpTopology = refBgpTopology;
//        assignPeerIp();
//    }
//
//    public BgpTopology getRefBgpTopology() {
//        return refBgpTopology;
//    }
//
//    public List<PeerErrorType> getErrorTypes() {
//        List<PeerErrorType> errList = new ArrayList<PeerErrorType>();
//        // 逐项排查
//        if (localPeer == null && remotePeer == null) {
//            errList.add(PeerErrorType.PEER_NOT_CONFIGURED_LOCAL);
//            errList.add(PeerErrorType.PEER_NOT_CONFIGURED_LOCAL);
//            return errList;
//        } else if (localPeer != null && remotePeer != null) {
//            // 两边都配过peer, 是不一致的问题
//            if (!localPeer.isConsistent(remotePeer)) {
//                // ip或者as-num不一致, 至少有一个错了, 顺着诊断一遍
//                if (!localPeer.getLocalIpString().equals(remotePeer.getPeerIpString())) {
//                    errList.add(PeerErrorType.PEER_IP_INCONSISTENT_LOCAL);
//                }
//                if (!remotePeer.getLocalIpString().equals(localPeer.getPeerIpString())) {
//                    errList.add(PeerErrorType.PEER_IP_INCONSISTENT_REMOTE);
//                }
//                if (localPeer.getLocalAsNum() != remotePeer.getPeerAsNum()) {
//                    errList.add(PeerErrorType.PEER_AS_NUMBER_INCONSISTENT_REMOTE);
//                }
//                if (remotePeer.getLocalAsNum() != localPeer.getPeerAsNum()) {
//                    errList.add(PeerErrorType.PEER_AS_NUMBER_INCONSISTENT_LOCAL);
//                }
//            } else {
//                // local和remote节点逐个排查【codes need improving】
//                // localNode
//                boolean isLocalConnectInterface = isConnectInterface(localNode);
//                boolean isLocalIgnorePeer = isIgnorePeer(localNode);
//                if (!isLocalConnectInterface) {
//                    errList.add(PeerErrorType.PEER_CONNECT_INTERFACE_LOCAL);
//                }
//                if (isLocalIgnorePeer) {
//                    errList.add(PeerErrorType.PEER_IGNORE_LOCAL);
//                }
//
//                if (localPeer.getBgpPeerType() == BgpPeerType.EBGP) {
//                    int atLeastHop = generator.hopNumberToReachIpUsingStatic(localNode, localPeer.getPeerIpString());
//                    if (atLeastHop == 0) {
//                        errList.add(PeerErrorType.PEER_IP_REACH_LOCAL);
//                    } else if (atLeastHop > localPeer.getEBgpMaxHop()) {
//                        errList.add(PeerErrorType.EBGP_MAX_HOP_LOCAL);
//                    }
//                } else {
//                    errList.add(PeerErrorType.PEER_IP_REACH_LOCAL);
//                }
//
//                // remoteNode
//                boolean isRemoteConnectInterface = isConnectInterface(remoteNode);
//                boolean isRemoteIgnorePeer = isIgnorePeer(remoteNode);
//                if (!isRemoteConnectInterface) {
//                    errList.add(PeerErrorType.PEER_CONNECT_INTERFACE_REMOTE);
//                }
//                if (isRemoteIgnorePeer) {
//                    errList.add(PeerErrorType.PEER_IGNORE_REMOTE);
//                }
//
//                if (remotePeer.getBgpPeerType() == BgpPeerType.EBGP) {
//                    int atLeastHop = generator.hopNumberToReachIpUsingStatic(remoteNode, remotePeer.getPeerIpString());
//                    if (atLeastHop == 0) {
//                        errList.add(PeerErrorType.PEER_IP_REACH_REMOTE);
//                    } else if (atLeastHop > remotePeer.getEBgpMaxHop()) {
//                        errList.add(PeerErrorType.EBGP_MAX_HOP_REMOTE);
//                    }
//                } else {
//                    errList.add(PeerErrorType.PEER_IP_REACH_REMOTE);
//                }
//
//            }
//        } else if (localPeer != null) {
//            errList.add(PeerErrorType.PEER_NOT_CONFIGURED_REMOTE);
//        } else if (remotePeer != null) {
//            errList.add(PeerErrorType.PEER_NOT_CONFIGURED_LOCAL);
//        }
//        return errList;
//    }
//
//    private void addPeerErrorLines(Map<Integer, String> lines) {
//        if (peerErrorLines == null) {
//            peerErrorLines = new HashMap<>();
//        }
//        peerErrorLines.putAll(lines);
//    }
//
//    @Override
//    // 只查自身localNode可改的错
//    public List<ConfigurationLine> genErrorConfigLines() {
//        List<PeerErrorType> errorTypes = getErrorTypes();
//
//        String[] localKeyWords = { "peer", remoteIp };
//        addErrorLines(ConfigTaint.peerTaint(localNode, localKeyWords, localCfgFilePath));
//
//        String[] peerKeyWords = { "peer", localIp };
//        addPeerErrorLines(ConfigTaint.peerTaint(remoteNode, peerKeyWords, remoteCfgFilePath));
//
//        // for (PeerErrorType err : errorTypes) {
//        // switch (err) {
//        // case PEER_AS_NUMBER_INCONSISTENT_LOCAL: {
//        // String[] keyWords = { "peer", localPeer.getPeerIpString().toString(),
//        // String.valueOf(localPeer.getPeerAsNum()) };
//
//        // if (!initializeFromOtherNode) {
//        // addErrorLines(ConfigTaint.peerTaint(localNode, keyWords));
//        // } else {
//        // if (BgpDiagnosis.errMap.containsKey(localNode)) {
//        // BgpDiagnosis.errMap.get(localNode).putAll(ConfigTaint.peerTaint(localNode,
//        // keyWords));
//        // } else {
//        // BgpDiagnosis.errMap.put(localNode, ConfigTaint.peerTaint(localNode,
//        // keyWords));
//        // }
//        // }
//
//        // break;
//        // }
//        // // case PEER_AS_NUMBER_INCONSISTENT_REMOTE: {
//        // // String[] keyWords = {"peer", remotePeer.getPeerIpString().toString(),
//        // // String.valueOf(remotePeer.getPeerAsNum())};
//        // // lines.putAll(ConfigTaint.taint(remoteNode, keyWords));
//        // // break;
//        // // }
//        // case PEER_CONNECT_INTERFACE_LOCAL: {
//        // // 这个错误默认缺失对应语句
//        // String line = "peer " + localPeer.getPeerIpString().toString() + "
//        // connect-interface "
//        // + localPeer.getLocalIpString().toString();
//        // addErrorLine(violation.getMissingLine(), line);
//        // break;
//        // }
//        // // case PEER_CONNECT_INTERFACE_REMOTE: {
//        // // String line = "peer " + remotePeer.getPeerIpString().toString() + "
//        // // connect-interface " + remotePeer.getLocalIpString().toString();
//        // // lines.put(violation.getMissingLine(), line);
//        // // break;
//        // // }
//        // case PEER_IGNORE_LOCAL: {
//        // // 这个错误默认多写了
//        // }
//        // case PEER_IGNORE_REMOTE:
//
//        // case PEER_IP_INCONSISTENT_LOCAL:
//        // case PEER_IP_REACH_LOCAL: {
//        // // IP不一致 就把所有 peer *ip* 有关的语句都找出来
//        // String[] keyWords = { "peer", localPeer.getPeerIpString().toString() };
//        // addErrorLines(ConfigTaint.taint(localNode, keyWords, null));
//        // break;
//        // }
//        // // case PEER_IP_INCONSISTENT_REMOTE:
//        // // case PEER_IP_REACH_REMOTE: {
//        // // String[] keyWords = {"peer", remotePeer.getPeerIpString().toString()};
//        // // lines.putAll(ConfigTaint.taint(remoteNode, keyWords));
//        // // break;
//        // // }
//
//        // case PEER_NOT_CONFIGURED_LOCAL: {
//        // String asNumber = "as-number";
//        // if (refBgpTopology != null) {
//        // asNumber = Long.toString(refBgpTopology.getAsNumber(remoteNode));
//        // }
//        // // 因为有的peer只配了单边，但是bgp topo上两端的peer info都会缺失，所以要在配置里再检查一遍
//        // Map<Integer, String> peerConfig = getPeerConfiguration(localNode, remoteNode,
//        // peerIp);
//        // if (peerConfig.size() == 0) {
//        // List<String> missingLines = ConfigTaint.genMissingPeerConfigLines(localIp,
//        // peerIp, asNumber);
//        // if (initializeFromOtherNode) {
//        // missingLines.forEach(line -> peerConfig.put(violation.getMissingLine(),
//        // line));
//        // peerErrorLines.putAll(peerConfig);
//        // } else {
//        // missingLines.forEach(line -> addErrorLine(violation.getMissingLine(), line));
//        // }
//
//        // } else {
//        // if (initializeFromOtherNode) {
//        // putErrorLinesToGlobalErrorMap(localNode, peerConfig);
//        // } else {
//        // addErrorLines(peerConfig);
//        // }
//
//        // }
//
//        // break;
//        // }
//        // // case PEER_NOT_CONFIGURED_REMOTE: {
//        // // String line1 = "peer " + remotePeer.getPeerIpString().toString() + "
//        // enable";
//        // // lines.put(violation.getMissingLine(), line1);
//        // // String line2 = "peer " + remotePeer.getPeerIpString().toString() + "
//        // // connect-interface " + remotePeer.getLocalIpString().toString();
//        // // lines.put(violation.getMissingLine(), line2);
//        // // break;
//        // // }
//        // case EBGP_MAX_HOP_LOCAL: {
//        // int realHop = generator.hopNumberToReachIpUsingStatic(localNode,
//        // localPeer.getPeerIpString());
//        // String line = "peer " + localPeer.getPeerIpString().toString() + "
//        // ebgp-max-hop "
//        // + String.valueOf(realHop);
//        // String[] keyWords = { "peer", localPeer.getPeerIpString().toString(),
//        // "ebgp-max-hop" };
//        // addErrorLines(ConfigTaint.peerTaint(localNode, keyWords));
//        // addErrorLine(violation.getMissingLine(), line);
//        // break;
//        // }
//        // // case EBGP_MAX_HOP_REMOTE: {
//        // // int realHop = generator.hopNumberToReachIpUsingStatic(remoteNode,
//        // // remotePeer.getPeerIpString());
//        // // String line = "peer " + remotePeer.getPeerIpString().toString() + "
//        // // ebgp-max-hop " + String.valueOf(realHop);
//        // // String[] keyWords = {"peer", remotePeer.getPeerIpString().toString(),
//        // // "ebgp-max-hop"};
//        // // lines.putAll(ConfigTaint.taint(remoteNode, keyWords));
//        // // lines.put(violation.getMissingLine(), line);
//        // // break;
//        // // }
//        // case UNKOWN_LOCAL: {
//        // String[] keyWords = { "peer", localPeer.getPeerIpString().toString() };
//        // addErrorLines(ConfigTaint.peerTaint(localNode, keyWords));
//        // break;
//        // }
//        // // case UNKOWN_REMOTE: {
//        // // String[] keyWords = {"peer", remotePeer.getPeerIpString().toString()};
//        // // lines.putAll(ConfigTaint.taint(remoteNode, keyWords));
//        // // break;
//        // // }
//        // }
//        // }
//
//        return getErrorLines();
//    }
//
//    public Map<Integer, String> getPeerConfiguration(String localDev, String peerDev, String peerIpString) {
//        String[] peerWords = { "peer", transPrefixOrIpToIpString(peerIpString) };
//        String[] enableWords = { "enable", "connect" };
//        return ConfigTaint.peerTaint(localDev, peerWords, localCfgFilePath);
//    }
//
//    private boolean isConnectInterface(String node) {
//        // node上对 对端的peer配置 是否有connect-interface命令
//        if (node.equals(localNode)) {
//            String[] keyWords = { "peer", localPeer.getPeerIpString().toString(), "connect-interface" };
//            return ConfigTaint.taint(node, keyWords, localCfgFilePath).keySet().size() > 0;
//        } else if (node.equals(remoteNode)) {
//            String[] keyWords = { "peer", remotePeer.getPeerIpString().toString(), "connect-interface" };
//            return ConfigTaint.taint(node, keyWords, remoteCfgFilePath).keySet().size() > 0;
//        } else {
//            return false;
//        }
//
//    }
//
//    private boolean isIgnorePeer(String node) {
//        // node上对 对端的peer配置 是否有peer ignore命令
//        String[] keyWords = { "peer", localPeer.getPeerIpString().toString(), "ignore" };
//        if (ConfigTaint.taint(node, keyWords, localCfgFilePath).keySet().size() > 0) {
//            return true;
//        }
//        return false;
//    }
//
//    public String getLocalNode() {
//        return this.localNode;
//    }
//
//    public void setLocalNode(String localNode) {
//        this.localNode = localNode;
//    }
//
//    public String getRemoteNode() {
//        return this.remoteNode;
//    }
//
//    public void setRemoteNode(String remoteNode) {
//        this.remoteNode = remoteNode;
//    }
//
//    public BgpPeer getLocalPeer() {
//        return this.localPeer;
//    }
//
//    public void setLocalPeer(BgpPeer localPeer) {
//        this.localPeer = localPeer;
//    }
//
//    public BgpPeer getRemotePeer() {
//        return this.remotePeer;
//    }
//
//    public void setRemotePeer(BgpPeer remotePeer) {
//        this.remotePeer = remotePeer;
//    }
//
//    @Override
//    public Repairer genRepairer() {
//        // TODO Auto-generated method stub
//        return null;
//    }
//
//}
