package uk.gov.ons.census.fwmt.outcomeservice.converter.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.config.GatewayOutcomeQueueConfig;
import uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeServiceProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCache;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;
import uk.gov.ons.census.fwmt.outcomeservice.service.impl.GatewayCacheService;
import uk.gov.ons.census.fwmt.outcomeservice.template.TemplateCreator;

import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static uk.gov.ons.census.fwmt.outcomeservice.enums.EventType.ADDRESS_TYPE_CHANGED;
import static uk.gov.ons.census.fwmt.outcomeservice.util.SpgUtilityMethods.regionLookup;

@Component("ADDRESS_TYPE_CHANGED_HH")
public class AddressTypeChangedHhProcessor implements OutcomeServiceProcessor {

  public static final String PROCESSING_OUTCOME = "PROCESSING_OUTCOME";

  public static final String OUTCOME_SENT = "OUTCOME_SENT";

  @Autowired
  private DateFormat dateFormat;

  @Autowired
  private GatewayOutcomeProducer gatewayOutcomeProducer;

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Autowired
  private GatewayCacheService gatewayCacheService;

  @Override
  public UUID process(OutcomeSuperSetDto outcome, UUID caseIdHolder, String type) throws GatewayException {
    UUID caseId = (caseIdHolder != null) ? caseIdHolder : outcome.getCaseId();
    
    gatewayEventManager.triggerEvent(String.valueOf(caseId), PROCESSING_OUTCOME,
    "survey type", type,
    "processor", "ADDRESS_TYPE_CHANGED_HH",
    "original caseId", String.valueOf(outcome.getCaseId()),
    "Site Case id", (outcome.getSiteCaseId() != null ? String.valueOf(outcome.getSiteCaseId()) : "N/A"));

    Map<String, Object> root = new HashMap<>();
    root.put("caseId", caseId);
    UUID newCaseId = UUID.randomUUID();
    root.put("newCaseId", newCaseId);
    cacheData(outcome, caseId, newCaseId);

    String eventDateTime = dateFormat.format(outcome.getEventDate());
    root.put("outcome", outcome);
    root.put("region", regionLookup(outcome.getOfficerId()));
    root.put("eventDate", eventDateTime);
    root.put("estabType", "HH");
    root.put("usualResidents", 0);

    String outcomeEvent = TemplateCreator.createOutcomeMessage(ADDRESS_TYPE_CHANGED, root);

    gatewayOutcomeProducer.sendOutcome(outcomeEvent, String.valueOf(outcome.getTransactionId()),
        GatewayOutcomeQueueConfig.GATEWAY_ADDRESS_UPDATE_ROUTING_KEY);
    gatewayEventManager.triggerEvent(String.valueOf(caseId), OUTCOME_SENT,
        "survey type", type,
        "type", ADDRESS_TYPE_CHANGED.toString(),
        "transactionId", outcome.getTransactionId().toString(),
        "routing key", GatewayOutcomeQueueConfig.GATEWAY_ADDRESS_UPDATE_ROUTING_KEY);

    return newCaseId;
  }

  private void cacheData(OutcomeSuperSetDto outcome, UUID caseId, UUID newCaseId) throws GatewayException {
    GatewayCache parentCacheJob = gatewayCacheService.getById(caseId.toString());
    if (parentCacheJob == null) {
      throw new GatewayException(GatewayException.Fault.SYSTEM_ERROR, "Parent case does not exist in cache: {}", caseId);
    }

    GatewayCache newCachedJob = gatewayCacheService.getById(newCaseId.toString());
    if (newCachedJob != null) {
      throw new GatewayException(GatewayException.Fault.SYSTEM_ERROR, "New case exists in cache: {}", caseId);
    }

    gatewayCacheService.save(GatewayCache.builder()
        .caseId(newCaseId.toString())
        .existsInFwmt(false)
        .accessInfo(outcome.getAccessInfo())
        .careCodes(OutcomeSuperSetDto.careCodesToText(outcome.getCareCodes()))
        .type(0)
        .build());
  }
}
