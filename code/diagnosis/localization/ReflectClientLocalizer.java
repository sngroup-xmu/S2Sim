//package org.batfish.diagnosis.localization;
//
//import java.util.List;
//import java.util.Set;
//import org.batfish.datamodel.BgpProcess;
//import org.batfish.diagnosis.common.BgpPeerLog;
//import org.batfish.diagnosis.common.ConfigurationLine;
//import org.batfish.diagnosis.repair.ReflectClientRepairer;
//import org.batfish.diagnosis.repair.Repairer;
//
//public class ReflectClientLocalizer extends Localizer {
//    private String localDevName;
//    private Set<BgpPeerLog> clients;
//    private Violation violation;
//    private BgpProcess bgpProcess;
//
//    public String getCfgPath() {
//        return cfgPath;
//    }
//
//    String cfgPath;
//
//    public ReflectClientLocalizer(String localDev, Set<BgpPeerLog> clients, Violation violation, String cfgPath) {
//        this.localDevName = localDev;
//        this.clients = clients;
//        this.violation = violation;
//        this.cfgPath = cfgPath;
//    }
//
//    @Override
//    public List<ConfigurationLine> genErrorConfigLines() {
//        // TODO Auto-generated method stub
//        // 行号为-1表示没有缺失该行
//
//        clients.forEach(n -> {
//            String peerIp = n.getLocalIpString();
//            addErrorLine(violation.getMissingLine(), "peer " + peerIp + " reflect-client");
//        });
//        return getErrorLines();
//    }
//
//
//    /**
//     * @return Violation return the violation
//     */
//    public Violation getViolation() {
//        return violation;
//    }
//
//    /**
//     * @param violation the violation to set
//     */
//    public void setViolation(Violation violation) {
//        this.violation = violation;
//    }
//
//
//    /**
//     * @return String return the localDevName
//     */
//    public String getLocalDevName() {
//        return localDevName;
//    }
//
//    /**
//     * @param localDevName the localDevName to set
//     */
//    public void setLocalDevName(String localDevName) {
//        this.localDevName = localDevName;
//    }
//
//    /**
//     * @return List<String> return the clientDevs
//     */
//    public Set<BgpPeerLog> getClients() {
//        return clients;
//    }
//
//    /**
//     * @param clients the clientDevs to set
//     */
//    public void setClients(Set<BgpPeerLog> clients) {
//        this.clients = clients;
//    }
//
//    @Override
//    public Repairer genRepairer() {
//        // TODO Auto-generated method stub
//        ReflectClientRepairer repairer = new ReflectClientRepairer(this);
//        repairer.genRepair();
//        return repairer;
//    }
//
//}
