package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Used to create (name required) and to rename or reorder (all fields optional) a column. */
@Data
public class BoardColumnDTO {
    @Size(max = 80, message = "O nome da coluna deve ter no maximo 80 caracteres.")
    private String name;

    private Integer sortOrder;
}
