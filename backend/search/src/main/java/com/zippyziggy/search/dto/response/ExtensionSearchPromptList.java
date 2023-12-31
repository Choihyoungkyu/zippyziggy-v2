package com.zippyziggy.search.dto.response;

import com.zippyziggy.search.dto.response.server.ExtensionSearchPrompt;
import java.util.List;
import lombok.Data;

@Data
public class ExtensionSearchPromptList {

    public static ExtensionSearchPromptList of(
        Long totalPromptsCnt,
        Integer totalPageCnt,
        List<ExtensionSearchPrompt> searchPrompts
    ) {
        return new ExtensionSearchPromptList(totalPromptsCnt, totalPageCnt, searchPrompts);
    }

    private final Long totalPromptsCnt;
    private final Integer totalPageCnt;
    private final List<ExtensionSearchPrompt> extensionSearchPromptList;

}
