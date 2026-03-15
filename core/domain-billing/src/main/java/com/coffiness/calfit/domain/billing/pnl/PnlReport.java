package com.coffiness.calfit.domain.billing.pnl;

import java.util.List;

public record PnlReport(
    List<String> columns,
    long[] revenueTotal,
    long[] laborCost,
    long[] infraCost,
    long[] marketingCost,
    long[] otherCost,
    long[] costTotal,
    long[] operatingProfit) {}
