package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Optional body of the convert route: fills the fields RN01 requires before the idea enters the board. */
@Data
public class ConvertCardDTO {
    @Size(max = 255, message = "O título deve ter no máximo 255 caracteres.")
    private String title;
    @Size(max = 255, message = "O público-alvo deve ter no máximo 255 caracteres.")
    private String targetAudience;
    @Size(max = 10000, message = "O objetivo pedagógico deve ter no máximo 10000 caracteres.")
    private String pedagogicalObjective;
}
