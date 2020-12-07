package uk.gov.ons.census.fwmt.outcomeservice.converter.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.config.GatewayOutcomeQueueConfig;
import uk.gov.ons.census.fwmt.outcomeservice.converter.OutcomeServiceProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCache;
import uk.gov.ons.census.fwmt.outcomeservice.data.GatewayCache.GatewayCacheBuilder;
import uk.gov.ons.census.fwmt.outcomeservice.dto.FulfilmentRequestDto;
import uk.gov.ons.census.fwmt.outcomeservice.dto.OutcomeSuperSetDto;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;
import uk.gov.ons.census.fwmt.outcomeservice.service.impl.GatewayCacheService;
import uk.gov.ons.census.fwmt.outcomeservice.template.TemplateCreator;

import javax.transaction.Transactional;
import java.text.DateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static uk.gov.ons.census.fwmt.outcomeservice.enums.EventType.QUESTIONNAIRE_LINKED;

@Component("LINKED_QID")
@Transactional
public class LinkedQidProcessor implements OutcomeServiceProcessor {

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

  private boolean isQuestionnaireLinked(FulfilmentRequestDto fulfilmentRequest) {
    return (fulfilmentRequest.getQuestionnaireID() != null);
  }

  @Override
  public UUID process(OutcomeSuperSetDto outcome, UUID caseIdHolder, String type) throws GatewayException {
    gatewayEventManager.triggerEvent(String.valueOf(outcome.getCaseId()), PROCESSING_OUTCOME,
    "survey type", type,
    "processor", "LINKED_QID",
    "original caseId", String.valueOf(outcome.getCaseId()),
    "Site Case id", (outcome.getSiteCaseId() != null ? String.valueOf(outcome.getSiteCaseId()) : "N/A"));


    if (outcome.getFulfilmentRequests() == null) return caseIdHolder;
    UUID caseId = (caseIdHolder != null) ? caseIdHolder : outcome.getCaseId();
    for (FulfilmentRequestDto fulfilmentRequest : outcome.getFulfilmentRequests()) {
      if (isQuestionnaireLinked(fulfilmentRequest)) {
        String eventDateTime = dateFormat.format(outcome.getEventDate());

        Map<String, Object> root = new HashMap<>();
        root.put("outcome", outcome);
        root.put("caseId", caseId);
        root.put("questionnaireId", fulfilmentRequest.getQuestionnaireID());
        root.put("eventDate", eventDateTime);
        cacheData(caseId);

        String outcomeEvent = TemplateCreator.createOutcomeMessage(QUESTIONNAIRE_LINKED, root);

        gatewayOutcomeProducer.sendOutcome(outcomeEvent, String.valueOf(outcome.getTransactionId()),
            GatewayOutcomeQueueConfig.GATEWAY_QUESTIONNAIRE_UPDATE_ROUTING_KEY);
        gatewayEventManager.triggerEvent(String.valueOf(caseId), OUTCOME_SENT,
            "survey type", type,
            "type", QUESTIONNAIRE_LINKED.toString(),
            "transactionId", outcome.getTransactionId().toString(),
            "routing key", GatewayOutcomeQueueConfig.GATEWAY_QUESTIONNAIRE_UPDATE_ROUTING_KEY);
      }
    }
    return caseIdHolder;
  }

  private void cacheData(UUID caseId) {
    GatewayCache cache = gatewayCacheService.getById(String.valueOf(caseId));
    GatewayCacheBuilder builder;
    if (cache == null) builder = GatewayCache.builder();
    else builder = cache.toBuilder();

    gatewayCacheService.save(builder
        .caseId(String.valueOf(caseId))
        .delivered(true)
        .build());
  }
}
