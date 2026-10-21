package br.edu.ifsp.tcc.application.dto;

import lombok.Data;

/** Optional body of the convert route: fills the fields RN01 requires before the idea enters the board. */
@Data
public class ConvertCardDTO {
    private String title;
    private String targetAudience;
    private String pedagogicalObjective;
}
