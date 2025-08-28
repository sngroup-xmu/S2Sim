package org.batfish.diagnosis.util;

public class KeyWord {
    public static String LOGGER_NAME = "bgpDiagnosis";

    public static String OPT = "operator";
    public static String VPN_NAME_OR_VPNV4 = "vpnNameOrVpnv4";
    public static String ROUTE = "route";
    public static String NEXT_HOP_IP = "nextHopIp";
    public static String NEXT_HOP_DEV = "nextHopDevice";
    public static String INDEX = "index";
    public static String CONFIG_ENDING_TOKEN = "#";
    public static String VPNV4 = "vpnv4";
    public static String EVPN = "evpn";

    // static related
    public static String NEXT_HOP = "nextHop";
    public static String OUT_INFO = "outInf";
    public static String DEV_NAME = "deviceName";
    public static String IFACE_NAME = "infName";
    public static String IFACE_IP = "infIpv4HostIp";
    public static String INTERFACE = "interface";
    public static String INTERFACE_ADDRESS = "ip address";

    public static String SENDER = "sender";
    public static String VPN_NAME = "vpnName";
    public static String SEND_PEER = "sendPeer";
    public static String SRC_DEV_NAME = "srcDeviceName";
    public static String DST_DEV_NAME = "dstDeviceName";
    public static String SRC_ADDRESS = "srcAddress";
    public static String DST_ADDRESS = "dstAddress";

    public static String UNKNOWN = "unkown";
    public static String PROV_INFO_FILE_NAME = "provenanceInfo.json";
    public static String RELATED_STATIC_INFO_FILE = "relatedStaticAndDirectInfo.json";
    public static String OTHER_IMPORT_BGP_INFO_FILE_NAME = "otherProtocolImportInBgpInfo.json";
    public static String INTERFACE_INFO_FILE_PATH = "vpnBindingInfo.json";

    public static String ERROR = "error";
    public static String CORRECT = "correct";
    public static String REPAIRED = "repaired";

    public static String IBGP_PEER_INFO = "ibgpPeerInfoSet";
    public static String EBGP_PEER_INFO = "ebgpPeerInfoSet";

    public static String BGP_PEER = "neighbor";

    public static String PEER_IP = "peerIp";
    public static String PRINT_LINE = "******************************************";
    public static String PRINT_LINE_HALF = "********************";

    public static String IP_STATIC_ROUTE_PREFIX = "ip route";
    public static String PREFERENCE = "preference";
    public static String ALL_VPN_BINDING_INFO = "allVpnBindingInfo";

    public static String DEV2_VIOLATE_RULES = "dev2ViolateRules";
    public static int MISSING_LINE = -1;
    public static String PUBLIC_VPN_NAME = "_public_";
    public static String IPV4_FAMILY = "address-family ipv4";
    public static String UNICAST = "unicast";
    public static String IP_VPN_INSTANCE = "ip vpn-instance";
    public static String L2VPN_FAMILY = "l2vpn-family";

    // vpn keywords
    public static final String IP_FAMILY_TOKEN = "family";
    public static final String TUNNEL_POLICY = "tnl-policy";
    public static final String RD_TOKEN = "route-distinguisher";
    public static final String RT_TOKEN = "vpn-target";
    public static final String EXPORT = "export";
    public static final String IMPORT = "import";

    public static String ENDING_TOKEN = "!";
    public static String LOOPBACK0 = "LoopBack0";
    public static String SHUTDOWN = "shutdown";
    public static String UNDO_SHUTDOWN = "undo shutdown";

    public static String ROUTE_POLICY = "route-map";
    public static String NODE = "node";

    public static String PERMIT = "permit";
    public static String DENY = "deny";

    public static String POLICY_MATCH = "match";
    public static String POLICY_ACTION = "set";

    public static String VLAN_TYPE = "vlan-type dot1q";
    public static String AS_NUMBER = "as-number";
    public static String CONNECTED_INTERFACE = "connect-interface";
    public static String ENABLE = "enable";
    public static String BGP_PROCESS_COMMAND = "router bgp";
    public static String ADDRESS_FAMILY = "address-family";
    public static String BGP_NEIGHBOR = "neighbor";

    //Batfish policy Name
    public static String PEER_EXPORT_POLICY_DEFAULT = "~BGP_PEER_EXPORT_POLICY:default:";

    public static String REQUIREMENT_FILE = "requirements.json";
    public static String CONDITION_FILE = "conditions.json";

}

