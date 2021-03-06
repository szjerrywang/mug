package com.google.mu.util.stream;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.truth.MultimapSubject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import junit.framework.TestCase;

/**
 * Tests to ensure {@link BiStream#from(Stream, Function, Function)} maintains the invariant that
 * the functions are invoked at most once per entry.
 */
@RunWith(JUnit4.class)
public final class BiStreamFunctionEvaluationTest extends TestCase {
  private final List<Object> evaluatedKeys = new ArrayList<>();
  private final List<Object> evaluatedValues = new ArrayList<>();

  @Test public void testKeys_toKeyFunctionCalledOnce() {
    assertThat(biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 2).keys())
        .containsExactly("1", "2", "3")
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testValues_toValueFunctionCalledOnce() {
    assertThat(biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 2).values())
        .containsExactly(2, 4, 6)
        .inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testFilter_bothFunctionsCalledOnce() {
    assertKeyValues(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10).filter((k, v) -> v > 10))
        .containsExactly("2", 20, "3", 30)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testFilterKeys_bothFunctionsCalledOnce() {
    assertKeyValues(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10).filterKeys(String::isEmpty))
        .isEmpty();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testFilterValues_bothFunctionsCalledOnce() {
    assertKeyValues(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10).filterValues(v -> v < 0))
        .isEmpty();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testPeek_bothFunctionsCalledOnce() {
    assertKeyValues(biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10).peek((k, v) -> {}))
        .containsExactly("1", 10, "2", 20, "3", 30)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testDistinct_bothFunctionsCalledOnce() {
    assertKeyValues(biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10).distinct())
        .containsExactly("1", 10, "2", 20, "3", 30)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testSortedByKeys_bothFunctionsCalledOnce() {
    assertKeyValues(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .sortedByKeys(Comparator.naturalOrder()))
        .containsExactly("1", 10, "2", 20, "3", 30)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testSortedByValues_bothFunctionsCalledOnce() {
    assertKeyValues(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .sortedByValues(Comparator.naturalOrder()))
        .containsExactly("1", 10, "2", 20, "3", 30)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testMapKeys_toKeyFunctionCalledOnce() {
    assertThat(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .mapKeys(k -> k + "." + k)
                .keys())
        .containsExactly("1.1", "2.2", "3.3")
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testMapValues_toValueFunctionCalledOnce() {
    assertThat(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .mapValues(v -> v + 1)
                .values())
        .containsExactly(11, 21, 31)
        .inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testMapKeysWithBiFunction_bothFunctionsCalledOnce() {
    assertKeyValues(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .mapKeys((k, v) -> k + "." + v))
        .containsExactly("1.10", 10, "2.20", 20, "3.30", 30)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testMapValuesWithBiFunction_bothFunctionsCalledOnce() {
    assertKeyValues(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .mapValues((k, v) -> k + "." + v))
        .containsExactly("1", "1.10", "2", "2.20", "3", "3.30")
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testMapWithBiFunctions_bothFunctionsCalledOnce() {
    assertKeyValues(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .map((k, v) -> k + "." + v, (k, v) -> v + "<-" + k))
        .containsExactly("1.10", "10<-1", "2.20", "20<-2", "3.30", "30<-3")
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testMapToObj_bothFunctionsCalledOnce() {
    assertThat(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .mapToObj((k, v) -> k + "->" + v))
        .containsExactly("1->10", "2->20", "3->30")
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testMapToInt_bothFunctionsCalledOnce() {
    assertThat(biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10).mapToInt((k, v) -> v))
        .containsExactly(10, 20, 30)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testMapToLong_bothFunctionsCalledOnce() {
    assertThat(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .mapToLong((k, v) -> Long.valueOf(k)))
        .containsExactly(1L, 2L, 3L)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testMapToDouble_bothFunctionsCalledOnce() {
    assertThat(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .mapToDouble((k, v) -> Double.valueOf(k))
                .boxed())
        .containsExactly(1D, 2D, 3D)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testFlatMapKeys_bothFunctionsCalledOnlyOnce() {
    assertKeyValues(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .flatMapKeys(k -> Stream.of(k, k)))
        .containsExactly("1", 10, "1", 10, "2", 20, "2", 20, "3", 30, "3", 30)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testFlatMapKeysWithBiFunction_bothFunctionsCalledOnlyOnce() {
    assertKeyValues(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .flatMapKeys((k, v) -> Stream.of(k, v)))
        .containsExactly("1", 10, 10, 10, "2", 20, 20, 20, "3", 30, 30, 30)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testFlatMapValues_bothFunctionsCalledOnlyOnce() {
    assertKeyValues(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .flatMapValues(v -> Stream.of(v, v)))
        .containsExactly("1", 10, "1", 10, "2", 20, "2", 20, "3", 30, "3", 30)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testFlatMapValuesWithBiFunction_bothFunctionsCalledOnlyOnce() {
    assertKeyValues(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .flatMapValues((k, v) -> Stream.of(k, v)))
        .containsExactly("1", "1", "1", 10, "2", "2", "2", 20, "3", "3", "3", 30)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testFlatMap_bothFunctionsCalledOnlyOnce() {
    assertKeyValues(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .flatMap((k, v) -> BiStream.of(k, v, v, k)))
        .containsExactly("1", 10, 10, "1", "2", 20, 20, "2", "3", 30, 30, "3")
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testFlatMapToObj_bothFunctionsCalledOnlyOnce() {
    assertThat(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .flatMapToObj((k, v) -> Stream.of(v, k)))
        .containsExactly(10, "1", 20, "2", 30, "3")
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testFlatMapToInt_bothFunctionsCalledOnlyOnce() {
    assertThat(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .flatMapToInt((k, v) -> IntStream.of(v, Integer.parseInt(k))))
        .containsExactly(10, 1, 20, 2, 30, 3)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testFlatMapToLong_bothFunctionsCalledOnlyOnce() {
    assertThat(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .flatMapToLong((k, v) -> LongStream.of(Long.valueOf(v), Long.parseLong(k))))
        .containsExactly(10L, 1L, 20L, 2L, 30L, 3L)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testFlatMapToDouble_bothFunctionsCalledOnlyOnce() {
    assertThat(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10)
                .flatMapToDouble((k, v) -> DoubleStream.of((double) v, Double.parseDouble(k)))
                .boxed())
        .containsExactly(10D, 1D, 20D, 2D, 30D, 3D)
        .inOrder();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testInverseThenKeys_toValueFunctionCalledOnce() {
    assertThat(biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 2).inverse().keys())
        .containsExactly(2, 4, 6);
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testInverseThenValues_toKeyFunctionCalledOnce() {
    assertThat(biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 2).inverse().values())
        .containsExactly("1", "2", "3");
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testSkip_skippedEntriesNotEvaluated() {
    assertKeyValues(biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 2).skip(2))
        .containsExactly("3", 6);
    assertThat(evaluatedKeys).containsExactly(3).inOrder();
    assertThat(evaluatedValues).containsExactly(3).inOrder();
  }

  @Test public void testLimit_limitedEntriesNotEvaluated() {
    assertKeyValues(biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10).limit(2))
        .containsExactly("1", 10, "2", 20);
    assertThat(evaluatedKeys).containsExactly(1, 2).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2).inOrder();
  }

  @Test public void testAnyMatch_functionsCalledOnlyOnce() {
    assertThat(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10).anyMatch((k, v) -> v < 0))
        .isFalse();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testAllMatch_functionsCalledOnlyOnce() {
    assertThat(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10).allMatch((k, v) -> v > 0))
        .isTrue();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  @Test public void testNoneMatch_functionsCalledOnlyOnce() {
    assertThat(
            biStream(Stream.of(1, 2, 3), Object::toString, i -> i * 10).noneMatch((k, v) -> v < 0))
        .isTrue();
    assertThat(evaluatedKeys).containsExactly(1, 2, 3).inOrder();
    assertThat(evaluatedValues).containsExactly(1, 2, 3).inOrder();
  }

  private <K, V, T> BiStream<K, V> biStream(
      Stream<T> stream,
      Function<? super T, ? extends K> toKey,
      Function<? super T, ? extends V> toValue) {
    return BiStream.from(
        stream, trackCallHistory(toKey, evaluatedKeys), trackCallHistory(toValue, evaluatedValues));
  }

  // Poor man's mock
  private static <F, T> Function<F, T> trackCallHistory(
      Function<? super F, ? extends T> function, List<Object> history) {
    return arg -> {
      history.add(arg);
      return function.apply(arg);
    };
  }

  private static MultimapSubject assertKeyValues(BiStream<?, ?> stream) {
    Multimap<?, ?> multimap = stream.collect(BiStreamFunctionEvaluationTest::toLinkedListMultimap);
    return assertThat(multimap);
  }

  private static <T, K, V> Collector<T, ?, LinkedListMultimap<K, V>> toLinkedListMultimap(
      Function<? super T, ? extends K> toKey, Function<? super T, ? extends V> toValue) {
    return Collector.of(
        LinkedListMultimap::create,
        (m, e) -> m.put(toKey.apply(e), toValue.apply(e)),
        (m1, m2) -> {
          m1.putAll(m2);
          return m1;
        });
  }
}
