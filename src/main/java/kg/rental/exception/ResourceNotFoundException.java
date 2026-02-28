package kg.rental.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
    public ResourceNotFoundException(String entity, Long id) {
        super(entity + " с ID " + id + " не найден");
    }
}
