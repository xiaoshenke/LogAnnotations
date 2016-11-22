package wuxian.me.logannotations.compiler;

import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.Element;

/**
 * Processor exception class for errors that occur during processing.
 */
public class ProcessingException extends Exception {
    private final @Nullable Element mElement;

    public ProcessingException(@Nullable Element element, @Nullable String message) {
        super(message);
        this.mElement = element;
    }

    @Nullable
    public Element getElement() {
        return mElement;
    }
}
