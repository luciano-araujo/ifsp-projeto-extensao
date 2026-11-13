package br.edu.ifsp.tcc.application.dto;

import br.edu.ifsp.tcc.application.entity.PublicationProblemType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PublicationProblemResponse {
    private PublicationProblemType type;
    /** Modules or lessons that have the problem (empty for NO_MODULES). */
    private List<Long> ids;
    private List<String> titles;
    private String message;
}
