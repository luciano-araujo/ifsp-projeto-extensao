package br.edu.ifsp.tcc.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BoardResponse {
    private Long teamId;
    private String teamName;
    private List<BoardColumnResponse> columns;
}
