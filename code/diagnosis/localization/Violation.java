//package org.batfish.diagnosis.localization;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import java.io.File;
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import org.batfish.datamodel.Bgpv4Route;
//import org.batfish.datamodel.Configuration;
//import org.batfish.datamodel.ConnectedRoute;
//import org.batfish.datamodel.Prefix;
//import org.batfish.datamodel.StaticRoute;
//import org.batfish.diagnosis.Diagnoser;
//import org.batfish.diagnosis.common.BgpPeerLog;
//import org.batfish.diagnosis.common.BgpRouteLog;
//import org.batfish.diagnosis.common.ConfigurationLine;
//import org.batfish.diagnosis.reference.BgpGenerator;
//import org.batfish.diagnosis.repair.Repairer;
//import org.batfish.diagnosis.repair.RouteForbiddenRepairer;
//import org.batfish.diagnosis.util.InputData;
//
//// 每个设备一个violation实例
//
//public class Violation {
//    @JsonProperty("ipPrefix")
//    String ipPrefixString;
//    String vpnName;
//    Set<BgpPeerLog> violatedRrClient;
//    List<BgpRouteLog> violatedPropNeighbors;
//    List<BgpRouteLog> violatedAcptNeighbors;
//    // prefer 的表示?
//    List<Map<String, BgpRouteLog>> violatedRoutePrefer;
//    Set<String> violateIbgpPeer;
//    Set<String> violateEbgpPeer;
//    StaticRoute originStaticRoute;
//    ConnectedRoute originDirectRoute;
//    // DirectRoute originDirectRoute;
//    int missingLineCounter;
//
//    // 描述redis失败的原因的字符串，用逗号分隔多个原因
//    String violateRedis;
//    // 自身节点的所有localizer
//    Set<Localizer> localizers = new HashSet<>();
//
//    public String getVpnName() {
//        return vpnName;
//    }
//
//    /*
//     * 对violation里的各种violated rules去重
//     */
//    public void preProcessOfViolation() {
//        // STEP 1: 先处理重复的violatedPropNeighbors/violatedAcptNeighbors;
//    }
//
//    public void addViolateEbgpPeer(String node) {
//        if (violateEbgpPeer == null) {
//            violateEbgpPeer = new HashSet<String>();
//        }
//        violateEbgpPeer.add(node);
//    }
//
//    public void addViolateIbgpPeer(String node) {
//        if (violateIbgpPeer == null) {
//            violateIbgpPeer = new HashSet<String>();
//        }
//        violateIbgpPeer.add(node);
//    }
//
//    public static <T> boolean ifListValid(List<T> aimList) {
//        if (aimList == null || aimList.size() < 1) {
//            return false;
//        }
//        return true;
//    }
//
//    public static <T> boolean ifSetValid(Set<T> aimList) {
//        if (aimList == null || aimList.size() < 1) {
//            return false;
//        }
//        return true;
//    }
//
//    public int getMissingLine() {
//        missingLineCounter -= 1;
//        return missingLineCounter;
//    }
//
//    // public List<BgpRoute>
//
//    public List<BgpRouteLog> getViolatedPropNeighbors() {
//        return violatedPropNeighbors;
//    }
//
//    public List<BgpRouteLog> getViolatedAcptNeighbors() {
//        return violatedAcptNeighbors;
//    }
//
//    public Set<String> getViolateEbgpPeers() {
//        return violateEbgpPeer;
//    }
//
//    public Set<String> getViolateIbgpPeers() {
//        return violateIbgpPeer;
//    }
//
//    public Set<Localizer> getLocalizers() {
//        return localizers;
//    }
//
//    public void addResults(Localizer localizer) {
//        localizers.add(localizer);
//    }
//
//    /*
//     * 如果之前有repairer使用过同样的policy，则把相应的routePolicy返回，新的routeForbiddenRepairer引用它
//     */
//    private RouteForbiddenRepairer getRouteForbiddenRepairerWithSamePolicyName(String name, Set<Repairer> repairSet) {
//        for (Repairer repairer: repairSet) {
//            if (repairer instanceof RouteForbiddenRepairer) {
//                if (((RouteForbiddenRepairer) repairer).getPolicyName().equals(name)) {
//                    return (RouteForbiddenRepairer) repairer;
//                }
//            }
//        }
//        return null;
//    }
//
//    public Set<Repairer> tryRepair(String curNode) {
//        Set<Repairer> repairSet = new HashSet<>();
//        for (Localizer localizer: localizers) {
//            // @TODO: routeForbidden的修复，对于同一个policy的多个localizer/repairer需引用同一个routePolicy实例【不需要是对同一个neighbor的】
//            Repairer repairer = localizer.genRepairer();
//            repairer.genRepair();
//            repairSet.add(repairer);
//        }
//        return repairSet;
//    }
//
//    public List<ConfigurationLine> getErrorLinesForSingleNode(String curDevName, Diagnoser diagnoser, BgpGenerator newGenerator) {
//        BgpGenerator errGenerator = diagnoser.getBgpGenerator();
//        String configPath = errGenerator.getFlow().getConfigPath(curDevName);
//        Configuration curConfiguration = diagnoser.getConfiguration(curDevName);
//        // 未配置反射客户的错误
//        if (ifSetValid(violatedRrClient)) {
//            localizers.add(new ReflectClientLocalizer(curDevName, violatedRrClient, this , configPath));
//        }
//
//        /*
//         未能正常接收目标路由，有以下原因：
//           1）策略拦截：如果importPolicy字段非空
//           2）非策略拦截原因：
//             * a）peer关系未建立
//             * b）VPN交叉失败
//             * c）迭代不到隧道
//             *
//             * 先按a）处理
//        */
//        //TODO: 确认import policy记录的格式，是否记在BgpRpute里，如果和华为的格式一样，则无需更换成BF的Bgpv4Route
//        if (ifListValid(violatedAcptNeighbors)) {
//            violatedAcptNeighbors.forEach(n -> {
//                if (n.getPrefix().containsPrefix(newGenerator.getFlow().getReqDstPrefix())) {
//                    if (n.getImRoutePolicyName()!=null) {
//                        RouteForbiddenLocalizer routeForbiddenLocalizer = new RouteForbiddenLocalizer(curDevName, n,
//                                RouteForbiddenLocalizer.Direction.IN, this, curConfiguration, configPath);
//                        localizers.add(routeForbiddenLocalizer);
//                    } else {
//                        String peer = n.getFromDeviceName();
//                        assert false: "Other Mistakes make the route cannot be accepted";
//
//                    }
//                }
//
//            });
//        }
//
//        /*
//         未能正常发送目标路由，有以下原因：
//           1）策略拦截：如果exportPolicy字段非空【还要再检查newGenerator里prop的节点是否已有默认路由，有默认路由就默认这个violatedProp是误报】
//           2）非策略拦截原因：
//             * a）peer关系未建立
//             * c）迭代不到隧道
//             *
//        */
//        if (ifListValid(violatedPropNeighbors)) {
//            violatedPropNeighbors.forEach(n -> {
//                // 当前条目的prefix要包含dstPrefix才继续下一步
//                if (n.getPrefix().containsPrefix(newGenerator.getFlow().getReqDstPrefix())) {
//                    if (n.getExRoutePolicyName()!=null) {
//                        // 检测是否已有默认路由
//                        Bgpv4Route bgpRouteInNewGenerator = newGenerator.getBgpTree().getBestBgpRoute(n.getToDeviceName());
//                        if (bgpRouteInNewGenerator==null || !bgpRouteInNewGenerator.getNetwork().containsPrefix(Prefix.ZERO)) {
//                            RouteForbiddenLocalizer routeForbiddenLocalizer = new RouteForbiddenLocalizer(curDevName, n,
//                                    RouteForbiddenLocalizer.Direction.OUT, this, curConfiguration, configPath);
//                            localizers.add(routeForbiddenLocalizer);
//                        }
//                    } else {
//                        String peer = n.getToDeviceName();
//                        assert false: "Other Mistakes make the route cannot be propagated";
//                    }
//                }
//
//            });
//        }
//
//        //@TODO: Bgp Peer
////        if (ifSetValid(violateEbgpPeer)) {
////            violateEbgpPeer.forEach(n -> {
////                localizers.add(new PeerLocalizer(curDevName, n, errGenerator, this, errGenerator.getBgpTopology()));
////            });
////        }
//
////        if (ifSetValid(violateIbgpPeer)) {
////            violateIbgpPeer.forEach(n -> {
////                localizers.add(new PeerLocalizer(curDevName, n, errGenerator, this, errGenerator.getBgpTopology()));
////            });
////        }
//
//        if (violateRedis != null && !violateRedis.equals("")) {
//            if (originStaticRoute != null || originDirectRoute!=null) {
//                RedistributionLocalizer redistributionLocalizer = new RedistributionLocalizer(curDevName, violateRedis, originStaticRoute, originDirectRoute, this,
//                        configPath, diagnoser.getConfiguration(curDevName));
//                localizers.add(redistributionLocalizer);
//                // 如果是由policy拦截的，还要再生成policy Forbidden的localizer
//                if (redistributionLocalizer.getErrorTypes().contains(RedistributionLocalizer.RedisErrorType.POLICY)) {
//                    if (originDirectRoute!=null) {
//                        localizers.add(new RouteForbiddenLocalizer(curDevName, originDirectRoute, RouteForbiddenLocalizer.Direction.REDISTRIBUTE, this, curConfiguration,  configPath));
//                    } else {
//                        localizers.add(new RouteForbiddenLocalizer(curDevName, originStaticRoute, RouteForbiddenLocalizer.Direction.REDISTRIBUTE, this, curConfiguration,  configPath));
//                    }
//
//                }
//            } else {
//                /*
//                 * 有redistribution的错，但是没有violatedRoute，生成一条valid的静态路由
//                 *  1）先找有没有静态路由 或者 直连路由
//                 */
//                assert false: "Did not record correct redistributed log";
//
//            }
//        }
//
//        List<ConfigurationLine> lineMap = new ArrayList<>();
//        localizers.forEach(n -> {
//            lineMap.addAll(n.genErrorConfigLines());
//        });
//        return lineMap;
//    }
//
//    public static void main(String[] args) throws JsonProcessingException {
//        String path = "/home/yrl/yrl/batfish-repair-/viorules.json";
//        File file = new File(path);
//        ObjectMapper mapper = new ObjectMapper();
//        Violation violation = mapper.readValue(InputData.getStr(file), Violation.class);
//        System.out.println();
//    }
//
//}
