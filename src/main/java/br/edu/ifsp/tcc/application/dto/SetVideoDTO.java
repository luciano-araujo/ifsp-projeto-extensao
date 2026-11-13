package br.edu.ifsp.tcc.application.dto;

import lombok.Data;

@Data
public class SetVideoDTO {
    /** YouTube link of the published video; null or blank clears the link. */
    private String url;
}
