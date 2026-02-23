package org.actus.risksrv3.controllers;
import  org.actus.risksrv3.models.ReferenceIndex;
import  org.actus.risksrv3.models.Scenario;
import  org.actus.risksrv3.models.TwoDimensionalPrepaymentModelData;
import  org.actus.risksrv3.models.TwoDimensionalDepositTrxModelData;
import  org.actus.risksrv3.models.CollateralLTVModelData;
// ====== STABLECOIN MODEL IMPORTS ======
import  org.actus.risksrv3.models.stablecoin.BackingRatioModelData;
import  org.actus.risksrv3.models.stablecoin.RedemptionPressureModelData;
import  org.actus.risksrv3.models.stablecoin.MaturityLadderModelData;
import  org.actus.risksrv3.models.stablecoin.AssetQualityModelData;
import  org.actus.risksrv3.models.stablecoin.ConcentrationDriftModelData;
import  org.actus.risksrv3.models.stablecoin.ComplianceDriftModelData;
import  org.actus.risksrv3.models.stablecoin.EarlyWarningModelData;
import  org.actus.risksrv3.models.stablecoin.ContinuousAttestationModelData;
// ====== END STABLECOIN MODEL IMPORTS ======
import  org.actus.risksrv3.repository.ReferenceIndexStore;
import  org.actus.risksrv3.repository.ScenarioStore;
import  org.actus.risksrv3.repository.TwoDimensionalPrepaymentModelStore;
import  org.actus.risksrv3.repository.TwoDimensionalDepositTrxModelStore;
import  org.actus.risksrv3.repository.CollateralLTVModelStore;
// ====== STABLECOIN STORE IMPORTS ======
import  org.actus.risksrv3.repository.stablecoin.BackingRatioModelStore;
import  org.actus.risksrv3.repository.stablecoin.RedemptionPressureModelStore;
import  org.actus.risksrv3.repository.stablecoin.MaturityLadderModelStore;
import  org.actus.risksrv3.repository.stablecoin.AssetQualityModelStore;
import  org.actus.risksrv3.repository.stablecoin.ConcentrationDriftModelStore;
import  org.actus.risksrv3.repository.stablecoin.ComplianceDriftModelStore;
import  org.actus.risksrv3.repository.stablecoin.EarlyWarningModelStore;
import  org.actus.risksrv3.repository.stablecoin.ContinuousAttestationModelStore;
// ====== END STABLECOIN STORE IMPORTS ======
import  org.springframework.beans.factory.annotation.Autowired;
import  org.springframework.beans.factory.annotation.Value;
import  org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

//Annotation 
@RestController
public class RiskDataManager {
	@Autowired
	private ReferenceIndexStore referenceIndexStore;	
	@Autowired
	private ScenarioStore scenarioStore;
	@Autowired
	private TwoDimensionalPrepaymentModelStore twoDimensionalPrepaymentModelStore;
	@Autowired
	private TwoDimensionalDepositTrxModelStore twoDimensionalDepositTrxModelStore;
	@Autowired
	private CollateralLTVModelStore collateralLTVModelStore;

	// ====== STABLECOIN MODEL STORES ======
	@Autowired
	private BackingRatioModelStore backingRatioModelStore;
	@Autowired
	private RedemptionPressureModelStore redemptionPressureModelStore;
	@Autowired
	private MaturityLadderModelStore maturityLadderModelStore;
	@Autowired
	private AssetQualityModelStore assetQualityModelStore;
	@Autowired
	private ConcentrationDriftModelStore concentrationDriftModelStore;
	@Autowired
	private ComplianceDriftModelStore complianceDriftModelStore;
	@Autowired
	private EarlyWarningModelStore earlyWarningModelStore;
	@Autowired
	private ContinuousAttestationModelStore continuousAttestationModelStore;
	// ====== END STABLECOIN MODEL STORES ======
	
	private
	@Value("${spring.data.mongodb.host}")
	String mongodbHost;
	
	private
	@Value("${spring.data.mongodb.port}")
	Integer mongodbPort;
	
	// demonstrate access to application properties 
	@GetMapping("/propertiesMongoHost")
	public String doPropertiesMongoHost ( ) {
	    return ( "Value of mongodbHost = " + mongodbHost + "\n");
	}
	
	@GetMapping("/propertiesMongoPort")
	public String doPropertiesMongoPort ( ) {
		return ( "Value of mongodbPort = " + mongodbPort + "\n");
	}
	
	@PostMapping("/addReferenceIndex")
    public String saveReferenceIndex(@RequestBody ReferenceIndex referenceIndex){
        referenceIndexStore.save(referenceIndex);      
        return "ReferenceIndex added Successfully\n";
    }	
	// Path parameter id is ReferenceIndexID  i.e. riskFactorType == "ReferenceIndex" in any descriptor 
    @DeleteMapping("/deleteReferenceIndex/{id}")
    public String deleteReferenceIndex(@PathVariable String id){
        referenceIndexStore.deleteById(id);     
        return "ReferenceIndex deleted Successfully\n";
    }   
    @GetMapping("/findReferenceIndex/{id}")
    public Optional<ReferenceIndex> findReferenceIndex(@PathVariable String id) {
    	Optional<ReferenceIndex> rx = referenceIndexStore.findById(id);
    	return rx;
    }
	
    @GetMapping("/findAllReferenceIndexes")
    public List<ReferenceIndex> getReferenceIndexes() {      
    	return referenceIndexStore.findAll();
    }
    
    // id is here a ScenarioID  i.e. riskFactorType == "Scenario" in any descriptor
	@PostMapping("/addScenario")
    public String saveScenario(@RequestBody Scenario scenario){
        scenarioStore.save(scenario);      
        return "Scenario added Successfully\n";
    }	
	// id is here a ScenarioID i.e. riskFactorType == "Scenario" in any descriptor
    @DeleteMapping("/deleteScenario/{id}")
    public String deleteScenario(@PathVariable String id){
        scenarioStore.deleteById(id);      
        return "Scenario Deleted Successfully\n";
    }
    @GetMapping("/findScenario/{id}")
    public Optional<Scenario> findScenario(@PathVariable String id) {
    	 return  scenarioStore.findById(id);
    }
    
    @GetMapping("/findAllScenarios")
    public List<Scenario> getScenarios() {
        return scenarioStore.findAll();
    }
    
    // Path Parameter id is here a TwoParameterPrepaymentModelID 
    //i.e.  a String riskFactorID  with associated riskFactorType == "TwoDimensionalPrepaymentModel" 
	@PostMapping("/addTwoDimensionalPrepaymentModel")
    public String saveTwoDimensionalPrepaymentModelData(
    		@RequestBody TwoDimensionalPrepaymentModelData twoDimensionalPrepaymentModelData){
        twoDimensionalPrepaymentModelStore.save(twoDimensionalPrepaymentModelData);      
        return "TwoDimensionalPrepayment model added successfully\n";
    }	
	// id is a TwoDimensionalPrepaymentModelID 
    @DeleteMapping("/deleteTwoDimensionalPrepaymentModel/{id}")
    public String deleteTwoDimensionalPrepaymentModel(@PathVariable String id){
        twoDimensionalPrepaymentModelStore.deleteById(id);      
        return "TwoDimensionalPrepaymentModel deleted Successfully\n";
    }
    @GetMapping("/findTwoDimensionalPrepaymentModel/{id}")
    public Optional<TwoDimensionalPrepaymentModelData> findTwoDimensionalPrepaymentModelData(@PathVariable String id) {
    	 return  twoDimensionalPrepaymentModelStore.findById(id);
    }
    
    @GetMapping("/findAllTwoDimensionalPrepaymentModels")
    public List<TwoDimensionalPrepaymentModelData> getTwoDimensionalPrepaymentModels() {
        return twoDimensionalPrepaymentModelStore.findAll();
    }
    
    // Path Parameter id is here a TwoParameterDepositTrxModelID 
    //i.e.  a String riskFactorID with associated riskFactorType == "TwoDimensionalDepositTrxModel" 
	@PostMapping("/addTwoDimensionalDepositTrxModel")
    public String saveTwoDimensionalDepositTrxModelData(
    		@RequestBody TwoDimensionalDepositTrxModelData twoDimensionalDepositTrxModelData){
        twoDimensionalDepositTrxModelStore.save(twoDimensionalDepositTrxModelData);      
        return "TwoDimensionalDepositTrx model added successfully\n";
    }
	// id is a TwoDimensionalDepositTrxModelID 
    @DeleteMapping("/deleteTwoDimensionalDepositTrxModel/{id}")
    public String deleteTwoDimensionalDepositTrxModel(@PathVariable String id){
        twoDimensionalDepositTrxModelStore.deleteById(id);      
        return "TwoDimensionalDepositTrx model deleted Successfully\n";
    }
    @GetMapping("/findTwoDimensionalDepositTrxModel/{id}")
    public Optional<TwoDimensionalDepositTrxModelData> findTwoDimensionalDepositTrxModelData(@PathVariable String id) {
    	 return  twoDimensionalDepositTrxModelStore.findById(id);
    }
    
    @GetMapping("/findAllTwoDimensionalDepositTrxModels")
    public List<TwoDimensionalDepositTrxModelData> getTwoDimensionalDepositTrxModels() {
        return twoDimensionalDepositTrxModelStore.findAll();
    }

    // =========================================================================
    // CollateralLTVModel CRUD endpoints
    // =========================================================================

    @PostMapping("/addCollateralLTVModel")
    public String saveCollateralLTVModelData(
    		@RequestBody CollateralLTVModelData collateralLTVModelData){
        collateralLTVModelStore.save(collateralLTVModelData);
        return "CollateralLTVModel added successfully\n";
    }

    @DeleteMapping("/deleteCollateralLTVModel/{id}")
    public String deleteCollateralLTVModel(@PathVariable String id){
        collateralLTVModelStore.deleteById(id);
        return "CollateralLTVModel deleted Successfully\n";
    }

    @GetMapping("/findCollateralLTVModel/{id}")
    public Optional<CollateralLTVModelData> findCollateralLTVModelData(@PathVariable String id) {
        return collateralLTVModelStore.findById(id);
    }

    @GetMapping("/findAllCollateralLTVModels")
    public List<CollateralLTVModelData> getCollateralLTVModels() {
        return collateralLTVModelStore.findAll();
    }

    // =========================================================================
    // STABLECOIN BEHAVIORAL MODEL CRUD ENDPOINTS (8 models × 4 endpoints = 32)
    // riskFactorType values in scenario descriptors:
    //   "BackingRatioModel", "RedemptionPressureModel", "MaturityLadderModel",
    //   "AssetQualityModel", "ConcentrationDriftModel", "ComplianceDriftModel",
    //   "EarlyWarningModel", "ContinuousAttestationModel"
    // =========================================================================

    // --- 6.1 BackingRatioModel ---
    @PostMapping("/addBackingRatioModel")
    public String saveBackingRatioModelData(
            @RequestBody BackingRatioModelData data) {
        backingRatioModelStore.save(data);
        return "BackingRatioModel added successfully\n";
    }
    @DeleteMapping("/deleteBackingRatioModel/{id}")
    public String deleteBackingRatioModel(@PathVariable String id) {
        backingRatioModelStore.deleteById(id);
        return "BackingRatioModel deleted successfully\n";
    }
    @GetMapping("/findBackingRatioModel/{id}")
    public Optional<BackingRatioModelData> findBackingRatioModelData(@PathVariable String id) {
        return backingRatioModelStore.findById(id);
    }
    @GetMapping("/findAllBackingRatioModels")
    public List<BackingRatioModelData> getBackingRatioModels() {
        return backingRatioModelStore.findAll();
    }

    // --- 6.2 RedemptionPressureModel ---
    @PostMapping("/addRedemptionPressureModel")
    public String saveRedemptionPressureModelData(
            @RequestBody RedemptionPressureModelData data) {
        redemptionPressureModelStore.save(data);
        return "RedemptionPressureModel added successfully\n";
    }
    @DeleteMapping("/deleteRedemptionPressureModel/{id}")
    public String deleteRedemptionPressureModel(@PathVariable String id) {
        redemptionPressureModelStore.deleteById(id);
        return "RedemptionPressureModel deleted successfully\n";
    }
    @GetMapping("/findRedemptionPressureModel/{id}")
    public Optional<RedemptionPressureModelData> findRedemptionPressureModelData(@PathVariable String id) {
        return redemptionPressureModelStore.findById(id);
    }
    @GetMapping("/findAllRedemptionPressureModels")
    public List<RedemptionPressureModelData> getRedemptionPressureModels() {
        return redemptionPressureModelStore.findAll();
    }

    // --- 6.3 MaturityLadderModel ---
    @PostMapping("/addMaturityLadderModel")
    public String saveMaturityLadderModelData(
            @RequestBody MaturityLadderModelData data) {
        maturityLadderModelStore.save(data);
        return "MaturityLadderModel added successfully\n";
    }
    @DeleteMapping("/deleteMaturityLadderModel/{id}")
    public String deleteMaturityLadderModel(@PathVariable String id) {
        maturityLadderModelStore.deleteById(id);
        return "MaturityLadderModel deleted successfully\n";
    }
    @GetMapping("/findMaturityLadderModel/{id}")
    public Optional<MaturityLadderModelData> findMaturityLadderModelData(@PathVariable String id) {
        return maturityLadderModelStore.findById(id);
    }
    @GetMapping("/findAllMaturityLadderModels")
    public List<MaturityLadderModelData> getMaturityLadderModels() {
        return maturityLadderModelStore.findAll();
    }

    // --- 6.4 AssetQualityModel ---
    @PostMapping("/addAssetQualityModel")
    public String saveAssetQualityModelData(
            @RequestBody AssetQualityModelData data) {
        assetQualityModelStore.save(data);
        return "AssetQualityModel added successfully\n";
    }
    @DeleteMapping("/deleteAssetQualityModel/{id}")
    public String deleteAssetQualityModel(@PathVariable String id) {
        assetQualityModelStore.deleteById(id);
        return "AssetQualityModel deleted successfully\n";
    }
    @GetMapping("/findAssetQualityModel/{id}")
    public Optional<AssetQualityModelData> findAssetQualityModelData(@PathVariable String id) {
        return assetQualityModelStore.findById(id);
    }
    @GetMapping("/findAllAssetQualityModels")
    public List<AssetQualityModelData> getAssetQualityModels() {
        return assetQualityModelStore.findAll();
    }

    // --- 6.5 ConcentrationDriftModel ---
    @PostMapping("/addConcentrationDriftModel")
    public String saveConcentrationDriftModelData(
            @RequestBody ConcentrationDriftModelData data) {
        concentrationDriftModelStore.save(data);
        return "ConcentrationDriftModel added successfully\n";
    }
    @DeleteMapping("/deleteConcentrationDriftModel/{id}")
    public String deleteConcentrationDriftModel(@PathVariable String id) {
        concentrationDriftModelStore.deleteById(id);
        return "ConcentrationDriftModel deleted successfully\n";
    }
    @GetMapping("/findConcentrationDriftModel/{id}")
    public Optional<ConcentrationDriftModelData> findConcentrationDriftModelData(@PathVariable String id) {
        return concentrationDriftModelStore.findById(id);
    }
    @GetMapping("/findAllConcentrationDriftModels")
    public List<ConcentrationDriftModelData> getConcentrationDriftModels() {
        return concentrationDriftModelStore.findAll();
    }

    // --- 6.6 ComplianceDriftModel ---
    @PostMapping("/addComplianceDriftModel")
    public String saveComplianceDriftModelData(
            @RequestBody ComplianceDriftModelData data) {
        complianceDriftModelStore.save(data);
        return "ComplianceDriftModel added successfully\n";
    }
    @DeleteMapping("/deleteComplianceDriftModel/{id}")
    public String deleteComplianceDriftModel(@PathVariable String id) {
        complianceDriftModelStore.deleteById(id);
        return "ComplianceDriftModel deleted successfully\n";
    }
    @GetMapping("/findComplianceDriftModel/{id}")
    public Optional<ComplianceDriftModelData> findComplianceDriftModelData(@PathVariable String id) {
        return complianceDriftModelStore.findById(id);
    }
    @GetMapping("/findAllComplianceDriftModels")
    public List<ComplianceDriftModelData> getComplianceDriftModels() {
        return complianceDriftModelStore.findAll();
    }

    // --- 6.7 EarlyWarningModel ---
    @PostMapping("/addEarlyWarningModel")
    public String saveEarlyWarningModelData(
            @RequestBody EarlyWarningModelData data) {
        earlyWarningModelStore.save(data);
        return "EarlyWarningModel added successfully\n";
    }
    @DeleteMapping("/deleteEarlyWarningModel/{id}")
    public String deleteEarlyWarningModel(@PathVariable String id) {
        earlyWarningModelStore.deleteById(id);
        return "EarlyWarningModel deleted successfully\n";
    }
    @GetMapping("/findEarlyWarningModel/{id}")
    public Optional<EarlyWarningModelData> findEarlyWarningModelData(@PathVariable String id) {
        return earlyWarningModelStore.findById(id);
    }
    @GetMapping("/findAllEarlyWarningModels")
    public List<EarlyWarningModelData> getEarlyWarningModels() {
        return earlyWarningModelStore.findAll();
    }

    // --- 6.8 ContinuousAttestationModel ---
    @PostMapping("/addContinuousAttestationModel")
    public String saveContinuousAttestationModelData(
            @RequestBody ContinuousAttestationModelData data) {
        continuousAttestationModelStore.save(data);
        return "ContinuousAttestationModel added successfully\n";
    }
    @DeleteMapping("/deleteContinuousAttestationModel/{id}")
    public String deleteContinuousAttestationModel(@PathVariable String id) {
        continuousAttestationModelStore.deleteById(id);
        return "ContinuousAttestationModel deleted successfully\n";
    }
    @GetMapping("/findContinuousAttestationModel/{id}")
    public Optional<ContinuousAttestationModelData> findContinuousAttestationModelData(@PathVariable String id) {
        return continuousAttestationModelStore.findById(id);
    }
    @GetMapping("/findAllContinuousAttestationModels")
    public List<ContinuousAttestationModelData> getContinuousAttestationModels() {
        return continuousAttestationModelStore.findAll();
    }
}
