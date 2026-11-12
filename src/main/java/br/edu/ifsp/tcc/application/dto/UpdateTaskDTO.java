package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** All fields are optional; a field that is not sent stays as it is. */
@Data
public class UpdateTaskDTO {
    @Size(max = 255, message = "O título da tarefa deve ter no máximo 255 caracteres.")
    private String title;

    /** Id of the team member responsible for the task; 0 removes the assignee. */
    private Long assigneeMemberId;

    /** Due date as yyyy-MM-dd; an empty string removes it. */
    @Pattern(regexp = "^$|^\\d{4}-\\d{2}-\\d{2}$", message = "Prazo inválido. Use o formato AAAA-MM-DD.")
    private String dueDate;
}
