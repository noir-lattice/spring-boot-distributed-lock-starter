package com.noir.common.lock.aop;

import com.noir.common.lock.DLockFactory;
import com.noir.common.lock.annotation.DLock;

import com.noir.common.lock.excptions.TryLockFailException;
import lombok.extern.slf4j.Slf4j;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
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

@Slf4j
@Aspect
@Order(1)
@Component
public class DLockAspect {
    @Autowired
    private DLockFactory lockFactory;

    @Pointcut(value = "@annotation(dLock)")
    public void pointCut(DLock dLock){}

    @Around(value = "pointCut(dLock)", argNames = "proceedingJoinPoint,dLock")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, DLock dLock){
        return doCheck(proceedingJoinPoint, dLock);
    }

    private Object doCheck(ProceedingJoinPoint proceedingJoinPoint, DLock dLock){
        //得到被切面修饰的方法的参数列表
        Object[] args = proceedingJoinPoint.getArgs();
        // 得到被代理的方法
        Method method = ((MethodSignature) proceedingJoinPoint.getSignature()).getMethod();

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
            return proceedingJoinPoint.proceed();
        } catch (TryLockFailException e) {
            throw e;
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
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
