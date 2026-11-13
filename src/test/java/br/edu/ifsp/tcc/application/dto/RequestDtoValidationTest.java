package br.edu.ifsp.tcc.application.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Over-long text and negative orders must be rejected by validation (400), not reach the database (409). */
class RequestDtoValidationTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final String LONG = "t".repeat(256);

    @Test
    void linkLessonMaterial_shouldRequireTheMaterialId() {
        LinkLessonMaterialDTO dto = new LinkLessonMaterialDTO();

        var violations = VALIDATOR.validate(dto);

        assertEquals(1, violations.size());
        assertEquals("Informe o material.", violations.iterator().next().getMessage());
        dto.setMaterialId(7L);
        assertTrue(VALIDATOR.validate(dto).isEmpty());
    }

    @Test
    void createCard_shouldRejectLongTitleAudienceAndTag() {
        CreateCardDTO dto = new CreateCardDTO();
        dto.setTitle(LONG);
        dto.setTargetAudience(LONG);
        dto.setPedagogicalObjective("o".repeat(10001));
        dto.setTag("x".repeat(41));

        assertEquals(4, VALIDATOR.validate(dto).size());
    }

    @Test
    void createCard_shouldAcceptLimitValues() {
        CreateCardDTO dto = new CreateCardDTO();
        dto.setTitle("t".repeat(255));
        dto.setTargetAudience("a".repeat(255));
        dto.setTag("x".repeat(40));

        assertTrue(VALIDATOR.validate(dto).isEmpty());
    }

    @Test
    void createTask_shouldRejectLongTitle() {
        CreateTaskDTO dto = new CreateTaskDTO();
        dto.setTitle(LONG);

        assertEquals(1, VALIDATOR.validate(dto).size());
    }

    @Test
    void updateKanbanItem_shouldRejectLongTitleAndAudience() {
        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setTitle(LONG);
        dto.setTargetAudience(LONG);

        assertEquals(2, VALIDATOR.validate(dto).size());
    }

    @Test
    void convertCard_shouldRejectLongTitle() {
        ConvertCardDTO dto = new ConvertCardDTO();
        dto.setTitle(LONG);

        assertEquals(1, VALIDATOR.validate(dto).size());
    }

    @Test
    void courseDtos_shouldRejectLongTitles() {
        CreateCourseDTO course = new CreateCourseDTO();
        course.setTitle(LONG);
        CreateModuleDTO module = new CreateModuleDTO();
        module.setTitle(LONG);
        CreateCourseLessonDTO lesson = new CreateCourseLessonDTO();
        lesson.setTitle(LONG);
        UpdateCourseLessonDTO update = new UpdateCourseLessonDTO();
        update.setTitle(LONG);

        assertEquals(1, VALIDATOR.validate(course).size());
        assertEquals(1, VALIDATOR.validate(module).size());
        assertEquals(1, VALIDATOR.validate(lesson).size());
        assertEquals(1, VALIDATOR.validate(update).size());
    }

    @Test
    void userDtos_shouldRejectLongNameAndEmail() {
        RegisterTokenRequest register = new RegisterTokenRequest();
        register.setName(LONG);
        register.setEmail("a@b.com");
        UpdateUserDTO update = new UpdateUserDTO();
        update.setName(LONG);
        update.setEmail(LONG);
        AddTeamMemberDTO member = new AddTeamMemberDTO();
        member.setEmail("a".repeat(250) + "@b.com");

        assertEquals(1, VALIDATOR.validate(register).size());
        assertEquals(2, VALIDATOR.validate(update).size());
        assertFalse(VALIDATOR.validate(member).isEmpty());
    }

    @Test
    void sortOrder_shouldNotBeNegative() {
        BoardColumnDTO column = new BoardColumnDTO();
        column.setSortOrder(-1);
        MoveCardDTO move = new MoveCardDTO();
        move.setColumnId(1L);
        move.setSortOrder(-5);

        assertEquals(1, VALIDATOR.validate(column).size());
        assertEquals(1, VALIDATOR.validate(move).size());

        column.setSortOrder(0);
        move.setSortOrder(null);
        assertTrue(VALIDATOR.validate(column).isEmpty());
        assertTrue(VALIDATOR.validate(move).isEmpty());
    }

    @Test
    void passwordFields_shouldHaveLengthLimitsWithPortugueseMessages() {
        ConfirmRegistrationRequest confirm = new ConfirmRegistrationRequest();
        confirm.setEmail("a@b.com");
        confirm.setToken("123456");
        confirm.setPassword("a".repeat(73));
        ResetPasswordRequest reset = new ResetPasswordRequest();
        reset.setEmail("a@b.com");
        reset.setToken("123456");
        reset.setNewPassword("a".repeat(73));
        UpdateUserDTO update = new UpdateUserDTO();
        update.setPassword("a".repeat(73));

        assertEquals("A senha deve ter no máximo 72 caracteres.", VALIDATOR.validate(confirm).iterator().next().getMessage());
        assertEquals("A senha deve ter no máximo 72 caracteres.", VALIDATOR.validate(reset).iterator().next().getMessage());
        assertEquals("A senha deve ter no máximo 72 caracteres.", VALIDATOR.validate(update).iterator().next().getMessage());
    }

    @Test
    void requiredFields_shouldNeverFallBackToTheDefaultEnglishMessage() {
        java.util.List<Object> empties = java.util.List.of(new LoginRequest(), new ForgotPasswordRequest(),
                new RegisterTokenRequest(), new ConfirmRegistrationRequest(), new ResetPasswordRequest(),
                new CreateModuleDTO(), new CreateCardDTO(), new CreateTaskDTO(), new CreateTeamDTO(),
                new CreateCourseDTO(), new CreateCourseLessonDTO(), new AddTeamMemberDTO(), new UpdateTeamMemberDTO(),
                new MoveCardDTO());
        for (Object dto : empties) {
            VALIDATOR.validate(dto).forEach(v -> {
                assertFalse(v.getMessage().contains("must ") || v.getMessage().contains("size must"),
                        dto.getClass().getSimpleName() + "." + v.getPropertyPath() + ": " + v.getMessage());
                assertTrue(v.getMessage().matches(".*[áéíóúãõçÁÉÍÓÚ].*") || v.getMessage().contains("e-mail"),
                        "message without accents: " + v.getMessage());
            });
        }
    }
}
