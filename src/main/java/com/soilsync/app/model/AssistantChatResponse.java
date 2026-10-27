package com.soilsync.app.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

public record AssistantChatResponse(
    String answer,
    String language,
    String units,
    String location,
    List<String> sources,
    boolean vettedRegionalSourceUsed,
    boolean machineTranslated,
    @JsonInclude(JsonInclude.Include.NON_NULL) Long chatLogId
) {
    public AssistantChatResponse withChatLogId(Long logId) {
        return new AssistantChatResponse(
            answer,
            language,
            units,
            location,
            sources,
            vettedRegionalSourceUsed,
            machineTranslated,
            logId
        );
    }
}
