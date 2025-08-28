package org.batfish.diagnosis.localization;

import java.util.List;
import java.util.Map;
import org.batfish.datamodel.Configuration;
import org.batfish.diagnosis.common.BgpRouteLog;
import org.batfish.diagnosis.common.ConfigurationLine;
import org.batfish.diagnosis.repair.Repairer;
import org.batfish.diagnosis.util.ConfigTaint;
import org.batfish.diagnosis.util.KeyWord;

public class RoutePreferenceLocalizer extends Localizer {

//   BgpRouteLog _shouldPreferRoute;
//   BgpRouteLog _actualPreferRoute;
   String _node;
   List<BgpRouteLog> _betterRoutes;
   Configuration _configuration;
   private String _cfgPath;
   public RoutePreferenceLocalizer(String node, List<BgpRouteLog> betterRoutes, String cfgPath, Configuration configuration) {
      // 传入的bgp topo需要是假设的，不然可能找不到peer dev
      this._node = node;
      // 如果是因为export被deny的，在export那端会有记录
      this._betterRoutes = betterRoutes;
      this._cfgPath = cfgPath;
      this._configuration = configuration;
   }


   @Override
   public List<ConfigurationLine> genErrorConfigLines() {
      for(BgpRouteLog r : _betterRoutes){
         String peerIp = r.getPeerIpString();
         //对每条记录的路由查找有没有配置route-map的命令
         String[] keyWords = { "neighbor", peerIp, KeyWord.ROUTE_POLICY, "in" };
         Map<Integer, String> peerRpLines = ConfigTaint.peerTaint(_node, keyWords, _cfgPath);
         //存在策略，根据配置命令获得策略名，并查找相关的策略
         if(!peerRpLines.isEmpty()){
            addErrorLines(peerRpLines);
            //根据应用配置的命令查找真实的策略名
            //    0       1       2        3         4
            //neighbor PeerIP route-map RMap_G_to_A in/out
            String policyName = null;
            for(Integer i : peerRpLines.keySet()){
               if(peerRpLines.get(i).contains(KeyWord.ROUTE_POLICY)){
                  policyName = peerRpLines.get(i).split(" ")[3];
               }
               addErrorLines(ConfigTaint.policyLinesFinder(_node, policyName, _cfgPath));
            }
         }
      }

      return getErrorLines();
   }

   @Override
   public Repairer genRepairer() {
       // TODO Auto-generated method stub
         return null;
//       throw new UnsupportedOperationException("Unimplemented method 'genRepairer'");
   }

}
