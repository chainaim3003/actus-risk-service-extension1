package org.actus.risksrv3.utils.defiliquidation1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.defiliquidation1.CollateralVelocityModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * CollateralVelocityModel (Domain 1 — Model 2.3)
 *
 * Monitors the RATE OF CHANGE of LTV deterioration, not just LTV level.
 * Provides early warning by computing dLTV/dt (velocity) and estimating
 * days-to-liquidation.
 *
 * Key insight: ETH dropping $50/day vs $500/day both show "LTV = 75%" but
 * have very different urgency. This model triggers pre-emptive intervention
 * when velocity indicates imminent liquidation even if current LTV is safe.
 *
 * Decision logic:
 *   daysToLiquidation > safeHorizonDays  → return 0.0 (healthy pace)
 *   daysToLiquidation <= urgentDays       → return fraction for aggressive repay
 *   daysToLiquidation <= safeHorizonDays  → return smaller fraction for moderate repay
 *
 * Maintains rolling window of LTV values from previous callouts.
 *
 * Volatility data sources for velocity estimation:
 *   - CoinGecko API: hourly/daily OHLCV for ETH price velocity
 *   - Deribit ETH DVOL: implied volatility index (behavioral forward-looking)
 *   - CryptoDataDownload API: historical DVOL OHLC values
 *   - Glassnode: realized volatility, SOPR, exchange net flows
 *   - ETH ETF flow data (farside.co.uk/eth): institutional behavioral signal
 *     Large outflows → bearish velocity signal → tighten velocity thresholds
 *
 * Historical ETH volatility context for calibration:
 *   2017-2018: Annualized vol 80-150% (ICO boom/bust)
 *   2020: March crash 200%+ annualized, then 60-90%
 *   2021: Bull run 70-110% annualized
 *   2022: UST/LUNA crash 120%+, then 50-80% bear market
 *   2023: Recovery 40-60% annualized
 *   2024: ETF approval rally 50-80%, then normalization
 *   2025-2026: 40-70% base, spikes to 100%+ during events
 *
 * Place this file in:
 *   src/main/java/org/actus/risksrv3/utils/defiliquidation1/
 */
public class CollateralVelocityModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String collateralPriceMOC;
    private final double collateralQuantity;
    private final double liquidationThreshold;     // e.g. 0.83
    private final double safeHorizonDays;          // e.g. 7 days
    private final double urgentDays;               // e.g. 2 days
    private final double moderateRepayFraction;    // e.g. 0.10
    private final double aggressiveRepayFraction;  // e.g. 0.25
    private final int rollingWindowSize;           // e.g. 5 data points
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    // Rolling LTV history for velocity computation
    private final LinkedList<double[]> ltvHistory = new LinkedList<>(); // [0]=time_epoch, [1]=ltv

    public CollateralVelocityModel(String riskFactorId,
                                   CollateralVelocityModelData data,
                                   MultiMarketRiskModel marketModel) {
        this.riskFactorId           = riskFactorId;
        this.collateralPriceMOC     = data.getCollateralPriceMOC();
        this.collateralQuantity     = data.getCollateralQuantity();
        this.liquidationThreshold   = data.getLiquidationThreshold();
        this.safeHorizonDays        = data.getSafeHorizonDays();
        this.urgentDays             = data.getUrgentDays();
        this.moderateRepayFraction  = data.getModerateRepayFraction();
        this.aggressiveRepayFraction= data.getAggressiveRepayFraction();
        this.rollingWindowSize      = data.getRollingWindowSize();
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

        double ethPrice = this.marketModel.stateAt(this.collateralPriceMOC, time);
        if (ethPrice <= 0.0) return 0.0;

        double collateralValue = this.collateralQuantity * ethPrice;
        double debt = states.notionalPrincipal + states.accruedInterest;
        if (debt <= 0.0) return 0.0;

        double currentLTV = debt / collateralValue;

        // Store in rolling window
        long epochDay = time.toLocalDate().toEpochDay();
        ltvHistory.addLast(new double[]{epochDay, currentLTV});
        if (ltvHistory.size() > rollingWindowSize) {
            ltvHistory.removeFirst();
        }

        // Compute velocity (dLTV/dt in LTV-per-day)
        double velocity = 0.0;
        double daysToLiquidation = Double.MAX_VALUE;

        if (ltvHistory.size() >= 2) {
            double[] oldest = ltvHistory.getFirst();
            double[] newest = ltvHistory.getLast();
            double daysDiff = newest[0] - oldest[0];
            double ltvDiff = newest[1] - oldest[1];

            if (daysDiff > 0) {
                velocity = ltvDiff / daysDiff; // positive = deteriorating

                if (velocity > 0.0) {
                    double ltvGap = this.liquidationThreshold - currentLTV;
                    if (ltvGap > 0) {
                        daysToLiquidation = ltvGap / velocity;
                    } else {
                        daysToLiquidation = 0.0; // already past threshold
                    }
                }
            }
        }

        System.out.println("**** CollateralVelocityModel: time=" + time
                + " LTV=" + String.format("%.4f", currentLTV)
                + " velocity=" + String.format("%.6f", velocity) + "/day"
                + " daysToLiq=" + String.format("%.1f", daysToLiquidation)
                + " windowSize=" + ltvHistory.size());

        // Decision based on time-to-liquidation
        if (daysToLiquidation <= 0.0) {
            System.out.println("**** CollateralVelocityModel: PAST_THRESHOLD");
            return 1.0;
        } else if (daysToLiquidation <= urgentDays) {
            System.out.println("**** CollateralVelocityModel: URGENT daysToLiq="
                    + String.format("%.1f", daysToLiquidation));
            return aggressiveRepayFraction;
        } else if (daysToLiquidation <= safeHorizonDays) {
            System.out.println("**** CollateralVelocityModel: MODERATE daysToLiq="
                    + String.format("%.1f", daysToLiquidation));
            return moderateRepayFraction;
        } else {
            return 0.0;
        }
    }
}
