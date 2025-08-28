package org.batfish.diagnosis.conditions;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;


/*
 * Each BGP Condition is for a router (for single prefix)
 */
public class BgpCondition {

    @JsonProperty("ipPrefix")
    private String _networkString;

    @JsonProperty("redistribution")
    private boolean _redistribution;

    @JsonProperty("rrClients")
    private Set<String> _rrClients;

    @JsonProperty("selectionRoute")
    private SelectionRoute _route;

    @JsonProperty("propNeighbors")
    private Set<String> _propNeighbors;

    @JsonProperty("acptNeighbors")
    private Set<String> _acptNeighbors;

    @JsonProperty("ibgpPeers")
    private Set<String> _ibgpPeers;

    @JsonProperty("ebgpPeers")
    private Set<String> _ebgpPeers;


    public BgpCondition(Builder builder) {
        this._networkString = builder._network;
        this._redistribution = builder._redistribution;
        this._route = builder._route;
        this._propNeighbors = builder._propNeighbors;
        this._acptNeighbors = builder._acptNeighbors;
        this._rrClients = builder._rrClients;
        this._ibgpPeers = builder._ibgpPeers;
        this._ebgpPeers = builder._ebgpPeers;
    }

//    public static Map<String, BgpCondition> deserialize(String filePath) {
//        File file = new File(filePath);
//        String jsonStr;
//        try {
//            jsonStr = FileUtils.readFileToString(file,"UTF-8");
//            Map<String, BgpCondition> conds = new Gson().fromJson(jsonStr, new TypeToken<Map<String, BgpCondition>>() {}.getType());
//            return conds;
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return null;
//    }


    public void setRedistribute(boolean value) {
        this._redistribution = value;
    }


    public static class Builder {
        private String _network;

        private boolean _redistribution;

        private Set<String> _rrClients;

        private SelectionRoute _route;

        private Set<String> _propNeighbors;

        private Set<String> _acptNeighbors;

        private Set<String> _ibgpPeers;

        private Set<String> _ebgpPeers;

        private String _vpnName;

        public Builder(String network) {
            this._network = network;
        }

        public Builder vpnName(String name) {
            this._vpnName = name;
            return this;
        }

        public Builder redistribution(boolean flag) {
            this._redistribution = flag;
            return this;
        }

        public Builder rrClient(Set<String> rrClients) {
            this._rrClients = rrClients;
            return this;
        }

        public Builder selectionRoute(SelectionRoute route) {
            this._route = route;
            return this;
        }

        public Builder propNeighbors(Set<String> nodes) {
            this._propNeighbors = nodes;
            return this;
        }

        public Builder acptNeighbors(Set<String> nodes) {
            this._acptNeighbors = nodes;
            return this;
        }

        public Builder ibgpPeers(Set<String> peers) {
            this._ibgpPeers = peers;
            return this;
        }

        public Builder ebgpPeers(Set<String> peers) {
            this._ebgpPeers = peers;
            return this;
        }


        public BgpCondition build() {
            return new BgpCondition(this);
        }
    }

}
