package br.edu.ifsp.tcc.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaskAssigneeResponse {
    private Long memberId;
    private Long userId;
    private String name;
}
