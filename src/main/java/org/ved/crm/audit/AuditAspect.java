package org.ved.crm.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.ved.crm.common.audit.BaseAuditEntity;
import org.ved.crm.user.UserRepository;

import java.lang.reflect.Method;
import java.util.UUID;

// @Aspect tells Spring this class contains AOP advice
// @Component makes it a Spring bean so it gets picked up automatically
// @Slf4j gives us a logger for debugging
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {
    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    // @Around — run this method both before AND after the target method
    // "@annotation(audited)" — intercept any method annotated with @Audited
    // The parameter "audited" is automatically bound to the @Audited instance
    // so we can read audited.action() and audited.entityType()
    @Around("@annotation(audited)")
    public Object auditMethod(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable{

        // Step 1 — Get current authenticated user from JWT SecurityContext
        // This is the email stored in the JWT subject field
        String userEmail = getCurrentUserEmail();
        UUID userId = getCurrentUserId(userEmail);

        // Step 2 — Call the real service method
        // joinPoint.proceed() executes the actual method being intercepted
        // If the method throws an exception it propagates from here
        Object result = null;
        try{

            result = joinPoint.proceed();

            // Step 3 — Method succeeded — extract entityId from result
            // Most methods return a DTO with an id field
            // We use reflection to read it without coupling to specific DTO types
            UUID entityId = extractEntityId(result,joinPoint.getArgs());

            // Step 4 — Save SUCCESS audit log
            AuditLog auditLog  =AuditLog.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .action(audited.action())
                    .entityId(entityId)
                    .entityType(audited.entityType().isEmpty() ? null : audited.entityType())
                    .result(AuditResult.SUCCESS)
                    .build();
            auditLogRepository.save(auditLog);

            // Step 5 — Return the original result to the caller
            // The controller must receive exactly what the service returned
            return result;

        }catch (Throwable ex){

            // Step 6 — Method failed — save FAILURE audit log
            // We still log who tried to do what and why it failed
            UUID entityId = extractEntityId(null, joinPoint.getArgs());

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .userEmail(userEmail)
                    .action(audited.action())
                    .entityId(entityId)
                    .entityType(audited.entityType().isEmpty()
                            ? null : audited.entityType())
                    .result(AuditResult.FAILURE)
                    .errorMessage(ex.getMessage())
                    .build();

            auditLogRepository.save(auditLog);

            // Step 7 — Re-throw the exception
            // The aspect must never swallow exceptions
            // Normal error handling in GlobalExceptionHandler must still work
            throw ex;

        }

    }

    // Extract the current user's email from the JWT SecurityContext
    // Returns "anonymous" if no user is authenticated
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication == null || !authentication.isAuthenticated()){
            return "anonymous";
        }
        return authentication.getName();
    }

    // Look up the user's UUID from their email
    // Returns null if user not found — audit log still saves with null userId

    private UUID getCurrentUserId(String email){
        if ("anonymous".equals(email)) return null;
        return userRepository.findByEmail(email)
                .map(BaseAuditEntity::getId)
                .orElse(null);
    }

    // Extract entityId from the method result or arguments.
    // Strategy:
    //   1. If result is not null — try to read getId() via reflection
    //   2. If result is null (void methods) — look for first UUID argument
    // This works for all our service methods without any coupling.
    private UUID extractEntityId(Object result, Object[] args){
        // Strategy 1 — try to get id from return value
        if(result != null){
            try {
                Method getId = result.getClass().getMethod("id");
                Object id = getId.invoke(result);
                if (id instanceof UUID){
                    return (UUID) id;
                }
            }catch (Exception e){
                // Result doesn't have an id() method — try args
            }
        }

        // Strategy 2 — look for first UUID in method arguments
        // Used for void methods like deactivateUser(UUID id)
        if(args != null){
            for(Object arg : args){
                if (arg instanceof UUID){
                    return (UUID) arg;
                }
            }
        }

        return null;


    }

}
