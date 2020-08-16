package com.noir.common.lock.annotation;

import java.lang.annotation.*;

/**
 * 注解式分布式锁支持
 *
 * 在标定的方法执行结束后将自动释放锁，同样的，
 * 支持可重入意味着可以嵌套使用而不发生死锁。
 *
 * {@code ElementType.ANNOTATION_TYPE}，
 * {@code @Inherited}
 * 这意味着这同样是一个元注解，可以像普通使用
 * Spring annotation一样将多个注解标定到一
 * 个通用的注解上并使用
 * {@link org.springframework.core.annotation.AliasFor}
 * 来指定annotation并将value标定merge进来。
 *
 * {@code ElementType.TYPE}
 * 而对于标定在class上的注解，都将会尝试解析
 * 锁并在其下的方法执行过程中上锁，我认为这不
 * 是一个好方法，对于整体的逻辑和类内部的接口
 * 设计都是一个限制，但我还是提供了，请注意。
 *
 * 同所有Spring AOP标定的注解一样，他可能存
 * 在失效的场景，即内部互相调用时会失去作用，
 * 这会带来额外的心智负担，如果你的团队对于这
 * 些方面并不熟悉，请更多使用
 * {@link com.noir.common.lock.LockableService}
 * or
 * {@link com.noir.common.lock.DLockFactory}
 *
 * 如果还有疑问及迷惑，可以查看具体的aop实现
 * @see com.noir.common.lock.aop.DLockAnnotationAdvice
 * @see com.noir.common.lock.aop.DLockClassAnnotationAdvisor
 * @see com.noir.common.lock.aop.DLockMethodAnnotationAdvisor
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface DLock {
    String[] value();

    int timeOutSecond() default 30;
}
