//package org.batfish.diagnosis.repair;
//
//import org.batfish.datamodel.AbstractRoute;
//import org.batfish.datamodel.Configuration;
//import org.batfish.datamodel.Prefix;
//import org.batfish.datamodel.routing_policy.RoutingPolicy;
//import org.batfish.diagnosis.localization.RouteForbiddenLocalizer;
//
//public class RouteForbiddenRepairer extends Repairer {
//    RouteForbiddenLocalizer localizer;
//
//    public AbstractRoute getForbiddenRoute() {
//        return localizer.getRoute();
//    }
//
//    //TODO: 考虑同一个policy多次被localized，生成repair的时候序号不要重叠
//    public RouteForbiddenRepairer(RouteForbiddenLocalizer localizer) {
//        this.localizer = localizer;
//    }
//
//
//    public String getPolicyName() {
//        return localizer.getPolicyName();
//    }
//
//
//    /*
//     * 修复步骤
//       STEP 0: 找到拦截的那条规则：
//       *       只考虑
//       *        a) [ip-prefix ... deny + route-policy ... permit] / [ip-prefix ... permit + route-policy ... deny]
//       *        b) [ip-prefix ... deny + route-policy ... deny]
//       *       最终都是找到那条explicitly match 的 ip-prefix + route-policy 改成permit
//       STEP 1: 没有这样一条完全匹配的ip规则拦截，则在route-policy最前面新添规则
//       STEP 2: 如何在现有配置上改动最小（可以都先按STEP 0做 然后压缩配置？）
//     *
//     */
//    @Override
//    public void genRepair() {
//
//        Configuration configuration = localizer.getConfiguration();
//        Prefix prefix = localizer.getRoute().getNetwork();
//
//        // STEP 0.1: 找到拦截的那条route-policy规则
//        String policyName = localizer.getPolicyName();
//        if (policyName!=null) {
//            RoutingPolicy routingPolicy = configuration.getRoutingPolicies().get(policyName);
//        } else {
//            ;
//        }
//        // STEP 0.2: 找到拦截的那条ip-prefix规则
//
//
//        // STEP 1: 没有这样一条完全匹配的ip规则拦截，则在route-policy最前面新添规则【startLine是route-policy最前面】
//        int startLine;
//
//    }
//
//}
