package com.hansenjc.tempus;

import org.thymeleaf.engine.AbstractTemplateHandler;
import org.thymeleaf.model.IText;

public class TrimWhitespacePostProcessor extends AbstractTemplateHandler {
    @Override
    public void handleText(IText textNode) {
        String text = textNode.getText();

        if (text == null || text.isEmpty()) {
            super.handleText(textNode);
            return;
        }

        String newText = text.trim();
        IText newNode = getContext().getModelFactory().createText(newText);
        super.handleText(newNode);
    }
}