package io.hhplus.tdd.helper;


import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
/*
* 테스트 결과 이벤트(성공·실패·스킵·중단)를 후킹하는데 쓰임.
로깅및 , 성능 계측, 외부 알림 등 “사후 처리” 가 가능하다.
* */
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

        // TODO : 실패한 케이스에대해 File 에 테스트정보및 , 실행시간, 등의 정보를 기록하여 관리가능!
    }
}
