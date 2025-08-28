package org.batfish.ghost;

import org.batfish.datamodel.Bgpv4Route;

public class Condition {

    private ConditionType _type;
    private Bgpv4Route _r1;
    private Bgpv4Route _r2;
    private String _n1;
    private String _n2;
    private Boolean _flag;

    public Condition(ConditionType type){
        _type = type;
    }

    public Condition(ConditionType type, Bgpv4Route r1, Bgpv4Route r2, String n1, String n2) {
        _type = type;
        _r1 = r1;
        _r2 = r2;
        _n1 = n1;
        _n2 = n2;
    }

    public void setFlag(Boolean flag) {
        _flag = flag;
    }

    public Boolean voilate(Bgpv4Route r1, Bgpv4Route r2, String n1, String n2, Boolean flag) {
        if (r1.equals(_r1) && r2.equals(_r2) && n1.equals(_n1) && n2.equals(_n2)) {
            return (flag == _flag);
        }
        return false;
    }


}
