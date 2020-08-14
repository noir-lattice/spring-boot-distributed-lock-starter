package com.noir.common.lock.aop;

import com.noir.common.lock.annotation.DLock;
import org.aopalliance.aop.Advice;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DLockMethodAnnotationAdvisor extends AbstractPointcutAdvisor {

    @Autowired
    private DLockAnnotationAdvice dLockAnnotationAdvice;

    @Override
    public Pointcut getPointcut() {
        return new AnnotationMatchingPointcut(null, DLock.class, true);
    }

    @Override
    public Advice getAdvice() {
        return dLockAnnotationAdvice;
    }
}
