package org.ved.crm.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// @Target(METHOD) — this annotation can only be placed on methods
// @Retention(RUNTIME) — annotation is available at runtime for AOP to read
// Without RUNTIME retention, the AOP aspect cannot see the annotation
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    // The action name that will be stored in audit_log.action
    // e.g. "INVOICE_GENERATED", "PAYMENT_RECORDED"
    String action();

    // The type of entity being affected
    // e.g. "Invoice", "Payment", "Return"
    // Defaults to empty string when no specific entity type applies
    String entityType() default "";

}
