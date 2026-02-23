package org.actus.risksrv3.models.stablecoin;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.util.List;

/**
 * AssetQualityModelData
 *
 * MongoDB document for AssetQualityModel configuration.
 * Collection: assetQualityModels
 *
 * JSON example:
 * {
 *   "riskFactorId": "aq_sc01",
 *   "bankStressIndexMOC": "BANK_STRESS_INDEX",
 *   "sovereignStressMOC": "US_SOVEREIGN_STRESS",
 *   "bankStressThreshold": 0.5,
 *   "baseQuality": 100.0,
 *   "qualityFloor": 50.0,
 *   "sovereignMaxDegradation": 0.30,
 *   "monitoringEventTimes": ["2025-06-01T00:00:00", ...]
 * }
 */
@Document(collection = "assetQualityModels")
public class AssetQualityModelData {

    @Id
    private String riskFactorId;

    private String bankStressIndexMOC;
    private String sovereignStressMOC;
    private double bankStressThreshold;
    private double baseQuality;
    private double qualityFloor;
    private double sovereignMaxDegradation;
    private List<String> monitoringEventTimes;

    public AssetQualityModelData() {
    }

    public String getRiskFactorId() { return riskFactorId; }
    public void setRiskFactorId(String riskFactorId) { this.riskFactorId = riskFactorId; }

    public String getBankStressIndexMOC() { return bankStressIndexMOC; }
    public void setBankStressIndexMOC(String bankStressIndexMOC) { this.bankStressIndexMOC = bankStressIndexMOC; }

    public String getSovereignStressMOC() { return sovereignStressMOC; }
    public void setSovereignStressMOC(String sovereignStressMOC) { this.sovereignStressMOC = sovereignStressMOC; }

    public double getBankStressThreshold() { return bankStressThreshold; }
    public void setBankStressThreshold(double bankStressThreshold) { this.bankStressThreshold = bankStressThreshold; }

    public double getBaseQuality() { return baseQuality; }
    public void setBaseQuality(double baseQuality) { this.baseQuality = baseQuality; }

    public double getQualityFloor() { return qualityFloor; }
    public void setQualityFloor(double qualityFloor) { this.qualityFloor = qualityFloor; }

    public double getSovereignMaxDegradation() { return sovereignMaxDegradation; }
    public void setSovereignMaxDegradation(double sovereignMaxDegradation) { this.sovereignMaxDegradation = sovereignMaxDegradation; }

    public List<String> getMonitoringEventTimes() { return monitoringEventTimes; }
    public void setMonitoringEventTimes(List<String> monitoringEventTimes) { this.monitoringEventTimes = monitoringEventTimes; }
}
