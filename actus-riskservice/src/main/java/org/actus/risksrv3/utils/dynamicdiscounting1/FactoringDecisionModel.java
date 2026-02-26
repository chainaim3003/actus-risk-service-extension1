package org.actus.risksrv3.utils.dynamicdiscounting1;

import org.actus.risksrv3.core.attributes.ContractModel;
import org.actus.risksrv3.core.states.StateSpace;
import org.actus.risksrv3.models.CalloutData;
import org.actus.risksrv3.models.dynamicdiscounting1.FactoringDecisionModelData;
import org.actus.risksrv3.utils.BehaviorRiskModelProvider;
import org.actus.risksrv3.utils.MultiMarketRiskModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * FactoringDecisionModel — Compares 3 financing channels:
 *   1. Dynamic discount (configurable function)
 *   2. Bank factoring (fixed APR)
 *   3. Reverse factoring (fixed APR)
 * Returns 1.0 if dynamic discount is cheapest, triggering early settlement.
 */
public class FactoringDecisionModel implements BehaviorRiskModelProvider {

    public static final String CALLOUT_TYPE = "MRD";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final String riskFactorId;
    private final LocalDateTime invoiceDate;
    private final LocalDateTime dueDate;
    private final double notionalAmount;
    private final String discountFunctionType;
    private final double maxDiscountRate;
    private final double decayLambda;
    private final double powerAlpha;
    private final TreeMap<Integer, Double> stepDiscountSchedule;
    private final double bankFactoringRateAnnualized;
    private final double reverseFactoringRateAnnualized;
    private final List<String> monitoringEventTimes;
    private final MultiMarketRiskModel marketModel;
    private boolean settled = false;

    public FactoringDecisionModel(String riskFactorId, FactoringDecisionModelData data, MultiMarketRiskModel marketModel) {
        this.riskFactorId = riskFactorId;
        this.invoiceDate = LocalDateTime.parse(data.getInvoiceDate(), FMT);
        this.dueDate = LocalDateTime.parse(data.getDueDate(), FMT);
        this.notionalAmount = data.getNotionalAmount();
        this.discountFunctionType = data.getDiscountFunctionType() != null ? data.getDiscountFunctionType() : "LINEAR";
        this.maxDiscountRate = data.getMaxDiscountRate() > 0 ? data.getMaxDiscountRate() : 0.02;
        this.decayLambda = data.getDecayLambda() > 0 ? data.getDecayLambda() : 3.0;
        this.powerAlpha = data.getPowerAlpha() > 0 ? data.getPowerAlpha() : 1.0;
        this.stepDiscountSchedule = data.getStepDiscountSchedule() != null ? new TreeMap<>(data.getStepDiscountSchedule()) : new TreeMap<>();
        this.bankFactoringRateAnnualized = data.getBankFactoringRateAnnualized();
        this.reverseFactoringRateAnnualized = data.getReverseFactoringRateAnnualized();
        this.monitoringEventTimes = data.getMonitoringEventTimes();
        this.marketModel = marketModel;
    }

    @Override public Set<String> keys() { Set<String> k = new HashSet<>(); k.add(riskFactorId); return k; }
    @Override public List<CalloutData> contractStart(ContractModel contract) {
        List<CalloutData> c = new ArrayList<>();
        for (String t : monitoringEventTimes) c.add(new CalloutData(riskFactorId, t, CALLOUT_TYPE));
        return c;
    }
    @Override public double stateAt(String id, LocalDateTime time, StateSpace states) {
        if (settled || states.notionalPrincipal <= 0.0 || time.isAfter(dueDate)) return 0.0;
        long daysSince = ChronoUnit.DAYS.between(invoiceDate, time);
        long totalDays = ChronoUnit.DAYS.between(invoiceDate, dueDate);
        long daysRemaining = ChronoUnit.DAYS.between(time, dueDate);
        if (totalDays <= 0 || daysRemaining <= 0) return 0.0;
        double t = (double) daysSince / totalDays;
        double discount;
        switch (discountFunctionType.toUpperCase()) {
            case "LINEAR": discount = maxDiscountRate * (1.0 - t); break;
            case "EXPONENTIAL": discount = maxDiscountRate * Math.exp(-decayLambda * t); break;
            case "POWER": discount = maxDiscountRate * (1.0 - Math.pow(t, powerAlpha)); break;
            default: discount = maxDiscountRate * (1.0 - t);
        }
        double dynDiscAPR = discount * 365.0 / daysRemaining;
        double bankCost = notionalAmount * bankFactoringRateAnnualized * daysRemaining / 365.0;
        double reverseCost = notionalAmount * reverseFactoringRateAnnualized * daysRemaining / 365.0;
        double dynDiscCost = notionalAmount * discount;
        String cheapest = "BANK";
        double cheapestCost = bankCost;
        if (reverseCost < cheapestCost) { cheapest = "REVERSE"; cheapestCost = reverseCost; }
        if (dynDiscCost < cheapestCost) { cheapest = "DYNDISC"; cheapestCost = dynDiscCost; }
        System.out.println("**** FactoringDecisionModel: time=" + time + " dynDisc=$" + String.format("%.2f", dynDiscCost) + "(APR=" + String.format("%.2f%%", dynDiscAPR*100) + ") bank=$" + String.format("%.2f", bankCost) + " reverse=$" + String.format("%.2f", reverseCost) + " → " + cheapest);
        if ("DYNDISC".equals(cheapest)) { settled = true; return 1.0; }
        return 0.0;
    }
}
