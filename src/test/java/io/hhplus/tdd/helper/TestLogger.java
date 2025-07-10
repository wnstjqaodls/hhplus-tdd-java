package io.hhplus.tdd.helper;


import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class TestLogger implements TestWatcher {

    private static final Logger log = LoggerFactory.getLogger(TestLogger.class);

    @Override
    public void testSuccessful(ExtensionContext context) {
        String testName = context.getDisplayName();
        String className = context.getTestClass().map(clazz -> clazz.getSimpleName()).orElse("");
        log.info("[항해99999] 테스트 성공 케이스 parameters : {}.{}",className,testName);
    }
    @Override
    public void testFailed(ExtensionContext context, Throwable throwable) {
        String testName = context.getDisplayName();
        String className = context.getTestClass().map(clazz -> clazz.getSimpleName()).orElse("");
        log.info("[항해99999] 테스트 실패 케이스 parameters : {}.{}",className,testName);

    }
}
