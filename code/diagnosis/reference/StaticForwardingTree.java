//package org.batfish.diagnosis.reference;
//
//import org.batfish.common.topology.Layer2Topology;
//import org.batfish.datamodel.Prefix;
//import org.batfish.datamodel.StaticRoute;
//
//import java.util.*;
//
//public class StaticForwardingTree {
//    private String _vpnName;
//
//    private Map<String, String> _nextHopForwardingMap;
//
//    private Prefix _dstPrefix;
//    private String _dstDevName;
//
//    private Map<String, List<StaticRoute>> _routesMap;
//    private Map<String, StaticRoute> _bestRouteMap;
//
//    public StaticForwardingTree(String dstDev, Prefix prefix, String vpnName) {
//        _dstDevName = dstDev;
//        _dstPrefix = prefix;
//        _nextHopForwardingMap = new HashMap<>();
//        _routesMap = new HashMap<>();
//        _vpnName = vpnName;
//        _bestRouteMap = new HashMap<>();
//    }
//
//
//
//
//    public String getNextHop(String node) {
//        return _nextHopForwardingMap.get(node);
//    }
//
//    public StaticRoute getBestRoute(String node) {
//        if (_bestRouteMap.containsKey(node)) {
//            return _bestRouteMap.get(node);
//        }
//        throw new IllegalArgumentException("Node (" + node + ")" + "has not target static route");
//    }
//
//    public void addForwardingInfo(String node, StaticRoute curRoute, Layer2Topology layer2Topology) {
//        if (!_routesMap.containsKey(node)) {
//            _routesMap.put(node, new ArrayList<StaticRoute>());
//        }
//        _routesMap.get(node).add(curRoute);
//
//        if (_bestRouteMap.containsKey(node)) {
//            // 根据layer2 topo，把端口对应的邻接点名称加入map，如果没有对端设备，nextHop还是用接口名称
//            if (curRoute.getMetric()>_bestRouteMap.get(node).getMetric()) {
//                //@TODO: 在layer2 Topology上查找
//                String nextNode = "next-hop";
//                _nextHopForwardingMap.put(node, curRoute.getNextHopInterface());
//                _bestRouteMap.put(node, curRoute);
//            }
//
//        }
//
//    }
//
//}
