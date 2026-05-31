package br.edu.ifsp.tcc.application.dto;

import lombok.Data;

@Data
public class UpdateUserDTO {
    private String name;
    private String email;
    private String password;
}
