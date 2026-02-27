package org.actus.risksrv3.models.hybridtreasury1;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * AllocationDriftModelData
 *
 * MongoDB document for AllocationDriftModel configuration.
 * Collection: allocationDriftModels
 *
 * Generic for any digital asset (BTC, ETH, etc.) — determined by
 * which spotPriceMOC is configured.
 *
 * JSON example for Postman POST /addAllocationDriftModel:
 * {
 *   "riskFactorId": "ad_btc01",
 *   "spotPriceMOC": "BTC_USD_SPOT",
 *   "portfolioTotalValueMOC": "PORTFOLIO_TOTAL_VALUE",
 *   "targetAllocation": 0.12,
 *   "maxAllocation": 0.15,
 *   "minAllocation": 0.09,
 *   "monitoringEventTimes": [
 *     "2026-03-01T00:00:00", "2026-03-02T00:00:00", ...
 *   ]
 * }
 *
 * For ETH:
 * {
 *   "riskFactorId": "ad_eth01",
 *   "spotPriceMOC": "ETH_USD_SPOT",
 *   "portfolioTotalValueMOC": "PORTFOLIO_TOTAL_VALUE",
 *   "targetAllocation": 0.08,
 *   "maxAllocation": 0.10,
 *   "minAllocation": 0.06,
 *   "monitoringEventTimes": [...]
 * }
 */
@Document(collection = "allocationDriftModels")
public class AllocationDriftModelData {

    @Id
    private String riskFactorId;

    private String spotPriceMOC;
    private String portfolioTotalValueMOC;
    private double targetAllocation;
    private double maxAllocation;
    private double minAllocation;
    private List<String> monitoringEventTimes;

    // ================================================================
    // CLM/UMP/PAM support: when BTC is modeled as a debt instrument
    // (e.g. CLM for yield-producing lending), notionalPrincipal is in
    // USD (e.g. $2,000,000), NOT in BTC units (e.g. 40). The model
    // needs to know the actual BTC quantity separately.
    //
    // positionQuantity: actual asset units (e.g. 40 BTC)
    //   - When > 0: assetValue = positionQuantity × spotPrice,
    //                scaled by (current notionalPrincipal / initialNotionalPrincipal)
    //                to reflect PP event reductions
    //   - When 0 or null: legacy STK/COM mode —
    //                assetValue = abs(notionalPrincipal) × spotPrice
    //                (notionalPrincipal IS the quantity)
    //
    // initialNotionalPrincipal: the starting notionalPrincipal from the
    //   contract JSON ($2,000,000 for CLM, 40 for STK). Used to compute
    //   the reduction factor after PP events.
    // ================================================================
    private double positionQuantity;            // e.g. 40.0 (BTC units)
    private double initialNotionalPrincipal;    // e.g. 2000000.0 (CLM) or 40.0 (STK)

    // ================================================================
    // Cash-mirror support: when an AllocationDriftModel is attached to
    // a cash/fiat contract to mirror BTC rebalancing in the opposite
    // direction. The same allocation signal is computed, then scaled
    // by signalMultiplier to convert to the correct fraction of the
    // cash contract's notionalPrincipal.
    //
    // signalMultiplier: scales the final allocation signal
    //   - Default 1.0 (normal behavior for BTC contract)
    //   - Set to -(btcPositionValue / cashNP) for cash mirror
    //     e.g. -(2000000 / 500000) = -4.0
    //   - Negative inverts direction: overweight BTC → sell BTC → buy cash
    //
    // useFixedQuantity: when true, positionQuantity is used as-is
    //   without scaling by NP reduction factor. Required for cash
    //   mirror because the cash contract's NP changes don't reflect
    //   BTC quantity changes.
    //   - Default false (normal CLM mode: scale by currentNP/initialNP)
    //   - Set true for cash mirror contracts
    // ================================================================
    private double signalMultiplier = 1.0;
    private boolean useFixedQuantity = false;

    // ================================================================
    // Mirror passthrough support: when set, this model does NOT compute
    // allocation independently. Instead, stateAt() reads the dollar
    // payoff cached by the source model and returns the equivalent
    // fraction of this contract's NP (inverted direction).
    //
    // This guarantees dollar-for-dollar matching: if the source model
    // sells $148K of BTC, the mirror adds exactly $148K to cash.
    //
    // When mirrorSourceModelId is set, spotPriceMOC, portfolioTotalValueMOC,
    // targetAllocation, positionQuantity etc. are ignored at runtime.
    // Only monitoringEventTimes is still used (for callout scheduling).
    //
    // REQUIREMENT: the source contract must appear BEFORE the mirror
    // contract in the simulation contracts array, so the source model's
    // stateAt() runs first and populates the dollar cache.
    // ================================================================
    private String mirrorSourceModelId;

    public AllocationDriftModelData() {
    }

    // --- Getters and Setters ---

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getSpotPriceMOC() { return spotPriceMOC; }
    public void setSpotPriceMOC(String spotPriceMOC) { this.spotPriceMOC = spotPriceMOC; }

    public String getPortfolioTotalValueMOC() { return portfolioTotalValueMOC; }
    public void setPortfolioTotalValueMOC(String portfolioTotalValueMOC) { this.portfolioTotalValueMOC = portfolioTotalValueMOC; }

    public double getTargetAllocation() { return targetAllocation; }
    public void setTargetAllocation(double targetAllocation) { this.targetAllocation = targetAllocation; }

    public double getMaxAllocation() { return maxAllocation; }
    public void setMaxAllocation(double maxAllocation) { this.maxAllocation = maxAllocation; }

    public double getMinAllocation() { return minAllocation; }
    public void setMinAllocation(double minAllocation) { this.minAllocation = minAllocation; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }

    public double getPositionQuantity() { return positionQuantity; }
    public void setPositionQuantity(double positionQuantity) { this.positionQuantity = positionQuantity; }

    public double getInitialNotionalPrincipal() { return initialNotionalPrincipal; }
    public void setInitialNotionalPrincipal(double initialNotionalPrincipal) { this.initialNotionalPrincipal = initialNotionalPrincipal; }

    public double getSignalMultiplier() { return signalMultiplier; }
    public void setSignalMultiplier(double signalMultiplier) { this.signalMultiplier = signalMultiplier; }

    public boolean isUseFixedQuantity() { return useFixedQuantity; }
    public void setUseFixedQuantity(boolean useFixedQuantity) { this.useFixedQuantity = useFixedQuantity; }

    public String getMirrorSourceModelId() { return mirrorSourceModelId; }
    public void setMirrorSourceModelId(String mirrorSourceModelId) { this.mirrorSourceModelId = mirrorSourceModelId; }
}
