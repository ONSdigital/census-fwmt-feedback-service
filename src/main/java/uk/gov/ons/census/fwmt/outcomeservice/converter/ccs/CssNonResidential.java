package uk.gov.ons.census.fwmt.outcomeservice.converter.ccs;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.fwmt.common.data.ccs.CCSPropertyListingOutcome;
import uk.gov.ons.census.fwmt.common.error.GatewayException;
import uk.gov.ons.census.fwmt.events.component.GatewayEventManager;
import uk.gov.ons.census.fwmt.outcomeservice.converter.CcsOutcomeServiceProcessor;
import uk.gov.ons.census.fwmt.outcomeservice.message.GatewayOutcomeProducer;
import uk.gov.ons.census.fwmt.outcomeservice.template.TemplateCreator;

import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uk.gov.ons.census.fwmt.outcomeservice.config.GatewayEventsConfig.CCSPL_NON_RESIDENTIAL_OUTCOME_SENT;
import static uk.gov.ons.census.fwmt.outcomeservice.enums.EventType.NON_RESIDENTIAL;
import static uk.gov.ons.census.fwmt.outcomeservice.enums.SurveyType.ccs;
import static uk.gov.ons.census.fwmt.outcomeservice.util.CcsUtilityMethods.getAddressLevel;
import static uk.gov.ons.census.fwmt.outcomeservice.util.CcsUtilityMethods.getAddressType;
import static uk.gov.ons.census.fwmt.outcomeservice.util.CcsUtilityMethods.getOrganisationName;

@Component
public class CssNonResidential implements CcsOutcomeServiceProcessor {

  @Autowired
  private GatewayOutcomeProducer gatewayOutcomeProducer;

  @Autowired
  private GatewayEventManager gatewayEventManager;

  @Override
  public boolean isValid(CCSPropertyListingOutcome ccsPLOutcome) {
    List<String> validSecondaryOutcomes = Collections.singletonList("Non residential / business");
    return validSecondaryOutcomes.contains(ccsPLOutcome.getSecondaryOutcome());
  }

  @Override
  public void processMessage(CCSPropertyListingOutcome ccsPLOutcome) {
    Map<String, Object> root = new HashMap<>();
    root.put("ccsPropertyListingOutcome", ccsPLOutcome);
    root.put("addressType", getAddressType(ccsPLOutcome));
    root.put("addressLevel", getAddressLevel(ccsPLOutcome));
    root.put("organisationName", getOrganisationName(ccsPLOutcome));

    String outcomeEvent = TemplateCreator.createOutcomeMessage(NON_RESIDENTIAL, root, ccs);

    gatewayOutcomeProducer.sendPropertyListing(outcomeEvent, String.valueOf(ccsPLOutcome.getTransactionId()));
    gatewayEventManager.triggerEvent(String.valueOf(ccsPLOutcome.getPropertyListingCaseId()), CCSPL_NON_RESIDENTIAL_OUTCOME_SENT);
  }
}
