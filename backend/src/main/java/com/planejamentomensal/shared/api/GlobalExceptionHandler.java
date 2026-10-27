package com.planejamentomensal.shared.api;

import com.planejamentomensal.shared.domain.BusinessRuleException;
import com.planejamentomensal.shared.domain.DomainException;
import com.planejamentomensal.shared.domain.InvalidInputException;
import com.planejamentomensal.shared.domain.NotFoundException;
import com.planejamentomensal.shared.domain.StaleVersionException;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Single place that turns exceptions into RFC 9457 {@link ProblemDetail} responses.
 *
 * <p>Implements API-03 and I18N-06: Spring MVC errors (validation, unreadable body, missing resource) are
 * handled by the parent class with {@code problemDetail.*} message keys; business errors use
 * {@code error.<code>.title} and {@code error.<code>.detail}. All texts follow {@code Accept-Language}.</p>
 *
 * @since 1.0
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Base of the {@code type} URI of business problems. */
    public static final String PROBLEM_TYPE_BASE = "/api/profile/errors/";

    private final MessageSource messages;

    /**
     * Creates the handler.
     *
     * @param messages localized messages
     */
    public GlobalExceptionHandler(MessageSource messages) {
        this.messages = messages;
        setMessageSource(messages);
    }

    /**
     * Maps a missing resource to 404.
     *
     * @param e      exception
     * @param locale locale resolved from {@code Accept-Language}
     * @return problem with status 404
     */
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException e, Locale locale) {
        return problem(HttpStatus.NOT_FOUND, e, locale);
    }

    /**
     * Maps a business rule conflict to 409.
     *
     * @param e      exception
     * @param locale locale resolved from {@code Accept-Language}
     * @return problem with status 409
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException e, Locale locale) {
        return problem(HttpStatus.CONFLICT, e, locale);
    }

    /**
     * Adds the localized message of every invalid field to the 400 problem, as {@code errors}.
     *
     * @param e       validation failure
     * @param headers response headers
     * @param status  status chosen by Spring (400)
     * @param request current request
     * @return problem with {@code errors: [{field, message}]}
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException e,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Locale locale = LocaleContextHolder.getLocale();
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", messages.getMessage(error, locale)))
                .toList();
        ProblemDetail problem = e.updateAndGetBody(messages, locale);
        problem.setProperty("errors", errors);
        return handleExceptionInternal(e, problem, headers, status, request);
    }

    /**
     * Maps input that cannot be processed to 400.
     *
     * @param e      exception
     * @param locale locale resolved from {@code Accept-Language}
     * @return problem with status 400
     */
    @ExceptionHandler(InvalidInputException.class)
    public ProblemDetail handleInvalidInput(InvalidInputException e, Locale locale) {
        return problem(HttpStatus.BAD_REQUEST, e, locale);
    }

    /**
     * Maps an edit of an outdated version to 412 (API-04).
     *
     * @param e      exception
     * @param locale locale resolved from {@code Accept-Language}
     * @return problem with status 412
     */
    @ExceptionHandler(StaleVersionException.class)
    public ProblemDetail handleStaleVersion(StaleVersionException e, Locale locale) {
        return problem(HttpStatus.PRECONDITION_FAILED, e, locale);
    }

    /**
     * Maps a concurrent write detected by JPA to 412 (API-04).
     *
     * @param e      exception
     * @param locale locale resolved from {@code Accept-Language}
     * @return problem with status 412
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException e, Locale locale) {
        return problem(HttpStatus.PRECONDITION_FAILED, new StaleVersionException(-1, -1), locale);
    }

    /**
     * Maps an invalid argument (such as a malformed billing period) to 400.
     *
     * @param e      exception
     * @param locale locale resolved from {@code Accept-Language}
     * @return problem with status 400
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException e, Locale locale) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create(PROBLEM_TYPE_BASE + "invalid-argument"));
        problem.setTitle(messages.getMessage("error.invalid-argument.title", null, "Invalid argument", locale));
        problem.setDetail(e.getMessage());
        problem.setProperty("code", "invalid-argument");
        return problem;
    }

    private ProblemDetail problem(HttpStatus status, DomainException e, Locale locale) {
        Object[] args = e.arguments().toArray();
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(PROBLEM_TYPE_BASE + e.code()));
        problem.setTitle(messages.getMessage("error." + e.code() + ".title", args, e.code(), locale));
        problem.setDetail(messages.getMessage("error." + e.code() + ".detail", args, null, locale));
        problem.setProperty("code", e.code());
        return problem;
    }
}
