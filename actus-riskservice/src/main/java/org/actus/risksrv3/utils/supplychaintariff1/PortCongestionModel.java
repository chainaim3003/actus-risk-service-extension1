package org.actus.risksrv3.utils.supplychaintariff1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.supplychaintariff1.PortCongestionModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PortCongestionModel (Domain 2 — Model 7.6)
 *
 * Port Congestion: physical → financial supply chain delays.
 * References: WTO Trade Monitoring Report, port dwell time statistics
 *
 * Tariff policy changes cause port congestion, extending physical
 * and financial supply chain timelines. Monitors PORT_CONGESTION_INDEX
 * and TARIFF_INDEX.
 *
 * Delay computation:
 *   congestionMultiplier = portCongestionIndex (normalized 0-1)
 *   tariffContribution = tariff × congestionSensitivity
 *   totalDelayDays = baseDwellDays × (1 + congestionMultiplier + tariffContribution)
 *   delayDays = min(totalDelayDays - baseDwellDays, maxDelayDays)
 *
 * Financial impact = delayDays × financialImpactPerDay
 * (working capital cost of delayed receivables per day as fraction of notional)
 *
 * Returns: financial impact fraction (0.0 = no delay impact)
 */
public class PortCongestionModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";

    private final String riskFactorId;
    private final String portCongestionIndexMOC;
    private final String tariffIndexMOC;
    private final double baseDwellDays;
    private final double congestionSensitivity;
    private final double maxDelayDays;
    private final double financialImpactPerDay;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;

    public PortCongestionModel(String riskFactorId,
                               PortCongestionModelData data,
                               MultiMarketRiskModel marketModel) {
        this.riskFactorId           = riskFactorId;
        this.portCongestionIndexMOC = data.getPortCongestionIndexMOC();
        this.tariffIndexMOC         = data.getTariffIndexMOC();
        this.baseDwellDays          = data.getBaseDwellDays();
        this.congestionSensitivity  = data.getCongestionSensitivity();
        this.maxDelayDays           = data.getMaxDelayDays();
        this.financialImpactPerDay  = data.getFinancialImpactPerDay();
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

    /**
     * Compute financial impact of port congestion delays.
     *
     * @return financial impact fraction per notional (0.0 = no delay)
     */
    @Override
    public double stateAt(String id, LocalDateTime time, StateSpace states) {

        double congestionIndex = this.marketModel.stateAt(this.portCongestionIndexMOC, time);
        double currentTariff = this.marketModel.stateAt(this.tariffIndexMOC, time);

        // Tariff-induced congestion contribution
        double tariffContribution = currentTariff * this.congestionSensitivity;

        // Total dwell time under stress
        double totalDwell = this.baseDwellDays * (1.0 + congestionIndex + tariffContribution);

        // Extra delay days beyond baseline
        double delayDays = totalDwell - this.baseDwellDays;
        delayDays = Math.max(0.0, delayDays);
        delayDays = Math.min(delayDays, this.maxDelayDays);

        // Financial impact: cost per day of delay × number of delay days
        double financialImpact = delayDays * this.financialImpactPerDay;

        System.out.println("**** PortCongestionModel: time=" + time
                + " congestionIdx=" + String.format("%.3f", congestionIndex)
                + " tariff=" + String.format("%.4f", currentTariff)
                + " dwell=" + String.format("%.1f→%.1f", this.baseDwellDays, totalDwell)
                + " delayDays=" + String.format("%.1f", delayDays)
                + " financialImpact=" + String.format("%.6f", financialImpact));

        return financialImpact;
    }
}
