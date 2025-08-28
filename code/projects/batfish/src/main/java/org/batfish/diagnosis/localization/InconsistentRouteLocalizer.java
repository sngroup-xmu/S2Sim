 package org.batfish.diagnosis.localization;


 import org.batfish.datamodel.ConnectedRoute;
 import java.util.List;
 import java.util.Map;
 import org.batfish.datamodel.StaticRoute;
 import org.batfish.diagnosis.common.ConfigurationLine;
import org.batfish.diagnosis.repair.InconsistentRouteRepairer;
import org.batfish.diagnosis.repair.Repairer;

 /**
 * 转发时因为路由优先级导致的错误：
 *  1）静态路由导致转发错误
 *  2）直连路由导致转发错误
 * */
 public class InconsistentRouteLocalizer extends Localizer{

    String _node;
    StaticRoute _staticRoute;
    ConnectedRoute _connectedRoute;

    public InconsistentRouteLocalizer(String node, StaticRoute staticRoute, ConfigurationLine line) {
        this._node = node;
        this._staticRoute = staticRoute;
        addErrorLine(line);
    }

    public InconsistentRouteLocalizer(String node, ConnectedRoute connectedRoute, Map<Integer, String> errorLines) {
        this._node = node;
        this._connectedRoute = connectedRoute;
        addErrorLines(errorLines);
    }

    @Override
    List<ConfigurationLine> genErrorConfigLines() {
        // TODO Auto-generated method stub
        return getErrorLines();
    }

    @Override
    public Repairer genRepairer() {
        return new InconsistentRouteRepairer(this);
    }

 }
