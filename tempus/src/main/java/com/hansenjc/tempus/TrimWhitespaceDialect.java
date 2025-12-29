package com.hansenjc.tempus;

import org.thymeleaf.dialect.IPostProcessorDialect;
import org.thymeleaf.postprocessor.IPostProcessor;
import org.thymeleaf.postprocessor.PostProcessor;
import org.thymeleaf.templatemode.TemplateMode;

import java.util.Collections;
import java.util.Set;

public class TrimWhitespaceDialect implements IPostProcessorDialect {
    final Set<IPostProcessor> processors = Collections.singleton(new PostProcessor(TemplateMode.HTML, TrimWhitespacePostProcessor.class, Integer.MAX_VALUE));

    @Override
    public Set<IPostProcessor> getPostProcessors() {
        return processors;
    }

    @Override
    public int getDialectPostProcessorPrecedence() {
        return 0;
    }

    @Override
    public String getName() {
        return "TrimWhiteSpace";
    }
}
