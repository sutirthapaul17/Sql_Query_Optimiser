package org.example.queryoptimizer.Target.Adapter;

import org.example.queryoptimizer.Target.Dto.QueryPlanDto;

public interface DatabaseAdapter {

    QueryPlanDto getBaselinePlan(String validatedSql);
}
