package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.ReviewDecision;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class CardReviewResponse {
    private Long id;
    private Long cardId;
    private ReviewDecision decision;
    private String comment;

    /** Null after the reviewer deletes the account. */
    private MovedByResponse reviewer;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;
}
