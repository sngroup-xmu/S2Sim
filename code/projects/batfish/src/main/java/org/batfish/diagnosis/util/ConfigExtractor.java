package org.batfish.diagnosis.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.batfish.datamodel.Ip;
import org.batfish.datamodel.Ip6;
import org.batfish.datamodel.Prefix;
import org.batfish.datamodel.routing_policy.RoutingPolicy;


public class ConfigExtractor {

    Prefix _prefix; // flow destination
    String _node; //
    List<RoutingPolicy> _routePolicies;
    
    /**
     * 解析配置里所有配过的 BGP peer,先按 peer ip 提取,只要出现过peer XX ...配置就new一个BgpPeer实例,再在 bgpTopology 里查找对应的node名称
     * 【如果某个ip在bgpTopology里查找不到，说明这个节点一个已经建立好的peer都没有，先假设这种情况不会发生，若要处理则需要layer2Topology来查找各个设备的接口ip】
     * @param configPath 配置文件路径
     * @param bgpTopology 第一次通过provenance文件提取的bgp拓扑
     * @return
     */

    /**
     * 解析配置里所有配过的 BGP peer,先按 peer ip 提取,只要出现过peer XX ...配置就new一个BgpPeer实例【remoteNode置空，之后处理】
     * 注意事项：
     *  1. ipv4或者ipv6都有可能
     *  2. 先不管group配置
     * @param filePath 配置文件路径
     *
     */
    public static Map<String,String> parseBgpPeerIpFromConfiguration(String filePath) {
        //记录配置中的peer信息<peerName,peerIp>,peerName可以从description命令中提取
        Map<String,String> bgpPeers = new HashMap<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine().trim();
            while (line != null) {
                line = line.trim();
                if (line.startsWith(KeyWord.BGP_PEER) && line.contains("description")) {
                    String[] words = line.split(" ");
                    String ipAddr = words[1];
                    String peerName = words[4].split("\"")[0].toLowerCase();
                    // String peerName = words[3].toLowerCase();
                    if (!bgpPeers.keySet().contains(peerName)) {
                        if (Ip.tryParse(ipAddr).isPresent() || Ip6.tryParse(ipAddr).isPresent()) {
                            // peer XX 配置如果是包含ip地址的配置，ip一定是第二个位置的单词，遇到"group"跳过
                            bgpPeers.put(peerName,ipAddr);
                        }
                    }
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bgpPeers;
    }

    //从配置中获取与给定节点的相连的接口的ip
    public static String getPeerIpForPeer(String filePath, String peername){
        String peerIp = "0.0.0.0";
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine().trim();
            while (line != null) {
                line = line.trim();
                if (line.startsWith(KeyWord.INTERFACE_ADDRESS)) {
                    peerIp = line.split(" ")[2];
                    line = reader.readLine().trim();
                    // if (line.startsWith("description") && line.split(" ")[1].toLowerCase().equals(peername)){                    
                    if (line.startsWith("description") && line.split(" ")[2].split("\"")[0].toLowerCase().equals(peername)){
                        break;
                    }
                }
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return peerIp;
    }


    public static void main(String[] argv) {
        Map<String,String> peers = parseBgpPeerIpFromConfiguration("C:\\Users\\yrl\\Desktop\\network\\Config-Error-Localization\\Scalpel\\networks\\provenanceInfo\\cloudnet_multiflow_large\\case1\\config\\UpperER-1.cfg");
    }


}
