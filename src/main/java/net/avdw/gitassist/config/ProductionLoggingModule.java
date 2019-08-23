package net.avdw.gitassist.config;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.AbstractMatcher;
import net.avdw.gitassist.Ansi;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import static com.google.inject.matcher.Matchers.inSubpackage;
import static com.google.inject.matcher.Matchers.not;

public final class ProductionLoggingModule extends AbstractModule {
    @Override
    protected void configure() {
        String level = String.format("%s{level}%s", Ansi.BLUE, Ansi.RESET);
        Logger.getConfiguration()
                .formatPattern(String.format("[%s] {message}", level))
                .level(Level.INFO).activate();

        bindInterceptor(inSubpackage("net.avdw.gitassist")
                        .and(not(inSubpackage("net.avdw.gitassist.model"))),
                new AbstractMatcher<Method>() {
                    @Override
                    public boolean matches(final Method method) {
                        return !method.isSynthetic();
                    }
                },
                (methodInvocation) -> {
                    Parameter[] parameters = methodInvocation.getMethod().getParameters();
                    StringBuilder stringBuilder = new StringBuilder();
                    for (int i = 0; i < parameters.length; i++) {
                        stringBuilder.append(parameters[i].getName());
                        stringBuilder.append("=");
                        stringBuilder.append(methodInvocation.getArguments()[i]);
                        if (i != parameters.length - 1) {
                            stringBuilder.append(", ");
                        }
                    }
                    Logger.trace(String.format("%s.%s(%s)",
                            methodInvocation.getMethod().getDeclaringClass().getSimpleName(),
                            methodInvocation.getMethod().getName(),
                            stringBuilder.toString()
                    ));
                    return methodInvocation.proceed();
                });
    }
}
