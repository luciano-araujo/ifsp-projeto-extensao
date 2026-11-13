package br.edu.ifsp.tcc.application.dto;

/** Review state of a card as shown on the board, derived from its latest review. Response only. */
public enum ReviewStatus {
    NONE,
    APPROVED,
    CHANGES_REQUESTED
}
