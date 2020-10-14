package com.openhtmltopdf.layout;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.function.ToIntFunction;

import com.openhtmltopdf.extend.FSTextBreaker;

public class BreakerTestSupport {
    enum ContextIs {
        UNBREAKABLE,
        NEEDS_NEW_LINE,
        FINISHED_IN_CHAR_BREAKING_MODE,
        FINISHED,
        ENDS_ON_NL,
        /**
         * This should really be called ENDS_ON_VISIBLE_SOFT_HYPHEN.
         */
        ENDS_ON_SOFT_HYPHEN,
        ENDS_ON_WORD_BREAK
    }

    static void assertContextIs(LineBreakContext context, ContextIs... trues) {
        EnumSet<ContextIs> truthy = trues.length > 0 ? 
                     EnumSet.copyOf(Arrays.asList(trues)) :
                     EnumSet.noneOf(ContextIs.class);
        boolean desired;

        desired = truthy.contains(ContextIs.UNBREAKABLE);
        assertThat(context.isUnbreakable(), equalTo(desired));

        desired = truthy.contains(ContextIs.NEEDS_NEW_LINE);
        assertThat(context.isNeedsNewLine(), equalTo(desired));

        desired = truthy.contains(ContextIs.FINISHED_IN_CHAR_BREAKING_MODE);
        assertThat(context.isFinishedInCharBreakingMode(), equalTo(desired));

        desired = truthy.contains(ContextIs.FINISHED);
        assertThat(context.isFinished(), equalTo(desired));

        desired = truthy.contains(ContextIs.ENDS_ON_NL);
        assertThat(context.isEndsOnNL(), equalTo(desired));

        desired = truthy.contains(ContextIs.ENDS_ON_SOFT_HYPHEN);
        assertThat(context.isEndsOnSoftHyphen(), equalTo(desired));

        desired = truthy.contains(ContextIs.ENDS_ON_WORD_BREAK);
        assertThat(context.isEndsOnWordBreak(), equalTo(desired));
    }

    private static class SimpleCharBreaker implements FSTextBreaker {
        private String text;
        private int pos;

        @Override
        public int next() {
            return pos > text.length() ? -1 : pos++;
        }

        @Override
        public void setText(String newText) {
            this.text = newText;
            this.pos = 0;
        }
    }

    private static class SimpleLineBreaker implements FSTextBreaker {
        private String text;
        private int position;

        @Override
        public int next() {
            if (this.position == -1) {
                return -1;
            }

            int ret = text.indexOf(' ', this.position);
            int softHyphen = text.indexOf('\u00ad', this.position);

            if (softHyphen != -1 && (softHyphen < ret || ret == -1)) {
                ret = softHyphen;
            }

            this.position = ret < 0 ? -1 : ret + 1;
            return this.position;
        }

        @Override
        public void setText(String newText) {
            this.text = newText;
            this.position = 0;
        }
    }

    static FSTextBreaker createLine(String line) {
        SimpleLineBreaker breaker = new SimpleLineBreaker();
        breaker.setText(line);
        return breaker;
    }

    static FSTextBreaker createChar(String line) {
        FSTextBreaker breaker = new SimpleCharBreaker();
        breaker.setText(line);
        return breaker;
    }

    static LineBreakContext createContext(String str) {
        LineBreakContext ctx = new LineBreakContext();
        ctx.setMaster(str);
        return ctx;
    }

    static final ToIntFunction<String> MEASURER = String::length;
    static final ToIntFunction<String> MEASURER3 = (str) -> str.length() * 3;
    static final ToIntFunction<String> MEASURER_WITH_ZERO_WIDTH_SOFT_HYPHEN = (str) -> {
        long softHyphenCount = str.chars().filter(ch -> ch == Breaker.SOFT_HYPHEN).count();
        return (int) (str.length() - softHyphenCount);
    };
}
