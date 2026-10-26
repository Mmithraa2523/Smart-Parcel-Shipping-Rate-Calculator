package com.swiftship.shipping.api;

import com.swiftship.shipping.api.ApiModels.ApiError;
import com.swiftship.shipping.api.ApiModels.ValidationIssue;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Provider
@Component
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<ValidationIssue> details = exception.getConstraintViolations().stream()
                .map(violation -> new ValidationIssue(
                        violation.getPropertyPath().toString().replaceAll("^.*\\.", ""),
                        violation.getMessage()))
                .sorted(Comparator.comparing(ValidationIssue::field))
                .toList();
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ApiError("VALIDATION_ERROR", "Please correct the highlighted shipping details.", details))
                .build();
    }
}
