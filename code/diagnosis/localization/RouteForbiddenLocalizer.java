//package org.batfish.diagnosis.localization;
//
//import java.util.List;
//import org.batfish.datamodel.AbstractRoute;
//import org.batfish.datamodel.BgpPeerConfig;
//import org.batfish.datamodel.Configuration;
//import org.batfish.diagnosis.common.ConfigurationLine;
//import org.batfish.diagnosis.repair.Repairer;
//import org.batfish.diagnosis.repair.RouteForbiddenRepairer;
//import org.batfish.diagnosis.util.ConfigTaint;
//
///*
// * Localize "violatedPropNeighbors"/"violatedAcptNeighbors" errors
// * 针对peer export/import policy的
// * => Why a route can not be propagated expectedly?
// * 1) policy (filtered)
// * 2) vpn RT match
// */
//public class RouteForbiddenLocalizer extends Localizer {
//
//    String node;
//    //----------- 只有BGP路由传播时拦截才会有这两个字段 ------------
//    private String peerNode;
//    private BgpPeerConfig bgpPeerConfig;
//    //----------- 只有BGP路由传播时拦截才会有这两个字段 ------------
//
//    // 路由策略policy的名称（如果没有这个policy，那么就是vpn交叉失败导致forbid的）
//    String policyName;
//    AbstractRoute route;
//    Configuration configuration;
//    private final Violation violation;
//    // direction是按照violatedRule类型设置的，绝对准确
//    Direction direction;
//    // 这个表示没有正常建立peer关系的节点
//    private String cfgPath;
//
//
//    public enum Direction {
//        IN("import"),
//        OUT("export"),
//        REDISTRIBUTE("redistribute");
//        String name;
//        Direction(String name) {
//            this.name = name;
//        }
//        String getName() {
//            return name;
//        }
//    }
//
//    public AbstractRoute getRoute() {
//        return route;
//    }
//    public String getPolicyName() {
//        return this.policyName;
//    }
//    public String getPeerNode() {
//        return peerNode;
//    }
//    public Direction getDirection() {
//        return direction;
//    }
//    public String getNode() {
//        return node;
//    }
//    public Configuration getConfiguration() {
//        return configuration;
//    }
//
//    public RouteForbiddenLocalizer(String node, String peerNode, AbstractRoute route, BgpPeerConfig bgpPeerConfig,
//                                   Direction direction, Violation violation,
//                                   Configuration configuration, String cfgPath) {
//        // 传入的bgp topo需要是假设的，不然可能找不到peer dev
//        this.node = node;
//        // 如果是因为export被deny的，在export那端会有记录
//        this.route = route;
//        this.direction = direction;
//        this.violation = violation;
//        this.cfgPath = cfgPath;
//        this.bgpPeerConfig = bgpPeerConfig;
//        this.peerNode = peerNode;
//        this.configuration = configuration;
//    }
//
//    public RouteForbiddenLocalizer(String node, AbstractRoute route,
//                                   Direction direction, Violation violation,
//                                   Configuration configuration, String cfgPath) {
//        // 传入的bgp topo需要是假设的，不然可能找不到peer dev
//        this.node = node;
//        // 如果是因为export被deny的，在export那端会有记录
//        this.route = route;
//        this.direction = direction;
//        this.violation = violation;
//        this.cfgPath = cfgPath;
//        this.configuration = configuration;
//    }
//
//    @Override
//    public List<ConfigurationLine> genErrorConfigLines() {
//        // TODO 先检查是不是因为 policy filter route【可能是 peer不通或者路由交叉不了】
//
//        // STEP1: 检测是否直接配了peer ip policy
//        String[] keyWords = { "neighbor", bgpPeerConfig.getLocalIp().toString(), policyName, direction.getName() };
//        addErrorLines(ConfigTaint.peerTaint(node, keyWords, cfgPath));
//        addErrorLines(ConfigTaint.policyLinesFinder(node, policyName, cfgPath));
//        return getErrorLines();
//    }
//
//    @Override
//    public Repairer genRepairer() {
//        return new RouteForbiddenRepairer(this);
//    }
//
//}
