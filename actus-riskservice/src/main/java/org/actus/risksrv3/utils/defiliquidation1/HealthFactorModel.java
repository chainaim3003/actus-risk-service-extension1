package org.actus.risksrv3.utils.defiliquidation1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.defiliquidation1.HealthFactorModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * HealthFactorModel (Domain 1 — Model 2.2)
 *
 * Computes the multi-collateral weighted Health Factor for DeFi lending positions:
 *   HF = Sum(collateral_i x price_i x liquidationThreshold_i) / totalDebt
 *
 * Extends CollateralLTVModel for multi-asset scenarios (ETH + wstETH + USDC).
 *
 * Data sources: CoinGecko API, Glassnode, DeFi Llama, CryptoCompare,
 *   farside.co.uk/eth (ETH ETF flow behavioral sentiment)
 *
 * Scholarly: Aave V3 HF formula, Qin et al. (2021), Compound V3 Account Liquidity
 */
public class HealthFactorModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final List<String> collateralMOCs;
    private final List<Double> collateralQuantities;
    private final List<Double> liquidationThresholds;
    private final double healthyThreshold;
    private final double targetHealthFactor;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public HealthFactorModel(String riskFactorId, HealthFactorModelData data,
                             MultiMarketRiskModel marketModel) {
        this.riskFactorId           = riskFactorId;
        this.collateralMOCs         = data.getCollateralMOCs();
        this.collateralQuantities   = data.getCollateralQuantities();
        this.liquidationThresholds  = data.getLiquidationThresholds();
        this.healthyThreshold       = data.getHealthyThreshold();
        this.targetHealthFactor     = data.getTargetHealthFactor();
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
        double weightedCollateral = 0.0;
        for (int i = 0; i < collateralMOCs.size(); i++) {
            double price = this.marketModel.stateAt(collateralMOCs.get(i), time);
            double qty = collateralQuantities.get(i);
            double liqThreshold = liquidationThresholds.get(i);
            weightedCollateral += qty * price * liqThreshold;
        }
        double totalDebt = states.notionalPrincipal + states.accruedInterest;
        if (totalDebt <= 0.0) return 0.0;
        double healthFactor = weightedCollateral / totalDebt;

        System.out.println("**** HealthFactorModel: time=" + time
                + " weightedCollateral=" + String.format("%.2f", weightedCollateral)
                + " totalDebt=" + String.format("%.2f", totalDebt)
                + " HF=" + String.format("%.4f", healthFactor));

        if (healthFactor < 1.0) {
            System.out.println("**** HealthFactorModel: LIQUIDATION HF=" + String.format("%.4f", healthFactor));
            return 1.0;
        } else if (healthFactor < this.healthyThreshold) {
            double targetDebt = weightedCollateral / this.targetHealthFactor;
            double repayFraction = (totalDebt - targetDebt) / totalDebt;
            System.out.println("**** HealthFactorModel: PARTIAL_REPAY fraction=" + String.format("%.4f", repayFraction));
            return Math.max(0.0, Math.min(1.0, repayFraction));
        }
        return 0.0;
    }
}
