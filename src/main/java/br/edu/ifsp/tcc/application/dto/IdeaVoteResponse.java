package br.edu.ifsp.tcc.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class IdeaVoteResponse {
    private Long cardId;
    private Integer votes;
    private Boolean votedByMe;
}
