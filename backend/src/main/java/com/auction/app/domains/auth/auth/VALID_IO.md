What is Valid IO?

- The @Valid annotation in Spring Boot is used to trigger validation on objects, parameters, or fields
⇒ Ensuring data integrity
- Basic Usage: Just put it before the incoming DTOs (check auth controller for that)

What is happened if user type wrong data?
- By default, a validation failure triggers a MethodArgumentNotValidException
- Standard HTTP status code should be returned to the user is 400 Bad Request

To catch the exception, I should recommend to use global exception handler, which I'll note in other files