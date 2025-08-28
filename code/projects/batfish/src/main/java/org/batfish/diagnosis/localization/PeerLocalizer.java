package org.batfish.diagnosis.localization;

import static org.batfish.diagnosis.util.ConfigTaint.getInterfaceLine;
import static org.batfish.diagnosis.util.ConfigTaint.transPrefixOrIpToIpString;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.diagnosis.common.ConfigurationLine;
import org.batfish.diagnosis.reference.BgpGenerator;
import org.batfish.diagnosis.repair.PeerRepairer;
import org.batfish.diagnosis.repair.Repairer;
import org.batfish.diagnosis.util.ConfigTaint;
import org.batfish.diagnosis.util.KeyWord;


/*
 * Localize "violateIbgpPeer"/"violateEbgpPeer" errors
 * 注意：【暂时不能处理，某节点上一个bgpPeer都没有配置对过，因为这样bgpTopo解析的时候根本没有遇到过这样的一个node】
 */
public class PeerLocalizer extends Localizer {
    private String _localNode;
    private String _remoteNode;
    private String _localIp;
    private String _remoteIp;
    private BgpPeerConfigId _localPeer;
    private BgpPeerConfigId _remotePeer;

    public String getLocalCfgFilePath() {
        return _localCfgFilePath;
    }

    public String getRemoteCfgFilePath() {
        return _remoteCfgFilePath;
    }

    private String _localCfgFilePath;
    private String _remoteCfgFilePath;
    private BgpGenerator _generator;
    private BgpTopology _refBgpTopology;
    private Violation _violation;
    // 表示创建的时候是通过其他节点的violation创建的，所以这里定位出的error lines要写入全局变量
    // private boolean initializeFromOtherNode;
    private Map<Integer, String> _peerErrorLines;

    public Violation getViolation() {
        return _violation;
    }

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

    public BgpGenerator getGenerator() {
        return _generator;
    }

    public Map<Integer, String> getPeerErrorLines() {
        return _peerErrorLines;
    }

    public enum PeerErrorType {
        PEER_IP_REACH_LOCAL,
        PEER_IP_REACH_REMOTE,

        ACL_FILTER_TCP_PORT,

        PEER_AS_NUMBER_INCONSISTENT_LOCAL,
        PEER_AS_NUMBER_INCONSISTENT_REMOTE,

        PEER_IP_INCONSISTENT_LOCAL,
        PEER_IP_INCONSISTENT_REMOTE,

        EBGP_MAX_HOP_LOCAL,
        EBGP_MAX_HOP_REMOTE,

        PEER_CONNECT_INTERFACE_LOCAL,
        PEER_CONNECT_INTERFACE_REMOTE,

        PEER_NOT_CONFIGURED_LOCAL,
        PEER_NOT_CONFIGURED_REMOTE,

        UNKOWN_LOCAL,
        UNKOWN_REMOTE;
    }

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

    public PeerLocalizer(String node1, String node2, BgpGenerator generator, Violation violation,
            BgpTopology refBgpTopology) {
        this._localNode = node1;
        this._remoteNode = node2;
        this._localCfgFilePath = generator.getFlow().getConfigPath(node1);
        this._remoteCfgFilePath = generator.getFlow().getConfigPath(node2);
        this._generator = generator;
        // 这边应该把refBgpTopoplogy上的peer信息写下来
        this._localPeer = refBgpTopology.getBgpPeerConfigId(node1, node2);
        this._remotePeer = refBgpTopology.getBgpPeerConfigId(node2, node1);
        this._violation = violation;
        this._refBgpTopology = refBgpTopology;
        this._localIp = _remotePeer.getRemotePeerPrefix().getStartIp().toString();
        this._remoteIp = _localPeer.getRemotePeerPrefix().getStartIp().toString();
    }

    public BgpTopology getRefBgpTopology() {
        return _refBgpTopology;
    }

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

    private void addPeerErrorLines(Map<Integer, String> lines) {
        if (_peerErrorLines == null) {
            _peerErrorLines = new HashMap<>();
        }
        _peerErrorLines.putAll(lines);
    }

    private void addPeerErrorLine(Integer num, String command){
        if (_peerErrorLines == null) {
            _peerErrorLines = new HashMap<>();
        }
        _peerErrorLines.put(num, command);
    }

    @Override
    // 只查自身localNode可改的错,因为记录违规信息时两边都有记录，因此只关注当前节点就可以
    public List<ConfigurationLine> genErrorConfigLines() {
        //List<PeerErrorType> errorTypes = getErrorTypes();
        String remoteAs = String.valueOf(_refBgpTopology.getAsNumber(_remoteNode, _localNode));
        String[] localKeyWords = {KeyWord.BGP_PEER, _remoteIp, "remote-as", remoteAs };
        Map<Integer, String> cfglines = ConfigTaint.peerTaint(_localNode, localKeyWords, _localCfgFilePath);
        // String remoteAs = String.valueOf(_refBgpTopology.getAsNumber(_remoteNode, _localNode));
        //如果cfglines为空，说明没有建立peer的命令或者已配置的命令中peerip是错误的，但peerip写错的可能情况很多，在这里统一按没有正确的peer命令处理
        if(cfglines.isEmpty()){
            Map<Integer, String> peercfg = ConfigTaint.genMissingPeerConfigLines(_remoteNode, _remoteIp, String.valueOf(_refBgpTopology.getAsNumber(_remoteNode, _localNode)));
            addErrorLines(peercfg);
        }
        //如果cfglines不为空，有三种情况：(peer 命令没有配置也需要检查接口，防止同时接口错误且没有配peer)
        //1. As num error
        //2. 与peer相连的接口配置错误
        //3. 当前节点peer配置本身没有错误，但因为记录违规信息时加边的两端都记录，所以会有违规条目
        // else{
        //     //先检查As num error
        //     boolean flag = false;
        //     for(Integer lnum : cfglines.keySet()){
        //         //配置正确
        //         String[] cfgWords = cfglines.get(lnum).trim().split(" ");
        //         //neighbor 10.0.0.5 remote-as 600
        //         if(cfgWords.length > 3 && cfgWords[3].equals(remoteAs)){
        //             flag = true;
        //             break;
        //         }
        //     }
        //     //As num有误，将找到的配置命令都添加到peerErrorlines里
        //     if(!flag){
        //         addErrorLines(cfglines);
        //     }
        // }
        // //扫描接口,先只考虑接口shutdown的情况
        // List<ConfigurationLine> ifaceLines = getInterfaceLine(_localCfgFilePath, _localIp);
        // assert ifaceLines != null;
        // boolean isShutDown = false;
        // for(ConfigurationLine l : ifaceLines){
        //     //接口被禁用
        //     if(l.getLine().contains("shutdown") && !l.getLine().contains("no shutdown") && !l.getLine().contains(KeyWord.ENDING_TOKEN)){
        //         isShutDown =true;
        //         break;
        //     }
        // }
        // if(isShutDown){
        //     addErrorLines(ifaceLines);
        // }


//        String[] peerKeyWords = { "peer", localIp };
//        addPeerErrorLines(ConfigTaint.peerTaint(remoteNode, peerKeyWords, remoteCfgFilePath));

        // for (PeerErrorType err : errorTypes) {
        // switch (err) {
        // case PEER_AS_NUMBER_INCONSISTENT_LOCAL: {
        // String[] keyWords = { "peer", localPeer.getPeerIpString().toString(),
        // String.valueOf(localPeer.getPeerAsNum()) };

        // if (!initializeFromOtherNode) {
        // addErrorLines(ConfigTaint.peerTaint(localNode, keyWords));
        // } else {
        // if (BgpDiagnosis.errMap.containsKey(localNode)) {
        // BgpDiagnosis.errMap.get(localNode).putAll(ConfigTaint.peerTaint(localNode,
        // keyWords));
        // } else {
        // BgpDiagnosis.errMap.put(localNode, ConfigTaint.peerTaint(localNode,
        // keyWords));
        // }
        // }

        // break;
        // }
        // // case PEER_AS_NUMBER_INCONSISTENT_REMOTE: {
        // // String[] keyWords = {"peer", remotePeer.getPeerIpString().toString(),
        // // String.valueOf(remotePeer.getPeerAsNum())};
        // // lines.putAll(ConfigTaint.taint(remoteNode, keyWords));
        // // break;
        // // }
        // case PEER_CONNECT_INTERFACE_LOCAL: {
        // // 这个错误默认缺失对应语句
        // String line = "peer " + localPeer.getPeerIpString().toString() + "
        // connect-interface "
        // + localPeer.getLocalIpString().toString();
        // addErrorLine(violation.getMissingLine(), line);
        // break;
        // }
        // // case PEER_CONNECT_INTERFACE_REMOTE: {
        // // String line = "peer " + remotePeer.getPeerIpString().toString() + "
        // // connect-interface " + remotePeer.getLocalIpString().toString();
        // // lines.put(violation.getMissingLine(), line);
        // // break;
        // // }
        // case PEER_IGNORE_LOCAL: {
        // // 这个错误默认多写了
        // }
        // case PEER_IGNORE_REMOTE:

        // case PEER_IP_INCONSISTENT_LOCAL:
        // case PEER_IP_REACH_LOCAL: {
        // // IP不一致 就把所有 peer *ip* 有关的语句都找出来
        // String[] keyWords = { "peer", localPeer.getPeerIpString().toString() };
        // addErrorLines(ConfigTaint.taint(localNode, keyWords, null));
        // break;
        // }
        // // case PEER_IP_INCONSISTENT_REMOTE:
        // // case PEER_IP_REACH_REMOTE: {
        // // String[] keyWords = {"peer", remotePeer.getPeerIpString().toString()};
        // // lines.putAll(ConfigTaint.taint(remoteNode, keyWords));
        // // break;
        // // }

        // case PEER_NOT_CONFIGURED_LOCAL: {
        // String asNumber = "as-number";
        // if (refBgpTopology != null) {
        // asNumber = Long.toString(refBgpTopology.getAsNumber(remoteNode));
        // }
        // // 因为有的peer只配了单边，但是bgp topo上两端的peer info都会缺失，所以要在配置里再检查一遍
        // Map<Integer, String> peerConfig = getPeerConfiguration(localNode, remoteNode,
        // peerIp);
        // if (peerConfig.size() == 0) {
        // List<String> missingLines = ConfigTaint.genMissingPeerConfigLines(localIp,
        // peerIp, asNumber);
        // if (initializeFromOtherNode) {
        // missingLines.forEach(line -> peerConfig.put(violation.getMissingLine(),
        // line));
        // peerErrorLines.putAll(peerConfig);
        // } else {
        // missingLines.forEach(line -> addErrorLine(violation.getMissingLine(), line));
        // }

        // } else {
        // if (initializeFromOtherNode) {
        // putErrorLinesToGlobalErrorMap(localNode, peerConfig);
        // } else {
        // addErrorLines(peerConfig);
        // }

        // }

        // break;
        // }
        // // case PEER_NOT_CONFIGURED_REMOTE: {
        // // String line1 = "peer " + remotePeer.getPeerIpString().toString() + "
        // enable";
        // // lines.put(violation.getMissingLine(), line1);
        // // String line2 = "peer " + remotePeer.getPeerIpString().toString() + "
        // // connect-interface " + remotePeer.getLocalIpString().toString();
        // // lines.put(violation.getMissingLine(), line2);
        // // break;
        // // }
        // case EBGP_MAX_HOP_LOCAL: {
        // int realHop = generator.hopNumberToReachIpUsingStatic(localNode,
        // localPeer.getPeerIpString());
        // String line = "peer " + localPeer.getPeerIpString().toString() + "
        // ebgp-max-hop "
        // + String.valueOf(realHop);
        // String[] keyWords = { "peer", localPeer.getPeerIpString().toString(),
        // "ebgp-max-hop" };
        // addErrorLines(ConfigTaint.peerTaint(localNode, keyWords));
        // addErrorLine(violation.getMissingLine(), line);
        // break;
        // }
        // // case EBGP_MAX_HOP_REMOTE: {
        // // int realHop = generator.hopNumberToReachIpUsingStatic(remoteNode,
        // // remotePeer.getPeerIpString());
        // // String line = "peer " + remotePeer.getPeerIpString().toString() + "
        // // ebgp-max-hop " + String.valueOf(realHop);
        // // String[] keyWords = {"peer", remotePeer.getPeerIpString().toString(),
        // // "ebgp-max-hop"};
        // // lines.putAll(ConfigTaint.taint(remoteNode, keyWords));
        // // lines.put(violation.getMissingLine(), line);
        // // break;
        // // }
        // case UNKOWN_LOCAL: {
        // String[] keyWords = { "peer", localPeer.getPeerIpString().toString() };
        // addErrorLines(ConfigTaint.peerTaint(localNode, keyWords));
        // break;
        // }
        // // case UNKOWN_REMOTE: {
        // // String[] keyWords = {"peer", remotePeer.getPeerIpString().toString()};
        // // lines.putAll(ConfigTaint.taint(remoteNode, keyWords));
        // // break;
        // // }
        // }
        // }
        return getErrorLines();
    }

    public Map<Integer, String> getPeerConfiguration(String localDev, String peerDev, String peerIpString) {
        String[] peerWords = { "peer", transPrefixOrIpToIpString(peerIpString) };
        String[] enableWords = { "enable", "connect" };
        return ConfigTaint.peerTaint(localDev, peerWords, _localCfgFilePath);
    }

    public String getLocalNode() {
        return this._localNode;
    }

    public void setLocalNode(String localNode) {
        this._localNode = localNode;
    }

    public String getRemoteNode() {
        return this._remoteNode;
    }

    public void setRemoteNode(String remoteNode) {
        this._remoteNode = remoteNode;
    }

    public BgpPeerConfigId getLocalPeer() {
        return this._localPeer;
    }

    public void setLocalPeer(BgpPeerConfigId localPeer) {
        this._localPeer = localPeer;
    }

    public BgpPeerConfigId getRemotePeer() {
        return this._remotePeer;
    }

    public void setRemotePeer(BgpPeerConfigId remotePeer) {
        this._remotePeer = remotePeer;
    }

    @Override
    public Repairer genRepairer() {
        // TODO Auto-generated method stub
        PeerRepairer repairer = new PeerRepairer(_localNode, _localIp, _remoteNode, _remoteIp, this);
        repairer.genRepair();
        return repairer;
    }




}
