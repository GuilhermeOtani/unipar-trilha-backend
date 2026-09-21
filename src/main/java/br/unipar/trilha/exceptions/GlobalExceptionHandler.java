package br.unipar.trilha.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(error -> error.getField(),
                        error -> error.getDefaultMessage() == null ? "inválido" : error.getDefaultMessage(),
                        (first, ignored) -> first, LinkedHashMap::new));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Erro de validação",
                "Um ou mais campos são inválidos.", request);
        problem.setProperty("errors", errors);
        return response(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> unreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String detail = "O corpo da requisição contém JSON inválido.";
        InvalidFormatException invalidFormat = findCause(ex, InvalidFormatException.class);
        if (invalidFormat != null) {
            String campo = invalidFormat.getPath().isEmpty()
                    ? "informado"
                    : invalidFormat.getPath().getLast().getFieldName();
            detail = "O valor do campo '%s' possui formato inválido.".formatted(campo);
        }
        return response(HttpStatus.BAD_REQUEST,
                problem(HttpStatus.BAD_REQUEST, "Requisição inválida", detail, request));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> typeMismatch(MethodArgumentTypeMismatchException ex,
                                               HttpServletRequest request) {
        String detail = "O parâmetro '%s' possui valor inválido.".formatted(ex.getName());
        return response(HttpStatus.BAD_REQUEST,
                problem(HttpStatus.BAD_REQUEST, "Parâmetro inválido", detail, request));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ProblemDetail> missingParameter(MissingServletRequestParameterException ex,
                                                   HttpServletRequest request) {
        String detail = "O parâmetro obrigatório '%s' não foi informado.".formatted(ex.getParameterName());
        return response(HttpStatus.BAD_REQUEST,
                problem(HttpStatus.BAD_REQUEST, "Parâmetro obrigatório ausente", detail, request));
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    ResponseEntity<ProblemDetail> binding(ServletRequestBindingException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST,
                problem(HttpStatus.BAD_REQUEST, "Requisição inválida",
                        "A requisição não contém todos os dados obrigatórios.", request));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> constraintViolation(ConstraintViolationException ex,
                                                      HttpServletRequest request) {
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage(),
                        (first, ignored) -> first,
                        LinkedHashMap::new));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Erro de validação",
                "Um ou mais parâmetros são inválidos.", request);
        problem.setProperty("errors", errors);
        return response(HttpStatus.BAD_REQUEST, problem);
    }

    @ExceptionHandler(RegraNegocioException.class)
    ResponseEntity<ProblemDetail> business(RegraNegocioException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT,
                problem(HttpStatus.CONFLICT, "Regra de negócio violada", ex.getMessage(), request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> integrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT,
                problem(HttpStatus.CONFLICT, "Conflito de integridade",
                        "A operação viola uma restrição de integridade ou duplicidade.", request));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    ResponseEntity<ProblemDetail> notFound(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND,
                problem(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage(), request));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    ResponseEntity<ProblemDetail> routeNotFound(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.NOT_FOUND,
                problem(HttpStatus.NOT_FOUND, "Rota não encontrada",
                        "A rota solicitada não existe.", request));
    }

    @ExceptionHandler(BadCredentialsException.class)
    ResponseEntity<ProblemDetail> credentials(BadCredentialsException ex, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED,
                problem(HttpStatus.UNAUTHORIZED, "Não autenticado", "Credenciais inválidas.", request));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> denied(AccessDeniedException ex, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN,
                problem(HttpStatus.FORBIDDEN, "Acesso negado",
                        "Você não tem permissão para este recurso.", request));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> methodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                     HttpServletRequest request) {
        return response(HttpStatus.METHOD_NOT_ALLOWED,
                problem(HttpStatus.METHOD_NOT_ALLOWED, "Método não permitido",
                        "O método HTTP não é aceito nesta rota.", request));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> mediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                        HttpServletRequest request) {
        return response(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de conteúdo não suportado",
                        "Envie a requisição usando um tipo de conteúdo aceito pela rota.", request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> generic(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.INTERNAL_SERVER_ERROR,
                problem(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", "Erro inesperado.", request));
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create("about:blank"));
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }

    private ResponseEntity<ProblemDetail> response(HttpStatus status, ProblemDetail problem) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
