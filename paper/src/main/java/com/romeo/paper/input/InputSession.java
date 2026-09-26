package com.romeo.paper.input;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

/**
 * One pending chat-input session. Created only by {@link InputManager}; the
 * manager owns the lifecycle and guarantees callbacks run on the main thread.
 *
 * @param <T> the validated input type (String, Integer, ...)
 */
public final class InputSession<T> {
    /** What the player is allowed to enter; also drives validation. */
    public enum Type {
        STRING,
        INTEGER,
        URL,
        CONFIRMATION,
        SELECTION
    }

    /** Outcome of validating a raw chat message. */
    public static final class ValidationResult<T> {
        final T value;
        final String error;

        private ValidationResult(T value, String error) {
            this.value = value;
            this.error = error;
        }

        public static <T> ValidationResult<T> ok(T value) {
            return new ValidationResult<T>(value, null);
        }

        public static <T> ValidationResult<T> fail(String error) {
            return new ValidationResult<T>(null, error);
        }

        public boolean isValid() {
            return error == null;
        }

        public T value() {
            return value;
        }

        public String error() {
            return error;
        }
    }

    private final Type type;
    private final String prompt;
    private final Consumer<T> onSuccess;
    private final long createdAt;
    private final Player player;
    private long timeoutTicks;

    private boolean allowCancel = true;
    private boolean allowEmpty = false;
    private boolean reopensGui = false;
    private String invalidMessage = null;
    private Consumer<String> onInvalid = null;
    private Runnable onCancel = null;
    private InputSession<?> next = null;
    private java.util.function.Predicate<String> extraValidator = null;

    InputSession(Player player, Type type, String prompt, Consumer<T> onSuccess) {
        this.player = player;
        this.type = type;
        this.prompt = prompt;
        this.onSuccess = onSuccess;
        this.createdAt = System.currentTimeMillis();
    }

    Player player() {
        return player;
    }

    Type type() {
        return type;
    }

    String prompt() {
        return prompt;
    }

    Consumer<T> onSuccess() {
        return onSuccess;
    }

    long createdAt() {
        return createdAt;
    }

    long timeoutTicks() {
        return timeoutTicks;
    }

    boolean allowsCancel() {
        return allowCancel;
    }

    boolean allowsEmpty() {
        return allowEmpty;
    }

    boolean reopensGui() {
        return reopensGui;
    }

    String invalidMessage() {
        return invalidMessage;
    }

    Consumer<String> onInvalid() {
        return onInvalid;
    }

    Runnable onCancel() {
        return onCancel;
    }

    InputSession<?> next() {
        return next;
    }

    java.util.function.Predicate<String> extraValidator() {
        return extraValidator;
    }

    /** Builder-style options (return the session for fluent chaining). */

    /** Disables the implicit "cancel" keyword for this session. */
    InputSession<T> withoutCancel() {
        this.allowCancel = false;
        return this;
    }

    /** Allows an empty chat message to succeed with the empty string. */
    InputSession<T> allowingEmpty() {
        this.allowEmpty = true;
        return this;
    }

    /**
     * Marks that the caller re-opens a GUI when the session finishes; the
     * manager then skips its own re-prompt handling. Informational only.
     */
    InputSession<T> marksGuiReopen() {
        this.reopensGui = true;
        return this;
    }

    /** Custom message shown when validation fails (default is type-specific). */
    InputSession<T> withInvalidMessage(String message) {
        this.invalidMessage = message;
        return this;
    }

    /** Extra hook invoked (main thread) whenever validation fails. */
    InputSession<T> onInvalid(Consumer<String> handler) {
        this.onInvalid = handler;
        return this;
    }

    /** Hook invoked (main thread) when the player cancels or the session expires. */
    InputSession<T> onCancel(Runnable handler) {
        this.onCancel = handler;
        return this;
    }

    /** Seconds until the session silently expires; 0 disables the timeout. */
    InputSession<T> withTimeoutSeconds(int seconds) {
        this.timeoutTicks = seconds <= 0 ? 0L : seconds * 20L;
        return this;
    }

    /** Additional raw-message check applied after the type validator. */
    InputSession<T> validatedBy(java.util.function.Predicate<String> validator) {
        this.extraValidator = validator;
        return this;
    }

    /** Queues another session to start automatically after this one succeeds. */
    InputSession<T> then(InputSession<?> following) {
        this.next = following;
        return this;
    }

    /** Validates a raw chat message for this session's type. */
    @SuppressWarnings("unchecked")
    ValidationResult<T> validate(String raw) {
        String message = raw == null ? "" : raw.trim();
        if (message.isEmpty() && !allowEmpty) {
            return ValidationResult.fail(invalidMessage != null ? invalidMessage : "&cInput cannot be empty.");
        }
        switch (type) {
            case INTEGER: {
                Integer parsed = tryParseInt(message);
                if (parsed == null) {
                    return ValidationResult.fail(invalidMessage != null ? invalidMessage : "&cInvalid number. Try again.");
                }
                break;
            }
            case URL: {
                if (!isValidUrl(message)) {
                    return ValidationResult.fail(invalidMessage != null ? invalidMessage : "&cInvalid URL.");
                }
                break;
            }
            case CONFIRMATION: {
                String lowered = message.toLowerCase(java.util.Locale.ENGLISH);
                if (!lowered.equals("yes") && !lowered.equals("no")
                        && !lowered.equals("confirm") && !lowered.equals("cancel")) {
                    return ValidationResult.fail("&cType yes or no.");
                }
                break;
            }
            default:
                break;
        }
        if (extraValidator != null && !extraValidator.test(message)) {
            return ValidationResult.fail(invalidMessage != null ? invalidMessage : "&cInvalid input. Try again.");
        }
        return ValidationResult.ok((T) (type == Type.INTEGER ? (Object) tryParseInt(message) : (Object) message));
    }

    private static Integer tryParseInt(String message) {
        try {
            return Integer.valueOf(Integer.parseInt(message));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static boolean isValidUrl(String message) {
        try {
            java.net.URL url = new java.net.URL(message);
            String protocol = url.getProtocol();
            return protocol != null && (protocol.equalsIgnoreCase("http") || protocol.equalsIgnoreCase("https"));
        } catch (Exception exception) {
            return false;
        }
    }
}
