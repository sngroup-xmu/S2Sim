package org.batfish.ghost;

import static java.util.Objects.requireNonNull;
import static org.batfish.datamodel.routing_policy.Environment.Direction.IN;
import static org.batfish.dataplane.protocols.BgpProtocolHelper.transformBgpRouteOnImport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.BgpPeerConfig;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
import org.batfish.datamodel.Bgpv4Route;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.RoutingProtocol;
import org.batfish.datamodel.bgp.AddressFamily;
import org.batfish.datamodel.bgp.AddressFamily.Type;
import org.batfish.datamodel.bgp.BgpTopology;
import org.batfish.datamodel.bgp.BgpTopology.EdgeId;
import org.batfish.datamodel.routing_policy.Environment.Direction;
import org.batfish.datamodel.routing_policy.RoutingPolicy;
import org.batfish.dataplane.ibdp.BgpRoutingProcess;
import org.batfish.dataplane.ibdp.Node;
import org.batfish.dataplane.protocols.BgpProtocolHelper;


public class GhostNode {
    private Node _node;
    private BgpRoutingProcess _bgpRoutingProcess;
    // The RIBIn of this node
    private Map<String, List<GhostBgpv4Route>> _ghostMap;

    // the Map indicates the best route(s) of this node at different trace/condition
    private Map<Integer, GhostBgpv4Route> _selectMap;
    // the update condition numbers of this node in the latest iteration
    private List<Integer> _updateCondList;
    // the update routes from different conditions of this node in the latest iteration
    private List<GhostBgpv4Route> _updateRouteList;
    // the above three member variables are must consistent with each other
    
    private Map<Integer, Condition> _conditions;
    
    private List<AsSet> _selectionAsPath;
    // By default, a node needs to propagate (only) selection path to prop neighbors.
    private List<String> _propagationNeighbors;
    private List<String> _acceptNeighbors;
    private String _hostName;

    public GhostNode(Node node, Map<Integer, Condition> conds) {
        _node = node;
        _bgpRoutingProcess = _node.getVirtualRouterYrl("default").getBgpRoutingProcessYrl();
        _ghostMap = new HashMap<String, List<GhostBgpv4Route>>();
        _selectMap = new HashMap<>();
        _updateCondList = new ArrayList<>();
        _updateRouteList = new ArrayList<GhostBgpv4Route>();
        _conditions = conds;
        _hostName = node.getConfiguration().getHostname();
        //
        initGhostReq();
    }

    public String getHostName() {
        return _hostName;
    }

    private void initGhostReq() {
        _selectionAsPath = new ArrayList<>();
        _propagationNeighbors = new ArrayList<>();
        _acceptNeighbors = new ArrayList<>();
    }

    public void setSelectionAsPath(List<String> asPath, Map<String, Long> asMap) {
        if (_selectionAsPath.size()>0) {
            assert asPath.size()==_selectionAsPath.size();
            return;
        }
        for (String string : asPath) {
            _selectionAsPath.add(AsSet.of(asMap.get(string)));
        }
    }

    public void addPropNeighbor(String n) {
        if (!_propagationNeighbors.contains(n)) {
            _propagationNeighbors.add(n);
        }
    }

    public void clearAssignment() {
        _propagationNeighbors.clear();
        _acceptNeighbors.clear();
        _selectionAsPath.clear();
    }

    public void addAcptNeighbor(String n) {
        if (!_acceptNeighbors.contains(n)) {
            _acceptNeighbors.add(n);
        }
    }
    
    public Node getNode() {
        return _node;
    }

    // public List<GhostBgpv4Route> getRoutes(){
    //     return _routes;
    // }
    public boolean isMatchSelectionAsPath(List<AsSet> asSets) {
        if (asSets.size() != _selectionAsPath.size()) {
            return false;
        }
        for (int i=0; i < asSets.size(); i++) {
            if (!asSets.get(i).equals(_selectionAsPath.get(i))) {
                return false;
            }
        }
        return true;
    }

    public List<GhostBgpv4Route> getUpdList() {
        return _updateRouteList;
    }

    public void addUpdateCond(int condNum) {
        _updateCondList.add(condNum);
        
    }

    public void initForDstNode(Prefix prefix) {
        Set<Bgpv4Route> dstLocalBgpRoutes = _node.getVirtualRouterYrl("default").getBgpRoutingProcessYrl().getV4LocalRoutesYrl();
        for (Bgpv4Route bgpv4Route : dstLocalBgpRoutes) {
            if (bgpv4Route.getNetwork().equals(prefix)) {
                GhostBgpv4Route originRoute = new GhostBgpv4Route(bgpv4Route);
                originRoute.addExistCond(0);
                originRoute.addSelectedCond(0);
                originRoute.setIsMatch(true);
                _updateRouteList.add(originRoute);
                _updateCondList.add(0);
                _selectMap.put(0, originRoute);
                // For dst node, we need synchronize the RIBIn and selection Map, 
                // so that when a new condition comes up, the ExistList of the origin route can be updated
                List<GhostBgpv4Route> ll = new ArrayList<>();
                ll.add(originRoute);
                _ghostMap.put(_hostName, ll);
            }
        }
        // BgpProtocolHelper.convertNonBgpRouteToBgpRoute()
    }

    public boolean isUpdated() {
        return (_updateCondList.size()>0);
    }


    public boolean isUpdateCond(int condNum) {
        return _updateCondList.contains(condNum);
    }

    public void initForIteration() {
        // this two should always consistent
        _updateCondList.clear();
        _updateRouteList.clear();
    }

    public Optional<GhostBgpv4Route> getMaxCondSelectedRoute() {
        if (_selectMap.size()>0) {
            return Optional.of((GhostBgpv4Route)_selectMap.values().toArray()[_selectMap.size()-1]);
        } else {
            return Optional.empty();
        }
        
    }
    public Optional<GhostBgpv4Route> getCondSelectedRoute(int condNum) {
        if (_selectMap.containsKey(condNum)) {
            return Optional.of(_selectMap.get(condNum));
        } else {
            return Optional.empty();
        }
        
    }

    public void updateCondForEachRoute(int condNum, boolean ifUpdateSelection) {
        // if a route exists in the latest condition, we think it will exist in the new condition, temporarily.
        // if a route is not be selected in the latest cond, it will not be selected in the new cond.
        if (ifUpdateSelection) {
            if (_selectMap.size()>0) {
                getMaxCondSelectedRoute().get().addSelectedCond(condNum);
                _selectMap.put(condNum, getMaxCondSelectedRoute().get());
            }
            
        }
        for (List<GhostBgpv4Route> routes : _ghostMap.values()) {
            // add condNum to ribIn 
            for (GhostBgpv4Route r : routes) {
                // theoratically, there won't exist a route which has exist/select falgs like: ...i-1, i+1...,
                // because cond(i) is a subset of cond(i+1)
                r.addExistCond(condNum); 
            }
        }
    }

    public GhostBgpv4Route getIterUpdateRoute(int condNum) {
        for (GhostBgpv4Route ghostBgpv4Route : _updateRouteList) {
            if (ghostBgpv4Route.isSelectedAtCond(condNum)) {
                return ghostBgpv4Route;
            }
        }
        return null;
    }

    public Optional<GhostBgpv4Route> importFromNode(GhostBgpv4Route remoteGhostRoute,  
                                                    EdgeId edgeId, 
                                                    BgpTopology bgpTopology, 
                                                    NetworkConfigurations nc, 
                                                    Map<String, Node> allNodes,
                                                    GhostComputation ghostComputation) {
        // Setup helper vars


        BgpPeerConfigId remoteConfigId = edgeId.tail();
        BgpPeerConfigId ourConfigId = edgeId.head();
        BgpSessionProperties sessionProperties = BgpRoutingProcess.getBgpSessionPropertiesYrl(bgpTopology, new EdgeId(remoteConfigId, ourConfigId));
        BgpPeerConfig ourBgpConfig = requireNonNull(nc.getBgpPeerConfig(edgeId.head()));
        assert ourBgpConfig.getIpv4UnicastAddressFamily() != null;
        BgpPeerConfig ourConfig = nc.getBgpPeerConfig(edgeId.tail());
        BgpPeerConfig remoteConfig = nc.getBgpPeerConfig(edgeId.head());
        BgpRoutingProcess remoteBgpRoutingProcess = BgpRoutingProcess.getNeighborBgpProcessYrl(remoteConfigId, allNodes);
        BgpSessionProperties session = BgpRoutingProcess.getBgpSessionPropertiesYrl(bgpTopology, edgeId);
        
        // is the accepted route from accepted neighbor detection
        

        // Default BGP import policy: loop detection
        Bgpv4Route.Builder transformedIncomingRouteBuilder =
            transformBgpRouteOnImport(
                remoteGhostRoute.getBgpv4Route(),
                sessionProperties.getHeadAs(),
                ourBgpConfig
                    .getIpv4UnicastAddressFamily()
                    .getAddressFamilyCapabilities()
                    .getAllowLocalAsIn(),
                sessionProperties.isEbgp(),
                _bgpRoutingProcess.getProcess(),
                sessionProperties.getTailIp(),
                ourConfigId.getPeerInterface());

        if (transformedIncomingRouteBuilder==null) {
            return Optional.empty();
        }

        // Process route through import policy, if one exists
        String importPolicyName = ourBgpConfig.getIpv4UnicastAddressFamily().getImportPolicy();
        boolean acceptIncoming = true;
        // TODO: ensure there is always an import policy
        if (importPolicyName != null) {
            RoutingPolicy importPolicy = _bgpRoutingProcess.getRoutingPolicies().get(importPolicyName).orElse(null);
            if (importPolicy != null) {
            acceptIncoming =
                importPolicy.processBgpRoute(
                    remoteGhostRoute.getBgpv4Route() , transformedIncomingRouteBuilder, sessionProperties, IN);
            }
        }

        boolean isMatchFlag = isMatchSelectionAsPath(remoteGhostRoute.getBgpv4Route().getAsPath().getAsSets());
        if (!acceptIncoming) {
            // if there needs a new condition
            if (isMatchFlag && _acceptNeighbors.contains(edgeId.tail().getHostname())) {
                ghostComputation.addCondition(new Condition(
                    ConditionType.ROUTEFILTER_IN, remoteGhostRoute.getBgpv4Route(), null, 
                    edgeId.head().getHostname(), edgeId.tail().getHostname()));
            } else {
                return Optional.empty();
            }
        }
        Bgpv4Route transformedIncomingRoute = transformedIncomingRouteBuilder.build();
        remoteGhostRoute.setBgpv4Route(transformedIncomingRoute);
        // set isMatchFlag during import process, 
        remoteGhostRoute.setIsMatch(isMatchFlag);
        // importedRoute.setExistCondListCopy(remoteGhostRoute.getSelectedConsList());
        return Optional.of(remoteGhostRoute);                
    }

    private Optional<GhostBgpv4Route> exportSingleRouteToNeighbor(
        GhostBgpv4Route exportCandidate,
        BgpPeerConfigId ourConfigId,
        BgpPeerConfigId remoteConfigId,BgpPeerConfig ourConfig,BgpPeerConfig remoteConfig,
        BgpRoutingProcess remoteBgpRoutingProcess,
        BgpSessionProperties sessionProperties,
        AddressFamily.Type afType,
        GhostComputation ghostComputation) {
        // the input route: exportCandidate is a route in this exported node, 
        // and it will be (deeep) copyed in this function, 
        // so that the return/output route can be processed further in remote node and will not influence the original copy in this node 

        AddressFamily addressFamily = ourConfig.getAddressFamily(afType);
        String exportPolicyName = addressFamily.getExportPolicy();
        assert exportPolicyName != null; // Conversion guarantee
        RoutingPolicy exportPolicy = _bgpRoutingProcess.getRoutingPolicies().get(exportPolicyName).orElse(null);
        assert exportPolicy != null; // Conversion guarantee

        Bgpv4Route.Builder transformedOutgoingRouteBuilder =
            BgpProtocolHelper.transformBgpRoutePreExport(
                ourConfig,
                remoteConfig,
                sessionProperties,
                _bgpRoutingProcess.getProcess(),
                remoteBgpRoutingProcess.getProcess(),
                exportCandidate.getBgpv4Route(),
                addressFamily.getType());

        if (transformedOutgoingRouteBuilder == null) {
        // This route could not be exported for core bgp protocol reasons
        return Optional.empty();
        }


        // Process transformed outgoing route by the export policy
        boolean shouldExport =
            exportPolicy.processBgpRoute(
                exportCandidate.getBgpv4Route(), transformedOutgoingRouteBuilder, sessionProperties, Direction.OUT);

        // sessionProperties represents the incoming edge, so its tailIp is the remote peer's IP
        Ip remoteIp = sessionProperties.getTailIp();

        if (!shouldExport) {
        // This route could not be exported due to export policy
            if (isMatchSelectionAsPath(exportCandidate.getBgpv4Route().getAsPath().getAsSets()) && _propagationNeighbors.contains(remoteConfigId.getHostname())) {
                ghostComputation.addCondition(new Condition(
                    ConditionType.ROUTEFILTER_OUT, exportCandidate.getBgpv4Route(), null, 
                    ourConfigId.getHostname(), remoteConfigId.getHostname()));
                // exportCandidate.setExistCond(new HashSet<Integer>());
                // return Optional.empty();
            } else {
                return Optional.empty();
            }
        }
        // Apply final post-policy transformations before sending advertisement to neighbor
        BgpProtocolHelper.transformBgpRoutePostExport(
            transformedOutgoingRouteBuilder,
            sessionProperties.isEbgp(),
            sessionProperties.getConfedSessionType(),
            sessionProperties.getHeadAs(),
            sessionProperties.getHeadIp(),
            exportCandidate.getBgpv4Route().getNextHopIp());
        // Successfully exported route
        Bgpv4Route transformedOutgoingRoute = transformedOutgoingRouteBuilder.build();

        GhostBgpv4Route gRoute = new GhostBgpv4Route(transformedOutgoingRoute);
        if (!shouldExport) {
            Set<Integer> set = new HashSet<Integer>();
            set.add(ghostComputation.getMaxCondNum());
            gRoute.setExistCondList(set);
        } else {
            gRoute.setExistCondListCopy(exportCandidate.getExistCondsList());
        }

        return Optional.of(gRoute);       

    }

    /**
     * @param edgeId
     * @param bgpTopology
     * @param nc
     * @param allNodes
     * @return
     */
    public Stream<GhostBgpv4Route> exprotToNeighbor(EdgeId edgeId, 
                                                    BgpTopology bgpTopology, 
                                                    NetworkConfigurations nc,
                                                    Map<String, Node> allNodes, 
                                                    GhostComputation ghostComputation) {
        // do not change ghost variables here
        // Setup helper vars

    
        //
        String remoteHost = edgeId.tail().getHostname();
        String ourHost = edgeId.head().getHostname();

        BgpPeerConfigId remoteConfigId = edgeId.tail();
        BgpPeerConfigId ourConfigId = edgeId.head();
        // BgpSessionProperties sessionProperties = BgpRoutingProcess.getBgpSessionPropertiesYrl(bgpTopology, new EdgeId(remoteConfigId, ourConfigId));
        BgpPeerConfig ourBgpConfig = requireNonNull(nc.getBgpPeerConfig(edgeId.tail()));
        assert ourBgpConfig.getIpv4UnicastAddressFamily() != null;
        BgpPeerConfig ourConfig = nc.getBgpPeerConfig(edgeId.head());
        BgpPeerConfig remoteConfig = nc.getBgpPeerConfig(edgeId.tail());
        BgpRoutingProcess remoteBgpRoutingProcess = BgpRoutingProcess.getNeighborBgpProcessYrl(remoteConfigId, allNodes);
        BgpSessionProperties session = BgpRoutingProcess.getBgpSessionPropertiesYrl(bgpTopology, edgeId);
        
        // Ip remoteIp = sessionProperties.getTailIp();

        List<GhostBgpv4Route> exportedRoutes = new ArrayList<>();

        if (_updateCondList.size()>0) {
            for (GhostBgpv4Route ghostBgpv4Route : _updateRouteList) {
                // only update the new route selection(s)
                Optional<GhostBgpv4Route> transformedRoute =
                    exportSingleRouteToNeighbor(
                                  ghostBgpv4Route,
                                  ourConfigId,
                                  remoteConfigId,
                                  ourConfig,
                                  remoteConfig,
                                  remoteBgpRoutingProcess,
                                  session,
                                  Type.IPV4_UNICAST,
                                  ghostComputation);
                if (transformedRoute.isPresent()) {
                    exportedRoutes.add(transformedRoute.get());
                }
               
            }
    
        }

 
        return exportedRoutes.stream();

        // Stream<RouteAdvertisement<Bgpv4Route>> exportedRoutes =
        // ourConfig.getGeneratedRoutes().stream()
        //     .map(
        //         r -> {
        //           // Activate route and convert to BGP if activated
        //           Bgpv4Route bgpv4Route =
        //               processNeighborSpecificGeneratedRoute(r, session.getHeadIp());
        //           if (bgpv4Route == null) {
        //             // Route was not activated
        //             return Optional.<Bgpv4Route>empty();
        //           }
        //           // Run pre-export transform, export policy, & post-export transform
        //           return transformBgpRouteOnExport(
        //               bgpv4Route,
        //               ourConfigId,
        //               remoteConfigId,
        //               ourConfig,
        //               remoteConfig,
        //               remoteBgpRoutingProcess,
        //               session,
        //               Type.IPV4_UNICAST);
        //         })
        //     .filter(Optional::isPresent)
        //     .map(Optional::get)
        //     .map(RouteAdvertisement::new);
    }

    public void iteration(NetworkConfigurations nc, 
                            Map<String, Node> allNodes, 
                            Map<String, GhostNode> allGhostNodes, 
                            BgpTopology bgpTopology,
                            GhostComputation ghostComputation) {
        
        for (EdgeId edgeId : _bgpRoutingProcess.getBgpEdgesYrl()) {
            
            String neighborHost = edgeId.tail().getHostname();

            if (!allGhostNodes.get(neighborHost).isUpdated()) {
                continue;
            }
            String slefHost = edgeId.head().getHostname();
            GhostNode neighborGhostNode = allGhostNodes.get(neighborHost);  
            Iterator<GhostBgpv4Route> exportedRoutes =  
                    neighborGhostNode.exprotToNeighbor(edgeId.reverse(), bgpTopology, nc, allNodes, ghostComputation).iterator();
            List<GhostBgpv4Route> neighborInRoutes = new ArrayList<>();
            while (exportedRoutes.hasNext()) {
                GhostBgpv4Route remoteRoute = exportedRoutes.next();

                // apply Import policy
                Optional<GhostBgpv4Route> importedGhostBgpv4Route = importFromNode(remoteRoute, edgeId, bgpTopology, nc, allNodes, ghostComputation);
                if (!importedGhostBgpv4Route.isPresent()) {
                    continue;
                }
                // add route to ribIn, imported route must exist here
                neighborInRoutes.add(importedGhostBgpv4Route.get());

                GhostBgpv4Route r = importedGhostBgpv4Route.get();
                int maxCondNum = r.getMaxExistCond();
                Optional<GhostBgpv4Route> selectedGhostBgpv4RouteNow = getCondSelectedRoute(maxCondNum);
                
                if (selectedGhostBgpv4RouteNow.isPresent()) {
                    // generate a new execution if the route matches selection-path and don't be selected as the best route
                    if (r.getIsMatch() && selectedGhostBgpv4RouteNow.get().getIsMatch()) {
                        continue;
                    }
                    if (r.getIsMatch()) {
                        if (Prefer(selectedGhostBgpv4RouteNow, importedGhostBgpv4Route)) {
                            // r and selection route must not null,
                            ghostComputation.addCondition(new Condition(ConditionType.PREFER, 
                                                                r.getBgpv4Route(), selectedGhostBgpv4RouteNow.get().getBgpv4Route(),
                                                                _hostName, null), this);
                            
                            // imported r as selection setttings ...
                            r.addExistCond(ghostComputation.getMaxCondNum());
                            r.addSelectedCond(ghostComputation.getMaxCondNum());
                            // r.addSelectedCond(maxCondNum);
                            _selectMap.put(ghostComputation.getMaxCondNum(), r);
                            _updateCondList.add(ghostComputation.getMaxCondNum());
                            _updateRouteList.add(r);
                        } else {
                            selectedGhostBgpv4RouteNow.get().removeSelectedCond(maxCondNum);
                            r.addSelectedCond(maxCondNum);
                            _selectMap.put(maxCondNum, r);
                            _updateCondList.add(maxCondNum);
                            _updateRouteList.add(r);
                        }
                        continue;
                    } else if (selectedGhostBgpv4RouteNow.get().getIsMatch()) {
                        if (Prefer(importedGhostBgpv4Route, selectedGhostBgpv4RouteNow)) {
                            ghostComputation.addCondition(new Condition(ConditionType.PREFER, 
                                                                selectedGhostBgpv4RouteNow.get().getBgpv4Route(), r.getBgpv4Route(), 
                                                                _hostName, null), this);
                            
                                                                selectedGhostBgpv4RouteNow.get().removeExistCond(maxCondNum);
                            selectedGhostBgpv4RouteNow.get().addExistCond(ghostComputation.getMaxCondNum());
                            _selectMap.put(ghostComputation.getMaxCondNum(), selectedGhostBgpv4RouteNow.get());
                            _updateCondList.add(ghostComputation.getMaxCondNum());
                            _updateRouteList.add(selectedGhostBgpv4RouteNow.get());

                            r.addSelectedCond(maxCondNum);
                            _selectMap.put(maxCondNum, r);
                            _updateCondList.add(maxCondNum);
                            _updateRouteList.add(r);
                            
                            
                            continue;
                        }
                    } else {
                        // normal route selection
                        if (comparePreference(selectedGhostBgpv4RouteNow.get().getBgpv4Route(), importedGhostBgpv4Route.get().getBgpv4Route())>=0) {
                            continue;
                        } else {
                            selectedGhostBgpv4RouteNow.get().removeSelectedCond(maxCondNum);
                            r.addSelectedCond(maxCondNum);
                            _selectMap.put(maxCondNum, r);
                            _updateCondList.add(maxCondNum);
                            _updateRouteList.add(r);
                        }
                    }                   
                } else {
                    r.addSelectedCond(maxCondNum);
                    _selectMap.put(maxCondNum, r);
                    _updateCondList.add(maxCondNum);
                    _updateRouteList.add(r);
                }

            }
            _ghostMap.put(neighborHost, neighborInRoutes);
        }

    }

    private static int getTypeCost(RoutingProtocol protocol) {
        switch (protocol) {
          case AGGREGATE:
            return 2;
          case BGP: // eBGP
            return 1;
          case IBGP:
            return 0;
          default:
            throw new IllegalArgumentException(String.format("Invalid BGP protocol: %s", protocol));
        }
      }

    public boolean Prefer(Optional<GhostBgpv4Route> l, Optional<GhostBgpv4Route> r) {
        // if l >= r, return true, else return false(<)
        if (!r.isPresent()) {
            return true;
        } else if (l.isPresent()) {
            return comparePreference(l.get().getBgpv4Route(), r.get().getBgpv4Route())>=0;
        }
        return false;
    }
  
    public int comparePreference(Bgpv4Route lhs, Bgpv4Route rhs) {
        int multipathCompare =
            Comparator
                // Prefer higher Weight (cisco only)
                .comparing(Bgpv4Route::getWeight)
                // Prefer higher LocalPref
                .thenComparing(Bgpv4Route::getLocalPreference)
                // NOTE: Accumulated interior gateway protocol (AIGP) is not supported
                // Aggregates (for non-juniper devices, won't appear on Juniper)
                // .thenComparing(r -> getAggregatePreference(r.getProtocol()))
                // AS path: prefer shorter
                // TODO: support `bestpath as-path ignore` (both cisco, juniper)
                .thenComparing(r -> r.getAsPath().length(), Comparator.reverseOrder())
                // Prefer certain origin type Internal over External over Incomplete
                .thenComparing(r -> r.getOriginType().getPreference())
                // Prefer eBGP over iBGP
                .thenComparing(r -> getTypeCost(r.getProtocol()))
                /*
                 * Prefer lower Multi-Exit Discriminator (MED)
                 * TODO: better support for MED rules
                 * Most rules are currently not supported:
                 *    - normally this comparison is done only if the first AS is the same in both AS Paths
                 *    - `always-compare-med` -- overrides above
                 *    - there are additional confederation rules
                 *    - On Juniper `path-selection cisco-nondeterministic` changes behavior
                 *    - On Cisco `bgp bestpath med missing-as-worst` changes missing MED values from 0 to MAX_INT
                 */
                .thenComparing(Bgpv4Route::getMetric, Comparator.reverseOrder())
                // Prefer next hop IPs with the lowest IGP metric
                // .thenComparing(this::getIgpCostToNextHopIp, Comparator.reverseOrder())
                // Prefer lowest CLL as a proxy for IGP Metric. FRR-Only.
                // .thenComparing(this::getClusterListLength, Comparator.reverseOrder())
                // Evaluate AS path compatibility for multipath
                // .thenComparing(this::compareRouteAsPath)
                .compare(lhs, rhs);
        return multipathCompare;
      }
    
    public boolean checkReq() {
        if (getMaxCondSelectedRoute().isPresent()) {
            return getMaxCondSelectedRoute().get().getIsMatch();
        } else {
            // unreach
            return (_selectMap.size()==0);
        }
        
    }

    public boolean ifChange() {
        if (_selectionAsPath.size()>0) {
            if (getCondSelectedRoute(0).isPresent()) {
                return (!getCondSelectedRoute(0).get().getIsMatch());
            } else {
                return true;
            }
        } 
        return getMaxCondSelectedRoute().isPresent();
    }


}
