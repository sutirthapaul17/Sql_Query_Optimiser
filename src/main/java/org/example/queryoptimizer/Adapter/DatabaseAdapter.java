package org.example.queryoptimizer.Adapter;

import org.example.queryoptimizer.Dto.QueryPlanDto;

public interface DatabaseAdapter {

    QueryPlanDto getBaselinePlan(String validatedSql);
}
