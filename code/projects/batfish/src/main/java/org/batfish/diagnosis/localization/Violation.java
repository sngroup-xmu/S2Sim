package org.batfish.diagnosis.localization;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.batfish.datamodel.Configuration;
import org.batfish.datamodel.ConnectedRoute;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.StaticRoute;
import org.batfish.diagnosis.common.BgpPeerLog;
import org.batfish.diagnosis.common.BgpRouteLog;
import org.batfish.diagnosis.common.ConfigurationLine;
import org.batfish.diagnosis.reference.BgpGenerator;
import org.batfish.diagnosis.util.InputData;


// 每个设备一个violation实例


public class Violation implements Serializable {
   @SerializedName("ipPrefix")
   String _ipPrefixString;

   String _vpnName;
   Set<BgpPeerLog> _violatedRrClient = new HashSet<>();
   List<BgpRouteLog> _violatedPropNeighbors = new ArrayList<>();
   List<BgpRouteLog> _violatedAcptNeighbors = new ArrayList<>();
   // prefer 的表示:记录所有优先级大于selected route的路由
   List<BgpRouteLog> _violatedPreferRoutes = new ArrayList<>();
   Map<String,String> _violateIbgpPeer = new HashMap<>();
   Map<String, String> _violateEbgpPeer = new HashMap<>();
   StaticRoute _originStaticRoute;
   ConnectedRoute _originDirectRoute;

   // 描述redis失败的原因的字符串，用逗号分隔多个原因
   String _violateRedis = null;
   // 自身节点的所有localizer
   Set<Localizer> _localizers = new HashSet<>();

   public String getVpnName() {
       return _vpnName;
   }

   public String getIpPrefixString() {return _ipPrefixString;}

   public void setIpPrefixString(String ipPrefixString) {this._ipPrefixString = ipPrefixString;}

   public String getViolateRedis() {return _violateRedis;}

   public void setViolateRedis(String violateRedis){
     if(this._violateRedis == null){
       this._violateRedis = violateRedis;
     }
     else{
       this._violateRedis = this._violateRedis + ',' + violateRedis;
     }
   }

   public StaticRoute getOriginStaticRoute() {return _originStaticRoute;}

   public void setOriginStaticRoute(StaticRoute staticRoute) {this._originStaticRoute = staticRoute;}

   public ConnectedRoute getOriginDirectRoute() {return _originDirectRoute;}

   public void setOriginDirectRoute(ConnectedRoute connectedRoute) {this._originDirectRoute = connectedRoute;}

   /*
    * 对violation里的各种violated rules去重
    */
   public void preProcessOfViolation() {
       // STEP 1: 先处理重复的violatedPropNeighbors/violatedAcptNeighbors;
   }

   public void addViolateEbgpPeer(String node, String peerIp) {
       _violateEbgpPeer.put(node, peerIp);
   }

   public void addViolateIbgpPeer(String node, String peerIp) {
       _violateIbgpPeer.put(node, peerIp);
   }

   public static <T> boolean ifListValid(List<T> aimList) {
       if (aimList == null || aimList.size() < 1) {
           return false;
       }
       return true;
   }

   public static <T> boolean ifSetValid(Set<T> aimList) {
       if (aimList == null || aimList.size() < 1) {
           return false;
       }
       return true;
   }

   public List<BgpRouteLog> getViolatedPreferRoutes() { return _violatedPreferRoutes; }

   public List<BgpRouteLog> getViolatedPropNeighbors() {
       return _violatedPropNeighbors;
   }

   public List<BgpRouteLog> getViolatedAcptNeighbors() {
       return _violatedAcptNeighbors;
   }

   public void addViolatedPreferRoutes(BgpRouteLog violatedPreferRoute){
    this._violatedPreferRoutes.add(violatedPreferRoute);
   }

   public void addViolatedAcptNeighbors(BgpRouteLog violatedAcptNeighbor){
     this._violatedAcptNeighbors.add(violatedAcptNeighbor);
   }

   public void addViolatedPropNeighbors(BgpRouteLog violatedPropNeighbor){
     this._violatedPropNeighbors.add(violatedPropNeighbor);
   }


   public Map<String, String> getViolateEbgpPeers() {
       return _violateEbgpPeer;
   }

   public Map<String, String> getViolateIbgpPeers() {
       return _violateIbgpPeer;
   }

   public Set<Localizer> getLocalizers() {
       return _localizers;
   }

   public void addResults(Localizer localizer) {
       _localizers.add(localizer);
   }

   /*
    * 如果之前有repairer使用过同样的policy，则把相应的routePolicy返回，新的routeForbiddenRepairer引用它
    */
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

//  //序列化违规信息，并输出到文件
//  public static <T> void serializeToFile(String filePath,T object) {
//    Gson gson = new GsonBuilder().serializeNulls().create();
//    String jsonString = gson.toJson(object);
//    // System.out.println(jsonString);
//    try{
//      File file = new File(filePath);
//      if(!file.getParentFile().exists()){
//        //若父目录不存在则创建父目录
//        file.getParentFile().mkdirs();
//      }
//      if(file.exists()){
//        file.delete();
//      }
//      file.createNewFile();
//      Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
//      writer.write(jsonString);
//      writer.flush();
//      writer.close();
//    } catch (IOException e){
//      e.printStackTrace();
//    }
//  }
//
//  public static String fromJsonToString(String filePath) {
//    File file = new File(filePath);
//    String jsonStr = "";
//    if(file.exists()){
//      try {
//        jsonStr = FileUtils.readFileToString(file,"UTF-8");
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
//    }
//    return jsonStr;
//  }
//
//  // 把peer connection这种双向的错误分发到两端的设备上
//  public static Map<String, Violation> genViolationsFromFile(String filePath) {
//    Map<String, Violation> violations;
//    String jsonStr = fromJsonToString(filePath);
//
//    if (jsonStr==null) {
//      throw new IllegalArgumentException("There is no violated rules file! " + "(" + filePath + ")");
//    }
//    String jsonObject = JsonParser.parseString(jsonStr).getAsJsonObject().toString();
//    violations = new Gson().fromJson(jsonStr, new TypeToken<Map<String, Violation>>() {}.getType());
//
//
//    for (String node : new HashSet<>(violations.keySet())) {
//      Violation vio = violations.get(node);
//      if (Violation.ifSetValid(vio.getViolateEbgpPeers())) {
//        vio.getViolateEbgpPeers().forEach(neighbor->{
//          if (!violations.containsKey(neighbor)) {
//            violations.put(neighbor, new Violation());
//          }
//          violations.get(neighbor).addViolateEbgpPeer(node);
//        });
//      }
//      if (Violation.ifSetValid(vio.getViolateIbgpPeers())) {
//        vio.getViolateIbgpPeers().forEach(neighbor->{
//          if (!violations.containsKey(neighbor)) {
//            violations.put(neighbor, new Violation());
//          }
//          violations.get(neighbor).addViolateIbgpPeer(node);
//        });
//      }
//    }
//    return violations;
//  }

  public List<ConfigurationLine> getErrorLinesForSingleNode(String curDevName, String configPath, Configuration curConfiguration, Prefix reqDstPrefix, BgpGenerator generator) {

       // 未配置反射客户的错误
       if (ifSetValid(_violatedRrClient)) {
           _localizers.add(new ReflectClientLocalizer(curDevName, _violatedRrClient, configPath));
       }

       /*
        未能正常接收目标路由，有以下原因：
          1）策略拦截：如果importPolicy字段非空
          2）非策略拦截原因：
            * a）peer关系未建立
            * b）VPN交叉失败
            * c）迭代不到隧道
            *
            * 先按a）处理
       */
       //确认import policy记录的格式，是否记在BgpRpute里，如果和华为的格式一样，则无需更换成BF的Bgpv4Route
       if (ifListValid(_violatedAcptNeighbors)) {
           _violatedAcptNeighbors.forEach(n -> {
             if (n.getImRoutePolicyName()!=null) {
               RouteForbiddenLocalizer routeForbiddenLocalizer = new RouteForbiddenLocalizer(curDevName, n.getPeerIpString(), n,
                   RouteForbiddenLocalizer.Direction.IN, curConfiguration, configPath);
               _localizers.add(routeForbiddenLocalizer);
             } else {
               String peer = n.getFromDeviceName();
               assert false: "Other Mistakes make the route cannot be accepted";

             }
           });
       }

       /*
        未能正常发送目标路由，有以下原因：
          1）策略拦截：如果exportPolicy字段非空【还要再检查newGenerator里prop的节点是否已有默认路由，有默认路由就默认这个violatedProp是误报】
          2）非策略拦截原因：
            * a）peer关系未建立
            * c）迭代不到隧道
            *
       */
       if (ifListValid(_violatedPropNeighbors)) {
           _violatedPropNeighbors.forEach(n -> {
               // 当前条目的prefix要包含dstPrefix才继续下一步
             if (n.getExRoutePolicyName()!=null) {
               // 检测是否已有默认路由(华为）
               //                       Bgpv4Route bgpRouteInNewGenerator = newGenerator.getBgpTree().getBestBgpRoute(n.getToDeviceName());
               //                       if (bgpRouteInNewGenerator==null || !bgpRouteInNewGenerator.getNetwork().containsPrefix(Prefix.ZERO)) {
               RouteForbiddenLocalizer routeForbiddenLocalizer = new RouteForbiddenLocalizer(curDevName, n.getPeerIpString(), n,
                   RouteForbiddenLocalizer.Direction.OUT, curConfiguration, configPath);
               _localizers.add(routeForbiddenLocalizer);
             } else {
               String peer = n.getToDeviceName();
               assert false: "Other Mistakes make the route cannot be propagated";
             }

           });
       }

       //@TODO: Bgp Peer
        if (!_violateEbgpPeer.isEmpty()) {
            _violateEbgpPeer.forEach((peerName, IpStr) -> {
                _localizers.add(new PeerLocalizer(curDevName, peerName, generator, this, generator.getBgpTopology()));
            });
        }

        if (!_violateIbgpPeer.isEmpty()) {
            _violateIbgpPeer.forEach((peerName, IpStr) -> {
                _localizers.add(new PeerLocalizer(curDevName, peerName, generator, this, generator.getBgpTopology()));
            });
        }

        //Redistribution
       if (_violateRedis != null && !_violateRedis.equals("")) {
           //if (originStaticRoute != null || originDirectRoute!=null) {
           RedistributionLocalizer redistributionLocalizer = new RedistributionLocalizer(curDevName, _violateRedis, _originStaticRoute, _originDirectRoute,
                   configPath, curConfiguration);
           _localizers.add(redistributionLocalizer);
       }

       //Prefer path
       if(ifListValid(_violatedPreferRoutes)){
//         _violatedPreferRoutes.forEach(r -> {
//           if (r.getImRoutePolicyName()!=null) {
//             RouteForbiddenLocalizer routeForbiddenLocalizer = new RouteForbiddenLocalizer(curDevName,
//                 r.getPeerIpString(),
//                 r,
//                 RouteForbiddenLocalizer.Direction.IN,
//                 curConfiguration,
//                 configPath);
//             _localizers.add(routeForbiddenLocalizer);
//           }
           RoutePreferenceLocalizer preferenceLocalizer = new RoutePreferenceLocalizer(curDevName, _violatedPreferRoutes,configPath, curConfiguration);
           _localizers.add(preferenceLocalizer);
//         });
       }

       List<ConfigurationLine> lineMap = new ArrayList<>();
       _localizers.forEach(n -> {
           lineMap.addAll(n.genErrorConfigLines());
       });
       return lineMap;
   }

   public static void main(String[] args) {
       String path = "/home/dell/hg/batfish-repair-/viorules.json";
       File file = new File(path);
       String jsonStr = InputData.getStr(file);
       String jsonObject = JsonParser.parseString(jsonStr).getAsJsonObject().toString();
       Violation violation = new Gson().fromJson(jsonObject, Violation.class);
       violation.getErrorLinesForSingleNode("basel", "/home/dell/hg/batfish-repair-/yrl-sse-data/networks/Bics_abs_simple_2_old/configs/basel.cfg",
                                            null, null, null);
       System.out.println();
    }

}
