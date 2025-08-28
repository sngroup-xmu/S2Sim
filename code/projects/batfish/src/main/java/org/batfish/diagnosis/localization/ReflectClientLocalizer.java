package org.batfish.diagnosis.localization;

import java.util.List;
import java.util.Set;
import org.batfish.datamodel.BgpProcess;
import org.batfish.diagnosis.common.BgpPeerLog;
import org.batfish.diagnosis.common.ConfigurationLine;
import org.batfish.diagnosis.repair.Repairer;

public class ReflectClientLocalizer extends Localizer {
   private String _localDevName;
   private Set<BgpPeerLog> _clients;
   private BgpProcess _bgpProcess;

   public String getCfgPath() {
       return _cfgPath;
   }

   String _cfgPath;

   public ReflectClientLocalizer(String localDev, Set<BgpPeerLog> clients, String cfgPath) {
       this._localDevName = localDev;
       this._clients = clients;
       this._cfgPath = cfgPath;
   }

   @Override
   public List<ConfigurationLine> genErrorConfigLines() {
       // TODO Auto-generated method stub
       // 行号为-1表示没有缺失该行

       _clients.forEach(n -> {
           String peerIp = n.getLocalIpString();
           addErrorLine(new ConfigurationLine(-1, "peer " + peerIp + " reflect-client"));
       });
       return getErrorLines();
   }


   /**
    * @return String return the localDevName
    */
   public String getLocalDevName() {
       return _localDevName;
   }

   /**
    * @param localDevName the localDevName to set
    */
   public void setLocalDevName(String localDevName) {
       this._localDevName = localDevName;
   }

   /**
    * @return return the clientDevs
    */
   public Set<BgpPeerLog> getClients() {
       return _clients;
   }

   /**
    * @param clients the clientDevs to set
    */
   public void setClients(Set<BgpPeerLog> clients) {
       this._clients = clients;
   }

   @Override
   public Repairer genRepairer() {
       // TODO Auto-generated method stub
    //    ReflectClientRepairer repairer = new ReflectClientRepairer(this);
    //    repairer.genRepair();
       return null;
   }

}
