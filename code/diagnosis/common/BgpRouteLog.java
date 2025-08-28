//package org.batfish.diagnosis.common;
//
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import org.apache.commons.collections4.ListUtils;
//import org.batfish.datamodel.*;
//
//import javax.annotation.Nullable;
//import java.util.List;
//
//public class BgpRouteLog extends AbstractRoute {
//    protected BgpRouteLog(@Nullable Prefix network, int admin, long tag, boolean nonRouting, boolean nonForwarding) {
//        super(network, admin, tag, nonRouting, nonForwarding);
//    }
//
//    public enum FromType{
//        FROMORIGIN("FROMORIGIN"),
//        FROMIBGP("FROMIBGP"),
//        FROMEBGP("FROMEBGP");
//
//        private String type;
//
//        private FromType(String type) {
//            this.type = type;
//        }
//    }
//
//    public enum OriginType {
//        EGP("EGP", 1),
//        IGP("IGP", 2),
//        INCOMPLETE("INCOMPLETE", 0);
//
//        private final String _name;
//
//        private final int _preference;
//
//        OriginType(String originType, int preference) {
//          _name = originType;
//          _preference = preference;
//        }
//
//        public String getOriginTypeName() {
//          return _name;
//        }
//
//        public int getPreference() {
//          return _preference;
//        }
//      }
//
//    // prov BgpRouteLog attrs
//    private int id;
//    private String deviceName;
//    // 就是网段信息
//    @JsonProperty("ipPrefix")
//    private String ipPrefixString;
//    private String nextHopIp;
//    private String nextHopDevice;
//
//    private String importType; // ?
//    private String originProtocol;
//    private int preferredValue;
//    private int localPreference;
//
//    private String fromType; // ?
//    private List<Long> asPath;
//    private OriginType origin;
//    private String peerIp;
//    private long routerId;
//    private int med;
//    private List<Long> clusterList;
//    // private String originalPreference;
//    private List<String> curVpnList;
//
//
//    // violated BgpRouteLog attrs
//    private String toDeviceName;
//    private String fromDeviceName;
//
//    @JsonProperty("exRoutePolicy")
//    private String exRoutePolicyName;
//
//    @JsonProperty("imRoutePolicy")
//    private String imRoutePolicyName;
//
//    public String getImRoutePolicyName() {
//        return imRoutePolicyName;
//    }
//
//    public String getDeviceName() {
//        return deviceName;
//    }
//
//    public String getIpPrefixString() {
//        return ipPrefixString;
//    }
//
//    public String getNextHopDevice() {
//        return nextHopDevice;
//    }
//
//    public String getExRoutePolicyName() {
//        return exRoutePolicyName;
//    }
//
//    public String getImportType() {
//        return importType;
//    }
//
//    public String getOriginProtocol() {
//        return originProtocol;
//    }
//
//    public List<String> getCurVpnList() {
//        return curVpnList;
//    }
//
//    public Prefix getPrefix() {
//        return Prefix.parse(ipPrefixString);
//    }
//
////    public static BgpRouteLog deserialize(String jsonStr) {
////        return new Gson().fromJson(jsonStr, BgpRouteLog.class);
////    }
//
//    public String getLatestVpnName() {
//        if (curVpnList !=null && curVpnList.size()>0) {
//           return curVpnList.get(curVpnList.size()-1);
//        } else {
//            return null;
//        }
//    }
//
//    public boolean isDefaultIpv4Route() {
//        return ipPrefixString.equals("0.0.0.0") || ipPrefixString.equals("0.0.0.0/0");
//    }
//
//    public String getToDeviceName() {
//        return toDeviceName;
//    }
//
//    public String getFromDeviceName() {
//        return fromDeviceName;
//    }
//
//    /*
//     * 获取路由发送方的名称：如果有fromDevName就返回，否则就以peerIp在layer2Topo上查找设备名称
//     */
//
//
//    @Override
//    public RoutingProtocol getProtocol() {
//        return null;
//    }
//
//    @Override
//    public AbstractRouteBuilder<?, ?> toBuilder() {
//        return null;
//    }
//
//    public String getNextHopIpString() {
//        return nextHopIp;
//    }
//
//    public Ip getPeerIp() {
//        if (Prefix.tryParse(peerIp).isPresent()) {
//            return Prefix.parse(peerIp).getEndIp();
//        } else if (Ip.tryParse(peerIp).isPresent()) {
//            return Ip.parse(peerIp);
//        } else {
//            return Ip.ZERO;
//        }
//    }
//
//    public String getPeerIpString() {
//        return peerIp;
//    }
//
//    public String getNextHopDev() {
//        return nextHopDevice;
//    }
//
//    public boolean ifTwoStringEqual(String str1, String str2) {
//        if (str1!=null && str2!=null) {
//            return str1.equals(str2);
//        } else if (str1!=null || str2!=null) {
//            return false;
//        } else {
//            return true;
//        }
//    }
//
//    @Override
//    public boolean equals(Object object) {
//        if (object instanceof BgpRouteLog) {
//            BgpRouteLog BgpRouteLog = (BgpRouteLog) object;
//            return ifTwoStringEqual(BgpRouteLog.getDeviceName(), deviceName) &&
//                    ifTwoStringEqual(BgpRouteLog.getFromDeviceName(), fromDeviceName) &&
//                    ifTwoStringEqual(BgpRouteLog.getToDeviceName(), toDeviceName) &&
//                    ifTwoStringEqual(BgpRouteLog.getPeerIpString(), peerIp) &&
//                    ifTwoStringEqual(BgpRouteLog.getExRoutePolicyName(), exRoutePolicyName) &&
//                    ifTwoStringEqual(BgpRouteLog.getImRoutePolicyName(), importType) &&
//                    ifTwoStringEqual(BgpRouteLog.getImportType(), importType) &&
//                    ifTwoStringEqual(BgpRouteLog.getNextHopDev(), nextHopDevice) &&
//                    ifTwoStringEqual(BgpRouteLog.getOriginProtocol(), originProtocol) &&
//                    ListUtils.isEqualList(BgpRouteLog.getCurVpnList(), curVpnList);
//        }
//        return false;
//    }
//
//    @Override
//    public int hashCode() {
//        return 0;
//    }
//
//    @Override
//    public Long getMetric() {
//        return 0L;
//    }
//
//}
