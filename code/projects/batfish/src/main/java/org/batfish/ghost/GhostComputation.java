package org.batfish.ghost;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.batfish.common.topology.ValueEdge;
import org.batfish.datamodel.AsPath;
import org.batfish.datamodel.AsSet;
import org.batfish.datamodel.BgpPeerConfigId;
import org.batfish.datamodel.BgpSessionProperties;
// import org.batfish.datamodel.Edge;
import org.batfish.datamodel.NetworkConfigurations;
import org.batfish.datamodel.Prefix;
// import org.batfish.datamodel.Topology;
import org.batfish.datamodel.bgp.BgpTopology;
// import org.batfish.datamodel.bgp.BgpTopology.EdgeId;
// import org.batfish.datamodel.routing_policy.expr.IpPrefix;
import org.batfish.dataplane.ibdp.Node;
import org.batfish.dataplane.ibdp.TopologyContext;
// import org.batfish.dataplane.ibdp.VirtualRouter;

import com.google.common.collect.ImmutableMap;
// import com.google.common.collect.ImmutableSet;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GhostComputation {
    // private TopologyContext _topContext;
    private Map<Integer, Condition> _conditions;
    private BgpTopology _bgpTopology;
    private Map<String, GhostNode> _nodes;
    private String _dstHost;
    private Prefix _ip;
    private int _condNumMax;
    private Map<String, Long> _asMap;

    public GhostComputation(TopologyContext topContext, SortedMap<String, Node> nodes) {
        // _topContext = topContext;
        _condNumMax = 0;
        _bgpTopology = topContext.getBgpTopology();
        _nodes = new HashMap<>();
        _conditions = new HashMap<>();
        _conditions.put(0, new Condition(ConditionType.NONE));
        for (Node node : nodes.values()) {
            _nodes.put(node.getConfiguration().getHostname(), new GhostNode(node, _conditions));
        }
        _asMap = getNodeAsNumber(_bgpTopology);
    }

    public int getMaxCondNum() {
        return _condNumMax;
    }

    public boolean isDstNode(Node node) {
        return node.getConfiguration().getHostname()==_dstHost;
    }

    public void addCondition(Condition condition, GhostNode excludedNode) {
        // add condNum for all routes 
        _conditions.put(_conditions.size(), condition);
        _condNumMax += 1;
        for (GhostNode ghostNode : _nodes.values()) {
            ghostNode.updateCondForEachRoute(_condNumMax, ghostNode.getHostName()!=excludedNode.getHostName());
        }
        // return true;
    }

    public void addCondition(Condition condition) {
        // add condNum for all routes 
        _conditions.put(_conditions.size(), condition);
        _condNumMax += 1;
        for (GhostNode ghostNode : _nodes.values()) {
            ghostNode.updateCondForEachRoute(_condNumMax, true);
        }
        // return true;
    }

    public void ghostCompute(String dstHost, Prefix ip, Map<String, Node> allNodes) {
        _dstHost = dstHost;
        _ip = ip;
        // find the route in 
        GhostNode dstNode = _nodes.get(_dstHost);
        // dstNode.addUpdateCond(0);
        dstNode.initForDstNode(ip);

        // get all configurations
        NetworkConfigurations nc =
        NetworkConfigurations.of(
            _nodes.entrySet().stream()
                .collect(
                    ImmutableMap.toImmutableMap(
                        Entry::getKey, e -> e.getValue().getNode().getConfiguration())));

        // set the update value for dataplane computation
        boolean converged = false;
        int iterNum =0;
        while (!converged) {
            converged = true;
            for (GhostNode ghostNode : _nodes.values()) {
                // if (ghostNode.getHostName() == dstHost) {
                //     continue;
                // }
                if ((iterNum==0) && ghostNode.getHostName().equals(_dstHost)) {
                    continue;
                }
                ghostNode.initForIteration();
                ghostNode.iteration(nc, allNodes, _nodes, _bgpTopology, this);
            }
            for (GhostNode ghostNode : _nodes.values()) {
                if (ghostNode.getUpdList().size()>0) {
                    converged = false;
                }
            }
            iterNum += 1;
        }
        assert checkViolation();
    }


    public boolean checkViolation() {
        Map<String, String> nextHopMap = new HashMap<>();

        for (int i=0; i<=_condNumMax; i++) {
            // check the validation of the i-th ghost trace
            for (GhostNode node : _nodes.values()) {

                if (node.getCondSelectedRoute(i).isPresent()) {
                    List<AsSet> asSets = node.getCondSelectedRoute(i).get().getBgpv4Route().getAsPath().getAsSets();
                    if (nextHopMap.containsKey(asSets.get(0).toString())) {
                        // it means the current as-path is one of a subset of other nodes which have already been recorded
                        for (int j=0; j<asSets.size()-1; j++) {
                            if (!nextHopMap.containsKey(asSets.get(j).toString())) {
                                return false;
                            } else {
                                if (nextHopMap.get(asSets.get(j).toString())!=asSets.get(j+1).toString()) {
                                    return false;
                                }
                            }
                        } 
                    } else {
                        // new as-path
                        for (int j=0; j<asSets.size()-1; j++) {
                            nextHopMap.put(asSets.get(j).toString(), asSets.get(j+1).toString());
                        }
                    }
                } else {
                    // the path of the node to dst is unreachable in this condition, 
                    // however, if one of the super path of this node is already existing, means there are errors.
                    Long currAs = _asMap.get(node.getHostName());
                    if (nextHopMap.containsKey(currAs)) {
                        return false;
                    }
                }
                
            }
        }
        return true;
    }

    public Map<String, Long> getNodeAsNumber(BgpTopology bgpTopology) {
        Map<String, Long> asMap = new HashMap<>();
        List<ValueEdge<BgpPeerConfigId, BgpSessionProperties>> edges = bgpTopology.getEdges();
        // BgpSessionProperties session = BgpRoutingProcess.getBgpSessionPropertiesYrl(bgpTopology, edgeId);
        for (ValueEdge<BgpPeerConfigId,BgpSessionProperties> valueEdge : edges) {
            BgpSessionProperties session = valueEdge.getValue();
            String a = "";
            asMap.put(valueEdge.getSource().getHostname(), session.getTailAs());
            asMap.put(valueEdge.getTarget().getHostname(), session.getHeadAs());
        }
        return asMap;
    }


    public void forwardingTreeAssignment(String filePath) {
        // generate the selection path and prop/acpt neighbors for each node, 
        // the input file specifies the forwarding paths for nodes

        // 1. assign AS-Path for (BGP) node
        _nodes.values().stream().forEach(t -> t.clearAssignment());
        File file;
        InputStreamReader isr = null;
        BufferedReader br = null;
        long totalTime = 0;
        try {
            file = new File(filePath);
            isr = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
            br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null) {
                if (line.startsWith("#")) {
                    continue;
                }
                String[] token = line.split("-");
                List<String> path = new ArrayList<>();
                Arrays.asList(token).stream().forEach(e->path.add(e.trim()));
                
                for (int i=0; i < token.length; i++) {
                    if (i<token.length-1) {
                        _nodes.get(path.get(i)).setSelectionAsPath(path.subList(i+1, path.size()), _asMap);
                        _nodes.get(path.get(i)).addAcptNeighbor(path.get(i+1));
                    }
                    
                    if (i>0) {
                        _nodes.get(path.get(i)).addPropNeighbor(path.get(i-1));
                    }
                    
                }
            }
            isr.close();

        }catch (Exception e){
            e.printStackTrace();
        } 

    }

    public boolean checkReq() {
        List<GhostNode> ns = _nodes.values().stream().filter(t->!t.checkReq()).collect(Collectors.toList());
        return ns.size()==0;
    }

    public int countErrorPaths() {
        int count = 0;
        for (GhostNode node : _nodes.values()) {
            if (node.ifChange()) {
                count+=1;
            }
        }
        return count;
    }


}
