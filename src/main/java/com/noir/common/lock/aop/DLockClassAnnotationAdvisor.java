package com.noir.common.lock.aop;

import com.noir.common.lock.annotation.DLock;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 类注解advisor
 *
 * 用于标定类上有 {@link com.noir.common.lock.annotation.DLock}
 * 的切点与对应AnnotationAdvice
 *
 * @see com.noir.common.lock.aop.DLockAnnotationAdvice
 */
@Component
public class DLockClassAnnotationAdvisor extends AbstractPointcutAdvisor {

    @Autowired
    private DLockAnnotationAdvice dLockAnnotationAdvice;

    @Override
    public Pointcut getPointcut() {
        return new AnnotationMatchingPointcut(DLock.class, null, true);
    }

    @Override
    public Advice getAdvice() {
        return dLockAnnotationAdvice;
    }
}
