package org.batfish.diagnosis.repair;

import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.StaticRoute;
import org.batfish.diagnosis.common.ConfigurationLine;
import org.batfish.diagnosis.localization.RedistributionLocalizer;
import org.batfish.diagnosis.util.ConfigTaint;

public class RedistributionRepairer extends Repairer{

   RedistributionLocalizer localizer;

   public RedistributionRepairer(RedistributionLocalizer localizer) {

       this.localizer = localizer;
   }

   private void routeInvalidRepair() {
       // voi rules 里面会记录valid的直连/静态路由，只是静态的outInf为null
       StaticRoute staticRoute = localizer.getStaticRoute();
       ConnectedRoute connectedRoute = localizer.getConnectedRoute();
       if (staticRoute!=null) {
           /*
              静态路由无效可能是因为：
            * 1)：直连父接口被shutdown了【只有物理接口可以被shutdown，子接口是逻辑接口】
            * 2)：下一跳无效
            */
           boolean ifNonRouting = staticRoute.getNonRouting();

       } else {
           /*
            * 直连路由无效可能是父接口被shutdown
            */
           int lineIndex;
           String infName = connectedRoute.getNextHopInterface();
       }
   }


   public void noRedistributeCommandRepair() {
       /*
        * there are several options to let a static/connected route be redistributed:
        * 1) network command
        * 2) redistribute static / connected
        */
       // 时间原因，都用1)方法
       generateNetworkCommand();
   }

   // policy forbid 已经在外面修过了
   public void policyRepair() {
//        Map<Integer, String> lines = ConfigTaint.taint(node, route.genRedistributionConfig(policyName).split(" "), localizer.getCfgPath());
//        assert lines.size()==1;
//        addAddedLine(lines.keySet().iterator().next(), ConfigTaint.genMissingNetworkConfigLine(route.getPrefix()));
   }

   private void generateNetworkCommand() {
       ConfigurationLine ipv4AddressFamilyLine = ConfigTaint.findBgpAddressFamilyLine(localizer.getCfgPath());
       addAddedLine(ipv4AddressFamilyLine.getLineNumber(), ConfigTaint.genMissingNetworkConfigLine(localizer.getPrefix()));
   }

   /**
    * 错在import的路由不是最优的
    *  检查【直连】还是【静态】路由更优，导入对应类型的路由
    * @TODO: 导入最优类型的路由
    */

   public void notBestRepair() {
       generateNetworkCommand();
   }

   @Override
   public void genRepair() {
       for (RedistributionLocalizer.RedisErrorType errorType: localizer.getErrorTypes()) {
           switch (errorType) {
               case NO_REDISTRIBUTE_COMMOND: noRedistributeCommandRepair(); break;
               case NOT_BEST: notBestRepair(); break;
               case POLICY: policyRepair(); break;
               case ROUTE_INVALID: routeInvalidRepair();
           }
       }
   }



}
