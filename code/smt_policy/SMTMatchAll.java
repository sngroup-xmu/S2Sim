package smt_policy;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import smt_context.SolverContext;

public class SMTMatchAll implements SMTAbstractMatch{
  private SolverContext _ctx;
  private BoolExpr _match_var;

  public SMTMatchAll(SolverContext ctx) {
    this._ctx = ctx;

  }

  public BoolExpr get_match_var() {
    return _match_var;
  }

  public SolverContext get_ctx() {
    return _ctx;
  }
}
