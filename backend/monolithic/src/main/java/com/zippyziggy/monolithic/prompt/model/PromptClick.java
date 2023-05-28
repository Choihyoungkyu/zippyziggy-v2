package com.zippyziggy.monolithic.prompt.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
@Getter
public class PromptClick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "BINARY(16)")
    private UUID memberUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_uuid", nullable = false)
    private Prompt prompt;

    private LocalDateTime regDt;

    public static PromptClick from(Prompt prompt, UUID memberUuid) {
        return PromptClick.builder()
                .memberUuid(memberUuid)
                .prompt(prompt)
                .regDt(LocalDateTime.now()).build();
    }
}
