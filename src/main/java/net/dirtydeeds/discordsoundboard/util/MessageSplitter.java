package net.dirtydeeds.discordsoundboard.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MessageSplitter {
    private final int maxMessageLength;

    public MessageSplitter(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }

    private static String wrapInCodeBlockFormatting(StringBuilder stringBuilder) {
        stringBuilder.insert(0, "```");
        stringBuilder.append("```");
        return stringBuilder.toString();
    }

    private static String wrapInCodeBlockFormatting(String string) {
        StringBuilder sb = new StringBuilder(string);
        return wrapInCodeBlockFormatting(sb);
    }

    /**
     * Split the message in chunks by length. Attempts to keep full lines together, and if that fails then splits by
     * whitespace, and then by force to fit the max message length.
     */
    public List<String> splitMessage(@NotNull StringBuilder message) {
        Splitter newlineSplitter = Splitter.on('\n');
        Splitter wordSplitter = Splitter.on(CharMatcher.breakingWhitespace());
        Splitter fixedSplitter = Splitter.fixedLength(maxMessageLength);

        List<String> lines = newlineSplitter.splitToList(message.toString());
        List<String> messages = new ArrayList<>();
        StringBuilder sb = new StringBuilder(maxMessageLength);

        if (lines.isEmpty()) {
            return Collections.emptyList();
        }

        for (String line : lines) {
            if (line.length() > maxMessageLength) {
                List<String> words = wordSplitter.splitToList(line);
                for (String word : words) {
                    if (word.length() > maxMessageLength) {
                        List<String> partialWords = fixedSplitter.splitToList(word);
                        for (String partialWord : partialWords) {
                            if (sb.length() > 0) {
                                messages.add(wrapInCodeBlockFormatting(sb));
                                sb = new StringBuilder(maxMessageLength);
                            }
                            messages.add(wrapInCodeBlockFormatting(partialWord));
                        }
                        continue;
                    }

                    if (sb.length() + word.length() > maxMessageLength) {
                        if (sb.length() > 0) {
                            messages.add(wrapInCodeBlockFormatting(sb));
                            sb = new StringBuilder(maxMessageLength);
                        }
                    }
                    if (sb.length() > 0) {
                        sb.append(" ").append(word);
                    } else {
                        sb.append(word);
                    }
                }
                if (sb.length() > 0) {
                    messages.add(wrapInCodeBlockFormatting(sb));
                    sb = new StringBuilder(maxMessageLength);
                }
                continue;
            }

            if (sb.length() + line.length() > maxMessageLength) {
                messages.add(wrapInCodeBlockFormatting(sb));
                sb = new StringBuilder(maxMessageLength);
            }

            sb.append(line).append('\n');
        }
        if (sb.length() > 0) {
            messages.add(wrapInCodeBlockFormatting(sb));
        }

        return messages;

    }
}
