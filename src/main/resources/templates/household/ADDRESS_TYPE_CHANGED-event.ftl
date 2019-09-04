"addressTypeChange" : {
"collectionCase" : {
"id":"${householdOutcome.caseId}",
<#if usualResidents gt 0>
    "numberOfResidents”:"${usualResidents}",
</#if>
"address" : {
"addressType":"${estabType}",
"estabType":"${householdOutcome.ceDetails.establishmentType}",
"orgName":"${householdOutcome.ceDetails.establishmentName}"
}
<#if secondaryOutcome == "CE - Contact made">
    ,
    "contact" : {
    <#if householdOutcome.ceDetails.managerTitle??>
        "title":"${householdOutcome.ceDetails.managerTitle}",
    <#else>
        "title":null,
    </#if>
    <#if householdOutcome.ceDetails.managerForename??>
        "forename":"${householdOutcome.ceDetails.managerForename}",
    <#else>
        "forename":null,
    </#if>
    <#if householdOutcome.ceDetails.managerSurname??>
        "surname":"${householdOutcome.ceDetails.managerSurname}",
    <#else>
        "surname":null,
    </#if>
    <#if householdOutcome.ceDetails.contactPhone>
        "telNo":"${householdOutcome.ceDetails.contactPhone}"
        <#else>
            "telNo":null
    </#if>
    }
</#if>
}
}