//package org.batfish.diagnosis.localization;
//
//import java.util.ArrayList;
//import java.util.List;
//import org.batfish.datamodel.Configuration;
//import org.batfish.datamodel.ConnectedRoute;
//import org.batfish.datamodel.Prefix;
//import org.batfish.datamodel.StaticRoute;
//import org.batfish.datamodel.bgp.BgpTopology;
//import org.batfish.diagnosis.common.ConfigurationLine;
//import org.batfish.diagnosis.repair.RedistributionRepairer;
//import org.batfish.diagnosis.repair.Repairer;
//import org.batfish.diagnosis.util.ConfigTaint;
//
///*
// * Localize "violateRedis" errors (3 types):
// * ==> Why the target route can not be redistributed to target routing process?
// * 1) policy 【TODO 要不要把这个归类到forbidden的错里？】
// * 2) "No Redistribution Config"
// * 3) "inValid"
// */
//public class RedistributionLocalizer extends Localizer {
//    private String node;
//    private StaticRoute staticRoute;
//    private ConnectedRoute connectedRoute;
//    private String[] causeKeyWords;
//    private String splitSymbol = ",";
//    private Violation violation;
//    private BgpTopology bgpTopology;
//    List<RedisErrorType> errorTypes;
//
//    public String getCfgPath() {
//        return cfgPath;
//    }
//
//    public Prefix getForbiddenPrefix() {
//        if (staticRoute!=null) {
//            return staticRoute.getNetwork();
//        } else {
//            return connectedRoute.getNetwork();
//        }
//    }
//
//    private final String cfgPath;
//    private Configuration configuration;
//
//    public List<RedisErrorType> getErrorTypes() {
//        return errorTypes;
//    }
//
//    public enum RedisErrorType {
//        NO_REDISTRIBUTE_COMMOND,
//        ROUTE_INVALID,
//        POLICY,
//        NOT_BEST
//    }
//
//    private RedisErrorType getErrorTypeFromKeyWord(String causeKeyWord) {
//        causeKeyWord = causeKeyWord.toLowerCase();
//        if (causeKeyWord.contains("no") && causeKeyWord.contains("config")) {
//            return RedisErrorType.NO_REDISTRIBUTE_COMMOND;
//        } else if (causeKeyWord.contains("invalid") || causeKeyWord.contains("invaild")) {
//            return RedisErrorType.ROUTE_INVALID;
//        } else if (causeKeyWord.contains("best")) {
//            return RedisErrorType.NOT_BEST;
//        } else {
//            return RedisErrorType.POLICY;
//        }
//    }
//
//    public String getPolicyName() {
//        for (String word : causeKeyWords) {
//            if (getErrorTypeFromKeyWord(word).equals(RedisErrorType.POLICY)) {
//                return word;
//            }
//        }
//        return null;
//    }
//
//    public RedistributionLocalizer(String node, String causeKeyWord, StaticRoute staticRoute, ConnectedRoute connectedRoute, Violation violation,
//                                   String configPath, Configuration configuration) {
//        this.node = node;
//        this.causeKeyWords = causeKeyWord.split(splitSymbol);
//        this.errorTypes = genErrorTypes();
//
//        this.staticRoute = staticRoute;
//        this.connectedRoute = connectedRoute;
//        assert staticRoute!=null || connectedRoute!=null;
//
//        this.violation = violation;
//        this.cfgPath = configPath;
//
//        this.configuration = configuration;
//    }
//
//    public List<RedisErrorType> genErrorTypes() {
//        List<RedisErrorType> errList = new ArrayList<>();
//        for (String word : causeKeyWords) {
//            errList.add(getErrorTypeFromKeyWord(word));
//        }
//        return errList;
//    }
//
//    @Override
//    public List<ConfigurationLine> genErrorConfigLines() {
//        // TODO Auto-generated method stub
//        errorTypes.forEach(n -> {
//            switch (n) {
//                case NO_REDISTRIBUTE_COMMOND: {
//                    String netCommond = "network " + staticRoute.getNetwork().getStartIp() + " "
//                            + staticRoute.getNetwork().getPrefixLength();
//                    addErrorLine(violation.getMissingLine(), netCommond);
//                    break;
//                }
//                case POLICY: {
//                    // TODO 先找到调用ref policy的那一行
//                    String[] keWords = { "import", getPolicyName() };
//                    addErrorLines(ConfigTaint.taintWithForbidWord(node, keWords, "peer", cfgPath));
//                    addErrorLines(ConfigTaint.policyLinesFinder(node, getPolicyName(), cfgPath));
//                    break;
//                }
//                case ROUTE_INVALID: {
//                    // 判断是接口invalid还是下一跳ip无接口
//                    boolean ifRouteHasOrigin = false;
//
//                    break;
//
//                }
//                case NOT_BEST: {
//                    // 只可能是直连优于静态
//                    addErrorLine(ConfigTaint.staticRouteLinesFinder(node, staticRoute, cfgPath));
//                    addErrorLines(ConfigTaint.interfaceLinesFinder(connectedRoute.getNextHopInterface(),
//                            configuration.getAllInterfaces().get(connectedRoute.getNextHopInterface())
//                            , cfgPath));
//                    // @TODO: 找到import static的命令
//
//                }
//            }
//        });
//        return errorLines;
//    }
//
//    /**
//     * @return String return the node
//     */
//    public String getNode() {
//        return node;
//    }
//
//    /**
//     * @param node the node to set
//     */
//    public void setNode(String node) {
//        this.node = node;
//    }
//
//    /**
//     * @return StaticRoute return the targetRoute
//     */
//    public StaticRoute getStaticRoute() {
//        return staticRoute;
//    }
//    /**
//     * @return ConnectedRoute return the targetRoute
//     */
//    public ConnectedRoute getConnectedRoute() {
//        return connectedRoute;
//    }
//
//    /**
//     * @param staticRoute the targetRoute to set
//     */
//    public void setStaticRoute(StaticRoute staticRoute) {
//        this.staticRoute = staticRoute;
//    }
//
//    /**
//     * @return String[] return the causeKeyWords
//     */
//    public String[] getCauseKeyWords() {
//        return causeKeyWords;
//    }
//
//    /**
//     * @return Violation return the violation
//     */
//    public Violation getViolation() {
//        return violation;
//    }
//
//    /**
//     * @param violation the violation to set
//     */
//    public void setViolation(Violation violation) {
//        this.violation = violation;
//    }
//
//    /**
//     * @return BgpTopology return the bgpTopology
//     */
//    public BgpTopology getBgpTopology() {
//        return bgpTopology;
//    }
//
//    /**
//     * @param bgpTopology the bgpTopology to set
//     */
//    public void setBgpTopology(BgpTopology bgpTopology) {
//        this.bgpTopology = bgpTopology;
//    }
//
//
//    public Prefix getPrefix() {
//        if (staticRoute!=null) {
//            return staticRoute.getNetwork();
//        } else if (connectedRoute!=null) {
//            return connectedRoute.getNetwork();
//        }
//        assert false;
//        return null;
//    }
//
//
//    @Override
//    public Repairer genRepairer() {
//        // TODO Auto-generated method stub
//        RedistributionRepairer repairer = new RedistributionRepairer(this);
//        repairer.genRepair();
//        return repairer;
//    }
//
//
//
//}
