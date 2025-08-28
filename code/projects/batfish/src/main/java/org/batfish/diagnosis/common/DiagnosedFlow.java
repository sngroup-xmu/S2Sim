package org.batfish.diagnosis.common;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.batfish.common.topology.Layer2Edge;
import org.batfish.datamodel.Prefix;
import org.batfish.diagnosis.util.InputData;
import org.batfish.diagnosis.util.KeyWord;

/**
 * Description of a flow (traffic), including these characters:
 * --------------------必选项 necessary【用户输入】------------------------- 1) srcNode,
 * 起点（数据包发送方） 2) vpn, 起点vpn的名称 3) dstNode, 终点（数据包要送达的节点） 4) networkType, 组网类型 5)
 * caseType, 错误编号 ------------非必选项 necessary【分析得到/已知特性的组网结构】--------------- 4)
 * ifMpls 5) mplsProtocol 6) mplsArea ... EVPN, SRv6, ....
 */
public class DiagnosedFlow {

    Set<String> srcNode = new HashSet<>();
    String dstNode;
    String reqDstPrefixString; // specified in requirement
    String cfgDstPrefixString; // searching from the configuration
    NetworkType networkType;
    String configRootPath;
    Set<Layer2Edge> failedEdges;
    // the forwarding path
    List<String> existingFlowPath;
    Map<String, String> configPathMap;
    int failure;    //the k value of k-failure
    List<List<String>> path;
    String networkRootPath;

    public void setNetworkRootPath(String path) {
        networkRootPath = path;
    }

    public String getNetworkRootPath() {return networkRootPath;}

    public Set<String> getSrcNode() {
        return srcNode;
    }

    public int getFailureNumber() {
        return failure;
    }

    public String getDstNode() {
        return dstNode;
    }

    public String getReqDstPrefixString() {
        return reqDstPrefixString;
    }

    public Prefix getReqDstPrefix() {
        return Prefix.parse(reqDstPrefixString);
    }

    public String getCfgDstPrefixString() {
        if (cfgDstPrefixString != null) {
            return cfgDstPrefixString;
        }
        else {
            return reqDstPrefixString;
        }

    }

    public NetworkType getNetworkType() {
        return networkType;
    }

    public String getConfigRootPath() {
        return configRootPath;
    }

    public Set<Layer2Edge> getFailedEdges() {
        return failedEdges;
    }

    public List<String> getExistingFlowPath() {
        return existingFlowPath;
    }

    public Map<String, String> getConfigPathMap() {
        return configPathMap;
    }

    public String getConfigPath(String node) {
        return configPathMap.get(node);
    }

    private boolean isValidArgument(String str) {
        return str != null && !str.equals("");
    }

    public boolean isValid() {
        return isValidArgument(dstNode) && isValidArgument(cfgDstPrefixString)
                && isValidArgument(configRootPath);
    }

    public static List<DiagnosedFlow> parse(String filePath) {
      File file = new File(filePath);
      Gson gson = new Gson();
//      DiagnosedFlow flow;
//      flow = gson.fromJson(InputData.getStr(file), Builder.class).build();
//      修改：一次读入多条流
      Type listType = new TypeToken<List<Builder>>(){}.getType();
      List<Builder> builderList = gson.fromJson(InputData.getStr(file), listType);
      List<DiagnosedFlow> flowList = builderList.stream()
                                                .map(Builder::build)
                                                .collect(Collectors.toList());
      return flowList;
    }

    public static final class Builder {
        Set<String> srcNode;
        String dstNode;
        String reqDstPrefixString; // specified in requirement
        String cfgDstPrefixString; // searching from the configuration
        NetworkType networkType;
        String configRootPath;
        Set<Layer2Edge> failedEdges;
        // the forwarding path
        String existingFlowPath;
        Map<String, String> configPathMap;
        int failure;
        List<List<String>> path;

        public Builder() {
        }

        public Builder withSrcNode(Set<String> srcNode) {
            this.srcNode = srcNode;
            return this;
        }

        public Builder withReqDstPrefix(String dstPrefix) {
            this.reqDstPrefixString = dstPrefix;
            return this;
        }

        public Builder withDstDev(String dstNode) {
            this.dstNode = dstNode;
            return this;
        }

        public Builder withNetworkType(NetworkType networkType) {
            this.networkType = networkType;
            return this;
        }

        public Builder withCfgRootPath(String cfgRootPath) {
            this.configRootPath = cfgRootPath;
            return this;
        }

      //path：节点之间通过逗号分割开
       public Builder withExistingFlowPath(String path) {
           this.existingFlowPath = path;
           return this;
       }


       public DiagnosedFlow build() {
           DiagnosedFlow flow = new DiagnosedFlow();
           flow.dstNode = this.dstNode.toLowerCase();
           flow.configRootPath = this.configRootPath;
           for (String item : this.srcNode) {
               flow.srcNode.add(item.toLowerCase());
           }
           flow.networkType = this.networkType;
           flow.reqDstPrefixString = this.reqDstPrefixString;
           flow.cfgDstPrefixString = this.cfgDstPrefixString;
           flow.existingFlowPath = (this.existingFlowPath != null && !this.existingFlowPath.isEmpty()) ? Arrays.asList(this.existingFlowPath.split(",")) : null;
           flow.configPathMap = new HashMap<>();
           flow.failure = failure;
           flow.path = path;
           File rootFile = new File(configRootPath);
           File[] files = rootFile.listFiles();
           for (File file: files) {
               String fileName = file.getName();
               if (fileName.contains("cfg")) {
                   String[] words = fileName.split("\\.");
                   flow.configPathMap.put(words[0].toLowerCase(), file.getAbsolutePath());
               }
           }
           // ------------------------------------------------
           return flow;
       }
   }
    public String getConditionPath() {
        // TODO Auto-generated method stub
        return InputData.concatFilePath(networkRootPath, KeyWord.CONDITION_FILE);
    }
}
