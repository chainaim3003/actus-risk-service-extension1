package org.actus.risksrv3.utils.hybridtreasury1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.hybridtreasury1.AllocationDriftModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 * ACTUS contract types supported:
 *   STK/COM  — notionalPrincipal IS the quantity (legacy mode)
 *   CLM/UMP/PAM — notionalPrincipal is in USD; positionQuantity provides
 *                 the actual asset units (e.g. 40 BTC). Reduction factor
 *                 from PP events is computed as:
 *                   currentNotional / initialNotionalPrincipal
 *
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

    // CLM/UMP/PAM support fields
    private final double positionQuantity;           // 0 = legacy STK mode
    private final double initialNotionalPrincipal;   // 0 = legacy STK mode

    // Cash-mirror support fields
    private final double signalMultiplier;            // default 1.0; set -4.0 for cash mirror
    private final boolean useFixedQuantity;           // default false; true = bypass NP reduction scaling

    // Mirror passthrough support
    private final String mirrorSourceModelId;          // null = normal mode; set = read from source cache
    private AllocationDriftModel mirrorSource;          // wired by RiskObservationHandler after construction
    private final Map<LocalDateTime, Double> dollarPayoffCache = new HashMap<>();  // time → dollar amount from stateAt()

    public AllocationDriftModel(String riskFactorId,
                                AllocationDriftModelData data,
                                MultiMarketRiskModel marketModel) {
        this.riskFactorId              = riskFactorId;
        this.spotPriceMOC              = data.getSpotPriceMOC();
        this.portfolioTotalValueMOC    = data.getPortfolioTotalValueMOC();
        this.targetAllocation          = data.getTargetAllocation();
        this.maxAllocation             = data.getMaxAllocation();
        this.minAllocation             = data.getMinAllocation();
        this.monitoringEventTimes      = data.getMonitoringEventTimes();
        this.marketModel               = marketModel;
        this.positionQuantity          = data.getPositionQuantity();
        this.initialNotionalPrincipal  = data.getInitialNotionalPrincipal();
        this.signalMultiplier          = data.getSignalMultiplier();
        this.useFixedQuantity          = data.isUseFixedQuantity();
        this.mirrorSourceModelId       = data.getMirrorSourceModelId();
    }

    /** Called by RiskObservationHandler to wire the source model reference */
    public void setMirrorSource(AllocationDriftModel source) {
        this.mirrorSource = source;
    }

    /** Returns the mirrorSourceModelId from config (null if normal mode) */
    public String getMirrorSourceModelId() {
        return this.mirrorSourceModelId;
    }

    /** Read cached dollar payoff for a given time (used by mirror models) */
    public Double getCachedDollarPayoff(LocalDateTime time) {
        return this.dollarPayoffCache.get(time);
    }

    @Override
    public Set<String> keys() {
        Set<String> keys = new HashSet<>();
        keys.add(this.riskFactorId);
        return keys;
    }

    @Override
    public List<CalloutData> contractStart(ContractModel contract) {
        LocalDateTime ied = contract.getAs("initialExchangeDate");
        List<CalloutData> callouts = new ArrayList<>();
        for (String eventTime : this.monitoringEventTimes) {
            if (ied != null) {
                LocalDateTime eventDateTime = LocalDateTime.parse(eventTime);
                if (eventDateTime.isBefore(ied)) {
                    System.out.println("**** AllocationDriftModel: SKIPPING pre-IED callout " + eventTime + " (IED=" + ied + ")");
                    continue;
                }
            }
            callouts.add(new CalloutData(this.riskFactorId, eventTime, CALLOUT_TYPE));
        }
        return callouts;
    }

    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        // ================================================================
        // MIRROR PASSTHROUGH MODE
        // If this model is a mirror, read the source model's cached dollar
        // payoff and return the equivalent fraction of THIS contract's NP
        // (inverted direction: source sells → mirror receives).
        // ================================================================
        if (this.mirrorSourceModelId != null && this.mirrorSource != null) {
            Double sourceDollars = this.mirrorSource.getCachedDollarPayoff(time);
            double currentNP = Math.abs(states.notionalPrincipal);

            if (sourceDollars == null || sourceDollars == 0.0 || currentNP <= 0.0) {
                System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: MIRROR time=" + time
                        + " sourceDollars=" + sourceDollars + " currentNP=" + String.format("%.2f", currentNP)
                        + " → signal=0.0 (no source payoff)");
                return 0.0;
            }

            // sourceDollars is positive when source SOLD (PP payoff > 0)
            // Mirror should RECEIVE that amount: signal = -(dollars / NP)
            // STF_PP_rf2 applies: NP -= signal * NP = NP -= (-(dollars/NP)) * NP = NP += dollars
            double mirrorSignal = -(sourceDollars / currentNP);

            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: MIRROR time=" + time
                    + " sourceDollars=" + String.format("%.2f", sourceDollars)
                    + " currentNP=" + String.format("%.2f", currentNP)
                    + " → mirrorSignal=" + String.format("%.6f", mirrorSignal)
                    + " (cash will change by " + String.format("%.2f", sourceDollars) + ")");
            return mirrorSignal;
        }

        double spotPrice = this.marketModel.stateAt(this.spotPriceMOC, time);
        double portfolioTotal = this.marketModel.stateAt(this.portfolioTotalValueMOC, time);

        // ================================================================
        // Compute effective quantity of the digital asset
        //
        // Three modes of operation:
        //
        // 1. FIXED mode (useFixedQuantity == true):
        //    positionQuantity is used as-is, no NP reduction scaling.
        //    Used for cash-mirror contracts where this contract's NP
        //    is the cash balance, not the BTC position.
        //    Example: positionQuantity=40 (BTC), cash NP=$500K
        //
        // 2. CLM/UMP/PAM mode (positionQuantity > 0, useFixedQuantity == false):
        //    notionalPrincipal is in USD (e.g. $2,000,000)
        //    positionQuantity is the actual BTC units (e.g. 40)
        //    PP events reduce notionalPrincipal proportionally
        //    Effective quantity = positionQuantity × (currentNP / initialNP)
        //    Example: after 20% PP, NP = $1.6M, qty = 40 × (1.6M/2M) = 32
        //
        // 3. Legacy STK/COM mode (positionQuantity == 0):
        //    notionalPrincipal IS the quantity (e.g. 40 BTC)
        //    PP events reduce it directly (40 → 32 after 20% sell)
        // ================================================================
        double quantity;
        String quantityMode;
        if (this.useFixedQuantity && this.positionQuantity > 0) {
            // FIXED mode: use positionQuantity as-is (cash mirror)
            quantity = this.positionQuantity;
            quantityMode = "FIXED";
        } else if (this.positionQuantity > 0 && this.initialNotionalPrincipal > 0) {
            // CLM/UMP/PAM mode: scale positionQuantity by PP reduction factor
            double currentNP = Math.abs(states.notionalPrincipal);
            double reductionFactor = currentNP / this.initialNotionalPrincipal;
            quantity = this.positionQuantity * reductionFactor;
            quantityMode = "CLM";
        } else {
            // Legacy STK/COM mode: notionalPrincipal IS the quantity
            quantity = Math.abs(states.notionalPrincipal);
            quantityMode = "STK";
        }

        if (portfolioTotal <= 0.0) {
            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: time=" + time
                    + " WARNING: portfolioTotal <= 0, returning 0.0");
            return 0.0;
        }

        double assetValue = quantity * spotPrice;
        double allocation = assetValue / portfolioTotal;

        System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: time=" + time
                + " mode=" + quantityMode
                + " spotPriceMOC=" + this.spotPriceMOC
                + " spotPrice=" + String.format("%.2f", spotPrice)
                + " quantity=" + String.format("%.6f", quantity)
                + " assetValue=" + String.format("%.2f", assetValue)
                + " portfolioTotal=" + String.format("%.2f", portfolioTotal)
                + " allocation=" + String.format("%.4f", allocation)
                + " target=" + targetAllocation
                + " max=" + maxAllocation
                + " min=" + minAllocation
                + " signalMultiplier=" + signalMultiplier);

        if (allocation > this.maxAllocation) {
            double driftFraction = allocation - this.targetAllocation;
            double finalSignal = Math.min(1.0, driftFraction) * this.signalMultiplier;
            // Cache dollar payoff for mirror models to read
            double dollarPayoff = finalSignal * Math.abs(states.notionalPrincipal);
            this.dollarPayoffCache.put(time, dollarPayoff);
            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: OVERWEIGHT drift="
                    + String.format("%.4f", driftFraction)
                    + " × multiplier=" + String.format("%.2f", signalMultiplier)
                    + " → finalSignal=" + String.format("%.6f", finalSignal)
                    + " dollarPayoff=" + String.format("%.2f", dollarPayoff) + " (cached)");
            return finalSignal;
        } else if (allocation < this.minAllocation) {
            double driftFraction = this.targetAllocation - allocation;
            double finalSignal = -Math.min(1.0, driftFraction) * this.signalMultiplier;
            // Cache dollar payoff for mirror models to read
            double dollarPayoff = finalSignal * Math.abs(states.notionalPrincipal);
            this.dollarPayoffCache.put(time, dollarPayoff);
            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: UNDERWEIGHT drift="
                    + String.format("%.4f", driftFraction)
                    + " × multiplier=" + String.format("%.2f", signalMultiplier)
                    + " → finalSignal=" + String.format("%.6f", finalSignal)
                    + " dollarPayoff=" + String.format("%.2f", dollarPayoff) + " (cached)");
            return finalSignal;
        } else {
            // Cache zero for mirror models
            this.dollarPayoffCache.put(time, 0.0);
            System.out.println("**** AllocationDriftModel [" + this.riskFactorId + "]: WITHIN BAND → signal=0.0 (cached)");
            return 0.0;
        }
    }
}
