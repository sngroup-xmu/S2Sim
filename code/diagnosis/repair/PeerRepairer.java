//package org.batfish.diagnosis.repair;
//
//
//import org.batfish.diagnosis.localization.PeerLocalizer;
//
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.LinkedHashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//
//import static org.batfish.diagnosis.util.ConfigTaint.isIpv4OrIpv6;
//
///*
// * 遵循该模板修复：最基本的peer连接的3个配置项：
// *  (bgp XX)视图下
// *      1. peer as-number XX
// *      2. peer connect-interface XX
// *      3. peer ebgp-max-hop XX / peer valid-ttl-hops XX 【针对eBGP peer的，两种只能选一个，如果存在冲突，则删除其中一个】
// *  (ipv4-family XX)视图下
// *      3. peer enable
// */
//public class PeerRepairer extends Repairer {
//
//    String localDevName;
//    String localIp;
//    String peerDevName;
//    String peerIp;
//    BgpPeerConnectType connectType;
//    BgpPeerConfigTemplate desiredLocalBgpPeerConfiguration;
//    BgpPeerConfigTemplate desiredRemoteBgpPeerConfiguration;
//    private Map<Integer, String> peerDeletedLines = new HashMap<>();
//    private Map<Integer, Set<String>> peerAddedLines = new HashMap<>();
//    PeerLocalizer localizer;
//
//    public Map<Integer, String> getPeerDeletedLines() {
//        return peerDeletedLines;
//    }
//
//    public Map<Integer, Set<String>> getPeerAddedLines() {
//        return peerAddedLines;
//    }
//
//    private void addPeerAddedLine(int lineNum, String line) {
//        if (!peerAddedLines.containsKey(lineNum)) {
//            peerAddedLines.put(lineNum, new LinkedHashSet<>());
//        }
//        peerAddedLines.get(lineNum).add(line);
//    }
//
//    public PeerRepairer(String localDevName, String localIp, String peerDevName, String peerIp, BgpPeerType peerType,
//            BgpPeerConnectType connectType, PeerLocalizer localizer) {
//        this.localDevName = localDevName;
//        this.localIp = localIp;
//        this.peerDevName = peerDevName;
//        this.peerIp = peerIp;
//        this.peerType = peerType;
//        this.connectType = connectType;
//        this.localizer = localizer;
//
//        if (peerType == BgpPeerType.IBGP) {
//            desiredLocalBgpPeerConfiguration = new BgpPeerConfigTemplate.Builder(localDevName, peerIp)
//                    .peerAsNumber(localizer.getRefBgpTopology().getAsNumber(peerDevName))
//                    .peerEnabled(true)
//                    .connectInterfaceName(localizer.getGenerator().getLayer2Topology()
//                            .getIpLocatedInterface(peerDevName, peerIp).getInfName())
//                    .build();
//            desiredRemoteBgpPeerConfiguration = new BgpPeerConfigTemplate.Builder(peerDevName, localIp)
//                    .peerAsNumber(localizer.getRefBgpTopology().getAsNumber(localDevName))
//                    .peerEnabled(true)
//                    .connectInterfaceName(localizer.getGenerator().getLayer2Topology()
//                            .getIpLocatedInterface(localDevName, localIp).getInfName())
//                    .build();
//        } else {
//            // maxHop先设2跳，这里还要检查一下：1）是否有两跳及以上可达，2）如果没有可达，先要修复使得可达【以两跳为标准修】
//            desiredLocalBgpPeerConfiguration = new BgpPeerConfigTemplate.Builder(localDevName, peerIp)
//                    .peerAsNumber(localizer.getRefBgpTopology().getAsNumber(peerDevName))
//                    .peerEnabled(true)
//                    .connectInterfaceName(localizer.getGenerator().getLayer2Topology()
//                            .getIpLocatedInterface(peerDevName, peerIp).getInfName())
//                    .eBgpMaxHop(2).build();
//            desiredRemoteBgpPeerConfiguration = new BgpPeerConfigTemplate.Builder(peerDevName, localIp)
//                    .peerAsNumber(localizer.getRefBgpTopology().getAsNumber(localDevName))
//                    .peerEnabled(true)
//                    .connectInterfaceName(localizer.getGenerator().getLayer2Topology()
//                            .getIpLocatedInterface(localDevName, localIp).getInfName())
//                    .eBgpMaxHop(2).build();
//        }
//    }
//
//    /*
//     * 获取bgp进程配置的行号
//     */
//    private int getBgpProcessLineNum(boolean ifLocalNode) {
//        String[] words = new String[2];
//        words[0] = "bgp";
//        String taintNode;
//        if (ifLocalNode) {
//            taintNode = localDevName;
//            words[1] = String.valueOf(desiredRemoteBgpPeerConfiguration.getPeerAsNum());
//            return ConfigTaint.taint(taintNode, words, localizer.getLocalCfgFilePath()).keySet().iterator().next();
//        } else {
//            taintNode = peerDevName;
//            words[1] = String.valueOf(desiredLocalBgpPeerConfiguration.getPeerAsNum());
//            return ConfigTaint.taint(taintNode, words, localizer.getRemoteCfgFilePath()).keySet().iterator().next();
//        }
//
//    }
//
//    /*
//     * 根据peer vpn类型获取对应地址族视图入口
//     */
//    private int getPeerAddressFamilyLine(boolean ifLocalNode) {
//        String[] words = new String[2];
//        String node;
//        switch (localizer.getDefaultPeerConnectType()) {
//            case UNICAST: {
//                words[0] = KeyWord.IPV4_FAMILY;
//                words[1] = KeyWord.UNICAST;
//                break;
//            }
//            case VPNV4: {
//                words[0] = KeyWord.IPV4_FAMILY;
//                words[1] = KeyWord.VPNV4;
//                break;
//            }
//            case EVPN: {
//                words[0] = KeyWord.L2VPN_FAMILY;
//                words[1] = KeyWord.EVPN;
//                break;
//            }
//        }
//        if (ifLocalNode) {
//            node = localDevName;
//            return ConfigTaint.taint(node, words, localizer.getLocalCfgFilePath()).keySet().iterator().next();
//        } else {
//            node = peerDevName;
//            return ConfigTaint.taint(node, words, localizer.getRemoteCfgFilePath()).keySet().iterator().next();
//        }
//    }
//
//    private boolean ifPeerIpConfig(String line) {
//        line = line.strip();
//        String words[] = line.split(" ");
//        assert  words.length > 2 && words[0].equals("peer");
//        return isIpv4OrIpv6(words[1]);
//    }
//
//    @Override
//    /*
//     * 和现有lines做差分输出add/del行的func
//     * 【peer配置前面有一行缩进】
//     */
//    public void genRepair() {
//        // 先local节点
//        String[] localKeyWords = { "peer", peerIp };
//        Map<Integer, String> realLines = removePeerConnectionUnrelatedLines(
//                ConfigTaint.peerTaint(localDevName, localKeyWords, localizer.getLocalCfgFilePath()));
//        List<String> desiredLines = desiredLocalBgpPeerConfiguration.genConfigurationLines();
//        // @TODO：先不考虑group的情况，一般group配置是配置策略和反射类的
//        for (Integer lineNum : realLines.keySet()) {
//            String line = realLines.get(lineNum).strip();
//            if (!desiredLines.remove(line)) {
//                if (ifPeerIpConfig(line)) {
//                    addDeletedLine(lineNum, line);
//                }
//
//            }
//        }
//        /*
//         * peer配置加入的位置不一定就是当前peer其他配置的行上，因为
//         * 1) 有可能这个peer根本没有配置
//         * 2）peer enable/reflect client/route-policy...的配置配在对应的地址族下
//         */
//        if (desiredLines.size() > 0) {
//            int insertLineIndex1 = getBgpProcessLineNum(true);
//            int insertLineIndex2 = getPeerAddressFamilyLine(true);
//
//            for (String line : desiredLines) {
//                switch (BgpPeerConfigTemplate.getPeerConfigLocationView(line)) {
//                    case BGP_PROCESS: {
//                        addAddedLine(insertLineIndex1, line);
//                        break;
//                    }
//                    case PEER_ADDRESS_FAMILY: {
//                        addAddedLine(insertLineIndex2, line);
//                        break;
//                    }
//                }
//            }
//        }
//
//        // peer节点
//        String[] remoteKeyWords = { "peer", localIp };
//        realLines = removePeerConnectionUnrelatedLines(ConfigTaint.peerTaint(peerDevName, remoteKeyWords,localizer.getRemoteCfgFilePath()));
//        desiredLines = desiredRemoteBgpPeerConfiguration.genConfigurationLines();
//        // @TODO：先不考虑group的情况，一般group配置是配置策略和反射类的
//        for (Integer lineNum : realLines.keySet()) {
//            String line = realLines.get(lineNum).strip();
//            if (!desiredLines.remove(line)) {
//                if (ifPeerIpConfig(line)) {
//                    peerDeletedLines.put(lineNum, line);
//                }
//            }
//        }
//        if (desiredLines.size() > 0) {
//            int insertLineIndex1 = getBgpProcessLineNum(false);
//            int insertLineIndex2 = getPeerAddressFamilyLine(false);
//
//            for (String line : desiredLines) {
//                switch (BgpPeerConfigTemplate.getPeerConfigLocationView(line)) {
//                    case BGP_PROCESS: {
//                        addPeerAddedLine(insertLineIndex1, IndentController.process(line));
//                        break;
//                    }
//                    case PEER_ADDRESS_FAMILY: {
//                        addPeerAddedLine(insertLineIndex2, IndentController.process(line));
//                        break;
//                    }
//                }
//            }
//        }
//
//    }
//
//    /*
//     * 把对peer策略，反射类的配置都删除，只留下和建立peer相关的
//     */
//    public Map<Integer, String> removePeerConnectionUnrelatedLines(Map<Integer, String> lines) {
//        Set<Integer> lineNumList = new HashSet<>(lines.keySet());
//        lineNumList.forEach(lineNum -> {
//            String line = lines.get(lineNum);
//            if (!(line.contains("as-number") || line.contains("enable") || line.contains("ebgp-max-hop")
//                    || line.contains("connect-interface"))) {
//                lines.remove(lineNum);
//            }
//        });
//        return lines;
//    }
//
//}
