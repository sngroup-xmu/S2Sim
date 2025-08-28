package org.batfish.diagnosis.localization;


import java.util.List;
import java.util.Map;
import org.batfish.datamodel.AbstractRoute;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.routing_policy.Environment;
import org.batfish.datamodel.routing_policy.Result;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.datamodel.routing_policy.statement.Statement;
import org.batfish.diagnosis.common.BgpRouteLog;
import org.batfish.diagnosis.common.ConfigurationLine;
import org.batfish.diagnosis.repair.Repairer;
import org.batfish.diagnosis.repair.RouteForbiddenRepairer;
import org.batfish.diagnosis.util.ConfigTaint;
import org.batfish.diagnosis.util.KeyWord;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import org.checkerframework.checker.units.qual.C;

/*
* Localize "violatedPropNeighbors"/"violatedAcptNeighbors" errors
* 针对peer export/import policy的
* => Why a route can not be propagated expectedly?
* 1) policy (filtered)
* 2) vpn RT match
*/
public class RouteForbiddenLocalizer extends Localizer {

   String _node;
   //----------- 只有BGP路由传播时拦截才会有这两个字段 ------------
   private String _peerNodeIp;
   //----------- 只有BGP路由传播时拦截才会有这两个字段 ------------

   // 路由策略policy的名称（如果没有这个policy，那么就是vpn交叉失败导致forbid的）
   String _policyName;
   AbstractRoute _route;
   BgpRouteLog _bgpRouteLog;
   Bgpv4Route _bgpv4Route;
   Configuration _configuration;
   // direction是按照violatedRule类型设置的，绝对准确
   Direction _direction;
   // 这个表示没有正常建立peer关系的节点
   private String _cfgPath;


   public enum Direction {
       IN("in"),
       OUT("out"),
       REDISTRIBUTE("redistribute");
       String _name;
       Direction(String name) {
           this._name = name;
       }

       String getName() {
           return _name;
       }
   }

   public Bgpv4Route getBgpv4Route() {
       return _bgpv4Route;
   }

   public BgpRouteLog getBgpRouteLog() {
       return _bgpRouteLog;
   }

   public AbstractRoute getRoute() {
       return _route;
   }

   public String getPolicyName() {
       return this._policyName;
   }

   public String getPeerNodeIp() {
       return _peerNodeIp;
   }

   public Direction getDirection() {
       return _direction;
   }

   public String getNode() {
       return _node;
   }

   public Configuration getConfiguration() {
       return _configuration;
   }

   public RouteForbiddenLocalizer(String node, String peerNodeIp, BgpRouteLog bgpRouteLog,
                                  Direction direction, Configuration configuration, String cfgPath) {
       // 传入的bgp topo需要是假设的，不然可能找不到peer dev
       this._node = node;
       // 如果是因为export被deny的，在export那端会有记录
       this._bgpRouteLog = bgpRouteLog;
       this._direction = direction;
       this._cfgPath = cfgPath;
       this._peerNodeIp = peerNodeIp;
       this._configuration = configuration;

       if (direction.equals(Direction.IN)) {
         _policyName = bgpRouteLog.getImRoutePolicyName();
       } else {
         _policyName = bgpRouteLog.getExRoutePolicyName();
       }
   }

   public RouteForbiddenLocalizer(String node, AbstractRoute route, String policyName,
                                  Direction direction, Configuration configuration, String cfgPath) {
       // 传入的bgp topo需要是假设的，不然可能找不到peer dev
       this._node = node;
       // 如果是因为export被deny的，在export那端会有记录
       this._route = route;
       this._policyName = policyName;
       this._direction = direction;
       this._cfgPath = cfgPath;
       this._configuration = configuration;
   }

   @Override
   public List<ConfigurationLine> genErrorConfigLines() {
     // TODO 先检查是不是因为 policy filter route【可能是 peer不通或者路由交叉不了】
     assert _policyName!=null;

     // 因为策略会应用在整个peer-group，因此要先看配置中是否存在设置peer-group的命令，如果有，则通过匹配group 名在查找相应的配置
//     String[] keyWords = { "neighbor", _peerNodeIp, "peer-group"};
     //由于仿真中记录的策略名称已经被序列化，跟实际配置可能不一致，且思科配置中每个方向只能配置一条rp,因此这里不传入具体策略名
     String[] keyWords = { "neighbor", _peerNodeIp, KeyWord.ROUTE_POLICY, _direction.getName() };

     Map<Integer, String> peerRpLines = ConfigTaint.peerTaint(_node, keyWords, _cfgPath);
     addErrorLines(peerRpLines);
     //根据应用配置的命令查找真实的策略名
     //    0       1       2        3         4
     //neighbor PeerIP route-map RMap_G_to_A in/out
     String policyName = null;
     for(Integer i : peerRpLines.keySet()){
       if(peerRpLines.get(i).contains(KeyWord.ROUTE_POLICY)){
         policyName = peerRpLines.get(i).split(" ")[3];
       }
     }
     Map<Integer, String> policyLines = ConfigTaint.policyLinesFinder(_node, policyName, _cfgPath);
//     // 这里的matchedRuleOrder是从1开始的
//    int matchedRuleOrder = getTheMatchedRuleOrder();
//
//    if (matchedRuleOrder > 0) {
//        // add specific error lines
//        int i=0; // 当前遍历到第ii条rule
//        Boolean isMatched = false;
//        for (int lineNum: policyLines.keySet()) {
//            String line = policyLines.get(lineNum);
//            if (line.contains(KeyWord.ROUTE_POLICY)) {
//                i++;
//                // 当前遍历到的rule已经超出要找的那条
//                if (i > matchedRuleOrder) {
//                    break;
//                } else if (i == matchedRuleOrder) {
//                    isMatched = true;
//                    addErrorLine(lineNum, line);
//                }
//            }
//            if (isMatched) {
//                addErrorLine(lineNum, line);
//            }
//        }
//    } else {
//        addErrorLines(policyLines);
//    }

     addErrorLines(policyLines);
     return getErrorLines();
   }

   private int getTheMatchedRuleOrder() {
        // 暂时未支持非BGP路由拦截的配置
        if (_direction.equals(Direction.REDISTRIBUTE)) {
            return 0;
        }
        RoutingPolicy routingPolicy = _configuration.getRoutingPolicies().get(_policyName);
        Bgpv4Route bgpRoute = null;
        // 检查是第几条rule匹配的
        if (_bgpRouteLog.getJsonStr() != null) {
            // 把记录的BgpRoute字符串反序列化出来
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new GuavaModule());
            try {
                bgpRoute = objectMapper.readValue(_bgpRouteLog.getJsonStr(), Bgpv4Route.class);
                _bgpv4Route = bgpRoute;
            }
            catch (JsonMappingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch (JsonProcessingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        } else {
            bgpRoute = Bgpv4Route.builder()
            .setNetwork(_bgpRouteLog.getPrefix())
            // .setOriginatorIp(_bgpRouteLog.getOriginatorIp())
            // .setNextHopIp(_bgpRouteLog.getNextHopIp())
            // .setProtocol(_bgpRouteLog.getOriginProtocol())
            // .setAdmin(_bgpRouteLog.getAdmin())
            // .setLocalPreference(_bgpRouteLog.getLocalPreference())
            // .setClusterList(_bgpRouteLog.getClusterList())
            // .setCommunities(_bgpRouteLog.getCommunities())
            // .setTag(_bgpRouteLog.getTag())
            .build();
            return 0;
        }

        Environment environment = Environment.builder(_configuration).setOriginalRoute(bgpRoute).build();
        for (int i = 0; i < routingPolicy.getStatements().size(); i++) {
            Statement statement = routingPolicy.getStatements().get(i);
            Result result = statement.execute(environment);
            // if (result.getExit()) {
            // return i+1;
            // }
            if (result.getReturn()) {
                return i+1;
            }
        }
        return 0;
   }

   public Repairer genRepairer() {
        return new RouteForbiddenRepairer(this);
    }
}
