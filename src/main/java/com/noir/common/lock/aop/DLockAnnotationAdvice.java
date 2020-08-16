package com.noir.common.lock.aop;

import com.noir.common.lock.DLockFactory;
import com.noir.common.lock.annotation.DLock;
import com.noir.common.lock.excptions.TryLockFailException;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.TemplateParserContext;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

/**
 * 具体的注解解析advice
 *
 * 实现拓展了 {@link MethodInterceptor} 以支持
 * 对于方法的上锁增强，通过查找方法及其类上的注解对
 * 相应资源进行上锁或等待。
 *
 * 拓展提供了对方法参数的SpEL解析，可以通过如
 * {@code @DLock("#{#xxx}")} 来获取传入参
 * 数的解析与资源的上锁
 *
 * 资源锁依赖 {@link DLockFactory}来获取实现了
 * {@link Lock} 的实例并对资源进行后续操作，如有
 * 疑惑可看工厂类的接口及其下实现
 * @see com.noir.common.lock.DLockFactory
 */
@Component
public class DLockAnnotationAdvice implements MethodInterceptor {
    @Autowired
    private DLockFactory lockFactory;

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        return doLock(invocation);
    }

    private Object doLock(MethodInvocation invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        Method method = invocation.getMethod();
        Class<?> clz = method.getDeclaringClass();

        DLock dLock = AnnotatedElementUtils.findMergedAnnotation(clz, DLock.class);
        DLock methodDLock = AnnotatedElementUtils.findMergedAnnotation(method, DLock.class);
        if (Objects.nonNull(methodDLock)) {
            dLock = methodDLock;
        }
        assert dLock != null;

        // 创建解析器
        ExpressionParser parser = new SpelExpressionParser();
        // 创建上下文
        StandardEvaluationContext ctx = creteCtx(method, args);

        List<Lock> locks = Arrays.stream(dLock.value())
                .map(origin -> parseKey(parser, ctx, origin))
                .map(lockFactory::getLock)
                .collect(Collectors.toList());
        try {
            for (Lock lock:locks) {
                if (!lock.tryLock(dLock.timeOutSecond(), TimeUnit.SECONDS)) {
                    throw new TryLockFailException();
                }
            }
            return invocation.proceed();
        } finally {
            for (Lock lock:locks) {
                if (lock != null) {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * SpEL解析
     */
    private String parseKey(ExpressionParser parser, StandardEvaluationContext ctx, String key) {
        if (StringUtils.isEmpty(key)) return "";
        return parser.parseExpression(key, new TemplateParserContext()).getValue(ctx, String.class);
    }

    /**
     * 初始化解析上下文
     */
    private StandardEvaluationContext creteCtx(Method method, Object[] args) {
        LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
        String[] paraNameArr = u.getParameterNames(method);

        //SpEL上下文
        StandardEvaluationContext context = new StandardEvaluationContext();
        //把方法参数放入SpEL上下文中
        if (Objects.nonNull(paraNameArr)) {
            for (int i = 0; i < paraNameArr.length; i++) {
                context.setVariable(paraNameArr[i], args[i]);
            }
        }
        return context;
    }
}
