package org.batfish.diagnosis.conditions;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;


public class SelectionRoute implements Serializable {
    @JsonProperty("nextHopIp")
    private List<String> _nextHopIps;


    @JsonProperty("asPath")
    private List<Long> _asPath;

    @JsonProperty("ipPrefix")
    private String _networkString;

    @JsonProperty("vpnName")
    private String _vpnName;

    public SelectionRoute(Builder builder) {
        this._networkString = builder._network;
        this._asPath = builder._asPath;
        this._nextHopIps = builder._nextHopIps;

//        this._nextHopStrings = transIpListToString(_nextHopIps);
        this._vpnName = builder._vpnName;
    }

    public List<String> get_nextHopIps() {return this._nextHopIps;}

    public List<Long> get_asPath() {return this._asPath;}

    public String get_networkString() {return _networkString;}

    // need rewrite
    public boolean ifMatch() {
        return false;
    }

    public static class Builder{
        private String _network;

        private String _vpnName;

        private List<String> _nextHopIps;

        private List<Long> _asPath;

        public Builder(String prefix) {
            this._network = prefix;
        }

        public Builder nextHop(List<String> nextHopIp) {
            this._nextHopIps = nextHopIp;
            return this;
        }

        public Builder vpnName(String name) {
            this._vpnName = name;
            return this;
        }

        public Builder asPath(List<Long> asPath) {
            this._asPath = asPath;
            return this;
        }

        public SelectionRoute build() {
            return new SelectionRoute(this);
        }
    }
}
