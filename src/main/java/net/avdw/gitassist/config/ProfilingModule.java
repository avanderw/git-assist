package net.avdw.gitassist.config;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.AbstractMatcher;
import net.avdw.gitassist.Ansi;
import org.pmw.tinylog.Logger;

import java.lang.reflect.Method;

import static com.google.inject.matcher.Matchers.inSubpackage;
import static com.google.inject.matcher.Matchers.not;

public final class ProfilingModule extends AbstractModule {
    @Override
    protected void configure() {
        bindInterceptor(inSubpackage("net.avdw.todo")
                        .and(not(inSubpackage("net.avdw.todo.model"))),
                new AbstractMatcher<Method>() {
                    @Override
                    public boolean matches(final Method method) {
                        return !method.isSynthetic();
                    }
                }, (methodInvocation) -> {
                    long start = System.currentTimeMillis();
                    try {
                        Logger.trace(String.format("%s-> %s.%s%s",
                                Ansi.MAGENTA,
                                methodInvocation.getMethod().getDeclaringClass().getSimpleName(),
                                methodInvocation.getMethod().getName(),
                                Ansi.RESET
                        ));
                        return methodInvocation.proceed();
                    } finally {
                        Logger.trace(String.format("%s<- %s.%s : %,d ms%s",
                                Ansi.MAGENTA,
                                methodInvocation.getMethod().getDeclaringClass().getSimpleName(),
                                methodInvocation.getMethod().getName(),
                                System.currentTimeMillis() - start,
                                Ansi.RESET
                        ));
                    }
                });
    }
}
