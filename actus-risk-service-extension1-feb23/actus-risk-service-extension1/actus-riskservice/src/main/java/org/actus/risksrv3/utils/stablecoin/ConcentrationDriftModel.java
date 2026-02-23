package org.actus.risksrv3.utils.stablecoin;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.stablecoin.ConcentrationDriftModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ConcentrationDriftModel (Domain 5 — Model 6.5)
 *
 * Computes Herfindahl-Hirschman Index (HHI) across reserve asset buckets
 * and monitors single-asset concentration. Concentration changes dynamically
 * as T-bills mature, redemptions deplete cash, and compositions shift.
 *
 * HHI = Sum(share_i²) across all asset buckets.
 *   HHI = 1.0 → perfectly concentrated (single asset)
 *   HHI = 1/N → perfectly diversified across N equal assets
 *
 * Signals rebalancing when single-asset concentration > maxSingleAssetShare (40%).
 *
 * Historical finding: "other" ratio of 57% was key differentiator between
 * stablecoins that survived vs failed de-peg events.
 *
 * Market Object Codes consumed (one per asset bucket):
 *   SC_BUCKET_CASH     — cash holdings value
 *   SC_BUCKET_4W_TBILL — 4-week T-bill value
 *   SC_BUCKET_13W_TBILL — 13-week T-bill value
 *   SC_BUCKET_26W_TBILL — 26-week T-bill value
 *
 * Returns: excess concentration fraction above threshold (0.0 if diversified)
 */
public class ConcentrationDriftModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    // -------------------------------------------------------------------------
    // Fields
    // -------------------------------------------------------------------------

    private final String riskFactorId;
    private final List<String> assetBucketMOCs;  // market object codes for each bucket
    private final double maxSingleAssetShare;    // 0.40 (40% maximum)
    private final double hhiWarningThreshold;    // 0.35 (HHI above this = warning)
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ConcentrationDriftModel(String riskFactorId,
                                   ConcentrationDriftModelData data,
                                   MultiMarketRiskModel marketModel) {
        this.riskFactorId        = riskFactorId;
        this.assetBucketMOCs     = data.getAssetBucketMOCs();
        this.maxSingleAssetShare = data.getMaxSingleAssetShare();
        this.hhiWarningThreshold = data.getHhiWarningThreshold();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel         = marketModel;
    }

    // -------------------------------------------------------------------------
    // BehaviorRiskModelProvider interface
    // -------------------------------------------------------------------------

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

    /**
     * Concentration drift logic — computes HHI and max single-asset share.
     *
     * @return excess concentration fraction (0.0 if within limits, up to 0.6)
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        // Fetch values for each asset bucket
        double totalValue = 0.0;
        double[] bucketValues = new double[this.assetBucketMOCs.size()];

        for (int i = 0; i < this.assetBucketMOCs.size(); i++) {
            bucketValues[i] = this.marketModel.stateAt(this.assetBucketMOCs.get(i), time);
            totalValue += bucketValues[i];
        }

        // Guard against zero total
        if (totalValue <= 0.0) {
            System.out.println("**** ConcentrationDriftModel: time=" + time
                    + " WARNING: totalValue <= 0, returning 0.0");
            return 0.0;
        }

        // Compute shares and HHI
        double hhi = 0.0;
        double maxShare = 0.0;
        String maxBucket = "";

        for (int i = 0; i < bucketValues.length; i++) {
            double share = bucketValues[i] / totalValue;
            hhi += share * share;
            if (share > maxShare) {
                maxShare = share;
                maxBucket = this.assetBucketMOCs.get(i);
            }
        }

        System.out.println("**** ConcentrationDriftModel: time=" + time
                + " totalValue=" + String.format("%.0f", totalValue)
                + " HHI=" + String.format("%.4f", hhi)
                + " maxShare=" + String.format("%.4f", maxShare)
                + " maxBucket=" + maxBucket);

        // Check single-asset concentration breach
        if (maxShare > this.maxSingleAssetShare) {
            double excessConcentration = maxShare - this.maxSingleAssetShare;
            System.out.println("**** ConcentrationDriftModel: CONCENTRATION_BREACH"
                    + " excess=" + String.format("%.4f", excessConcentration));
            return Math.min(1.0, excessConcentration);
        }

        // Check HHI warning
        if (hhi > this.hhiWarningThreshold) {
            double hhiExcess = hhi - this.hhiWarningThreshold;
            System.out.println("**** ConcentrationDriftModel: HHI_WARNING"
                    + " hhiExcess=" + String.format("%.4f", hhiExcess));
            return Math.min(1.0, hhiExcess * 0.5); // scaled signal
        }

        return 0.0;
    }
}
