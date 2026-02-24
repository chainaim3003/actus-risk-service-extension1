package org.actus.risksrv3.utils.hybridtreasury1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.hybridtreasury1.AllocationDriftModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AllocationDriftModel (Domain 4 — Treasury Model 5.1)
 *
 * Monitors the allocation of any digital asset (BTC, ETH, or other)
 * within a hybrid treasury portfolio. When the asset's market-value
 * share breaches maxAllocation, a rebalancing signal is returned.
 * When below minAllocation, an underweight signal is returned.
 *
 * Generic design: The same model class works for BTC, ETH, or any
 * digital asset — the specific asset is determined by which
 * spotPriceMOC is configured (e.g. BTC_USD_SPOT or ETH_USD_SPOT).
 * Multiple instances can run simultaneously (one per asset STK contract).
 *
 * Runs on a digital-asset STK contract. At each daily monitoring point
 * via MRD callout, receives StateSpace containing notionalPrincipal
 * (position value or units held). Looks up spot price and total
 * portfolio value from risksrv3 marketModel.
 *
 * Decision logic at each check date:
 *   asset_value = notionalPrincipal × spot_price
 *   allocation  = asset_value / portfolio_total_value
 *
 *   allocation > maxAllocation  → return (allocation - targetAllocation)
 *                                  positive signal = overweight, sell to rebalance
 *   allocation < minAllocation  → return -(targetAllocation - allocation)
 *                                  negative signal = underweight, buy to rebalance
 *   else                        → return 0.0  (within tolerance band)
 *
 * Portfolio context ($10M hybrid treasury):
 *   50% fiat ($5M cash + T-bills)
 *   30% stablecoin ($3M USDC)
 *   20% digital ($2M BTC/ETH)
 *   Per-asset targets configured independently
 *
 * ACTUS contract type: STK (equity-like digital asset position)
 * Market Object Codes consumed:
 *   [spotPriceMOC]           — e.g. BTC_USD_SPOT or ETH_USD_SPOT
 *   PORTFOLIO_TOTAL_VALUE    — total portfolio NAV
 */
public class AllocationDriftModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String spotPriceMOC;
    private final String portfolioTotalValueMOC;
    private final double targetAllocation;
    private final double maxAllocation;
    private final double minAllocation;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public AllocationDriftModel(String riskFactorId,
                                AllocationDriftModelData data,
                                MultiMarketRiskModel marketModel) {
        this.riskFactorId           = riskFactorId;
        this.spotPriceMOC           = data.getSpotPriceMOC();
        this.portfolioTotalValueMOC = data.getPortfolioTotalValueMOC();
        this.targetAllocation       = data.getTargetAllocation();
        this.maxAllocation          = data.getMaxAllocation();
        this.minAllocation          = data.getMinAllocation();
        this.monitoringEventTimes   = data.getMonitoringEventTimes();
        this.marketModel            = marketModel;
    }

    @Override
    public Set<String> keys() {
        Set<String> keys = new HashSet<>();
        keys.add(this.riskFactorId);
        return keys;
    }

    @Override
    public List<CalloutData> contractStart(ContractModel contract) {
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double spotPrice = this.marketModel.stateAt(this.spotPriceMOC, time);
        double portfolioTotal = this.marketModel.stateAt(this.portfolioTotalValueMOC, time);
        double quantity = Math.abs(states.notionalPrincipal);

        if (portfolioTotal <= 0.0) {
            System.out.println("**** AllocationDriftModel: time=" + time
                    + " WARNING: portfolioTotal <= 0, returning 0.0");
            return 0.0;
        }

        double assetValue = quantity * spotPrice;
        double allocation = assetValue / portfolioTotal;

        System.out.println("**** AllocationDriftModel: time=" + time
                + " spotPriceMOC=" + this.spotPriceMOC
                + " spotPrice=" + String.format("%.2f", spotPrice)
                + " quantity=" + String.format("%.6f", quantity)
                + " assetValue=" + String.format("%.2f", assetValue)
                + " portfolioTotal=" + String.format("%.2f", portfolioTotal)
                + " allocation=" + String.format("%.4f", allocation)
                + " target=" + targetAllocation
                + " max=" + maxAllocation
                + " min=" + minAllocation);

        if (allocation > this.maxAllocation) {
            double driftFraction = allocation - this.targetAllocation;
            System.out.println("**** AllocationDriftModel: OVERWEIGHT drift="
                    + String.format("%.4f", driftFraction)
                    + " → rebalance sell signal");
            return Math.min(1.0, driftFraction);
        } else if (allocation < this.minAllocation) {
            double driftFraction = this.targetAllocation - allocation;
            System.out.println("**** AllocationDriftModel: UNDERWEIGHT drift="
                    + String.format("%.4f", driftFraction)
                    + " → rebalance buy signal");
            return -Math.min(1.0, driftFraction);
        } else {
            return 0.0;
        }
    }
}
