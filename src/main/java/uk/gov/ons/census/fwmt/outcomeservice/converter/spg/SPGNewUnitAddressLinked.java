package uk.gov.ons.census.fwmt.outcomeservice.converter.spg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.data.spg.SPGOutcome;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.converter.SPGOutcomeServiceProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;
import uk.gov.ons.census.fwmt.outcomeservice.service.impl.GatewayCacheService;
import uk.gov.ons.census.fwmt.outcomeservice.template.TemplateCreator;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static uk.gov.ons.census.fwmt.outcomeservice.config.GatewayEventsConfig.CESPG_ADDRESS_NOT_VALID_OUTCOME_SENT;
import static uk.gov.ons.census.fwmt.outcomeservice.config.GatewayEventsConfig.CESPG_OUTCOME_SENT;
import static uk.gov.ons.census.fwmt.outcomeservice.enums.EventType.NEW_UNIT_ADDRESS;
import static uk.gov.ons.census.fwmt.outcomeservice.enums.SurveyType.spg;

@Component("NEW_UNIT_ADDRESS")
public class SPGNewUnitAddressLinked implements SPGOutcomeServiceProcessor {

  @Autowired
  private GatewayOutcomeProducer gatewayOutcomeProducer;

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Autowired
  private GatewayCacheService gatewayCacheService;

  @Override
  public void processMessage(SPGOutcome spgOutcome) throws GatewayException {
    /** TODO
     * given this is a new address, there should NOT be an entry in the Gateway Cache (an error case if it does), therefore create a new entry:
     * keyed on the new Unit case Id
     * set the delivered flag:
     * true - if a linked QID is provided with the new Unit address request (because the new Unit case Id is generated by the Gateway)
     * false - default
     * existsInField is set to false (because it hasn't been created in the tool yet; thus enabling the SPG Unit Create in Deliver or Follow-up modes to trigger after RM send it)
     * store all Care Codes and Access Information
     */

    // TODO : caseId is provided in the Outcome URL request, not the body
    //a newUnitCaseId (UUID) has to be generated by the Gateway and provided in the collectionCase.id field
    UUID newRandomUUID = UUID.randomUUID();
    String eventDateTime = spgOutcome.getEventDate().toString();

    Map<String, Object> root = new HashMap<>();
    root.put("spgOutcome", spgOutcome);
    root.put("generatedUuid", newRandomUUID);
    root.put("eventDate", eventDateTime + "Z");
    root.put("agentId", spgOutcome.getOfficerId());
    root.put("address", spgOutcome.getAddress());

    String outcomeEvent = TemplateCreator.createOutcomeMessage(NEW_UNIT_ADDRESS, root, spg);

    gatewayOutcomeProducer.sendPropertyListing(outcomeEvent, String.valueOf(spgOutcome.getTransactionId()));
    gatewayEventManager.triggerEvent(String.valueOf(newRandomUUID), CESPG_OUTCOME_SENT,
        "type", CESPG_ADDRESS_NOT_VALID_OUTCOME_SENT, "transactionId",
        spgOutcome.getTransactionId().toString());
  }
}
