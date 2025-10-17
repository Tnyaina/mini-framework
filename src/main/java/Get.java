import java.lang.annotation.*;

// Annotation pour les routes GET
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Get {
    String value();
}
