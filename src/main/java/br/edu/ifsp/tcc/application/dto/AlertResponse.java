package br.edu.ifsp.tcc.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class AlertResponse {
    private AlertType type;
    private Long cardId;
    private String cardTitle;
    private Long taskId;
    private String taskTitle;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private Integer daysLeft;
}
