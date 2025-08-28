//package org.batfish.diagnosis.repair;
//
//
//import org.batfish.diagnosis.localization.InconsistentRouteLocalizer;
//
//public class InconsistentRouteRepairer extends Repairer {
//
//    InconsistentRouteLocalizer localizer;
//
//    public InconsistentRouteRepairer(InconsistentRouteLocalizer localizer) {
//        this.localizer = localizer;
//    }
//    @Override
//    public void genRepair() {
//        // 修复就是把这些影响转发的静态路由/直连端口【删除配置】
//        addDeletedLines(localizer.getErrorLines());
//    }
//
//}
