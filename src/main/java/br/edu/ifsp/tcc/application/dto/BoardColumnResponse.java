package br.edu.ifsp.tcc.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class BoardColumnResponse {
    private Long id;
    private String name;
    private Integer sortOrder;
    private List<CardResponse> cards;
}
