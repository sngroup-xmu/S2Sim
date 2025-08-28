package org.batfish.diagnosis.localization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.diagnosis.common.ConfigurationLine;
import org.batfish.diagnosis.repair.RedistributionRepairer;
import org.batfish.diagnosis.repair.Repairer;
import org.batfish.diagnosis.util.ConfigTaint;

/*
* Localize "violateRedis" errors (3 types):
* ==> Why the target route can not be redistributed to target routing process?
* 1) policy 【TODO 要不要把这个归类到forbidden的错里？】
* 2) "No Redistribution Config"
* 3) "inValid"
*/
public class RedistributionLocalizer extends Localizer {
   private String _node;
   private StaticRoute _staticRoute;
   private ConnectedRoute _connectedRoute;
   private String[] _causeKeyWords;
   private String _splitSymbol = ",";
   private BgpTopology _bgpTopology;
   List<RedisErrorType> _errorTypes;

   public String getCfgPath() {
       return _cfgPath;
   }

   public Prefix getForbiddenPrefix() {
       if (_staticRoute!=null) {
           return _staticRoute.getNetwork();
       } else {
           return _connectedRoute.getNetwork();
       }
   }

   private final String _cfgPath;
   private Configuration _configuration;

   public List<RedisErrorType> getErrorTypes() {
       return _errorTypes;
   }

   public enum RedisErrorType {
       NO_REDISTRIBUTE_COMMOND,
       ROUTE_INVALID,
       POLICY,
       NOT_BEST
   }

   private RedisErrorType getErrorTypeFromKeyWord(String causeKeyWord) {
     if (causeKeyWord==null) {
       return null;
     }
       causeKeyWord = causeKeyWord.toLowerCase();
       if (causeKeyWord.contains("no") && causeKeyWord.contains("config")) {
           return RedisErrorType.NO_REDISTRIBUTE_COMMOND;
       } else if (causeKeyWord.contains("invalid") || causeKeyWord.contains("invaild")) {
           return RedisErrorType.ROUTE_INVALID;
       } else if (causeKeyWord.contains("best")) {
           return RedisErrorType.NOT_BEST;
       } else {
           return RedisErrorType.POLICY;
       }
   }

   public String getPolicyName() {
       for (String word : _causeKeyWords) {
           if ( getErrorTypeFromKeyWord(word).equals(RedisErrorType.POLICY)) {
               return word.split(":")[1];
           }
       }
       return null;
   }

   public RedistributionLocalizer(String node, String causeKeyWord, StaticRoute staticRoute, ConnectedRoute connectedRoute,
                                  String configPath, Configuration configuration) {
       this._node = node;
       this._causeKeyWords = causeKeyWord.split(_splitSymbol);
       this._errorTypes = genErrorTypes();

       this._staticRoute = staticRoute;
       this._connectedRoute = connectedRoute;
       assert staticRoute!=null || connectedRoute!=null;

       this._cfgPath = configPath;

       this._configuration = configuration;
   }

   public List<RedisErrorType> genErrorTypes() {
       List<RedisErrorType> errList = new ArrayList<>();
       for (String word : _causeKeyWords) {
           errList.add(getErrorTypeFromKeyWord(word));
       }
       return errList;
   }

   @Override
   public List<ConfigurationLine> genErrorConfigLines() {
       // TODO Auto-generated method stub
       _errorTypes.forEach(n -> {
           switch (n) {
               case NO_REDISTRIBUTE_COMMOND: {
                  //重分发命令要区分静态和直连(如果是按流诊断的话应该不存在同时缺少两种命令的情况)
                  String redisCommand;
                  if(_staticRoute != null){
                    redisCommand = "redistribute static";
                  }
                  else{
                    redisCommand = "network " + _connectedRoute.getNetwork().getStartIp()
                                    + " mask " + _connectedRoute.getNetwork().getMask();
                  }
                  addErrorLine(new ConfigurationLine(-1, redisCommand));
                  break;
               }
               case POLICY: {
                  // 先找到调用ref policy的那一行；
                  // 由于无法获得真实的策略名，因此这里不把PolicyName作为关键字传入，为了防止找到neighbor的rp，这里把neighbor作为屏蔽词
                  // TODO 但适用于只有一种协议重分发的情况；若OSPF和BGP同时考虑重分发，需要考虑屏蔽另一个协议的配置
                  //String[] keWords = { "redistribute", getPolicyName() };
                  String[] keyWords = {"route-map"};
                  Map<Integer, String> rpLines = ConfigTaint.taintWithForbidWord(_node, keyWords, "neighbor", _cfgPath);
                  addErrorLines(rpLines);
                  //通过记录的配置内容获得实际的PolicyName
                  //    0               1             2      3
                  //redistribute static/connected route-map xxx
                  //    0   1   2    3      4      5
                  //network ip mask xxx route-map xxx
                  String policyName = null;
                  for(Integer i : rpLines.keySet()){
                    if(rpLines.get(i).contains("redistribute")){
                      policyName = rpLines.get(i).split(" ")[3];
                    }
                    else if(rpLines.get(i).contains("network")){
                      policyName = rpLines.get(i).split(" ")[5];
                    }
                  }
                  addErrorLines(ConfigTaint.policyLinesFinder(_node, policyName, _cfgPath));
                  break;
               }
               case ROUTE_INVALID: {
                    // 判断是接口invalid还是下一跳ip无接口
                  boolean ifRouteHasOrigin = false;
                  // 已知：inactive的接口如果作为静态路由下一跳，则该条静态路由不会被解析成功
                  // 输入的staticRoute是targetRoute
                  // 0. 先查找有没有这条前缀相关的静态路由命令ConfigTaint.staticRouteLinesFinder：
                  // 0.1 无则加入行号为-1的静态路由命令（由传入的静态路由生成），结束
                  // 0.2 若有则加入errorLines，继续在配置里搜索下一跳ip所在的接口配置ConfigTaint.interfaceLinesFinder：
                 ConfigurationLine staticCfg = ConfigTaint.staticRouteLinesFinder(_node,_staticRoute,_cfgPath);
                 if(staticCfg == null){
                   String redisCommand = ConfigTaint.genStaticRouteLine(_staticRoute);
                   addErrorLine(new ConfigurationLine(-1,redisCommand));
                 }
                 else{
                   addErrorLine(staticCfg);
                   /* 0  1     2      3        4
                    * ip route prefix mask { ip-address | interface-type interface-number [ ip-address ]}
                    */
                   //TODO: 现只考虑静态路由配置的出口接口，常见的还有下一跳ip；
                   String[] key = staticCfg.getLine().split(" ");
                   if(key.length > 4){
                     String nextHop = key[4];
                     List<ConfigurationLine> interfaceLines = ConfigTaint.interfaceLinesFinder(nextHop,_configuration.getAllInterfaces().get(_staticRoute.getNextHopInterface()),_cfgPath);
                     if(!interfaceLines.isEmpty()){
                       addErrorLines(interfaceLines);
                     }
                   }
                 }
                 break;
               }
               case NOT_BEST: {
                   // 只可能是直连优于静态
                   addErrorLine(ConfigTaint.staticRouteLinesFinder(_node, _staticRoute, _cfgPath));
                   addErrorLines(ConfigTaint.interfaceLinesFinder(_connectedRoute.getNextHopInterface(),
                           _configuration.getAllInterfaces().get(_connectedRoute.getNextHopInterface())
                           , _cfgPath));
                   // @TODO: 找到import static的命令
               }
           }
       });
       return _errorLines;
   }

   /**
    * @return String return the node
    */
   public String getNode() {
       return _node;
   }

   /**
    * @param node the node to set
    */
   public void setNode(String node) {
       this._node = node;
   }

   /**
    * @return StaticRoute return the targetRoute
    */
   public StaticRoute getStaticRoute() {
       return _staticRoute;
   }
   /**
    * @return ConnectedRoute return the targetRoute
    */

   public ConnectedRoute getConnectedRoute() {
       return _connectedRoute;
   }

   /**
    * @param staticRoute the targetRoute to set
    */
   public void setStaticRoute(StaticRoute staticRoute) {
       this._staticRoute = staticRoute;
   }

   /**
    * @return String[] return the causeKeyWords
    */
   public String[] getCauseKeyWords() {
       return _causeKeyWords;
   }

   /**
    * @return BgpTopology return the bgpTopology
    */
   public BgpTopology getBgpTopology() {
       return _bgpTopology;
   }

   /**
    * @param bgpTopology the bgpTopology to set
    */
   public void setBgpTopology(BgpTopology bgpTopology) {
       this._bgpTopology = bgpTopology;
   }


   public Prefix getPrefix() {
       if (_staticRoute!=null) {
           return _staticRoute.getNetwork();
       } else if (_connectedRoute!=null) {
           return _connectedRoute.getNetwork();
       }
       assert false;
       return null;
   }


   @Override
   public Repairer genRepairer() {
       // TODO Auto-generated method stub
       RedistributionRepairer repairer = new RedistributionRepairer(this);
       repairer.genRepair();
       return repairer;
   }



}
