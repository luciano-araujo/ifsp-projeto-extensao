package br.edu.ifsp.tcc.application.entity;

/** What stops a course from being published (RN03 and RN06). */
public enum PublicationProblemType {
    NO_MODULES,
    EMPTY_MODULE,
    LESSON_WITHOUT_MATERIAL,
    INVALID_LINK
}
