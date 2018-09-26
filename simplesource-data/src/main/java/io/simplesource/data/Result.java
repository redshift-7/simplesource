package io.simplesource.data;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * A Result is either a success containing a value of the specified type, or a failure with a list value reasons.
 *
 * @param <E> on failure there will be a NonEmptyList of Reason instances with an error value of this type.
 * @param <A> when successful there will be a contained value of this type.
 */
public abstract class Result<E, A> {

    /**
     * Factory method to creates a succesful {@code Result} from a value.
     *
     * @param <E> The error type for failures.
     * @param <S> when successful there will be a contained value this type.
     * @param s value that will be lifted into this {@code Result}
     * @return new success {@code Result} containing value provided.
     */
    public static <E, S> Result<E, S> success(final S s) {
        return new Success<>(s);
    }

    /**
     * Factory method to creates a failed {@code Result} from a reason message.
     *
     * @param <E> The error type for failures.
     * @param <S> when successful there will be a contained value this type.
     * @param error for the type of failure
     * @param reason that this {@code Result} to be failed
     * @return new failed {@code Result} containing the failed {@code Reason}
     */
    public static <E, S> Result<E, S> failure(final E error, final String reason) {
        return failure(Reason.of(error, reason));
    }

    /**
     * Factory method to creates a Failed {@code Result} from a {@code Throwable}.
     *
     * @param <E> The error type for failures.
     * @param <S> when successful there will be a contained value this type.
     * @param error for the type of failure
     * @param throwable that caused this {@code Result} to be failed
     * @return new failed {@code Result} containing the failed {@code Reason}
     */
    public static <E, S> Result<E, S> failure(final E error, final Throwable throwable) {
        return failure(Reason.of(error, throwable));
    }

    /**
     * Factory method to creates a Failed {@code Result} from a array value {@code Reason}
     *
     * @param <E> The error type for failures.
     * @param <S> when successful there will be a contained value this type.
     * @param reason the first cause of {@code Result} to be failed
     * @param reasons a variable argument list of further causes of failure
     * @return new failed {@code Result} containing the failed {@code Reason}
     */
    public static <E, S> Result<E, S> failure(final Reason<E> reason, final Reason<E>... reasons) {
        return failure(NonEmptyList.of(reason, reasons));
    }

    /**
     * Factory method to creates a Failed {@code Result} from a {@code {@link NonEmptyList<Reason>}}}
     *
     * @param <E> The error type for failures.
     * @param <S> when successful there will be a contained value this type.
     * @param reasons what caused this {@code Result} to failed
     * @return new failed {@code Result} containing the failed {@code Reason}
     */
    public static <E, S> Result<E, S> failure(final NonEmptyList<Reason<E>> reasons) {
        return new Failure<>(reasons);
    }

    /**
     *  ADT, do not construct outside this class
     */
    private Result() {}

    /**
     * Return true if this Result is a success.
     *
     * @return true if this Result is a success.
     */
    public abstract boolean isSuccess();

    /**
     * Return true if this Result is a failure.
     *
     * @return true if this Result is a failure.
     */
    public final boolean isFailure() {
        return !isSuccess();
    }

    /**
     * Return the value contained within this {@code Result} if the {@linkplain Result#isSuccess()} is true else
     * return the default value provided.
     *
     * @param defaultValue to return if this {@linkplain Result#isSuccess()} is false
     * @return the value contained within this {@code Result} if the {@linkplain Result#isSuccess()} is true else
     * return the default value provided.
     */
    public A getOrElse(final A defaultValue) {
        return fold(r -> defaultValue, a -> a);
    }

    /**
     * Turn this Result into a destination type by supplying functions
     * from the reasons, or a function from the contained value.
     *
     * @param <T> The target return type
     * @param f a function to apply to the list of reseans for failure, if any
     * @param s a function to apply to successfully updated aggregate value
     * @return the target type returned in either the success or the failure cases
     */
    public <T> T fold(final Function<NonEmptyList<Reason<E>>, T> f, final Function<A, T> s) {
        return isSuccess() ?
                s.apply(((Success<E, A>) this).value) :
                f.apply(((Failure<E, A>) this).reasons);
    }

    /**
     * If this is a failure, return the reasons for that failure, if not return nothing.
     *
     * @return the nonempty list of failures
     */
    public Optional<NonEmptyList<Reason<E>>> failureReasons() {
        return fold(Optional::of, a -> Optional.empty());
    }

    /**
     * Runs the given function on the contents value this Result, returning a new {@code Result} with the
     * output value the result of that function.
     *
     * @param <T>    The new component type
     * @param mapper A function
     * @return a {@code Result<E, T>}
     * @throws NullPointerException if {@code mapper} is null
     */
    public <T> Result<E, T> map(final Function<A, T> mapper) {
        return fold(
                Result::failure,
                a -> success(requireNonNull(mapper, "mapper is null").apply(a))
        );
    }

    /**
     * Runs the given function on the contents value this Result, returning a new {@code Result} with the
     * extracted reason extracted from the functions returned {@code Result<E, T>}
     *
     * @param mapper A mapper
     * @param <T>    The new component type
     * @return a {@code Reason}
     * @throws NullPointerException if {@code mapper} is null
     */
    public <T> Result<E, T> flatMap(final Function<A, Result<E, T>> mapper) {
        return fold(
                Result::failure,
                a -> requireNonNull(mapper, "mapper is null").apply(a)
        );
    }

    /**
     * If a value is present, invoke the specified consumer with the value,
     * otherwise do nothing.
     *
     * @param consumer The consumer to invoke
     */
    public void ifSuccessful(final Consumer<A> consumer) {
        if (isSuccess()) {
            consumer.accept(((Success<E, A>) this).value);
        }
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof Result) {
            final Result<?, ?> resultObj= (Result<?, ?>)obj;
            if (isSuccess() == resultObj.isSuccess()) {
                return resultObj.fold(
                        reasons -> Objects.equals(((Failure<E, A>) this).reasons, reasons),
                        value -> Objects.equals(((Success<E, A>)this).value, value)
                );
            }
        }
        return false;
    }

    // -- implementations

    static final class Success<E, A> extends Result<E, A> {
        private final A value;

        public Success(final A value) {
            this.value = requireNonNull(value);
        }

        @Override
        public boolean isSuccess() {
            return true;
        }

        @Override
        public String toString() {
            return "Success{" + "value=" + value + '}';
        }

    }

    static final class Failure<E, A> extends Result<E, A> {
        private final NonEmptyList<Reason<E>> reasons;

        Failure(final NonEmptyList<Reason<E>> reasons) {
            this.reasons = requireNonNull(reasons);
        }

        @Override
        public boolean isSuccess() {
            return false;
        }

        @Override
        public String toString() {
            return "Failure{" + "reasons=" + reasons + '}';
        }
    }
}
