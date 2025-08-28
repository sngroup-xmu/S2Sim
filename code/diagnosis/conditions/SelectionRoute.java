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
        this._networkString = builder.network;
        this._asPath = builder.asPath;
        this._nextHopIps = builder.nextHopIps;

//        this._nextHopStrings = transIpListToString(_nextHopIps);
        this._vpnName = builder.vpnName;
    }


    // need rewrite
    public boolean ifMatch() {
        return false;
    }

    public static class Builder{
        private String network;

        private String vpnName;

        private List<String> nextHopIps;

        private List<Long> asPath;

        public Builder(String prefix) {
            this.network = prefix;
        }

        public Builder nextHop(List<String> nextHopIp) {
            this.nextHopIps = nextHopIp;
            return this;
        }

        public Builder vpnName(String name) {
            this.vpnName = name;
            return this;
        }

        public Builder asPath(List<Long> asPath) {
            this.asPath = asPath;
            return this;
        }

        public SelectionRoute build() {
            return new SelectionRoute(this);
        }
    }
}
