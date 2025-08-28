//package org.batfish.diagnosis.common;
//
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import org.batfish.datamodel.Ip;
//import org.batfish.diagnosis.util.KeyWord;
//
//import java.io.Serializable;
//
///*
// * 这个是解析后的peer，一定包含Ip（自己和对方）
// * 和仿真时的BgpTopology一起反序列化出来时【一定是成对的】，因为需要建好后才会生成这个peer
// */
//public class BgpPeerLog implements Serializable{
//
//    public enum PeerConnectType{
//        UNICAST,
//        VPNV4,
//        EVPN
//    }
//
//    public enum BgpPeerType {
//        IBGP,
//        EBGP
//    }
//
//    public enum BgpPeerConnectType{
//        DIRECT_CONNECT,
//        NONDIRECT_CONNECT
//    }
//
//    private static String LOCAL_DEV_NAME = "localDevName";
//    private static String PEER_DEV_NAME = "peerDevName";
//    private static String LOCAL_IP = "localIp";
//    private static String PEER_IP = "peerIp";
//    private static String lOCAL_AS_NUM = "localAsNum";
//    private static String LOCAL_VPN_NAME = "localVpnName";
//
//    // eBGP attributes
//    private static String PEER_AS_NUM = "peerAsNum";
//    private static String EBGP_MAX_HOP = "ebgpMaxHopNum";
//    // iBGP attributes
//    private static String RR_CLIENT = "rrclient";
//
//    private BgpPeerType _type;
//
//    @JsonProperty("localDevName")
//    private String _localDevName;
//
//    @JsonProperty("peerDevName")
//    private String _peerDevName;
//
//    // 这里的string可以是ipv4的，也可以是ipv6的
//    @JsonProperty("localIp")
//    private String _localIpString;
//    @JsonProperty("peerIp")
//    private String _peerIpString;
//
//    private Ip _localIp;
//    private Ip _peerIp;
//
////    // 有的underlay是ipv6承载
////    private Ip6 _localIp6;
////    private Ip6 _peerIp6;
//
//    @JsonProperty("localAsNum")
//    private long _localAsNum;
//
//    @JsonProperty("localVpnName")
//    private String _localVpnName;
//
//    @JsonProperty("peerAsNum")
//    private long _peerAsNum;
//
//    @JsonProperty("ebgpMaxHopNum")
//    private int _ebgpMaxHop;
//
//    @JsonProperty("validTtlHops")
//    private int _validTtlHops;
//
//    @JsonProperty("rrclient")
//    private boolean _ifPeerClient;
//
//    public String getLocalVpnName() {
//        return _localVpnName;
//    }
//
//    public String getLocalIpString() {
//        return _localIpString;
//    }
//
//    public String getPeerIpString() {
//        return _peerIpString;
//    }
//
//    public BgpPeerType getBgpPeerType() {
//        return _type;
//    }
//
//    public long getLocalAsNum() {
//        if (_type.equals(BgpPeerType.IBGP)) {
//            return _peerAsNum;
//        }
//        return _localAsNum;
//    }
//
//    public long getPeerAsNum() {
//        return _peerAsNum;
//    }
//
//    public int getEBgpMaxHop() {
//        return _ebgpMaxHop;
//    }
//
//    public String getLocalDevName() {
//        return _localDevName;
//    }
//
//    public String getPeerDevName() {
//        return _peerDevName;
//    }
//
//    public boolean isClient() {
//        return _ifPeerClient;
//    }
//
//
//    public void setPeerType(BgpPeerType type) {
//        this._type = type;
//    }
//
//    public boolean ifPeerBetween(String node1, String node2) {
//        return _localDevName.equals(node1) && _peerDevName.equals(node2);
//    }
//
//    public PeerConnectType getPeerConnectType() {
//        if (_localVpnName.toLowerCase().equals(KeyWord.VPNV4)) {
//            return PeerConnectType.VPNV4;
//        }
//        if (_localVpnName.toLowerCase().equals(KeyWord.EVPN)) {
//            return PeerConnectType.EVPN;
//        }
//        return PeerConnectType.UNICAST;
//
//    }
//
//    public BgpPeerLog(String localDevName, String peerDevName, String localIp,
//                      String peerIp, long localAsNum, String localVpnName,
//                      boolean ifClient, BgpPeerType type) {
//        _localDevName = localDevName;
//        _peerDevName = peerDevName;
//        _localIpString = localIp;
//        _peerIpString = peerIp;
//        _localIp = Ip.parse(localIp);
//        _peerIp = Ip.parse(peerIp);
//        _localAsNum = localAsNum;
//        _localVpnName = localVpnName;
//        _ifPeerClient = ifClient;
//        _type = type;
//    }
//
//    public BgpPeerLog(String localDevName, String peerDevName, String localIp,
//                      String peerIp, long localAsNum, String localVpnName,
//                      long peerAsNum, int ebgpMaxHop, BgpPeerType type) {
//        _localDevName = localDevName;
//        _peerDevName = peerDevName;
//        _localIpString = localIp;
//        _peerIpString = peerIp;
//        _localIp = Ip.parse(localIp);
//         _peerIp = Ip.parse(peerIp);
//        _localAsNum = localAsNum;
//        _localVpnName = localVpnName;
//        _peerAsNum = peerAsNum;
//        _ebgpMaxHop = ebgpMaxHop;
//        _type = type;
//    }
//
////TODO: 反序列化
////    public static BgpPeerLog deserialize(JsonObject object) {
////        BgpPeerLog peer = new Gson().fromJson(object, BgpPeerLog.class);
////        peer.setPeerType(BgpPeerType.IBGP);
////        if (object.get(EBGP_MAX_HOP)!=null) {
////            peer.setPeerType(BgpPeerType.EBGP);
////        }
////        return peer;
////    }
//
////    private boolean isConsistentIp(BgpPeer peer) {
////        if (ifIpv6Peer()) {
////            return
////        }
////    }
//
//    public boolean isConsistent(BgpPeerLog peer) {
//        if (!peer.getBgpPeerType().equals(_type)) {
//            return false;
//        }
//        switch(_type) {
//            case IBGP: {
//                return (_localDevName.equals(peer.getPeerDevName()) && getLocalIpString().equals(peer.getPeerIpString()));
//            }
//            case EBGP: {
//                return (_localAsNum==peer.getPeerAsNum() && _localDevName.equals(peer.getPeerDevName()) && getLocalIpString().equals(peer.getPeerIpString()));
//            }
//            default: return false;
//        }
//
//    }
//
//    // public BgpPeerConnectType getBgpPeerConnectType(Layer2Topology layer2Topology) {
//
//    // }
//
//    public boolean isLocalDev(String node) {
//        return _localDevName.equals(node);
//    }
//
//    public boolean isPeerDev(String node) {
//        return _peerDevName.equals(node);
//    }
//
//
//}
