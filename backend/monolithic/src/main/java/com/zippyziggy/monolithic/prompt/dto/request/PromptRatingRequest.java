package com.zippyziggy.monolithic.prompt.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromptRatingRequest {
    private Integer score;
}
