package br.edu.ifsp.tcc.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PublicationCheckResponse {
    private boolean publishable;
    private List<PublicationProblemResponse> problems;
}
